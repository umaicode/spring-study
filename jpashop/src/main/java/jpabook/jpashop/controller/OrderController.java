package jpabook.jpashop.controller;

import jpabook.jpashop.domain.Item.Item;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.service.ItemService;
import jpabook.jpashop.service.MemberService;
import jpabook.jpashop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MemberService memberService;
    private final ItemService itemService;

    @GetMapping("/order")
    public String createForm(Model model) {

        List<Member> members = memberService.findMembers();
        List<Item> items = itemService.findItems();

        model.addAttribute("members", members);
        model.addAttribute("items", items);

        return "order/orderForm";
    }

    @PostMapping("/order")
    public String order(@RequestParam("memberId") Long memberId,
                        @RequestParam("itemId") Long itemId,
                        @RequestParam("count") int count) {

        // 조금만 고치면 하나의 상품이 아니라 여러 개 상품을 한 번에 주문할 수 있도록 할 수 있다. -> 어떻게?
        // 예제에서는 단순화하기 위해서 하나의 상품만 주문
        // Q. 컨트롤러에서 멤버랑 아이디 찾으면 안되나요?
        // A. 되는데 이게 더 낫다. 테스트할 때도 그렇고 이게 단순화 된다. 아이디만 넘겨서 트랜재션안에서 JPA가 제일 깔끔하게 동작한다.
        // 식별자만 넘기고 바깥입장에서는 크게 엔티티나 이런거 몰라도 되는걷네 서비스 계층에서는 엔티티에 크게 의존하니깐 안에서 찾는게 더 깔끔하고 많아진다.
        // 또한 엔티티도 영속상태로 흘러가게 된다.
        // 개인적으로는 주로 command성, 주문 등은 외부에서 컨트롤러 레벨에서는 그냥 식별자만 넘기고 실제 핵심 비즈니스 서비스에서 어떻게 하냐면 거기서 엔티티를 찾는 것부터 거기서 한다.
        // 그렇게 하면 엔티티 값들도 지금 보면 트랜잭션 안에서 엔티티를 조회를 해야 이게 영속 상태로 진행이 된다.
        // 멤버나 아이템의 상태도 바꿀수 있다. 이렇게 하면
        // 가급적이면 조회는 상관 없는데 조회가 아닌 핵심 비즈니스 로직이 있는 것 같은 경우에는 바깥에서 막 그런걸 엔티티를 찾아서 넘기는 것도 괜찮긴 한데 그것 보다는 이렇게 식별자만 넘겨주고 핵심 비즈니스 로직을 이 안에서 하게 되면
        // 어쨌든 이거를 영속 상태로 영속성 컨텍스트가 존재하는 상태에서 이걸 조회할 수 있단 말이다. 그러면 저희가 아는 그 주문하면서 멤버가 바뀌거나 아이템이 바뀌더라도 더티 체킹을 해도 자연스럽게 적용이 되는데
        // 바깥에서 이걸 가지고 넘어오게 되면 그런 것들에 아무것도 적용이 되지 않는다.
        // 왜? -> 트랜잭션 없이 바깥에서 조회 한거니깐 영속상태가 끝나버린다. 그 상태로 오더로 넘어오면 여기서 멤버는 JPA와 관계가 없는 애가 오게 되버려서 애매해진다.(값을 바꾸려면)
        orderService.order(memberId, itemId, count);
        return "redirect:/orders";

        // 만약 주문 결과 페이지를 만든다면?
//        Long orderId = orderService.order(memberId, itemId, count);
//        return "redirect:/orders/" + orderId;
    }

    @GetMapping("/orders")
    public String orderList(@ModelAttribute("orderSearch") OrderSearch orderSearch, Model model) {

        // 단순한 위임이면 그냥 호출 하는 편
        // 컨트롤해서 리포지토리를 바로 불러도 괜찮다. 단순히 화면에서 조회하는 기능이라면
        // 강사의 경우 리포지토리에 접근하는 것, 단순한 위임이면 그냥 호출하느 ㄴ편
        // 여기서는 코드도 얼마 없으니 서비스에 위임한 것
        // 얽메이지 말고 여러분의 선택
        List<Order> orders = orderService.findOrders(orderSearch);
        model.addAttribute("orders", orders);
        // @ModelAttribute("orderSearch")의 의미
        // model.addAttribute("orderSearch", orderSearch);
        // 이게 생략된거다.

        return "order/orderList";
    }

    @PostMapping("/orders/{orderId}/cancel")
    public String cancelOrder(@PathVariable("orderId") Long orderId) {
        orderService.cancelOrder(orderId);
        return "redirect:/orders";
    }
}
