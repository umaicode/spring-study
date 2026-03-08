package jpabook.jpashop_2.repository.order.simplequery;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

// 왜 이렇게 바꿀까?
// Repository는 가급적 순수한 Entity를 조회하는데 쓴다.
// 이 코드는 그냥 화면에 박힌거다.
// API 뭐 다 만들어야 하는데 이런 식으로 정말 복잡한 조인 쿼리를 가지고 뭔가 정말 DTO를 뽑아야 할 떄가 많다.
// 이런 거를 강사는 QueryService나 QueryRepository 이렇게 해서 보통 별도로 뽑아낸다.
// -> 유지보수성이 좋아진다.
// 화면에 딱 Dependency한건데 로직은 되게 Query는 무겁고 복잡하고 근데 이게 막 Repository에 있으면 용도가 애매해진다.
// 그래서 이렇게 분리하는 것을 개인적으로 권장 드린다.

// 강사의 권장하는 방법은 다음과 같다.
// 우선 강사는 쿼리를 어떤 식으로 우선 순위를 잡으면 좋으냐 하면
// 1. 엔티티를 dto로 변환하는 방법을 항상 먼저 선택을 한다. (강사 개인적으로) [V2]
// -> 코드 유지보수성에서 낫다.
// 2. 필요하면 패치조인으로 성능 최적화를 한다. [V3]
// -> 여기서 대부분 성능 이슈가 해결된다. (95% 이상)
// -> 그래도 안될때가 있다. 패치 조인만으로 해결하기 어렵거나 성능최적화가 약하다?
// 3. DTO로 직접 조인하는 방법을 사용한다. [V4]
// -> 거의 없는데 이래도 안될 때가 있다.
// -> Native Query에 DBS 제공하는 굉장히 복잡한 그런 네이티브한 기능을 써야 되는 경우에는
// 4. JPA가 제공하는 네이티브 SQL이나 Spring JDBC Template을 사용해서 SQL을 직접 사용한다.
// -> 우리가 만들었던 이런 Query Repository 같은 데서 EntityManager가 아니라 직접 데이터 베이스 커넥션을 받아서 하거나 스프링 JDBC 템플릿을 직접 써서 뭔가 API에 최적화된 코드를 제공을 해버리는 거다.

// 지금까지는 xToOne
// order에서 orderItems는 데이터가 뻥튀기가 된다.
// DB로 보면 일대다 조인
// 이런 것을 컬렉션 조회라고 하는데 이걸 어떻게 성능최적화 하는지 알아본다.

@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {

    private final EntityManager em;

    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery(
                        "select new jpabook.jpashop_2.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
                                "from Order o" +
                                " join o.member m" +
                                " join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }
}
