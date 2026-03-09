package jpabook.jpashop_2.api;

import jpabook.jpashop_2.domain.Address;
import jpabook.jpashop_2.domain.Order;
import jpabook.jpashop_2.domain.OrderItem;
import jpabook.jpashop_2.domain.OrderStatus;
import jpabook.jpashop_2.repository.OrderRepository;
import jpabook.jpashop_2.repository.OrderSearch;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

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
                .collect(Collectors.toList());

        return result;
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
                    .collect(Collectors.toList());
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
