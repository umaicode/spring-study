package jpabook.jpashop_2.api;

import jpabook.jpashop_2.domain.Address;
import jpabook.jpashop_2.domain.Order;
import jpabook.jpashop_2.domain.OrderItem;
import jpabook.jpashop_2.domain.OrderStatus;
import jpabook.jpashop_2.repository.OrderRepository;
import jpabook.jpashop_2.repository.OrderSearch;
import jpabook.jpashop_2.repository.order.query.OrderFlatDto;
import jpabook.jpashop_2.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop_2.repository.order.query.OrderQueryDto;
import jpabook.jpashop_2.repository.order.query.OrderQueryRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());

        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();

            // orderItems와 그 안의 orderItem 정보를 같이 출력하고 싶다.
            // 강제 초기화를 하였다. -> Hibernate5 모듈의 기본 설정 자체가 레이지로딩을 했을 때 프록시를 다 레이지이니까 프록시는 그냥 데이터를 안뿌린다.
            // 아래 코드는 프록시를 강제 초기화하게 되면 아 얘는 데이터가 있으니까 뿌려야지 하고 뿌린다.
            // 물론 여기서도 양방향 관계는 다 보시면 @JsonIgnore 엔티티를 직접 다 내가 출력하는 거기 때문에 양방향 관계는 @JsonIgnore로 꼭 찾아서 다 걸어줘야 한다.
            // item에서는 반대로 orderItems로 가는 레퍼런스를 일부로 안만들었다. -> 양방향에 대해서 고민하지 않아도 된다.
            // 이 방법은 결국 엔티티를 직접 노출하기 때문에 요런 방법은 가급적이면 API 엔티티를 직접 노출하면 안된다.
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
//            for (OrderItem orderItem : orderItems) {
//                orderItem.getItem().getName();
//            }
        }
        return all;
    }

    // 먼저 전체를 조회해와서 Entity를 조회해온 다음에 실무에서는 이걸 페이징하거나 여러가지 한다.
    // 어쨌든 Entity에서 Orders를 조회한다.
    // 그 다음에 이거를 루프로 돌리면서 Order DTO로 변환을 해서 List로 바꾼다.
    // 자 근데 DTO로 변환을 할 떄 이 생성자로 넘긴다.
    // Order에서 쭉 값을 세팅을 하는데 또 하나가 더 있다.
    // OrderItems도 똑같이 한다.
    // 왜냐? Entity가 또 나왔으니깐 또 돌리는 거다.
    // 루프돌려서 OrderItem을 OrderItemDto로 변환을 한다.
    // 그리고 원하는 값들을 세팅을 하고 반환을 하면 된다.

    // SQL 많이 실행된다.
    // -> 지연 로딩이 많아서 너무 많이 실행이 된다.
    // -> 컬렉션 써버리면 query도 더 많이 나간다.
    // -> 최적화에 대해서 사실 더 많이 고민을 해야 한다.
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());

        return result;
    }

    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();

        // 강의에서는 order가 레퍼런스까지 똑같다. -> JPA에서 이 PK 아이디가 똑같으면 완전 똑같은 거다.
        // DB의 참조값까지 똑같다.
        // -> JPA 입장에서는 이걸 뻥튀기를 해서 줘야 될지 아니면 줄여서 줘야 될지를 판단하게 하면 안된다.
        // -> 그래서 distinct 라는 연산을 사용한다. in OrderRepository.java
        // order ref = jpabook.jpashop_2.domain.Order@170628ca id = 1
        // order ref = jpabook.jpashop_2.domain.Order@5889bbf id = 2
        // -> 나는 왜 2개만 나오지?

//        for (Order order : orders) {
//            System.out.println("order ref = " + order + " id = " + order.getId());
//        }

        // 이전에는 쿼리가 거의 10번 가까이 나갔는데 이번에는 1번 나갔다.
        // 차이는 객체 그래프를 탐색하도록 fetch join을 했냐 안했냐의 차이만 있는 거지 코드는 v2랑 똑같다.
        // 실제 운영환경에서는 v2에서 findAllWithItem을 수정하지 v3쪽 코드를 고칠 필요가 없었을 것이다.
        // 그런데 쿼리를 여러 방 나가는게 한 번으로 튜닝이 돼 버린거다. -> 어마어마한거다.
        // 이것을 이전의 코드에서 그냥 내가 직접 SQL을 다 담는다고 가정을 하면 이전처럼 막 쿼리가 막나가는 거를 이렇게 한방 쿼리로 바꾼다?
        // -> 한방 쿼리 다 코드 작성하고 또 이거 전용 DTO 만들고 해야 한다. => 엄청 복잡하다.
        // 근데 JPA의 fetch join을 사용하면 정말 막강하게 이 객체 그래프만 내가 찍어 주면 내가 원하는 것만 다다다 찍어주면 심지어 distinct까지 다 고려해 가지고 최적의 결과를 반환해준다.
        // 쿼리 한방에!
        // 이런 것만 써도 사실 성능이 대부분 어마어마한 해결이 된다.
        // 그런데 생각을 해보면 이전에 심플 버전에서도 똑같았다. 그냥 fetch join 쓰는거 아니야? 근데 왜 컬렉션에서 이야기를 또 하지?
        // 컬렉션 페치 조인의 어마어마한 단점
        // -> DB의 제약 때문에 오는 한계이긴 한데 우선 좋았던건 패치 조인을 사용해서 어쨌든 SQL 한 번만 실행된 건 이제 너무 좋다.
        // -> 근데 페이징이 불가능해진다. (꼭 기억해야 한다.)
        // -> 1대 다를 패치 조인하는 순간 페이징 쿼리가 아예 안나간다.
        // 쿼리를 보면 limit가 안보인다. -> 뭔가 쎄하다.
        // HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory
        // fetch join 을 네가 썼는데 뭔가 페이징 쿼리가 들어갔어? 난 메모리에서 이걸 소팅해버릴거야. -> 메모리에서 페이징 처리를 해버린다는 것이다.
        // -> 지금 소팅이 안되니까 메모리에서 페이징 처리를 지금 해버린 것이다. -> 그래서 경고를 냄
        // 이게 지금 데이터가 몇 건 없어서 그렇지 데이터가 만약에 만개가 있었으면 어떻게 될까?
        // -> 만개를 다 애플리케이션에 퍼 올린 다음에 페이징 처리를 하는 것이다.
        // -> 메모리에서 그러면 아우터 메모리 난다. -> 잘못되면 끝장난다.
        // 왜 이런 극단적인 선택을 하이버네이트가 했냐면 지금 오더는 몇 개인가? -> 우리가 기대하는 오더는 2개다.
        // 이렇게 하면 데이터베이스 쿼리에서는 어떻게 나오냐? -> 4개가 나온다.
        // 1대 다 조인을 해버리는 순간 패치 조인도 마찬가지로 조인이 나가는 거니까 1대 다 조인을 한 순간 오더의 기준 자체가 다 틀어져 버린다.
        // 지금 DB에 4개가 나오는데 만약에 페이징이 값을 2개 끊고 2개만 가져가라고 하면?
        // 그러니까 이게 우리가 원했던 오더에 대한 사이즈가 정확하게 안 나오는 거다.
        // 예를 들어 3개 부터 가져와 라고 하면 3개 지나서 가져오면 마지막에 하나만 선택이 된다.
        // 우리는 오더의 데이터를 기준으로 페이징을 하고 싶은데 다 기준으로 뻥튀기가 되어버리니까 페이징 자체가 불가능해지는 DB SQL 입장에서는 LIMIT OFFSET하면 지금 오더를 기준으로 OFFSET이 적용되는게 아니라 정확히 얘기하면 이 데이터가 뻥튀기 되는 이 N 오더아이템을 기준으로 페이징이 되어버린다.
        // 그러면 이제 오더가 중간에서 데이터를 못맞춘다.
        // 어쩔 수 없이 이런 일대다 패치 조인이 들어가게 되면 Hibernate는 경고를 내고 메모리에서 이걸 해준다.
        // 왜냐하면 데이터가 작은데서는 할 수도 있으니까
        // 딱 보면 알겠지만 이거 쓰면 큰일나겠죠?
        // 그래서 1대 다 패치조엔에서는 페이징을 하면 안된다.
        // 패치 조인을 쓸 때는 1대 다가 아니면 상관없다. -> 멤버나 딜리버리 이런 애들은 그냥 마음껏 해도 된다.
        // 그런데 뭐 오더 아이템을 기준으로 아이템 하는 애도 해도 된다.
        // 근데 중간에 o.orderItems oi가 걸려 들어간다.
        // 얘는 오더 입장에서는 1대 다 패치 조인이기 때문에 데이터가 뻥튀기가 되서 페이징 자체가 이제 불가능해진다.(DB 상으로)
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());

        return result;
    }

    // 오더랑 멤버랑 딜리버리는 투원관계여서 쿼리 1번
    // 오더 아이템스는 쿼리 1번
    // 그 안에 오더 아이템은 총 2개 이기에 쿼리 2번
    // 1 -> N -> M
    // 오더가 한 100개면 성능 안나온다!
    // application.yml에서 batch_size 설정으로 미리 꺼내올 수 있다.
    // size: IN query의 개수
    // 아이템이 총 4번 호출되었던게 한방에 4개를 다 땡겨온다.
    // 아이템이 1000개인데 limit가 100이면 10번 돈다.
    // DB 입장에서 최적화가 잘되는 코드이다.
    // v3는 쿼리 1번에 나가긴했다. -> 한방 쿼리
    // 얘는 이전꺼에 비해서 3번이 나갔지만, 느릴까?
    // 장단점이 있다.
    // v3의 문제는 FETCH JOIN을 하는데 한방에 JOIN 하니깐 중복이 엄청 많다.
    // 오더 같은 경우 다 중복이 된다. 중복데이터가 너무 많은데 DB에서 애플리케이션한테 다 전송한다.
    // 쿼리는 한방에 나가지만 데이터 전송량 자체가 많아진다. -> 일대다 JOIN하면 데이터가 뻥튀기가 된다.
    // 다의 데이터에 맞추어서 1의 데이터를 뻥튀기시킨다.
    // 결과적으로 SQL에 나가기 때문에 용량이 많다.
    // v3.1은 쿼리가 최적화되서 나간다.
    // 첫번째쿼리: 2개 (애플리케이션으로 정확하게 중복 없는 데이터가 전송이 된다) -> 데이터 전송량 자체가 준다.
    // 오더아이템스도 중복없이 정확하게 가져온다.
    // 아이템도 중복없이 정확하게 가져온다.
    // -> 테이블 단위로 IN query를 팍팍 찍어서 가져오기 때문이다.
    // 이전 것이 정교화가 안되어 있다면 이건 이제 정교화된 상태로 데이터를 조회하는거다. 물론 쿼리가 더 나가긴 하지만.
    // 네트워크를 호출하는 횟수랑 전송하는 거 사이에서 트레이드오프가 있다.
    // 단쿼리에 데이터가 많이 없으면 한방 FETCH JOIN으로 날리는게 당연히 성능이 이로운데 데이터가 한번에 1000개씩 퍼올려야 되는 상황이면 한방 FETCH JOIN보다 이게 더 성능이 잘 나올 수 있다.
    // 상황과 데이터 양에 따라 다르다.
    // 그리고 애는 페이징도 가능하다.
    // 강사는 이 방식을 선호한다.
    // 페이징 써야되면 방법이 이 방식밖에 없다.
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit)
    {

        // findAllWithMemberDelivery()는 ToOne 관계를 FETCH JOIN해서 페이징 쿼리를 써도 아무 문제가 없다.
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);



        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());

        return result;
    }

    // Query: 루트 1번, 컬렉션 N번 실행
    // ToOne(N:1, 1:1) 관계들을 먼저 조회하고, ToMany(1:N) 관계는 각각 별도로 처리한다.
    // 이런 방식을 선택한 이유는 다음과 같다.
    // ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다.
    // ToMany(1:N) 관계는 조인하면 row 수가 증가한다.
    // row 수가 증가하지 않는 ToOne 관계는 조인으로 최적화하기 쉬우므로 한번에 조회하고, ToMany 관계는 최적화하기 어려우므로 findOrderItems() 같은 별도의 메서드로 조회한다.
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    @GetMapping("api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    // 쿼리는 1번이지만 JOIN으로 인해서 DB에서 애플리케이션에 전달하는 데이터의 중복 데이터가 추가되는게 단점이다.
    // 상황에 따라서 앞서 봤던 v5보다 더 늘수도 있다.
    // 데이터가 많지 않으면 훨씬 빠르다.
    // 애플리케이션에서 추가작업이 크다. -> 분해를 해야하는 경우
    // 분해를 안하고 FLAT DTO를 뿌리면 되게 편하겠지만 대부분 그런 경우는 없다.
    // 페이징이 불가능하다. -> 오더를 기준으로 페이징 하는 건 안된다.
    // OrderFlatDto나 오더 아이템과 관련되서는 어떻게 페이징을 해볼 수 있겠지만 오더를 기준으로 해서는 안된다.
    // 왜냐하면 DBS 이거 자꾸 데이터 중복이 되버린다.
    // 이미 이렇게 되면은 페이징 해서 정확한 결과가 안나온다.
    // 예를 들어서 페이지를 두 개만 가져가라고 하면 userA만 선택되는 것이다.
    // 오더 아이템을 기준으로 하면 아이템 개수랑 딱 맞기 때문에 지금 페이징할 수 있는데 지금은 안된다.
    @GetMapping("api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        // 참고로 스트림으로 발라내려면?
        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(toList());

    }

    // no properties 오류가 나온다면 보통 getter, setter 오류
    // simple하게 @Data쓰는데 회사의 룰에 따라서 @Data가 해주는게 많아서 안쓰는게 나을 수도 있다.
    // 원래는 orderItems가 Entity여서 null이 나와야 하는데 왜 내 환경에서는 나오지?
    // DTO로 반환 하고 잘됐네요 하면 안된다.
    // DTO 안에 엔티티가 있으면 안되고 랩핑해도 안된다.
    // -> orderItems의 스펙이 다 나가버렸다. 데이터가 외부에 이게 다 노출되었다.
    // -> 엔티티가 외부에 노출되면 안된다는 뜻은 이렇게 단순하게 DTO 하나 감싸서 보내라는 뜻이 아니다.
    // -> 엔티티에 대한 의존을 완전히 끊어야 한다.
    // OrderItem 조차도 DTO로 바꿔야 한다.
    // -> 현업에서도 실수를 많이 한다.
    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
//        private List<OrderItem> orderItems;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getMember().getAddress();
//            order.getOrderItems().stream().forEach(o -> o.getItem().getName());
//            orderItems = order.getOrderItems();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(toList());
        }
    }

    // 이렇게 해야 온전하게 이제 완전히 entity가 나가지 않고 dto를 출력할 수가 있다.
    // 나는 상품명만 필요해 라는게 요구 조건이면 그냥 이렇게 만드는게 좋다.
    // 클라이언트랑 서버를 생각할 때 Depth를 한번 줄일 수 있기도 하고 그 JSON Depth를 줄일 수 있기도 하다.
    // 이 API를 사용하는 클라이언트 입장에서는 그냥 이 데이터가 3개만 있으면 되는거다.
    // 이렇게 하면 OrderDto 에서 OrderItemDto로 또 랩핑해서 나가기 때문에 이제는 정말 깔끔하게 문제가 해결이 된다.
    // 기존 방식은 또 아이템 한 depth 들어가고 이러니깐 클라이언트가 파싱하려면 좀 귀찮다.
    // 어찌되었건 엔티티를 외부로 노출하지 말라는게 단순히 껍데기만 엔티티로 노출하라는게 아니라 정말 그 속에 있는 것까지 다 entity로 노출하시면 안된다.
    // 단, 이런 address 같은 값 객체로 노출하셔도 된다.(value object)

    @Getter
    static class OrderItemDto {
        private String itemName;    // 상품 명
        private int orderPrice;     // 주문 가격
        private int count;          // 주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
