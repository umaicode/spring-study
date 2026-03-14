package jpabook.jpashop_2.repository.order.query;

import jpabook.jpashop_2.domain.Address;
import jpabook.jpashop_2.domain.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderFlatDto {

    // 귀찮으니깐 오더랑 오더아이템을 조인해서 가져온다.
    // 오더아이템이랑 아이템 조인해서 가져온다.
    // 데이터가 한 줄로 플랫하게 SQL 조인의 결과를 가져올 수 있도록 데이터 구조를 맞춰야 한다.

    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    private String itemName;
    private int orderPrice;
    private int count;

    public OrderFlatDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address, String itemName, int orderPrice, int count) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
        this.itemName = itemName;
        this.orderPrice = orderPrice;
        this.count = count;
    }
}
