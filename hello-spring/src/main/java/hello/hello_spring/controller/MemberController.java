package hello.hello_spring.controller;

import hello.hello_spring.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class MemberController {

    // new의 문제: 멤버 컨트롤러 말고 다른 여러 컨트롤러들이 MemberService를 가져다 쓸 수 있다.
    // MemberService는 별 기능이 없어서 하나만 생성해 놓고 같이 공용으로 쓰면 된다.
    // 해결책: Spring Container에 등록하면 딱 하나만 등록이 된다. 그 외에 굉장히 부가적인 여러가지 효과 많이 볼 수 있다.
    // private final MemberService memberService = new MemberService();

    // 생성자에 Autowired가 있으면 스프링이 스프링 컨테이너에 있는 멤버 서비스를 가져다가 연결을 시켜준다.
    // 스프링은 스프링 컨테이너에 스프링 빈을 등록할 때, 기본으로 싱글톤으로 등록한다.
    private final MemberService memberservice;

    // Dependency Injection
    @Autowired
    public MemberController(MemberService memberService) {
        this.memberservice = memberService;
    }
}
