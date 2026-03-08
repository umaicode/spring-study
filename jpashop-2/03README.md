# API 개발 고급 - 지연 로딩과 조회 성능 최적화

> xToOne 관계(ManyToOne, OneToOne)의 N+1 문제를 V1→V4 단계적 개선으로 해결하는 실전 조회 성능 최적화 학습

## 📚 강의 출처

**인프런 - 김영한의 "실전! 스프링 부트와 JPA 활용 2 - API 개발과 성능 최적화"**

이 문서는 강의 챕터 3 "API 개발 고급 - 지연 로딩과 조회 성능 최적화"를 학습하며 작성한 노트입니다.
xToOne 연관관계(`Order → Member`, `Order → Delivery`)를 예제로, 엔티티 직접 노출(V1)부터 JPA DTO 직접 조회(V4)까지 4단계에 걸쳐 API 조회 성능을 개선하는 과정을 다룹니다.

---

## 🎯 학습 목표

1. **엔티티 직접 노출의 문제점 이해**
   - 양방향 연관관계에서 발생하는 무한 루프(`@JsonIgnore` 필요 이유)
   - LAZY 로딩과 프록시 객체 직렬화 문제 (Spring Boot 버전별 모듈 등록 방법)
   - N+1 문제의 발생 원인과 구체적인 쿼리 수 계산

2. **DTO 변환 패턴 적용 (V2)**
   - 엔티티 대신 전용 DTO를 반환해 API 스펙과 내부 도메인 분리
   - 영속성 컨텍스트 1차 캐시 효과 이해

3. **페치 조인(Fetch Join)으로 N+1 해결 (V3)**
   - JPQL `join fetch` 문법으로 연관 엔티티를 한 번의 쿼리로 조회
   - LAZY 로딩 없이 진짜 객체를 즉시 채우는 원리

4. **JPA에서 DTO 직접 조회 (V4)**
   - JPQL `new` 절로 SELECT 절을 최소화
   - 별도 Query Repository 패키지로 Repository 순수성 유지

5. **V3 vs V4 트레이드오프 판단 기준과 강사 권장 전략 이해**

---

## 🗺️ 학습 로드맵

```
V1. 엔티티 직접 노출
    GET /api/v1/simple-orders
    - 문제: 무한루프 / LAZY 프록시 직렬화 / N+1 / 불필요한 필드 노출
    ↓
V2. 엔티티 → DTO 변환
    GET /api/v2/simple-orders
    - 개선: API 스펙 분리, 엔티티 노출 방지
    - 문제: N+1 여전히 존재 (1 + N + N 쿼리)
    ↓
V3. 페치 조인 최적화  ✅ 권장
    GET /api/v3/simple-orders
    - 개선: 쿼리 1회로 Order + Member + Delivery 한 번에 조회
    - 특징: 재사용성 높음, Repository 순수성 유지
    ↓
V4. JPA에서 DTO 직접 조회
    GET /api/v4/simple-orders
    - 개선: SELECT 절 필드 최소화 (원하는 컬럼만)
    - 특징: 재사용성 낮음, 별도 QueryRepository 분리
```

---

## 📖 목차

1. [간단한 주문 조회 V1: 엔티티 직접 노출](#1-간단한-주문-조회-v1-엔티티-직접-노출)
2. [트러블슈팅: Spring Boot 버전별 Hibernate 모듈 등록](#2-트러블슈팅-spring-boot-버전별-hibernate-모듈-등록)
3. [간단한 주문 조회 V2: 엔티티를 DTO로 변환 (N+1 문제)](#3-간단한-주문-조회-v2-엔티티를-dto로-변환-n1-문제)
4. [간단한 주문 조회 V3: 페치 조인 최적화](#4-간단한-주문-조회-v3-페치-조인-최적화)
5. [간단한 주문 조회 V4: JPA에서 DTO 직접 조회](#5-간단한-주문-조회-v4-jpa에서-dto-직접-조회)
6. [V3 vs V4 트레이드오프 비교](#6-v3-vs-v4-트레이드오프-비교)
7. [쿼리 최적화 우선순위 (강사 권장 전략)](#7-쿼리-최적화-우선순위-강사-권장-전략)
8. [Best Practice 및 주의사항](#best-practice-및-주의사항)
9. [부록](#부록)

---

## 1. 간단한 주문 조회 V1: 엔티티 직접 노출

### 엔드포인트

```
GET /api/v1/simple-orders
```

### 코드

```java
// OrderSimpleApiController.java
@GetMapping("/api/v1/simple-orders")
public List<Order> ordersV1() {
    List<Order> all = orderRepository.findAllByString(new OrderSearch());
    for (Order order : all) {
        order.getMember().getName();      // Lazy 강제 초기화
        order.getDelivery().getAddress(); // Lazy 강제 초기화
    }
    return all;
}
```

### 문제 1: 양방향 연관관계 → 무한 루프

`Order → Member → orders → Member → ...` 처럼 양방향 연관관계를 그대로 직렬화하면 무한 루프가 발생한다.

**해결**: 양방향이 걸리는 곳마다 한 쪽에 `@JsonIgnore`를 붙여야 한다.

```java
// Member 엔티티
@JsonIgnore
@OneToMany(mappedBy = "member")
private List<Order> orders = new ArrayList<>();
```

### 문제 2: LAZY 로딩 → 프록시 객체 직렬화 문제

LAZY 로딩으로 설정된 연관 엔티티는 실제 쿼리를 날리지 않고 **ByteBuddy 프록시 객체**를 대신 넣어둔다. Jackson이 이 프록시 객체를 직렬화하려 할 때 문제가 발생한다.

```
Order 조회 시:
  member 필드 → new ProxyMember() (가짜 객체, DB 미조회)
  실제 member.getName() 호출 시 → 이 때 DB에 SELECT 쿼리 → 프록시 초기화
```

### 문제 3: N+1 문제

```
ORDER 조회: 쿼리 1번 → 결과 2건 (주문 2개)
  └─ order1.getMember() → DB 쿼리 1번 (Member SELECT)
  └─ order1.getDelivery() → DB 쿼리 1번 (Delivery SELECT)
  └─ order2.getMember() → DB 쿼리 1번 (Member SELECT)
  └─ order2.getDelivery() → DB 쿼리 1번 (Delivery SELECT)

최악의 경우: 1 + N + N = 1 + 2 + 2 = 5번 쿼리
```

> **N+1 이름의 유래**: 1번 쿼리 결과 N건에 대해 각각 추가 쿼리가 N번 나가는 문제

### 문제 4: 불필요한 필드 노출

엔티티를 그대로 반환하면 `orderItems`, `category` 등 API에서 필요 없는 모든 필드가 JSON에 포함된다. 보안 위험 및 성능 저하의 원인이 된다.

### 왜 EAGER로 바꾸면 안 되는가?

```
EAGER로 변경 시:
  JPQL "select o from Order o" 실행
  → Order만 가져옴
  → "어? Member가 EAGER네?" → 영속성 컨텍스트에서 단건으로 조회
  → 결국 N+1 문제 동일하게 발생
  + 다른 API에서 Member가 필요 없어도 항상 JOIN해서 가져옴 → 예측 불가
```

**결론: 모든 연관관계는 LAZY로 설정해야 한다.**

---

## 2. 트러블슈팅: Spring Boot 버전별 Hibernate 모듈 등록

### 왜 강사는 500 에러가 나고 나는 안 나는가?

Jackson은 기본적으로 Hibernate 프록시 객체를 JSON으로 어떻게 생성해야 하는지 모른다 → **예외 발생**.
`Hibernate5Module`을 스프링 빈으로 등록하면 해결된다. 단, **Spring Boot 버전에 따라 등록 방법이 다르다**.

| Spring Boot 버전 | 등록할 모듈 | build.gradle |
|-----------------|-----------|-------------|
| **3.0 미만** | `Hibernate5Module` | `jackson-datatype-hibernate5` |
| **3.0 이상** | `Hibernate5JakartaModule` | `jackson-datatype-hibernate5-jakarta` |
| **4.x (내 환경)** | 불필요 (아래 참고) | - |

### Spring Boot 3.0 미만: Hibernate5Module

```groovy
// build.gradle
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-hibernate5'
```

```java
@Bean
Hibernate5Module hibernate5Module() {
    return new Hibernate5Module();
}
// 기본: 초기화된 프록시만 노출, 미초기화 프록시는 null 처리
```

### Spring Boot 3.0 이상: Hibernate5JakartaModule

Spring Boot 3.0부터 `javax` → `jakarta` 패키지로 변경되어 다른 모듈을 사용해야 한다.
모듈 없이 실행하면 아래 에러 발생:

```
java.lang.ClassNotFoundException: javax.persistence.Transient
```

```groovy
// build.gradle
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-hibernate5-jakarta'
```

```java
@Bean
Hibernate5JakartaModule hibernate5Module() {
    return new Hibernate5JakartaModule();
}
// 기본: 초기화된 프록시만 노출, 미초기화 프록시는 null 처리
```

### 강제 지연 로딩 옵션 (공통)

```java
@Bean
Hibernate5Module hibernate5Module() {
    Hibernate5Module hibernate5Module = new Hibernate5Module();
    // 강제 지연 로딩: 미초기화 LAZY 필드를 모두 초기화
    hibernate5Module.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true);
    return hibernate5Module;
}
// 주의: order -> member -> orders -> member ... 양방향 무한 로딩 발생
// 따라서 @JsonIgnore가 반드시 필요하다
```

> **주의**: 엔티티에 `@JsonIgnore` 없이 `FORCE_LAZY_LOADING`을 켜면 양방향 연관관계를 무한 로딩한다.
> **참고**: `Hibernate5Module`을 쓰기보다 DTO로 변환하는 것이 훨씬 더 좋은 방법이다.

### Spring Boot 4.x (내 환경)에서 에러가 발생하지 않는 이유

PDF에서는 이 부분에 대한 설명이 없다. 아래는 자체 분석이다.

Spring Boot는 기본적으로 **OSIV(Open Session In View)** 가 활성화되어 있다.

```yaml
spring:
  jpa:
    open-in-view: true  # 기본값 ON
```

**OSIV**: HTTP 요청 시작부터 응답 완료까지 영속성 컨텍스트를 열어두는 전략. 덕분에 트랜잭션 밖(컨트롤러 레이어)에서도 LAZY 로딩이 가능하여 500 에러 없이 실제 데이터가 로딩된다.

| 동작 | 강사 결과 (모듈 기본 설정 시) | 내 결과 (Spring Boot 4.x + OSIV) |
|------|------------------------------|----------------------------------|
| 미초기화 LAZY 필드 | `null` | 실제 데이터 로딩 후 노출 |
| `orderItems` 필드 | `null` | 실제 주문 아이템 목록 전부 노출 |

**결론**: 동작은 하지만, 이것이 V1의 문제점(불필요한 데이터 과다 노출)을 오히려 더 극명하게 보여준다.

---

## 3. 간단한 주문 조회 V2: 엔티티를 DTO로 변환 (N+1 문제)

### 엔드포인트

```
GET /api/v2/simple-orders
```

### 코드

```java
@GetMapping("/api/v2/simple-orders")
public List<SimpleOrderDto> ordersV2() {
    List<Order> orders = orderRepository.findAllByString(new OrderSearch());

    List<SimpleOrderDto> result = orders.stream()
            .map(o -> new SimpleOrderDto(o))
            .collect(Collectors.toList());

    return result;
}

@Data
static class SimpleOrderDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    public SimpleOrderDto(Order order) {
        orderId = order.getId();
        name = order.getMember().getName();        // LAZY 초기화
        orderDate = order.getOrderDate();
        orderStatus = order.getStatus();
        address = order.getDelivery().getAddress(); // LAZY 초기화
    }
}
```

### V2에서 개선된 점

- 엔티티 대신 `SimpleOrderDto`를 반환 → API 스펙과 내부 도메인 분리
- 필요한 필드만 노출 (`orderId`, `name`, `orderDate`, `orderStatus`, `address`)
- 엔티티 변경이 API 응답에 자동으로 영향을 주지 않음

### V2의 문제: N+1은 여전히 존재

```
ORDER 조회: 쿼리 1번 → 주문 2건
루프 1회 (order1):
  └─ getMember().getName() → DB 쿼리 (userA 조회)
  └─ getDelivery().getAddress() → DB 쿼리 (delivery1 조회)
루프 2회 (order2):
  └─ getMember().getName() → 1차 캐시에 userA 있음 → DB 쿼리 생략!
  └─ getDelivery().getAddress() → DB 쿼리 (delivery2 조회)

최악: 1 + 2 + 2 = 5번 / 1차 캐시 덕분에 최선: 1 + 1 + 2 = 4번
```

### 영속성 컨텍스트 1차 캐시 효과

같은 트랜잭션 내에서 이미 조회한 엔티티는 1차 캐시에서 반환하므로 추가 쿼리가 나가지 않는다.

```
order1.getMember() → userA SELECT (DB 조회) → 1차 캐시 저장
order2.getMember() → userA가 이미 1차 캐시에 있음 → DB 쿼리 생략
```

하지만 이것은 운에 의존하는 최적화이며, 서로 다른 회원이 각 주문에 연관된 경우 혜택이 없다.

---

## 4. 간단한 주문 조회 V3: 페치 조인 최적화

### 엔드포인트

```
GET /api/v3/simple-orders
```

### 코드

```java
// OrderSimpleApiController.java
@GetMapping("/api/v3/simple-orders")
public List<SimpleOrderDto> ordersV3() {
    List<Order> orders = orderRepository.findAllWithMemberDelivery();
    List<SimpleOrderDto> result = orders.stream()
            .map(o -> new SimpleOrderDto(o))
            .collect(Collectors.toList());
    return result;
}
```

```java
// OrderRepository.java
public List<Order> findAllWithMemberDelivery() {
    return em.createQuery(
            "select o from Order o" +
                    " join fetch o.member m" +
                    " join fetch o.delivery d", Order.class
    ).getResultList();
}
```

### 페치 조인이란?

`join fetch`는 JPA에만 있는 문법으로, SQL의 JOIN과 다르게 **연관 엔티티를 SELECT 절에 포함해 한 번에 가져온다**.

```sql
-- 실제 실행되는 SQL
SELECT o.*, m.*, d.*
FROM orders o
INNER JOIN member m ON o.member_id = m.member_id
INNER JOIN delivery d ON o.delivery_id = d.delivery_id
```

### V3의 장점

- **쿼리 1번**으로 Order + Member + Delivery 모두 조회
- LAZY 로딩 발동 자체가 없음 (프록시가 아닌 진짜 객체가 채워져 있음)
- `SimpleOrderDto` 변환 시 추가 쿼리 0건
- Repository가 Entity를 조회하는 본래 역할을 유지함 (재사용성 높음)

### V3의 특징

- 실무에서 가장 자주 사용하는 최적화 기법
- 보통 주문서 조회 시 회원, 배송 정보를 함께 쓰는 패턴이 정해져 있어, `findAllWithMemberDelivery()`를 범용으로 사용 가능

---

## 5. 간단한 주문 조회 V4: JPA에서 DTO 직접 조회

### 엔드포인트

```
GET /api/v4/simple-orders
```

### 코드 구조

V4는 별도의 Query Repository 패키지를 분리한다.

```
repository/
  order/
    simplequery/
      OrderSimpleQueryRepository.java   ← DTO 전용 쿼리
      OrderSimpleQueryDto.java          ← DTO 클래스
```

```java
// OrderSimpleQueryDto.java
@Data
public class OrderSimpleQueryDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    public OrderSimpleQueryDto(Long orderId, String name, LocalDateTime orderDate,
                               OrderStatus orderStatus, Address address) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
    }
}
```

```java
// OrderSimpleQueryRepository.java
@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {

    private final EntityManager em;

    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery(
                "select new jpabook.jpashop_2.repository.order.simplequery.OrderSimpleQueryDto" +
                "(o.id, m.name, o.orderDate, o.status, d.address) " +
                "from Order o" +
                " join o.member m" +
                " join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }
}
```

```java
// OrderSimpleApiController.java
@GetMapping("/api/v4/simple-orders")
public List<OrderSimpleQueryDto> ordersV4() {
    return orderSimpleQueryRepository.findOrderDtos();
}
```

### V4의 장점

- SELECT 절에 필요한 컬럼만 포함 → DB → 애플리케이션 네트워크 용량 최적화 **(생각보다 미비)**
- 쿼리 1번으로 처리 (V3와 동일한 쿼리 횟수)

```sql
-- 실제 실행되는 SQL (V3 대비 SELECT 절이 더 적음)
SELECT o.order_id, m.name, o.order_date, o.status, d.city, d.street, d.zipcode
FROM orders o
INNER JOIN member m ON o.member_id = m.member_id
INNER JOIN delivery d ON o.delivery_id = d.delivery_id
```

### V4의 단점

- **재사용성 없음**: `OrderSimpleQueryDto`는 특정 API 스펙에 종속
- **Repository가 API 스펙에 의존**: API 스펙 변경 시 Repository까지 수정해야 함
- **JPA 변경 감지 불가**: DTO 조회이므로 영속성 컨텍스트가 관리하지 않음

### 왜 별도 Repository 패키지로 분리하는가?

```
나쁜 예: OrderRepository에 findOrderDtos() 포함
  → Repository가 화면(API 스펙)에 의존
  → 용도가 섞여 유지보수 어려움

좋은 예: OrderSimpleQueryRepository 별도 분리
  → OrderRepository: 엔티티 순수 조회 전용
  → OrderSimpleQueryRepository: 화면/성능 최적화 전용 쿼리
  → 관심사가 분리되어 유지보수성 향상
```

---

## 6. V3 vs V4 트레이드오프 비교

| 구분 | V3 (페치 조인) | V4 (DTO 직접 조회) |
|------|---------------|------------------|
| 쿼리 수 | 1회 | 1회 |
| SELECT 컬럼 수 | 전체 엔티티 컬럼 | 필요한 컬럼만 |
| 네트워크 전송량 | 상대적으로 많음 | 적음 (생각보다 미비) |
| 재사용성 | ✅ 높음 (여러 API에서 활용 가능) | ❌ 낮음 (특정 API 전용) |
| Repository 순수성 | ✅ 유지 (Entity 조회 역할 유지) | ❌ API 스펙에 의존 |
| 코드 복잡도 | 낮음 | 높음 (별도 DTO + Repository) |
| JPA 변경 감지 | ✅ 가능 (Entity 반환) | ❌ 불가 (DTO 반환) |
| 권장 상황 | 일반적인 경우 | 고트래픽 / 대용량 SELECT 최적화 |

### 언제 V4를 선택할까?

다음 조건이 **모두** 해당될 때 V4를 고려한다:

1. SELECT 컬럼이 20~30개 이상으로 매우 많은 경우
2. 고객이 실시간으로 대량 호출하는 API (고트래픽)
3. V3로도 성능이 해결되지 않는 경우

단순 admin API나 내부 API라면 V3로 충분한 경우가 대부분이다.
실무에서 성능 문제의 대부분은 인덱스 설정 오류에서 비롯되며, SELECT 컬럼 수는 상대적으로 영향이 작다.

---

## 7. 쿼리 최적화 우선순위 (강사 권장 전략)

강사가 실무에서 권장하는 단계적 접근 방법:

```
1단계: 엔티티 → DTO 변환 (V2)
   └─ API 스펙 분리, 코드 유지보수성 확보
   └─ 항상 첫 번째로 선택

2단계: 페치 조인으로 성능 최적화 (V3)  ← 95% 이상 해결
   └─ N+1 문제 해결
   └─ 여기서 대부분의 성능 이슈 종료

3단계: DTO 직접 조회 (V4)
   └─ V3로 부족한 경우에만 고려
   └─ SELECT 절 최소화

4단계: Native SQL / Spring JDBC Template
   └─ JPA 한계를 넘어야 하는 극단적 상황
   └─ EntityManager 대신 DB 커넥션 직접 사용
```

> 실무에서 JPA 성능 문제의 90%는 N+1에서 발생한다.
> 페치 조인(V3)으로 대부분 해결되며, 나머지는 인덱스 점검을 먼저 하라.

---

## Best Practice 및 주의사항

### ✅ 반드시 지켜야 할 규칙

1. **모든 연관관계는 LAZY로 설정**
   - `@ManyToOne(fetch = FetchType.LAZY)`
   - `@OneToOne(fetch = FetchType.LAZY)`
   - EAGER는 절대 사용하지 않는다 (예측 불가 쿼리 발생)

2. **엔티티를 API 응답으로 직접 반환하지 않는다**
   - 엔티티 변경 → API 스펙 변동 → 클라이언트 장애
   - 전용 DTO를 별도로 정의한다

3. **Repository 계층 분리**
   - `OrderRepository`: 엔티티 순수 조회 (페치 조인 포함 가능)
   - `OrderSimpleQueryRepository`: 화면/DTO 전용 쿼리 분리

4. **`@JsonIgnore` 남용 금지**
   - V1처럼 엔티티에 `@JsonIgnore`를 붙이는 방식은 임시방편
   - DTO 패턴으로 근본적으로 해결한다

### ⚠️ 주의사항

- **OSIV 이해**: Spring Boot 기본값은 `open-in-view: true`
  - 편리하지만 DB 커넥션을 HTTP 요청 전체에서 점유
  - 실시간 트래픽이 많은 서비스라면 `false`로 설정 고려
  - `false`로 설정 시 지연 로딩은 반드시 트랜잭션 안에서 해결해야 함

- **성능 최적화 순서**: 인덱스 → 페치 조인 → DTO 직접 조회 순으로 접근
  - SELECT 컬럼 수보다 인덱스, JOIN, WHERE 조건이 성능에 더 큰 영향

- **1차 캐시 의존 금지**: 영속성 컨텍스트 캐시 히트는 보장되지 않음

---

## 부록

### API 엔드포인트 정리

| 버전 | URL | 쿼리 수 | 특징 |
|------|-----|---------|------|
| V1 | `GET /api/v1/simple-orders` | 1 + N + N | 엔티티 직접 노출, 문제 많음 |
| V2 | `GET /api/v2/simple-orders` | 1 + N + N | DTO 변환, N+1 미해결 |
| V3 | `GET /api/v3/simple-orders` | **1** | 페치 조인, 권장 |
| V4 | `GET /api/v4/simple-orders` | **1** | DTO 직접 조회, SELECT 최소화 |

### 핵심 개념 정리

| 개념 | 설명 |
|------|------|
| **N+1 문제** | 1번 쿼리 결과 N건에 대해 각각 추가 쿼리 N번이 발생하는 문제 |
| **프록시** | LAZY 로딩 시 실제 객체 대신 넣어두는 가짜 객체 (ByteBuddy 기반) |
| **프록시 초기화** | 프록시 객체에 실제 값에 접근할 때 DB 쿼리를 날려 실제 객체로 채우는 과정 |
| **페치 조인** | JPA 전용 문법. `join fetch`로 연관 엔티티를 SELECT 절에 포함해 한 번에 조회 |
| **OSIV** | Open Session In View. HTTP 요청 동안 영속성 컨텍스트를 열어두는 전략 |
| **1차 캐시** | 영속성 컨텍스트 내 엔티티 캐시. 같은 트랜잭션에서 동일 엔티티 재조회 시 DB 생략 |

### 핵심 파일 경로

```
src/main/java/jpabook/jpashop_2/
├── api/
│   └── OrderSimpleApiController.java       ← V1~V4 API 컨트롤러
├── repository/
│   ├── OrderRepository.java                ← findAllWithMemberDelivery() (V3 페치 조인)
│   └── order/
│       └── simplequery/
│           ├── OrderSimpleQueryRepository.java   ← findOrderDtos() (V4 DTO 조회)
│           └── OrderSimpleQueryDto.java          ← V4 전용 DTO
└── domain/
    ├── Order.java                          ← @ManyToOne LAZY 설정
    ├── Member.java                         ← @JsonIgnore (양방향 연관관계)
    └── Delivery.java                       ← @JsonIgnore (양방향 연관관계)
```

### 다음 챕터 예고

챕터 4: **API 개발 고급 - 컬렉션 조회 최적화**

`Order → OrderItem`처럼 **일대다(OneToMany) 컬렉션** 관계에서 발생하는 데이터 뻥튀기 문제와 N+1 해결을 다룬다. xToOne보다 훨씬 복잡한 최적화가 필요하다.
