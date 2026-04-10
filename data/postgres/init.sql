DROP TABLE IF EXISTS restaurant;

-- 1. 테이블 생성
CREATE TABLE IF NOT EXISTS restaurant (
    id SERIAL PRIMARY KEY,
    restaurant_id VARCHAR(50) DEFAULT ('RES-' || nextval('restaurant_id_seq')),
    name VARCHAR(255),
    address VARCHAR(500),
    category varchar(20),
    longitude DECIMAL(18, 14), -- 경도
    latitude DECIMAL(18, 14)  -- 위도
);

-- 2. CSV 데이터 적재 (컬럼 순서를 CSV 헤더와 일치시켜야 함)
COPY restaurant(name, address, category, longitude, latitude)
FROM '/docker-entrypoint-initdb.d/상권.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

-- 3. 도로명 주소 테이블 생성 (주소기반산업지원서비스)
CREATE TABLE IF NOT EXISTS road_address (
    road_address_mgmt_no VARCHAR(26) NOT NULL,      -- 도로명주소관리번호
    legal_dong_code VARCHAR(10),                    -- 법정동코드
    sido_name VARCHAR(40),                          -- 시도명
    sigungu_name VARCHAR(40),                       -- 시군구명
    legal_eupmyeondong_name VARCHAR(40),            -- 법정읍면동명
    road_code VARCHAR(12) NOT NULL,                 -- 도로명코드
    road_name VARCHAR(80),                          -- 도로명
    underground_yn CHAR(1) NOT NULL,                -- 지하여부 (0:지상, 1:지하, 2:공중, 3:수상)
    building_main_no INTEGER NOT NULL,              -- 건물본번
    building_sub_no INTEGER NOT NULL,               -- 건물부번
    postal_code VARCHAR(5),                         -- 기초구역번호(우편번호)
    PRIMARY KEY (
        road_address_mgmt_no,
        road_code,
        underground_yn,
        building_main_no,
        building_sub_no
    )
);

-- 4. 필요한 컬럼(1,2,3,4,5,10,11,12,13,14,17)만 직접 적재
COPY road_address (
    road_address_mgmt_no,
    legal_dong_code,
    sido_name,
    sigungu_name,
    legal_eupmyeondong_name,
    road_code,
    road_name,
    underground_yn,
    building_main_no,
    building_sub_no,
    postal_code
)
FROM PROGRAM 'cut -d"|" -f1,2,3,4,5,10,11,12,13,14,17 /docker-entrypoint-initdb.d/rnaddrkor_incheon.txt'
WITH (
    FORMAT csv,
    DELIMITER '|',
    HEADER false,
    ENCODING 'WIN949'
);