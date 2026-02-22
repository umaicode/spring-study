package hello.hello_spring.service;

import hello.hello_spring.domain.Member;
import hello.hello_spring.repository.MemberRepository;
import hello.hello_spring.repository.MemoryMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// DB는 트랜잭션이라는 개념이 있다.
// DB에 데이터를 인서트 쿼리를 한 다음에 사실은 커밋이라는 걸 해줘야 DB에 반영이 된다.
// 그게 아니면 Auto Commit Mode
// Test 끝난 다음에 rollback 해버리면 DB에 insert query 다 날리고?
// .memberService.join(member) 하게 되면 DB에 insert query 날라갔고, findOne()에서 검증까지 해서 셀렉트 콜까지 했다!
// 근데 이상태에서 rollback을 하게 되면 DB에서 데이터가 다 없어진다.(반영이 안된다.)
// 그 때 필요한게 @Transactional
// @Transactional annotation을 테스트 케이스에 딱 달면 테스트를 실행할 때, 트랜잭션을 먼저 실행을 하고
// 그 다음에 DB에 데이터를 insert query를 하고 다 넣은 다음에 테스트가 끝나면 rollback을 해준다.
// 즉, DB에 있는 넣었던 데이터가 다 깔끔하게 반영이 안되고 다 지워진다! -> 반영을 안한다!
// 즉, 다음 테스트를 반복해서 실행할 수 있다.
// 참고로, @Transactional은 서비스나 이런 데 붙으면 rollback하지 않고 당연히 정상적으로 돈다.
// TestCase에 붙었을 때만 rollback 하도록 동작한다.

// 가급적이면 순수한 단위 테스트가 훨씬 좋은 테스트일 확률이 높다.
// 단위 단위로 쪼개서 테스트를 잘 할 수 있도록 하고 스프링 컨테이너 없이 테스트할 수 있도록 훈련을 해야 한다.
// 그것이 꼭 좋은 테스트라고 표현하기는 애매한데 그 테스트가 좋은 테스트일 확률이 되게 높다.
// 컨테이너까지 올리고 어쩔수 없이 올려야 되는 상황이면 테스트 설계가 잘못됐을 확률이 높다.
// 살다 보면 통합테스트도 필요하다.
// 그러나 진짜 좋은 테스트는 MemberServiceTest 처럼 단위 테스트를 잘 만드는게 훨씬 더 좋은 테스트다.
@SpringBootTest
@Transactional
class MemberServiceIntegrationTest {

    // 테스트는 제일 끝단에 있기 때문에 제일 편한 방법으로 만들면 된다.
    // 테스트 케이스 할 때는 필드 기반으로 @Autowired로 받는게 편하다.
    // 기존 코드와의 차이는 MemoryMemberRepository -> MemberRepository 변경
    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;

    @Test
    void 회원가입() {
        // given
        Member member = new Member();
        member.setName("spring");

        // when
        Long saveId = memberService.join(member);

        // then
        Member findMember = memberService.findOne(saveId).get();
        assertThat(member.getName()).isEqualTo(findMember.getName());
    }

    @Test
    public void 중복_회원_예외() {
        // given
        Member member1 = new Member();
        member1.setName("spring");

        Member member2 = new Member();
        member2.setName("spring");

        // when
        memberService.join(member1);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> memberService.join(member2));

        assertThat(e.getMessage()).isEqualTo("이미 존재하는 회원입니다.");
    }
}