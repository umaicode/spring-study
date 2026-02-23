package jpabook.jpashop;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRepository {

    // JPA를 쓰기 때문에 엔티티 매니저가 필요하다.
    // 이 annotation이 있으면 spring boot가 EntityManager를 주입을 해준다.
    @PersistenceContext
    private EntityManager em;

    public Long save(Member member) {
        // 왜 id만 반환하지?
        // -> command랑 query를 분리해라!
        // -> 저장을 하고 나면 가급적이면 사이드 이펙트를 일으키느 커맨드 성이기 때문에 리턴값을 거의 안만든다.
        // -> 대신에 id정도 있으면 다음에 다시 조회할 수 있으니까 아이디 정도만 조회하는 걸로 주로 설계.
        em.persist(member);
        return member.getId();
    }

    public Member find(Long id) {
        return em.find(Member.class, id);
    }
}
