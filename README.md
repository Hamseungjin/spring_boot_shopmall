# E-Commerce MVP — Spring Boot ShopMall

데이터 분석 기능과 견고한 도메인 로직을 갖춘 이커머스 백엔드 MVP 프로젝트

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.4.4 |
| ORM | Spring Data JPA + Hibernate |
| 검색 | QueryDSL 5.1.0 |
| DB | PostgreSQL 15 |
| Cache / Lock | Redis 7 + Redisson (분산락) |
| 인증 | JWT (jjwt 0.12.6) |
| 빌드 | Maven |
| 컨테이너 | Docker / Docker Compose |
| 프론트엔드 | React 19, TypeScript, Vite 7, Tailwind CSS v4 |
| 상태관리 | Zustand, TanStack Query v5 |
| 차트 | Recharts |

---

## 프로젝트 구조

```
src/main/java/com/hsj/
├── controller/          # REST 컨트롤러
│   └── admin/           # 관리자 전용 컨트롤러
├── service/             # 비즈니스 로직
│   ├── analytics/       # 대시보드·실시간 분석
│   └── storage/         # 파일 업로드
├── entity/              # JPA 엔티티
│   └── enums/           # 상태 Enum (OrderStatus, PaymentStatus 등)
├── repository/          # Spring Data JPA + QueryDSL
│   └── custom/          # 동적 쿼리 (상품 검색, KPI 통계)
├── dto/                 # 요청/응답 DTO
├── security/            # JWT 필터·인증
├── interceptor/         # Rate Limiting 인터셉터
├── config/              # Spring 설정
├── exception/           # 전역 예외 처리
└── runner/              # 애플리케이션 시작 훅

frontend/src/
├── api/                 # API 호출 모듈
├── components/          # 공통 컴포넌트
├── pages/               # 페이지 컴포넌트
├── stores/              # Zustand 스토어 (auth, cart)
└── types/               # TypeScript 타입 정의
```

---

## 로컬 실행

### 사전 요구사항

- Docker & Docker Compose (필수)
- Java 17 (로컬 IDE 실행 시)
- Node.js 18+ (프론트엔드 개발 시)

### 방법 1 — Docker Compose (권장)

```bash
# 1. 환경 변수 파일 설정
cp .env.example .env
# .env 파일에서 JWT_SECRET, POSTGRES_PASSWORD 등 값 설정

# 2. 전체 실행
docker compose up -d

# 3. 프론트엔드 (별도 터미널)
cd frontend && npm install && npm run dev
```

| 서비스 | 접속 주소 |
|---|---|
| Spring Boot API | http://localhost:8081 |
| 프론트엔드 | http://localhost:5173 |
| pgAdmin | http://localhost:5050 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

### 방법 2 — 로컬 IDE

```bash
# DB + Redis만 Docker로 실행
docker compose up -d postgres redis

# application-dev.yml에서 호스트를 localhost로 변경 후
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 방법 3 — 운영 빌드

```bash
docker compose -f docker-compose-prod.yml up -d --build
```

### 테스트 실행

```bash
mvn test
# Redis / PostgreSQL 없이 Mockito 단위 테스트만 실행됩니다.
```

---

## 환경 변수

| 변수 | 기본값 (dev) | 필수 (prod) | 설명 |
|---|---|---|---|
| `JWT_SECRET` | base64 내장값 | Y | JWT 서명 키 (256-bit Base64) |
| `POSTGRES_USER` | `hsj` | Y | DB 사용자 |
| `POSTGRES_PASSWORD` | `runner12` | Y | DB 비밀번호 |
| `SPRING_DATASOURCE_URL` | postgres 컨테이너 | Y (prod) | DB 접속 URL |
| `REDIS_HOST` | `redis` | Y (prod) | Redis 호스트 |
| `REDIS_PORT` | `6379` | N | Redis 포트 |
| `PGADMIN_EMAIL` | — | N | pgAdmin 계정 이메일 |
| `PGADMIN_PASSWORD` | — | N | pgAdmin 계정 비밀번호 |

---

## 핵심 설계

### 재고 동시성 제어

- **Redisson 분산락**: 상품별 `LOCK:STOCK:{productId}` 키로 락 획득 후 재고 차감 (TTL: 3초)
- **Optimistic Lock**: `Product` 엔티티에 `@Version` 적용
- **보상 트랜잭션**: 주문 생성 실패 시 이미 차감된 재고 자동 롤백

### 결제 멱등성

- `idempotencyKey`로 중복 요청 시 기존 결제 결과 반환 (HTTP 200)
- DB unique index + 서비스 레벨 검증 이중 보호

### 주문 상태 FSM

```
PENDING_PAYMENT → PAID → PREPARING → SHIPPED → DELIVERED
       │           │         │                      │
       └───────────┴─────────┘                      │
                CANCELLED              REFUND_REQUESTED → REFUNDED
```

### 가격 스냅샷

`OrderItem` 생성 시 상품의 현재 가격·이름·이미지를 별도 컬럼에 저장.
이후 상품 정보가 변경되어도 주문 당시 정보가 보존됩니다.

### JWT 인증

- **AccessToken** (30분): API 요청 인증에 사용
- **RefreshToken** (7일): Redis에 저장, 토큰 갱신 전용
- **블랙리스트**: 로그아웃 시 AccessToken 잔여 TTL만큼 Redis에 등록

### 보안

| 항목 | 구현 |
|---|---|
| 비밀번호 저장 | BCrypt 해시 |
| Secrets 관리 | 환경 변수 (`.env`) |
| IDOR 방지 | 주문 소유권 검증 (`verifyOwnership`) |
| 브루트포스 방어 | Redis Rate Limiting — IP당 5회/60초 |
| 입력 검증 | Jakarta Validation (`@Valid`) |
| Soft Delete | `deleted` 플래그로 논리 삭제 |

### 기본 관리자 계정 (dev 환경)

`AdminCommandLineRunner`(`@Profile("dev")`)가 시작 시 ADMIN 계정이 없으면 자동 생성합니다.

| Email | Password |
|---|---|
| `hsj0403@admin.com` | `runner12!@` |

---

## 공통 응답 형식

```json
// 성공
{
  "success": true,
  "message": "처리 완료 메시지 (선택)",
  "data": { }
}

// 에러
{
  "success": false,
  "code": "에러 코드",
  "message": "에러 메시지"
}

// 페이지네이션 (data 내부)
{
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "last": false
}
```

---

## 권한 표기

| 표기 | 의미 |
|---|---|
| **공개** | 인증 불필요 |
| **인증** | 로그인 필요 (`Authorization: Bearer <accessToken>`) |
| **관리자** | `ROLE_ADMIN` 필요 |

---

# API 명세

---

## 1. 인증 — `/api/auth`

### POST `/api/auth/signup` — 회원가입 `공개`

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "Password1!",
  "name": "홍길동",
  "phone": "010-1234-5678",
  "address": "서울시 강남구"
}
```

| 필드 | 타입 | 필수 | 조건 |
|---|---|---|---|
| email | String | Y | 이메일 형식 |
| password | String | Y | 8~100자, 영문 + 숫자 포함 필수 |
| name | String | Y | 50자 이하 |
| phone | String | N | 20자 이하 |
| address | String | N | 255자 이하 |

**Response** `201 Created`

```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "CUSTOMER"
  }
}
```

---

### POST `/api/auth/login` — 로그인 `공개`

> IP당 60초 내 5회 실패 시 `429 Too Many Requests` 반환

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "Password1!"
}
```

**Response** `200 OK`

```json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "token": {
      "accessToken": "eyJhbGci...",
      "refreshToken": "eyJhbGci...",
      "tokenType": "Bearer"
    },
    "member": {
      "id": 1,
      "email": "user@example.com",
      "name": "홍길동",
      "role": "CUSTOMER"
    }
  }
}
```

---

### POST `/api/auth/refresh` — 토큰 갱신 `공개`

**Request Body**

```json
{ "refreshToken": "eyJhbGci..." }
```

**Response** `200 OK`

```json
{
  "success": true,
  "message": "토큰 갱신 성공",
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "tokenType": "Bearer"
  }
}
```

---

### POST `/api/auth/logout` — 로그아웃 `인증`

**Response** `200 OK`

```json
{ "success": true, "message": "로그아웃 되었습니다." }
```

---

## 2. 회원 — `/api/members`

### GET `/api/members/me` — 내 정보 조회 `인증`

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "phone": "010-1234-5678",
    "address": "서울시 강남구",
    "role": "CUSTOMER"
  }
}
```

---

## 3. 카테고리 — `/api/categories`

| 엔드포인트 | Method | 권한 | 설명 |
|---|---|---|---|
| `/api/categories` | GET | 공개 | 카테고리 목록 (평면) |
| `/api/categories/tree` | GET | 공개 | 카테고리 계층 트리 |
| `/api/categories/{id}` | GET | 공개 | 단건 조회 |
| `/api/categories` | POST | 관리자 | 카테고리 등록 |
| `/api/categories/{id}` | PUT | 관리자 | 카테고리 수정 |
| `/api/categories/{id}` | DELETE | 관리자 | 카테고리 삭제 (소프트) |

### GET `/api/categories` — 목록 조회 `공개`

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    { "id": 1, "name": "전자기기", "description": "...", "sortOrder": 1, "parentId": null },
    { "id": 2, "name": "노트북",  "description": "...", "sortOrder": 1, "parentId": 1 }
  ]
}
```

### GET `/api/categories/tree` — 트리 조회 `공개`

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "전자기기",
      "children": [
        { "id": 2, "name": "노트북", "children": [] }
      ]
    }
  ]
}
```

### POST `/api/categories` — 등록 `관리자`

**Request Body**

```json
{
  "name": "스마트폰",
  "description": "모바일 기기",
  "parentId": 1,
  "sortOrder": 2
}
```

**Response** `201 Created`

---

## 4. 상품 — `/api/products`

| 엔드포인트 | Method | 권한 | 설명 |
|---|---|---|---|
| `/api/products` | GET | 공개 | 상품 목록 (페이지네이션) |
| `/api/products/search` | GET | 공개 | 상품 검색 (필터·정렬) |
| `/api/products/{id}` | GET | 공개 | 상품 단건 조회 |
| `/api/products` | POST | 관리자 | 상품 등록 |
| `/api/products/{id}` | PUT | 관리자 | 상품 수정 |
| `/api/products/{id}` | DELETE | 관리자 | 상품 삭제 (소프트) |
| `/api/products/{id}/image` | POST | 관리자 | 이미지 업로드 (multipart) |
| `/api/products/{id}/stock` | PATCH | 관리자 | 재고 추가 |

### GET `/api/products` — 목록 조회 `공개`

| Query Param | 기본값 | 설명 |
|---|---|---|
| page | 0 | 페이지 번호 |
| size | 20 | 페이지 크기 |

### GET `/api/products/search` — 검색 `공개`

| Query Param | 타입 | 설명 |
|---|---|---|
| keyword | String | 상품명 키워드 |
| categoryId | Long | 카테고리 필터 |
| minPrice | BigDecimal | 최소 가격 |
| maxPrice | BigDecimal | 최대 가격 |
| inStock | Boolean | 재고 있는 상품만 |
| sortBy | String | 정렬 기준 (기본: `createdAt`) |
| sortDirection | String | `asc` / `desc` (기본: `desc`) |
| page | Integer | 0부터 시작 |
| size | Integer | 기본 20 |

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "맥북 프로 14인치",
        "description": "Apple Silicon M3",
        "price": 2990000,
        "stockQuantity": 15,
        "imageUrl": "/uploads/macbook.jpg",
        "categoryId": 2,
        "categoryName": "노트북"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

### POST `/api/products` — 상품 등록 `관리자`

**Request Body**

```json
{
  "name": "맥북 프로 14인치",
  "description": "Apple Silicon M3 Pro",
  "price": 2990000,
  "stockQuantity": 10,
  "imageUrl": "/uploads/macbook.jpg",
  "categoryId": 2
}
```

| 필드 | 타입 | 필수 | 조건 |
|---|---|---|---|
| name | String | Y | — |
| price | BigDecimal | Y | 양수 |
| stockQuantity | int | N | 0 이상, 기본 0 |
| categoryId | Long | N | — |

**Response** `201 Created`

### POST `/api/products/{id}/image` — 이미지 업로드 `관리자`

- **Content-Type**: `multipart/form-data`
- Form field: `file` (이미지 파일, 최대 10MB)

### PATCH `/api/products/{id}/stock` — 재고 추가 `관리자`

| Query Param | 타입 | 설명 |
|---|---|---|
| quantity | int | 추가할 재고 수량 |

---

## 5. 장바구니 — `/api/cart`

> 모든 엔드포인트 `인증` 필요

| 엔드포인트 | Method | 설명 |
|---|---|---|
| `/api/cart` | GET | 장바구니 조회 |
| `/api/cart/items` | POST | 상품 추가 |
| `/api/cart/items/{productId}` | PATCH | 수량 변경 |
| `/api/cart/items/{productId}` | DELETE | 상품 제거 |
| `/api/cart` | DELETE | 장바구니 전체 비우기 |

### GET `/api/cart` — 조회

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "cartId": 1,
    "items": [
      {
        "productId": 1,
        "productName": "맥북 프로 14인치",
        "price": 2990000,
        "quantity": 2,
        "subtotal": 5980000
      }
    ],
    "totalAmount": 5980000
  }
}
```

### POST `/api/cart/items` — 상품 추가

**Request Body**

```json
{
  "productId": 1,
  "quantity": 2
}
```

| 필드 | 조건 |
|---|---|
| productId | 필수 |
| quantity | 1~100 (기본: 1) |

> 이미 담긴 상품이면 수량이 합산됩니다.

### PATCH `/api/cart/items/{productId}` — 수량 변경

| Query Param | 타입 |
|---|---|
| quantity | int |

---

## 6. 주문 — `/api/orders`

> 모든 엔드포인트 `인증` 필요
> CUSTOMER는 **본인 주문만** 조회·취소 가능 (IDOR 보호)

| 엔드포인트 | Method | 권한 | 설명 |
|---|---|---|---|
| `/api/orders` | POST | 인증 | 주문 생성 |
| `/api/orders/my` | GET | 인증 | 내 주문 목록 |
| `/api/orders/{orderId}` | GET | 인증 | 주문 단건 조회 |
| `/api/orders/number/{orderNumber}` | GET | 인증 | 주문번호로 조회 |
| `/api/orders/{orderId}/cancel` | POST | 인증 | 주문 전체 취소 |
| `/api/orders/{orderId}/items/{itemId}/cancel` | POST | 인증 | 주문 상품 부분 취소 |
| `/api/orders/{orderId}/history` | GET | 인증 | 주문 상태 이력 |
| `/api/orders/{orderId}/status` | PATCH | **관리자** | 주문 상태 변경 |

### POST `/api/orders` — 주문 생성

**Request Body**

```json
{
  "shippingAddress": "서울시 강남구 테헤란로 123",
  "receiverName": "홍길동",
  "receiverPhone": "010-1234-5678",
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ]
}
```

| 필드 | 조건 |
|---|---|
| shippingAddress | 필수 |
| receiverName | 필수 |
| receiverPhone | 필수 |
| items[].productId | 필수 |
| items[].quantity | 1~100, 양수 |

**Response** `201 Created`

```json
{
  "success": true,
  "message": "주문이 생성되었습니다.",
  "data": {
    "orderId": 10,
    "memberId": 1,
    "orderNumber": "ORD-a1b2c3d4-...",
    "status": "PENDING_PAYMENT",
    "totalAmount": 5980000,
    "shippingAddress": "서울시 강남구 테헤란로 123",
    "receiverName": "홍길동",
    "receiverPhone": "010-1234-5678",
    "items": [
      {
        "orderItemId": 20,
        "productId": 1,
        "snapshotProductName": "맥북 프로 14인치",
        "snapshotPrice": 2990000,
        "quantity": 2,
        "subtotal": 5980000,
        "status": "PENDING_PAYMENT"
      }
    ],
    "createdAt": "2025-01-01T10:00:00"
  }
}
```

### GET `/api/orders/my` — 내 주문 목록

| Query Param | 기본값 |
|---|---|
| page | 0 |
| size | 10 |

### POST `/api/orders/{orderId}/cancel` — 주문 취소

> 취소 가능 상태: `PENDING_PAYMENT`, `PAID`, `PREPARING`
> `PAID` / `PREPARING` 취소 시 **결제 자동 취소** 처리

**Request Body (선택)**

```json
{ "reason": "단순 변심" }
```

### POST `/api/orders/{orderId}/items/{itemId}/cancel` — 부분 취소

**Request Body (선택)**

```json
{ "reason": "상품 불량" }
```

### GET `/api/orders/{orderId}/history` — 상태 이력

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "previousStatus": "PENDING_PAYMENT",
      "newStatus": "PAID",
      "reason": "결제 완료",
      "changedBy": "admin@admin.com",
      "changedAt": "2025-01-01T10:05:00"
    }
  ]
}
```

### PATCH `/api/orders/{orderId}/status` — 상태 변경 `관리자`

**Request Body**

```json
{
  "status": "PREPARING",
  "reason": "상품 준비 시작"
}
```

**유효한 상태 전이**

| 현재 상태 | 전이 가능 상태 |
|---|---|
| PENDING_PAYMENT | PAID, CANCELLED |
| PAID | PREPARING, CANCELLED, REFUND_REQUESTED |
| PREPARING | SHIPPED, CANCELLED |
| SHIPPED | DELIVERED |
| DELIVERED | REFUND_REQUESTED |
| REFUND_REQUESTED | REFUNDED |

---

## 7. 결제 — `/api/payments`

| 엔드포인트 | Method | 권한 | 설명 |
|---|---|---|---|
| `/api/payments` | POST | 인증 | 결제 처리 |
| `/api/payments/order/{orderId}` | GET | 공개 | 결제 정보 조회 |
| `/api/payments/order/{orderId}/cancel` | POST | 인증 | 결제 취소 |

### POST `/api/payments` — 결제 처리

> `idempotencyKey`가 동일하면 기존 결제 결과를 반환합니다 (중복 방지).
> 결제 처리 후 주문 상태가 `PENDING_PAYMENT` → `PAID`로 자동 전환됩니다.

**Request Body**

```json
{
  "orderId": 10,
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "paymentMethod": "CREDIT_CARD"
}
```

| 필드 | 필수 | 설명 |
|---|---|---|
| orderId | Y | 결제할 주문 ID |
| idempotencyKey | Y | 클라이언트 생성 고유 키 (UUID 권장) |
| paymentMethod | N | 결제 수단 |

**Response** `200 OK`

```json
{
  "success": true,
  "message": "결제가 완료되었습니다.",
  "data": {
    "paymentId": 5,
    "orderId": 10,
    "amount": 5980000,
    "paymentMethod": "CREDIT_CARD",
    "status": "COMPLETED",
    "idempotencyKey": "550e8400-...",
    "createdAt": "2025-01-01T10:10:00"
  }
}
```

### POST `/api/payments/order/{orderId}/cancel` — 결제 취소

> `COMPLETED` 상태의 결제만 취소 가능합니다.

---

## 8. 리뷰 — `/api/reviews`

| 엔드포인트 | Method | 권한 | 설명 |
|---|---|---|---|
| `/api/reviews` | POST | 인증 | 리뷰 작성 |
| `/api/reviews/products/{productId}` | GET | 공개 | 상품별 리뷰 목록 |
| `/api/reviews/{reviewId}` | PATCH | 인증 | 리뷰 수정 (본인) |
| `/api/reviews/{reviewId}` | DELETE | 인증 | 리뷰 삭제 (본인 또는 관리자) |

### POST `/api/reviews` — 리뷰 작성 `인증`

**작성 조건**
1. 주문 상태가 `DELIVERED` (배송 완료)
2. 본인 주문에 포함된 상품
3. 동일 주문+상품 조합으로 미작성

**Request Body**

```json
{
  "productId": 1,
  "orderId": 10,
  "rating": 5,
  "content": "정말 좋은 제품입니다!"
}
```

| 필드 | 조건 |
|---|---|
| productId | 필수 |
| orderId | 필수 |
| rating | 1~5 정수, 필수 |
| content | 선택 |

**Response** `201 Created`

```json
{
  "success": true,
  "message": "리뷰가 작성되었습니다.",
  "data": {
    "reviewId": 3,
    "productId": 1,
    "memberId": 1,
    "memberName": "홍길동",
    "rating": 5,
    "content": "정말 좋은 제품입니다!",
    "createdAt": "2025-01-10T14:00:00"
  }
}
```

### GET `/api/reviews/products/{productId}` — 상품별 리뷰 목록 `공개`

| Query Param | 기본값 |
|---|---|
| page | 0 |
| size | 10 |

### PATCH `/api/reviews/{reviewId}` — 리뷰 수정 `인증`

**Request Body**

```json
{
  "rating": 4,
  "content": "조금 아쉬운 점도 있네요."
}
```

| 필드 | 조건 |
|---|---|
| rating | 1~5, 필수 |
| content | 선택 |

### DELETE `/api/reviews/{reviewId}` — 리뷰 삭제 `인증`

> CUSTOMER: 본인 리뷰만 삭제 가능
> ADMIN: 모든 리뷰 삭제 가능

---

## 9. 이벤트 로그 — `/api/events`

프론트엔드 행동 데이터를 수집하는 Analytics 엔드포인트입니다.

| 엔드포인트 | Method | 권한 | 설명 |
|---|---|---|---|
| `/api/events` | POST | 공개 | 단건 이벤트 기록 |
| `/api/events/batch` | POST | 공개 | 이벤트 배치 기록 |
| `/api/events/realtime/online` | GET | 공개 | 실시간 접속자 수 |
| `/api/events/realtime/product/{productId}/views` | GET | 공개 | 상품 실시간 조회수 |
| `/api/events/export` | GET | 공개 | 이벤트 로그 CSV 내보내기 |

### POST `/api/events` — 이벤트 기록

**Request Body**

```json
{
  "eventType": "PRODUCT_VIEW",
  "sessionId": "sess-abc123",
  "pageUrl": "/products/1",
  "targetId": 1,
  "targetType": "PRODUCT",
  "durationMs": 3500
}
```

| eventType | 설명 |
|---|---|
| `PAGE_VIEW` | 페이지 조회 |
| `PRODUCT_VIEW` | 상품 상세 조회 |
| `PRODUCT_CLICK` | 상품 클릭 |
| `CART_ADD` | 장바구니 추가 |
| `ORDER_START` | 주문 시작 |
| `SEARCH` | 검색 |
| `CLICK` | 일반 클릭 |
| `CUSTOM` | 커스텀 이벤트 |

### GET `/api/events/export` — CSV 내보내기

| Query Param | 타입 | 필수 | 예시 |
|---|---|---|---|
| from | LocalDate | Y | `2025-01-01` |
| to | LocalDate | Y | `2025-01-31` |
| eventType | EventType | N | `PRODUCT_VIEW` |

**Response**: `text/csv` 파일 다운로드

---

## 10. 관리자 대시보드 — `/api/admin/dashboard`

> 모든 엔드포인트 `관리자` 전용

공통 Query Param: `from` (LocalDate, 필수), `to` (LocalDate, 필수)
예시: `?from=2025-01-01&to=2025-01-31`

| 엔드포인트 | Method | 설명 |
|---|---|---|
| `/api/admin/dashboard` | GET | 종합 대시보드 |
| `/api/admin/dashboard/kpi` | GET | KPI 요약 지표 |
| `/api/admin/dashboard/sales/daily` | GET | 일별 매출 추이 |
| `/api/admin/dashboard/sales/category` | GET | 카테고리별 매출 |

### GET `/api/admin/dashboard` — 종합 대시보드

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "totalRevenue": 15000000,
    "totalOrders": 120,
    "totalMembers": 500,
    "averageOrderValue": 125000,
    "dailySales": [ ],
    "categorySales": [ ]
  }
}
```

### GET `/api/admin/dashboard/kpi` — KPI 요약

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "totalRevenue": 15000000,
    "totalOrders": 120,
    "cancelledOrders": 5,
    "refundedAmount": 300000
  }
}
```

### GET `/api/admin/dashboard/sales/daily` — 일별 매출

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    { "date": "2025-01-01", "revenue": 500000, "orderCount": 4 },
    { "date": "2025-01-02", "revenue": 1200000, "orderCount": 9 }
  ]
}
```

### GET `/api/admin/dashboard/sales/category` — 카테고리별 매출

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    { "categoryName": "노트북", "revenue": 8000000, "orderCount": 50 },
    { "categoryName": "스마트폰", "revenue": 5000000, "orderCount": 40 }
  ]
}
```

---

## 에러 코드

| 코드 | HTTP | 설명 |
|---|---|---|
| `M001` | 404 | 회원을 찾을 수 없음 |
| `M002` | 409 | 이미 존재하는 이메일 |
| `P001` | 404 | 상품을 찾을 수 없음 |
| `P002` | 409 | 재고 부족 |
| `C001` | 404 | 카테고리를 찾을 수 없음 |
| `C002` | 409 | 이미 존재하는 카테고리명 |
| `O001` | 404 | 주문을 찾을 수 없음 |
| `O002` | 400 | 취소 불가 상태 |
| `O003` | 404 | 주문 항목을 찾을 수 없음 |
| `O004` | 403 | 주문 접근 권한 없음 (IDOR) |
| `PAY001` | 404 | 결제 정보 없음 |
| `PAY002` | 400 | 결제 불가 상태 |
| `PAY003` | 409 | 이미 처리 중인 결제 |
| `R001` | 404 | 리뷰를 찾을 수 없음 |
| `R002` | 409 | 중복 리뷰 |
| `R003` | 400 | 배송 완료 주문이 아님 |
| `R004` | 400 | 주문에 해당 상품 없음 |
| `AUTH001` | 401 | 유효하지 않은 토큰 |
| `AUTH002` | 401 | 만료된 토큰 |
| `AUTH003` | 403 | 접근 권한 없음 |
| `AUTH004` | 401 | 이메일/비밀번호 불일치 |
| `AUTH005` | 404 | 회원을 찾을 수 없음 |
| `AUTH006` | 429 | 로그인 시도 횟수 초과 |

---

## 프론트엔드 라우트

### 일반 사용자

| 경로 | 설명 | 인증 필요 |
|---|---|---|
| `/app` | 메인 홈 | — |
| `/app/login`, `/app/signup` | 로그인/회원가입 | — |
| `/app/products` | 상품 목록 | — |
| `/app/products/:id` | 상품 상세 | — |
| `/app/products/search` | 상품 검색 결과 | — |
| `/app/cart` | 장바구니 | — |
| `/app/checkout` | 주문서 작성 및 결제 | Y |
| `/app/orders` | 내 주문 목록 | Y |
| `/app/orders/:id` | 주문 상세 | Y |
| `/app/mypage` | 마이페이지 | Y |

### 관리자

| 경로 | 설명 |
|---|---|
| `/app/admin` | 관리자 대시보드 |
| `/app/admin/products` | 상품 관리 목록 |
| `/app/admin/products/new` | 상품 등록 |
| `/app/admin/products/:id/edit` | 상품 수정 |
| `/app/admin/categories` | 카테고리 관리 |
| `/app/admin/orders` | 전체 주문 관리 |
