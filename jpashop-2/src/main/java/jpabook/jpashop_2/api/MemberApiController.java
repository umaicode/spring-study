package jpabook.jpashop_2.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jpabook.jpashop_2.domain.Member;
import jpabook.jpashop_2.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        // 그냥 가져와서 리스트에 엔티티 반환 끝
        // 만약에 api 라는게 회원에 대한 정보만 달라고 했는데 지금 실제 회원이 주문한 게 있으면 orders 정보에 뭔가 포함되어 있을 것이다.
        // 오더를 정보로 원하는게 아니라 회원정보를 원하고 있다.
        // 그런데 이렇게 entity를 직접 노출하게 되면 이 Entity에 있는 정보들이 다 외부에 노출이 되버린다.
        // -> @JsonIgnore 어노테이션 사용하면 주문정보가 빠지고 순수하게 회원 데이터만 뿌린다.
        // 이게 어디서 멘붕이 되냐? -> 다른 API 만들 때 멘붕이 된다.
        // -> 회원과 관련된 조회 API가 하나가 아닐 거란 말이다.
        // -> 굉장히 많은 클라이언트들이 다양한 API 스타일을 요구할텐데 그러면 그때마다 필요한 파라미터가 다르다면?
        // -> 케이스가 엄청 다양해서 엔티티 안에 이런 것들을 녹이기 시작하면 답이 안나온다.
        // 엔티티가 화면을 뿌리기 위한 로직이 들어가 버렸다.
        // -> 엔티티에 프레젠테이션 계층을 위한 로직이 추가되기 시작한 것이다.
        // -> 엔티티로 의존관계가 들어와야 하는데 반대로 엔티티에 의존관계가 나가버린 거다!
        // -> 이렇게 되면 양방향으로 의존관계가 걸리면서 애플리케이션을 수정하기 되게 어렵게 된다.
        // -> name => username으로 바꾸면 엔티티 변경으로 api 스펙 전체가 바뀌는 심각한 문제가 발생한다.
        // -> 엔티티 직접 반환 하지 마라!
        // 번외: array가 넘어오는데 여기다가 카운트 넘겨달라하면 json 스펙 꺠진다.
        // "data": [] 이렇게 오면 "count" : 4 이렇게 할 순 있는데 지금 방식은 Array를 바로 반환하여 스펙이 굳어서 확장할 수가 없다.
        // -> 유연성이 떨어진다.
        return memberService.findMembers();
    }

    @GetMapping("/api/v2/members")
    public Result memberV2() {
        // list를 바로 collection이랑 바로 내면 json 배열 타입으로 나가 버리기 때문에 유연성이 확 떨어진다.
        // 배열만으로 되는게 간단하지 않다. 실무에서는
        // api는 좋아하는 스타일로 스트림으로 받아서 그냥 돌리면 된다.
        // MemberDto가 오브젝트고 그안에 name이!
        // name -> username으로 바뀌어도 컴파일 단계에서 오류가 발생하여 막을 수 있다!
        // -> 스펙이 변하지 않는다!
        // API 스펙에서 내가 딱 노출할 것만 여기 스펙에 노출할 수 있고 그 말인 즉, API 스펙이 곧 이 DTO랑 코드가 1대1이 된다.
        // 유지 보수도 쉽다. -> 엔티티 그대로 나가면 유지 보수 힘들다.
        // 엔티티를 dto로 변환하는 수고로움이 추가되었다.
        // 그러나 엔티티가 변경이 되어도 API 스펙이 변하지 않는 장점이 있다.
        // 추가로 한번 감싸서 받아내기 때문에 유연성이 생긴다.

        // [강조]
        // - api를 만들 때는 파라미터를 받든 나가든 절대 엔티티를 노출하거나 받지 마라!
        // - 꼭 중간에 api 스펙에 맞는 dto를 만들고 그것을 활용하는 것을 추천이 아니라 강제한다.
        // - 이것만 해도 많은 문제들이 자연스럽게 해결이 된다.

        // [다음시간에 할 것]
        // - 회원 조회처럼 단순한 조회가 아니라 진짜 복잡한 조회를 어떤 식으로 해결하는지 보여줄 것이다.
        // - 그러면서 성능까지
        // - 지금은 단순하게 한 건이니까 성능 문제 같은게 전혀 없는데 단순하게 하나의 테이블만 바라보는 거니까
        // - 그런데 실무에서는 여러 테이블을 조인하면서 api가 만들어져야 한다.
        // - 그러면서 성능까지 어떻게 챙길 수 있는지, 뭐 페이징 처리 같은 것들 포함해서 어떻게 문제를 해결할 수 있는지 설명을 하겠다.
        // - 실무에서 어려워하는 부분: 되게 복잡한 API일 때 어떻게 성능을 챙기면서 API를 만드는지. JPA랑 spring을 활용해서. -> 이 부분들을 시원하게 해결해주겠다.
        List<Member> findMembers = memberService.findMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());

//        return new Result(collect.size(), collect);
        return new Result(collect);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        // 필요하면 여기다 count도 추가하면 바로 적용된다.
//        private int count;
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }

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

    // put은 멱등하다고 한다.
    // 같은 것을 여러 번 해도 결과가 똑같다.
    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateMemberRequest request) {

        // 수정할 때는 가급적이면 변경 감지를 쓰라고 했다.
        // 강사는 단순하게 PK 하나 찍어서 조회하는 것 정도는 특별하게 트래픽 많은 API가 아니면 이슈가 안되기 때문에 그냥 커맨드랑 쿼리를 이렇게 해서 분리하는 스타일로 개발한다.
        // -> 유지보수성이 많이 증대된다.
        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    // 강사는 lombok annotation 중에 몇 개만 제한적으로 쓴다.
    // 엔티티에 쓰는건 최대한 자제한다. (@Getter 정도만)
    // DTO에는 막쓴다. -> 대충 데이터 왔다갔다 하는거니깐 -> 실용적인 관점에서
    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
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
