package jpabook.jpashop.service;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.spring6.processor.SpringValueTagProcessor;

import static org.junit.jupiter.api.Assertions.*;

// JPA가 실제 DB까지 돌고 이런걸 보여주는게 중요하다.
// 메모리 모드로 DB까지 다 엮어서 테스트 해드리는게 중요하다.
// -> 완전히 스프링이랑 인티그레이션을 해가지고 테스트할 것이다.
@SpringBootTest
@Transactional
public class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;

    // rollback이지만 DB에 쿼리 날리는거 보고 싶어 하면 이렇게 하자!
    @Autowired EntityManager em;

    @Test
    // 의심이 되면 그냥 @Rollback(false)하고 DB 들어가봐라
//    @Rollback(false)
    public void 회원가입() throws Exception {
        // given
        // generatedValue 전략에서는 persist한다고해서 insert가 나가지 않는다.
        // 데이터베이스 트랜잭션이 딱 정확하게 커밋을 하는 순간 플러쉬라는게 되면서 이제 JPA 영속성 컨텍스트에 있는 이 멤버 객체가 insert가 만들어지면서 DB에 인서트가 딱 나간다.
        // 그런데 spring에서 @transactional은 트랜잭션 커밋을 안하고 롤백을 해버린다.
        // rollback(false)주면 rollback 안하고 commit 한다.
        Member member = new Member();
        member.setName("kim");

        // when
        Long savedId = memberService.join(member);

        // then
//        rollback이지만 DB에 쿼리 날리는 거 보고 싶을 때 넣는 코드
//        em.flush();
        assertEquals(member, memberRepository.findOne(savedId));
    }

    // 테스트가 좀 지저분한데?
    @Test
    public void 중복_회원_예외() throws Exception {
        // given
        Member member1 = new Member();
        member1.setName("kim");

        Member member2 = new Member();
        member2.setName("kim");

        // when
        memberService.join(member1);
        try {
            memberService.join(member2);    // 예외가 발생해야 한다!!!
        } catch (IllegalStateException e) {
            return;
        }

        // then
        // memberService.join(member1); 만 하고 fail 떨구면 성공이라고 뜨는 잘못된 테스트케이스가 된다.
        // 원래 구조는 memberService.join(member2); 도 작성하고 그 때 exception이 터져가지고 제어가 끝나고 밖으로 나가야 한다. -> then으로 가면 안된다.
        fail("예외가 발생해야 한다.");
    }
}