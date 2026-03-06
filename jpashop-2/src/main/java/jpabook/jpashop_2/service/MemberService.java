package jpabook.jpashop_2.service;

import jpabook.jpashop_2.domain.Member;
import jpabook.jpashop_2.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

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

    private void validateDuplicateMember(Member member) {
        // EXCEPTION
        List<Member> findMembers = memberRepository.findByName(member.getName());
        if (!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    // 회원 전체 조회
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public Member findOne(Long memberId) {
        return memberRepository.findOne(memberId);
    }

    // 트랜잭션이 시작되었고 영속성 컨텍스트 멤버 올린 거 반환해서 영속 상태
    // 영속 상태 멤버를 바꾸면 스프링 AOP가 동작하고 JPA가 플러쉬해주고 커밋 다한다.
    // 강사가 좋아하는 기법: 그냥 public void update 말고 public Member update로 반환해도 된다.
    // -> 애매해지는 부분이 있다.
    // -> 영속 상태가 끊기는 멤버가 반환하게 된다.
    // 강사는 커맨드와 쿼리를 철저하게 분리한다 라는 정책을 가지고 있다.
    // -> public Member update 방식은 업데이트를 하면서 결국 멤버를 쿼리해버리는 꼴이다.
    // -> id를 가지고 Member에서 조회하는 꼴이 된다. -> 커맨드랑 쿼리가 같이 있다.
    @Transactional
    public void update(Long id, String name) {
        Member member = memberRepository.findOne(id);
        member.setName(name);
    }
}
