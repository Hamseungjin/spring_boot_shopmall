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


--
## 👤 일반 사용자 및 게스트 (USER / GUEST)

일반 사용자는 상품 조회, 장바구니, 주문 및 마이페이지 관련 기능을 이용할 수 있습니다.

### 🖼️ Frontend - UI Routes

| 구분 | 경로 (URL) | 설명 |
| --- | --- | --- |
| **공통/조회** | `/app` | 메인 홈 페이지 |
|  | `/app/login`, `/app/signup` | 로그인 및 회원가입 페이지 |
|  | `/app/products` | 상품 목록 페이지 |
|  | `/app/products/:id` | 상품 상세 페이지 |
|  | `/app/products/search` | 상품 검색 결과 페이지 |
|  | `/app/cart` | 장바구니 페이지 |
| **인증 필요** | `/app/checkout` | 주문서 작성 및 결제 페이지 |
|  | `/app/orders` | 나의 주문 목록 페이지 |
|  | `/app/orders/:id` | 주문 상세 내역 페이지 |
|  | `/app/mypage` | 마이페이지 (내 정보 조회) |

### ⚙️ Backend - API Endpoints

| 기능 | 엔드포인트 | Method | 접근 권한 |
| --- | --- | --- | --- |
| **인증/계정** | `/api/auth/signup`, `/api/auth/login` | `POST` | 전체 허용 |
|  | `/api/auth/logout`, `/api/auth/refresh` | `POST` | 인증 필요 |
|  | `/api/members/me` | `GET` | 인증 필요 |
| **상품/카테고리** | `/api/products/**` | `GET` | 전체 허용 |
|  | `/api/categories/**` | `GET` | 전체 허용 |
| **장바구니** | `/api/cart/**` | `ALL` | 인증 필요 |
| **주문/결제** | `/api/orders` (생성/조회/취소) | `POST`, `GET` | 인증 필요 |
|  | `/api/payments/**` | `POST`, `GET` | 인증 필요 |
| **이벤트/로그** | `/api/events`, `/api/events/batch` | `POST` | 전체 허용 (트래킹) |
|  | `/api/events/realtime/**` | `GET` | 전체 허용 |

---

## 🔐 관리자 (ADMIN)

관리자는 일반 사용자의 기능을 포함하며, 상품 관리 및 시스템 대시보드 접근 권한을 가집니다.

### 🖼️ Frontend - UI Routes

| 경로 (URL) | 설명 |
| --- | --- |
| `/app/admin` | 관리자 메인 대시보드 |
| `/app/admin/products` | 상품 관리 목록 |
| `/app/admin/products/new` | 새 상품 등록 페이지 |
| `/app/admin/products/:id/edit` | 상품 정보 수정 페이지 |
| `/app/admin/categories` | 카테고리 관리 페이지 |
| `/app/admin/orders` | 전체 주문 관리 목록 |

### ⚙️ Backend - API Endpoints

| 기능 | 엔드포인트 | Method | 접근 권한 |
| --- | --- | --- | --- |
| **대시보드** | `/api/admin/dashboard/**` | `GET` | **ADMIN** |
| **상품 관리** | `/api/products` (등록) | `POST` | **ADMIN** |
|  | `/api/products/{id}` (수정/삭제) | `PUT`, `DELETE` | **ADMIN** |
|  | `/api/products/{id}/image` (업로드) | `POST` | **ADMIN** |
|  | `/api/products/{id}/stock` (재고 수정) | `PATCH` | **ADMIN** |
| **카테고리 관리** | `/api/categories` (등록) | `POST` | **ADMIN** |
|  | `/api/categories/{id}` (수정/삭제) | `PUT`, `DELETE` | **ADMIN** |
| **주문 관리** | `/api/orders/{orderId}/status` | `PATCH` | **ADMIN** |
| **데이터 추출** | `/api/events/export` (로그 추출) | `GET` | **ADMIN** |

  ---


  특이사항:
   - Security 설정: SecurityConfig.java에서 /api/admin/** 경로는 hasRole('ADMIN')으로 엄격히 제한되어 있으며, 상품 및 카테고리의 CUD(생성, 수정, 삭제) 작업은
     Controller 레벨에서 @PreAuthorize("hasRole('ADMIN')") 어노테이션을 통해 보호되고 있습니다.
   - 파일 업로드: /uploads/** 경로는 상품 이미지 조회를 위해 전체 공개되어 있습니다.
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
