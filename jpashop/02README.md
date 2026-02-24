# 도메인 분석 설계

> 요구사항 분석부터 연관관계 매핑까지 — 실전 쇼핑몰 도메인을 설계하는 법

## 📚 강의 출처

**인프런 - 김영한의 "실전! 스프링 부트와 JPA 활용 1 - 웹 애플리케이션 개발"**

이 문서는 강의의 두 번째 챕터인 "도메인 분석 설계"를 학습하며 작성한 노트입니다. 프로젝트 환경이 준비된 후, 본격적으로 코드를 작성하기 전에 **"무엇을 만들지"와 "어떻게 구조를 잡을지"** 를 설계하는 단계입니다. 엔티티 클래스 개발 이전까지의 분석·설계 과정을 다룹니다.

**이전 챕터(01)와 이 챕터(02)의 관계:**

| 항목 | 01 - 프로젝트 환경설정 | 02 - 도메인 분석 설계 |
|------|----------------------|---------------------|
| **목표** | 개발 환경 구성 | 개발 전 설계도 작성 |
| **핵심 질문** | "어떻게 실행하는가?" | "무엇을 어떻게 만들 것인가?" |
| **결과물** | 동작하는 빈 프로젝트 | 도메인 모델, 테이블 설계, 연관관계 분석 |
| **다음 단계** | 도메인 설계 | 엔티티 클래스 구현 |

---

## 🎯 학습 목표

이 문서를 통해 다음을 이해합니다:

1. **요구사항 분석**
   - 쇼핑몰 기능을 회원/상품/주문으로 분류
   - 각 기능의 도메인 요소(엔티티 후보) 도출

2. **도메인 모델 설계**
   - 클래스 다이어그램(UML)으로 객체 간 관계 표현
   - 상속 관계(Item → Book/Album/Movie) 설계

3. **테이블 설계 (ERD)**
   - 객체 모델을 관계형 DB 테이블로 변환
   - 외래 키(FK) 위치와 조인 테이블 결정

4. **연관관계 매핑 분석**
   - 연관관계 주인(Owner) 개념 이해
   - 각 관계에서 어느 쪽이 FK를 갖는지 분석

5. **설계 시 핵심 주의점**
   - `@ManyToMany`를 지양해야 하는 이유
   - 값 타입(`@Embeddable`)과 엔티티 타입의 차이
   - 단방향 vs 양방향 연관관계 선택 기준

---

## 🗺️ 학습 로드맵

이 문서는 **설계 4단계**로 구성되어 있습니다.

```
1. 요구사항 분석
   - 회원/상품/주문 기능 나열
   - 도메인 요소(엔티티 후보) 추출
   ↓
2. 도메인 모델 설계
   - 클래스 다이어그램(객체 관점)
   - 상속 구조, 값 타입 정의
   ↓
3. 테이블 설계 (ERD)
   - 객체 → 테이블 변환
   - FK 위치, 조인 테이블 결정
   ↓
4. 연관관계 매핑 분석
   - 연관관계 주인 결정
   - 단방향/양방향 선택
   - 각 매핑 어노테이션 정리
```

**왜 이 순서인가?**

- **요구사항 먼저**: 무엇을 만들지 명확해야 설계가 가능합니다.
- **도메인 모델 → 테이블 순서**: 객체 지향적으로 먼저 설계한 뒤, DB 테이블로 변환합니다.
- **테이블 설계 후 연관관계 분석**: FK 위치가 결정되어야 JPA 연관관계 주인을 확정할 수 있습니다.

---

## 📖 목차

1. [요구사항 분석](#1-요구사항-분석)
2. [도메인 모델 설계](#2-도메인-모델-설계)
3. [테이블 설계 (ERD)](#3-테이블-설계-erd)
4. [연관관계 매핑 분석](#4-연관관계-매핑-분석)
5. [설계 핵심 주의점 (Best Practice)](#5-설계-핵심-주의점-best-practice)
6. [부록](#6-부록)

---

## 1. 요구사항 분석

### 1.1 기능 목록

이 프로젝트는 간단한 **온라인 쇼핑몰(jpashop)**입니다. 실무 수준의 복잡한 도메인을 JPA로 구현하는 것이 목표입니다.

#### 회원 기능

| 기능 | 설명 |
|------|------|
| **회원 등록** | 이름과 주소를 입력해 회원 가입 |
| **회원 조회** | 전체 회원 목록을 조회 |

#### 상품 기능

| 기능 | 설명 |
|------|------|
| **상품 등록** | 상품(도서/음반/영화) 정보를 등록 |
| **상품 수정** | 등록된 상품의 정보를 수정 |
| **상품 조회** | 전체 상품 목록을 조회 |

#### 주문 기능

| 기능 | 설명 |
|------|------|
| **상품 주문** | 회원이 원하는 상품을 선택해 주문 |
| **주문 내역 조회** | 회원별 주문 이력을 조회 |
| **주문 취소** | 배송 전 주문을 취소 |

#### 기타 요구사항

| 항목 | 설명 |
|------|------|
| **재고 관리** | 상품은 재고 수량을 관리하며, 주문 시 재고가 감소 |
| **상품 분류** | 상품은 도서(Book), 음반(Album), 영화(Movie)로 구분 |
| **카테고리** | 상품에 카테고리를 지정 가능 |
| **배송 정보** | 주문 시 배송 정보(주소, 상태)를 함께 저장 |

### 1.2 도메인 요소 추출 (엔티티 후보)

요구사항에서 명사(주요 개념)를 추출하면 엔티티 후보가 됩니다.

```
요구사항 → 도메인 요소

"회원이 상품을 주문한다"
  → Member (회원)
  → Order (주문)
  → Item (상품)

"주문에는 여러 상품이 포함된다"
  → OrderItem (주문 상품, 중간 엔티티)

"주문 시 배송 정보를 저장한다"
  → Delivery (배송)

"상품은 카테고리에 속한다"
  → Category (카테고리)

"회원은 주소를 가진다"
  → Address (주소 - 값 타입 Embeddable)

"상품은 도서/음반/영화로 구분된다"
  → Book, Album, Movie (Item 상속)
```

---

## 2. 도메인 모델 설계

### 2.1 도메인 모델 다이어그램 (클래스 다이어그램)

객체 관점에서 엔티티 간의 관계를 표현합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                      도메인 모델 다이어그램                           │
└─────────────────────────────────────────────────────────────────────┘

                    ┌──────────────┐
                    │    Member    │
                    │──────────────│
                    │ id: Long     │
                    │ name: String │
                    │ address:     │◄─── @Embedded
                    │   Address    │     (값 타입)
                    │ orders:      │
                    │  List<Order> │
                    └──────┬───────┘
                           │ 1
                           │ (양방향)
                           │ N
                    ┌──────▼───────┐         ┌─────────────────────┐
                    │    Order     │   1 : 1  │      Delivery       │
                    │──────────────│──────────│─────────────────────│
                    │ id: Long     │          │ id: Long            │
                    │ member:      │          │ address: Address    │
                    │  Member      │          │ status: DeliveryStatus│
                    │ orderItems:  │          └─────────────────────┘
                    │  List<OI>    │
                    │ delivery:    │
                    │  Delivery    │
                    │ status:      │
                    │  OrderStatus │
                    │ orderDate:   │
                    │  LocalDateTime│
                    └──────┬───────┘
                           │ 1
                           │ (양방향)
                           │ N
                    ┌──────▼───────┐
                    │  OrderItem   │
                    │──────────────│
                    │ id: Long     │
                    │ item: Item   │◄──── N : 1
                    │ order: Order │
                    │ orderPrice:  │
                    │  int         │
                    │ count: int   │
                    └──────────────┘

                    ┌──────────────┐         ┌──────────────────┐
                    │    Item      │ M  :  N  │    Category      │
                    │──────────────│──────────│──────────────────│
                    │ id: Long     │          │ id: Long         │
                    │ name: String │          │ name: String     │
                    │ price: int   │          │ items:           │
                    │ stockQty: int│          │  List<Item>      │
                    │ categories:  │          │ parent:          │
                    │  List<Cat>   │          │  Category        │◄─ 자기참조
                    └──────┬───────┘          │ children:        │
                           │                  │  List<Category>  │
              ┌────────────┼────────────┐     └──────────────────┘
              │            │            │
         ┌────▼───┐  ┌─────▼──┐  ┌─────▼──┐
         │  Book  │  │ Album  │  │ Movie  │
         │────────│  │────────│  │────────│
         │ author │  │ artist │  │director│
         │ isbn   │  │ etc    │  │ actor  │
         └────────┘  └────────┘  └────────┘
           (상속)      (상속)      (상속)
```

### 2.2 연관관계 요약

| 관계 | 형태 | 방향 | 설명 |
|------|------|------|------|
| Member ↔ Order | 1:N | 양방향 | 한 회원이 여러 주문 |
| Order ↔ OrderItem | 1:N | 양방향 | 한 주문에 여러 주문 상품 |
| OrderItem → Item | N:1 | 단방향 | 여러 주문상품이 하나의 상품 참조 |
| Order ↔ Delivery | 1:1 | 양방향 | 주문 하나에 배송 하나 |
| Item ↔ Category | M:N | 양방향 | 상품-카테고리 다대다 (중간 테이블) |
| Category → Category | 1:N | 자기참조 | 카테고리 부모-자식 트리 |
| Item → Book/Album/Movie | 상속 | - | 단일 테이블 전략(기본) |

### 2.3 Address - 값 타입 (Embeddable)

`Address`는 엔티티가 아닌 **값 타입(Value Object)**입니다.

```
Address (값 타입)
├── city: String     (도시)
├── street: String   (거리)
└── zipcode: String  (우편번호)
```

**엔티티와 값 타입의 차이:**

| 구분 | 엔티티 (Entity) | 값 타입 (Value Object) |
|------|----------------|----------------------|
| **식별자** | `@Id`로 고유 식별 | 식별자 없음 |
| **생명주기** | 독립적 | 소유 엔티티에 종속 |
| **공유** | 여러 곳에서 참조 가능 | 공유하면 안 됨 (불변으로 사용) |
| **JPA 어노테이션** | `@Entity` | `@Embeddable` |
| **예시** | Member, Order | Address, Money |

`Address`는 `Member`와 `Delivery`가 각각 가집니다. 같은 클래스를 재사용하지만, 각각 별도의 값으로 저장됩니다.

---

## 3. 테이블 설계 (ERD)

### 3.1 ERD 개요

도메인 모델(객체)을 관계형 DB 테이블로 변환합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                           ERD                                        │
└─────────────────────────────────────────────────────────────────────┘

┌───────────────────┐        ┌──────────────────────────────────┐
│       MEMBER      │        │               ORDERS             │
│───────────────────│        │──────────────────────────────────│
│ MEMBER_ID (PK)    │◄───────│ ORDER_ID (PK)                    │
│ NAME              │ 1 : N  │ MEMBER_ID (FK) ─────────────────►│
│ CITY              │        │ DELIVERY_ID (FK) ────────────────┐│
│ STREET            │        │ STATUS                           ││
│ ZIPCODE           │        │ ORDER_DATE                       ││
└───────────────────┘        └──────────────────────────────────┘│
                                       │ 1                        │
                                       │                          │
                                       │ N                        ▼
                             ┌─────────▼────────────┐  ┌──────────────────┐
                             │      ORDER_ITEM       │  │     DELIVERY     │
                             │──────────────────────-│  │──────────────────│
                             │ ORDER_ITEM_ID (PK)    │  │ DELIVERY_ID (PK) │
                             │ ORDER_ID (FK)         │  │ CITY             │
                             │ ITEM_ID (FK)  ────────┐  │ STREET           │
                             │ ORDER_PRICE           ││  │ ZIPCODE          │
                             │ COUNT                 ││  │ STATUS           │
                             └───────────────────────┘│  └──────────────────┘
                                                      │
                                                      ▼ N : 1
                             ┌─────────────────────────────────────────────┐
                             │                    ITEM                      │
                             │─────────────────────────────────────────────│
                             │ ITEM_ID (PK)                                 │
                             │ NAME                                         │
                             │ PRICE                                        │
                             │ STOCK_QUANTITY                               │
                             │ DTYPE (구분 컬럼: 'B'/'A'/'M')              │
                             │ AUTHOR / ISBN          (Book 컬럼)           │
                             │ ARTIST / ETC           (Album 컬럼)          │
                             │ DIRECTOR / ACTOR       (Movie 컬럼)          │
                             └──────────────────────────────────────────────┘
                                            │ M : N
                                            │
                             ┌──────────────▼──────────────┐
                             │        CATEGORY_ITEM         │
                             │─────────────────────────────│
                             │ CATEGORY_ID (PK, FK)         │◄──┐
                             │ ITEM_ID     (PK, FK)         │   │
                             └─────────────────────────────┘   │
                                                                │
                             ┌──────────────────────────────────┘
                             │          CATEGORY
                             │──────────────────────────────────
                             │ CATEGORY_ID (PK)
                             │ NAME
                             │ PARENT_ID (FK, 자기참조)  ─────┐
                             └──────────────────────────────────┘
                                          ▲                    │
                                          └────────────────────┘
                                            (PARENT_ID → CATEGORY_ID)
```

### 3.2 테이블별 컬럼 상세

#### MEMBER 테이블

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| MEMBER_ID | BIGINT | PK, NOT NULL | 회원 식별자 |
| NAME | VARCHAR(255) | NOT NULL | 회원 이름 |
| CITY | VARCHAR(255) | - | 주소 - 도시 (`@Embedded Address`) |
| STREET | VARCHAR(255) | - | 주소 - 거리 (`@Embedded Address`) |
| ZIPCODE | VARCHAR(255) | - | 주소 - 우편번호 (`@Embedded Address`) |

#### ORDERS 테이블

> **왜 ORDER가 아닌 ORDERS인가?**
> `ORDER`는 SQL 예약어이므로 테이블명 충돌을 피하기 위해 `ORDERS`를 사용합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| ORDER_ID | BIGINT | PK, NOT NULL | 주문 식별자 |
| MEMBER_ID | BIGINT | FK, NOT NULL | 회원 외래 키 |
| DELIVERY_ID | BIGINT | FK | 배송 외래 키 |
| STATUS | VARCHAR(255) | - | 주문 상태 (ORDER / CANCEL) |
| ORDER_DATE | TIMESTAMP | - | 주문 일시 |

#### ORDER_ITEM 테이블

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| ORDER_ITEM_ID | BIGINT | PK, NOT NULL | 주문 상품 식별자 |
| ORDER_ID | BIGINT | FK, NOT NULL | 주문 외래 키 |
| ITEM_ID | BIGINT | FK, NOT NULL | 상품 외래 키 |
| ORDER_PRICE | INTEGER | - | 주문 당시 가격 |
| COUNT | INTEGER | - | 주문 수량 |

#### ITEM 테이블 (단일 테이블 전략 - SINGLE_TABLE)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| ITEM_ID | BIGINT | PK, NOT NULL | 상품 식별자 |
| DTYPE | VARCHAR(31) | NOT NULL | 구분 컬럼 ('B'=Book, 'A'=Album, 'M'=Movie) |
| NAME | VARCHAR(255) | - | 상품명 |
| PRICE | INTEGER | - | 가격 |
| STOCK_QUANTITY | INTEGER | - | 재고 수량 |
| AUTHOR | VARCHAR(255) | - | 저자 (Book 전용) |
| ISBN | VARCHAR(255) | - | ISBN (Book 전용) |
| ARTIST | VARCHAR(255) | - | 아티스트 (Album 전용) |
| ETC | VARCHAR(255) | - | 기타 정보 (Album 전용) |
| DIRECTOR | VARCHAR(255) | - | 감독 (Movie 전용) |
| ACTOR | VARCHAR(255) | - | 배우 (Movie 전용) |

#### DELIVERY 테이블

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| DELIVERY_ID | BIGINT | PK, NOT NULL | 배송 식별자 |
| STATUS | VARCHAR(255) | - | 배송 상태 (READY / COMP) |
| CITY | VARCHAR(255) | - | 주소 - 도시 |
| STREET | VARCHAR(255) | - | 주소 - 거리 |
| ZIPCODE | VARCHAR(255) | - | 주소 - 우편번호 |

#### CATEGORY 테이블

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| CATEGORY_ID | BIGINT | PK, NOT NULL | 카테고리 식별자 |
| PARENT_ID | BIGINT | FK (자기참조) | 부모 카테고리 ID |
| NAME | VARCHAR(255) | - | 카테고리 이름 |

#### CATEGORY_ITEM 테이블 (조인 테이블)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| CATEGORY_ID | BIGINT | PK, FK | 카테고리 외래 키 |
| ITEM_ID | BIGINT | PK, FK | 상품 외래 키 |

### 3.3 객체 모델 vs 테이블 모델 비교

| 도메인 객체 | DB 테이블 | 주요 차이점 |
|------------|----------|------------|
| `Member` | `MEMBER` | `Address`는 테이블에 컬럼으로 펼쳐짐 |
| `Order` | `ORDERS` | 예약어 충돌로 이름 변경 |
| `OrderItem` | `ORDER_ITEM` | 동일 |
| `Item` (추상) + Book/Album/Movie | `ITEM` (단일 테이블) | 상속을 하나의 테이블로 표현, `DTYPE` 컬럼 추가 |
| `Delivery` | `DELIVERY` | `Address`는 테이블에 컬럼으로 펼쳐짐 |
| `Category` | `CATEGORY` | 자기참조로 `PARENT_ID` 추가 |
| `Category` ↔ `Item` (M:N) | `CATEGORY_ITEM` | M:N은 조인 테이블로 변환 |

**객체와 테이블의 근본적인 차이:**

```
[객체 세계]
Order.getMember()  →  Member 객체를 직접 참조

[테이블 세계]
ORDERS.MEMBER_ID  →  MEMBER_ID 값(숫자)을 저장
                      JOIN으로 연결해서 데이터 가져옴

→ JPA가 이 두 세계의 차이(패러다임 불일치)를 해결해 줍니다.
```

---

## 4. 연관관계 매핑 분석

### 4.1 연관관계 주인(Owner) 개념

JPA에서 양방향 연관관계를 매핑할 때 가장 중요한 개념이 **연관관계 주인**입니다.

**왜 주인이 필요한가?**

```
[객체의 양방향 연관관계]

Member.orders → Order 목록 참조
Order.member  → Member 참조

→ 두 방향 모두 존재

[테이블의 FK]

ORDERS.MEMBER_ID → 하나의 FK만 존재

→ 어느 쪽 객체의 변경이 FK를 업데이트해야 하는가?
→ 반드시 하나를 "주인"으로 지정해야 함!
```

**연관관계 주인의 규칙:**

| 구분 | 설명 |
|------|------|
| **주인 (Owner)** | FK를 직접 관리 → `@JoinColumn` 사용, 데이터 변경 시 UPDATE/INSERT 발생 |
| **비주인 (Inverse)** | FK를 읽기만 함 → `mappedBy` 속성으로 주인 명시, 데이터 변경 시 무시됨 |

**주인 선택 기준:**

```
FK가 있는 쪽이 연관관계의 주인!

ORDERS.MEMBER_ID (FK)  →  Order가 주인
ORDER_ITEM.ORDER_ID (FK)  →  OrderItem이 주인
ORDER_ITEM.ITEM_ID (FK)  →  OrderItem이 주인
ORDERS.DELIVERY_ID (FK)  →  Order가 주인
```

### 4.2 연관관계 매핑 분석표

| 엔티티 쌍 | 관계 | FK 위치 | 주인 | mappedBy 설정 |
|-----------|------|---------|------|--------------|
| Member ↔ Order | 1:N (양방향) | ORDERS.MEMBER_ID | **Order** | Member.orders에 `mappedBy="member"` |
| Order ↔ OrderItem | 1:N (양방향) | ORDER_ITEM.ORDER_ID | **OrderItem** | Order.orderItems에 `mappedBy="order"` |
| OrderItem → Item | N:1 (단방향) | ORDER_ITEM.ITEM_ID | **OrderItem** | - (단방향이므로 mappedBy 없음) |
| Order ↔ Delivery | 1:1 (양방향) | ORDERS.DELIVERY_ID | **Order** | Delivery.order에 `mappedBy="delivery"` |
| Item ↔ Category | M:N (양방향) | CATEGORY_ITEM (조인 테이블) | **Category** | Item.categories에 `mappedBy="items"` |
| Category → Category | 1:N (자기참조) | CATEGORY.PARENT_ID | **Category(자식)** | Category.children에 `mappedBy="parent"` |

### 4.3 관계별 상세 분석

#### 4.3.1 Member (1) - Order (N): 양방향 1:N

```
[객체]
Member.orders : List<Order>     ← 비주인 (mappedBy = "member")
Order.member  : Member          ← 주인 (@ManyToOne, @JoinColumn)

[테이블]
MEMBER ─────────────── ORDERS
MEMBER_ID (PK)         ORDER_ID (PK)
                        MEMBER_ID (FK) ← 이 FK를 Order가 관리
```

```java
// Member (비주인 - 읽기 전용)
@OneToMany(mappedBy = "member")
private List<Order> orders = new ArrayList<>();

// Order (주인 - FK 관리)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "MEMBER_ID")
private Member member;
```

#### 4.3.2 Order (1) - OrderItem (N): 양방향 1:N

```
[객체]
Order.orderItems    : List<OrderItem>  ← 비주인 (mappedBy = "order")
OrderItem.order     : Order            ← 주인 (@ManyToOne, @JoinColumn)

[테이블]
ORDERS ─────────────────── ORDER_ITEM
ORDER_ID (PK)              ORDER_ITEM_ID (PK)
                            ORDER_ID (FK) ← 이 FK를 OrderItem이 관리
```

```java
// Order (비주인 - 읽기 전용)
@OneToMany(mappedBy = "order")
private List<OrderItem> orderItems = new ArrayList<>();

// OrderItem (주인 - FK 관리)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "ORDER_ID")
private Order order;
```

#### 4.3.3 OrderItem (N) - Item (1): 단방향 N:1

```
[객체]
OrderItem.item : Item   ← 주인 (단방향, @ManyToOne, @JoinColumn)
Item는 OrderItem을 참조하지 않음

[테이블]
ORDER_ITEM ─────────────────── ITEM
ORDER_ITEM_ID (PK)             ITEM_ID (PK)
ITEM_ID (FK) ← OrderItem이 관리
```

```java
// OrderItem (주인 - 단방향)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "ITEM_ID")
private Item item;

// Item에는 OrderItem 참조 없음 (단방향)
```

#### 4.3.4 Order (1) - Delivery (1): 양방향 1:1

```
[객체]
Order.delivery       : Delivery  ← 주인 (@OneToOne, @JoinColumn)
Delivery.order       : Order     ← 비주인 (mappedBy = "delivery")

[테이블]
ORDERS ─────────────────── DELIVERY
ORDER_ID (PK)              DELIVERY_ID (PK)
DELIVERY_ID (FK) ← Order가 관리
```

```java
// Order (주인 - FK 관리)
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "DELIVERY_ID")
private Delivery delivery;

// Delivery (비주인 - 읽기 전용)
@OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
private Order order;
```

**1:1 관계에서 FK를 어느 쪽에 두는가?**

```
주 테이블(주로 접근하는 쪽)에 FK를 두는 것이 일반적입니다.
→ 주문(Order)이 주 테이블이므로 ORDERS에 DELIVERY_ID 배치
→ 장점: Order 조회 시 JOIN 없이 DELIVERY_ID 확인 가능
```

#### 4.3.5 Item (M) - Category (N): 양방향 M:N

```
[객체]
Category.items    : List<Item>     ← 주인 (@ManyToMany, @JoinTable)
Item.categories   : List<Category> ← 비주인 (mappedBy = "items")

[테이블]
ITEM ─────── CATEGORY_ITEM ─────── CATEGORY
ITEM_ID(PK)  ITEM_ID(PK, FK)       CATEGORY_ID(PK)
             CATEGORY_ID(PK, FK)
```

```java
// Category (주인 - @JoinTable로 조인 테이블 명시)
@ManyToMany
@JoinTable(
    name = "CATEGORY_ITEM",
    joinColumns = @JoinColumn(name = "CATEGORY_ID"),
    inverseJoinColumns = @JoinColumn(name = "ITEM_ID")
)
private List<Item> items = new ArrayList<>();

// Item (비주인 - 읽기 전용)
@ManyToMany(mappedBy = "items")
private List<Category> categories = new ArrayList<>();
```

> ⚠️ **주의**: M:N 관계를 `@ManyToMany`로 구현하면 실무에서 문제가 많습니다. 자세한 내용은 [5.1절](#51-manytomany-사용을-지양해야-하는-이유)을 참고하세요.

#### 4.3.6 Category 자기참조: 부모-자식 트리

```
[객체]
Category.parent   : Category        ← 주인 (N:1, @ManyToOne)
Category.children : List<Category>  ← 비주인 (1:N, mappedBy = "parent")

[테이블]
CATEGORY
CATEGORY_ID (PK)
PARENT_ID (FK → CATEGORY_ID) ← 자기 자신을 참조
```

```
카테고리 트리 예시:
CATEGORY_ID   NAME       PARENT_ID
1             전체        NULL
2             음식        1
3             한식        2
4             중식        2
5             전자제품    1
6             가전        5
```

```java
// Category
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "PARENT_ID")
private Category parent;

@OneToMany(mappedBy = "parent")
private List<Category> children = new ArrayList<>();
```

---

## 5. 설계 핵심 주의점 (Best Practice)

### 5.1 @ManyToMany 사용을 지양해야 하는 이유

`@ManyToMany`는 JPA가 제공하는 편리한 기능이지만, **실무에서는 거의 사용하지 않습니다.**

**문제점:**

```
@ManyToMany로 생성되는 CATEGORY_ITEM 테이블:

CATEGORY_ID (PK, FK)
ITEM_ID     (PK, FK)

→ 오직 두 FK 컬럼만 존재!
→ 실무에서는 중간 테이블에 추가 컬럼이 필요한 경우가 많음
  예) 등록일, 등록자, 메모 등
→ @ManyToMany는 추가 컬럼을 넣을 수 없음!
→ 결국 나중에 중간 엔티티로 바꿔야 하는 상황 발생
```

**권장 패턴 - 중간 엔티티 사용:**

```
[비권장] @ManyToMany
Item ─────────────────────── Category
     (CATEGORY_ITEM 자동 생성)
     (추가 컬럼 불가)

[권장] 중간 엔티티
Item ─── CategoryItem ─── Category
         (추가 컬럼 가능)
         id: Long
         item: Item
         category: Category
         createdAt: LocalDateTime   ← 추가 컬럼 자유롭게 추가 가능
```

```java
// ❌ 안티패턴
@ManyToMany
@JoinTable(name = "CATEGORY_ITEM", ...)
private List<Item> items;

// ✅ 권장 패턴 (이 강의에서는 학습 목적으로 @ManyToMany 사용)
@Entity
public class CategoryItem {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORY_ID")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITEM_ID")
    private Item item;

    // 필요한 컬럼 자유롭게 추가 가능
    private LocalDateTime createdAt;
}
```

### 5.2 값 타입 (@Embeddable) Address의 개념

**Address는 왜 엔티티가 아닌 값 타입인가?**

```
[엔티티로 설계할 경우의 문제]
Member.address = Address (id=1)
Delivery.address = Address (id=2)
→ 각자 다른 DB 행이 필요, 불필요한 조인 발생

[값 타입으로 설계]
Member   테이블: CITY, STREET, ZIPCODE 컬럼 직접 포함
Delivery 테이블: CITY, STREET, ZIPCODE 컬럼 직접 포함
→ 조인 없음, 간단하고 효율적
```

**값 타입 사용 시 핵심 주의사항:**

```java
// ❌ 위험 - 같은 Address 인스턴스를 공유하면 안 됨!
Address address = new Address("Seoul", "Gangnam", "12345");
member.setAddress(address);
delivery.setAddress(address);  // 같은 객체 공유

// 나중에:
address.setCity("Busan");  // member와 delivery 모두 영향 받음! 사이드 이펙트!

// ✅ 안전 - 값 타입은 불변(Immutable)으로 만들어야 함
@Embeddable
public class Address {
    private String city;
    private String street;
    private String zipcode;

    // 생성자로만 값 설정 (setter 금지)
    protected Address() {} // JPA 기본 생성자

    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
    // getter만 제공, setter 없음
}
```

### 5.3 외래 키 위치 결정 기준 (연관관계 주인 선택)

**원칙: FK가 있는 테이블의 엔티티가 연관관계 주인**

```
[1:N 관계]
"N" 쪽이 항상 FK를 가짐 → "N" 쪽이 주인

Member(1) : Order(N) → Order가 MEMBER_ID FK 가짐 → Order가 주인
Order(1) : OrderItem(N) → OrderItem이 ORDER_ID FK 가짐 → OrderItem이 주인

[1:1 관계]
어느 쪽에 FK를 둘지 선택해야 함 → 주로 접근하는 쪽(주 테이블)에 FK 배치
Order : Delivery → Order가 DELIVERY_ID FK 가짐 → Order가 주인

[M:N 관계]
조인 테이블이 양쪽 FK를 모두 가짐
```

**비주인 쪽에서 값을 변경하면?**

```java
// 예시: 주인이 아닌 Member에서 Order를 추가하는 경우
member.getOrders().add(order);  // Member.orders에 추가 (비주인!)

// JPA는 이 변경을 DB에 반영하지 않음!
// Order 테이블의 MEMBER_ID는 변경되지 않음

// ✅ 반드시 주인 쪽에서 설정
order.setMember(member);  // Order.member 설정 (주인!) → MEMBER_ID 업데이트
```

**관례: 양방향 연관관계 편의 메서드 작성**

```java
// Order (주인) - 연관관계 편의 메서드 예시
public void setMember(Member member) {
    this.member = member;
    member.getOrders().add(this);  // 반대쪽도 함께 설정 (객체 동기화)
}
```

### 5.4 단방향 vs 양방향 연관관계 선택 기준

```
[원칙]
기본은 단방향으로 설계하고,
역방향 조회가 실제로 필요할 때만 양방향으로 확장

[단방향으로 충분한 경우]
OrderItem → Item: Item에서 OrderItem 목록을 조회할 일이 거의 없음
→ 단방향으로 유지

[양방향이 필요한 경우]
Order ↔ OrderItem: 주문서를 조회하면 주문 상품 목록이 반드시 필요
→ 양방향 설정 (Order.orderItems)
```

**양방향 연관관계 주의사항:**

| 항목 | 설명 |
|------|------|
| **무한 루프** | `toString()`, `JSON 직렬화` 시 양방향 참조로 무한 루프 발생 가능 |
| **테이블 증가 없음** | 양방향 추가해도 테이블 변경 없음 (객체 그래프만 추가됨) |
| **Lombok @Data 금지** | `@EqualsAndHashCode`가 양방향 참조에서 무한 루프 유발 |

---

## 6. 부록

### 6.1 핵심 용어 정리

| 용어 | 설명 |
|------|------|
| **도메인 모델** | 비즈니스 개념을 객체로 표현한 설계도 (클래스 다이어그램) |
| **ERD** | Entity-Relationship Diagram. 테이블 간의 관계를 나타내는 DB 설계도 |
| **엔티티(Entity)** | DB 테이블과 매핑되는 Java 클래스 (`@Entity`) |
| **값 타입(Value Type)** | 식별자 없이 소유 엔티티에 종속되는 데이터 (`@Embeddable`) |
| **연관관계 주인(Owner)** | FK를 실제로 관리하는 쪽. 이 쪽의 변경만 DB에 반영됨 |
| **mappedBy** | 비주인 쪽에서 사용하는 속성. "나는 이 필드에 의해 매핑된다"는 의미 |
| **조인 테이블** | M:N 관계에서 두 테이블의 FK를 모아놓은 중간 테이블 |
| **단일 테이블 전략** | 상속 관계를 하나의 테이블로 표현. `DTYPE` 컬럼으로 구분 |
| **영속성 컨텍스트** | JPA가 엔티티를 관리하는 1차 캐시 공간 |
| **패러다임 불일치** | 객체 지향(참조)과 관계형 DB(FK) 사이의 차이. JPA가 해결 |

### 6.2 어노테이션 미리보기

이 설계를 구현할 때 사용할 JPA 어노테이션 목록입니다.

| 어노테이션 | 설명 | 사용 위치 |
|-----------|------|----------|
| `@Entity` | JPA 엔티티 클래스 선언 | 클래스 |
| `@Table(name = "...")` | 매핑할 테이블 이름 지정 | 클래스 |
| `@Id` | 기본 키 매핑 | 필드 |
| `@GeneratedValue` | PK 자동 생성 전략 | 필드 |
| `@Column(name = "...")` | 컬럼 매핑 (이름, 제약조건 등) | 필드 |
| `@Embeddable` | 값 타입 클래스 선언 | 클래스 |
| `@Embedded` | 값 타입 필드 선언 | 필드 |
| `@OneToMany` | 일대다 연관관계 매핑 | 필드 |
| `@ManyToOne` | 다대일 연관관계 매핑 | 필드 |
| `@OneToOne` | 일대일 연관관계 매핑 | 필드 |
| `@ManyToMany` | 다대다 연관관계 매핑 (지양) | 필드 |
| `@JoinColumn(name = "...")` | FK 컬럼 이름 지정 (주인 쪽) | 필드 |
| `@JoinTable(...)` | 조인 테이블 설정 (M:N) | 필드 |
| `@Inheritance` | 상속 매핑 전략 지정 | 클래스 |
| `@DiscriminatorColumn` | 구분 컬럼 지정 (`DTYPE`) | 클래스 |
| `@DiscriminatorValue` | 구분 컬럼 값 지정 | 클래스 |
| `@Enumerated` | Enum 타입 매핑 | 필드 |

**fetch 전략:**

| 전략 | 어노테이션 | 설명 |
|------|-----------|------|
| **즉시 로딩** | `FetchType.EAGER` | 연관 엔티티를 즉시 JOIN해서 가져옴 (기본값: @ManyToOne, @OneToOne) |
| **지연 로딩** | `FetchType.LAZY` | 연관 엔티티를 실제 사용 시점에 쿼리 (권장) |

> **중요**: 실전에서는 모든 연관관계를 `FetchType.LAZY`로 설정하는 것을 강하게 권장합니다.

### 6.3 설계 요약표

| 항목 | 이 프로젝트의 설계 결정 |
|------|----------------------|
| **상속 매핑 전략** | 단일 테이블 전략 (SINGLE_TABLE) - `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` |
| **M:N 처리** | `@ManyToMany` 사용 (학습 목적) - 실무에서는 중간 엔티티 권장 |
| **값 타입** | `Address`를 `@Embeddable`로 설계, 불변으로 사용 |
| **1:1 FK 위치** | 주 테이블(Order)에 FK 배치 |
| **연관관계 방향** | 비즈니스 로직에서 역방향 탐색이 필요한 경우만 양방향 설정 |
| **Fetch 전략** | 모든 연관관계 `LAZY` 로딩 (지연 로딩) 기본 원칙 |

### 6.4 학습 확인 질문

다음 질문에 답하며 학습 내용을 확인해 보세요:

1. **연관관계 주인을 결정하는 기준은?**
   - FK가 있는 테이블의 엔티티가 주인이 됩니다.

2. **mappedBy를 사용하는 쪽은 주인인가, 비주인인가?**
   - 비주인입니다. mappedBy는 "나는 이 필드에 의해 매핑된다"는 의미로, 읽기 전용입니다.

3. **Order.delivery와 Delivery.order 중 어느 쪽이 주인인가?**
   - `Order.delivery`가 주인입니다. ORDERS 테이블에 DELIVERY_ID FK가 있기 때문입니다.

4. **@ManyToMany를 실무에서 사용하지 않는 이유는?**
   - 조인 테이블에 추가 컬럼(등록일, 메모 등)을 넣을 수 없어서, 나중에 반드시 중간 엔티티로 바꿔야 하는 상황이 발생합니다.

5. **Address를 엔티티가 아닌 값 타입(@Embeddable)으로 설계하는 이유는?**
   - Address는 식별자가 필요 없고 소유 엔티티(Member, Delivery)에 종속됩니다. 값 타입으로 설계하면 별도 테이블 없이 소유 테이블의 컬럼으로 펼쳐져 조인이 필요 없습니다.

6. **비주인(mappedBy) 쪽에서 값을 변경하면 DB에 반영되는가?**
   - 반영되지 않습니다. 반드시 주인 쪽에서 값을 변경해야 합니다. 다만 객체의 양쪽 상태를 모두 맞춰주는 연관관계 편의 메서드 작성을 권장합니다.

7. **단일 테이블 전략(SINGLE_TABLE)에서 DTYPE 컬럼의 역할은?**
   - Book인지, Album인지, Movie인지 구분하는 컬럼입니다. `@DiscriminatorColumn`으로 설정하며, 각 서브 클래스는 `@DiscriminatorValue`로 자신의 값을 지정합니다.

### 6.5 다음 챕터 예고

**챕터 2-2: 엔티티 클래스 개발**

다음 학습에서 이 설계를 실제 코드로 구현합니다:

```
설계도 완성 (이번 챕터)
    ↓
엔티티 클래스 작성 (다음 챕터)
    ├── Member.java          (@Entity, @Embedded Address)
    ├── Order.java           (@OneToMany, @ManyToOne, @OneToOne)
    ├── OrderItem.java       (@ManyToOne × 2)
    ├── Item.java            (@Inheritance, @DiscriminatorColumn)
    │   ├── Book.java        (@DiscriminatorValue("B"))
    │   ├── Album.java       (@DiscriminatorValue("A"))
    │   └── Movie.java       (@DiscriminatorValue("M"))
    ├── Delivery.java        (@OneToOne)
    ├── Category.java        (@ManyToMany, 자기참조)
    └── Address.java         (@Embeddable)
        ↓
엔티티 설계 시 주의점
    ├── 가급적 Getter만 공개
    ├── 모든 연관관계는 지연 로딩(LAZY)
    ├── 컬렉션은 필드에서 초기화
    └── 테이블/컬럼명 관례 설정
```

---

*작성일: 2026-02-24*
*강의: 인프런 - 김영한의 실전! 스프링 부트와 JPA 활용 1*
*챕터: 2. 도메인 분석 설계 (엔티티 클래스 개발 이전까지)*
