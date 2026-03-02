package jpabook.jpashop.controller;

import jakarta.validation.Valid;
import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/members/new")
    public String createForm(Model model) {
        // model: controller에서 view로 넘어갈 때 데이터를 실어서 넘긴다.
        model.addAttribute("memberForm", new MemberForm());
        return "members/createMemberForm";
    }

    // @Valid 넣으면 다 valid 해준다. -> memberForm에 @NotEmpty가 있다는 거 생각하자.
    // bindingResult가 있으면 오류가 담겨서 코드가 실행이 된다.
    // Q. 여기에 member entity를 그대로 넣으면 되지 않나요? 왜 굳이 form을 넣나요?
    // A. member entity에 필드가 되게 많은데 저기다가 또 @NotEmpty 이런거 집어넣으면 지저분해진다.
    // A. 그 결과 컨트롤러에서 화면에서 넘어올 때 Validation이랑 실제 도메인이 원하는 Validation이 다를 수 있다.
    // A. 엔티티를 폼 데이터를 막 화면에 왔다 갔다 하는 이런 폼 데이터를 쓰기 시작하면 뭔가 이게 안맞는다.
    // A. 안맞으면 억지로 맞춰야 되기 때문에 차라리 깔끔하게 그냥 화면에 딱 핏한 폼 데이터를 만들고 그걸로 데이터를 받는게 낫다.
    // A. 정말 애플리케이션이 진짜 심플하면 엔티티를 바로 그냥 데이터를 바인딩 받아도 되는데 실무에서 할 때는 그렇게 단순한 폼 화면이 거의 없다.
    // A. 헬로 월드 할 때나 하는거지 실무에서 파라미터 받을 때랑 차이 많이 난다.
    // A. 따라서 권장: 폼을 받으시고 이거를 컨트롤러 같은데서 한번 중간에 이렇게 한번 좀 정제를 하신 다음에 딱 필요한 데이터만 채워 가지고 이렇게 넘기시는 거를 권장을 해드립니다.
    @PostMapping("/members/new")
    public String create(@Valid MemberForm form, BindingResult result) {

        if (result.hasErrors()) {
            return "members/createMemberForm";
        }

        Address address = new Address(form.getCity(), form.getStreet(), form.getZipcode());

        Member member = new Member();
        member.setName(form.getName());
        member.setAddress(address);

        memberService.join(member);
        return "redirect:/";
    }

    // JPA를 쓸 때 조심해야 되는 것: 엔티티를 최대한 순수하게 유지를 해야 한다.
    // 실무에서는 엔티티는 핵심 비즈니스 로직만 가지고 있고 화면을 위한 로직은 화면에 맞는 API나 이런거는 폼 객체나 DTO를 사용해야 한다.
    // API를 만들 때는 절대 엔티티를 웹으로 반환하면 안된다.
    // API라는 건 스펙이다.
    // 엔티티에 로직을 추가했는데 API의 스펙이 변한다? -> 불완전한 API 스펙
    // 가장 권장하는 것은 템플릿엔진에서 조차도 멤버를 이대로 보기보다는 멤버 DTO나 화면에 맞는 DTO로 변환하여서 반환하는게 제일 깔끔하다.
    @GetMapping("/members")
    public String list(Model model) {
        List<Member> members = memberService.findMembers();
        model.addAttribute("members", members);
        return "members/memberList";
    }
}
