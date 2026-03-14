package jpabook.jpashop_2.repository.order.query;

import jakarta.persistence.EntityManager;
import jpabook.jpashop_2.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

// package를 나누는 이유
// OrderRepository는 진짜 오더 엔티티를 조회하거나 이런 용도로 쓰는 거고
// 쿼리 쪽은 대체로 화면이나 뭔가 api에 되게 뭔가 약간 의존 관계가 있는 그런걸 이제 떼어내려고 한 것이다.
// 핵심 비즈니스를 위해서 엔티티를 찾을 땐 OrderRepository를 찾고 특정 화면들의 핏한 쿼리들을 내보내는 것
// 장점: 화면과 관련된 것들은 생각보다 쿼리랑 밀접하게 될 떄가 많다. 그런 것들은 이제 떼어내고
// 중요한 핵심 비즈니스 로직들은 OrderRepository를 참조하면서 거의 문제를 해결할 수 있다.
// -> 관심사를 분리할 수 있다.
// -> 서로 라이프 사이클이 생각보다 다르기 때문(화면을 API에 어떤 돌아간 라이프 사이클이랑 핵심 비즈니스 로직을 처리하기 위한 라이프 사이클이 좀 다르다.)
@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    // 루프를 돌면서 컬렉션 부분을 직접 채우고 있다.
    // 제일 처음에 orders를 가지고 온다. -> 2개
    // 그 다음 루프를 돌리면서 채워준다. 쿼리를 날려서 orderitems를 가지고와서 setorderitems하면 아이템이 다 찬다!
    // OrderItem입장에서 Item은 2:1 => 데이터베이스 입장에서 조인을 해도 데이터가 증가하지 않는다. -> 최적화 완
    // order 1번, orderItems - item JOIN 1번 한거 총 3번 실행
    public List<OrderQueryDto> findOrderQueryDtos() {
        List<OrderQueryDto> result = findOrders();  // query 1번 -> 2개 = N개

        result.forEach(o -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());    // Query 2번 = N번
            o.setOrderItems(orderItems);
        });

        return result;
    }

    // oi.order.id가 왜 될까요?
    // -> OrderItem을 보면 Order가 있다.
    // -> 여기에서 아이디를 가져오려면 Order로 가서 얘 아이디를 가져오면 된다.
    // -> 사실 foreign key가 여기 있기 때문에 얘를 Order를 참조하지 않는다.
    // -> 그냥 여기 있는 foreign key에 있는 값을 바로 가져다가 oi.order.id에 넣어준다.
    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                "select new jpabook.jpashop_2.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                        " from OrderItem oi" +
                        " join oi.item i" +
                        " where oi.order.id = :orderId", OrderItemQueryDto.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    // OrderQueryDto를 따로 만든 이유
    // OrderApiController 안에 있는 Order DTO를 참조해버리게 되면 Repository가 Controller를 참조하는 의존관계가 순환이 되버린다.
    // 그리고 findOrderQUeryDtos가 만드는 거니깐 얘가 OrderQueryDto를 알아야 하니까 같은 패키지에 넣어버렸다.
    private List<OrderQueryDto> findOrders() {
        // 먼저 쿼리를 짜야 한다.
        // 물론 이렇게 짜도 collection을 바로 넣을 수는 없다.
        // 이렇게 더럽게 데이터를 넣을 수 밖에 없다.
        // SQL을 생각해보면 된다. new operation을 쓸 때는 그냥 SQL처럼 쓰는 건데 그냥 이 DTO를 할 뿐 데이터를 플랫하게 한 줄 밖에 못넣는다.
        // 그런데 지금 orderItems 같은 경우에는 일대다다. -> 이거를 바로 플랫하게 못 넣는다.(데이터가 뻥튀기가 된다.) -> 그냥 이렇게 query를 날리고 끝을 낸다.
        return em.createQuery(
                "select new jpabook.jpashop_2.repository.order.query.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d", OrderQueryDto.class)
                .getResultList();
    }
}
