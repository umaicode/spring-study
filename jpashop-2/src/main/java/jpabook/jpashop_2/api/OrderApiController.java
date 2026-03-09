package jpabook.jpashop_2.api;

import jpabook.jpashop_2.domain.Order;
import jpabook.jpashop_2.domain.OrderItem;
import jpabook.jpashop_2.repository.OrderRepository;
import jpabook.jpashop_2.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
