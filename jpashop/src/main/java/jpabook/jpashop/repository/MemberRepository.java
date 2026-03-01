package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jpabook.jpashop.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

// repository annotation을 사용하면 컴포넌트 스캔에 의해서 자동으로 spring bean으로 관리가 된다.
@Repository
@RequiredArgsConstructor
public class MemberRepository {

    // spring이 EntityManager를 만들어서 인젝션 해준다.
//    @PersistenceContext
//    private EntityManager em;

    // spring boot 라이브러리를 쓰시면 그 spring boot에 spring data jpa를 쓰면
    // @PersistenceContext를 @Autowired로 바꿀 수 있다.
//    @Autowired
//    private EntityManager em;
//
//    public MemberRepository(EntityManager em) {
//        this.em = em;
//    }

    // 위 방식이 가능하기 때문에 @RequiredArgsConstructor 사용 가능
    private final EntityManager em;

    // 참고로 엔티티매니저는 @Autowired로 안되고 @PersistenceContext애로 표준 annotation이 있어야 인젝션이 된다.
    // 근데 spring boot(spring data jpa)가 @Autowired도 인젝션 되게 지원해준다.

    // 만약에 entity manager factory를 직접 주입하고 싶다면?
//    @PersistenceUnit
//    private EntityManagerFactory emf;

    public void save(Member member) {
        em.persist(member);
    }

    // 단건 조회 -> em.find(타입, pk)
    public Member findOne(Long id) {
        return em.find(Member.class, id);
    }

    // SQL은 테이블을 대상으로 쿼리를 하는데 얘는 entity 객체를 대상으로 쿼리를 한다고 보면 된다.
    // 멤버에 대한 entity 객체에 대한 alias를 m으로 주고 entity member를 조회해.
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    // :name -> 파라미터를 바인딩 하는 것
    public List<Member> findByName(String name) {
        return em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
    }
}
