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

    // 지금 Order랑 OrderItems를 join 한다.
    // 지금 데이터 양이 원래 Order가 몇 개? -> 2개
    // OrderItems -> 4개
    // -> Order가 4개가 되어버린다.
    // Order1: 2개, Order2: 2개
    // join 이라는 것은 1이랑 2가 join되면 join은 그냥 쭉쭉 한 줄씩 다 만들어야 하기 떄문에 1이 그냥 2개 생긴다.
    // -> 이게 문제가 뭐냐? JPA에서 데이터로 Order를 가져올 때도 데이터가 두 배로 되버린다.
    // postman에서는 중복 안하는데 h2 console에서는 2배로 뻥튀기가 되었다!
    // fetch join도 결과적으로 그냥 DB 입장에서는 SQL문 입장에서는 그냥 JOIN이다.
    // 대신에 SELECT 절에 더 데이터를 넣어 주냐 마냐의 차이 정도이다. (DB 입장에서)
    // 이걸 가져와서 객체 그래프로 이제 막 다 담아야 되는데 DB 입장에서 이렇게 뻥튀기 된 거를 하이버네이트나 이런 애들 입장에서는 모른다.
    // 그냥 뻥튀기 하는 대로 써야 될지 이것에 대해서 명확히 기준으로 우리가 알려줘야 한다.
    // 자 근데 우리가 원하는 것은 사실 Order에 대해서는 뻥튀기를 하고 싶지 않다.
    // -> 심지어 이거 오더 객체를 갖다가 지금 2개 뿌린거여서 중복이다.
    // distinct: 얘는 DB의 Distinct 플러스 한 가지를 더 해준다.
    // 실제 SQL에도 distinct를 넣어준다.
    // 그런데 문제가 있다.
    // -> DB의 distinct는 정말 한 줄이 전부 다 똑같아야 중복 제거가 된다.
    // -> 지금 경우에는 DB query에서는 distinct는 안된다. distinct는 명령을 날렸지만 DB query의 결과를 뽑을 때는 그냥 이전이랑 똑같다.

    // JPA에서는 이 Distinct가 있으면 이 Order를 가지고 올 때 이 Order가 같은 ID 값이면 얘를 그냥 중복을 제거해 준다. -> 하나를 버린다. -> 그렇게 해서 리스트에 담아서 반환을 해준다.

    // [정리]
    // 1. DB에 Distinct 키워드를 날려주고
    // 2. 보통 루트라고 표현을 하는데 요 엔티티가 중복일 경우에 그 중복을 걸러서 컬렉션에 담아준다.

    // [참고]
    // 컬렉션 패치 조인은 한 개만 사용할 수 있다.
    // -> 1대 다에 대한 패치 조인은 하나만 사용해야 한다.
    // -> 이 컬렉션 패치 조인을 둘 이상에서 사용하면 데이터가 지금 1대 다도 복잡한데 1대 다의 다가 된다.
    // -> N:M이 곱하기가 되어서 데이터가 완전 뻥튀기가 되어버린다.
    // -> 잘못하면 JPA 입장에서 이거 데이터를 못 맞출 수 있다.
    // -> 이거를 뭘 기준으로 데이터를 끌고 와야 되지 이제 모르게 될 수 있다.
    // -> 데이터 갯수가 안맞거나 정합성이 안맞아질 수 있다.
    // -> 그래서 보통 컬렉션 패치 조인 두 개 하면 경고를 내는데 우리는 딱 하나만 써야 한다!
    // -> 걍 기억하자.
    // 1. 두 개 쓰면 안된다.
    // 2. 페이지 불가능하다.
//    public List<Order> findAllWithItem() {
//        return em.createQuery(
//                "select distinct o from Order o" +
//                        " join fetch o.member m" +
//                        " join fetch o.delivery d" +
//                        " join fetch o.orderItems oi" +
//                        " join fetch oi.item i", Order.class)
////                .setFirstResult(1)
////                .setMaxResults(100)
//                .getResultList();
//    }


    // Order 입장에서 member랑 delivery는 확실하게 ToOne 관계이다.
    // ToOne 관계로 계속 이어지는 거는 FETCH JOIN을 계속 걸어도 된다.
    // -> 그래도 데이터가 증가하지 않는다. 그냥 일자로 쭉 생성이 되니까 데이터 로우 수가 변하지는 않는다.
    // 데이터가 없는 경우에는 LEFT JOIN을 해야 되겠지만 그런 경우를 제외하고는 기본적으로 데이터 뻥튀기가 되지 않는다.
    // 근데 orderItems는 ToOne 관계가 아니다.
    // -> 얘를 FETCH JOIN하면 성능 최적화가 불가능해진다.
    // -> OrderItems 다음의 item도 일대다로 간다음에 얘네끼리 하는 것이기 때문에 FETCH JOIN을 하면 안된다.
    // -> 따라서 member랑 delivery까지만 FETCH JOIN
    // 컬렉션은 지연 로딩으로 조회해야 한다.
    // -> 한마디로 FETCH JOIN으로 안가져온다. (LAZY로 둔다)
    // 지연 로딩 성능 최적화를 위해 hibernate.default_batch_fetch_size, @BatchSize를 적용한다.
    // -> hibernate.default_batch_fetch_size: 글로벌 설정
    // -> @BatchSize: 개별 최적화
    // -> 이 옵션을 사용하면 컬렉션이나, 프록시 객체를 한꺼번에 설정한 size 만큼 IN 쿼리로 조회한다.
    public List<Order> findAllWithItem() {
        return em.createQuery(
                "select distinct o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d" +
                        " join fetch o.orderItems oi" +
                        " join fetch oi.item i", Order.class)
                .setFirstResult(1)
                .setMaxResults(100)
                .getResultList();
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
    // ToOne 관계는 뺴도 상관없다. -> batch_size에 영향을 받기 때문
    // 그러나 네트워크를 더 많이 쓴다.
    // 그래서 ToOne관계는 그냥 다 FETCH JOIN을 미리 잡는게 좋다.
    // 이렇게 하면 Member랑 Delivery에 나가는 쿼리를 줄일 수 있다.
    // 결론: default_batch_fetch_size 덕분에 n + 1문제에서 어느정도 해방이 된다.
    // detail하게 적용하고 싶으면 orderItems에다가 @BatchSize(size = 1000) 한 1000개씩 넣고 싶으면
    // 아이템(ToOne) 이런데 하고 싶으면 최상단 @Entity위에 @BatchSize 적용
    // 이렇게 적는게 의미는 없다고 강사는 이야기 한다...
    // 그런데 size는 중요하다.
    // 1000개 넘어가면 오류를 일으키는 DB도 있다.. 그래서 Maximum 1000개
    // 적당한 사이즈가 좋다.(100 ~ 1000) -> 너무 작으면 쿼리가 너무 많이 나간다.
    // 1000개는 DB, 애플리케이션 모두 순간적인 부하가 생긴다.
    // 10개는 DB, 애플리케이션 짧게 다다다다 끊어서 가기 때문에 부하는 주는데 시간은 더 오래 걸린다.
    // -> 정답이 없다. 개인적으로 강사가 권장하는 것은 WAS랑 DB가 버틸 수 있으면 큰 숫자를 선택
    // 순간 부하의 걱정이 있으면 100정도 넣고 써보면서 늘리기
    // 메모리같은 경우에는 DB는 제쳐두고 WAS 입장에서는 10개씩 하면 WAS 메모리가 최적화되는 거 아니냐고 생각할 수 있는데 아니다.
    // -> 10개든 1000개든 최종적으로 데이터베이스에서 끌어와야하는 데이터 개수는 똑같다.
    // 어차피 메모리는 루프를 다 돌때까지 기다려야 한다.
    // -> result 리스트가 다 찰 때까지 기다려서 특정 시점에 JVM에 들어가는 메모리는 옵션과 무관하게 대부분 찬다고 보면 된다.(batch_size 써도)
    // out of memory 가 날 확률은 100이든 1000이든 거의 같다.
    // 대부분의 성능 최적화는 이 레벨에서 90% 이상은 다 해결이 된다
    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return em.createQuery(
                "select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
