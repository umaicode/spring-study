# 주문 도메인 개발

> CascadeType.ALL · 생성 메서드 · 도메인 모델 패턴으로 복잡한 주문 비즈니스 로직 구현하기

## 📚 강의 출처

**인프런 - 김영한의 "실전! 스프링 부트와 JPA 활용 1 - 웹 애플리케이션 개발"**

이 문서는 강의의 챕터 6 "주문 도메인 개발"을 학습하며 작성한 노트입니다.
챕터 5까지 구축한 회원(Member)과 상품(Item) 도메인을 연결하는 핵심 도메인으로,
**연관관계 편의 메서드**, **정적 팩토리 생성 메서드**, **CascadeType.ALL**, **JPA Dirty Checking** 이라는
4가지 핵심 개념이 본격적으로 등장합니다.

| 챕터 | 내용 |
|------|------|
| 챕터 5 (이전) | 상품 도메인 개발 — JPA 상속 매핑, 도메인 모델 패턴, persist vs merge |
| **챕터 6 (현재)** | **주문 도메인 개발 — 연관관계 편의 메서드, 생성 메서드, Cascade, 동적 쿼리** |
| 챕터 7 (다음) | 웹 계층 개발 — Thymeleaf 뷰, 상품 수정(변경 감지 vs merge) |

---

## 🎯 학습 목표

이 문서를 통해 다음을 이해하고 실습합니다:

1. **연관관계 편의 메서드** — 양방향 관계를 한 번의 호출로 양쪽에 설정하는 이유와 구현 방법
2. **정적 팩토리 생성 메서드** — 복잡한 엔티티 생성 로직을 `createOrder()` 안에 캡슐화하는 이유
3. **`@NoArgsConstructor(access = PROTECTED)`** — protected 생성자로 잘못된 생성 경로를 컴파일 단계에서 차단
4. **`CascadeType.ALL`** — 언제 써야 하고 언제 쓰면 안 되는지 판단 기준
5. **JPA Dirty Checking(변경 감지)** — `cancelOrder()`에서 UPDATE 쿼리를 직접 작성하지 않아도 되는 이유
6. **동적 쿼리 3가지 방법** — String JPQL, Criteria API, Querydsl 각각의 특징과 한계

---

## 🗺️ 학습 로드맵

챕터 6은 **도메인 → 리포지토리 → 서비스 → 테스트 → 검색** 순으로 Bottom-Up 방식으로 구성됩니다.

```
1단계: 주문·주문상품 엔티티 개발 (Order / OrderItem / OrderStatus)
   - 연관관계 매핑 (ManyToOne, OneToMany, OneToOne)
   - 연관관계 편의 메서드 (setMember, addOrderItem, setDelivery)
   - 생성 메서드 (Order.createOrder, OrderItem.createOrderItem)
   - protected 생성자 → 잘못된 생성 차단
   - 비즈니스 로직 (cancel, getTotalPrice)
   ↓
2단계: 주문 리포지토리 개발 (OrderRepository)
   - save, findOne 기본 구현
   ↓
3단계: 주문 서비스 개발 (OrderService)
   - order(): Cascade로 단일 save 가능
   - cancelOrder(): Dirty Checking으로 자동 UPDATE
   ↓
4단계: 주문 기능 테스트 (OrderServiceTest)
   - 상품주문, 재고수량초과, 주문취소 — 3개 통합 테스트
   ↓
5단계: 주문 검색 기능 개발
   - String JPQL vs Criteria API 동적 쿼리 구현
```

**왜 이 순서인가?**
- **엔티티 우선**: 비즈니스 로직이 Order/OrderItem 엔티티 안에 있으므로, 서비스보다 엔티티를 먼저 완성해야 합니다.
- **Cascade 전제**: `orderRepository.save(order)` 한 번으로 모든 연관 엔티티가 저장되려면, Order 엔티티의 Cascade 설정이 먼저 완료되어야 합니다.

---

## 📖 목차

1. [주문·주문상품 엔티티 개발](#1-주문주문상품-엔티티-개발)
   - 1.1 엔티티 관계 설계
   - 1.2 테이블 이름 `orders` 사용 이유
   - 1.3 연관관계 편의 메서드
   - 1.4 생성 메서드 `Order.createOrder()`
   - 1.5 protected 생성자
   - 1.6 비즈니스 로직: `cancel()`, `getTotalPrice()`
   - 1.7 OrderItem 엔티티
   - 1.8 전체 코드
2. [CascadeType.ALL — 언제 써야 하는가](#2-cascadetypeall--언제-써야-하는가)
   - 2.1 Cascade 없을 때 vs 있을 때
   - 2.2 Cascade 사용 조건 2가지
   - 2.3 이번 설계에서 Cascade가 가능한 이유
   - 2.4 Cascade를 쓰면 안 되는 경우
3. [주문 리포지토리 개발](#3-주문-리포지토리-개발)
4. [주문 서비스 개발](#4-주문-서비스-개발)
   - 4.1 `order()` 메서드 구현 흐름
   - 4.2 `cancelOrder()` — Dirty Checking
   - 4.3 도메인 모델 패턴 vs 트랜잭션 스크립트 패턴
   - 4.4 JPA Dirty Checking(변경 감지)
   - 4.5 전체 코드
5. [주문 기능 테스트](#5-주문-기능-테스트)
   - 5.1 통합 테스트 vs 단위 테스트
   - 5.2 테스트 1: 상품주문()
   - 5.3 테스트 2: 상품주문_재고수량초과()
   - 5.4 테스트 3: 주문취소()
   - 5.5 헬퍼 메서드 패턴
   - 5.6 전체 코드
6. [주문 검색 기능 개발 — 동적 쿼리](#6-주문-검색-기능-개발--동적-쿼리)
   - 6.1 OrderSearch DTO
   - 6.2 방법 1: String JPQL
   - 6.3 방법 2: JPA Criteria API
   - 6.4 방법 3: Querydsl
   - 6.5 동적 쿼리 방법 3가지 비교표
7. [Best Practice 및 주의사항](#7-best-practice-및-주의사항)
8. [부록](#8-부록)

---

## 1. 주문·주문상품 엔티티 개발

### 1.1 엔티티 관계 설계

주문 도메인은 **Order**, **OrderItem**, **Delivery** 세 엔티티를 중심으로 구성됩니다.
회원(Member)과 상품(Item)은 챕터 4·5에서 이미 개발된 도메인입니다.

```
Member ─── (N:1) ──→ Order ←── (1:1) ──→ Delivery
                       │
                  (1:N) ↓
                    OrderItem ──→ (N:1) Item
```

| 연관관계 | 매핑 | FK 위치 | 지연 로딩 |
|----------|------|---------|---------|
| Order → Member | `@ManyToOne` | `order.member_id` | `LAZY` |
| Order → OrderItem | `@OneToMany` | `order_item.order_id` | (컬렉션) |
| Order → Delivery | `@OneToOne` | `order.delivery_id` | `LAZY` |
| OrderItem → Item | `@ManyToOne` | `order_item.item_id` | `LAZY` |

---

### 1.2 테이블 이름 `orders` 사용 이유

```java
@Entity
@Table(name = "orders")   // ← 여기
public class Order { ... }
```

`order`는 SQL 예약어(ORDER BY의 ORDER)이기 때문에, 그대로 테이블 이름으로 사용하면
DB에 따라 충돌이 발생할 수 있습니다. `@Table(name = "orders")`로 명시하여 충돌을 방지합니다.

---

### 1.3 연관관계 편의 메서드

양방향 연관관계에서는 양쪽 모두를 설정해야 합니다.
이를 한 번의 호출로 처리하도록 **연관관계 편의 메서드**를 정의합니다.

```java
// Order.java

// Member ←→ Order 양방향 설정
public void setMember(Member member) {
    this.member = member;
    member.getOrders().add(this);   // 반대쪽도 함께 설정
}

// Order ←→ OrderItem 양방향 설정
public void addOrderItem(OrderItem orderItem) {
    orderItems.add(orderItem);
    orderItem.setOrder(this);       // 반대쪽도 함께 설정
}

// Order ←→ Delivery 양방향 설정
public void setDelivery(Delivery delivery) {
    this.delivery = delivery;
    delivery.setOrder(this);        // 반대쪽도 함께 설정
}
```

**편의 메서드 없이 양방향 설정하는 경우:**

```java
// ❌ 편의 메서드 없이 — 매번 양쪽을 따로 설정해야 함
member.getOrders().add(order);
order.setMember(member);
// → 한 쪽을 빠뜨리면 데이터 불일치 발생
```

**편의 메서드 위치 선택 원칙:**
> 양방향 관계에서 편의 메서드는 **핵심적으로 컨트롤하는 쪽**에 배치합니다.
> 주문 시나리오에서는 Order가 Member, OrderItem, Delivery를 모두 연결하는 중심이므로,
> Order 엔티티에 편의 메서드를 배치합니다.

---

### 1.4 생성 메서드 `Order.createOrder()`

주문 생성은 Order, OrderItem, Delivery를 연결하는 복잡한 과정입니다.
이 로직을 **정적 팩토리 메서드**로 캡슐화합니다.

```java
// === 생성 메서드 === //
public static Order createOrder(Member member, Delivery delivery, OrderItem... orderItems) {
    Order order = new Order();
    order.setMember(member);           // 연관관계 편의 메서드 활용
    order.setDelivery(delivery);       // 연관관계 편의 메서드 활용
    for (OrderItem orderItem : orderItems) {
        order.addOrderItem(orderItem); // 연관관계 편의 메서드 활용
    }
    order.setStatus(OrderStatus.ORDER);
    order.setOrderDate(LocalDateTime.now());
    return order;
}
```

**정적 팩토리 메서드의 장점:**

| 관점 | 설명 |
|------|------|
| **단일 진입점** | 주문 생성 로직이 오직 이 메서드 하나에만 존재 → 수정 시 이 메서드만 바꾸면 됨 |
| **일관성 보장** | 모든 Order 객체가 동일한 초기화 로직을 거침 (status=ORDER, orderDate 자동 설정) |
| **가독성** | `Order.createOrder(member, delivery, orderItem)` — 의도가 명확히 드러남 |

---

### 1.5 protected 생성자

`@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용하면
**외부에서 `new Order()`로 직접 생성하는 것을 컴파일 단계에서 차단**합니다.

```java
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // ← protected 기본 생성자
public class Order {
    ...
    public static Order createOrder(...) { ... }    // ← 유일한 생성 경로
}
```

```java
// ✅ 올바른 생성 — 컴파일 통과
Order order = Order.createOrder(member, delivery, orderItem);

// ❌ 잘못된 생성 — 컴파일 에러 발생
Order order = new Order();  // protected 생성자이므로 외부에서 호출 불가
order.setMember(member);
order.setStatus(OrderStatus.ORDER);
// → 필드 초기화 누락 위험, 생성 로직 분산
```

> `protected` 접근 제어자를 사용하는 이유:
> JPA는 내부적으로 엔티티를 프록시로 생성할 때 기본 생성자가 필요합니다.
> 따라서 완전히 막을 수는 없고, `protected`로 JPA는 허용하되 외부 코드는 차단합니다.

---

### 1.6 비즈니스 로직: `cancel()`, `getTotalPrice()`

```java
// === 비즈니스 로직 === //

/**
 * 주문 취소
 */
public void cancel() {
    if (delivery.getStatus() == DeliveryStatus.COMP) {
        throw new IllegalStateException("이미 배송완료된 상품은 취소가 불가능합니다.");
    }

    this.setStatus(OrderStatus.CANCEL);
    for (OrderItem orderItem : orderItems) {
        orderItem.cancel();     // 각 주문상품의 재고를 원복
    }
}

// === 조회 로직 === //

/**
 * 전체 주문 가격 조회
 */
public int getTotalPrice() {
    int totalPrice = 0;
    for (OrderItem orderItem : orderItems) {
        totalPrice += orderItem.getTotalPrice();    // 가격 × 수량
    }
    return totalPrice;
}
```

| 메서드 | 역할 | 핵심 로직 |
|--------|------|---------|
| `cancel()` | 주문 취소 | 배송 완료 검증 → 상태 CANCEL 변경 → 각 OrderItem.cancel() 호출 |
| `getTotalPrice()` | 전체 가격 계산 | 모든 OrderItem의 `getTotalPrice()` 합산 |

---

### 1.7 OrderItem 엔티티

OrderItem은 쿠폰, 할인 등의 이유로 **실제 주문 시점의 가격(`orderPrice`)** 을 별도로 저장합니다.
현재 상품 가격(`item.getPrice()`)과 다를 수 있기 때문입니다.

```java
// === 생성 메서드 === //
public static OrderItem createOrderItem(Item item, int orderPrice, int count) {
    OrderItem orderItem = new OrderItem();
    orderItem.setItem(item);
    orderItem.setOrderPrice(orderPrice);  // 주문 시점 가격 (할인가 적용 가능)
    orderItem.setCount(count);

    item.removeStock(count);              // 재고 차감 (엔티티 비즈니스 로직)
    return orderItem;
}

// === 비즈니스 로직 === //
public void cancel() {
    getItem().addStock(count);            // 재고 원복
}

// === 조회 로직 === //
public int getTotalPrice() {
    return getOrderPrice() * getCount();  // 주문 가격 × 수량
}
```

**왜 `orderPrice`를 별도로 저장하는가?**

```
주문 시점:   책 가격 = 10,000원  → orderPrice = 10,000 저장
나중에:      책 가격 = 15,000원으로 인상
주문 취소 시: 환불은 10,000원을 기준으로 처리해야 함 (주문 시점 가격)
```

---

### 1.8 전체 코드

**Order.java**

```java
package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.*;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(fetch = LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // 주문 상태 [ORDER, CANCEL]

    // === 연관관계 편의 메서드 === //
    public void setMember(Member member) {
        this.member = member;
        member.getOrders().add(this);
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        delivery.setOrder(this);
    }

    // === 생성 메서드 === //
    public static Order createOrder(Member member, Delivery delivery, OrderItem... orderItems) {
        Order order = new Order();
        order.setMember(member);
        order.setDelivery(delivery);
        for (OrderItem orderItem : orderItems) {
            order.addOrderItem(orderItem);
        }
        order.setStatus(OrderStatus.ORDER);
        order.setOrderDate(LocalDateTime.now());
        return order;
    }

    // === 비즈니스 로직 === //
    public void cancel() {
        if (delivery.getStatus() == DeliveryStatus.COMP) {
            throw new IllegalStateException("이미 배송완료된 상품은 취소가 불가능합니다.");
        }
        this.setStatus(OrderStatus.CANCEL);
        for (OrderItem orderItem : orderItems) {
            orderItem.cancel();
        }
    }

    // === 조회 로직 === //
    public int getTotalPrice() {
        int totalPrice = 0;
        for (OrderItem orderItem : orderItems) {
            totalPrice += orderItem.getTotalPrice();
        }
        return totalPrice;
    }
}
```

**OrderItem.java**

```java
package jpabook.jpashop.domain;

import jakarta.persistence.*;
import jpabook.jpashop.domain.Item.Item;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private int orderPrice; // 주문 가격
    private int count;      // 주문 수량

    // === 생성 메서드 === //
    public static OrderItem createOrderItem(Item item, int orderPrice, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setOrderPrice(orderPrice);
        orderItem.setCount(count);
        item.removeStock(count);    // 재고 차감
        return orderItem;
    }

    // === 비즈니스 로직 === //
    public void cancel() {
        getItem().addStock(count);  // 재고 원복
    }

    // === 조회 로직 === //
    public int getTotalPrice() {
        return getOrderPrice() * getCount();
    }
}
```

**OrderStatus.java**

```java
package jpabook.jpashop.domain;

public enum OrderStatus {
    ORDER, CANCEL
}
```

---

## 2. CascadeType.ALL — 언제 써야 하는가

### 2.1 Cascade 없을 때 vs 있을 때

**Cascade 없을 때:**
```java
// 각 엔티티를 개별적으로 persist 해야 함
em.persist(delivery);
em.persist(orderItem1);
em.persist(orderItem2);
em.persist(order);       // 마지막에 order persist
```

**Cascade 있을 때:**
```java
// order만 persist 하면 연관된 엔티티들이 자동으로 persist됨
orderRepository.save(order);  // delivery, orderItems 모두 자동 저장
```

`CascadeType.ALL`은 **persist, remove, merge, refresh, detach** 모든 연산을 연관 엔티티에 전파합니다.
실무에서 가장 중요한 cascade 효과는 **persist 전파**(저장 시 자동 저장)와 **remove 전파**(삭제 시 자동 삭제)입니다.

---

### 2.2 Cascade 사용 조건 2가지

Cascade를 사용할 수 있는 조건은 다음 두 가지를 **모두 만족**할 때입니다:

| 조건 | 설명 |
|------|------|
| **단독 소유(Private Ownership)** | 해당 엔티티를 오직 이 연관관계의 부모만 참조한다 |
| **동일 생명주기** | 부모가 생성될 때 함께 생성되고, 부모가 삭제될 때 함께 삭제된다 |

---

### 2.3 이번 설계에서 Cascade가 가능한 이유

```java
// Order → OrderItem: CascadeType.ALL 가능
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
private List<OrderItem> orderItems = new ArrayList<>();

// Order → Delivery: CascadeType.ALL 가능
@OneToOne(fetch = LAZY, cascade = CascadeType.ALL)
@JoinColumn(name = "delivery_id")
private Delivery delivery;
```

**OrderItem:**
- Order 외에 다른 엔티티에서 OrderItem을 참조하는 곳이 없다 → **단독 소유 ✅**
- 주문과 함께 생성되고 주문과 함께 삭제된다 → **동일 생명주기 ✅**

**Delivery:**
- Order 외에 다른 엔티티에서 Delivery를 참조하는 곳이 없다 → **단독 소유 ✅**
- 주문과 함께 생성되고 주문과 함께 삭제된다 → **동일 생명주기 ✅**

---

### 2.4 Cascade를 쓰면 안 되는 경우

```
시나리오: Delivery가 여러 시스템에서 공유되는 경우

OrderService ──→ Order ──cascade──→ Delivery ←── ShippingService
                                                      (다른 서비스에서도 참조)
```

만약 Delivery를 OrderService뿐 아니라 ShippingService에서도 독립적으로 관리한다면:

- `order.cancel()` → cascade → **Delivery 자동 삭제 → ShippingService 데이터 손실**
- `order`를 persist할 때 → cascade → **ShippingService가 따로 관리하던 Delivery와 충돌**

이런 경우에는 Cascade 대신 **별도의 Repository를 생성하여 각 엔티티를 독립적으로 persist/remove** 해야 합니다.

---

## 3. 주문 리포지토리 개발

기본 저장 및 단건 조회 메서드를 구현합니다.
`Item`의 `save()`와 달리, `Order`는 항상 새롭게 생성되므로 `merge` 분기 없이 `persist`만 사용합니다.

```java
package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    // 동적 쿼리 메서드는 섹션 6에서 설명
    public List<Order> findAllByString(OrderSearch orderSearch) { ... }
    public List<Order> findAllByCriteria(OrderSearch orderSearch) { ... }
}
```

**`ItemRepository.save()`와 `OrderRepository.save()` 비교:**

| | `ItemRepository.save()` | `OrderRepository.save()` |
|--|------------------------|--------------------------|
| persist/merge 분기 | O (id null 여부로 분기) | X (항상 persist) |
| 이유 | 상품은 수정 폼에서 detach 상태로 넘어올 수 있음 | 주문은 항상 새로 생성됨 |

---

## 4. 주문 서비스 개발

### 4.1 `order()` 메서드 구현 흐름

```
1. 엔티티 조회
   ├── memberRepository.findOne(memberId) → Member 조회
   └── itemRepository.findOne(itemId)     → Item 조회

2. 배송정보 생성
   └── new Delivery(), delivery.setAddress(member.getAddress())

3. 주문상품 생성
   └── OrderItem.createOrderItem(item, item.getPrice(), count)
       └── 내부에서 item.removeStock(count) 재고 차감

4. 주문 생성
   └── Order.createOrder(member, delivery, orderItem)
       └── 연관관계 편의 메서드들 호출

5. 주문 저장 (단 한 번)
   └── orderRepository.save(order)
       └── CascadeType.ALL → delivery, orderItems 자동 저장
```

---

### 4.2 `cancelOrder()` — Dirty Checking

```java
@Transactional
public void cancelOrder(Long orderId) {
    // 주문 엔티티 조회
    Order order = orderRepository.findOne(orderId);
    // 주문 취소
    order.cancel();
    // ← UPDATE 쿼리를 직접 작성하지 않아도 됨!
}
```

`order.cancel()` 내부에서:
1. `order.status = CANCEL` 로 변경 → Order 테이블 UPDATE 발생
2. `orderItem.cancel()` → `item.addStock(count)` → Item 테이블 `stockQuantity` UPDATE 발생

트랜잭션이 종료될 때, JPA가 **변경 감지(Dirty Checking)** 를 수행하여 변경된 필드만 자동으로 UPDATE 쿼리를 날립니다.

---

### 4.3 도메인 모델 패턴 vs 트랜잭션 스크립트 패턴

| 구분 | 도메인 모델 패턴 | 트랜잭션 스크립트 패턴 |
|------|----------------|---------------------|
| 비즈니스 로직 위치 | **엔티티(도메인) 내부** | **서비스 계층** |
| 특징 | 객체 지향적 — 엔티티가 데이터 + 로직을 함께 보유 | 절차적 — 서비스가 데이터를 꺼내 로직을 처리 |
| 예시 (주문 취소) | `order.cancel()` — 1줄 | 서비스에서 if/else로 상태 변경 + 재고 조정 |
| JPA 궁합 | 좋음 (Dirty Checking과 자연스럽게 연동) | 보통 (Update 쿼리를 별도로 고려해야 함) |
| 장점 | 응집도 높음, 서비스가 얇아짐 | 단순 CRUD에서 이해하기 쉬움 |
| 단점 | 도메인 설계 역량 필요 | 서비스가 비대해지고 중복 로직 발생 |

> 한 프로젝트 내에서도 두 패턴을 상황에 맞게 혼용할 수 있습니다.
> 복잡한 비즈니스 로직은 도메인 모델 패턴, 단순 CRUD는 트랜잭션 스크립트 패턴을 선택합니다.

---

### 4.4 JPA Dirty Checking(변경 감지)

Dirty Checking은 JPA의 핵심 기능 중 하나입니다.

```
트랜잭션 시작
    ↓
엔티티 조회 → 영속성 컨텍스트에 스냅샷 저장
    ↓
엔티티 필드 변경 (order.status = CANCEL)
    ↓
트랜잭션 커밋
    ↓
JPA가 현재 엔티티 상태 vs 스냅샷 비교 → 변경 감지
    ↓
변경된 필드에 대해 UPDATE SQL 자동 생성 및 실행
```

**SQL 방식과의 비교:**

```java
// SQL 직접 사용 방식
// → 상태 변경할 때마다 UPDATE 쿼리를 개발자가 직접 작성해야 함
jdbcTemplate.update("UPDATE orders SET status = ? WHERE order_id = ?", "CANCEL", orderId);
jdbcTemplate.update("UPDATE item SET stock_quantity = ? WHERE item_id = ?", newStock, itemId);

// JPA Dirty Checking 방식
// → 엔티티 필드만 바꾸면 JPA가 알아서 UPDATE 쿼리를 생성
order.cancel();   // 이 한 줄로 Order + Item 모두 자동 UPDATE
```

---

### 4.5 전체 코드

```java
package jpabook.jpashop.service;

import jpabook.jpashop.domain.Delivery;
import jpabook.jpashop.domain.Item.Item;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.repository.ItemRepository;
import jpabook.jpashop.repository.MemberRepository;
import jpabook.jpashop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    /**
     * 주문
     */
    @Transactional
    public Long order(Long memberId, Long itemId, int count) {
        // 엔티티 조회
        Member member = memberRepository.findOne(memberId);
        Item item = itemRepository.findOne(itemId);

        // 배송정보 생성
        Delivery delivery = new Delivery();
        delivery.setAddress(member.getAddress());

        // 주문상품 생성
        OrderItem orderItem = OrderItem.createOrderItem(item, item.getPrice(), count);

        // 주문 생성
        Order order = Order.createOrder(member, delivery, orderItem);

        // 주문 저장 (CascadeType.ALL → delivery, orderItems 자동 저장)
        orderRepository.save(order);
        return order.getId();
    }

    /**
     * 주문 취소
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findOne(orderId);
        order.cancel();   // Dirty Checking → 자동 UPDATE
    }
}
```

---

## 5. 주문 기능 테스트

### 5.1 통합 테스트 vs 단위 테스트

이번 테스트는 `@SpringBootTest`를 사용한 **통합 테스트**입니다.

| 구분 | 단위 테스트 | 통합 테스트 (이번) |
|------|------------|-----------------|
| 범위 | 특정 메서드/클래스 단독 | Spring 컨텍스트 + DB 포함 전체 흐름 |
| 속도 | 빠름 | 느림 |
| 의존성 | 없음 (Mock 활용) | Spring, JPA, H2 DB 모두 필요 |
| 목적 | 로직 자체 검증 | JPA 연동이 실제로 잘 동작하는지 검증 |

> **더 좋은 테스트는 단위 테스트입니다.**
> `removeStock()` 같은 비즈니스 로직은 단위 테스트로 충분히 검증할 수 있습니다.
> 여기서는 JPA와의 연동(Cascade, Dirty Checking)을 함께 검증하는 것이 목적이므로 통합 테스트를 사용합니다.

---

### 5.2 테스트 1: 상품주문()

```java
@Test
public void 상품주문() throws Exception {
    // given
    Member member = createMember();
    Book book = createBook("시골 JPA", 10000, 10);
    int orderCount = 2;

    // when
    Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

    // then
    Order getOrder = orderRepository.findOne(orderId);

    assertEquals(OrderStatus.ORDER, getOrder.getStatus(), "상품 주문시 상태는 ORDER");
    assertEquals(1, getOrder.getOrderItems().size(), "주문한 상품 종류 수가 정확해야 한다.");
    assertEquals(10000 * orderCount, getOrder.getTotalPrice(), "주문 가격은 가격 * 수량이다.");
    assertEquals(8, book.getStockQuantity(), "주문 수량만큼 재고가 줄어야 한다.");
}
```

**4가지 검증 항목:**

| 검증 항목 | 기대값 | 의미 |
|----------|--------|------|
| `getOrder.getStatus()` | `OrderStatus.ORDER` | 주문 완료 상태 확인 |
| `getOrder.getOrderItems().size()` | `1` | 주문 상품 종류 수 확인 |
| `getOrder.getTotalPrice()` | `20,000` (10000 × 2) | 총 주문 금액 확인 |
| `book.getStockQuantity()` | `8` (10 - 2) | 재고 차감 확인 |

---

### 5.3 테스트 2: 상품주문_재고수량초과()

```java
@Test
public void 상품주문_재고수량초과() throws Exception {
    // given
    Member member = createMember();
    Item item = createBook("시골 JPA", 10000, 10);
    int orderCount = 11;   // 재고(10)보다 많은 수량 주문

    // When, Then
    assertThrows(
        NotEnoughStockException.class,
        () -> orderService.order(member.getId(), item.getId(), orderCount)
    );
}
```

**JUnit5의 `assertThrows` 사용 이유:**

JUnit4에서는 `@Test(expected = ...)` 또는 `fail()` 패턴을 사용했습니다.
JUnit5에서는 `assertThrows()`를 사용하여 **예외 발생 여부와 예외 타입**을 한 번에 검증합니다.

```java
// JUnit4 방식 (JUnit5에서는 동작하지 않음)
// @Test(expected = NotEnoughStockException.class)
// 또는
// orderService.order(...);
// fail("예외가 발생해야 한다.");

// JUnit5 방식 ✅
assertThrows(
    NotEnoughStockException.class,       // 기대하는 예외 타입
    () -> orderService.order(...)        // 예외를 발생시킬 코드 (람다)
);
```

---

### 5.4 테스트 3: 주문취소()

```java
@Test
public void 주문취소() throws Exception {
    // given
    Member member = createMember();
    Book item = createBook("시골 JPA", 10000, 10);
    int orderCount = 2;
    Long orderId = orderService.order(member.getId(), item.getId(), orderCount);

    // when
    orderService.cancelOrder(orderId);

    // then
    Order getOrder = orderRepository.findOne(orderId);

    assertEquals(OrderStatus.CANCEL, getOrder.getStatus(), "주문 취소시 상태는 CANCEL 이다.");
    assertEquals(10, item.getStockQuantity(), "주문이 취소된 상품은 그만큼 재고가 증가해야 한다.");
}
```

**검증 포인트:**

| 검증 항목 | 기대값 | 의미 |
|----------|--------|------|
| `getOrder.getStatus()` | `OrderStatus.CANCEL` | 취소 상태 변경 확인 |
| `item.getStockQuantity()` | `10` (원복) | 재고 복구 확인 (Dirty Checking으로 자동 UPDATE) |

---

### 5.5 헬퍼 메서드 패턴

테스트 데이터 생성 로직을 `private` 헬퍼 메서드로 분리합니다.

```java
private Member createMember() {
    Member member = new Member();
    member.setName("회원1");
    member.setAddress(new Address("서울", "강가", "123-123"));
    em.persist(member);
    return member;
}

private Book createBook(String name, int price, int stockQuantity) {
    Book book = new Book();
    book.setName(name);
    book.setPrice(price);
    book.setStockQuantity(stockQuantity);
    em.persist(book);
    return book;
}
```

**헬퍼 메서드 분리 효과:**
- 각 테스트의 **given 블록이 간결**해져 테스트 의도를 빠르게 파악할 수 있음
- 테스트 데이터 생성 방식이 변경될 때 **헬퍼 메서드 하나만 수정**하면 됨
- `ctrl + alt + m` (IntelliJ 단축키)으로 코드 블록을 메서드로 추출 가능

---

### 5.6 전체 코드

```java
package jpabook.jpashop.service;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Item.Book;
import jpabook.jpashop.domain.Item.Item;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.exception.NotEnoughStockException;
import jpabook.jpashop.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class OrderServiceTest {

    @Autowired EntityManager em;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;

    @Test
    public void 상품주문() throws Exception {
        Member member = createMember();
        Book book = createBook("시골 JPA", 10000, 10);
        int orderCount = 2;

        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        Order getOrder = orderRepository.findOne(orderId);

        assertEquals(OrderStatus.ORDER, getOrder.getStatus(), "상품 주문시 상태는 ORDER");
        assertEquals(1, getOrder.getOrderItems().size(), "주문한 상품 종류 수가 정확해야 한다.");
        assertEquals(10000 * orderCount, getOrder.getTotalPrice(), "주문 가격은 가격 * 수량이다.");
        assertEquals(8, book.getStockQuantity(), "주문 수량만큼 재고가 줄어야 한다.");
    }

    @Test
    public void 상품주문_재고수량초과() throws Exception {
        Member member = createMember();
        Item item = createBook("시골 JPA", 10000, 10);
        int orderCount = 11;

        assertThrows(
            NotEnoughStockException.class,
            () -> orderService.order(member.getId(), item.getId(), orderCount)
        );
    }

    @Test
    public void 주문취소() throws Exception {
        Member member = createMember();
        Book item = createBook("시골 JPA", 10000, 10);
        int orderCount = 2;
        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);

        orderService.cancelOrder(orderId);

        Order getOrder = orderRepository.findOne(orderId);
        assertEquals(OrderStatus.CANCEL, getOrder.getStatus(), "주문 취소시 상태는 CANCEL 이다.");
        assertEquals(10, item.getStockQuantity(), "주문이 취소된 상품은 그만큼 재고가 증가해야 한다.");
    }

    private Member createMember() {
        Member member = new Member();
        member.setName("회원1");
        member.setAddress(new Address("서울", "강가", "123-123"));
        em.persist(member);
        return member;
    }

    private Book createBook(String name, int price, int stockQuantity) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        em.persist(book);
        return book;
    }
}
```

---

## 6. 주문 검색 기능 개발 — 동적 쿼리

### 6.1 OrderSearch DTO

검색 조건을 담는 데이터 전송 객체입니다.

```java
package jpabook.jpashop.repository;

import jpabook.jpashop.domain.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderSearch {

    private String memberName;      // 회원 이름
    private OrderStatus orderStatus; // 주문 상태 [ORDER, CANCEL]
}
```

**동적 쿼리의 핵심 문제:**
`memberName`이 null이면 회원 이름 조건 없이 전체 조회,
`orderStatus`가 null이면 상태 조건 없이 전체 조회해야 합니다.
즉, 입력값에 따라 WHERE 절이 달라지는 **동적 쿼리**가 필요합니다.

---

### 6.2 방법 1: String 기반 JPQL (`findAllByString`)

```java
public List<Order> findAllByString(OrderSearch orderSearch) {
    String jpql = "select o from Order o join o.member m";
    boolean isFirstCondition = true;

    // 주문 상태 검색
    if (orderSearch.getOrderStatus() != null) {
        if (isFirstCondition) {
            jpql += " where";
            isFirstCondition = false;
        } else {
            jpql += " and";
        }
        jpql += " o.status = :status";
    }

    // 회원 이름 검색
    if (StringUtils.hasText(orderSearch.getMemberName())) {
        if (isFirstCondition) {
            jpql += " where";
            isFirstCondition = false;
        } else {
            jpql += " and";
        }
        jpql += " m.name like :name";
    }

    TypedQuery<Order> query = em.createQuery(jpql, Order.class)
            .setMaxResults(1000);

    if (orderSearch.getOrderStatus() != null) {
        query = query.setParameter("status", orderSearch.getOrderStatus());
    }
    if (StringUtils.hasText(orderSearch.getMemberName())) {
        query = query.setParameter("name", orderSearch.getMemberName());
    }

    return query.getResultList();
}
```

**문제점:**
- 문자열을 직접 이어 붙이기 때문에 **공백, 오타 등의 버그 위험**이 높음
- 조건이 늘어날수록 코드가 기하급수적으로 복잡해짐
- 실제 코드에서 `jpql += "o.status = :status"` 앞에 공백이 빠져 있어 버그 발생 가능 (실제 소스에도 존재하는 버그)

---

### 6.3 방법 2: JPA Criteria API (`findAllByCriteria`)

```java
public List<Order> findAllByCriteria(OrderSearch orderSearch) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Order> cq = cb.createQuery(Order.class);
    Root<Order> o = cq.from(Order.class);
    Join<Object, Object> m = o.join("member", JoinType.INNER);

    List<Predicate> criteria = new ArrayList<>();

    // 주문 상태 검색
    if (orderSearch.getOrderStatus() != null) {
        Predicate status = cb.equal(o.get("status"), orderSearch.getOrderStatus());
        criteria.add(status);
    }

    // 회원 이름 검색
    if (StringUtils.hasText(orderSearch.getMemberName())) {
        Predicate name = cb.like(m.<String>get("name"), "%" + orderSearch.getMemberName() + "%");
        criteria.add(name);
    }

    cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
    TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000);
    return query.getResultList();
}
```

**치명적인 단점:**
- JPA 표준 스펙이지만 **코드를 보고 어떤 SQL이 생성될지 머릿속에서 그려지지 않음**
- 간단한 쿼리도 장황한 코드가 필요 → **유지보수성이 사실상 제로**
- 실무에서 경험자들이 도저히 유지보수가 안 된다고 포기한 방식

---

### 6.4 방법 3: Querydsl

```java
// 이번 챕터에서는 미구현 — 다음 프로젝트에서 적용
// 미리 살펴볼 코드 예시:
public List<Order> findAll(OrderSearch orderSearch) {
    return queryFactory
        .selectFrom(order)
        .join(order.member, member)
        .where(
            statusEq(orderSearch.getOrderStatus()),
            nameLike(orderSearch.getMemberName())
        )
        .limit(1000)
        .fetch();
}

private BooleanExpression statusEq(OrderStatus statusCond) {
    if (statusCond == null) return null;
    return order.status.eq(statusCond);
}

private BooleanExpression nameLike(String nameCond) {
    if (!StringUtils.hasText(nameCond)) return null;
    return member.name.like(nameCond);
}
```

**Querydsl의 장점:**
- 자바 코드로 쿼리 작성 → **컴파일 타임에 오류 감지**
- 코드가 JPQL처럼 읽혀 **가독성과 유지보수성이 뛰어남**
- 동적 쿼리 조건을 메서드로 분리 → **재사용 가능**

> 실무에서는 **Spring Boot + Spring Data JPA + Querydsl** 조합이 표준으로 사용됩니다.
> 자세한 내용은 "실전! 스프링 부트와 JPA 활용 2" 강의에서 다룹니다.

---

### 6.5 동적 쿼리 방법 3가지 비교표

| 방법 | 타입 안전성 | 가독성 | 유지보수성 | 실무 사용 |
|------|-----------|--------|----------|---------|
| **String JPQL** | ❌ 런타임 오류 | ❌ 문자열 이어붙이기 | ❌ 복잡하고 버그 위험 | 단순한 경우만 |
| **JPA Criteria API** | ✅ 컴파일 타임 | ❌ 매우 읽기 어려움 | ❌ 사실상 불가 | ❌ 거의 사용 안 함 |
| **Querydsl** | ✅ 컴파일 타임 | ✅ JPQL처럼 읽힘 | ✅ 조건 메서드 분리 | ✅ 실무 표준 |

---

## 7. Best Practice 및 주의사항

### ✅ 생성 메서드로 복잡한 객체 생성 캡슐화

```java
// ✅ 올바른 방법 — 단일 진입점으로 생성
Order order = Order.createOrder(member, delivery, orderItem);

// ❌ 잘못된 방법 — 생성 로직이 분산됨
Order order = new Order();
order.setMember(member);
order.setDelivery(delivery);
order.setStatus(OrderStatus.ORDER);
// → orderDate 설정 누락, addOrderItem 호출 누락 위험
```

### ✅ protected 생성자로 잘못된 생성 차단

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order { ... }
// → 컴파일 단계에서 new Order() 직접 생성 불가
```

### ✅ 비즈니스 로직을 엔티티에 (도메인 모델 패턴)

```java
// ✅ 올바른 방법 — 엔티티가 비즈니스 로직 담당
order.cancel();                     // 1줄로 모든 처리
item.removeStock(count);            // 재고 검증 + 차감 캡슐화

// ❌ 잘못된 방법 — 서비스에서 직접 상태 조작
order.setStatus(OrderStatus.CANCEL);  // 검증 로직 누락 위험
item.setStockQuantity(item.getStockQuantity() - count);  // 음수 방지 로직 없음
```

### ✅ Cascade는 단독 소유 엔티티에만 적용

| 적용 가능 | 적용 불가 |
|----------|---------|
| OrderItem (Order만 참조) | 다른 서비스에서도 사용하는 공유 엔티티 |
| Delivery (Order만 참조) | 여러 Aggregate에서 참조하는 엔티티 |

### ❌ Criteria API 실무 사용 — 유지보수 불가

```java
// ❌ Criteria API — 어떤 쿼리가 생성될지 읽기 어려움
Predicate status = cb.equal(o.get("status"), orderSearch.getOrderStatus());

// ✅ Querydsl — SQL처럼 읽혀 의도가 명확
order.status.eq(orderSearch.getOrderStatus())
```

### ⚠️ `@Enumerated(EnumType.STRING)` 필수

```java
// ❌ ORDINAL (기본값) — 숫자로 저장 → 중간에 새 상태 추가 시 데이터 깨짐
@Enumerated(EnumType.ORDINAL)   // ORDER=0, CANCEL=1 → 위험

// ✅ STRING — 문자열로 저장 → 안전
@Enumerated(EnumType.STRING)    // ORDER="ORDER", CANCEL="CANCEL"
```

---

## 8. 부록

### 8.1 프로젝트 구조 (챕터 6 추가분)

```
src/main/java/jpabook/jpashop/
├── domain/
│   ├── Order.java              ← 주문 엔티티 (생성 메서드, 연관관계 편의 메서드, 비즈니스 로직)
│   ├── OrderItem.java          ← 주문상품 엔티티 (생성 메서드, cancel, getTotalPrice)
│   ├── OrderStatus.java        ← 주문 상태 열거형 (ORDER, CANCEL)
│   ├── Delivery.java           ← 배송 엔티티
│   ├── DeliveryStatus.java     ← 배송 상태 (READY, COMP)
│   ├── Member.java             ← (챕터 4에서 개발)
│   └── Item/                   ← (챕터 5에서 개발)
├── repository/
│   ├── OrderRepository.java    ← save, findOne, findAllByString, findAllByCriteria
│   └── OrderSearch.java        ← 검색 조건 DTO
└── service/
    └── OrderService.java       ← order(), cancelOrder()

src/test/java/jpabook/jpashop/service/
└── OrderServiceTest.java       ← 3개 통합 테스트
```

---

### 8.2 핵심 용어 정리

| 용어 | 설명 |
|------|------|
| **연관관계 편의 메서드** | 양방향 연관관계를 한 번의 호출로 양쪽에 설정하는 헬퍼 메서드 |
| **정적 팩토리 생성 메서드** | `public static` 메서드로 객체 생성 로직을 캡슐화하는 패턴 |
| **CascadeType.ALL** | persist, remove 등 영속성 연산을 연관 엔티티에 전파하는 JPA 설정 |
| **Dirty Checking (변경 감지)** | 트랜잭션 커밋 시 영속성 컨텍스트가 스냅샷과 현재 상태를 비교하여 변경된 필드를 자동으로 UPDATE하는 JPA 메커니즘 |
| **도메인 모델 패턴** | 비즈니스 로직을 서비스가 아닌 엔티티(도메인) 내부에 배치하는 아키텍처 패턴 |
| **동적 쿼리** | 입력 조건에 따라 WHERE 절이 달라지는 쿼리 |
| **Querydsl** | 타입 안전한 방식으로 JPA 동적 쿼리를 작성할 수 있는 라이브러리 |

---

### 8.3 어노테이션 정리

| 어노테이션 | 패키지 | 역할 |
|-----------|--------|------|
| `@Table(name = "orders")` | `jakarta.persistence` | 엔티티가 매핑될 테이블 이름 명시 (예약어 충돌 방지) |
| `@NoArgsConstructor(access = PROTECTED)` | `lombok` | protected 기본 생성자 자동 생성 — JPA 허용, 외부 직접 생성 차단 |
| `@Enumerated(EnumType.STRING)` | `jakarta.persistence` | Enum을 DB에 문자열로 저장 (ORDINAL은 위험) |
| `@OneToMany(cascade = CascadeType.ALL)` | `jakarta.persistence` | 영속성 연산 전파 설정 |
| `@OneToOne(fetch = LAZY)` | `jakarta.persistence` | 1:1 관계 지연 로딩 설정 |
| `@ManyToOne(fetch = LAZY)` | `jakarta.persistence` | N:1 관계 지연 로딩 설정 (기본값은 EAGER — 반드시 LAZY로 변경) |
| `@SpringBootTest` | `org.springframework.boot.test` | 전체 Spring 컨텍스트를 로드하는 통합 테스트 |

---

### 8.4 학습 점검 Q&A

**Q1. 연관관계 편의 메서드가 필요한 이유는 무엇인가요?**

양방향 연관관계에서 한쪽만 설정하면 데이터 불일치가 발생할 수 있습니다.
`order.setMember(member)` 안에 `member.getOrders().add(this)`를 포함시켜,
한 번의 호출로 양방향이 정상적으로 설정되도록 보장합니다.

**Q2. `@NoArgsConstructor(access = PROTECTED)`를 쓰는 이유는?**

`createOrder()` 같은 생성 메서드만을 통해 객체를 생성하도록 강제합니다.
`private`이 아닌 `protected`를 쓰는 이유는, JPA가 프록시 생성 시 기본 생성자를 필요로 하기 때문입니다.

**Q3. `CascadeType.ALL`을 쓰면 안 되는 경우는 언제인가요?**

해당 엔티티가 여러 곳에서 공유 참조되는 경우입니다.
Order 외에 다른 서비스에서도 Delivery를 관리한다면, Order를 삭제할 때 cascade로 Delivery가 함께 삭제되어 데이터 손실이 발생할 수 있습니다.

**Q4. `cancelOrder()`에서 UPDATE 쿼리를 직접 작성하지 않아도 되는 이유는?**

JPA의 Dirty Checking(변경 감지) 덕분입니다.
트랜잭션 내에서 조회된 엔티티의 필드가 변경되면, 트랜잭션 커밋 시 JPA가 스냅샷과 비교하여 변경된 필드에 대한 UPDATE 쿼리를 자동으로 생성하고 실행합니다.

**Q5. 동적 쿼리에서 Querydsl이 권장되는 이유는?**

String JPQL은 오타·공백 버그 위험이 있고, Criteria API는 유지보수가 거의 불가능합니다.
Querydsl은 자바 코드로 쿼리를 작성하여 컴파일 타임에 오류를 잡을 수 있고,
조건 메서드를 분리하여 재사용 가능하며, 코드 가독성이 뛰어납니다.

---

### 8.5 다음 학습 단계 — 챕터 7: 웹 계층 개발

챕터 7에서는 Thymeleaf 뷰 템플릿을 사용하여 **웹 인터페이스**를 개발합니다.
챕터 4~6에서 구현한 회원·상품·주문 도메인을 사용자가 브라우저에서 직접 조작할 수 있게 됩니다.

| 개념 | 설명 |
|------|------|
| **Thymeleaf** | 스프링과 자연스럽게 통합되는 서버사이드 뷰 템플릿 엔진 |
| **상품 수정 — 변경 감지 vs merge** | `em.merge()` 의 문제점과 Dirty Checking 방식의 상품 수정 구현 |
| **준영속 엔티티** | 영속성 컨텍스트에서 분리된 상태의 엔티티 처리 방법 |
| **주문 목록 · 검색** | `OrderSearch`를 활용한 웹 검색 폼 구현 |

챕터 5에서 예고한 `em.merge()`의 위험성과 올바른 대안(변경 감지)을 이번 챕터에서 본격적으로 학습합니다.
