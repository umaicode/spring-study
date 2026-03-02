package jpabook.jpashop.controller;

import jpabook.jpashop.domain.Item.Book;
import jpabook.jpashop.domain.Item.Item;
import jpabook.jpashop.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items/new")
    public String createForm(Model model) {
        model.addAttribute("form", new BookForm());
        return "items/createItemForm";
    }

    @PostMapping("/items/new")
    public String create(BookForm form) {

        // 이런 set, set 하는 것보다 createBook() 해가지고 parameter를 넘기거나 하는게 더 나은 설계이다.
        // 생성자를 만드는게 더 깔끔한 설꼐이다. -> 예제니까 setter 열어놓아야 편하게 할 수 있어서 이렇게 해두었다..
        Book book = new Book();
        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());

        itemService.saveItem(book);
        return "redirect:/";
    }

    @GetMapping("/items")
    public String list(Model model) {
        List<Item> items = itemService.findItems();
        model.addAttribute("items", items);
        return "items/itemList";
    }

    @GetMapping("items/{itemId}/edit")
    public String updateItemForm(@PathVariable("itemId") Long itemId, Model model) {
        // 사실 반환이되는 것은 item type이다.
        // 그러나 예제를 심플하게 하기 위해 책만 가져온다고 가정.
        // 사실 캐스팅을 하고 이런게 막 좋진 않다. 예제를 단순화하기 위해 캐스팅 사용...
        Book item = (Book) itemService.findOne(itemId);

        // 많을 는 멀티라인 셀렉트를 intellij에서 사용하면 된다. -> 어떻게함?
        BookForm form = new BookForm();
        form.setId(item.getId());
        form.setName(item.getName());
        form.setPrice(item.getPrice());
        form.setStockQuantity(item.getStockQuantity());
        form.setAuthor(item.getAuthor());
        form.setIsbn(item.getIsbn());

        model.addAttribute("form", form);
        return "items/updateItemForm";
    }

    // @ModelAttritube("form")을 넣으면 object에서 잡은 {form}이 넘어온다.
    // 이 코드에서 form을 itemService에 서비스 계층으로 넘기는게 되게 지저분하다.
    // 그래서 안넘긴다.
    // 어떻게 해결해야할까?
    // -> book을 어설프게 만들어서 해결했다.
    // 그럼 더 나은 설계는 뭔데?
    // -> itemService.updateItem()사용
    @PostMapping("items/{itemId}/edit")
    public String updateItem(@PathVariable Long itemId, @ModelAttribute("form") BookForm form) {

        //    public String updateItem(@PathVariable Long itemId, UpdateITemDto itemDto) {
//        Book book = new Book();
//
//        // 여기가 보안 상 취약점 -> 누가 아이디를 건드린다면?
//        // 서비스 계층이 어디 뒷단에서든 앞단에서든 이 유저가 이 아이템에 대해서 권한이 있는지 없는지를 한 번 체크해 주느 로직이 서버에 있어야 한다.
//        // 아니면 업데이트할 객체를 세션에 담아두고 풀어내는 방법도 있다. -> 요즘 세션 객체 잘 안쓴다.
//        book.setId(form.getId());
//
//        book.setName(form.getName());
//        book.setPrice(form.getPrice());
//        // merge의 문제
//        // 만약에 book.setPrice(form.getPrice());를 안넣은 다면 book.price가 null이 되고 DB에 NULL로 업데이트 된다.
//        // 즉, merge는 다 갈아치기를 해서 null도 갈아치기 한다.
//        // 따라서 되도록이면 merge보다 변경 감지를 해야 한다. -> ItemService.java
//        book.setStockQuantity(form.getStockQuantity());
//        book.setAuthor(form.getAuthor());
//        book.setIsbn(form.getIsbn());
//
//        // merge를 원래 잘 설명 안한다. -> 실무에서 잘 안쓰니깐.. -> 화면에서 넘어오는 것을 봐야 merge를 제대로 알 수 있다.
//        itemService.saveItem(book);

        // 더 나은 방법
        // 어설프게 엔티티를 파라미터로 쓰지 않았다.
        // 정확하게 내가 필요한 데이터만 딱딱딱 받은 것이다.
        // -> 훨씬 더 유지보수성이 좋다.
        // 근데 업데이트할 데이터가 많다? -> 서비스 계층에 DTO 하나 더 만들어라.
        itemService.updateItem(itemId, form.getName(), form.getPrice(), form.getStockQuantity());
        return "redirect:/items";
    }
}
