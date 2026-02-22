package hello.hello_spring.repository;

import hello.hello_spring.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 참고로 interface일 때는 implements가 아니다.
// interface가 interface를 받을 때는 extends 라고 한다.
// interface는 다중상속이 된다.
// interface만 있으면 스프링 데이터 JPA가 JPA 리포지토리를 받고 있으면 구현체를 자동으로 만들어준다.
// -> 스프링 빈에 자동으로 등록한다.
// -> 우리는 그걸 가져다 쓰면 된다!
public interface SpringDataJpaMemberRepository extends JpaRepository<Member, Long>, MemberRepository {

    // 메소드 인터페이스가 공통화 하는 것이 불가능하다.
    // findByName -> JPQL: select m from Member m where m.name = ?
    // findByNameAndId -> 이런 방식도 가능하다.
    // => 인터페이스 이름만으로도 개발이 끝난 것이다!
    // 메서드 네임, 반환 타입, 파라미터 이런 것들을 다 리플렉션 기술로 다 읽어들어서 풀어내는 것!
    @Override
    Optional<Member> findByName(String name);
}
