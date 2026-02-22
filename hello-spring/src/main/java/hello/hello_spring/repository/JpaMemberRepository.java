package hello.hello_spring.repository;

import hello.hello_spring.domain.Member;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class JpaMemberRepository implements MemberRepository {

    // Jpa는 EntityManager로 모든 것이 동작한다!
    private final EntityManager em;

    public JpaMemberRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public Member save(Member member) {
        // persist: 영속하다, 영구 저장하다
        // 이렇게 하면 JPA가 insert query 다 만들어서 DB에 집어 넣고 id까지 member에다가 setId까지 해준다!
        em.persist(member);
        return member;
    }

    @Override
    public Optional<Member> findById(Long id) {
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    // pk기반이 아닌 나머지들은 JPQL이라는 것을 작성해줘야 한다.
    // 이 JPA 기술을 스프링에서 한번 감싸서 제공하는 기술이 스프링 데이터 jpa
    // 스프링 데이터 jpa를 사용하게 되면 JPQL 안짜도 된다!
    @Override
    public Optional<Member> findByName(String name) {
        // findByName의 경우 JPQL이 필요하다.
        List<Member> result = em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();

        // findByName은 하나만 찾는다고 하였으니 stream으로 해서 반환하면 된다.
        // stream(), findAny()의 역할이 뭐지?
        return result.stream().findAny();
    }

    @Override
    public List<Member> findAll() {
//        List<Member> result = em.createQuery("select m from Member m", Member.class).getResultList();
//        return result;

        // 주석처럼 result, result 이렇게 같으면 inline이라고 한다. -> alt+shift+enter
        // JPQL: 보통 우리는 테이블 대상으로 sql을 날리는데 이건 객체를 대상으로 쿼리를 날리고 이게 sql로 번역이 된다.
        // 정확히는 엔티티를 대상으로 쿼리를 날린다.
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }
}
