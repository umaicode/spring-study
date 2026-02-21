package hello.hello_spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // 왜 static 파일이 존재하는데 home으로 들어갈 수 있을까?
    // 정적 컨텐츠 이미지 요청이 오면 먼저 스프링 컨트롤러 안에 있는 관련 컨트롤러가 있는지 먼저 찾고
    // 없으면 static 파일을 찾도록 되어 있다.
    // GetMapping에 "/"가 존재하니 바로 컨트롤러 호출되고 끝!
    @GetMapping("/")
    public String home() {
        return "home";
    }
}
