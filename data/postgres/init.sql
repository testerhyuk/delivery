DROP TABLE IF EXISTS restaurant;

-- 1. 테이블 생성
CREATE TABLE IF NOT EXISTS restaurant (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    address VARCHAR(500),
    category varchar(20),
    longitude DECIMAL(18, 14), -- 경도
    latitude DECIMAL(18, 14),  -- 위도
    eum_card VARCHAR(50)
);

-- 2. CSV 데이터 적재 (컬럼 순서를 CSV 헤더와 일치시켜야 함)
COPY restaurant(name, address, category, longitude, latitude, eum_card)
FROM '/docker-entrypoint-initdb.d/인천상권_정제데이터_e음카드2.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');