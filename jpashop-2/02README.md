# API 개발 고급 - 준비

> 복잡한 연관관계를 가진 엔티티를 조회하는 API 개발을 앞두고,
> 실습용 샘플 데이터를 설계하고 초기화하는 방법을 익힌다.

---

## 📚 강의 출처

**인프런 - 김영한의 "실전! 스프링 부트와 JPA 활용 2 - API 개발과 성능 최적화"**

이 문서는 강의의 두 번째 챕터 "API 개발 고급 - 준비"를 학습하며 작성한 노트입니다.
챕터 1(API 기본)에서 단순 회원 CRUD를 다뤘다면, 이제 주문처럼 여러 테이블이 JOIN되는 복잡한 조회 API를 준비합니다.

---

## 🎯 학습 목표

1. **왜 준비 챕터가 필요한가** — 챕터 3~5의 성능 최적화 문제를 재현하려면 현실적 샘플 데이터가 필요하다
2. **조회용 샘플 데이터 설계** — 주문, 상품, 배송이 복잡하게 얽힌 시나리오를 구성한다
3. **`@PostConstruct` + `@Transactional` 분리 패턴** — 스프링 라이프사이클 제약을 올바르게 우회하는 방법을 이해한다
4. **엔티티 연관관계 복습** — 다음 챕터에서 N+1 문제가 터지는 배경 연관관계를 재확인한다

---

## 🗺️ 학습 로드맵

```
챕터 1 (API 기본)
  └─ 회원 단일 테이블 CRUD → DTO 설계 원칙 학습
       ↓
챕터 2 (준비 ← 현재)
  └─ 복잡한 연관관계 샘플 데이터 세팅
       ↓
챕터 3 (지연 로딩과 조회 성능 최적화)
  └─ 단순 조회 API에서 N+1 문제 발생 → fetch join / DTO 최적화
       ↓
챕터 4 (컬렉션 조회 최적화)
  └─ 컬렉션(OneToMany) 포함 API 최적화
       ↓
챕터 5 (실무 필수 최적화)
  └─ 페이징 + 컬렉션 → 하이버네이트 배치 사이즈 등 실무 전략
```

---

## 📖 목차

1. [왜 준비 챕터가 필요한가](#1-왜-준비-챕터가-필요한가)
2. [조회용 샘플 데이터 설계](#2-조회용-샘플-데이터-설계)
3. [InitDb 구현 — @PostConstruct + @Transactional 분리 패턴](#3-initdb-구현--postconstrcut--transactional-분리-패턴)
4. [엔티티 연관관계 복습](#4-엔티티-연관관계-복습)
5. [Best Practice 및 주의사항](#best-practice-및-주의사항)
6. [부록](#부록)

---

## 1. 왜 준비 챕터가 필요한가

### 챕터 1 vs 챕터 3~5 비교

| 구분 | 챕터 1 (API 기본) | 챕터 3~5 (고급 최적화) |
|------|------------------|----------------------|
| 대상 엔티티 | `Member` (단일 테이블) | `Order ↔ OrderItem ↔ Item`, `Order ↔ Delivery` |
| 쿼리 복잡도 | SELECT 1개 | JOIN 여러 테이블, N+1 문제 발생 |
| 최적화 필요성 | 낮음 | 높음 (fetch join, DTO 프로젝션, 배치 사이즈) |
| 지연 로딩 문제 | 없음 | LAZY 로딩이 N+1 쿼리를 유발 |

### 현실적 샘플 데이터가 필요한 이유

- 지연 로딩 N+1 문제는 **데이터가 여러 건 있을 때** 드러난다
- `userA`의 주문 → `JPA1 BOOK`, `JPA2 BOOK` (2개 아이템)
  `userB`의 주문 → `SPRING1 BOOK`, `SPRING2 BOOK` (2개 아이템)
  이 구조가 있어야 조회 시 쿼리가 몇 번 나가는지 확인 가능하다

---

## 2. 조회용 샘플 데이터 설계

### 시나리오 요약

```
주문 총 2건

userA (서울 거주)
  └─ 주문 1
       ├─ JPA1 BOOK   (가격: 10,000원 × 1권)
       └─ JPA2 BOOK   (가격: 20,000원 × 2권)

userB (진주 거주)
  └─ 주문 2
       ├─ SPRING1 BOOK (가격: 20,000원 × 3권)
       └─ SPRING2 BOOK (가격: 40,000원 × 4권)
```

### 샘플 데이터 ER 다이어그램 (ASCII)

```
Member ──────────── Order ──────────── Delivery
(userA/userB)  N:1  (주문1/주문2) 1:1  (배송지)
                        │
                    1:N │
                        │
                   OrderItem ──────── Item(Book)
                   (주문상품)    N:1  (JPA1/JPA2
                                      SPRING1/SPRING2)
```

### 테이블별 데이터

| 테이블 | 데이터 |
|--------|--------|
| member | userA(서울), userB(진주) |
| orders | 주문 2건 (각 userA, userB) |
| delivery | 2건 (각 주문의 배송지 = 회원 주소) |
| order_item | 4건 (주문당 2개 상품) |
| item | 4건 (JPA1, JPA2, SPRING1, SPRING2 BOOK) |

---

## 3. InitDb 구현 — @PostConstruct + @Transactional 분리 패턴

### 핵심 문제: @PostConstruct 안에서 트랜잭션이 동작하지 않는다

스프링 컨테이너는 빈 초기화 순서가 있다.
`@PostConstruct`는 **빈 초기화 단계**에서 호출되는데, 이 시점에는 트랜잭션 AOP 프록시가 아직 완전히 준비되지 않아 `@Transactional`이 적용되지 않는다.

```
[스프링 라이프사이클]

1. 빈 생성 (new InitDb())
2. 의존관계 주입 (@RequiredArgsConstructor → InitService 주입)
3. @PostConstruct 호출 ← 여기서 트랜잭션 AOP가 정상 동작 안 함
4. 실제 서비스 시작
```

### 해결책: 별도 @Component 빈으로 분리

`@PostConstruct`가 있는 `InitDb`는 **트랜잭션 없이 호출**만 담당하고,
실제 DB 작업은 `@Transactional`이 붙은 **별도 빈(`InitService`)**에 위임한다.

```java
@Component
@RequiredArgsConstructor
public class InitDb {

    private final InitService initService;

    @PostConstruct
    public void init() {
        initService.dbInit1();  // InitService 빈의 트랜잭션 AOP가 정상 작동
        initService.dbInit2();
    }

    @Component
    @Transactional           // 여기에 트랜잭션 적용
    @RequiredArgsConstructor
    static class InitService {

        private final EntityManager em;

        public void dbInit1() { ... }
        public void dbInit2() { ... }
    }
}
```

### Extract Method 리팩토링

`dbInit1()`과 `dbInit2()`는 Member, Book, Delivery를 생성하는 로직이 반복된다.
인텔리제이의 **Extract Method** 리팩토링으로 공통 생성 로직을 분리한다.

| 메서드 | 역할 |
|--------|------|
| `createMember(name, city, street, zipcode)` | Member 엔티티 생성 |
| `createBook(name, price, stockQuantity)` | Book(Item 서브타입) 생성 |
| `createDelivery(member)` | Delivery 생성 (회원 주소 복사) |

### 전체 InitDb 코드

```java
/**
 * 총 주문 2개
 *  * userA
 *      * JPA1 BOOK
 *      * JPA2 BOOK
 *  * userB
 *      * SPRING1 BOOK
 *      * SPRING2 BOOK
 */
@Component
@RequiredArgsConstructor
public class InitDb {

    private final InitService initService;

    @PostConstruct
    public void init() {
        initService.dbInit1();
        initService.dbInit2();
    }

    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {

        private final EntityManager em;

        public void dbInit1() {
            Member member = createMember("userA", "서울", "1", "1111");
            em.persist(member);

            Book book1 = createBook("JPA1 BOOK", 10000, 100);
            em.persist(book1);

            Book book2 = createBook("JPA2 BOOK", 20000, 100);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 10000, 1);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 20000, 2);

            Delivery delivery = createDelivery(member);
            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);
        }

        public void dbInit2() {
            Member member = createMember("userB", "진주", "2", "2222");
            em.persist(member);

            Book book1 = createBook("SPRING1 BOOK", 20000, 200);
            em.persist(book1);

            Book book2 = createBook("SPRING2 BOOK", 40000, 300);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 20000, 3);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 40000, 4);

            Delivery delivery = createDelivery(member);
            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);
        }

        private Delivery createDelivery(Member member) {
            Delivery delivery = new Delivery();
            delivery.setAddress(member.getAddress());
            return delivery;
        }

        private Member createMember(String name, String city, String street, String zipcode) {
            Member member = new Member();
            member.setName(name);
            member.setAddress(new Address(city, street, zipcode));
            return member;
        }

        private Book createBook(String name, int price, int stockQuantity) {
            Book book1 = new Book();
            book1.setName(name);
            book1.setPrice(price);
            book1.setStockQuantity(stockQuantity);
            return book1;
        }
    }
}
```

### 왜 `em.persist(order)`만 호출해도 OrderItem, Delivery가 저장되는가?

`Order` 엔티티에 `CascadeType.ALL`이 설정되어 있기 때문이다:

```java
// Order.java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
private List<OrderItem> orderItems = new ArrayList<>();

@OneToOne(fetch = LAZY, cascade = CascadeType.ALL)
@JoinColumn(name = "delivery_id")
private Delivery delivery;
```

`Order`를 persist하면 연관된 `OrderItem`과 `Delivery`도 자동으로 영속화된다.

---

## 4. 엔티티 연관관계 복습

### 주요 연관관계 매핑 요약

| 관계 | 방향 | 타입 | FetchType | Cascade |
|------|------|------|-----------|---------|
| `Order` → `Member` | 다대일 (N:1) | `@ManyToOne` | **LAZY** | 없음 |
| `Member` → `Order` | 일대다 (1:N) | `@OneToMany` | LAZY (기본) | 없음 |
| `Order` → `OrderItem` | 일대다 (1:N) | `@OneToMany` | LAZY (기본) | **ALL** |
| `Order` → `Delivery` | 일대일 (1:1) | `@OneToOne` | **LAZY** | **ALL** |
| `OrderItem` → `Item` | 다대일 (N:1) | `@ManyToOne` | **LAZY** | 없음 |

### 다음 챕터(N+1 문제)와의 연결

```
주문 목록 조회 시 발생하는 쿼리 흐름 (LAZY 로딩):

1. SELECT * FROM orders             → 주문 2건 조회 (쿼리 1번)
2. SELECT * FROM member WHERE id=?  → userA 조회 (쿼리 1번) ← 지연 로딩!
3. SELECT * FROM member WHERE id=?  → userB 조회 (쿼리 1번) ← 지연 로딩!
4. SELECT * FROM delivery WHERE id=? → 배송1 조회 (쿼리 1번)
5. SELECT * FROM delivery WHERE id=? → 배송2 조회 (쿼리 1번)

총 5번 쿼리 → 주문이 N건이면 최악 1 + N + N 쿼리 발생 = N+1 문제!
```

이 문제를 챕터 3에서 **fetch join**과 **DTO 직접 조회**로 해결한다.

---

## Best Practice 및 주의사항

### @PostConstruct + @Transactional 분리 패턴

```
✅ 올바른 방법
@PostConstruct
public void init() {
    initService.dbInit1();  // 별도 빈의 트랜잭션 이용
}

@Component
@Transactional
static class InitService { ... }

❌ 잘못된 방법
@PostConstruct
@Transactional          // 이 시점에는 AOP 프록시가 완전히 준비되지 않아 트랜잭션이 동작 안 함
public void init() {
    em.persist(...);    // TransactionRequiredException 발생 가능
}
```

### 샘플 데이터는 실제 비즈니스 시나리오를 반영해야 한다

- 단순히 `member 1명, order 1건`이면 N+1 문제가 드러나지 않는다
- **최소 2명의 회원 × 각 2개의 상품** 구조가 있어야 지연 로딩 쿼리 횟수를 체감할 수 있다

### Cascade 범위에 주의

- `CascadeType.ALL`은 편리하지만, 연관 엔티티를 다른 곳에서도 참조하는 경우 의도치 않은 삭제가 발생할 수 있다
- `OrderItem`과 `Delivery`는 `Order`에만 종속되므로 `ALL` 적용이 적절하다
- `Member`에는 Cascade를 걸지 않는다 — Member는 Order 외 다른 곳에서도 독립적으로 사용되기 때문이다

### 정적 내부 클래스 패턴의 의미

`InitService`를 `static class`로 선언한 이유:
- 외부 클래스(`InitDb`)의 인스턴스 없이도 스프링 빈으로 등록 가능
- `@Component`를 함께 붙여 스프링이 독립적으로 관리하도록 한다

---

## 부록

### 주요 엔티티 관계 요약

```
Member
  id, name, address(embedded)
  └─ orders: List<Order> (1:N, mappedBy)

Order
  id, member(N:1 LAZY), orderItems(1:N CASCADE ALL), delivery(1:1 LAZY CASCADE ALL)
  orderDate, status(ORDER/CANCEL)

OrderItem
  id, item(N:1 LAZY), order(N:1), orderPrice, count

Delivery
  id, order(1:1), address(embedded), status(READY/COMP)

Item (추상)
  id, name, price, stockQuantity
  └─ Book: author, isbn
  └─ Album: artist, etc
  └─ Movie: director, actor
```

### 다음 챕터 예고 — 지연 로딩과 조회 성능 최적화

챕터 3에서는 이 샘플 데이터를 기반으로 다음 5단계 최적화를 학습한다:

| 단계 | 방법 | 특징 |
|------|------|------|
| V1 | 엔티티 직접 노출 | LAZY 초기화 → N+1 문제 발생 |
| V2 | 엔티티 → DTO 변환 | 여전히 N+1 문제 있음 |
| V3 | fetch join 최적화 | 쿼리 1번으로 해결 |
| V4 | JPA DTO 직접 조회 | SELECT 컬럼 최소화 |
| V5 | NativeSQL / JdbcTemplate | 최후의 수단 |
