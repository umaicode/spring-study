package hello.hello_spring;

import hello.hello_spring.repository.MemberRepository;
import hello.hello_spring.repository.MemoryMemberRepository;
import hello.hello_spring.service.MemberService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 이거 왜 만들까?
// 우리는 현재 멤버 리포지토리를 설계할 때, 아직 데이터 저장소가 선정되지 않았다는 가상의 시나리오가 존재한다.
// 그래서 지금 인터페이스를 설계를 하고 구현체로 MemoryMemberRepository를 쓰는 그림이 된 것이다.
// 우리는 나중에 MemoryMemberRepository를 다른 리포지토리로 바꿔치기를 해야 한다!
// 그런데 기존의 운영 중인 코드를 하나도 손대지 않고 바꿔치기할 수 있는 방법이 있다!
// 그것을 하려고 지금 @Bean을 한다.
// MemoryMemberRepository를 데이터베이스에 실제 연결하는 리포지토리로 바꿀거다!
// 기존의 MemberService나 나머지 코드에 일절 손대는 거 없이 딱 바꿔치기할 수 있다!
// 그것을 하려고 한다면 구현체를 바꿔치기 해야 한다!
// 나중에 데이터베이스 연결을 하게 되면 return new MemoryMemberRepository();를 DbMemberRepository()로 바꿔주기만 하면 된다.
// -> 다른 코드를 전혀 손 댈 필요가 없다.
// -> 이것이 직접 설정 파일을 운영할 때 장점!
// -> 컴포넌트 스캔을 사용하면 여러 코드를 바꿔야 한다.
@Configuration
public class SpringConfig {

    @Bean
    public MemberService memberService() {
        // MemberService()에서 ()안에 커서를 가져다댄 후, cmd + p를 누르면 뭐가 필요한 지 알 수 있다.
        return new MemberService(memberRepository());
    }

    @Bean
    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
    }
}
