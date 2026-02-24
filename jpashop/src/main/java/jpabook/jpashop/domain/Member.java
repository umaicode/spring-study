package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String name;

    @Embedded
    private Address address;

    // 멤버의 입장에서 리스트는 하나의 회원이 여러개의 상품을 주문하기 때문에 일대다관계
    // 나는 연관관계의 거울이다. -> mappedBy -> 여기에 뭔가 값을 넣는다고해서 foreign key값이 변경되지 않는다.
    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();
}
