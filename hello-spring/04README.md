# 스프링 빈과 의존관계 - 컴포넌트 스캔과 자동 의존관계 설정

> 스프링의 핵심, IoC와 DI를 통한 객체 관리 자동화

## 📚 강의 출처

**인프런 - 김영한의 "스프링 입문 - 코드로 배우는 스프링 부트, 웹 MVC, DB 접근 기술"**

이 문서는 강의의 "스프링 빈과 의존관계" 섹션을 학습하며 작성한 노트입니다. 스프링의 핵심 개념인 IoC(Inversion of Control)와 DI(Dependency Injection)를 실전 코드로 익히는 것이 목표입니다.

---

## 🎯 학습 목표

이 문서를 통해 다음을 이해하고 실습합니다:

1. **Spring Container와 Spring Bean 개념 이해**
   - ApplicationContext가 무엇인지
   - Bean이란 무엇이고 왜 필요한지
   - Bean의 생명주기

2. **컴포넌트 스캔(Component Scan) 이해**
   - @ComponentScan의 동작 원리
   - @Controller, @Service, @Repository 차이
   - 스캔 범위와 필터

3. **의존관계 자동 주입(@Autowired) 마스터**
   - 생성자 주입, 필드 주입, 세터 주입 비교
   - 왜 생성자 주입을 권장하는지
   - final 키워드의 중요성

4. **싱글톤 패턴과 스프링의 관계**
   - 전통적 싱글톤의 문제점
   - 스프링이 싱글톤을 관리하는 방법
   - Stateless 설계의 중요성

5. **IoC/DI 패턴 실무 적용**
   - 느슨한 결합의 장점
   - 테스트 용이성
   - 코드 유지보수성 향상

---

## 🗺️ 학습 로드맵

이 문서는 **Bottom-Up** 방식으로 구성되어 있습니다. 문제를 먼저 인식한 후, 해결책을 배우고, 동작 원리를 이해하며, 실무에 적용하는 순서입니다.

```
1. 기존 방식의 문제점 인식
   - new 연산자의 한계
   - 강한 결합의 문제
   ↓
2. Spring Container와 Bean 개념
   - IoC: 제어의 역전
   - DI: 의존성 주입
   ↓
3. 컴포넌트 스캔 학습
   - @Component 계열 어노테이션
   - 자동 빈 등록
   ↓
4. 의존관계 자동 주입
   - @Autowired
   - 생성자 주입 권장
   ↓
5. 싱글톤 패턴 이해
   - 스프링의 싱글톤 컨테이너
   - 무상태 설계
   ↓
6. 수동 빈 등록 방법
   - @Configuration + @Bean
   - 언제 사용하는가
   ↓
7. 실전 예제와 Best Practice
   - MemberController 구현
   - 안티패턴 회피
```

**왜 이 순서인가?**
- **문제 중심 학습**: 왜 필요한지 먼저 이해하면 개념이 더 명확합니다.
- **점진적 심화**: 간단한 개념부터 복잡한 실무 패턴까지 단계적으로 학습합니다.
- **실전 적용**: 이론만이 아닌 실제 코드 예시로 체득합니다.

---

## 📖 목차

1. [기존 방식의 문제점](#1-기존-방식의-문제점)
2. [Spring Container와 Spring Bean](#2-spring-container와-spring-bean)
3. [컴포넌트 스캔(Component Scan)](#3-컴포넌트-스캔component-scan)
4. [의존관계 자동 주입(@Autowired)](#4-의존관계-자동-주입autowired)
5. [싱글톤 패턴과 스프링](#5-싱글톤-패턴과-스프링)
6. [자바 코드로 직접 빈 등록](#6-자바-코드로-직접-빈-등록)
7. [실전 예제 - MemberController 구현](#7-실전-예제---membercontroller-구현)
8. [Best Practice 및 안티패턴](#8-best-practice-및-안티패턴)
9. [부록](#9-부록)

---

## 1. 기존 방식의 문제점

스프링을 사용하지 않고 순수 자바로 객체를 관리할 때 어떤 문제가 있는지 살펴봅시다.

### 1.1 new 연산자로 직접 객체 생성하기

지금까지는 필요한 객체를 직접 `new` 연산자로 생성했습니다.

```java
// ❌ 문제 있는 코드
@Controller
public class MemberController {
    private final MemberService memberService = new MemberService();
    // 문제점 1: 매번 새로운 인스턴스 생성
    // 문제점 2: MemberService의 구현에 강하게 결합
    // 문제점 3: 테스트가 어려움
}

public class MemberService {
    private final MemberRepository memberRepository = new MemoryMemberRepository();
    // 문제점 1: 구현체에 직접 의존
    // 문제점 2: Repository 변경 시 Service 코드 수정 필요
}
```

**이 코드의 문제점**은 무엇일까요?

---

### 1.2 문제점 1: 인스턴스 중복 생성 (메모리 낭비)

`MemberService`는 특별한 상태를 갖지 않는 **무상태(Stateless)** 객체입니다. 즉, 하나만 만들어서 공유해도 충분합니다.

하지만 `new` 연산자를 사용하면 **컨트롤러가 생성될 때마다 새로운 Service 인스턴스가 만들어집니다**.

```java
// ❌ 안티패턴: 여러 컨트롤러가 각각 Service를 생성
@Controller
public class MemberController {
    private final MemberService memberService = new MemberService();  // 인스턴스 1
}

@Controller
public class OrderController {
    private final MemberService memberService = new MemberService();  // 인스턴스 2
}

@Controller
public class ProductController {
    private final MemberService memberService = new MemberService();  // 인스턴스 3
}
```

**다이어그램:**
```
[문제 상황: 중복 인스턴스 생성]

MemberController ──new──> MemberService 인스턴스1 (메모리 주소: 0x1001)
OrderController  ──new──> MemberService 인스턴스2 (메모리 주소: 0x1002)
ProductController──new──> MemberService 인스턴스3 (메모리 주소: 0x1003)

→ 같은 기능을 하는 객체가 3개 생성됨 (메모리 낭비)
→ 각 인스턴스를 개별 관리해야 함 (관리 복잡도 증가)
```

**왜 문제인가?**
- **메모리 낭비**: 동일한 기능을 하는 객체가 여러 개 생성됨
- **비효율**: JVM의 GC(Garbage Collector) 부담 증가
- **관리 어려움**: 각 인스턴스의 상태를 추적하기 어려움

---

### 1.3 문제점 2: 강한 결합 (Tight Coupling)

`new MemoryMemberRepository()`처럼 구체적인 구현 클래스를 직접 생성하면, **구현체에 강하게 결합(Tight Coupling)**됩니다.

```java
// ❌ 문제: 구현체에 직접 의존
public class MemberService {
    private final MemberRepository memberRepository = new MemoryMemberRepository();
    // MemoryMemberRepository라는 구체 클래스에 의존
    // 나중에 JpaMemberRepository로 변경하려면?
    // → 이 코드를 직접 수정해야 함!
}
```

**구현체를 변경하려면 어떻게 해야 할까요?**

```java
// ❌ 변경 시마다 코드 수정 필요
public class MemberService {
    // private final MemberRepository memberRepository = new MemoryMemberRepository();
    private final MemberRepository memberRepository = new JpaMemberRepository();
    // Service 코드를 수정해야 함!
}
```

**다이어그램:**
```
[강한 결합의 문제]

MemberService ──────> MemoryMemberRepository (구체 클래스)
    │
    └─ 만약 JpaMemberRepository로 변경하려면?
       → MemberService 코드를 직접 수정해야 함
       → 유연성 부족, 확장성 저하
```

**왜 문제인가?**
- **유연성 부족**: Repository 구현체를 쉽게 교체할 수 없음
- **OCP 위반**: 개방-폐쇄 원칙(확장에는 열려있고 수정에는 닫혀있어야 함) 위반
- **DIP 위반**: 의존관계 역전 원칙(추상화에 의존해야지 구체화에 의존하면 안 됨) 위반

---

### 1.4 문제점 3: 테스트 어려움

`new` 연산자로 직접 객체를 생성하면, **테스트할 때 Mock 객체를 주입하기 어렵습니다**.

```java
// ❌ 테스트가 어려운 코드
public class MemberService {
    private final MemberRepository memberRepository = new MemoryMemberRepository();
    // 테스트 시 Mock Repository를 주입할 수 없음!
}

// ❌ 테스트 코드 작성 불가
@Test
void 회원가입_테스트() {
    // Mock Repository를 주입하고 싶지만...
    // MemberService 내부에서 이미 new로 생성해버림
    // 어떻게 테스트하지?
}
```

**테스트 시나리오:**
```
1. Repository를 Mock으로 만들고 싶음
   - 실제 DB 없이 테스트하려고

2. 하지만 Service 내부에서 이미 new MemoryMemberRepository() 호출
   - Mock을 주입할 방법이 없음

3. 결과: 통합 테스트만 가능, 단위 테스트 불가
```

**왜 문제인가?**
- **단위 테스트 불가**: Service 로직만 독립적으로 테스트할 수 없음
- **Mock 주입 불가**: 테스트용 가짜 객체를 주입할 방법이 없음
- **테스트 속도 저하**: 실제 Repository를 사용해야 하므로 느림

---

### 1.5 개선 방향: 의존성 주입(DI)

위 문제들을 해결하려면 **의존성 주입(Dependency Injection)** 패턴을 사용해야 합니다.

```java
// ✅ 개선된 코드 (미리보기)
public class MemberService {
    private final MemberRepository memberRepository;

    // 생성자로 외부에서 주입받음
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
```

**개선 효과:**
```
1. 인스턴스 중복 생성 → 싱글톤 패턴으로 해결
2. 강한 결합 → 인터페이스 의존으로 느슨한 결합
3. 테스트 어려움 → 생성자 주입으로 Mock 주입 가능
```

**다이어그램:**
```
[개선 후: 스프링 컨테이너 사용]

┌─────────────────────────────────────┐
│      Spring Container               │
│                                      │
│  MemberService (단일 인스턴스)        │
│  MemberRepository (단일 인스턴스)     │
│                                      │
└─────────────────────────────────────┘
         ↓ 주입         ↓ 주입
  MemberController  OrderController

→ 모든 컨트롤러가 같은 Service 인스턴스 공유
→ Repository 교체 시 Spring 설정만 변경
→ 테스트 시 Mock 주입 가능
```

하지만 의존성 주입을 수동으로 하면 번거롭습니다. **스프링은 이를 자동화**해줍니다!

---

### 1.6 요약: 왜 스프링이 필요한가?

| 문제점 | new 연산자 사용 | 스프링 사용 |
|--------|---------------|------------|
| **인스턴스 관리** | 매번 새로 생성 (메모리 낭비) | 싱글톤으로 관리 (재사용) |
| **결합도** | 구체 클래스에 강하게 결합 | 인터페이스에 느슨하게 결합 |
| **유연성** | 구현체 변경 시 코드 수정 필요 | 설정만 변경 |
| **테스트** | Mock 주입 불가 | 생성자 주입으로 Mock 가능 |
| **관리 주체** | 개발자가 직접 관리 | 스프링 컨테이너가 관리 |

**결론**: 스프링을 사용하면 **객체의 생성과 의존관계 설정을 자동화**할 수 있습니다. 이것이 바로 **IoC(제어의 역전)**와 **DI(의존성 주입)**입니다.

---

## 2. Spring Container와 Spring Bean

이제 스프링이 어떻게 객체를 관리하는지 살펴봅시다.

### 2.1 Spring Container란?

**Spring Container**는 스프링에서 객체(Bean)를 생성하고 관리하는 컨테이너입니다.

```
┌────────────────────────────────────────────┐
│    Spring Container (ApplicationContext)   │
│                                             │
│  객체 생성    →  의존관계 주입  →  초기화   │
│                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │Service A │  │Service B │  │Service C │ │
│  └──────────┘  └──────────┘  └──────────┘ │
│         ↑             ↓            ↓       │
│         └─────────────┴────────────┘       │
│            의존관계 자동 연결                │
└────────────────────────────────────────────┘
```

**주요 역할:**
1. **객체 생성**: 개발자가 `new`를 쓰지 않아도 스프링이 객체를 생성
2. **의존관계 주입**: 필요한 객체를 자동으로 연결
3. **생명주기 관리**: 초기화부터 소멸까지 관리

---

### 2.2 Spring Bean이란?

**Spring Bean**은 스프링 컨테이너가 관리하는 자바 객체입니다.

```java
// 일반 자바 객체
MemberService service = new MemberService();  // 개발자가 직접 생성

// 스프링 빈
@Service
public class MemberService { }  // 스프링이 생성하고 관리
```

**Bean의 특징:**

1. **싱글톤으로 관리됨**
   ```java
   MemberService service1 = applicationContext.getBean(MemberService.class);
   MemberService service2 = applicationContext.getBean(MemberService.class);

   System.out.println(service1 == service2);  // true (같은 인스턴스)
   ```

2. **컨테이너에 등록됨**
   ```
   Spring Container
   ├─ memberController (Bean)
   ├─ memberService (Bean)
   └─ memoryMemberRepository (Bean)
   ```

3. **의존관계가 자동으로 연결됨**
   ```
   memberController
       ↓ 주입
   memberService
       ↓ 주입
   memoryMemberRepository
   ```

---

### 2.3 ApplicationContext

`ApplicationContext`는 스프링 컨테이너의 인터페이스입니다.

```java
@SpringBootApplication
public class HelloSpringApplication {
    public static void main(String[] args) {
        // SpringApplication.run()이 ApplicationContext를 반환
        ApplicationContext ac = SpringApplication.run(HelloSpringApplication.class, args);

        // 등록된 빈 조회
        String[] beanNames = ac.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            System.out.println("beanName = " + beanName);
        }

        // 특정 빈 가져오기
        MemberService memberService = ac.getBean(MemberService.class);
    }
}
```

**ApplicationContext의 기능:**
- 빈 조회: `getBean()`
- 빈 이름 목록: `getBeanDefinitionNames()`
- 빈 타입 목록: `getBeanNamesForType()`
- 환경 설정: `getEnvironment()`
- 리소스 로딩: `getResource()`

---

### 2.4 IoC (Inversion of Control) - 제어의 역전

**IoC**는 "제어의 역전"을 의미합니다. 프로그램의 제어 흐름을 개발자가 아닌 프레임워크가 가져가는 것입니다.

```java
// ❌ 전통적 방식: 개발자가 제어
public class MemberService {
    private MemberRepository repository = new MemoryMemberRepository();
    // 개발자가 직접 객체를 생성하고 관리
}

// ✅ IoC: 스프링이 제어
@Service
public class MemberService {
    private final MemberRepository repository;

    @Autowired
    public MemberService(MemberRepository repository) {
        this.repository = repository;
        // 스프링이 객체를 생성하고 주입
    }
}
```

**제어의 역전이란?**

```
[전통적 방식]
개발자 ──생성──> 객체
개발자 ──관리──> 객체
개발자 ──주입──> 의존성

[IoC 방식]
스프링 ──생성──> 객체
스프링 ──관리──> 객체
스프링 ──주입──> 의존성
개발자는 설정만 제공 (@Service, @Autowired)
```

**IoC의 장점:**
- **관심사 분리**: 비즈니스 로직에만 집중 가능
- **테스트 용이성**: Mock 객체 주입이 쉬움
- **코드 재사용성**: 같은 객체를 여러 곳에서 재사용

---

### 2.5 DI (Dependency Injection) - 의존성 주입

**DI**는 "의존성 주입"을 의미합니다. 객체가 필요로 하는 의존성을 외부에서 주입하는 것입니다.

```java
// ❌ 의존성을 직접 생성
public class MemberService {
    private MemberRepository repository = new MemoryMemberRepository();
    // MemberService가 직접 Repository를 생성
}

// ✅ 의존성을 주입받음
public class MemberService {
    private final MemberRepository repository;

    public MemberService(MemberRepository repository) {
        this.repository = repository;  // 외부에서 주입받음
    }
}
```

**DI의 3가지 방법:**

1. **생성자 주입** (권장) ⭐
   ```java
   @Service
   public class MemberService {
       private final MemberRepository repository;

       @Autowired
       public MemberService(MemberRepository repository) {
           this.repository = repository;
       }
   }
   ```

2. **필드 주입** (비권장) ⚠️
   ```java
   @Service
   public class MemberService {
       @Autowired
       private MemberRepository repository;
   }
   ```

3. **세터 주입**
   ```java
   @Service
   public class MemberService {
       private MemberRepository repository;

       @Autowired
       public void setRepository(MemberRepository repository) {
           this.repository = repository;
       }
   }
   ```

**왜 생성자 주입을 권장하는가?** (섹션 4에서 자세히 설명)

---

### 2.6 Bean 생명주기 (Bean Lifecycle)

스프링 빈은 다음과 같은 생명주기를 가집니다.

```
1. 스프링 컨테이너 생성
   ↓
2. 스프링 빈 생성
   - 생성자 호출
   ↓
3. 의존관계 주입
   - @Autowired 처리
   ↓
4. 초기화 콜백
   - @PostConstruct
   ↓
5. 사용
   - 애플리케이션 동작
   ↓
6. 소멸 전 콜백
   - @PreDestroy
   ↓
7. 스프링 종료
```

**생명주기 콜백 예시:**

```java
@Service
public class MemberService {

    @PostConstruct
    public void init() {
        System.out.println("MemberService 초기화");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("MemberService 소멸");
    }
}
```

**출력:**
```
MemberService 초기화
... (애플리케이션 사용)
MemberService 소멸
```

---

### 2.7 요약: Spring Container의 역할

```
┌──────────────────────────────────────────────────┐
│          Spring Container                        │
│                                                   │
│  1. 컴포넌트 스캔으로 @Component 찾기              │
│     @Controller, @Service, @Repository           │
│                                                   │
│  2. 찾은 클래스로 Bean 객체 생성                    │
│     new MemberController()                        │
│     new MemberService()                           │
│     new MemoryMemberRepository()                  │
│                                                   │
│  3. @Autowired 찾아서 의존관계 주입                │
│     memberController → memberService              │
│     memberService → memberRepository              │
│                                                   │
│  4. 초기화 콜백 호출                               │
│     @PostConstruct 메서드 실행                     │
│                                                   │
│  5. 애플리케이션 사용 (Bean을 싱글톤으로 재사용)     │
│                                                   │
│  6. 종료 시 소멸 콜백 호출                          │
│     @PreDestroy 메서드 실행                        │
└──────────────────────────────────────────────────┘
```

**핵심 정리:**
- **Spring Container**: Bean을 생성하고 관리하는 컨테이너
- **Spring Bean**: 스프링이 관리하는 객체 (싱글톤)
- **IoC**: 제어의 역전 (스프링이 객체 생성과 관리를 담당)
- **DI**: 의존성 주입 (외부에서 의존 객체를 주입)

---

## 3. 컴포넌트 스캔(Component Scan)

스프링은 어떻게 어떤 클래스를 Bean으로 등록할지 알 수 있을까요? 바로 **컴포넌트 스캔(Component Scan)** 덕분입니다.

### 3.1 컴포넌트 스캔이란?

**컴포넌트 스캔**은 `@Component` 어노테이션이 붙은 클래스를 찾아서 자동으로 스프링 빈으로 등록하는 기능입니다.

```java
@SpringBootApplication
public class HelloSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(HelloSpringApplication.class, args);
        // 이 메서드 안에서 컴포넌트 스캔이 일어남
    }
}
```

**@SpringBootApplication 내부:**
```java
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan  // ← 여기!
public @interface SpringBootApplication {
}
```

스프링 부트는 `@ComponentScan`을 기본으로 포함하고 있어서, **별도 설정 없이 자동으로 컴포넌트 스캔이 동작**합니다.

---

### 3.2 컴포넌트 스캔 동작 흐름

```
[1단계] 스프링 부트 애플리케이션 시작
    ↓
[2단계] @SpringBootApplication이 있는 패키지를 시작점으로 설정
    예: hello.hello_spring
    ↓
[3단계] 해당 패키지 및 하위 패키지를 탐색
    hello.hello_spring
    ├── hello.hello_spring.controller
    ├── hello.hello_spring.service
    └── hello.hello_spring.repository
    ↓
[4단계] @Component 및 파생 어노테이션을 찾음
    - @Controller
    - @Service
    - @Repository
    - @Component
    ↓
[5단계] 찾은 클래스를 Bean으로 등록
    memberController → Bean 등록
    memberService → Bean 등록
    memoryMemberRepository → Bean 등록
    ↓
[6단계] @Autowired가 붙은 생성자에 의존관계 주입
    memberController ← memberService 주입
    memberService ← memberRepository 주입
```

---

### 3.3 컴포넌트 스캔 범위

**기본 스캔 범위**: `@SpringBootApplication`이 있는 패키지와 그 하위 패키지

```
hello.hello_spring                    ← @SpringBootApplication 위치
├── HelloSpringApplication.java
├── controller/
│   └── MemberController.java         ✅ 스캔됨 (하위 패키지)
├── service/
│   └── MemberService.java            ✅ 스캔됨 (하위 패키지)
└── repository/
    └── MemoryMemberRepository.java   ✅ 스캔됨 (하위 패키지)

hello.other_package/
└── OtherService.java                 ❌ 스캔 안 됨 (범위 밖)
```

**⚠️ 주의사항:**

```java
// ❌ 스캔 범위 밖에 @Service를 붙여도 Bean으로 등록되지 않음!
package hello.other_package;

@Service
public class OtherService {
    // 이 클래스는 hello.hello_spring 패키지 밖에 있으므로
    // 컴포넌트 스캔 대상이 아님!
}
```

**스캔 범위 변경 방법:**

```java
// 명시적으로 스캔 범위 지정
@SpringBootApplication
@ComponentScan(basePackages = {"hello.hello_spring", "hello.other_package"})
public class HelloSpringApplication {
}
```

하지만 **권장하지 않습니다**. 스프링 부트의 기본 규칙을 따르는 것이 좋습니다.
- `@SpringBootApplication`을 프로젝트 최상위 패키지에 두기
- 모든 클래스를 그 하위 패키지에 배치

---

### 3.4 스테레오타입 어노테이션 (@Component의 파생)

스프링은 계층별로 `@Component`를 특화한 **스테레오타입 어노테이션**을 제공합니다.

| 어노테이션 | 계층 | 역할 | 추가 기능 |
|-----------|------|------|----------|
| **@Controller** | Presentation | HTTP 요청 처리 | - Spring MVC 기능 활성화<br>- @RequestMapping 인식 |
| **@Service** | Business | 비즈니스 로직 | - 특별한 기능 없음 (의미 부여)<br>- 트랜잭션 AOP 적용 대상 |
| **@Repository** | Persistence | 데이터 접근 | - JPA/JDBC 예외를 스프링 예외로 변환<br>- DataAccessException 변환 |
| **@Component** | 공통 | 일반 빈 | - 기본 빈 등록 기능만 |

**각 어노테이션의 내부:**

```java
@Component
public @interface Controller {
}

@Component
public @interface Service {
}

@Component
public @interface Repository {
}
```

→ 모두 `@Component`를 포함하고 있어서 컴포넌트 스캔 대상이 됩니다!

---

### 3.5 각 스테레오타입 어노테이션 사용 예시

#### 3.5.1 @Controller

**역할**: HTTP 요청을 받아 처리하는 계층

```java
package hello.hello_spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller  // ← 컴포넌트 스캔 대상
public class MemberController {

    @GetMapping("/members")
    public String list() {
        return "members/memberList";
    }
}
```

**컴포넌트 스캔 결과:**
```
스프링 컨테이너에 등록됨
Bean 이름: memberController (클래스 이름의 첫 글자를 소문자로)
Bean 타입: MemberController
```

---

#### 3.5.2 @Service

**역할**: 비즈니스 로직을 처리하는 계층

```java
package hello.hello_spring.service;

import org.springframework.stereotype.Service;

@Service  // ← 컴포넌트 스캔 대상
public class MemberService {

    public Long join(Member member) {
        // 중복 회원 검증
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }
}
```

**컴포넌트 스캔 결과:**
```
스프링 컨테이너에 등록됨
Bean 이름: memberService
Bean 타입: MemberService
```

**💡 실무 팁**: `@Service`는 기술적으로 특별한 기능은 없지만, **"이 클래스는 비즈니스 로직을 담당한다"**는 의미를 명확히 합니다. 나중에 트랜잭션 AOP를 적용할 때도 `@Service`가 붙은 클래스를 대상으로 할 수 있습니다.

---

#### 3.5.3 @Repository

**역할**: 데이터 접근 로직을 처리하는 계층

```java
package hello.hello_spring.repository;

import org.springframework.stereotype.Repository;

@Repository  // ← 컴포넌트 스캔 대상
public class MemoryMemberRepository implements MemberRepository {

    private static Map<Long, Member> store = new HashMap<>();

    @Override
    public Member save(Member member) {
        store.put(member.getId(), member);
        return member;
    }
}
```

**컴포넌트 스캔 결과:**
```
스프링 컨테이너에 등록됨
Bean 이름: memoryMemberRepository
Bean 타입: MemoryMemberRepository
```

**@Repository의 특별한 기능:**

```java
// JPA 사용 시
@Repository
public class JpaMemberRepository implements MemberRepository {

    public Member save(Member member) {
        em.persist(member);  // JPA 예외 발생 가능
        // PersistenceException 발생 시
        // → 스프링의 DataAccessException으로 변환
    }
}
```

→ **DB 기술에 종속적인 예외를 스프링의 추상화된 예외로 변환**해줍니다.

---

#### 3.5.4 @Component

**역할**: 위 3가지에 해당하지 않는 일반적인 빈

```java
@Component
public class EmailSender {
    public void sendEmail(String to, String message) {
        // 이메일 전송 로직
    }
}
```

**언제 사용하는가?**
- Controller, Service, Repository 어디에도 속하지 않는 경우
- 유틸리티 클래스
- 설정 클래스 등

---

### 3.6 Bean 이름 규칙

스프링은 클래스 이름을 기반으로 Bean 이름을 자동 생성합니다.

```java
@Service
public class MemberService { }
// Bean 이름: memberService (첫 글자만 소문자)

@Controller
public class MemberController { }
// Bean 이름: memberController

@Repository
public class MemoryMemberRepository { }
// Bean 이름: memoryMemberRepository
```

**Bean 이름 직접 지정:**

```java
@Service("myMemberService")  // 이름 직접 지정
public class MemberService { }
// Bean 이름: myMemberService
```

하지만 **기본 이름 사용을 권장**합니다. 명시적으로 지정하면 오히려 혼란을 줄 수 있습니다.

---

### 3.7 컴포넌트 스캔 필터

특정 클래스를 스캔 대상에 포함하거나 제외할 수 있습니다.

**포함 필터:**
```java
@ComponentScan(
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = MyIncludeComponent.class
    )
)
```

**제외 필터:**
```java
@ComponentScan(
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = Configuration.class
    )
)
```

**💡 실무 팁**: 대부분의 경우 기본 설정으로 충분합니다. 필터는 거의 사용하지 않습니다.

---

### 3.8 요약: 컴포넌트 스캔

```
[컴포넌트 스캔 전체 흐름]

1. @SpringBootApplication 실행
   ↓
2. @ComponentScan 동작
   ↓
3. 패키지 탐색 (hello.hello_spring.*)
   ↓
4. @Component 및 파생 어노테이션 검색
   ├─ @Controller 발견 → MemberController 빈 등록
   ├─ @Service 발견 → MemberService 빈 등록
   └─ @Repository 발견 → MemoryMemberRepository 빈 등록
   ↓
5. 빈 이름 자동 생성 (클래스 이름의 첫 글자를 소문자로)
   ↓
6. 스프링 컨테이너에 등록
```

**핵심 정리:**
- **@ComponentScan**: @Component가 붙은 클래스를 찾아 빈으로 등록
- **스캔 범위**: @SpringBootApplication이 있는 패키지와 하위 패키지
- **스테레오타입 어노테이션**: @Controller, @Service, @Repository
- **Bean 이름**: 클래스 이름의 첫 글자를 소문자로 (예: MemberService → memberService)

---

## 4. 의존관계 자동 주입(@Autowired)

컴포넌트 스캔으로 Bean을 등록했다면, 이제 Bean 간의 의존관계를 설정해야 합니다. 이때 사용하는 것이 `@Autowired`입니다.

### 4.1 의존관계 주입(DI)의 필요성

**MemberController**는 **MemberService**가 필요합니다. 이 관계를 어떻게 설정할까요?

```java
// ❌ 나쁜 방법: new 연산자로 직접 생성
@Controller
public class MemberController {
    private final MemberService memberService = new MemberService();
    // 문제: 스프링 컨테이너가 관리하는 빈이 아님
    // → 싱글톤 X, 의존성 주입 X
}

// ✅ 좋은 방법: 스프링이 관리하는 빈을 주입
@Controller
public class MemberController {
    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
        // 스프링이 MemberService 빈을 찾아서 주입
    }
}
```

**@Autowired의 역할:**
1. 스프링 컨테이너에서 `MemberService` 타입의 빈을 찾음
2. 찾은 빈을 생성자 파라미터로 주입
3. 의존관계 설정 완료

---

### 4.2 의존성 주입(DI)의 3가지 방법

스프링은 3가지 DI 방법을 지원합니다.

---

#### 4.2.1 생성자 주입 (Constructor Injection) ⭐ 권장

**가장 권장되는 방법**입니다.

```java
@Controller
public class MemberController {
    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }
}
```

**장점:**

1. **final 사용 가능 (불변성)**
   ```java
   private final MemberService memberService;  // final 가능!
   // 한 번 주입되면 변경 불가
   ```

2. **필수 의존성 명확**
   ```java
   // 생성자를 통해서만 객체 생성 가능
   // → 필수 의존성이 명확함
   MemberController controller = new MemberController(memberService);
   ```

3. **순환 참조 컴파일 시점 감지**
   ```java
   // AService → BService → AService 순환 참조 시
   // 컴파일 단계에서 에러 발생
   ```

4. **테스트 용이**
   ```java
   @Test
   void 테스트() {
       MemberService mockService = mock(MemberService.class);
       MemberController controller = new MemberController(mockService);
       // Mock 주입이 쉬움
   }
   ```

---

#### 생성자 주입의 핵심 개념: "애플리케이션 조립"

MemberController.java의 주석에서:
> "처음에 애플리케이션이 조립된다고 표현을 한다."

이 말의 의미는:
- 스프링 애플리케이션 시작 시 모든 빈을 생성하고 조립
- **조립 시점(애플리케이션 시작 시)에 딱 한 번만 의존관계 설정**
- 이후에는 변경 불가 (불변성)

**조립(Assembly) 개념 설명:**

```java
// 애플리케이션 시작 시 (조립 시점)
MemoryMemberRepository repository = new MemoryMemberRepository();
MemberService service = new MemberService(repository);
MemberController controller = new MemberController(service);
// ↑ 이 시점에 모든 의존관계가 "조립"됨

// 이후 애플리케이션 실행 중에는...
controller.memberService = ???;  // 불가능! (final이므로)
// → 조립 후에는 변경할 수 없음
```

**왜 "조립"이라는 표현을 쓸까?**

자동차 조립 과정을 생각해보세요:
1. 부품(빈)들을 준비
2. 조립 라인에서 부품을 결합 (의존관계 주입)
3. 조립 완료 후 출고 (애플리케이션 실행)
4. 출고 후에는 조립 라인으로 돌아갈 수 없음

**실전 예시:**

```java
// ✅ 생성자 주입: 조립 시점에 고정
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
        // 조립 시점에 결정되고, 이후 변경 불가
    }
}

// ❌ 세터 주입: 조립 후에도 변경 가능 (위험!)
@Service
public class MemberService {
    private MemberRepository memberRepository;

    public void setMemberRepository(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
        // 언제든지 변경 가능 → 예측 불가능
    }
}
```

MemberService.java의 주석에서:
> "조립 시점에 딱 생성자로 한 번만 조립해 놓고 얘를 끝을 내버려야 한다."

즉:
- **조립 시점**: 애플리케이션 시작 시 생성자로 의존관계 설정
- **끝을 낸다**: 이후에는 변경하지 않음 (불변성 유지)

**💡 실무 팁: 생성자가 1개면 @Autowired 생략 가능**

```java
@Controller
public class MemberController {
    private final MemberService memberService;

    // @Autowired 생략 가능! (스프링 4.3 이상)
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }
}
```

---

#### 4.2.2 필드 주입 (Field Injection) ⚠️ 비권장

```java
@Controller
public class MemberController {
    @Autowired
    private MemberService memberService;  // final 불가!
}
```

**단점:**

1. **final 사용 불가 (불변성 보장 안 됨)**
   ```java
   @Autowired
   private MemberService memberService;  // final 못 붙임
   // 나중에 변경 가능 → 위험
   ```

2. **테스트 어려움**
   ```java
   @Test
   void 테스트() {
       MemberController controller = new MemberController();
       // memberService가 null → NullPointerException!
       // 테스트용 Mock을 주입하기 어려움
   }
   ```

3. **순환 참조 런타임에서야 발견**
   ```java
   // AService ↔ BService 순환 참조
   // 애플리케이션 실행 시점에 에러 발생
   // → 늦은 발견
   ```

4. **DI 컨테이너 없이 사용 불가**
   ```java
   // 순수 자바로 테스트 불가
   MemberController controller = new MemberController();
   // memberService가 주입되지 않음
   ```

---

#### 필드 주입의 치명적 문제: "바꿔치기 불가능"

MemberController.java의 주석에서:
> "중간에 바꿔치기 할 수 있는 방법이 없다."

이 말의 의미는:
- 한번 스프링이 주입하면 테스트 시 Mock으로 교체 불가
- 외부에서 접근할 방법이 없음
- setter도 없고, 생성자도 없음

**구체적인 문제 상황:**

```java
// ❌ 필드 주입: 바꿔치기 불가
@Controller
public class MemberController {
    @Autowired
    private MemberService memberService;
    // memberService를 어떻게 바꿀까? → 불가능!
}

// 테스트에서 Mock 주입 시도
@Test
void 테스트() {
    MemberController controller = new MemberController();
    MemberService mockService = mock(MemberService.class);

    // memberService에 어떻게 mockService를 주입?
    // → setter가 없어서 불가능!
    // → 생성자도 없어서 불가능!
    // → private 필드라 직접 접근도 불가능!

    controller.memberService = mockService;  // 컴파일 에러!
}
```

**비교: 생성자 주입은 바꿔치기 가능**

```java
// ✅ 생성자 주입: 바꿔치기 가능
@Controller
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }
}

// 테스트에서 Mock 주입 가능!
@Test
void 테스트() {
    MemberService mockService = mock(MemberService.class);
    MemberController controller = new MemberController(mockService);
    // ✅ 생성자로 쉽게 Mock 주입!
}

// 실제 코드에서는 실제 Service 주입
MemberController realController = new MemberController(realService);
```

**시각화:**

```
[필드 주입 - 바꿔치기 불가]
┌──────────────────────┐
│ MemberController     │
│                      │
│ @Autowired           │
│ private Service      │ ← 스프링만 주입 가능
│                      │   개발자는 접근 불가!
└──────────────────────┘
     ↑
     │ (스프링만 가능)
     │
  스프링 컨테이너


[생성자 주입 - 바꿔치기 가능]
┌──────────────────────┐
│ MemberController     │
│                      │
│ Controller(Service)  │ ← 생성자로 주입
│                      │   누구나 접근 가능!
└──────────────────────┘
     ↑
     │ (생성자로 주입)
     │
  개발자 or 스프링
```

**실무 함정:**

필드 주입을 사용하면 테스트할 때 리플렉션을 사용해야 합니다:

```java
// ❌ 필드 주입 시 테스트 코드 (복잡함!)
@Test
void 필드주입_테스트() throws Exception {
    MemberController controller = new MemberController();
    MemberService mockService = mock(MemberService.class);

    // Reflection 사용해서 private 필드 강제 주입
    Field field = MemberController.class.getDeclaredField("memberService");
    field.setAccessible(true);
    field.set(controller, mockService);
    // → 너무 복잡하고 번거로움!
}
```

**언제 사용하는가?**
- 테스트 코드에서 간단히 사용할 때 (프로덕션 코드에서는 비권장)
- `@SpringBootTest` 같은 통합 테스트에서

**⚠️ 실무에서는 거의 사용하지 않습니다!**

---

#### 4.2.3 세터 주입 (Setter Injection)

```java
@Controller
public class MemberController {
    private MemberService memberService;  // final 불가

    @Autowired
    public void setMemberService(MemberService memberService) {
        this.memberService = memberService;
    }
}
```

**장점:**
- 선택적 의존성에 사용 가능
  ```java
  @Autowired(required = false)
  public void setOptionalService(OptionalService service) {
      this.optionalService = service;
  }
  ```

**단점:**
- final 사용 불가
- 의존성이 변경될 수 있음 (불변성 X)
- 필수 의존성이 명확하지 않음

---

#### 세터 주입의 문제점: "public 노출"과 "불필요한 개방"

MemberController.java의 주석에서:
> "누군가가 멤버 컨트롤을 호출했을 때, public으로 열려있어야 한다."
> "한번 세팅이 되고 나면 중간에 뭘 바꿀 일이 없다."

MemberService.java의 주석에서:
> "조립 시점에 딱 생성자로 한 번만 조립해 놓고 얘를 끝을 내버려야 한다."
> "개발은 최대한 변경, 호출하지 않아야 될 메서드가 호출되면 안된다."

이 말들의 의미는:
- Setter가 public이므로 누구나 호출 가능 → 보안 위험
- 애플리케이션 로딩 시점(조립 시점)에만 설정하면 되는데, 왜 계속 public으로 열어둬야 하는가?
- 실수로 setter를 다시 호출하면 의존성이 바뀔 위험

**구체적인 위험 시나리오:**

```java
// ❌ Setter 주입: public으로 열려있음
@Service
public class MemberService {
    private MemberRepository memberRepository;

    @Autowired
    public void setMemberRepository(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}

// 애플리케이션 실행 중...
@Controller
public class SomeController {
    @Autowired
    private MemberService memberService;

    public void someMethod() {
        // 실수로 또는 악의적으로 setter 호출 가능!
        memberService.setMemberRepository(null);  // 💣 위험!
        // → 다른 곳에서 사용 중인 memberService의 repository가 null이 됨
        // → 런타임 에러 발생!

        // 또는 잘못된 구현체로 교체
        memberService.setMemberRepository(hackedRepository);  // 💣 보안 위험!
    }
}
```

**비교: 생성자 주입은 조립 후 변경 불가**

```java
// ✅ 생성자 주입: 조립 시점에만 설정
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
        // 조립 시점(애플리케이션 시작 시)에 딱 한 번만 설정
    }

    // setter가 없으므로 외부에서 변경 불가!
}

// 애플리케이션 실행 중...
@Controller
public class SomeController {
    @Autowired
    private MemberService memberService;

    public void someMethod() {
        memberService.setMemberRepository(null);
        // 컴파일 에러! setter가 없어서 불가능
        // → 안전함!
    }
}
```

**시각화: 조립 시점 vs 실행 시점**

```
[Setter 주입 - 언제든 변경 가능]

조립 시점 (애플리케이션 시작)
  ↓
service.setRepository(realRepo);  ✅ 정상 주입

실행 시점 (애플리케이션 운영 중)
  ↓
service.setRepository(null);      ❌ 문제 발생 가능!
service.setRepository(hackedRepo); ❌ 보안 위험!


[생성자 주입 - 조립 시점에만 설정]

조립 시점 (애플리케이션 시작)
  ↓
new MemberService(realRepo);  ✅ 정상 주입

실행 시점 (애플리케이션 운영 중)
  ↓
service.setRepository(null);  ❌ 메서드 자체가 없음!
// → 변경 불가능 (final) → 안전!
```

**실무 사례:**

```java
// ❌ Setter 주입 시 발생할 수 있는 버그
@Service
public class OrderService {
    private PaymentService paymentService;

    @Autowired
    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void processOrder(Order order) {
        paymentService.pay(order);  // paymentService가 null이면? NPE!
    }
}

// 어딘가에서 실수로...
orderService.setPaymentService(null);
// → 다른 사용자가 주문할 때 에러 발생!


// ✅ 생성자 주입: 이런 일이 애초에 불가능
@Service
public class OrderService {
    private final PaymentService paymentService;  // final!

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
        // 조립 시점에 설정, 이후 변경 불가
    }

    public void processOrder(Order order) {
        paymentService.pay(order);  // 항상 안전!
    }
}
// setter가 없으므로 변경 자체가 불가능
```

**핵심 원칙:**

MemberService.java의 주석:
> "조립 시점에 딱 생성자로 한 번만 조립해 놓고 얘를 끝을 내버려야 한다."

즉:
- **조립 시점**: 애플리케이션 시작 시 한 번만 의존관계 설정
- **끝을 낸다**: 이후에는 변경하지 않음
- **왜?**: 변경할 이유도 없고, 변경되면 위험하기 때문

**언제 사용하는가?**
- 선택적 의존성일 때 (거의 없음)
- 레거시 코드 유지보수

**💡 실무에서는 거의 사용하지 않습니다.**

**💡 참고**: @Autowired가 언제 동작하고 언제 동작하지 않는지에 대한 자세한 내용은 [6.7 스프링 컨테이너 관리와 @Autowired의 관계](#67-스프링-컨테이너-관리와-autowired의-관계)를 참조하세요.

---

### 4.3 세 가지 방식 비교표

| 구분 | 생성자 주입 | 필드 주입 | 세터 주입 |
|------|-----------|----------|----------|
| **final 사용** | ✅ 가능 | ❌ 불가 | ❌ 불가 |
| **불변성 보장** | ✅ 보장 | ❌ 미보장 | ❌ 미보장 |
| **테스트 용이성** | ✅ 쉬움 | ❌ 어려움 | △ 보통 |
| **순환 참조 감지** | 컴파일 시점 | 런타임 | 런타임 |
| **필수 의존성 명확성** | ✅ 명확 | ❌ 불명확 | ❌ 불명확 |
| **코드 간결성** | △ 보통 | ✅ 간결 | ❌ 장황 |
| **권장 여부** | ⭐ 강력 권장 | ❌ 비권장 | △ 선택적 의존성만 |

---

### 4.4 왜 생성자 주입을 권장하는가?

#### 4.4.1 불변성 (Immutability)

```java
// ✅ 생성자 주입: final 사용 가능
@Service
public class MemberService {
    private final MemberRepository repository;  // 변경 불가!

    public MemberService(MemberRepository repository) {
        this.repository = repository;
    }
}

// ❌ 필드 주입: final 사용 불가
@Service
public class MemberService {
    @Autowired
    private MemberRepository repository;  // 변경 가능 → 위험!
}
```

**왜 불변이 중요한가?**
- 멀티스레드 환경에서 안전
- 예측 가능한 동작
- 버그 감소

---

#### 4.4.2 테스트 용이성

```java
// ✅ 생성자 주입: 순수 자바로 테스트 가능
@Test
void 회원가입_테스트() {
    MemberRepository mockRepo = mock(MemberRepository.class);
    MemberService service = new MemberService(mockRepo);  // 쉬움!

    Member member = new Member();
    service.join(member);
}

// ❌ 필드 주입: 스프링 컨테이너 필요
@SpringBootTest
class MemberServiceTest {
    @Autowired
    MemberService service;  // 스프링 컨테이너에 의존

    @Test
    void 회원가입_테스트() {
        // 순수 자바로 테스트 불가
    }
}
```

---

#### 4.4.3 순환 참조 조기 발견

```java
// 순환 참조 예시
@Service
public class AService {
    private final BService bService;

    @Autowired
    public AService(BService bService) {  // BService 필요
        this.bService = bService;
    }
}

@Service
public class BService {
    private final AService aService;

    @Autowired
    public BService(AService aService) {  // AService 필요 → 순환!
        this.aService = aService;
    }
}
```

**생성자 주입 사용 시:**
```
애플리케이션 시작 시점에 에러 발생!
Error: The dependencies of some of the beans in the application context form a cycle
→ 빠른 발견, 빠른 수정
```

**필드 주입 사용 시:**
```
애플리케이션 실행 중 특정 기능 사용 시에야 에러 발생
→ 늦은 발견, 프로덕션 환경에서 발견할 위험
```

---

#### 4.4.4 필수 의존성 명확성

```java
// ✅ 생성자 주입: 필수 의존성 명확
public class MemberService {
    private final MemberRepository repository;

    public MemberService(MemberRepository repository) {
        // repository 없이는 객체 생성 불가!
        this.repository = repository;
    }
}

// ❌ 세터 주입: 필수 여부 불명확
public class MemberService {
    private MemberRepository repository;

    public void setRepository(MemberRepository repository) {
        this.repository = repository;
    }

    public void join(Member member) {
        repository.save(member);  // repository가 null이면? NPE!
    }
}
```

---

### 4.5 Lombok을 활용한 생성자 주입 간소화

생성자 주입의 유일한 단점은 **코드가 장황**하다는 것입니다. Lombok을 사용하면 해결됩니다!

**Before: 수동 생성자 작성**
```java
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
```

**After: Lombok 사용**
```java
@Service
@RequiredArgsConstructor  // final 필드를 파라미터로 하는 생성자 자동 생성
public class MemberService {
    private final MemberRepository memberRepository;
    // 생성자가 자동으로 만들어짐!
}
```

**@RequiredArgsConstructor가 생성하는 코드:**
```java
// Lombok이 컴파일 시점에 자동 생성
public MemberService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
}
```

**💡 실무 팁:**
```java
// 의존성이 여러 개여도 간단!
@Service
@RequiredArgsConstructor
public class OrderService {
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    // 3개의 파라미터를 가진 생성자가 자동 생성됨!
}
```

**build.gradle에 Lombok 추가:**
```gradle
dependencies {
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

---

### 4.6 @Autowired의 동작 원리

`@Autowired`는 어떻게 동작할까요?

```
[1단계] 스프링이 MemberController 빈 생성 시도
   new MemberController(???)
   ↓
[2단계] 생성자에 @Autowired 발견
   public MemberController(MemberService memberService)
   ↓
[3단계] MemberService 타입의 빈을 컨테이너에서 검색
   applicationContext.getBean(MemberService.class)
   ↓
[4단계] 찾은 빈을 생성자 파라미터로 주입
   new MemberController(memberService 빈)
   ↓
[5단계] 의존관계 주입 완료
   memberController.memberService = memberService 빈
```

---

### 4.7 @Autowired 타입 매칭

`@Autowired`는 **타입으로 빈을 찾습니다**.

```java
@Autowired
public MemberController(MemberService memberService) {
    // MemberService 타입의 빈을 찾아서 주입
}
```

**만약 같은 타입의 빈이 2개 이상이면?**

```java
@Repository
public class MemoryMemberRepository implements MemberRepository { }

@Repository
public class JpaMemberRepository implements MemberRepository { }

// MemberRepository 타입의 빈이 2개!
// → 어떤 걸 주입?
```

**해결 방법 1: @Primary 사용**
```java
@Repository
@Primary  // 우선순위 지정
public class MemoryMemberRepository implements MemberRepository { }

@Repository
public class JpaMemberRepository implements MemberRepository { }
```

**해결 방법 2: @Qualifier 사용**
```java
@Repository
@Qualifier("memoryRepo")
public class MemoryMemberRepository implements MemberRepository { }

@Service
public class MemberService {
    @Autowired
    public MemberService(@Qualifier("memoryRepo") MemberRepository repository) {
        // memoryRepo 빈을 명시적으로 지정
    }
}
```

**해결 방법 3: 파라미터 이름으로 매칭**
```java
@Autowired
public MemberService(MemberRepository memoryMemberRepository) {
    // 파라미터 이름이 빈 이름과 일치하면 해당 빈 주입
}
```

**💡 실무 팁**: 대부분의 경우 같은 타입의 빈이 2개 이상 있는 상황은 드뭅니다. 인터페이스 하나에 구현체 하나로 설계하는 것이 일반적입니다.

---

### 4.8 요약: 의존관계 자동 주입

**핵심 정리:**
- **@Autowired**: 스프링이 의존관계를 자동으로 주입
- **생성자 주입**: 가장 권장되는 방법 (final, 불변, 테스트 용이)
- **필드 주입**: 비권장 (테스트 어려움, final 불가)
- **세터 주입**: 선택적 의존성에만 사용 (거의 안 씀)
- **Lombok**: @RequiredArgsConstructor로 생성자 주입 간소화

**Best Practice:**
```java
@Service
@RequiredArgsConstructor  // Lombok
public class MemberService {
    private final MemberRepository memberRepository;  // final 필수!
    // 생성자 자동 생성, @Autowired 자동 적용
}
```

---

## 5. 싱글톤 패턴과 스프링

스프링 컨테이너는 빈을 **싱글톤(Singleton)**으로 관리합니다. 이게 무슨 뜻일까요?

### 5.1 싱글톤 패턴이란?

**싱글톤 패턴**은 클래스의 인스턴스가 **딱 1개만 생성**되도록 보장하는 디자인 패턴입니다.

```java
// 전통적인 싱글톤 패턴
public class MemberService {
    // 1. static 영역에 객체를 딱 1개만 생성
    private static final MemberService instance = new MemberService();

    // 2. private 생성자로 외부에서 new 못하게 막음
    private MemberService() {
    }

    // 3. 인스턴스가 필요하면 이 메서드로만 조회
    public static MemberService getInstance() {
        return instance;
    }
}

// 사용
MemberService service1 = MemberService.getInstance();
MemberService service2 = MemberService.getInstance();
System.out.println(service1 == service2);  // true (같은 인스턴스)
```

**싱글톤의 장점:**
- 메모리 효율: 인스턴스가 1개만 생성됨
- 공유: 여러 곳에서 같은 인스턴스를 재사용

---

### 5.2 전통적 싱글톤 패턴의 문제점

하지만 위 방식의 싱글톤 패턴은 **많은 문제**가 있습니다.

#### 문제점 1: private 생성자 (상속 불가)

```java
public class MemberService {
    private MemberService() { }  // private 생성자
}

// ❌ 상속 불가
public class ExtendedMemberService extends MemberService {
    // 에러! 부모 클래스의 생성자를 호출할 수 없음
}
```

---

#### 문제점 2: static 필드 (유연성 저하)

```java
public class MemberService {
    private static final MemberService instance = new MemberService();
    // 컴파일 시점에 구현체가 고정됨
    // → 런타임에 다른 구현으로 교체 불가
}
```

---

#### 문제점 3: 테스트 어려움

```java
// ❌ Mock으로 교체 불가
@Test
void 테스트() {
    MemberService service = MemberService.getInstance();
    // 항상 같은 인스턴스
    // → Mock으로 교체할 수 없음
}
```

---

#### 문제점 4: DIP/OCP 위반

```java
// ❌ 구체 클래스에 의존
public class MemberController {
    private MemberService service = MemberService.getInstance();
    // MemberService라는 구체 클래스에 의존
    // → DIP 위반 (추상화에 의존해야 함)
}
```

---

### 5.3 스프링의 싱글톤 컨테이너

스프링은 위 문제들을 모두 해결합니다!

```java
// ✅ 스프링 방식: 일반 클래스처럼 작성
@Service
public class MemberService {
    // private 생성자 불필요
    // static 필드 불필요
    // getInstance() 메서드 불필요

    // 그냥 평범한 클래스!
}
```

**스프링의 마법:**
- 클래스는 평범하게 작성
- 스프링이 알아서 싱글톤으로 관리

---

### 5.4 스프링 빈이 싱글톤인지 확인

```java
@SpringBootTest
class SingletonTest {

    @Autowired
    ApplicationContext ac;

    @Test
    void 스프링빈_싱글톤_확인() {
        MemberService service1 = ac.getBean(MemberService.class);
        MemberService service2 = ac.getBean(MemberService.class);

        System.out.println("service1 = " + service1);
        System.out.println("service2 = " + service2);

        assertThat(service1).isSameAs(service2);  // 통과!
    }
}
```

**출력:**
```
service1 = hello.hello_spring.service.MemberService@5e025e70
service2 = hello.hello_spring.service.MemberService@5e025e70
```

→ 메모리 주소가 같음! 같은 인스턴스입니다.

---

### 5.5 싱글톤 방식의 주의점: Stateful vs Stateless

싱글톤 빈은 **여러 클라이언트가 공유**하므로, **무상태(Stateless)**로 설계해야 합니다.

#### ❌ 안티패턴: Stateful (상태 유지)

```java
@Service
public class MemberService {
    private Member currentMember;  // 공유 필드! 위험!

    public void login(Member member) {
        this.currentMember = member;  // 상태 저장
    }

    public Member getCurrentMember() {
        return currentMember;
    }
}
```

**문제 상황:**
```
시간 t1: 사용자A가 로그인
   memberService.login(memberA);
   currentMember = memberA

시간 t2: 사용자B가 로그인 (같은 인스턴스 사용!)
   memberService.login(memberB);
   currentMember = memberB  // memberA를 덮어씀!

시간 t3: 사용자A가 getCurrentMember() 호출
   return currentMember;  // memberB가 반환됨! (버그!)
```

---

#### ✅ 올바른 방식: Stateless (무상태)

```java
@Service
public class MemberService {
    // 공유 필드 없음!

    public Long join(Member member) {
        // 지역 변수만 사용
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
        // 지역 변수만 사용
        memberRepository.findByName(member.getName())
            .ifPresent(m -> {
                throw new IllegalStateException("이미 존재하는 회원입니다.");
            });
    }
}
```

**Stateless 설계 원칙:**
- 공유 필드(인스턴스 변수) 사용 금지
- 지역 변수, 파라미터, ThreadLocal 사용
- 읽기 전용 필드는 괜찮음 (final 필드, 상수 등)

---

### 5.6 싱글톤 빈의 필드 사용 규칙

```java
@Service
public class MemberService {
    // ✅ 괜찮음: 의존성 주입 (final)
    private final MemberRepository memberRepository;

    // ✅ 괜찮음: 상수
    private static final int MAX_RETRY = 3;

    // ✅ 괜찮음: 읽기 전용 설정값
    private final String appName;

    // ❌ 위험: 가변 상태
    private int count;  // 여러 스레드가 동시에 접근하면 문제!

    // ❌ 위험: 사용자별 상태
    private Member currentMember;  // 멀티스레드 환경에서 버그 발생!
}
```

**💡 실무 팁: 상태가 필요하면 어떻게 하나?**

사용자별 상태가 필요하면 다음 방법을 사용합니다:

1. **메서드 파라미터/지역 변수 사용**
   ```java
   public void process(Member member) {
       // member는 지역 변수
       // 각 요청마다 독립적
   }
   ```

2. **ThreadLocal 사용** (고급)
   ```java
   private ThreadLocal<Member> currentMember = new ThreadLocal<>();
   // 스레드별로 독립적인 저장공간
   ```

3. **세션 사용** (웹 애플리케이션)
   ```java
   @SessionAttribute("member")
   public Member getMember() {
       // HTTP 세션에 저장
   }
   ```

---

### 5.7 스프링 싱글톤의 장점

```
[전통적 방식: 매번 new]
요청1 → new MemberService() (인스턴스1, 메모리 0x1001)
요청2 → new MemberService() (인스턴스2, 메모리 0x1002)
요청3 → new MemberService() (인스턴스3, 메모리 0x1003)
→ 트래픽이 많으면 메모리 낭비, GC 부담

[스프링 싱글톤 방식]
요청1 ─┐
요청2 ─┼→ Spring Container → MemberService (단일 인스턴스, 메모리 0x1001)
요청3 ─┘
→ 메모리 효율, 성능 향상
```

**장점 정리:**
1. **메모리 효율**: 인스턴스가 1개만 생성되므로 메모리 절약
2. **성능**: 인스턴스 재사용으로 생성 비용 감소
3. **관리 용이**: 단일 인스턴스만 관리하면 됨
4. **일관성**: 같은 인스턴스를 공유하므로 일관된 동작

---

### 5.8 요약: 싱글톤 패턴과 스프링

**핵심 정리:**
- **싱글톤**: 인스턴스가 1개만 생성되는 패턴
- **전통적 싱글톤 문제점**: private 생성자, static 필드, 테스트 어려움, DIP 위반
- **스프링 싱글톤**: 일반 클래스로 작성해도 스프링이 싱글톤으로 관리
- **주의사항**: 무상태(Stateless) 설계 필수 (공유 필드 사용 금지)

**Best Practice:**
```java
@Service
public class MemberService {
    // ✅ final 의존성 주입
    private final MemberRepository memberRepository;

    // ✅ 지역 변수만 사용
    public Long join(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    // ❌ 공유 필드 사용 금지
    // private Member currentMember;  // 절대 이렇게 하지 말 것!
}
```

---

## 6. 자바 코드로 직접 빈 등록

지금까지는 `@Component` 계열 어노테이션으로 자동 빈 등록을 했습니다. 하지만 때로는 **수동으로 빈을 등록**해야 할 때가 있습니다.

### 6.1 @Configuration과 @Bean

**@Configuration**과 **@Bean**을 사용하면 자바 코드로 직접 빈을 등록할 수 있습니다.

#### SpringConfig.java 전체 코드와 핵심 설명

```java
package hello.hello_spring;

import hello.hello_spring.repository.MemberRepository;
import hello.hello_spring.repository.MemoryMemberRepository;
import hello.hello_spring.service.MemberService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfig {

    @Bean
    public MemberService memberService() {
        return new MemberService(memberRepository());
    }

    @Bean
    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
        // 나중에 이 한 줄만 변경!
        // return new JpaMemberRepository();
    }
}
```

**동작 원리:**
1. `@Configuration`이 붙은 클래스는 스프링 설정 클래스로 인식
2. `@Bean`이 붙은 메서드의 반환 객체가 스프링 빈으로 등록
3. 메서드 이름이 빈 이름이 됨 (예: `memberService()` → `memberService` 빈)

---

#### 왜 이 방식을 사용하는가?

**배경 시나리오** (SpringConfig.java 주석에서):

> "우리는 현재 멤버 리포지토리를 설계할 때, 아직 데이터 저장소가 선정되지 않았다는 가상의 시나리오가 존재한다."
> "그래서 지금 인터페이스를 설계를 하고 구현체로 MemoryMemberRepository를 쓰는 그림이 된 것이다."
> "우리는 나중에 MemoryMemberRepository를 다른 리포지토리로 바꿔치기를 해야 한다!"

**실무에서 흔한 상황:**
- 프로젝트 초기: DB 미정 → 메모리 DB 사용
- 개발 중반: MySQL 도입 → JdbcMemberRepository로 교체
- 나중: JPA 도입 → JpaMemberRepository로 교체

---

#### 핵심 장점: "기존 코드를 하나도 손대지 않고 바꿔치기!"

SpringConfig.java 주석에서:

> "그런데 기존의 운영 중인 코드를 하나도 손대지 않고 바꿔치기할 수 있는 방법이 있다!"
> "기존의 MemberService나 나머지 코드에 일절 손대는 거 없이 딱 바꿔치기할 수 있다!"
> "이것이 직접 설정 파일을 운영할 때 장점!"
> "컴포넌트 스캔을 사용하면 여러 코드를 바꿔야 한다."

**구체적 예시:**

```java
// Phase 1: 개발 초기 (현재 상태)
@Configuration
public class SpringConfig {
    @Bean
    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
        // 메모리에만 저장 (테스트용)
    }
}

// Phase 2: MySQL 도입 (이 한 줄만 수정!)
@Configuration
public class SpringConfig {
    @Bean
    public MemberRepository memberRepository() {
        // return new MemoryMemberRepository();  // 주석 처리
        return new JdbcMemberRepository(dataSource);
        // 실제 DB에 저장
    }
}

// Phase 3: JPA 도입 (이 한 줄만 수정!)
@Configuration
public class SpringConfig {
    @Bean
    public MemberRepository memberRepository() {
        // return new MemoryMemberRepository();
        // return new JdbcMemberRepository(dataSource);
        return new JpaMemberRepository(entityManager);
        // JPA로 저장
    }
}

// ✅ 중요: MemberService, MemberController는 단 한 줄도 수정하지 않음!
```

**시각화: 수정 범위 비교**

```
[수동 빈 등록 방식 - SpringConfig 사용]
수정 필요한 파일:
  ✅ SpringConfig.java: 1줄만 수정
  ✅ MemberService.java: 수정 없음
  ✅ MemberController.java: 수정 없음
  ✅ Test 코드: 수정 없음

총 수정: 1개 파일, 1줄


[컴포넌트 스캔 방식]
수정 필요한 파일:
  ❌ MemoryMemberRepository.java: @Repository 제거
  ❌ JpaMemberRepository.java: @Repository 추가
  ❌ 여러 파일 수정 필요
  ❌ Git conflict 가능성 증가

총 수정: 2개 이상의 파일
```

---

#### OCP (개방-폐쇄 원칙) 준수

SpringConfig.java 주석에서:
> "MemoryMemberRepository를 데이터베이스에 실제 연결하는 리포지토리로 바꿀거다!"
> "나중에 데이터베이스 연결을 하게 되면 return new MemoryMemberRepository();를 DbMemberRepository()로 바꿔주기만 하면 된다."
> "다른 코드를 전혀 손 댈 필요가 없다."

**OCP (Open-Closed Principle):**
- **개방**: 새로운 기능 추가에 열려있음 (새 Repository 추가 가능)
- **폐쇄**: 기존 코드 수정에 닫혀있음 (Service, Controller 수정 불필요)

```java
// 인터페이스는 그대로
public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findById(Long id);
    Optional<Member> findByName(String name);
    List<Member> findAll();
}

// 구현체만 교체
@Configuration
public class SpringConfig {
    @Bean
    public MemberRepository memberRepository() {
        // 이 한 줄만 바꾸면 전체 시스템의 Repository가 바뀜!
        return new MemoryMemberRepository();     // or
        // return new JdbcMemberRepository();    // or
        // return new JpaMemberRepository();     // or
        // return new MongoMemberRepository();   // 언제든 추가 가능!
    }
}
```

---

#### 실무 활용 예시

**1. 환경별로 다른 구현체 사용**

```java
@Configuration
public class SpringConfig {

    @Value("${spring.profiles.active}")
    private String profile;

    @Bean
    public MemberRepository memberRepository() {
        if ("dev".equals(profile)) {
            return new MemoryMemberRepository();  // 개발: 메모리
        } else if ("test".equals(profile)) {
            return new H2MemberRepository();      // 테스트: H2
        } else {
            return new JpaMemberRepository();     // 운영: JPA
        }
        // 환경만 바꾸면 자동으로 Repository 교체!
    }
}
```

**2. 기능 플래그(Feature Flag) 활용**

```java
@Configuration
public class SpringConfig {

    @Value("${feature.use-jpa}")
    private boolean useJpa;

    @Bean
    public MemberRepository memberRepository() {
        if (useJpa) {
            return new JpaMemberRepository();
        } else {
            return new MemoryMemberRepository();
        }
        // application.yml에서 feature.use-jpa: true/false만 바꾸면 됨!
    }
}
```

**3. A/B 테스트**

```java
@Configuration
public class SpringConfig {

    @Bean
    public MemberRepository memberRepository() {
        // 트래픽의 50%는 새로운 구현체 사용
        if (Math.random() < 0.5) {
            return new NewJpaMemberRepository();  // 새 버전
        } else {
            return new OldJpaMemberRepository();  // 기존 버전
        }
    }
}
```

---

#### 다이어그램: 의존관계 흐름

```
[SpringConfig가 의존관계를 조립]

SpringConfig
    │
    ├─ @Bean memberService()
    │    └─ new MemberService(memberRepository())
    │             ↑
    │             │ 주입
    │             │
    └─ @Bean memberRepository()
         └─ return new MemoryMemberRepository()
              (또는 JpaMemberRepository)
              ↑
              │ 이 한 줄만 바꾸면 됨!


[MemberService는 변경 없음]

@Service (제거됨 - SpringConfig에서 관리)
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
        // 어떤 구현체가 들어오든 상관없음!
        // 인터페이스만 의존
    }
}
```

---

#### 핵심 정리

SpringConfig 방식의 장점:
1. **구현체 교체가 쉬움**: 한 줄만 수정
2. **기존 코드 보호**: Service, Controller 수정 불필요
3. **OCP 준수**: 확장에는 열려있고, 수정에는 닫혀있음
4. **명시적 설정**: 어떤 구현체를 쓰는지 한눈에 파악
5. **Git Conflict 최소화**: 한 파일만 수정하므로 충돌 적음

---

### 6.2 수동 등록 시 어노테이션 제거

수동으로 빈을 등록하면, **해당 클래스의 @Service, @Repository 어노테이션을 제거**해야 합니다.

```java
// ❌ 어노테이션 제거 전
@Service
public class MemberService { }

@Repository
public class MemoryMemberRepository implements MemberRepository { }

// ✅ 어노테이션 제거 후
public class MemberService { }

public class MemoryMemberRepository implements MemberRepository { }
```

**왜 제거해야 하는가?**
- 자동 등록과 수동 등록이 동시에 일어나면 중복 등록
- 스프링 부트는 수동 등록이 우선권을 가지지만, 혼란 방지를 위해 제거

**⚠️ 주의**: `@Controller`는 그대로 두어야 합니다. Controller는 웹 요청을 받기 위해 컴포넌트 스캔이 필요합니다.

---

### 6.3 컴포넌트 스캔 vs 수동 빈 등록

| 구분 | 컴포넌트 스캔 | 수동 빈 등록 |
|------|-------------|------------|
| **방법** | @Component, @Service, @Repository | @Configuration + @Bean |
| **편리성** | ✅ 자동 (어노테이션만 붙이면 됨) | △ 수동 (설정 클래스 작성 필요) |
| **유연성** | △ 낮음 (구현체 교체 시 코드 수정) | ✅ 높음 (설정만 변경) |
| **가독성** | ✅ 클래스만 보면 됨 | △ 설정 클래스도 봐야 함 |
| **사용 사례** | 일반적인 업무 로직 빈 | 기술 지원 빈, 다형성 활용 |

---

#### 구현체 교체 시나리오 상세 비교

SpringConfig.java 주석에서:
> "컴포넌트 스캔을 사용하면 여러 코드를 바꿔야 한다."

**시나리오**: MemoryMemberRepository에서 JpaMemberRepository로 교체

**방법 1: 컴포넌트 스캔 방식의 문제**

```java
// 1단계: MemoryMemberRepository.java 수정
@Repository  // ← 이 어노테이션을 제거해야 함!
public class MemoryMemberRepository implements MemberRepository {
    // ...
}

// 2단계: JpaMemberRepository.java 수정
@Repository  // ← 이 어노테이션을 추가해야 함!
public class JpaMemberRepository implements MemberRepository {
    // ...
}

// 문제점:
// - 2개의 파일을 수정해야 함
// - @Repository를 잘못 제거/추가하면 빈이 중복 등록되거나 등록 안 될 수 있음
// - Git에서 여러 파일 수정으로 conflict 가능성 증가
// - 실수 가능성 높음
```

**방법 2: 수동 빈 등록 방식의 장점**

```java
// SpringConfig.java만 수정 (단 1줄!)
@Configuration
public class SpringConfig {

    @Bean
    public MemberRepository memberRepository() {
        // return new MemoryMemberRepository();  // 이전
        return new JpaMemberRepository();         // 이후
        // 이 한 줄만 바꾸면 끝!
    }

    @Bean
    public MemberService memberService() {
        return new MemberService(memberRepository());
        // 이 코드는 전혀 수정하지 않음!
    }
}

// 장점:
// - 1개의 파일만 수정
// - 1줄만 변경
// - 다른 팀원의 코드와 conflict 없음
// - 변경 이력 추적 용이
// - 실수 가능성 낮음
```

---

#### 실무 사례: 점진적 DB 전환

**상황**: 레거시 시스템을 JDBC에서 JPA로 전환하는 프로젝트

**컴포넌트 스캔 방식:**

```java
// 문제: 한 번에 모든 Repository를 바꿔야 함
@Repository
public class MemberJdbcRepository { }  // 기존

@Repository
public class MemberJpaRepository { }   // 신규

// ❌ 둘 다 @Repository가 있으면 충돌!
// → 한 번에 전환해야 함 (위험!)
```

**수동 빈 등록 방식:**

```java
@Configuration
public class SpringConfig {

    @Value("${feature.use-jpa}")
    private boolean useJpa;

    @Bean
    public MemberRepository memberRepository() {
        if (useJpa) {
            return new JpaMemberRepository();    // 점진적으로 전환
        } else {
            return new JdbcMemberRepository();   // 기존 방식 유지
        }
    }
}

// ✅ 설정만 바꾸면 즉시 롤백 가능!
// application.yml:
//   feature.use-jpa: false  ← 문제 발생 시 즉시 롤백
```

---

#### 팀 협업 관점

**컴포넌트 스캔 방식:**

```
팀원 A: MemoryMemberRepository.java 수정
팀원 B: JpaMemberRepository.java 수정
팀원 C: MemberService.java 수정 (혹시 모를 의존성 수정)

Git Merge:
  ❌ Conflict 발생 가능성 높음
  ❌ 여러 파일 Review 필요
  ❌ 통합 테스트 필수
```

**수동 빈 등록 방식:**

```
팀원 A: SpringConfig.java만 수정 (1줄)

Git Merge:
  ✅ Conflict 가능성 낮음
  ✅ 1개 파일만 Review
  ✅ 변경 범위 명확
```

---

#### 시각화: 변경 전파 범위

```
[컴포넌트 스캔 방식 - 변경 범위가 넓음]

MemoryMemberRepository.java
  ↓ @Repository 제거
  ❌ 수정 필요

JpaMemberRepository.java
  ↓ @Repository 추가
  ❌ 수정 필요

(선택적으로)
MemberService.java
  ↓ 혹시 모를 수정
  ❌ 확인 필요


[수동 빈 등록 방식 - 변경 범위가 좁음]

SpringConfig.java
  ↓ 1줄만 수정
  ✅ 수정 완료

MemoryMemberRepository.java
  ✅ 수정 없음

JpaMemberRepository.java
  ✅ 수정 없음

MemberService.java
  ✅ 수정 없음
```

---

#### 롤백(Rollback) 용이성

**컴포넌트 스캔:**

```bash
# JPA로 전환 후 문제 발생!
# 롤백 방법:
git revert <commit-hash>
# → 여러 파일 변경사항 롤백
# → 혹시 중간에 다른 커밋이 있었다면? 복잡!
```

**수동 빈 등록:**

```bash
# JPA로 전환 후 문제 발생!
# 롤백 방법 1: Git
git revert <commit-hash>
# → 1개 파일만 롤백 (간단!)

# 롤백 방법 2: 설정 변경
# application.yml에서 feature.use-jpa: false
# → 코드 변경 없이 즉시 롤백!

# 롤백 방법 3: 코드에서 1줄만 수정
return new MemoryMemberRepository();  // 이전 버전으로
```

---

### 6.4 언제 수동 빈 등록을 사용하는가?

#### 사용 사례 1: 기술 지원 빈

**기술 지원 빈**은 데이터베이스 연결, AOP, 로깅 등 **기술적인 문제를 처리하는 빈**입니다.

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setUsername("root");
        dataSource.setPassword("password");
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }
}
```

**왜 수동 등록?**
- 설정이 복잡하고 중요함
- 애플리케이션 전반에 영향을 미침
- 명시적으로 설정을 확인하고 싶음

---

#### 사용 사례 2: 다형성을 활용한 빈

여러 구현체 중 하나를 선택해야 할 때 수동 등록이 유용합니다.

```java
@Configuration
public class SpringConfig {

    @Bean
    public MemberRepository memberRepository() {
        // 환경에 따라 다른 구현체 반환
        if (useMemory) {
            return new MemoryMemberRepository();
        } else if (useJpa) {
            return new JpaMemberRepository();
        } else {
            return new JdbcMemberRepository();
        }
    }
}
```

**장점:**
- 구현체를 쉽게 교체 가능
- Service 코드 수정 불필요
- 설정 파일만 보면 어떤 구현체를 사용하는지 명확

---

#### 사용 사례 3: 프로파일별 빈 등록

개발/운영 환경에 따라 다른 빈을 등록할 수 있습니다.

```java
@Configuration
public class AppConfig {

    @Bean
    @Profile("dev")
    public MemberRepository devRepository() {
        return new MemoryMemberRepository();
    }

    @Bean
    @Profile("prod")
    public MemberRepository prodRepository() {
        return new JpaMemberRepository();
    }
}
```

---

### 6.5 비즈니스 로직 빈은 컴포넌트 스캔 권장

**비즈니스 로직 빈**(Controller, Service, Repository)은 **컴포넌트 스캔 사용을 권장**합니다.

```java
// ✅ 권장: 컴포넌트 스캔
@Controller
public class MemberController { }

@Service
public class MemberService { }

@Repository
public class MemoryMemberRepository { }
```

**왜 컴포넌트 스캔을 권장하는가?**
- 비즈니스 로직 빈은 개수가 많음
- 한눈에 파악하기 쉬움 (클래스만 보면 됨)
- 유지보수가 편리함

**❌ 비권장: 수동 등록**
```java
@Configuration
public class SpringConfig {
    @Bean public MemberController memberController() { }
    @Bean public OrderController orderController() { }
    @Bean public ProductController productController() { }
    @Bean public MemberService memberService() { }
    @Bean public OrderService orderService() { }
    @Bean public ProductService productService() { }
    // ... 수십 개의 @Bean 메서드
    // → 관리가 어려움!
}
```

---

### 6.6 실전 예제: Repository 구현체 교체

```java
@Configuration
public class SpringConfig {

    @Bean
    public MemberRepository memberRepository() {
        // return new MemoryMemberRepository();  // 메모리 사용
        return new JpaMemberRepository();  // JPA로 교체 (이 줄만 변경!)
    }

    @Bean
    public MemberService memberService() {
        return new MemberService(memberRepository());
        // Service 코드는 전혀 수정하지 않음!
    }
}
```

**OCP (개방-폐쇄 원칙) 준수:**
- 기능 확장에는 열려있음 (새로운 Repository 추가 가능)
- 코드 수정에는 닫혀있음 (Service 코드는 수정 안 함)

---

### 6.7 스프링 컨테이너 관리와 @Autowired의 관계

이 섹션에서는 **@Autowired가 언제 동작하고 언제 동작하지 않는지**를 이해합니다.

#### @Autowired는 언제 동작하는가?

MemberService.java의 주석에서:
> "이거 만약에 @Autowired를 했는데 SpringConfig에서 관리를 안한다면?"
> "→ 당연히 안먹는다."
> "→ 멤버 서비스가 스프링에 등록이 되고 스프링 관리를 해야 스프링이 @Autowired도 적용할 수 있다."

**핵심 원리:**

@Autowired가 동작하려면 **두 가지 조건**이 필요합니다:

1. **조건 1: 클래스 자체가 스프링 빈으로 등록되어야 함**
   - 컴포넌트 스캔(@Service, @Controller, @Repository) 또는
   - 수동 등록(@Bean)

2. **조건 2: 주입 대상도 스프링 빈이어야 함**

---

#### 코드 예시: @Autowired가 동작하는 경우

```java
// ✅ 정상 동작: SpringConfig에서 MemberService를 관리
@Configuration
public class SpringConfig {
    @Bean
    public MemberService memberService() {
        return new MemberService(memberRepository());
        // 스프링이 MemberService를 빈으로 등록
    }

    @Bean
    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
    }
}

public class MemberService {
    private final MemberRepository memberRepository;

    @Autowired  // ✅ 동작함! (MemberService가 스프링 빈이므로)
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
```

**왜 동작하는가?**
- MemberService가 SpringConfig의 @Bean으로 등록됨
- → 스프링이 관리하는 빈
- → @Autowired 동작!

---

#### 코드 예시: @Autowired가 동작하지 않는 경우

MemberService.java의 주석에서:
> "MemberService memberService = new MemberService();"
> "new해서 직접 MemberService를 생성하는 경우에도, @Autowired가 동작하지 않는다."
> "→ 내가 직접 생성한 거니까!"
> "→ 스프링 컨테이너에 올라가는 것들만 이 @Autowired 기능이 동작을 한다."

```java
// ❌ @Autowired 동작 안 함: new로 직접 생성
public class SomeController {
    public void someMethod() {
        MemberService memberService = new MemberService();
        // → 내가 직접 new로 생성
        // → 스프링이 관리하지 않음
        // → @Autowired 무시됨!
        // → memberRepository가 null이 됨!

        memberService.join(member);  // NullPointerException 발생!
    }
}
```

**왜 동작하지 않는가?**
- `new MemberService()`로 직접 생성
- → 스프링 컨테이너가 관리하지 않음
- → @Autowired를 처리할 주체가 없음
- → 의존성 주입 안 됨!

---

#### 실무 함정: 스프링 빈이 아닌 클래스에서 @Autowired 사용

```java
// ❌ 흔한 실수: 일반 클래스에서 @Autowired 사용
public class MyUtil {  // @Component가 없음!
    @Autowired
    private MemberService memberService;  // 동작 안 함!

    public void doSomething() {
        memberService.join(member);  // NullPointerException!
    }
}

// 해결 방법 1: @Component 추가
@Component  // 스프링 빈으로 등록!
public class MyUtil {
    @Autowired
    private MemberService memberService;  // 이제 동작함!
}

// 해결 방법 2: 생성자로 직접 주입
public class MyUtil {
    private final MemberService memberService;

    public MyUtil(MemberService memberService) {
        this.memberService = memberService;  // 직접 주입
    }
}
```

---

#### 시각화: 스프링 컨테이너 관리 여부

```
[스프링 컨테이너]
┌──────────────────────────────────────┐
│                                      │
│  MemberService (빈)                  │ ← @Autowired 동작 ✅
│    @Autowired로 주입됨                │
│    ↓                                 │
│  MemberRepository (빈)               │
│                                      │
└──────────────────────────────────────┘
         ↑
         │ 스프링이 관리
         │


[컨테이너 밖 - 개발자가 직접 생성]
┌──────────────────────────────────────┐
│                                      │
│  new MemberService()                 │ ← @Autowired 무시 ❌
│    (개발자가 직접 생성)                │
│    → 스프링이 관리하지 않음             │
│    → @Autowired 동작하지 않음          │
│    → memberRepository가 null!        │
│                                      │
└──────────────────────────────────────┘
```

---

#### 실전 예시: Test 코드

```java
// ❌ @Autowired가 동작하지 않는 테스트
@Test
void 테스트_실패() {
    MemberService memberService = new MemberService();
    // new로 직접 생성 → 스프링 관리 안 함
    // → @Autowired 동작 안 함
    // → memberRepository가 null

    Member member = new Member();
    memberService.join(member);  // NullPointerException!
}

// ✅ 올바른 방법 1: 생성자로 직접 주입
@Test
void 테스트_성공_1() {
    MemberRepository repository = new MemoryMemberRepository();
    MemberService memberService = new MemberService(repository);
    // 생성자로 직접 주입 → @Autowired 불필요

    Member member = new Member();
    memberService.join(member);  // 정상 동작!
}

// ✅ 올바른 방법 2: 스프링 컨테이너 사용
@SpringBootTest
class MemberServiceTest {
    @Autowired
    MemberService memberService;  // 스프링이 주입
    // → 스프링 컨테이너가 관리하므로 @Autowired 동작!

    @Test
    void 테스트_성공_2() {
        Member member = new Member();
        memberService.join(member);  // 정상 동작!
    }
}
```

---

#### 핵심 정리

**@Autowired 동작 조건:**

| 조건 | 설명 | 예시 |
|------|------|------|
| **빈 등록** | 클래스가 스프링 빈으로 등록되어야 함 | @Service, @Bean 사용 |
| **컨테이너 관리** | 스프링 컨테이너가 생성한 객체여야 함 | new로 직접 생성 ❌ |
| **주입 대상** | 주입할 객체도 스프링 빈이어야 함 | memberRepository도 빈 |

**자주 하는 실수:**

```java
// ❌ 실수 1: @Component 없이 @Autowired 사용
public class MyClass {  // @Component 없음!
    @Autowired
    private MemberService service;  // 동작 안 함!
}

// ❌ 실수 2: new로 생성한 객체에 @Autowired 기대
MemberService service = new MemberService();
// service 내부의 @Autowired가 동작할 거라 기대 → 안 됨!

// ❌ 실수 3: 스프링 빈이 아닌 대상을 주입
@Service
public class MyService {
    @Autowired
    private MyUtil myUtil;  // MyUtil이 빈이 아니면 에러!
}
```

**올바른 사용:**

```java
// ✅ 올바른 방법 1: 컴포넌트 스캔
@Service
public class MemberService {
    @Autowired
    public MemberService(MemberRepository repository) {
        // 동작함! (MemberService가 빈이므로)
    }
}

// ✅ 올바른 방법 2: 수동 빈 등록
@Configuration
public class SpringConfig {
    @Bean
    public MemberService memberService() {
        return new MemberService(memberRepository());
        // 동작함! (SpringConfig에서 관리하므로)
    }
}

// ✅ 올바른 방법 3: 순수 자바 (테스트용)
@Test
void 테스트() {
    MemberRepository repository = new MemoryMemberRepository();
    MemberService service = new MemberService(repository);
    // @Autowired 없이 생성자로 직접 주입
}
```

**💡 실무 팁:**

MemberService.java 주석의 핵심:
> "스프링 컨테이너에 올라가는 것들만 이 @Autowired 기능이 동작을 한다."

즉:
- 스프링이 관리하는 빈: @Autowired 동작 ✅
- 내가 new로 만든 객체: @Autowired 무시 ❌

**💡 참고**: 의존성 주입의 3가지 방법에 대한 자세한 내용은 [4.2 의존성 주입(DI)의 3가지 방법](#42-의존성-주입di의-3가지-방법)을 참조하세요.

---

### 6.8 요약: 수동 빈 등록

**핵심 정리:**
- **@Configuration + @Bean**: 자바 코드로 직접 빈 등록
- **컴포넌트 스캔**: 자동, 편리, 비즈니스 로직에 사용
- **수동 빈 등록**: 유연, 기술 지원 빈이나 다형성 활용 시 사용

**사용 지침:**
```
비즈니스 로직 빈 (Controller, Service, Repository)
  → 컴포넌트 스캔 사용 (@Component, @Service, @Repository)

기술 지원 빈 (DataSource, AOP, 로깅 등)
  → 수동 빈 등록 사용 (@Configuration + @Bean)

다형성을 활용하는 빈
  → 수동 빈 등록 사용 (구현체 교체 용이)
```

---

## 7. 실전 예제 - MemberController 구현

이제 배운 내용을 종합하여 실제 코드를 살펴봅시다.

### 7.1 전체 흐름 다이어그램

스프링은 빈을 등록하는 방법이 두 가지가 있습니다. 두 방식의 흐름을 비교해봅시다.

---

#### 방식 1: 컴포넌트 스캔 방식 (이전)

```
[Spring Boot 애플리케이션 시작 흐름 - 컴포넌트 스캔]

1. main() 메서드 실행
   SpringApplication.run(HelloSpringApplication.class, args);
   ↓
2. @SpringBootApplication 감지
   @ComponentScan 포함됨
   ↓
3. 컴포넌트 스캔 시작
   패키지 탐색: hello.hello_spring.*
   ↓
4. @Component 계열 어노테이션 검색
   ├─ @Controller 발견 → MemberController 빈 등록 시도
   ├─ @Service 발견 → MemberService 빈 등록
   └─ @Repository 발견 → MemoryMemberRepository 빈 등록
   ↓
5. MemberController 빈 생성
   생성자에 @Autowired 있음
   ↓
6. MemberService 타입 빈 검색
   memberService 빈 발견
   ↓
7. MemberController 생성자에 주입
   new MemberController(memberService)
   ↓
8. 마찬가지로 MemberService 생성 시
   MemberRepository 주입
   ↓
9. 의존관계 주입 완료
   memberController → memberService → memberRepository
   ↓
10. 애플리케이션 준비 완료
    웹 요청 대기
```

---

#### 방식 2: 수동 빈 등록 방식 (현재 - SpringConfig 사용)

```
[Spring Boot 애플리케이션 시작 흐름 - SpringConfig 방식]

1. main() 메서드 실행
   SpringApplication.run(HelloSpringApplication.class, args);
   ↓
2. @SpringBootApplication 감지
   @ComponentScan 실행 (동시에 @Configuration도 스캔)
   ↓
3. SpringConfig 클래스 발견
   @Configuration 처리
   ↓
4. @Bean 메서드 실행 (수동 등록)
   │
   ├─ memberRepository() 호출
   │  └─ new MemoryMemberRepository() 생성
   │  └─ 스프링 컨테이너에 빈 등록
   │     (빈 이름: "memberRepository")
   │
   └─ memberService() 호출
      └─ memberRepository() 메서드 호출
      └─ new MemberService(memberRepository) 생성
      └─ 스프링 컨테이너에 빈 등록
         (빈 이름: "memberService")
   ↓
5. 컴포넌트 스캔으로 @Controller 발견
   MemberController 빈 등록 시도
   (Service, Repository는 이미 수동 등록됨)
   ↓
6. MemberController 생성자 확인
   @Autowired 발견
   MemberService 타입 빈 검색
   ↓
7. 의존관계 주입
   memberController ← memberService (스프링이 주입)
   memberService ← memberRepository (이미 생성자로 주입됨)
   ↓
8. 의존관계 주입 완료
   memberController → memberService → memberRepository
   ↓
9. 애플리케이션 준비 완료
   웹 요청 대기
```

---

#### 두 방식의 핵심 차이점

| 단계 | 컴포넌트 스캔 | 수동 등록 (SpringConfig) |
|------|-------------|------------------------|
| **빈 발견** | @ComponentScan이 @Service, @Repository 찾음 | @Configuration의 @Bean 메서드 실행 |
| **빈 생성** | 스프링이 자동으로 new | 개발자가 명시적으로 new |
| **의존관계 주입** | @Autowired로 자동 | 생성자 파라미터로 명시 |
| **빈 등록 순서** | 스프링이 결정 (순서 보장 안 됨) | 개발자가 명시 (명확함) |
| **구현체 교체** | 여러 파일 수정 필요 | SpringConfig 1줄만 수정 |

---

#### 상세 비교: 빈 생성 과정

**컴포넌트 스캔 방식:**

```java
// 스프링이 자동으로 처리
@Service
public class MemberService {
    @Autowired
    public MemberService(MemberRepository repository) {
        // 스프링이 알아서 repository 주입
    }
}

// 내부 동작 (의사 코드)
MemberService memberService = new MemberService(
    applicationContext.getBean(MemberRepository.class)
);
// 스프링이 자동으로 빈을 찾아서 주입
```

**수동 빈 등록 방식:**

```java
// 개발자가 명시적으로 처리
@Configuration
public class SpringConfig {
    @Bean
    public MemberService memberService() {
        return new MemberService(memberRepository());
        // 개발자가 직접 의존관계를 명시
    }

    @Bean
    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
        // 어떤 구현체를 쓸지 명확하게 지정
    }
}

// 개발자가 명시한 대로 동작
MemberService memberService = new MemberService(
    memberRepository()  // 개발자가 지정한 메서드 호출
);
```

---

#### 시각화: 의존관계 조립 흐름

```
[컴포넌트 스캔 - 스프링이 자동으로 조립]

┌──────────────────────────────────────┐
│ 스프링 컨테이너                        │
│                                       │
│ 1. @Service 발견 → MemberService 생성  │
│    @Autowired 확인                    │
│    ↓                                  │
│ 2. MemberRepository 타입 빈 검색       │
│    (어떤 구현체인지 스프링이 결정)       │
│    ↓                                  │
│ 3. 자동 주입                           │
│    memberService ← 찾은 빈            │
│                                       │
└──────────────────────────────────────┘


[수동 빈 등록 - 개발자가 명시적으로 조립]

┌──────────────────────────────────────┐
│ SpringConfig                          │
│                                       │
│ 1. memberRepository() 호출            │
│    return new MemoryMemberRepository();
│    (개발자가 명시: 메모리 구현체 사용)  │
│    ↓                                  │
│ 2. memberService() 호출               │
│    return new MemberService(          │
│        memberRepository()             │
│    );                                 │
│    (개발자가 명시: repository 주입)    │
│    ↓                                  │
│ 3. 스프링 컨테이너에 등록              │
│    (개발자가 조립한 대로)               │
│                                       │
└──────────────────────────────────────┘
```

---

#### 실행 로그 비교

**컴포넌트 스캔 방식 로그:**

```
Creating shared instance of singleton bean 'memberService'
Creating shared instance of singleton bean 'memoryMemberRepository'
Autowiring by type from bean name 'memberService' to bean named 'memoryMemberRepository'
```
→ 스프링이 자동으로 타입을 찾아서 주입 ("Autowiring by type")

**수동 빈 등록 방식 로그:**

```
Creating shared instance of singleton bean 'springConfig'
Creating shared instance of singleton bean 'memberRepository'
Creating shared instance of singleton bean 'memberService'
```
→ SpringConfig의 @Bean 메서드 순서대로 생성
→ 의존관계가 이미 생성자로 명시되어 있으므로 "Autowiring by type" 로그 없음

---

#### 어떤 방식을 선택해야 하는가?

**컴포넌트 스캔 권장:**
- ✅ 일반적인 비즈니스 로직 (Controller, Service, Repository)
- ✅ 구현체가 거의 바뀌지 않는 경우
- ✅ 팀원들이 코드만 보고 빠르게 이해해야 하는 경우

**수동 빈 등록 권장:**
- ✅ 기술 지원 빈 (DataSource, AOP, 트랜잭션 매니저 등)
- ✅ 구현체를 자주 교체해야 하는 경우 (예: Repository 구현체)
- ✅ 환경별로 다른 빈을 사용해야 하는 경우
- ✅ 설정이 복잡하고 명시적으로 관리하고 싶은 경우

**💡 실무 팁:**

우리 프로젝트처럼 Repository 구현체를 자주 바꿔야 한다면:
```java
@Configuration
public class SpringConfig {
    @Bean
    public MemberRepository memberRepository() {
        // return new MemoryMemberRepository();  // 개발
        // return new JdbcMemberRepository();    // JDBC
        return new JpaMemberRepository();        // JPA
        // 이 한 줄만 바꾸면 전체 시스템이 바뀜!
    }
}
```

이것이 SpringConfig를 사용하는 이유입니다!

**💡 참고**: SpringConfig를 사용하는 이유와 장점에 대한 자세한 내용은 [6.1 @Configuration과 @Bean](#61-configuration과-bean)을 참조하세요.

---

### 7.2 Before/After 코드 비교

#### Before: new 연산자 사용

```java
// ❌ 문제 있는 코드
package hello.hello_spring.controller;

import hello.hello_spring.service.MemberService;

public class MemberController {
    private final MemberService memberService = new MemberService();
    // 문제점:
    // 1. 매번 새로운 인스턴스 생성
    // 2. 스프링 컨테이너가 관리하지 않음
    // 3. 싱글톤 아님
}

// ❌ 문제 있는 코드
package hello.hello_spring.service;

import hello.hello_spring.repository.MemberRepository;
import hello.hello_spring.repository.MemoryMemberRepository;

public class MemberService {
    private final MemberRepository memberRepository = new MemoryMemberRepository();
    // 문제점:
    // 1. 구체 클래스에 의존
    // 2. Repository 교체 어려움
}
```

---

#### After: 스프링 컨테이너 사용

**MemberController.java:**
```java
package hello.hello_spring.controller;

import hello.hello_spring.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class MemberController {

    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
        System.out.println("memberService = " + memberService.getClass());
    }
}
```

**MemberService.java:**
```java
package hello.hello_spring.service;

import hello.hello_spring.domain.Member;
import hello.hello_spring.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * 회원 가입
     */
    public Long join(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
        memberRepository.findByName(member.getName())
            .ifPresent(m -> {
                throw new IllegalStateException("이미 존재하는 회원입니다.");
            });
    }

    /**
     * 전체 회원 조회
     */
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public Optional<Member> findOne(Long memberId) {
        return memberRepository.findById(memberId);
    }
}
```

**MemoryMemberRepository.java:**
```java
package hello.hello_spring.repository;

import hello.hello_spring.domain.Member;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MemoryMemberRepository implements MemberRepository {

    private static Map<Long, Member> store = new HashMap<>();
    private static long sequence = 0L;

    @Override
    public Member save(Member member) {
        member.setId(++sequence);
        store.put(member.getId(), member);
        return member;
    }

    @Override
    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Member> findByName(String name) {
        return store.values().stream()
            .filter(member -> member.getName().equals(name))
            .findAny();
    }

    @Override
    public List<Member> findAll() {
        return new ArrayList<>(store.values());
    }

    public void clearStore() {
        store.clear();
    }
}
```

---

### 7.3 스프링 부트 실행 로그 분석

애플리케이션 실행 시 로그를 보면 스프링이 어떻게 동작하는지 알 수 있습니다.

```
Creating shared instance of singleton bean 'memberController'
Creating shared instance of singleton bean 'memberService'
Creating shared instance of singleton bean 'memoryMemberRepository'

Autowiring by type from bean name 'memberController' to bean named 'memberService'
Autowiring by type from bean name 'memberService' to bean named 'memoryMemberRepository'

memberService = class hello.hello_spring.service.MemberService
```

**로그 분석:**

1. **Creating shared instance of singleton bean 'memberController'**
   - MemberController 빈을 싱글톤으로 생성

2. **Creating shared instance of singleton bean 'memberService'**
   - MemberService 빈을 싱글톤으로 생성

3. **Creating shared instance of singleton bean 'memoryMemberRepository'**
   - MemoryMemberRepository 빈을 싱글톤으로 생성

4. **Autowiring by type from bean name 'memberController' to bean named 'memberService'**
   - memberController에 memberService를 타입으로 찾아서 주입

5. **Autowiring by type from bean name 'memberService' to bean named 'memoryMemberRepository'**
   - memberService에 memoryMemberRepository를 타입으로 찾아서 주입

---

### 7.4 빈 등록 확인 테스트

스프링 컨테이너에 빈이 제대로 등록되었는지 확인할 수 있습니다.

```java
package hello.hello_spring;

import hello.hello_spring.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class BeanRegistrationTest {

    @Autowired
    ApplicationContext ac;

    @Test
    void 빈_등록_확인() {
        // 1. 모든 빈 이름 출력
        String[] beanNames = ac.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = ac.getBean(beanName);
            System.out.println("name = " + beanName + ", object = " + bean);
        }

        // 2. 특정 빈 조회
        MemberService memberService = ac.getBean("memberService", MemberService.class);
        assertThat(memberService).isNotNull();

        // 3. 싱글톤 확인
        MemberService service1 = ac.getBean(MemberService.class);
        MemberService service2 = ac.getBean(MemberService.class);
        assertThat(service1).isSameAs(service2);

        System.out.println("✅ 모든 빈이 정상적으로 등록되었습니다!");
    }
}
```

**출력 예시:**
```
name = memberController, object = hello.hello_spring.controller.MemberController@5e025e70
name = memberService, object = hello.hello_spring.service.MemberService@2f7c7260
name = memoryMemberRepository, object = hello.hello_spring.repository.MemoryMemberRepository@3b94d659
✅ 모든 빈이 정상적으로 등록되었습니다!
```

---

### 7.5 의존관계 주입 흐름 시각화

```
[의존관계 주입 완료 상태]

┌─────────────────────────────────────────────┐
│         Spring Container                    │
│                                              │
│  ┌───────────────────┐                      │
│  │ memberController  │                      │
│  │  (Bean)           │                      │
│  └─────────┬─────────┘                      │
│            │ memberService 주입               │
│            ↓                                 │
│  ┌───────────────────┐                      │
│  │  memberService    │                      │
│  │  (Bean, 싱글톤)    │                      │
│  └─────────┬─────────┘                      │
│            │ memberRepository 주입            │
│            ↓                                 │
│  ┌──────────────────────────┐               │
│  │  memoryMemberRepository  │               │
│  │  (Bean, 싱글톤)           │               │
│  └──────────────────────────┘               │
│                                              │
└─────────────────────────────────────────────┘

웹 요청 처리 흐름:
브라우저 → MemberController (빈)
         → MemberService (빈, 싱글톤)
         → MemoryMemberRepository (빈, 싱글톤)
```

---

### 7.6 요약: 실전 예제

**전체 흐름:**
1. `@SpringBootApplication` 실행 → 컴포넌트 스캔 시작
2. `@Controller`, `@Service`, `@Repository` 발견 → 빈 등록
3. `@Autowired` 발견 → 의존관계 주입
4. 싱글톤으로 관리 → 재사용

**코드 개선 효과:**
- ✅ 인스턴스 중복 생성 문제 해결 (싱글톤)
- ✅ 강한 결합 문제 해결 (인터페이스 의존)
- ✅ 테스트 어려움 해결 (생성자 주입)

---

## 8. Best Practice 및 안티패턴

실무에서 지켜야 할 좋은 관행과 피해야 할 패턴을 정리합니다.

### 8.1 Best Practice (좋은 관행)

#### ✅ 1. 생성자 주입 + final 사용

```java
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    @Autowired  // 생성자가 1개면 생략 가능
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
```

**왜?**
- 불변성 보장 (final)
- 테스트 용이
- 필수 의존성 명확

---

#### ✅ 2. Lombok @RequiredArgsConstructor 활용

```java
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    // 생성자가 자동으로 생성됨!
}
```

**왜?**
- 코드 간결
- 유지보수 편리
- 실수 방지

---

#### ✅ 3. 인터페이스 기반 설계

```java
@Service
public class MemberService {
    private final MemberRepository memberRepository;  // 인터페이스에 의존
    // private final MemoryMemberRepository memberRepository;  // ❌ 구체 클래스
}
```

**왜?**
- 느슨한 결합
- 구현체 교체 용이
- DIP 준수

---

#### ✅ 4. 계층별 어노테이션 명확히 사용

```java
@Controller  // Presentation 계층
public class MemberController { }

@Service  // Business 계층
public class MemberService { }

@Repository  // Persistence 계층
public class MemoryMemberRepository { }
```

**왜?**
- 역할 명확
- 가독성 향상
- AOP 적용 편리

---

#### ✅ 5. 무상태(Stateless) 설계

```java
@Service
public class MemberService {
    // ✅ final 의존성만 필드로
    private final MemberRepository memberRepository;

    // ✅ 지역 변수 사용
    public Long join(Member member) {
        validateDuplicateMember(member);  // 지역 변수
        memberRepository.save(member);
        return member.getId();
    }
}
```

**왜?**
- 멀티스레드 안전
- 싱글톤 빈에 적합
- 버그 방지

---

### 8.2 안티패턴 (피해야 할 패턴)

#### ❌ 1. 필드 주입 사용

```java
// ❌ 비권장
@Service
public class MemberService {
    @Autowired
    private MemberRepository memberRepository;  // final 불가, 테스트 어려움
}

// ✅ 권장
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
```

---

#### ❌ 2. 순환 참조

```java
// ❌ 순환 참조
@Service
public class AService {
    private final BService bService;

    public AService(BService bService) {
        this.bService = bService;
    }
}

@Service
public class BService {
    private final AService aService;

    public BService(AService aService) {
        this.aService = aService;  // A ↔ B 순환!
    }
}
```

**해결 방법:**
- 설계 변경: 공통 로직을 별도 서비스로 분리
- 이벤트 기반: 직접 호출 대신 이벤트 발행

---

#### ❌ 3. 빈 스캔 범위 밖에 클래스 배치

```java
// ❌ 스캔 안 됨
package hello.other_package;  // @SpringBootApplication 패키지 밖

@Service
public class OtherService {
    // 컴포넌트 스캔 대상 아님!
}
```

**해결 방법:**
- `@SpringBootApplication`이 있는 패키지 하위에 배치

---

#### ❌ 4. Stateful 싱글톤 빈

```java
// ❌ 안티패턴
@Service
public class MemberService {
    private Member currentMember;  // 공유 필드! 멀티스레드 위험!

    public void login(Member member) {
        this.currentMember = member;  // 다른 스레드가 덮어씀!
    }
}

// ✅ 올바른 방식
@Service
public class MemberService {
    // 공유 필드 없음

    public Member login(String name, String password) {
        // 지역 변수만 사용
        Member member = memberRepository.findByName(name);
        return member;
    }
}
```

---

#### ❌ 5. new 연산자로 빈 생성

```java
// ❌ 스프링 빈이 아님
@Controller
public class MemberController {
    private final MemberService memberService = new MemberService();
    // 스프링 컨테이너가 관리하지 않음!
}

// ✅ 스프링 빈 주입
@Controller
public class MemberController {
    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }
}
```

---

### 8.3 리팩토링 체크리스트

프로젝트를 시작하거나 코드 리뷰 시 다음을 확인하세요:

- [ ] **생성자 주입을 사용하는가?**
- [ ] **필드에 final을 붙였는가?**
- [ ] **인터페이스에 의존하는가?**
- [ ] **공유 필드(인스턴스 변수)가 없는가?**
- [ ] **@Component 계열 어노테이션이 스캔 범위 안에 있는가?**
- [ ] **순환 참조가 없는가?**
- [ ] **Lombok @RequiredArgsConstructor를 활용하는가?**
- [ ] **계층별 어노테이션(@Controller, @Service, @Repository)을 명확히 사용하는가?**

---

### 8.4 💡 실무 팁 모음

#### 팁 1: Lombok 활용

```java
// Before: 장황한 코드
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    @Autowired
    public MemberService(MemberRepository memberRepository,
                        EmailService emailService,
                        SmsService smsService) {
        this.memberRepository = memberRepository;
        this.emailService = emailService;
        this.smsService = smsService;
    }
}

// After: Lombok 사용
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final EmailService emailService;
    private final SmsService smsService;
}
```

---

#### 팁 2: 테스트에서 Mock 주입

```java
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    MemberService memberService;

    @Test
    void 회원가입_성공() {
        // given
        Member member = new Member();
        member.setName("spring");

        given(memberRepository.findByName(anyString()))
            .willReturn(Optional.empty());

        given(memberRepository.save(any(Member.class)))
            .willReturn(member);

        // when
        Long saveId = memberService.join(member);

        // then
        assertThat(saveId).isEqualTo(member.getId());
        verify(memberRepository).save(member);
    }
}
```

---

#### 팁 3: 프로파일별 빈 설정

```java
@Configuration
public class AppConfig {

    @Bean
    @Profile("dev")
    public MemberRepository devRepository() {
        return new MemoryMemberRepository();
    }

    @Bean
    @Profile("prod")
    public MemberRepository prodRepository() {
        return new JpaMemberRepository();
    }
}
```

**application.yml:**
```yaml
spring:
  profiles:
    active: dev  # 개발 환경
```

---

#### 팁 4: 환경별 Repository 전환

```java
@Configuration
public class SpringConfig {

    @Value("${repository.type:memory}")
    private String repositoryType;

    @Bean
    public MemberRepository memberRepository() {
        if ("jpa".equals(repositoryType)) {
            return new JpaMemberRepository();
        } else if ("jdbc".equals(repositoryType)) {
            return new JdbcMemberRepository();
        } else {
            return new MemoryMemberRepository();
        }
    }
}
```

**application.yml:**
```yaml
repository:
  type: memory  # memory, jpa, jdbc 중 선택
```

---

### 8.5 요약: Best Practice

**코드 작성 원칙:**
1. **생성자 주입 + final**: 불변성, 테스트 용이성
2. **Lombok 활용**: 코드 간결화
3. **인터페이스 의존**: 느슨한 결합
4. **무상태 설계**: 싱글톤 안전
5. **명확한 계층 분리**: @Controller, @Service, @Repository

**피해야 할 패턴:**
1. **필드 주입**: 테스트 어려움, final 불가
2. **순환 참조**: 설계 문제, 에러 발생
3. **Stateful 싱글톤**: 멀티스레드 버그
4. **new 연산자**: 스프링 컨테이너 관리 X

---

## 9. 부록

### 9.1 프로젝트 구조

```
hello-spring/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── hello/hello_spring/
│   │   │       ├── HelloSpringApplication.java   // @SpringBootApplication
│   │   │       ├── controller/
│   │   │       │   └── MemberController.java     // @Controller
│   │   │       ├── service/
│   │   │       │   └── MemberService.java        // @Service
│   │   │       ├── repository/
│   │   │       │   ├── MemberRepository.java     // 인터페이스
│   │   │       │   └── MemoryMemberRepository.java  // @Repository
│   │   │       └── domain/
│   │   │           └── Member.java               // 도메인 객체
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
│           └── hello/hello_spring/
│               └── service/
│                   └── MemberServiceTest.java
└── build.gradle
```

---

### 9.2 핵심 용어 정리

| 용어 | 영문 | 설명 |
|------|------|------|
| **IoC** | Inversion of Control | 제어의 역전. 개발자가 아닌 프레임워크가 객체 생성/관리를 담당 |
| **DI** | Dependency Injection | 의존성 주입. 외부에서 의존 객체를 주입하는 패턴 |
| **Bean** | Spring Bean | 스프링 컨테이너가 관리하는 자바 객체 |
| **Container** | Spring Container | 빈을 생성하고 관리하는 컨테이너 (ApplicationContext) |
| **Component Scan** | - | @Component 어노테이션을 찾아 자동으로 빈 등록하는 기능 |
| **Autowired** | - | 스프링이 의존관계를 자동으로 주입하는 어노테이션 |
| **Singleton** | - | 인스턴스가 단 하나만 존재하는 디자인 패턴 |
| **Stateless** | - | 상태를 갖지 않는 설계 (공유 필드 없음) |
| **Stateful** | - | 상태를 갖는 설계 (공유 필드 있음, 싱글톤에서는 위험) |

---

### 9.3 스프링 어노테이션 정리

| 어노테이션 | 위치 | 역할 | 비고 |
|-----------|------|------|------|
| **@SpringBootApplication** | Main Class | 자동 설정, 컴포넌트 스캔 활성화 | @ComponentScan 포함 |
| **@Controller** | Class | 웹 컨트롤러 빈 등록 | Presentation 계층 |
| **@Service** | Class | 서비스 빈 등록 | Business 계층 |
| **@Repository** | Class | 레포지토리 빈 등록 | Persistence 계층, 예외 변환 |
| **@Component** | Class | 일반 빈 등록 | 위 3개의 부모 어노테이션 |
| **@Autowired** | Constructor/Field/Setter | 의존관계 자동 주입 | 생성자가 1개면 생략 가능 |
| **@Configuration** | Class | 수동 빈 등록 설정 클래스 | @Bean과 함께 사용 |
| **@Bean** | Method | 수동 빈 등록 | @Configuration 내부에서 사용 |
| **@RequiredArgsConstructor** | Class | final 필드 생성자 자동 생성 (Lombok) | 생성자 주입 간소화 |
| **@Primary** | Class | 같은 타입 빈이 여러 개일 때 우선순위 지정 | - |
| **@Qualifier** | Parameter | 같은 타입 빈이 여러 개일 때 명시적 지정 | - |

---

### 9.4 참고 자료

**강의:**
- 인프런 - 김영한의 "스프링 입문 - 코드로 배우는 스프링 부트, 웹 MVC, DB 접근 기술"
- 인프런 - 김영한의 "스프링 핵심 원리 - 기본편"

**공식 문서:**
- Spring Framework Documentation: https://docs.spring.io/spring-framework/docs/current/reference/html/
- Spring Boot Reference Documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/

**추천 도서:**
- "토비의 스프링 3.1" - 이일민 저
- "스프링 부트와 AWS로 혼자 구현하는 웹 서비스" - 이동욱 저

---

### 9.5 다음 학습 단계

이 문서를 마스터했다면 다음 주제로 넘어가세요:

1. **회원 관리 예제 - 웹 MVC 개발**
   - 회원 등록 폼
   - 회원 목록 조회
   - Thymeleaf 템플릿 엔진

2. **스프링 DB 접근 기술**
   - JDBC Template
   - JPA
   - Spring Data JPA

3. **AOP (Aspect-Oriented Programming)**
   - 시간 측정 AOP
   - 트랜잭션 AOP
   - 로깅 AOP

4. **스프링 핵심 원리 - 고급편**
   - 빈 생명주기 콜백
   - 빈 스코프 (Singleton, Prototype, Request, Session)
   - 프록시 패턴

---

### 9.6 학습 점검

이 문서를 충분히 이해했다면 다음 질문에 답할 수 있어야 합니다:

1. **IoC와 DI의 차이는 무엇인가?**
   - IoC: 제어의 역전 (누가 객체를 관리하는가)
   - DI: 의존성 주입 (어떻게 의존관계를 설정하는가)

2. **왜 생성자 주입을 권장하는가?**
   - final 사용 가능 (불변성)
   - 테스트 용이
   - 순환 참조 조기 발견
   - 필수 의존성 명확

3. **컴포넌트 스캔의 기본 범위는?**
   - @SpringBootApplication이 있는 패키지와 하위 패키지

4. **스프링 빈은 기본적으로 어떤 스코프인가?**
   - Singleton (단일 인스턴스)

5. **싱글톤 빈 설계 시 주의사항은?**
   - Stateless (무상태) 설계
   - 공유 필드 사용 금지
   - 지역 변수만 사용

6. **@Controller, @Service, @Repository의 차이는?**
   - 역할이 다름 (Presentation, Business, Persistence)
   - @Repository는 예외 변환 기능 추가

---

## 🎓 마치며

**이 문서에서 배운 핵심:**

1. **new 연산자의 문제점**: 중복 생성, 강한 결합, 테스트 어려움
2. **Spring Container**: IoC/DI를 통한 객체 관리 자동화
3. **컴포넌트 스캔**: @Component 계열 어노테이션으로 자동 빈 등록
4. **의존관계 자동 주입**: @Autowired, 생성자 주입 권장
5. **싱글톤 패턴**: 스프링이 빈을 싱글톤으로 관리, Stateless 설계 필수

**실무에서 가장 중요한 것:**

```java
@Service
@RequiredArgsConstructor  // Lombok으로 생성자 자동 생성
public class MemberService {
    private final MemberRepository memberRepository;  // final 필수!

    // 무상태 설계: 공유 필드 없음, 지역 변수만 사용
    public Long join(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }
}
```

**이 패턴만 기억하세요:**
- ✅ 생성자 주입 + final
- ✅ Lombok @RequiredArgsConstructor
- ✅ 인터페이스 의존
- ✅ 무상태 설계

이제 스프링의 핵심인 IoC/DI를 마스터했습니다! 🎉

다음 단계에서는 이를 바탕으로 실제 웹 애플리케이션을 만들어봅시다.

---

**문서 작성일**: 2026-02-17
**작성자**: Spring Study Group
**버전**: 1.0
