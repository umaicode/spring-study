package jpabook.jpashop_2.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jpabook.jpashop_2.domain.Member;
import jpabook.jpashop_2.service.MemberService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        // presentation을 위한 검증 로직이 이 엔티티(멤버)에 다 들어가 있다.
        // 어떤 api에서는 @NotEmpty가 필요할 수도 있고, 어떤 api 에서는 @NotEmpty가 필요 없을 수도 있다.
        // 만약 Member entity에서 name을 username으로 바꾼다면 어떻게 될까?
        // -> api 스펙 자체가 바뀌어버린다.
        // -> 자칫하면 동작안한다.
        // 즉, 엔티티를 손대서 api 스펙 자체가 변한게 문제다!
        // 엔티티는 여러 군데서 쓰이기 때문에 바뀔 확률이 높다.
        // 그런데 이거를 바꿨다고 해서 API가 만들어놓은 스펙 자체가 바뀌어 버린다는게 문제
        // 엔티티랑 API 스펙이 1대1로 딱 매핑이 되어있다.
        // -> API 스펙을 위한 별도의 데이터 트랜스퍼 오브젝트, DTO를 만들어야 한다!
        // -> 엔티티를 이렇게 외부에서 JSON 오는 걸 바인딩 받는데 쓰면 안된다.
        // -> 나중에 큰 장애를 발생시킨다.(편한 것 같지만 궁극적으로 이것 때문에 큰 장애를 만든다.)
        // 그냥 API를 만들 때는 항상 엔티티를 파라미터로 받지 마라!
        // 엔티티를 외부에 노출해서도 안된다!
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        // v1이 주는 유일한 장점: 클래스를 안만들어도 된다.
        // v2의 장점1: API 스펙이 바뀌지 않는다.
        // -> 누가 name => username으로 바꾸면 컴파일에서 오류난다.
        // v2의 장점2: v1은 파라미터가 뭐가 들어올지 모른다.
        // -> 개발자는 API 스펙을 까보지 않으면 멤버에서 어느 값이 파라미터로 넘어오는지를 모른다.
        // -> 유지보수에 큰 장점이 있다.
        // 이게 API의 정석이다.
        // 항상 DTO라는 객체를 사용해서 등록과 응답을 받는 것을 권장한다.
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @Data
    static class CreateMemberRequest {
        // validation 하고 싶으면 여기다 하면 된다!
        @NotEmpty
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }
}
