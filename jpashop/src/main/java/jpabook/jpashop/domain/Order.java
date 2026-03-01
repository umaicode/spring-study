package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.*;

@Entity
@Table(name = "orders")
@Getter @Setter
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
}
