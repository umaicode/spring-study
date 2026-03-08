package jpabook.jpashop_2.api;

import jpabook.jpashop_2.domain.Order;
import jpabook.jpashop_2.repository.OrderRepository;
import jpabook.jpashop_2.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * xToOne(ManyToOne, OneToOne)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {
    // 이거 왜 오류날까?
    // orders -> member -> orders -> member -> ... 무한루프
    // 양방향 연관관계의 문제가 발생한다.
    // 양방향이 걸리는데를 다 @JsonIgnore 해주어야 한다.
    // 그럼에도 문제가 또 발생한다.
    // 지연로딩: DB에서 안끌고 온다.
    // -> DB에서 가져올 때는 order의 데이터만 가지고 온다. -> lazy로 되어있기 때문에 Member에 아예 손을 안댄다.
    // -> 그렇다고 member에 null을 넣을 수는 없으니 hibernate에서는 new Member()해가지고 가짜 proxy 멤버 객체를 생성해서 놓어놓는다. -> ByteBuddy
    // 아무튼, proxy 객체를 가짜로 넣어놓고 뭔가 Member 객체 값을 꺼내거나 딱 손대면 그 때 DB에 Member 객체 SQL을 날려가지고 이 Member 객체 값을 가져와서 채워 준다고 이해하면 된다.
    // -> 이것을 proxy를 초기화한다고 한다.

    // hibernate5module을 설치해야 한다.
    // 근데 왜 난 spring4.x랑 hibernate쓰는데 왜 500 error가 발생안하고 잘 동작하지?

    // API 스펙 추가 노출 + 성능 상 문제(ITEM, CATEGORY 별의 별거 다가져와야 하니깐.) 어마어마한 쿼리가 나가고 있다.
    // API 만들 때 이렇게 복잡하게 안만든다.
    // 가급적이면 필요한 데이터만 API에 노출해야 한다.

    // @ManyToOne에서 fetch = eager로 바꾸는 사람 많은데 이러면 안된다.
    // 물론 이렇게 하면 for문 필요 없어진다.
    // findAll해서 JPQL 날릴 떄가 아니라 em.find()할 때나 성능 최적화가 가능하지 이경우에는 오더만 가져오기 때문에 쿼리 다 날려 버린다.
    // 쿼리의 결과는 오더만 가져오는 거였고, 오더해보니깐 eager네? 레이지 로딩하는거랑 똑같이 영속성 컨텍스트에서 단건으로 조회하면서 조회한다. -> N + 1 문제
    // EAGER로 해두면 또 다른 API에서 문제다.
    // 다른데서 Member가 필요 없어도 일단 EAGER여서 다 긁어온다.
    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            // 멤버 쿼리를 날려서 JPA가 이 데이터를 다 끌고 온다.
            order.getMember().getName();    // Lazy 강제 초기화
            order.getDelivery().getAddress();    // Lazy 강제 초기화
        }
        return all;
    }
}
