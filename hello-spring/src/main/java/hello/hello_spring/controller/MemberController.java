package hello.hello_spring.controller;

import hello.hello_spring.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class MemberController {



    // Dependency Injection
    // 1. 필드 주입 방식
    // 중간에 바꿔치기 할 수 있는 방법이 없다.
    // @Autowired private MemberService memberService;

    // 2. setter 주입 방식
    // 생성은 생성대로 되고, Setter는 나중에 호출
    // 누군가가 멤버 컨트롤을 호출했을 때, public으로 열려있어야 한다.
    // setMemberService는 중간에 바꿔치기 할 이유가 없다. -> 한번 세팅이 되고 나면 중간에 뭘 바꿀 일이 없다.
    // 애플리케이션 로딩 시점에 조립할 때 사실 바꾸는 거지 한번 세팅이 되고 나면 바꿀 일이 없다.
//    private MemberService memberService;
//
//    @Autowired
//    public void setMemberService(MemberService memberService) {
//        this.memberService = memberService;
//    }

    // 3. 생성자 주입 방식
    // 처음에 애플리케이션이 조립된다고 표현을 한다.
//    private final MemberService memberservice;

//    @Autowired
//    public MemberController(MemberService memberService) {
//        this.memberservice = memberService;
//    }
}
