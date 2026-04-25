# 🍔 배달의 음 (Delivery Service Platform)

MSA 기반 배달 플랫폼 백엔드 프로젝트입니다. 실제 배달 서비스의 핵심 기능인 주문, 결제, 배달 추적, 위치 기반 서비스를 구현했습니다.

---

## 목차

- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [서비스 구성](#서비스-구성)
- [주요 기능](#주요-기능)
- [이벤트 흐름](#이벤트-흐름)
- [성능 최적화](#성능-최적화)
- [트러블슈팅](#트러블슈팅)
- [실행 방법](#실행-방법)

---

## 기술 스택

**Backend**
- Java 21, Spring Boot 3.5, Spring Cloud Gateway
- Virtual Thread (Project Loom)
- Spring Data JPA, Hibernate Spatial
- Spring Security, OAuth2 (Google, Naver)

**Messaging & Event**
- Apache Kafka, Kafka Connect
- Debezium (PostgreSQL CDC)
- Outbox Pattern

**Database & Cache**
- PostgreSQL 17 + PostGIS (공간 쿼리)
- Redis (분산 락, 라이더 위치)
- HikariCP

**Infrastructure**
- Docker, Docker Compose
- Nominatim (자체 구축 지오코딩 서버)
- MockServer (결제사 API 모킹)

**Monitoring & Testing**
- Jaeger (분산 추적, OpenTelemetry)
- nGrinder (부하 테스트)

**Frontend**
- React, TypeScript, Vite, TailwindCSS
- Toss Payments SDK

---

## 시스템 아키텍처

```
                        ┌─────────────────┐
                        │   React Client  │
                        └────────┬────────┘
                                 │ HTTP / WebSocket
                        ┌────────▼────────┐
                        │  API Gateway    │  :8000
                        │  (JWT 인증)     │
                        └────────┬────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌────────▼──────┐    ┌───────────▼───────┐    ┌─────────▼───────┐
│ member-service│    │  order-service    │    │ restaurant-service│
│   :9096       │    │    :9100          │    │    :9097          │
└───────────────┘    └───────────────────┘    └─────────────────┘
         │                       │                       │
┌────────▼──────┐    ┌───────────▼───────┐    ┌─────────▼───────┐
│  pay-service  │    │  seller-service   │    │  rider-service   │
│   :9094       │    │    :9098          │    │    :9095          │
└───────────────┘    └───────────────────┘    └─────────────────┘
                                 │
                        ┌────────▼────────┐
                        │ location-service│  :9102
                        │ (PostGIS + NOM) │
                        └─────────────────┘

공통 인프라
├── PostgreSQL 17 + PostGIS  :5432
├── Redis                    :6379
├── Kafka + Kafka Connect    :9092 / :8083
├── Debezium CDC             (Kafka Connect 플러그인)
├── Nominatim                :8088
└── Jaeger                   :16686
```

---

## 서비스 구성

| 서비스 | 포트 | 역할 |
|--------|------|------|
| gateway | 8000 | API Gateway, JWT 인증 필터, 라우팅 |
| member-service | 9096 | 회원 관리, OAuth2 로그인, JWT 발급 |
| order-service | 9100 | 주문 생성/상태 관리, WebSocket |
| pay-service | 9094 | 결제 준비/승인 (Toss Payments) |
| seller-service | 9098 | 주문 수락, WebSocket |
| rider-service | 9095 | 배달 수락/완료, Redis 분산 락, WebSocket |
| restaurant-service | 9097 | 음식점/메뉴 관리, PostGIS 공간 쿼리 |
| location-service | 9102 | 주소 좌표 변환, 크라우드소싱 위치 학습 |

---

## 주요 기능

### 1. OAuth2 소셜 로그인
Google, Naver OAuth2를 통한 회원가입 및 로그인. 회원 유형(일반 유저 / 배달기사 / 판매자)에 따라 역할을 분리하여 관리합니다.

### 2. 주문 및 결제
Toss Payments API를 통한 카드 결제를 지원합니다. 주문 생성 후 결제 준비 → 결제 승인 플로우로 처리하며, 결제 실패 시 자동으로 주문을 취소합니다.

### 3. 실시간 WebSocket 알림
- **판매자**: 신규 주문 알림
- **배달기사**: 반경 3km 이내 배달 요청 알림 (PostGIS 공간 쿼리)
- **주문자**: 배달 시작 알림 (ETA 포함), 배달 완료 알림

### 4. 반경 기반 배달기사 매칭
음식점 좌표를 기준으로 반경 3km 이내에 있는 배달기사에게만 WebSocket 알림을 전송합니다. Haversine 공식으로 거리를 계산합니다.

### 5. 분산 락을 통한 배달 중복 수락 방지
Redis `SETNX`를 활용하여 동일 주문에 대한 배달기사의 중복 수락을 방지합니다. 30초 TTL 설정으로 데드락을 방지합니다.

### 6. ETA (예상 도착 시간) 계산
배달 시작 시 음식점 좌표와 주문자 좌표 사이의 거리를 계산하여 예상 도착 시간을 주문자에게 알려줍니다. 평균 배달 속도(20km/h) 기준으로 계산합니다.

### 7. 크라우드소싱 주소 좌표 학습
배달 완료 데이터를 누적하여 상세 주소(동/호수 단위) 좌표를 학습합니다. 초기에는 Nominatim으로 건물 단위 좌표를 사용하고, 배달이 쌓일수록 정밀도가 높아집니다.

```
조회 우선순위
1. address_detail_location (배달 완료 학습 좌표, 가장 정확)
2. address_location (Nominatim 건물 좌표)
3. Nominatim 실시간 조회 → DB 저장
```

### 8. 자체 지오코딩 서버 (Nominatim)
카카오/네이버 지도 API 대신 OpenStreetMap 기반 Nominatim을 Docker로 직접 구축하여 API 비용 없이 무제한 주소 → 좌표 변환을 처리합니다. 한국 전국 주소 데이터(행정안전부 제공, 640만 건)를 DB에 보유합니다.

---

## 이벤트 흐름

### Outbox Pattern + Debezium CDC

각 서비스는 직접 Kafka에 메시지를 발행하지 않고, 동일 트랜잭션 내에 outbox 테이블에 이벤트를 저장합니다. Debezium이 PostgreSQL WAL을 감지하여 Kafka로 전달합니다. 이를 통해 메시지 유실 없이 이벤트 기반 통신을 보장합니다.

```
주문 생성 → 결제 플로우

주문 생성 (order-service)
  → order_outbox 저장
    → Debezium → Kafka [order-events]
      → seller-service 컨슈밍 → 판매자 WebSocket 알림

결제 승인 (pay-service)
  → pay_outbox 저장
    → Debezium → Kafka [pay-events]
      → order-service 컨슈밍 → 주문 상태 PAID 변경
        → seller_outbox 저장
          → Debezium → Kafka [seller-events]
            → rider-service 컨슈밍 → 주변 배달기사 WebSocket 알림

배달 수락/완료 (rider-service)
  → rider_outbox 저장
    → Debezium → Kafka [rider-events]
      → order-service 컨슈밍 → 주문 상태 변경 + 주문자 WebSocket 알림
      → location-service 컨슈밍 → 주소 좌표 학습
```

---

## 성능 최적화

작성중

---

## 트러블슈팅

### JavaScript Long 타입 정밀도 손실

**문제**: 회원 ID(`304176937408598016`)가 JavaScript로 전달될 때 `304176937408598000`으로 변환되어, WebSocket 세션 매핑 키 불일치가 발생했습니다.

**원인**: JavaScript는 Number 타입이 64비트 부동소수점(IEEE 754)이라 53비트 이상의 정수를 정확히 표현하지 못합니다. Java의 Snowflake ID는 64비트이므로 손실이 발생합니다.

**해결**: JWT subject에 숫자 ID 대신 문자열 형태의 `memberId`(`member_304176937408598016`)를 사용하도록 변경하고, 모든 서비스에서 이 값으로 통일했습니다.

### Debezium 커넥터 재등록 자동화

**문제**: PostgreSQL 재시작 시 replication slot과 publication이 초기화되어, Kafka Connect 커넥터가 FAILED 상태가 되는 문제가 반복적으로 발생했습니다.

**해결**: `setup-connector.sh`에 커넥터 task 상태 확인 후 FAILED 시 자동 삭제 및 재등록 로직을 추가했습니다. `fix-connector.sh`로 수동 복구도 지원합니다.

### PostgreSQL max_connections

**문제**: Debezium CDC 커넥터들이 상시 연결을 유지하면서, 애플리케이션 HikariCP pool이 설정한 80개를 확보하지 못하고 70개에서 멈추는 현상이 발생했습니다.

**해결**: `max_connections`를 100에서 300으로 증가시키고, Debezium 커넥터 수를 고려하여 HikariCP pool 사이즈를 조정했습니다.

---

## 실행 방법

### 사전 요구사항

- Docker & Docker Compose
- Java 21
- Node.js 18+

### 인프라 실행

```bash
# 환경변수 설정
cp .env.example .env
# .env 파일에 POSTGRES_PASSWORD, NOMINATIM_PASSWORD 등 설정

# 인프라 컨테이너 실행
docker compose up -d

# Kafka Connect 커넥터 등록 확인
curl http://localhost:8083/connectors/order-outbox-connector/status
```

### 서비스 실행

각 서비스를 IntelliJ 또는 Gradle로 실행합니다.

```bash
# 예시: order-service
cd order
./gradlew bootRun
```

서비스별 포트: gateway(8000), member(9096), order(9100), pay(9094), seller(9098), rider(9095), restaurant(9097), location(9102)

### 프론트엔드 실행

```bash
cd delivery-front
npm install
npm run dev
```

브라우저에서 `http://localhost:5173` 접속

### 커넥터 복구 (PostgreSQL 재시작 후)

```bash
bash connect/fix-connector.sh
```

---

## 환경변수

| 변수 | 설명 |
|------|------|
| `POSTGRES_PASSWORD` | PostgreSQL, Redis 비밀번호 |
| `NOMINATIM_PASSWORD` | Nominatim DB 비밀번호 |
| `NOMINATIM_PBF_URL` | OSM PBF 파일 URL (기본값: 한국 전국) |
| `TOKEN_SECRET` | JWT 서명 키 |
| `TOSS_CLIENT_KEY` | Toss Payments 클라이언트 키 |
| `TOSS_SECRET_KEY` | Toss Payments 시크릿 키 |

---

## 모니터링

- **분산 추적**: Jaeger UI `http://localhost:16686`
- **부하 테스트**: nGrinder `http://localhost:8888`
- **Kafka Connect**: `http://localhost:8083/connectors`
