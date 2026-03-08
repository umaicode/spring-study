package jpabook.jpashop_2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Jpashop2Application {

	public static void main(String[] args) {
		SpringApplication.run(Jpashop2Application.class, args);
	}

	// 스프링 부트 3.0이상: Hibernate5jakartaModule 등록
	// lazy loading된 걸 무시했는데 lazy loading을 강제로 해서 json 생산하는 시점에 강제로 파바바바
	// 그리고 이거 사용해도 FORCE_LAZY_LOADING 되도록이면 이거 사용하면 안된다!
//	@Bean
//	Hibernate5JakartaModule hibernate5Module() {
//		Hibernate5JakartaModule hibernate5Module = new Hibernate5JakartaModule();
//		hibernate5Module.configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, true);
//		return hibernate5Module;
//	}
}
