# ShopMall - E-Commerce MVP

데이터 분석 기능과 견고한 도메인 로직을 갖춘 이커머스 MVP 프로젝트

## 기술 스택

| 영역 | 기술 |
|---|---|
| **Backend** | Java 17, Spring Boot 3.4, JPA, Querydsl 5.1, Spring Security |
| **Database** | PostgreSQL 15 |
| **Cache/Lock** | Redis 7, Redisson (분산 락) |
| **Frontend** | React 19, TypeScript, Vite 7, Tailwind CSS v4 |
| **상태관리** | Zustand, TanStack Query v5 |
| **차트** | Recharts |
| **Infra** | Docker & Docker Compose |

---

## 프로젝트 구조

```
├── src/main/java/com/hsj/     # Spring Boot 백엔드
│   ├── config/                 # 설정 (Redis, Querydsl, Security, Async 등)
│   ├── controller/             # REST API 컨트롤러
│   ├── dto/                    # 요청/응답 DTO
│   ├── entity/                 # JPA 엔티티
│   │   └── enums/              # 상태 Enum (OrderStatus, PaymentStatus 등)
│   ├── exception/              # 예외 처리 (ErrorCode, GlobalExceptionHandler)
│   ├── filter/                 # JWT 인증 필터
│   ├── repository/             # JPA Repository + Querydsl 커스텀
│   │   └── custom/             # Querydsl 동적 쿼리 (상품검색, KPI통계)
│   ├── security/               # Spring Security + JWT
│   ├── service/                # 비즈니스 로직
│   │   ├── analytics/          # 대시보드, 실시간 통계
│   │   └── storage/            # 파일 업로드 (StorageService)
│   └── util/                   # 유틸리티
│
├── src/main/resources/         # 설정 파일
│   ├── application.yml         # 공통 설정
│   ├── application-dev.yml     # 개발 환경
│   └── application-prod.yml    # 운영 환경
│
├── frontend/                   # React 프론트엔드
│   └── src/
│       ├── api/                # API 호출 모듈
│       ├── components/         # 공통 컴포넌트 (Button, Input, Layout, Navbar)
│       ├── lib/                # Axios 인터셉터, QueryClient, 유틸
│       ├── pages/              # 페이지 컴포넌트
│       ├── stores/             # Zustand 스토어 (auth, cart)
│       └── types/              # TypeScript 타입 정의
│
├── docker-compose.yml          # 개발 환경 (PostgreSQL + Redis + PgAdmin)
├── docker-compose-prod.yml     # 운영 환경
├── Dockerfile                  # 운영용 멀티스테이지 빌드
├── Dockerfile.dev              # 개발용 (Hot Reload)
└── pom.xml                     # Maven 의존성
```

---

## 실행 방법

### 사전 요구사항

- **Docker** & **Docker Compose** (필수)
- **Java 17** (로컬 개발 시)
- **Node.js 18+** (프론트엔드 개발 시)

---

### 방법 1: Docker Compose (권장)

모든 인프라(PostgreSQL, Redis, Spring Boot)를 한번에 실행합니다.

```bash
# 1. 프로젝트 클론
git clone https://github.com/Hamseungjin/spring_boot_shopmall.git
cd spring_boot_shopmall

# 2. 개발 환경 실행
docker compose up -d

# 3. 프론트엔드 실행 (별도 터미널)
cd frontend
npm install
npm run dev
```

- **백엔드**: http://localhost:8081
- **프론트엔드**: http://localhost:5173
- **PgAdmin**: http://localhost:5050 (hsjking0403@naver.com / runner12)
- **PostgreSQL**: localhost:5432 (hsj / runner12 / mydb)
- **Redis**: localhost:6379

---

### 방법 2: 로컬 개발 (IDE)

PostgreSQL과 Redis만 Docker로 실행하고, Spring Boot는 IDE에서 실행합니다.

```bash
# 1. DB + Redis만 실행
docker compose up -d postgres redis

# 2. application-dev.yml에서 호스트를 localhost로 변경
#    url: jdbc:postgresql://localhost:5432/mydb
#    redis.host: localhost

# 3. Spring Boot 실행 (IDE 또는 터미널)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 4. 프론트엔드 실행
cd frontend
npm install
npm run dev
```

> **참고**: 로컬 실행 시 `application-dev.yml`의 `postgres` → `localhost`, `redis` → `localhost`로 변경해야 합니다.

---

### 방법 3: 운영 빌드

```bash
# 멀티스테이지 Docker 빌드 + 운영 실행
docker compose -f docker-compose-prod.yml up -d --build

# 프론트엔드 빌드
cd frontend
npm run build
# dist/ 디렉토리를 Nginx 등으로 서빙
```

---

## API 엔드포인트

### 인증

| Method | URL | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/auth/signup` | 회원가입 | 공개 |
| POST | `/api/auth/login` | 로그인 (JWT 발급) | 공개 |
| POST | `/api/auth/refresh` | 토큰 갱신 | 공개 |
| POST | `/api/auth/logout` | 로그아웃 | 인증 |
| GET | `/api/members/me` | 내 정보 조회 | 인증 |

### 상품

| Method | URL | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/products` | 상품 목록 (페이징) | 공개 |
| GET | `/api/products/search` | 상품 검색 (Querydsl 필터) | 공개 |
| GET | `/api/products/{id}` | 상품 상세 | 공개 |
| POST | `/api/products` | 상품 등록 | ADMIN |
| PUT | `/api/products/{id}` | 상품 수정 | ADMIN |
| DELETE | `/api/products/{id}` | 상품 삭제 | ADMIN |
| POST | `/api/products/{id}/image` | 이미지 업로드 | ADMIN |
| PATCH | `/api/products/{id}/stock` | 재고 추가 | ADMIN |

### 카테고리

| Method | URL | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/categories` | 카테고리 목록 | 공개 |
| GET | `/api/categories/tree` | 트리 구조 | 공개 |
| POST | `/api/categories` | 카테고리 등록 | ADMIN |

### 장바구니

| Method | URL | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/cart` | 장바구니 조회 | 인증 |
| POST | `/api/cart/items` | 아이템 추가 | 인증 |
| PATCH | `/api/cart/items/{productId}` | 수량 변경 | 인증 |
| DELETE | `/api/cart/items/{productId}` | 아이템 삭제 | 인증 |
| DELETE | `/api/cart` | 장바구니 비우기 | 인증 |

### 주문

| Method | URL | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/orders` | 주문 생성 (분산 락 재고 차감) | 인증 |
| GET | `/api/orders/my` | 내 주문 목록 | 인증 |
| GET | `/api/orders/{id}` | 주문 상세 | 인증 |
| POST | `/api/orders/{id}/cancel` | 주문 취소 (재고 복원) | 인증 |
| POST | `/api/orders/{id}/items/{itemId}/cancel` | 아이템 부분 취소 | 인증 |
| PATCH | `/api/orders/{id}/status` | 상태 변경 | ADMIN |
| GET | `/api/orders/{id}/history` | 상태 변경 이력 | 인증 |

### 결제

| Method | URL | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/payments` | 결제 (Idempotency-Key 필수) | 인증 |
| GET | `/api/payments/order/{orderId}` | 결제 조회 | 인증 |
| POST | `/api/payments/order/{orderId}/cancel` | 결제 취소 | 인증 |

### 이벤트 로깅

| Method | URL | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/events` | 이벤트 수집 (비동기) | 공개 |
| POST | `/api/events/batch` | 배치 이벤트 수집 | 공개 |
| GET | `/api/events/realtime/online` | 실시간 접속자 수 | 공개 |
| GET | `/api/events/realtime/product/{id}/views` | 상품 뷰 카운트 | 공개 |
| GET | `/api/events/export?from=&to=` | 로그 CSV 다운로드 | 인증 |

### 관리자 대시보드

| Method | URL | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/admin/dashboard?from=&to=` | 통합 대시보드 | ADMIN |
| GET | `/api/admin/dashboard/kpi` | KPI 카드 | ADMIN |
| GET | `/api/admin/dashboard/sales/daily` | 일별 매출 | ADMIN |
| GET | `/api/admin/dashboard/sales/category` | 카테고리별 매출 | ADMIN |

---

## 핵심 설계

### 재고 동시성 제어
- **Redisson 분산 락**: 상품별 `LOCK:STOCK:{productId}` 키로 락 획득 후 재고 차감
- **Optimistic Lock**: Product 엔티티에 `@Version` 적용
- **보상 트랜잭션**: 주문 생성 실패 시 이미 차감된 재고 자동 롤백

### 결제 멱등성
- **Idempotency-Key**: 동일 키로 중복 요청 시 기존 결과 반환 (HTTP 200)
- DB unique index + 서비스 레벨 검증 이중 보호

### 주문 상태 FSM
```
PENDING_PAYMENT → PAID → PREPARING → SHIPPED → DELIVERED
      ↓             ↓        ↓                      ↓
   CANCELLED    CANCELLED  CANCELLED          REFUND_REQUESTED → REFUNDED
```

### 가격 스냅샷
- OrderItem 생성 시 상품의 현재 가격/이름/이미지를 별도 컬럼에 저장
- 이후 상품 가격이 변동되어도 주문 시점의 가격이 보존됨

### JWT 인증
- **Access Token** (30분): API 요청 인증
- **Refresh Token** (7일): Redis에 저장, 토큰 갱신용
- **블랙리스트**: 로그아웃 시 Access Token의 잔여 TTL만큼 Redis에 등록

---

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | 프로필 (dev / prod) |
| `POSTGRES_DB` | `mydb` | 데이터베이스명 |
| `POSTGRES_USER` | `hsj` | DB 사용자 |
| `POSTGRES_PASSWORD` | `runner12` | DB 비밀번호 |
| `jwt.secret` | (base64) | JWT 서명 키 |
| `jwt.access-token-expiration` | `1800000` | Access Token 만료 (ms) |
| `jwt.refresh-token-expiration` | `604800000` | Refresh Token 만료 (ms) |
| `file.upload-dir` | `uploads` | 파일 업로드 디렉토리 |
