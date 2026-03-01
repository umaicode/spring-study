package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 컴포넌트 스캔 대상
// JPA의 어떤 모든 데이터 변경이나 어떤 로직들은 가급적이면 트랜잭션 안에서 다 실행이 되어야 한다.
// public method들은 transactional이 들어간다.
// 조회하는 메서드들은 readOnly = true가 성능 상 더 좋은데 지금 이 클래스의 경우 조회가 더 많다
// 그렇다면 조회가 아닌 것만 @Transactional을 넣고 나머지는 @Transactional(readOnly = true)
// 쓰기가 더 많다면 반대로 하면 된다.
@Service
@Transactional(readOnly = true)
//@AllArgsConstructor
@RequiredArgsConstructor
public class MemberService {

    // @Autowired를 쓰면 인젝션이 된다! -> 나중에 더 좋은 방법 알려줌.
    // spring이 spring bean에 들어있는 member repository를 injection 해준다. -> field injection
    // 이거 단점이 액세스할 수 있는 방법이 없어서 바꾸지를 못한다.
//    @Autowired
//    private MemberRepository memberRepository;

    // 이 방식의 장점
    // 테스트 코드 돌릴 때 mock 같은 것을 직접 주입할 수 있다.
    // field는 주입하기 까다롭다.
    // 이 방식의 단점
    // 우리가 멤버 리포지토리를 호출해서 개발하는 중간에 이걸 바꿀일이 존재하나?
    // 이미 애플리케이션 로딩 시점에 조립이 끝나는데?
    // 조립한 이후에 실제 애플리케이션이 동작을 잘 하고 있는데 바꿀 일이 없다.
    // 이 때는 setter injection이 안좋다.
//    private MemberRepository memberRepository;
//
//    @Autowired
//    public void setMemberRepository(MemberRepository memberRepository) {
//        this.memberRepository = memberRepository;
//    }

    // 추천하는 방식: 생성자 인젝션
    // 생성자 인젝션은 중간에 set해서 바꿀수 있는 일은 없다.
    // 테스트 케이스 작성할 때 mock을 주입을 하든 뭐든 주입을 해줘야 하는데, 이런걸 안 놓치고 챙길 수 있다.
    // 생성 시점에 얘는 뭐에 의존하고 있어를 명확히 알 수 있다.
    // 생성자가 딱 하나만 있는 경우에는 @Autowired를 생략할 수 있다.
    // 필드를 변경할 일이 없기 때문에 final을 권장한다.
    // 생성자 코드 안에 아무것도 없을 때 final을 넣게 되면 컴파일 시점에서 체크할 수 있기 때문에 final annotation 추천
//    private final MemberRepository memberRepository;
//
//    @Autowired
//    public MemberService(MemberRepository memberRepository) {
//        this.memberRepository = memberRepository;

    // @AllArgsConstructor는 생성자를 만들어 준다.
//    private final MemberRepository memberRepository;

    // @RequiredArsConstructor를 적용하게 되면 final이 있는 필드만 가지고 생성자를 만든다.
    // 이 방식을 사용하면 테스트 케이스에서도 여러 개 실수로 멤버 리포지토리를 초기화안하면 에러를 내주고 하기 때문에 이 방법을 권장한다.
    private final MemberRepository memberRepository;

    /**
     * 회원 가입
     */
    @Transactional
    public Long join(Member member) {

        validateDuplicateMember(member);    // 중복 회원 검증
        memberRepository.save(member);
        return member.getId();
    }

    // 실무에서는 한번 더 최후의 방어를 해야한다.
    // -> 운이 없게 같은 A라는 이름들이 동시에 들어온다면?
    // -> member의 name을 유니크 제약 조건을 잡는 것을 권장한다.
    private void validateDuplicateMember(Member member) {
        // EXCEPTION
        List<Member> findMembers = memberRepository.findByName(member.getName());
        if (!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    // 회원 전체 조회
    // @Transactional(readOnly = true) : JPA가 조회하는 곳에서는 조금 더 성능을 최적화 한다.
//    @Transactional(readOnly = true)
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

//    @Transactional(readOnly = true)
    public Member findOne(Long memberId) {
        return memberRepository.findOne(memberId);
    }
}
