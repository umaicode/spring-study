package hello.hello_spring.service;

import hello.hello_spring.domain.Member;
import hello.hello_spring.repository.MemberRepository;
import hello.hello_spring.repository.MemoryMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// @Service는 스프링에 올라올 때, 얘를 서비스네? 하고 스프링 컨테이너에 멤버 서비스를 딱 등록해준다.
@Service
public class MemberService {

//    private final MemberRepository memberRepository = new MemoryMemberRepository();

    private final MemberRepository memberRepository;

    // 멤버 서비스는 멤버 리포지토리가 필요하다!
    // 멤버 서비스를 스프링이 딱 생성을 할 때, 스프링이 멤버 서비스네? 하고 스프링 컨텐츠에 등록
    // 등록을 하면서 이 생성자라를 또 호출.
    // 그때 Autowired가 있으면 너는 멤버 리포지토리가 필요하구나? 하고 스프링 컨테이너에 멤버 리포지토리 넣어준다.
    // MemoryMemberRepository가 구현체로 존재하므로, MemoryMemberRepository를 서비스에 주입해준다.
    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * 회원 가입
     */
    public Long join(Member member) {
        // 같은 이름이 있는 중복 회원X
//        Optional<Member> result = memberRepository.findByName(member.getName());
////        result.orElseGet();
////        result.get();
//        result.ifPresent(m -> {
//            throw new IllegalStateException("이미 존재하는 회원입니다.");
//        });

        validateDuplicateMember(member);    // 중복 회원 검증
        memberRepository.save(member);
        return member.getId();
    }

    // findByName을 통해서 로직이 쭉 나온다? -> method로 뽑는 것이 좋다!
    private void validateDuplicateMember(Member member) {
        memberRepository.findByName(member.getName())
                        .ifPresent(m -> {
                            throw new IllegalStateException("이미 존재하는 회원입니다.");
                        });
    }

    /**
     * 전체 회원 조회
     */
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public Optional<Member> findOne(Long memberId) {
        return memberRepository.findById(memberId);
    }
}
