package jpabook.jpashop;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    @GetMapping("hello")
    public String hello(Model model) {
        // springUI에 있는 model이란 얘가 어떤 데이터를 실어서 view에 넘길 수 있다.
        // Controller에서 데이터를 view로 넘길 수 있다.
        // return은 화면 이름이다.
        model.addAttribute("data", "hello!!!");
        return "hello";
    }
}
