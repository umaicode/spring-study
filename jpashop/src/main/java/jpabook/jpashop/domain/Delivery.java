package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter @Setter
public class Delivery {

    @Id @GeneratedValue
    @Column(name = "delivery_id")
    private Long id;

    // JPA에서는 FK를 1:1일 때 어디에 두어도 상관 없지만
    // 보통 주로 액세스를 많이 하는 곳에 두는 것을 선호한다.
    // 보통 오더를 보면서 딜리버리를 본다고 가정을 한다. -> 딜리버리를 가지고 오더를 찾을 일이 거의 없다고 가정
    // 그래서 오더에다가 FK를 둔다.
    // 그럼 이제 연관관계 주인을 정해야 하는데 그러면 FK랑 가까운 곳에 있는 딜리버리를 연관관계의 주인으로 잡아주면 된다.
    @OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
    private Order order;

    @Embedded
    private Address address;

    // enum 조심히 써야한다. ORDINAL 일 경우 중간에 다른 상태 생기면 망한다.
    // 꼭 STRING으로 넣자
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;  // READY, COMP
}
