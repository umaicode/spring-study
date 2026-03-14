# API 개발 고급 - 컬렉션 조회 최적화

> Order → OrderItem 컬렉션(일대다) 관계에서 발생하는 데이터 뻥튀기·N+1 폭발 문제를
> V1→V6 단계적 개선으로 해결하는 실전 조회 성능 최적화 학습

## 📚 강의 출처

**인프런 - 김영한의 "실전! 스프링 부트와 JPA 활용 2 - API 개발과 성능 최적화"**

이 문서는 강의 챕터 4 "API 개발 고급 - 컬렉션 조회 최적화"를 학습하며 작성한 노트입니다.
`Order → OrderItem`처럼 **일대다(OneToMany) 컬렉션** 관계를 예제로, 엔티티 직접 노출(V1)부터 JPA 플랫 DTO 직접 조회(V6)까지 6단계에 걸쳐 API 조회 성능을 개선하는 과정을 다룹니다.

---

## 🎯 학습 목표

1. **컬렉션 조회 시 데이터 뻥튀기 문제 이해**
   - 일대다 JOIN 시 Order 1건이 OrderItem N건만큼 중복 행으로 뻥튀기되는 원리
   - `distinct`의 두 역할(SQL DISTINCT + JPA 레벨 중복 제거) 차이 이해

2. **DTO 안에 엔티티 참조 금지 원칙 적용**
   - `List<OrderItem>` 대신 `List<OrderItemDto>` 사용해 API 스펙과 도메인 완전 분리

3. **컬렉션 페치 조인의 치명적 단점 이해**
   - 컬렉션 페치 조인 + 페이징 조합 시 메모리 OOM 위험 (`HHH90003004` 경고)
   - 컬렉션 페치 조인은 한 개만 사용 가능한 제약

4. **`default_batch_fetch_size`로 페이징과 성능 동시 해결 (V3.1)**
   - ToOne 페치 조인 + 컬렉션 LAZY + IN 쿼리 배치 전략으로 쿼리 3번에 페이징 가능

5. **DTO 직접 조회 3단계 개선 (V4 → V5 → V6)**
   - `OrderQueryRepository` 별도 패키지 분리로 관심사 분리
   - 1+N → 1+1 → 쿼리 1번으로 단계적 최적화

6. **강사 권장 전략과 엔티티 캐시 금지 원칙 이해**

---

## 🗺️ 학습 로드맵

```
[엔티티 조회 방식]

V1. 엔티티 직접 노출
    GET /api/v1/orders
    - 문제: 엔티티 스펙 전체 노출 / DTO 안에 List<OrderItem> 엔티티 참조
    ↓
V2. 엔티티 → DTO 변환 (OrderDto + OrderItemDto)
    GET /api/v2/orders
    - 개선: 엔티티 노출 방지, API 스펙 분리
    - 문제: N+1 폭발 (최악 1 + N + N + N + N*M 쿼리)
    ↓
V3. 컬렉션 페치 조인 + distinct
    GET /api/v3/orders
    - 개선: 쿼리 1번
    - 치명적 단점: 페이징 불가 (HHH90003004 경고 + 메모리 OOM 위험)
    ↓
V3.1. ToOne 페치 조인 + 컬렉션 LAZY + batch_fetch_size  ✅ 강사 권장
    GET /api/v3.1/orders
    - 개선: 쿼리 3번 + 페이징 가능 + 데이터 중복 없음

---

[DTO 직접 조회 방식]

V4. OrderQueryRepository: findOrderQueryDtos()
    GET /api/v4/orders
    - 특징: JPQL new 절 + 컬렉션 별도 조회
    - 문제: 1 + N 쿼리
    ↓
V5. findAllByDto_optimization()
    GET /api/v5/orders
    - 개선: IN 쿼리 + 메모리 Map 매칭 → 1 + 1 쿼리
    ↓
V6. findAllByDto_flat() + 컨트롤러 분해
    GET /api/v6/orders
    - 개선: 쿼리 1번
    - 단점: 페이징 불가(Order 기준) / 중복 데이터 전송 / 애플리케이션 추가 작업
```

---

## 📖 목차

1. [주문 조회 V1: 엔티티 직접 노출](#1-주문-조회-v1-엔티티-직접-노출)
2. [주문 조회 V2: 엔티티를 DTO로 변환 (컬렉션 N+1 폭발)](#2-주문-조회-v2-엔티티를-dto로-변환-컬렉션-n1-폭발)
3. [주문 조회 V3: 컬렉션 페치 조인 최적화](#3-주문-조회-v3-컬렉션-페치-조인-최적화)
4. [주문 조회 V3.1: 페이징과 한계 돌파 (batch_fetch_size)](#4-주문-조회-v31-페이징과-한계-돌파-batch_fetch_size)
5. [주문 조회 V4: JPA에서 DTO 직접 조회 (1+N)](#5-주문-조회-v4-jpa에서-dto-직접-조회-1n)
6. [주문 조회 V5: JPA에서 DTO 직접 조회 최적화 (1+1)](#6-주문-조회-v5-jpa에서-dto-직접-조회-최적화-11)
7. [주문 조회 V6: JPA에서 DTO로 직접 조회 - 플랫 데이터 최적화 (쿼리 1번)](#7-주문-조회-v6-jpa에서-dto로-직접-조회---플랫-데이터-최적화-쿼리-1번)
8. [종합 비교](#8-종합-비교)
9. [쿼리 최적화 우선순위 (강사 권장 전략)](#9-쿼리-최적화-우선순위-강사-권장-전략)
10. [Best Practice 및 주의사항](#best-practice-및-주의사항)
11. [부록](#부록)

---

## 1. 주문 조회 V1: 엔티티 직접 노출

### 엔드포인트

```
GET /api/v1/orders
```

### 코드

```java
// OrderApiController.java
@GetMapping("/api/v1/orders")
public List<Order> ordersV1() {
    List<Order> all = orderRepository.findAllByString(new OrderSearch());

    for (Order order : all) {
        order.getMember().getName();
        order.getDelivery().getAddress();

        List<OrderItem> orderItems = order.getOrderItems();
        orderItems.stream().forEach(o -> o.getItem().getName()); // 강제 초기화
    }
    return all;
}
```

### 핵심 교훈: DTO 안에 엔티티 있으면 안 된다

V1의 가장 큰 문제는 단순히 엔티티를 반환한다는 것뿐만 아니라, **DTO로 감싸도 내부에 엔티티(`List<OrderItem>`)가 남아있으면 그 엔티티의 스펙이 전부 노출된다**는 점이다.

```
나쁜 예:
OrderDto {
  orderId: 1,
  name: "userA",
  orderItems: [OrderItem 엔티티 전체 노출]  ← 엔티티 스펙이 다 나가버린다
}

좋은 예:
OrderDto {
  orderId: 1,
  name: "userA",
  orderItems: [OrderItemDto(itemName, orderPrice, count)]  ← 필요한 필드만
}
```

> **원칙**: DTO 안에 엔티티가 있으면 안 된다. 중첩된 엔티티(`OrderItem`)까지 모두 DTO(`OrderItemDto`)로 변환해야 한다.

### V1의 문제점 요약

- 엔티티 스펙이 API로 전부 노출됨 (보안 위험)
- 엔티티 변경이 API 스펙에 직접 영향
- N+1 문제 발생

---

## 2. 주문 조회 V2: 엔티티를 DTO로 변환 (컬렉션 N+1 폭발)

### 엔드포인트

```
GET /api/v2/orders
```

### 코드

```java
// OrderApiController.java
@GetMapping("/api/v2/orders")
public List<OrderDto> ordersV2() {
    List<Order> orders = orderRepository.findAllByString(new OrderSearch());
    List<OrderDto> result = orders.stream()
            .map(o -> new OrderDto(o))
            .collect(toList());
    return result;
}

@Getter
static class OrderDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;
    private List<OrderItemDto> orderItems;  // ← 엔티티 아닌 DTO 사용

    public OrderDto(Order order) {
        orderId = order.getId();
        name = order.getMember().getName();       // LAZY 초기화
        orderDate = order.getOrderDate();
        orderStatus = order.getStatus();
        address = order.getMember().getAddress();
        orderItems = order.getOrderItems().stream()
                .map(orderItem -> new OrderItemDto(orderItem))
                .collect(toList());
    }
}

@Getter
static class OrderItemDto {
    private String itemName;    // 상품 명
    private int orderPrice;     // 주문 가격
    private int count;          // 주문 수량

    public OrderItemDto(OrderItem orderItem) {
        itemName = orderItem.getItem().getName();  // LAZY 초기화
        orderPrice = orderItem.getOrderPrice();
        count = orderItem.getCount();
    }
}
```

### V2에서 개선된 점

- `OrderDto` + `OrderItemDto` 내부 클래스로 엔티티 완전 차단
- `private List<OrderItemDto> orderItems` (엔티티가 아닌 DTO) 로 API 스펙과 도메인 완전 분리

### V2의 문제: 컬렉션 N+1 폭발

```
ORDER 조회: 쿼리 1번 → 주문 2건 (order1, order2)

order1 처리:
  getMember().getName()     → DB 쿼리 (Member SELECT)
  getDelivery().getAddress() → DB 쿼리 (Delivery SELECT)
  getOrderItems()           → DB 쿼리 (OrderItem SELECT) → 2건 (oi1, oi2)
    oi1.getItem().getName() → DB 쿼리 (Item SELECT)
    oi2.getItem().getName() → DB 쿼리 (Item SELECT)

order2 처리:
  getMember().getName()     → DB 쿼리 (Member SELECT)
  getDelivery().getAddress() → DB 쿼리 (Delivery SELECT)
  getOrderItems()           → DB 쿼리 (OrderItem SELECT) → 2건 (oi3, oi4)
    oi3.getItem().getName() → DB 쿼리 (Item SELECT)
    oi4.getItem().getName() → DB 쿼리 (Item SELECT)

최악: 1 + N(member) + N(delivery) + N(orderItems) + N*M(item)
    = 1 + 2 + 2 + 2 + 4 = 11번 쿼리
```

---

## 3. 주문 조회 V3: 컬렉션 페치 조인 최적화

### 엔드포인트

```
GET /api/v3/orders
```

### 코드

```java
// OrderApiController.java
@GetMapping("/api/v3/orders")
public List<OrderDto> ordersV3() {
    List<Order> orders = orderRepository.findAllWithItem();
    List<OrderDto> result = orders.stream()
            .map(o -> new OrderDto(o))
            .collect(toList());
    return result;
}
```

```java
// OrderRepository.java
public List<Order> findAllWithItem() {
    return em.createQuery(
            "select distinct o from Order o" +
                    " join fetch o.member m" +
                    " join fetch o.delivery d" +
                    " join fetch o.orderItems oi" +
                    " join fetch oi.item i", Order.class)
            .getResultList();
}
```

### 데이터 뻥튀기 문제

일대다 JOIN을 하면 "다" 쪽 데이터 개수만큼 "일" 쪽이 중복된다.

```
실제 데이터:          DB JOIN 결과:
Order 2건             order_id | orderitem_id
OrderItem 4건    →    1        | 1
                      1        | 2    ← order_id=1 중복!
                      2        | 3
                      2        | 4    ← order_id=2 중복!
```

### `distinct`의 두 역할

| 역할 | 설명 | 실제 효과 |
|------|------|---------|
| SQL DISTINCT | DB에 DISTINCT 키워드 전달 | 모든 컬럼이 동일해야 중복 제거 → 뻥튀기된 경우 효과 미미 |
| JPA 레벨 중복 제거 | 같은 PK(id)를 가진 엔티티 중복 제거 후 컬렉션에 담음 | **실제 중복 제거** |

### 치명적 단점: 페이징 불가 + 메모리 OOM 위험

컬렉션 페치 조인에서 페이징을 시도하면 아래 경고가 발생한다.

```
HHH90003004: firstResult/maxResults specified with collection fetch;
applying in memory!
```

**무슨 일이 벌어지는가?**
```
1. DB에서 뻥튀기된 전체 데이터(4건)를 애플리케이션으로 모두 올림
2. 애플리케이션 메모리에서 페이징 처리
3. 데이터가 만 건이면 → 만 건을 전부 메모리에 올린 후 잘라냄 → OOM 위험
```

> **결론**: 컬렉션 페치 조인에서는 절대 페이징을 사용하면 안 된다.
> 또한 컬렉션 페치 조인은 한 개만 사용 가능하다 (둘 이상 사용 시 데이터 정합성 붕괴).

---

## 4. 주문 조회 V3.1: 페이징과 한계 돌파 (batch_fetch_size)

> **강사 최우선 권장 방식** - 페이징 + 성능 최적화를 동시에 해결

### 엔드포인트

```
GET /api/v3.1/orders?offset=0&limit=100
```

### 전략: ToOne 페치 조인 + 컬렉션 LAZY + batch_fetch_size

```java
// OrderApiController.java
@GetMapping("/api/v3.1/orders")
public List<OrderDto> ordersV3_page(
        @RequestParam(value = "offset", defaultValue = "0") int offset,
        @RequestParam(value = "limit", defaultValue = "100") int limit) {

    // ToOne 관계(Member, Delivery)만 페치 조인 → 페이징 쿼리 정상 동작
    List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

    List<OrderDto> result = orders.stream()
            .map(o -> new OrderDto(o))
            .collect(toList());
    return result;
}
```

```java
// OrderRepository.java
public List<Order> findAllWithMemberDelivery(int offset, int limit) {
    return em.createQuery(
            "select o from Order o" +
                    " join fetch o.member m" +
                    " join fetch o.delivery d", Order.class)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
}
```

### application.yml 설정

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

### 쿼리 흐름 (쿼리 3번)

```
1번째 쿼리: Order + Member + Delivery (페치 조인, 페이징 포함)
  SELECT o.*, m.*, d.*
  FROM orders o
  JOIN member m ON ...
  JOIN delivery d ON ...
  LIMIT 100 OFFSET 0
  → Order 2건 조회 (orderId: 1, 2)

2번째 쿼리: OrderItem IN 쿼리 (batch_fetch_size 적용)
  SELECT oi.*
  FROM order_item oi
  WHERE oi.order_id IN (1, 2)
  → OrderItem 4건 조회

3번째 쿼리: Item IN 쿼리 (batch_fetch_size 적용)
  SELECT i.*
  FROM item i
  WHERE i.item_id IN (1, 2, 3, 4)
  → Item 4건 조회
```

### V3 vs V3.1 데이터 전송량 비교

| 구분 | V3 (컬렉션 페치 조인) | V3.1 (batch_fetch_size) |
|------|---------------------|------------------------|
| 쿼리 수 | 1번 | 3번 |
| Order 데이터 | 4행 (뻥튀기 중복) | 2행 (중복 없음) |
| 데이터 전송량 | 많음 (중복 포함) | 적음 (정규화된 데이터) |
| 페이징 | **불가** | **가능** |

### `@BatchSize` 개별 적용 방법

글로벌 설정 대신 특정 컬렉션이나 엔티티에만 적용할 경우:

```java
// 컬렉션 필드에 직접 적용
@BatchSize(size = 1000)
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
private List<OrderItem> orderItems = new ArrayList<>();

// ToOne 엔티티 클래스 레벨에 적용
@BatchSize(size = 1000)
@Entity
public class Item { ... }
```

> **권장 사이즈**: 100 ~ 1000
> - 1000 초과: 일부 DB에서 오류 발생
> - 너무 작으면: 쿼리 횟수 증가
> - WAS/DB 순간 부하와 쿼리 횟수 사이의 트레이드오프 → 100으로 시작 후 조정

---

## 5. 주문 조회 V4: JPA에서 DTO 직접 조회 (1+N)

### 엔드포인트

```
GET /api/v4/orders
```

### `OrderQueryRepository` 별도 패키지 분리 이유

```
repository/
  OrderRepository.java              ← 핵심 비즈니스용 엔티티 조회 전용
  order/
    query/
      OrderQueryRepository.java     ← 화면/API 전용 DTO 쿼리 (관심사 분리)
      OrderQueryDto.java
      OrderItemQueryDto.java
      OrderFlatDto.java
```

**분리 이유**:
- `OrderRepository`에 DTO 조회 쿼리를 넣으면 Repository가 API 스펙에 의존 → 의존관계 역전
- 화면/API의 라이프사이클과 핵심 비즈니스 로직의 라이프사이클이 다름
- `OrderQueryRepository`를 `Controller`가 참조하면 순환 의존관계 발생 방지

### 코드

```java
// OrderApiController.java
@GetMapping("/api/v4/orders")
public List<OrderQueryDto> ordersV4() {
    return orderQueryRepository.findOrderQueryDtos();
}
```

```java
// OrderQueryRepository.java
public List<OrderQueryDto> findOrderQueryDtos() {
    List<OrderQueryDto> result = findOrders();  // 쿼리 1번 → N건

    result.forEach(o -> {
        List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId()); // 쿼리 N번
        o.setOrderItems(orderItems);
    });

    return result;
}

private List<OrderQueryDto> findOrders() {
    return em.createQuery(
            "select new jpabook.jpashop_2.repository.order.query.OrderQueryDto" +
                    "(o.id, m.name, o.orderDate, o.status, d.address)" +
                    " from Order o" +
                    " join o.member m" +
                    " join o.delivery d", OrderQueryDto.class)
            .getResultList();
}

private List<OrderItemQueryDto> findOrderItems(Long orderId) {
    return em.createQuery(
            "select new jpabook.jpashop_2.repository.order.query.OrderItemQueryDto" +
                    "(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                    " from OrderItem oi" +
                    " join oi.item i" +
                    " where oi.order.id = :orderId", OrderItemQueryDto.class)
            .setParameter("orderId", orderId)
            .getResultList();
}
```

### JPQL `new` 절에서 컬렉션 직접 주입이 불가능한 이유

```
JPQL new 절은 SQL의 플랫한 한 줄 결과를 DTO에 매핑하는 방식이다.
컬렉션(List<OrderItemQueryDto>)은 일대다이므로 데이터가 뻥튀기된다.
→ 플랫한 한 줄로 컬렉션을 직접 넣을 수 없음
→ 어쩔 수 없이 별도로 findOrderItems()를 루프에서 호출
```

### V4 쿼리 수

```
findOrders(): 쿼리 1번 → Order 2건
findOrderItems(1): 쿼리 1번
findOrderItems(2): 쿼리 1번
→ 총 1 + N 번
```

---

## 6. 주문 조회 V5: JPA에서 DTO 직접 조회 최적화 (1+1)

### 엔드포인트

```
GET /api/v5/orders
```

### 핵심 3단계

```java
// OrderQueryRepository.java
public List<OrderQueryDto> findAllByDto_optimization() {
    // 1단계: Order 루트 조회 (쿼리 1번)
    List<OrderQueryDto> result = findOrders();

    // 2단계: orderId 목록 추출 후 IN 쿼리로 OrderItem 한 번에 조회 (쿼리 1번)
    Map<Long, List<OrderItemQueryDto>> orderItemMap =
            findOrderItemMap(toOrderIds(result));

    // 3단계: 메모리에서 Map을 이용해 orderId 기준으로 매칭
    result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));

    return result;
}

private List<Long> toOrderIds(List<OrderQueryDto> result) {
    return result.stream()
            .map(o -> o.getOrderId())
            .collect(Collectors.toList());
}

private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
    List<OrderItemQueryDto> orderItems = em.createQuery(
                    "select new jpabook.jpashop_2.repository.order.query.OrderItemQueryDto" +
                            "(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                            " from OrderItem oi" +
                            " join oi.item i" +
                            " where oi.order.id in :orderIds", OrderItemQueryDto.class)
            .setParameter("orderIds", orderIds)
            .getResultList();

    // orderId 기준으로 Map으로 변환 → 메모리에서 O(1) 매칭
    return orderItems.stream()
            .collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));
}
```

### V4 vs V5 비교

| 구분 | V4 | V5 |
|------|----|----|
| 루트 조회 | findOrders() 1번 | findOrders() 1번 |
| 컬렉션 조회 | 루프마다 findOrderItems() N번 | IN 쿼리로 한 번 + Map 메모리 매칭 |
| 총 쿼리 | 1 + N | **1 + 1** |
| 핵심 최적화 | - | `groupingBy`로 Map 변환 후 메모리 매칭 |

> V4의 N번 루프 쿼리를 → IN 쿼리 1번으로 줄이고 → 메모리에서 Map 매칭으로 처리

---

## 7. 주문 조회 V6: JPA에서 DTO로 직접 조회 - 플랫 데이터 최적화 (쿼리 1번)

### 엔드포인트

```
GET /api/v6/orders
```

### `OrderFlatDto` 구조

```java
// OrderFlatDto.java
@Data
public class OrderFlatDto {
    // Order 정보
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    // OrderItem + Item 정보 (JOIN으로 플랫하게 합침)
    private String itemName;
    private int orderPrice;
    private int count;
}
```

### 쿼리: 모든 테이블을 한 번에 JOIN

```java
// OrderQueryRepository.java
public List<OrderFlatDto> findAllByDto_flat() {
    return em.createQuery(
            "select new jpabook.jpashop_2.repository.order.query.OrderFlatDto" +
                    "(o.id, m.name, o.orderDate, o.status, d.address, i.name, oi.orderPrice, oi.count)" +
                    " from Order o" +
                    " join o.member m" +
                    " join o.delivery d" +
                    " join o.orderItems oi" +
                    " join oi.item i", OrderFlatDto.class)
            .getResultList();
}
```

### 컨트롤러에서 `groupingBy` + `mapping`으로 분해

```java
// OrderApiController.java
@GetMapping("api/v6/orders")
public List<OrderQueryDto> ordersV6() {
    List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

    return flats.stream()
            .collect(groupingBy(o -> new OrderQueryDto(
                            o.getOrderId(), o.getName(), o.getOrderDate(),
                            o.getOrderStatus(), o.getAddress()),
                    mapping(o -> new OrderItemQueryDto(
                            o.getOrderId(), o.getItemName(),
                            o.getOrderPrice(), o.getCount()), toList())
            )).entrySet().stream()
            .map(e -> new OrderQueryDto(
                    e.getKey().getOrderId(), e.getKey().getName(),
                    e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                    e.getKey().getAddress(), e.getValue()))
            .collect(toList());
}
```

### `@EqualsAndHashCode(of = "orderId")` 가 필요한 이유

`groupingBy`의 키로 `OrderQueryDto` 객체를 사용할 때, 같은 `orderId`를 가진 DTO들을 같은 그룹으로 묶어야 한다.

```java
// OrderQueryDto.java
@Data
@EqualsAndHashCode(of = "orderId")  // orderId만으로 equals/hashCode 판단
public class OrderQueryDto { ... }
```

`@EqualsAndHashCode`가 없으면 `orderId`가 같아도 다른 객체로 인식 → 그룹이 묶이지 않아 분해 실패.

### V6의 단점 3가지

| 단점 | 설명 |
|------|------|
| 중복 데이터 전송 | 일대다 JOIN으로 Order 정보가 OrderItem 수만큼 중복 전송 |
| 페이징 불가 (Order 기준) | DB에서 뻥튀기된 행 기준으로 페이징 → Order 기준 페이징 불가 |
| 애플리케이션 추가 작업 | 플랫 DTO를 직접 `groupingBy` + `mapping`으로 분해해야 함 |

---

## 8. 종합 비교

### 엔티티 조회 방식 (V1 ~ V3.1)

| 버전 | 쿼리 수 | 페이징 | 데이터 중복 | 코드 복잡도 | 권장 |
|------|---------|--------|-----------|-----------|------|
| V1 | 1 + N + M | 가능 | 엔티티 전체 노출 | 낮음 | 금지 |
| V2 | 1 + N + M | 가능 | DTO (N+1 폭발) | 중간 | 시작점 |
| V3 | 1 | **불가** | 있음 (뻥튀기) | 낮음 | 페이징 없을 때 |
| V3.1 | 1 + 1 + 1 | **가능** | 없음 | 낮음 | **최우선 권장** |

### DTO 직접 조회 방식 (V4 ~ V6)

| 버전 | 쿼리 수 | 페이징 | 코드 복잡도 | 권장 |
|------|---------|--------|-----------|------|
| V4 | 1 + N | 가능 | 높음 | 미권장 (V5 이용) |
| V5 | 1 + 1 | 가능 | 높음 | DTO 직접 조회 선택 시 권장 |
| V6 | 1 | **불가** (Order 기준) | 매우 높음 | 드문 케이스 |

---

## 9. 쿼리 최적화 우선순위 (강사 권장 전략)

강사가 실무에서 권장하는 단계적 접근 방법:

```
1단계: 엔티티 → DTO 변환 (V2)
   └─ OrderDto + OrderItemDto로 API 스펙과 도메인 분리
   └─ 항상 첫 번째로 선택

2단계: 페치 조인 + batch_fetch_size (V3.1)  ← 95% 이상 해결
   └─ ToOne 페치 조인 + 컬렉션 LAZY + default_batch_fetch_size: 100
   └─ 페이징 가능 + 중복 없음 + 코드 간단
   └─ 여기서 대부분의 성능 이슈 종료

3단계: DTO 직접 조회 최적화 (V5)
   └─ OrderQueryRepository 분리 + IN 쿼리 + Map 매칭
   └─ V3.1로 부족한 경우에만 고려

4단계: 캐시 (Redis 등)
   └─ 반드시 DTO를 캐시해야 한다 (엔티티 캐시 절대 금지)
   └─ 엔티티를 캐시하면 영속성 컨텍스트 관리 상태와 충돌

5단계: Native SQL / JdbcTemplate
   └─ JPA 한계를 넘어야 하는 극단적 상황
   └─ EntityManager 대신 DB 커넥션 직접 사용
```

### 엔티티 캐시 금지 원칙

```
엔티티는 영속성 컨텍스트에서 관리된다 (상태 보유).
캐시에 올라간 엔티티는 영속성 컨텍스트가 삭제/변경해도 캐시에서 안 지워진다.
→ 데이터 정합성 문제 발생 가능

해결: 캐시는 반드시 DTO로 변환 후 DTO를 캐시
(Hibernate 2차 캐시는 존재하나 실무에서 적용이 매우 까다로움)
```

> 실무에서 JPA 성능 문제의 90%는 N+1에서 발생한다.
> `default_batch_fetch_size` 설정 하나로 대부분의 컬렉션 N+1이 해결된다.

---

## Best Practice 및 주의사항

### ✅ 반드시 지켜야 할 규칙

1. **DTO 안에 엔티티 참조 금지**
   - `private List<OrderItem> orderItems` → ❌
   - `private List<OrderItemDto> orderItems` → ✅
   - 중첩된 엔티티까지 모두 DTO로 변환해야 한다

2. **컬렉션 페치 조인 + 페이징 조합 절대 금지**
   - `HHH90003004` 경고 발생 + 메모리에서 전체 데이터 올린 후 페이징 → OOM 위험
   - V3.1 (`default_batch_fetch_size`) 방식으로 해결

3. **`default_batch_fetch_size` 100~1000 글로벌 설정 권장**
   - `spring.jpa.properties.hibernate.default_batch_fetch_size: 100`
   - 설정 하나로 모든 컬렉션·프록시 LAZY 로딩에 IN 쿼리 적용

4. **엔티티 캐시 금지 / DTO만 캐시**
   - Redis, 로컬 메모리 캐시 모두 DTO로 변환 후 캐시
   - 엔티티를 캐시하면 영속성 컨텍스트와 충돌

5. **컬렉션 페치 조인은 한 개만 사용**
   - 두 개 이상 사용 시 데이터 정합성 붕괴 위험

### ⚠️ 주의사항

- **성능 최적화 순서**: 인덱스 확인 → V3.1(batch_fetch_size) → V5(DTO 직접 조회) → 캐시
  - SELECT 컬럼 수보다 인덱스, JOIN, WHERE 조건이 성능에 훨씬 큰 영향
  - 성능 문제 대부분은 인덱스 미설정에서 발생

- **V6는 드문 케이스**: 쿼리 1번이지만 페이징 불가 + 중복 전송 + 코드 복잡 → 실무 선택 빈도 낮음

- **V5 실무 권장**: 강사는 DTO 직접 조회 방식 중에서는 V5를 선호

---

## 부록

### API 엔드포인트 정리

| 버전 | URL | 쿼리 수 | 페이징 | 특징 |
|------|-----|---------|--------|------|
| V1 | `GET /api/v1/orders` | 1 + N + M | 가능 | 엔티티 직접 노출, 금지 |
| V2 | `GET /api/v2/orders` | 1 + N + M | 가능 | DTO 변환, N+1 미해결 |
| V3 | `GET /api/v3/orders` | **1** | **불가** | 컬렉션 페치 조인 |
| V3.1 | `GET /api/v3.1/orders` | **3** | **가능** | batch_fetch_size, 권장 |
| V4 | `GET /api/v4/orders` | 1 + N | 가능 | DTO 직접 조회, 미권장 |
| V5 | `GET /api/v5/orders` | **1 + 1** | 가능 | IN 쿼리 최적화, 권장 |
| V6 | `GET /api/v6/orders` | **1** | **불가** | 플랫 DTO, 드문 케이스 |

### 핵심 개념 정리

| 개념 | 설명 |
|------|------|
| **데이터 뻥튀기** | 일대다 JOIN 시 "일" 쪽 데이터가 "다" 쪽 개수만큼 중복 행으로 증가하는 현상 |
| **distinct (JPA)** | SQL DISTINCT + JPA 레벨 동일 ID 엔티티 중복 제거. 두 가지 역할을 동시에 수행 |
| **HHH90003004** | 컬렉션 페치 조인 + 페이징 조합 시 Hibernate가 메모리 페이징으로 처리함을 알리는 경고 |
| **batch_fetch_size** | LAZY 로딩 시 IN 쿼리로 N+1을 해결. `default_batch_fetch_size`: 글로벌 / `@BatchSize`: 개별 적용 |
| **@BatchSize** | 특정 컬렉션 필드나 엔티티 클래스에 개별 배치 사이즈를 지정하는 어노테이션 |
| **플랫 DTO** | 여러 테이블을 JOIN한 결과를 중첩 없이 한 줄로 펼친 DTO (`OrderFlatDto`) |
| **@EqualsAndHashCode** | `groupingBy` 키로 DTO 사용 시 특정 필드(orderId)만으로 동등성을 판단하도록 Lombok으로 설정 |

### 핵심 파일 경로

```
src/main/java/jpabook/jpashop_2/
├── api/
│   └── OrderApiController.java         ← V1~V6 API 컨트롤러 + OrderDto + OrderItemDto
├── repository/
│   ├── OrderRepository.java            ← findAllWithItem() (V3) / findAllWithMemberDelivery() (V3.1)
│   └── order/
│       └── query/
│           ├── OrderQueryRepository.java    ← findOrderQueryDtos() (V4) / findAllByDto_optimization() (V5) / findAllByDto_flat() (V6)
│           ├── OrderQueryDto.java           ← V4~V6 루트 DTO (@EqualsAndHashCode)
│           ├── OrderItemQueryDto.java       ← V4~V6 OrderItem DTO
│           └── OrderFlatDto.java            ← V6 플랫 DTO (8개 컬럼)
└── resources/
    └── application.yml                 ← default_batch_fetch_size: 100
```

### 다음 챕터 예고

챕터 5: **API 개발 고급 - 실무 필수 최적화**

OSIV(Open Session In View) 전략의 동작 원리와 실무 적용 기준, QueryDSL 소개를 다룬다.
