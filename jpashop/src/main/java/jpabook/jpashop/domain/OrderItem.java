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
    private int count;  //  주문 수량

    // protected로 만들어 두면 다른 생성자를 막아줄 수 있다. in OrderService
    // @NoArgsConstructor(access = AccessLevel.PROTECTED) 넣으면 이거랑 똑같다.
//    protected OrderItem() {
//    }

    // === 생성 메서드 === //
    // 쿠폰 받거나 할인될 수 도 있기 때문에 orderPrice를 따로 가져가는 것이 맞다.
    public static OrderItem createOrderItem(Item item, int orderPrice, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setOrderPrice(orderPrice);
        orderItem.setCount(count);

        item.removeStock(count);
        return orderItem;
    }

    // === 비즈니스 로직 === //
    public void cancel() {
        // orderItem cancel의 의미: 재고 수량을 원복해준다.
        getItem().addStock(count);
    }

    // === 조회 로직 === //
    /**
     * 주문상품 전체 가격 조회
     */
    public int getTotalPrice() {
        return getOrderPrice() * getCount();
    }
}
