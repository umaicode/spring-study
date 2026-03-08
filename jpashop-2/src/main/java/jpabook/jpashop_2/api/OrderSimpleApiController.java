package jpabook.jpashop_2.api;

import jpabook.jpashop_2.domain.Address;
import jpabook.jpashop_2.domain.Order;
import jpabook.jpashop_2.domain.OrderStatus;
import jpabook.jpashop_2.repository.OrderRepository;
import jpabook.jpashop_2.repository.OrderSearch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    // v1과 v2 둘다 가지고 있는 문제가 있다.
    // -> 레이지 로딩으로 인한 데이터베이스 쿼리가 너무 많이 호출되는 문제
    // 첫번째 주문서는 쿼리 3번으로 완성이 된다.
    // 두번째 주문서도 member랑 delivery를 가지고 온다.
    // ORDER -> SQL 1번 -> 결과 주문수 2개
    // 처음 돌 때 SimpleOrderDto에서 name 가져오고 address 를 또 가져온다.
    // 즉, 총 1 + 2 + 2 = 5개가 나간다.
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        // ORDER 2개
        // N + 1 -> 1 + 회원 N(2) + 배송 N(2)
        // EAGER로 바꾸면 해결될까?
        // -> 안된다. 그리고 실무에서는 EAGER쓰면 안됨을 강사는 깨달았다.
        // EAGER 걸리면 예측이 안된다.
        // 일단 오더를 가지고 온다.(EAGER)
        // 어 EAGER가 있네?(MEMBER, DELIVERY)
        // 다 가져와야지 이러고 있다.
        // -> 모든 연관관계는 LAZY로 해두어야 한다. 필요하면 다음 시간에 말할 패치 조인이라는 걸로 튜닝해야 한다.

        // 같은 멤버를 조회하게 되면 어떻게 될까?
        // 처음에 오더가 나간다. -> 오더가 2개가 나온다.
        // 루프를 돌고 SimpleOrderDto 2번 호출
        // 처음에는 영속성 컨텍스트가 없기 때문에 Member를 가져온다.(userA)
        // 두번째 돌 때 Delivery 가져온다.
        // 영속성 컨텍스트에 가봤는데 없으니까 영속성 컨텍스트 DB에서 가져와서 초기화시켜주는 거다.
        // 어? 영속성 컨텍스트에 두번째 Member를 가져오라고 했는데 userA가 이미 있다!
        // 그렇기 때문에 쿼리를 안날리고 그냥 있는 거 가져온다.
        // Member 조회 안하고 Delivery만 조회.
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        // 2개면 결과적으로 2번 loop를 돈다.
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    // 쿼리가 한번 나간다. -> 기가 막힌다!
    // 이거 SQL로 한땀한땀 다 짜고 결국 가져와서 이거 맵핑하려면 진짜 한세월이다.
    // 지연로딩 자체가 일어나지 않는다.
    // 진짜 Member 객체랑 Delivery 객체가 여기 Order에 같이 조회가 되서 나온다.
    // 레이지 로딩 고민 안해도 된다.
    // 실무에서 정말 자주 사용하는 기법이다.
    // -> 실무에서 생각보다 객체 그래프를 자주 사용하는게 보통 정해져 있다.
    // -> 보통 주문서 할 때는 회원이랑 Delivery 정보를 같이 쓴다.
    // -> 이런게 전제가 되기 때문에 아예 그냥 하나를 찍어 놓고 findAll로 쓰기도 한다.
    // [단점]
    // 엔티티를 찍어서 줘야하는게 단점. -> 이거 최적화 다음 시간
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
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

        // entity 에서 name -> username으로 바꿔도 이제 오류를 잡을 수 있다.
        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // LAZY 초기화: 영속성 컨텍스트가 이 memberId를 가지고 영속성 컨텍스트를 찾아본다. 없으면 db쿼리 날린다. -> 데이터 긁어온다.
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // LAZY 초기화
        }
    }
}
