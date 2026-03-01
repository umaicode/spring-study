package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jpabook.jpashop.domain.Order;
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

    // 이거 나중에 설명. 동적 쿼리여서 어려움.
    public List<Order> findAllByString(OrderSearch orderSearch) {
        // 이 join문 설명 필요(상세히)
        // orderSearch에 name 파라미터가 없으면 그 name 파리미터를 필터링 조건을 쓰지 말고 다가져와.
        // 상태 값이 없다면(Null) 상태 체크하지 말고 그냥 주문이든 취소든 다 들고 와.
        // -> 이럴 때마다 바뀌는 동적 쿼리가 되어야만 한다.

        // 1. 무식한 방법: JPQL 문자를 생짜로 해결
        // 번거롭고 실수로 인한 버그가 충분히 발생할 수 있다.
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
            jpql += "o.status = :status";
        }

        // 회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += "m.name like :name";
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

    // JPQL을 자바 코드로 작성할 수 있게 JPA에서 표준으로 제공하는 것
    // 물론 권장하지 않음.
    // 치명적인 단점: 유지 보수성이 제로에 가깝다.
    // 이걸 가지고 무슨 쿼리가 생성될지 머리에 떠오르나?
    // 운영할 때 몇 번 썼다가 도저히 유지보수가 안되서 안씀 -> 그냥 이런 식으로 할 수 있다고 알려줄려고 보여줌.
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

    // Querydsl 버전은 마지막에 보여드리겠습니다..
    // spring boot, spring data jpa, querydsl 이렇게는 꼭 프로젝트에 가져간다.
}
