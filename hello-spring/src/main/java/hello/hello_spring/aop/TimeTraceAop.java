package hello.hello_spring.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
// @Component 하게 되면 컴포넌트 스캔이 된다.
// 그러나 스프링 빈에 등록해서 쓰는 것을 더 선호한다.
public class TimeTraceAop {

    // 이거를 어디에 적용할 것인가? -> @Around()
    // 패키지명, 클래스명 , 파라미터 타입 등등 원하는 조건을 다 넣을 수 있다.
    // "execution(* hello.hello_spring.service..*(..))"도 가능 (서비스 하위에 있는 애들만 보고 싶다!)
    // 현재는 패키지 하위에 있는 것을 다 적용하라는
    // * hello.hello_spring.*(..)과 * hello.hello_spring..*(..)의 차이가 뭘까?
    // joinPoint. 확인해보면 어느 타겟에서 호출하는지, 지금 내가 누구인지 등등이 있다.
    // -> 이것들을 가지고 원하는 걸 막 조작할 수 있다.
    // -> 메서드 호출 할 때마다 거기서 중간에서 인터셉트가 딱딱 걸리게 된다.
    // -> "이런 조건이면 다음으로 넘어가지마!" 까지도 가능
    @Around("execution(* hello.hello_spring..*(..))")
    public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        System.out.println("START: " + joinPoint.toString());
        try {
            return joinPoint.proceed();
        } finally {
            long finish = System.currentTimeMillis();
            long timeMs = finish - start;
            System.out.println("END: " + joinPoint.toString() + " " + timeMs + "ms");
        }
    }
}
