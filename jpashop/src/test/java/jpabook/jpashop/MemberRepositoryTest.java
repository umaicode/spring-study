package jpabook.jpashop;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class MemberRepositoryTest {

    // MemberRepository injection 받기
    @Autowired MemberRepository memberRepository;

    @Test
    @Transactional
    @Rollback(false)
    // EntityManager를 통한 모든 데이터 변경은 항상 트랜잭션 안에서 이루어져야 한다.
    // @Transactional은 spring꺼로 추천.
    // @Transactional annotation이 testcase에 있으면 테스트가 끝난 다음에 바로 롤백을 한다.
    // assert만 가지고는 의심이 될 때 -> @Rollback(false) -> rollback 안하고 그냥 commit
    public void testMember() throws Exception {
        // given
        Member member = new Member();
        member.setUsername("memberA");

        // when
        Long savedId = memberRepository.save(member);
        Member findMember = memberRepository.find(savedId);

        // then
        // AssertJ라는 라이브러리를 Spring 테스트가 자동으로 가지고 있다.
        Assertions.assertThat(findMember.getId()).isEqualTo(member.getId());
        Assertions.assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        // == 비교라고 보면 된다.
        // 같은 트랜잭션 안에서 저장을 하고 조회하면 영속성 컨텍스트가 똑같다.
        // 같은 영속성 컨텍스트 안에서는 id 값이 같으면 같은 엔티티로 식별한다.
        // insert query만 나가고 select query는 안나가게 된다. -> 영속성 컨텍스트 안에 있으니까
        Assertions.assertThat(findMember).isEqualTo(member);
        System.out.println("findMember == member: " + (findMember == member));
    }
}