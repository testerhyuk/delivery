-- PostGIS 활성화
CREATE EXTENSION IF NOT EXISTS postgis;

DO $$
DECLARE
    slot TEXT;
BEGIN
    FOR slot IN
        SELECT slot_name FROM pg_replication_slots
        WHERE slot_name IN (
            'order_outbox_slot',
            'pay_outbox_slot',
            'rider_outbox_slot',
            'seller_outbox_slot'
        )
    LOOP
        PERFORM pg_drop_replication_slot(slot);
    END LOOP;
END $$;


DROP TABLE IF EXISTS restaurant;

-- 1. 테이블 생성
CREATE TABLE IF NOT EXISTS restaurant (
    id SERIAL PRIMARY KEY,
    restaurant_id VARCHAR(50) UNIQUE DEFAULT ('RES-' || nextval('restaurant_id_seq')),
    name VARCHAR(255),
    address VARCHAR(500),
    category varchar(20),
    longitude DECIMAL(18, 14), -- 경도
    latitude DECIMAL(18, 14),  -- 위도
    location geometry(Point, 4326)
);

-- 2. CSV 데이터 적재
COPY restaurant(name, address, category, longitude, latitude)
FROM '/docker-entrypoint-initdb.d/상권데이터.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

-- 3. location 컬럼 채우기
UPDATE restaurant
SET location = ST_SetSRID(ST_MakePoint(longitude::float, latitude::float), 4326);

-- 4. 공간 인덱스 생성
CREATE INDEX idx_restaurant_location ON restaurant USING GIST(location);

-- 5. 복합 인덱스 생성
CREATE INDEX idx_restaurant_category_lat_lng ON restaurant(category, latitude, longitude);

-- 6. 트리거 함수 생성
CREATE OR REPLACE FUNCTION update_restaurant_location()
RETURNS TRIGGER AS $$
BEGIN
    NEW.location = ST_SetSRID(ST_MakePoint(NEW.longitude::float, NEW.latitude::float), 4326);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 7. 트리거 생성
CREATE TRIGGER trigger_restaurant_location
BEFORE INSERT OR UPDATE ON restaurant
FOR EACH ROW
EXECUTE FUNCTION update_restaurant_location();

-- 8. 메뉴 테이블 생성
CREATE TABLE IF NOT EXISTS restaurant_menus (
    id BIGINT PRIMARY KEY,
    menu_id VARCHAR(100) NOT NULL UNIQUE,
    restaurant_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    price INTEGER NOT NULL,
    CONSTRAINT fk_restaurant_menus_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant(restaurant_id)
);

CREATE TABLE IF NOT EXISTS address_master (
    mgmt_num           VARCHAR(40) PRIMARY KEY,
    sido               VARCHAR(20),
    sigungu            VARCHAR(20),
    zip_code           VARCHAR(5),
    location           GEOMETRY(Point, 4326),
    last_updated       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS address_road (
    mgmt_num           VARCHAR(40) PRIMARY KEY REFERENCES address_master(mgmt_num),
    sido               VARCHAR(20),
    sigungu            VARCHAR(20),
    b_dong_name        VARCHAR(20),
    road_name          VARCHAR(50),
    build_main         INT,
    build_sub          INT,
    zip_code           VARCHAR(5),
    build_nm_official  VARCHAR(100),
    build_nm_sgg       VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS address_jibun (
    id                 SERIAL PRIMARY KEY,
    mgmt_num           VARCHAR(40) REFERENCES address_master(mgmt_num),
    b_dong_name        VARCHAR(20),
    ri_name            VARCHAR(20),
    jibun_main         INT,
    jibun_sub          INT
);

COPY address_master(mgmt_num, sido, sigungu, zip_code)
FROM '/docker-entrypoint-initdb.d/address_master.csv'
DELIMITER ',' CSV HEADER;

COPY address_road(mgmt_num, sido, sigungu, b_dong_name, road_name, build_main, build_sub, zip_code, build_nm_official, build_nm_sgg)
FROM '/docker-entrypoint-initdb.d/도로명데이터.csv'
DELIMITER ',' CSV HEADER;

COPY address_jibun(mgmt_num, b_dong_name, ri_name, jibun_main, jibun_sub)
FROM '/docker-entrypoint-initdb.d/지번데이터.csv'
DELIMITER ',' CSV HEADER;

CREATE INDEX idx_master_location ON address_master USING GIST (location);
CREATE INDEX idx_road_search ON address_road (road_name, build_main, build_sub);
CREATE INDEX idx_jibun_search ON address_jibun (jibun_main, jibun_sub);

CREATE TABLE IF NOT EXISTS address_location (
    mgmt_num        VARCHAR(40) PRIMARY KEY REFERENCES address_master(mgmt_num),
    location        GEOMETRY(Point, 4326),  -- 건물 대표 좌표
    accuracy_level  SMALLINT DEFAULT 1,     -- 1:도로명, 2:동단위, 3:동호수
    source          VARCHAR(20) DEFAULT 'nominatim', -- nominatim / delivery
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_address_location ON address_location USING GIST(location);

CREATE TABLE delivery_location_log (
    id              BIGSERIAL PRIMARY KEY,
    order_id        VARCHAR(50) NOT NULL,
    mgmt_num        VARCHAR(40) REFERENCES address_master(mgmt_num),
    detail_address  VARCHAR(100),           -- "204동 1201호" 같은 상세주소
    location        GEOMETRY(Point, 4326),  -- 배달기사 완료 시점 GPS 좌표
    accuracy_meter  FLOAT,                  -- GPS 정확도 (미터)
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS address_detail_location (
    id              BIGSERIAL PRIMARY KEY,
    mgmt_num        VARCHAR(40) REFERENCES address_master(mgmt_num),
    detail_address  VARCHAR(100),           -- "204동", "204동 1201호"
    location        GEOMETRY(Point, 4326),  -- 누적 평균 좌표
    sample_count    INT DEFAULT 1,          -- 누적된 배달 완료 수
    confidence      FLOAT DEFAULT 0.0,      -- 신뢰도 (sample_count 기반)
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(mgmt_num, detail_address)
);

CREATE INDEX idx_detail_location ON address_detail_location USING GIST(location);