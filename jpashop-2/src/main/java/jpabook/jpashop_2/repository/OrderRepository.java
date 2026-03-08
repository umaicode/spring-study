package jpabook.jpashop_2.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jpabook.jpashop_2.domain.Order;
import jpabook.jpashop_2.repository.order.simplequery.OrderSimpleQueryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    public List<Order> findAllByString(OrderSearch orderSearch) {

        String jpql = "select o from Order o join o.member m";
        boolean isFirstCondition = true;

        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }

        // 회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }

        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000);    // 최대 1000건

        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }

        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }

        return query.getResultList();
    }

    /**
     * JPA Criteria
     */
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Object, Object> m = o.join("member", JoinType.INNER);

        List<Predicate> criteria = new ArrayList<>();

        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"), orderSearch.getOrderStatus());
            criteria.add(status);
        }

        // 회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name =
                    cb.like(m.<String>get("name"), "%" + orderSearch.getMemberName() + "%");
            criteria.add(name);
        }

        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000);
        return query.getResultList();
    }

    // 한방 쿼리로 Order랑 Member랑 Delivery를 조인한 다음에 아예 SELECT 절에 얘를 다 넣고 한방에 다 떙겨오기
    // Order entity를 보면 Member랑 Delivery가 LAZY로 되어있다.
    // LAZY 다 무시하고 이 경우에는 그냥 다 값을 채워서 proxy도 아니고 그냥 진짜 객체 값을 채워서 가져와 버린다.
    // 이것을 패치조인이라고 한다.
    // 기술적으로는 SQL의 JOIN을 쓰는데 fetch라는 명령어는 SQL에 없다. -> JPA에만 있는 문법
    // -> JPA 기본 편을 참고하자.
    // -> 단순하지 않다. 성능 최적화에서 실무에서 정말 자주 사용하는 것이다.
    // -> 책이나 강좌를 통해서 100% 이해해야 한다.(실무에서 JPA를 쓰려면)
    // -> 실무에서 JPA 성능 문제의 90%는 N + 1 문제에서 터진다.
    // -> 나머지 10%는 그 다음 강좌에서 설명

    // 강사는 요정도까지는 Repository에서 Entity를 써도 된다고 본다.
    // Entity를 순수하게 조회하거나 필요한 것을 성능 최적화를 위해서 패치 조인을 해서 가져오거나 이 정도까지는 Entity에 대한 순수성이 유지가 된다.
    public List<Order> findAllWithMemberDelivery() {
        return em.createQuery(
                "select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Order.class
        ).getResultList();
    }


    // Repository에 Controller랑 의존관계 생기면 망한다.
    // 가급적이면 한방향으로 흘러야 한다.
    // 엔티티 바로 넘기는게 안된다.
    // -> 엔티티를 넘기면 엔티티가 식별자로 넘어간다.

    // 이거는 냉정하게 이야기해서 물리적으로는 계층이 나눠져 있지만 이거 같은 경우에는 논리적으로는 계층이 다 깨져 있다.
    // -> API가, Repository가 화면을 의존하고 있다.
    // -> API 스펙이 바뀌면 다 뜯어 고쳐야 한다.
    // -> 이게 v3, v4의 트레이드 오프다.
    // 그러면 뭘 선택하는게 좋을까?
    // -> v3랑 v4랑 성능차이가 많이 날까?
    // -> 성능테스트 해봐야 한다.
    // -> 대부분의 경우는 여러분의 시스템에서 그렇게 성능차이가 많이 나지 않는다.
    // -> 네트워크가 되게 좋고, 대부분의 성능은 join, where 등에서 먹는다.
    // -> select에서는 데이터 전체적인 애플리케이션 관점에서 보면 이게 그렇게 성능에 크게 영향을 주지 않는다.
    // 진짜 성능에 문제가 있는 걸 튜닝해야 되는데 그건 대부분 보면 인덱스가 잘못 잡혔거나 그런데다.
    // SELECT 절에 몇 개 더 들어간다고 해서 차이 없다.
    // -> 데이터 사이즈가 너무 클 때는 얘기가 다르다.
    // -> 만약 select 필드가 20-30개 이러면? 고민을 해야 한다.
    // -> 여러분의 API가 트래픽이 엄청 많이 들어오는 API다.
    // -> admin api라면 뭐 그렇다 할 수 있는데 API가 고객이 실시간으로 계속 누르거나 이런 API면 고객 트래픽이 정말 많이 들어오는 API면 정말 최적화 하는 거에 대해서 고민할 필요가 있다.
    // -> 전반적인 것을 보고 결정하자!


//    public List<OrderSimpleQueryDto> findOrderDtos() {
//        return em.createQuery(
//                "select new jpabook.jpashop_2.repository.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
//                        "from Order o" +
//                        " join o.member m" +
//                        " join o.delivery d", OrderSimpleQueryDto.class)
//                .getResultList();
//    }

    // DTO를 바로 조회해야 하기 때문에 DTO class를 만들어야 한다.
    // v3까지는 DTO가 Controller에 있는데 지금은 Repository에 해야 되기 때문에 의존관계가 이제 Repository Controller로 보는 이상한 사태가 벌어질 수도 있다.
}
