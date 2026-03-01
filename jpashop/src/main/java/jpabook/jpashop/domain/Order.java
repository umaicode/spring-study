package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.cglib.core.Local;

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

    // order랑 member는 다대일 관계이다.
    // mapping을 뭘로 할꺼냐? -> JoinColumn(name = "member_id") -> foreign key 이름이 member_id
    // 얘가 연관관계의 주인이 되어야 한다. -> 이 부분은 좀 더 확실하게 이해가 필요할 것 같다!
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // cascade: orderItems에 데이터를 넣어두고 order를 저장하면 orderItems도 같이 저장이 된다.
    // 원래는 3개를 저장한다하면 persist 3번하고 persist(order)해야 한다.
    // cascade 하게 되면 persist(order)만 해도 나머지 3개가 자동으로 된다. (cascade는 persist 전파)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    // order를 저장할 때 Delivery Entity도 같이 persist 해준다.
    // 원래대로라면 각각 persist 해줘야 한다.
    // 모든 엔티티는 기본적으로 persist를 다 저장하고 싶으면 각자 해줘야 한다.
    // cascade를 활용하면 delivery에 값만 세팅해두면 다 persist 해준다.
    @OneToOne(fetch = LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // 주문 상태 [ORDER, CANCEL]

    // === 연관관계 편의메서드 === //
    // 연관관계 편의 메서드의 위치는 양쪽이 있으면 어디가 좋냐?
    // 핵심적으로 컨트롤하는 쪽이 들고 있는 것이 좋다.
    public void setMember(Member member) {
        this.member = member;
        member.getOrders().add(this);
    }

//    public static void main(String[] args) {
//        Member member = new Member();
//        Order order = new Order();
//
//        member.getOrders().add(order);
//        order.setMember(member);
//    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        delivery.setOrder(this);
    }

    // 주문 생성하는 것은 복잡하다.
    // order만 생성해서 될 게 아니라 오더 아이템도 있어야 되고 딜리버리.. 여러개 연관관계가 들어가 복잡해진다.
    // 이 때는 별도의 생성 메서드가 있는게 좋다.
    // Q. 이런 스타일로 작성하는게 왜 중요할까?
    // A. 앞으로 뭔가 생성하는 지점을 변경하게 되면 이것만 바꾸면 된다. 이것저것 찾아다닐 필요가 없다.
    // 생성 메서드의 경우 실무에서는 훨씬 복잡하다.
    // orderItem 도 이렇게 넘어오는 것이 아니라 뭔가 파라미터나 DTO(데이터 전송 객체)가 막 넘어오면서 더 복잡하게 넘어온다.
    // 요 안에서도 orderItem을 아예 생성을 해서 넣을 수도 있다. 상황에 따라서는 그게 더 깔끔한 방법이 될 수도 있다.
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
    /**
     * 주문 취소
     */
    public void cancel() {
        if (delivery.getStatus() == DeliveryStatus.COMP) {
            throw new IllegalStateException("이미 배송완료된 상품은 취소가 불가능합니다.");
        }

        this.setStatus(OrderStatus.CANCEL);
        for (OrderItem orderItem : orderItems) {
            // 만약 상품을 2개 주문했다면 각각 다 cancel 해주어야 한다.
            orderItem.cancel();
        }
    }

    // === 조회 로직 === //
    /**
     * 전체 주문 가격 조회
     */
    public int getTotalPrice() {
        // 현재 나한테 정보가 없고 orderItem 들을 다 더하면 된다.
        // totalPrice를 0으로 초기화 해놓고 orderItems 루프를 돌면서 orderItem에 있는 totalPrice를 가져온다.
        // 왜? -> 주문할 때 주문 가격과 수량이 있기 때문이다.
        // 스트림이나 람다같은거 활용하면 되게 깔끔하게 작성할 수 있다.
//        return orderItems.stream()
//                .mapToInt(OrderItem::getTotalPrice)
//                .sum();
        int totalPrice = 0;
        for (OrderItem orderItem : orderItems) {
            totalPrice += orderItem.getTotalPrice();
        }
        return totalPrice;
    }

}
