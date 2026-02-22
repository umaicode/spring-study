package hello.hello_spring.service;

import hello.hello_spring.domain.Member;
import hello.hello_spring.repository.MemberRepository;
import hello.hello_spring.repository.MemoryMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// JPA를 쓰려면 주의해야 될 것: 항상 트랜잭션이라는게 있어야 한다.
// JPA는 join이 들어올 때, 모든 데이터 변경이 다 트랜잭션 안에서 실행이 되어야 한다.
@Transactional
public class MemberService {

    // setter injection 안좋은 점
    // 누군가가 멤버 서비스. 하면 아무 개발자나 호출할 수 있게 열려있게 된다.
    // 개발은 최대한 변경, 호출하지 않아야 될 메서드가 호출되면 안된다.
    // 조립 시점에 딱 생성자로 한 번만 조립해 놓고 얘를 끝을 내버려야 한다.
//    private MemberService memberService;
//
//    @Autowired
//    public MemberController(MemberService memberService) {
//        this.memberService = memberService;
//        memberService.setMemberRepository();
//    }

    // 생성자 주입 방식
//    private final MemberRepository memberRepository;
//
//    public MemberService(MemberRepository memberRepository) {
//        this.memberRepository = memberRepository;
//    }

    // 이거 만약에 @Autowired를 했는데 SpringConfig에서 관리를 안한다면?
    // @Autowired 가 먹을까 안먹을까?
    // -> 당연히 안먹는다.
    // -> 멤버 서비스가 스프링에 등록이 되고 스프링 관리를 해야 스프링이 @Autowired도 적용할 수 있다.
    // -> 관리를 안하고 있다면 코드 자체가 동작하지 않는다.

    // MemberService memberService = new MemberService();
    // new해서 직접 MemberService를 생성하는 경우에도, @Autowired가 동작하지 않는다.
    // -> 내가 직접 생성한 거니까!
    // -> 스프링 컨테이너에 올라가는 것들만 이 @Autowired 기능이 동작을 한다.
    private MemberRepository memberRepository;

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

//        long start = System.currentTimeMillis();
//
//        try {
//            validateDuplicateMember(member);    // 중복 회원 검증
//            memberRepository.save(member);
//            return member.getId();
//        } finally {
//            long finish = System.currentTimeMillis();
//            long timeMs = finish - start;
//            System.out.println("join = " + timeMs + "ms");
//        }

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
    // 보통 처음 실행될 때는 시간이 꽤 걸린다.
    // 그 이후 부터는 빠르게 진행된다.
    // 그래서 실제 운영에서는 처음에 서버 올리고 이것저것 호출해서 웜업한다.
    public List<Member> findMembers() {
//        long start = System.currentTimeMillis();
//
//        try{
//            return memberRepository.findAll();
//        } finally {
//            long finish = System.currentTimeMillis();
//            long timeMs = finish - start;
//            System.out.println("findMembers " + timeMs + "ms");
//        }

        return memberRepository.findAll();
    }

    public Optional<Member> findOne(Long memberId) {
        return memberRepository.findById(memberId);
    }
}
