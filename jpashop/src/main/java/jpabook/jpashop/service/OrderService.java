package jpabook.jpashop.service;

import jpabook.jpashop.domain.Delivery;
import jpabook.jpashop.domain.Item.Item;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.repository.ItemRepository;
import jpabook.jpashop.repository.MemberRepository;
import jpabook.jpashop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    /**
     * 주문
     */
    @Transactional
    public Long order(Long memberId, Long itemId, int count) {

        // 엔티티 조회
        Member member = memberRepository.findOne(memberId);
        Item item = itemRepository.findOne(itemId);

        // 배송정보 생성
        Delivery delivery = new Delivery();
        delivery.setAddress(member.getAddress());

        // 주문상품 생성
        OrderItem orderItem = OrderItem.createOrderItem(item, item.getPrice(), count);

        // 생성 로직을 변경할 때 생성 로직에서 어떤 필드를 더 추가한다거나 뭐 로직을 더 넣는다거나 분산되니깐
        // 이 방식은 막아야 한다.
        // 위의 생성 스타일 외의 다른 스타일의 생성을 다 막아야 한다.
        // OrderItem.java에서 protected로 해두었고 이렇게 하면 빨간 줄 나오게 해서 컴파일 단계에서 막을 수 있다.
//        OrderItem orderItem1 = new OrderItem();

        // 주문 생성
        // 한번 주문할 때 여러 개의 상품을 넘기고 싶으면 어떻게 해야할까?
        Order order = Order.createOrder(member, delivery, orderItem);

        // 주문 저장
        // 원래대로 라면 deliveryRepository가 있어가지고 deliveryRepository.save() 해가지고 넣어주고
        // orderItem도 JPA에 넣어준 다음에 createOrder 값을 세팅해야 한다.
        // 근데 왜 save를 한번만했을까?
        // A. cascade 옵션 때문에.
        // order 를 persist 하면 orderitem도 persist 한다. delivery 역시 cascade 되어 있어서 persist 된다.
        // cascade는 참조하는게 딱 주인이 프라이빗 오너인 경우에만 써야 한다.
        // -> delivery? 오더 말고 아무도 안쓴다.
        // -> orderItem? 오더 말고 아무도 안쓴다. 오더만 참조해서 쓴다. 물론 오더 아이템이 다른걸 참조할 수 있지만 다른데서 오더 아이템을 참조하는 데가 없다.
        // 만약 delivery가 중요해서 다른 데서도 delivery를 참조하고 갖다 쓴다면?
        // -> cascade 막쓰면 안된다. 잘못하면 order 지울 때 저거 다 지워지고 persist도 다른게 걸려 있으면 복잡하게 얽혀 돌아간다.
        // -> orderItem도 마찬가지다. 다른데서도 쓰이고 주용하다면 cascade 쓰지말고 별도의 repository를 생성을 해서 persist를 별도로 하는게 낫다.
        // -> 이번 케이스는 life Cycle이 완전히 똑같고 Order만 orderItem을 사용하고 Order만 delivery를 사용했기 때문에 가능했다.
        orderRepository.save(order);
        return order.getId();
    }

    /**
     * 주문 취소
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        // 주문 엔티티 조회
        Order order = orderRepository.findOne(orderId);
        // 주문 취소
        // order.cancel()을 호출하게 되면 배송 완료가 이미 되어버린건 취소가 안되기 때문에 예외를 터트리고 그 외에는 나의 상태를 CANCEL로 바꾼다.
        // 그리고 재고 수량을 원복해야 되기때문에 orderItem을 또 cancel해준다.
        // 그러면 재고를 다시 카운트만큼 올린다.
        // 비즈니스 로직을 만들었기 때문에 가능
        // JPA의 강점이 여기서 나온다.
        // -> 원래는 바깥에서 업데이트 쿼리를 직접 짜서 날려야 한다. (SQL을 직접 다루는 스타일)
        // -> JPA를 활용하면 엔티티 안에서 있는 데이터들만 바꿔주면 JPA가 알아서 바뀐 변경 포인트들을 이제 Dirty Checking이라고 변경 내역 감지가 일어나면서 변경된 내역들을 다 찾아서 데이터 베이스에 업데이트 쿼리가 다 날라간다.
        // -> 이 경우에는 바꾼 데이터가 오더의 상태를 바꿨기 때문에 오더의 업데이트 쿼리가 날라갈 것이고, 오더 아이템은 바꾼게 없지만 이 아이템에 보면 stockQuantity가 바뀌어서 아이템도 업데이트 쿼리가 날라가서 stockQuantity가 원복이 될 것이다.
        // 한 프로젝트 내에서도 트랜젝션 스크립트 패턴과 도메인 모델 패턴이 양립을 한다.
        // 현재 내 문맥에서 뭐가 더 맞는지를 고민해보고 그것을 쓰면 된다.
        order.cancel();
    }

    // 검색
//    public List<Order> findOrders(OrderSearch orderSearch) {
//        return orderSearch.findAll(orderSearch);
//    }
}
