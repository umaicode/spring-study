# AOP

> 공통 관심 사항과 핵심 관심 사항의 분리 - 시간 측정 로직으로 배우는 AOP

## 📚 강의 출처

**인프런 - 김영한의 "스프링 입문 - 코드로 배우는 스프링 부트, 웹 MVC, DB 접근 기술"**

이 문서는 강의의 "AOP" 섹션을 학습하며 작성한 노트입니다. 모든 메서드에 시간 측정 코드를 직접 심는 방식의 문제점을 체감한 뒤, AOP로 공통 관심 사항을 핵심 관심 사항에서 깔끔하게 분리하는 방법을 배웁니다. 스프링이 내부적으로 프록시(Proxy)를 통해 AOP를 구현하는 원리까지 이해하는 것이 목표입니다.

---

## 🎯 학습 목표

이 문서를 통해 다음을 이해하고 실습합니다:

1. **AOP가 필요한 상황 - 문제 인식**
   - 모든 메서드에 시간 측정 코드를 직접 추가할 때 발생하는 5가지 문제점
   - 공통 관심 사항(Cross-cutting Concern)과 핵심 관심 사항(Core Concern)의 차이

2. **AOP 핵심 개념 및 용어**
   - Aspect, JoinPoint, Advice, Pointcut, Weaving 개념
   - `execution` 표현식 패턴 작성법

3. **TimeTraceAop 구현 - @Aspect, @Around**
   - `@Aspect`로 Aspect 클래스 선언
   - `@Around`로 메서드 실행 전/후 코드 주입
   - `ProceedingJoinPoint`와 `proceed()` 동작 방식

4. **스프링 AOP 동작 원리 - 프록시(Proxy)**
   - AOP 적용 전/후 의존관계 변화
   - 스프링이 프록시 객체를 생성하여 AOP를 구현하는 원리
   - `MemberController` 생성자 출력으로 프록시 확인

5. **스프링 빈 등록 방식 비교 - @Component vs @Bean**
   - `@Component` 자동 스캔 방식과 `SpringConfig`의 `@Bean` 명시적 등록 방식 비교
   - AOP 클래스에서 `@Bean` 등록을 선호하는 이유

---

## 🗺️ 학습 로드맵

이 문서는 **문제 인식 → 개념 이해 → 구현 → 동작 원리** 순으로 구성되어 있습니다.

```
[AOP 학습 흐름]

1. 문제 인식
   모든 메서드에 시간 측정 코드 직접 추가
   → 핵심 로직과 공통 코드가 뒤엉킨다 (유지보수 불가)
          ↓
2. 개념 이해
   AOP = 공통 관심 사항을 별도 Aspect로 분리
          ↓
3. 구현
   TimeTraceAop (@Aspect + @Around)
          ↓
4. 동작 원리
   스프링 → 프록시 객체 자동 생성
   memberController → 프록시 MemberService → 실제 MemberService
```

**왜 이 순서인가?**
- **문제 먼저 체감**: 나쁜 코드(직접 추가)를 먼저 보고 불편함을 느낀 뒤 AOP를 배웁니다.
- **원리까지 이해**: 단순히 어노테이션 사용법만이 아니라, 프록시 기반 동작 원리를 이해합니다.

---

## 📖 목차

1. [AOP가 필요한 상황](#1-aop가-필요한-상황)
2. [AOP 핵심 개념](#2-aop-핵심-개념)
3. [AOP 적용 - TimeTraceAop 구현](#3-aop-적용---timetraceaop-구현)
4. [스프링 AOP 동작 방식 - 프록시](#4-스프링-aop-동작-방식---프록시)
5. [스프링 빈 등록 - @Component vs @Bean](#5-스프링-빈-등록---component-vs-bean)
6. [Best Practice 및 주의사항](#6-best-practice-및-주의사항)
7. [부록](#7-부록)

---

## 1. AOP가 필요한 상황

### 1.1 문제: 모든 메서드에 시간 측정 코드 직접 추가

회원 서비스의 각 메서드 실행 시간을 측정해야 하는 요구사항이 생겼다고 가정합니다.
AOP 없이 직접 추가하면 아래와 같은 코드가 됩니다.

```java
// ❌ 시간 측정 로직을 직접 추가한 MemberService (나쁜 예)
@Transactional
public class MemberService {

    public Long join(Member member) {
        long start = System.currentTimeMillis(); // ← 공통 코드

        try {
            validateDuplicateMember(member);     // 핵심 비즈니스 로직
            memberRepository.save(member);       // 핵심 비즈니스 로직
            return member.getId();
        } finally {
            long finish = System.currentTimeMillis();
            long timeMs = finish - start;
            System.out.println("join = " + timeMs + "ms"); // ← 공통 코드
        }
    }

    public List<Member> findMembers() {
        long start = System.currentTimeMillis(); // ← 공통 코드 (또 추가!)

        try {
            return memberRepository.findAll();   // 핵심 비즈니스 로직
        } finally {
            long finish = System.currentTimeMillis();
            long timeMs = finish - start;
            System.out.println("findMembers " + timeMs + "ms"); // ← 공통 코드
        }
    }

    // findOne()에도, 다른 메서드에도 동일하게 반복...
}
```

> 실제 `MemberService.java`를 보면 `join()`과 `findMembers()` 안에 시간 측정 코드가 주석 처리되어 남아 있습니다.
> 이것이 AOP 적용 전의 "나쁜 방법" 코드입니다.

### 1.2 5가지 문제점

| # | 문제 | 설명 |
|---|------|------|
| 1 | **핵심 관심사와 공통 관심사 혼재** | 회원 가입/조회 로직과 시간 측정 로직이 한 메서드에 뒤섞임 |
| 2 | **코드 중복** | 100개 메서드에 측정이 필요하면 100번 같은 코드를 복붙해야 함 |
| 3 | **변경 어려움** | 나노초 단위로 바꾸고 싶다면? 100개 메서드를 모두 수정해야 함 |
| 4 | **비즈니스 로직 가독성 저하** | 핵심 로직이 공통 코드에 묻혀 읽기 어려움 |
| 5 | **테스트 어려움** | 공통 코드가 섞여 있어 비즈니스 로직 단독 테스트가 복잡해짐 |

### 1.3 공통 관심 사항 vs 핵심 관심 사항

```
[관심사 분류]

핵심 관심 사항 (Core Concern)          공통 관심 사항 (Cross-cutting Concern)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━         ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
회원 가입 비즈니스 로직                 시간 측정
회원 조회 비즈니스 로직                 로깅 (Logging)
중복 회원 검증                          트랜잭션 처리
                                        보안 (Security)
                                        예외 처리
```

- **핵심 관심 사항**: 해당 클래스/메서드가 진짜 해야 할 일. 비즈니스 요구사항.
- **공통 관심 사항**: 여러 곳에 공통으로 적용되어야 하는 부가 기능. 직접 추가하면 핵심 로직을 오염시킴.

### 1.4 AOP 없는 스프링 컨테이너 - 문제 시각화

```
[AOP 없이 공통 코드 직접 추가 - 문제 상황]

┌──────────────── 스프링 컨테이너 ────────────────┐
│                                                │
│  MemberController                              │
│  ┌─────────────────────────────────────┐      │
│  │ 핵심 로직 + 시간측정 + 로깅 혼재    │      │
│  └─────────────────────────────────────┘      │
│             │                                  │
│  MemberService                                 │
│  ┌─────────────────────────────────────┐      │
│  │ join()     → 핵심로직 + 시간측정   │      │
│  │ findMembers() → 핵심로직 + 시간측정│      │
│  │ findOne()  → 핵심로직 + 시간측정   │      │
│  └─────────────────────────────────────┘      │
│             │                                  │
│  MemberRepository                              │
│  ┌─────────────────────────────────────┐      │
│  │ 핵심 로직 + 시간측정 + 로깅 혼재    │      │
│  └─────────────────────────────────────┘      │
│                                                │
└────────────────────────────────────────────────┘

→ 공통 관심사 코드가 모든 빈에 뒤엉켜 유지보수 불가!
```

---

## 2. AOP 핵심 개념

### 2.1 AOP란?

**AOP(Aspect-Oriented Programming, 관점 지향 프로그래밍)**

> 공통 관심 사항(Cross-cutting Concern)을 핵심 관심 사항(Core Concern)에서 **분리**하여, 원하는 곳에 **적용**하는 프로그래밍 기법

```
[AOP의 핵심 아이디어]

공통 관심 사항           핵심 관심 사항
(시간 측정)              (비즈니스 로직)
    │                         │
    │   AOP로 연결             │
    └──────────→  적용 위치 지정으로 결합
                  (Pointcut으로 어디에 적용할지 정의)
```

### 2.2 주요 용어

| 용어 | 설명 | 예시 |
|------|------|------|
| **Aspect** | 공통 관심 사항을 모아둔 모듈 | `TimeTraceAop` 클래스 |
| **JoinPoint** | Advice를 적용할 수 있는 위치 | 메서드 실행, 예외 발생 등 |
| **Advice** | 실제로 실행될 공통 기능 코드 | `execute()` 메서드 본문 |
| **Pointcut** | Advice를 어디에 적용할지 선별하는 표현식 | `execution(* hello.hello_spring..*(..))` |
| **Weaving** | Pointcut으로 선별된 JoinPoint에 Advice를 적용하는 과정 | 스프링 컨테이너가 프록시 생성 시 수행 |
| **Target** | Advice가 적용되는 실제 객체 | `MemberService` 실제 객체 |
| **Proxy** | AOP 적용을 위해 스프링이 생성한 가짜 객체 | `MemberService$$SpringCGLIB...` |

### 2.3 execution 표현식 문법

`@Around`의 Pointcut은 `execution` 표현식으로 작성합니다.

```
execution( [접근제어자] 반환타입 [선언타입].메서드명(파라미터) )
           ↑생략가능                              ↑필수
```

**기본 예시:**

```
execution(* hello.hello_spring..*(..))
           ↑  ↑                 ↑  ↑
           │  └─ 패키지         │  └─ 모든 파라미터
           │                    └─ 모든 메서드명
           └─ 모든 반환 타입
```

**패키지 패턴 - 중요한 차이점:**

| 패턴 | 의미 | 적용 범위 |
|------|------|----------|
| `* hello.hello_spring.*(..)` | `.` 하나 → 해당 패키지만 | `hello.hello_spring` 패키지의 클래스만 |
| `* hello.hello_spring..*(..)` | `..` 두 개 → 하위 패키지 포함 | `hello.hello_spring` + 모든 하위 패키지 |

```java
// 전체 패키지 + 하위 패키지 전부 적용 (현재 프로젝트 설정)
@Around("execution(* hello.hello_spring..*(..))")

// 서비스 패키지 하위에만 적용 (좁은 범위)
@Around("execution(* hello.hello_spring.service..*(..))")

// 특정 클래스만 적용
@Around("execution(* hello.hello_spring.service.MemberService.*(..))")
```

**다양한 패턴 예시:**

| 표현식 | 설명 |
|--------|------|
| `* *(..)` | 모든 메서드 |
| `* hello..*(..)` | `hello` 패키지 하위 전체 |
| `String hello..*(String)` | 반환 타입 String, 파라미터 String인 메서드 |
| `* hello..Member.*(..)` | `Member` 클래스의 모든 메서드 |

---

## 3. AOP 적용 - TimeTraceAop 구현

### 3.1 TimeTraceAop 전체 코드 분석

```java
package hello.hello_spring.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
// ① Aspect 클래스임을 선언. 이 클래스가 공통 관심 사항을 담은 모듈임을 스프링에게 알림
@Component
// ② 컴포넌트 스캔으로 스프링 빈 자동 등록
//    (강의에서는 @Bean 등록 방식을 더 선호한다고 언급)
public class TimeTraceAop {

    @Around("execution(* hello.hello_spring..*(..))")
    // ③ Around Advice: 아래 메서드를 어디에 적용할지 지정
    //    "hello.hello_spring 패키지 및 하위 패키지의 모든 클래스, 모든 메서드"에 적용
    public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {
        // ④ ProceedingJoinPoint: 현재 실행 중인 메서드(타겟)에 대한 정보를 담은 객체
        //    joinPoint.toString() → 어느 메서드가 호출됐는지 문자열로 확인 가능
        //    joinPoint.proceed() → 실제 타겟 메서드를 실행

        long start = System.currentTimeMillis();
        // ⑤ proceed() 이전 코드 → 메서드 실행 "전"에 실행됨

        System.out.println("START: " + joinPoint.toString());
        // ⑥ 어느 메서드가 시작됐는지 로그 출력
        //    예: "execution(Long hello.hello_spring.service.MemberService.join(Member))"

        try {
            return joinPoint.proceed();
            // ⑦ 핵심! 실제 타겟 메서드를 실행하고 결과를 반환
            //    이 라인이 없으면 실제 메서드가 실행되지 않음
        } finally {
            long finish = System.currentTimeMillis();
            long timeMs = finish - start;
            System.out.println("END: " + joinPoint.toString() + " " + timeMs + "ms");
            // ⑧ proceed() 이후(finally) 코드 → 메서드 실행 "후"에 실행됨
            //    예외가 발생해도 finally이므로 항상 실행됨
        }
    }
}
```

**execute() 실행 흐름:**

```
@Around가 감지: MemberService.join() 호출됨
        ↓
long start = System.currentTimeMillis()   [시작 시간 기록]
        ↓
System.out.println("START: ...")           [시작 로그]
        ↓
joinPoint.proceed()                        [실제 join() 메서드 실행]
        ↓
실제 join() 내부 로직 완료
        ↓
finally 블록 실행
        ↓
System.out.println("END: ... Xms")        [종료 로그 + 경과 시간]
```

### 3.2 MemberService 개선 전/후 비교

```java
// ❌ AOP 없이 직접 추가 (개선 전)
public Long join(Member member) {
    long start = System.currentTimeMillis();    // 공통 코드 혼재

    try {
        validateDuplicateMember(member);        // 핵심 로직
        memberRepository.save(member);          // 핵심 로직
        return member.getId();
    } finally {
        long finish = System.currentTimeMillis();
        long timeMs = finish - start;
        System.out.println("join = " + timeMs + "ms"); // 공통 코드 혼재
    }
}

public List<Member> findMembers() {
    long start = System.currentTimeMillis();    // 또 공통 코드 반복

    try {
        return memberRepository.findAll();      // 핵심 로직
    } finally {
        long finish = System.currentTimeMillis();
        long timeMs = finish - start;
        System.out.println("findMembers " + timeMs + "ms"); // 또 반복
    }
}
```

```java
// ✅ AOP 적용 후 (개선 후) - 실제 현재 MemberService.java
@Transactional
public class MemberService {

    public Long join(Member member) {
        validateDuplicateMember(member);   // 핵심 로직만 남음!
        memberRepository.save(member);
        return member.getId();
    }

    public List<Member> findMembers() {
        return memberRepository.findAll(); // 핵심 로직만 남음!
    }

    public Optional<Member> findOne(Long memberId) {
        return memberRepository.findById(memberId);
    }
}
// → 시간 측정 코드가 완전히 사라졌다.
// → TimeTraceAop가 자동으로 모든 메서드에 시간 측정을 적용해준다.
```

| 항목 | AOP 없이 직접 추가 | AOP 적용 |
|------|-------------------|----------|
| **코드 중복** | 메서드마다 반복 | `TimeTraceAop` 한 곳에만 |
| **비즈니스 로직 가독성** | 시간 측정 코드에 묻힘 | 핵심 로직만 명확히 보임 |
| **측정 로직 변경 시** | 모든 메서드 수정 필요 | `TimeTraceAop` 한 곳만 수정 |
| **적용 범위 변경 시** | 추가/삭제 작업 반복 | `@Around` 표현식 한 줄만 수정 |

---

## 4. 스프링 AOP 동작 방식 - 프록시

### 4.1 AOP 적용 전 의존관계

```
[AOP 적용 전]

memberController
      │
      │ 의존 (직접 참조)
      ↓
memberService (실제 객체)
      │
      │ 의존
      ↓
memberRepository
```

`MemberController`는 `MemberService` 실제 객체를 직접 참조합니다.

### 4.2 AOP 적용 후 의존관계 - 프록시 등장

```
[AOP 적용 후]

memberController
      │
      │ 의존 (프록시를 참조! - 진짜인 줄 알고 씀)
      ↓
프록시 memberService         ← 스프링이 자동 생성
(MemberService$$SpringCGLIB...)
      │
      │ 1. 시작 시간 측정, START 로그
      │ 2. joinPoint.proceed() 호출
      ↓
실제 memberService           ← 진짜 객체
      │
      │ 3. 실제 비즈니스 로직 실행 후 반환
      ↑
프록시 memberService
      │ 4. 종료 시간 측정, END 로그 출력
      │
      ↑
memberController (결과 반환)
```

**프록시 동작 순서:**

```
1. MemberController가 memberService.join() 호출
        ↓
2. 실제로는 프록시 객체가 받음 (MemberController는 모름)
        ↓
3. 프록시: TimeTraceAop.execute() 실행 (START 로그, 시작 시간 기록)
        ↓
4. 프록시: joinPoint.proceed() 호출 → 실제 MemberService.join() 위임
        ↓
5. 실제 MemberService.join() 실행 (비즈니스 로직)
        ↓
6. 실제 join() 결과를 프록시에 반환
        ↓
7. 프록시: finally 블록 실행 (END 로그, 경과 시간 출력)
        ↓
8. 프록시가 MemberController에 최종 결과 반환
```

### 4.3 AOP 적용 전후 전체 그림 비교

```
[AOP 적용 전 - 3-tier 구조]

HTTP 요청
    ↓
MemberController (실제)
    ↓
MemberService (실제)
    ↓
MemberRepository (실제)
    ↓
DB
```

```
[AOP 적용 후 - 프록시가 앞에 위치]

HTTP 요청
    ↓
MemberController (실제)
    ↓
프록시 MemberService ──→ TimeTraceAop (시작 측정)
    ↓                          │
실제 MemberService              │ proceed()
    ↓                   TimeTraceAop (종료 측정) ←─┘
MemberRepository (실제)
    ↓
DB
```

### 4.4 프록시 확인 코드

`MemberController.java`의 생성자에 아래 출력 코드가 있습니다:

```java
@Autowired
public MemberController(MemberService memberService) {
    this.memberService = memberService;

    // 프록시 확인 코드 - memberService가 진짜인지 프록시인지 출력
    System.out.println("memberService = " + memberService.getClass());
}
```

**AOP 적용 전 출력 결과:**

```
memberService = class hello.hello_spring.service.MemberService
```

**AOP 적용 후 출력 결과:**

```
memberService = class hello.hello_spring.service.MemberService$$SpringCGLIB$$...
```

> `$$SpringCGLIB$$` 접미어가 붙으면 스프링이 생성한 **프록시 객체**입니다.
> `MemberController`에 주입된 것은 실제 `MemberService`가 아니라 스프링이 자동으로 만든 가짜(프록시) 객체입니다.

**CGLIB(Code Generation Library):**
- 스프링은 바이트코드 조작 라이브러리인 **CGLIB**를 사용하여 프록시 클래스를 동적으로 생성합니다.
- 이 프록시는 원본 클래스를 상속받아 만들어지므로, 클라이언트(`MemberController`)는 진짜와 가짜를 구분할 수 없습니다.

---

## 5. 스프링 빈 등록 - @Component vs @Bean

### 5.1 @Component 방식 (현재 코드)

```java
@Aspect
@Component  // 컴포넌트 스캔으로 자동 등록
public class TimeTraceAop {
    // ...
}
```

- 클래스에 `@Component`를 붙이면 스프링이 컴포넌트 스캔 시 자동으로 빈 등록
- 간단하고 빠름

### 5.2 @Bean 방식 (SpringConfig - 선호 방식)

```java
// SpringConfig.java
@Configuration
public class SpringConfig {

    // ...

    // @Bean 방식으로 TimeTraceAop 명시적 등록 (코드에서 주석 처리된 상태)
    @Bean
    public TimeTraceAop timeTraceAop() {
        return new TimeTraceAop();
    }
}
```

> 실제 `SpringConfig.java`에 주석 처리된 형태로 존재합니다.

### 5.3 비교 테이블

| 항목 | `@Component` | `@Bean` (SpringConfig) |
|------|-------------|----------------------|
| **등록 방식** | 자동 (컴포넌트 스캔) | 명시적 (설정 파일에서 직접) |
| **코드 위치** | AOP 클래스 자체 | `SpringConfig` |
| **가독성** | 간결 | 어디서 빈이 등록되는지 한눈에 파악 가능 |
| **AOP에서 권장** | ⚠️ 사용 가능 | ✅ 선호 |
| **이유** | - | AOP는 특수한 동작(프록시 생성)을 하므로, 명시적으로 등록하는 것이 의도를 드러냄 |

**강의 코멘트 (TimeTraceAop.java 주석 그대로):**

```java
// @Component 하게 되면 컴포넌트 스캔이 된다.
// 그러나 스프링 빈에 등록해서 쓰는 것을 더 선호한다.
```

---

## 6. Best Practice 및 주의사항

### 6.1 Pointcut 표현식은 너무 넓게 잡지 말 것

**✅ 권장: 서비스 레이어에만 적용**

```java
// 서비스 패키지 하위만 적용 (일반적으로 충분)
@Around("execution(* hello.hello_spring.service..*(..))")
```

**⚠️ 주의: 너무 넓은 범위**

```java
// 프로젝트 전체 적용 - 스프링 내부 빈까지 포함될 수 있어 성능/부작용 주의
@Around("execution(* hello.hello_spring..*(..))")
```

> 현재 프로젝트에서는 `hello.hello_spring..*`으로 전체를 잡고 있습니다.
> 실무에서는 서비스, 리포지토리 등 적용 대상을 명확히 좁히는 것이 좋습니다.

### 6.2 @Bean 등록 방식 선호

```java
// ✅ 권장: SpringConfig에서 명시적 등록
@Bean
public TimeTraceAop timeTraceAop() {
    return new TimeTraceAop();
}

// ⚠️ 주의: @Component 방식도 동작하지만
// AOP처럼 특수한 동작(프록시 생성)을 하는 객체는
// 명시적 @Bean 등록으로 의도를 드러내는 것이 좋음
```

### 6.3 AOP는 스프링 빈에만 적용됨

```java
// ✅ 스프링 빈 → AOP 적용됨
@Service
public class MemberService { ... }  // 스프링 빈 → TimeTraceAop 적용 O

// ❌ 스프링 빈이 아닌 객체 → AOP 적용 안됨
MemberService ms = new MemberService(repo);  // new로 직접 생성 → AOP 적용 X
```

> AOP는 스프링 컨테이너가 관리하는 **빈(Bean) 에만** 적용됩니다.
> `new`로 직접 생성한 객체에는 프록시가 생성되지 않아 AOP가 동작하지 않습니다.

### 6.4 final 클래스/메서드에는 AOP 불가

```java
// ❌ CGLIB 프록시는 상속 기반 → final이면 상속 불가
public final class MemberService { ... }  // AOP 적용 불가!

public class MemberService {
    public final Long join(Member member) { ... }  // AOP 적용 불가!
}
```

> CGLIB 프록시는 원본 클래스를 **상속**하여 프록시를 만들기 때문에,
> `final` 클래스나 `final` 메서드에는 적용할 수 없습니다.

### 6.5 종합 정리표

| 상황 | ✅ 권장 | ❌ 안티패턴 |
|------|---------|------------|
| 시간 측정 등 공통 기능 | AOP(`@Aspect`, `@Around`) | 모든 메서드에 직접 추가 |
| AOP 빈 등록 | `SpringConfig`에서 `@Bean` 명시적 등록 | `@Component`만 사용 (동작은 하지만) |
| Pointcut 범위 | 서비스 등 필요한 레이어만 좁게 지정 | `* *..*(..)` 등 너무 넓게 지정 |
| AOP 적용 대상 | 스프링 컨테이너가 관리하는 빈 | `new`로 직접 생성한 객체 |

---

## 7. 부록

### 7.1 프로젝트 구조 (변경 사항)

```
hello-spring/
└── src/
    └── main/
        └── java/
            └── hello/
                └── hello_spring/
                    ├── SpringConfig.java        ← TimeTraceAop @Bean 주석 추가
                    ├── aop/
                    │   └── TimeTraceAop.java     ← 이번 강의 추가 (핵심!)
                    ├── controller/
                    │   └── MemberController.java ← 프록시 확인 출력 코드 추가
                    └── service/
                        └── MemberService.java    ← 시간 측정 코드 제거 (주석 처리)
```

### 7.2 핵심 용어 정리

| 용어 | 설명 |
|------|------|
| **AOP** | Aspect-Oriented Programming. 공통 관심 사항을 핵심 관심 사항에서 분리하는 프로그래밍 기법 |
| **Aspect** | 공통 관심 사항(Advice + Pointcut)을 하나의 모듈로 묶은 것 |
| **Advice** | 실제로 실행할 공통 기능 코드. "무엇을 할 것인가?" |
| **Pointcut** | Advice를 어디에 적용할지 선별하는 표현식. "어디에 적용할 것인가?" |
| **JoinPoint** | Advice가 적용될 수 있는 지점. 스프링 AOP에서는 메서드 실행이 JoinPoint |
| **Weaving** | Pointcut으로 선별된 JoinPoint에 Advice를 끼워 넣는 과정 |
| **Proxy** | AOP 적용을 위해 스프링이 생성한 가짜 객체. 원본을 상속(CGLIB)하여 만들어짐 |
| **CGLIB** | Code Generation Library. 바이트코드 조작으로 동적 프록시를 생성하는 라이브러리 |
| **Cross-cutting Concern** | 공통 관심 사항. 여러 모듈에 걸쳐 반복되는 부가 기능 |
| **Core Concern** | 핵심 관심 사항. 해당 모듈이 진짜 해야 할 비즈니스 로직 |
| **@Aspect** | 이 클래스가 Aspect임을 선언하는 어노테이션 |
| **@Around** | 메서드 실행 전/후 모두에 Advice를 적용하는 Around Advice 어노테이션 |
| **ProceedingJoinPoint** | Around Advice에서 사용하는 JoinPoint. `proceed()`로 실제 타겟 메서드 실행 |

### 7.3 어노테이션 정리

| 어노테이션 | 위치 | 역할 |
|-----------|------|------|
| `@Aspect` | 클래스 | 이 클래스가 AOP Aspect임을 선언 |
| `@Around("expression")` | 메서드 | Pointcut 표현식으로 지정된 메서드 실행 전/후에 적용되는 Advice |
| `@Component` | 클래스 | 컴포넌트 스캔으로 스프링 빈 자동 등록 |
| `@Bean` | 메서드 | 반환 객체를 스프링 빈으로 명시적 등록 |
| `@Configuration` | 클래스 | 스프링 설정 파일 선언. `@Bean` 메서드 포함 |

### 7.4 학습 확인 질문

다음 질문에 답하며 학습 내용을 확인해보세요:

1. **모든 메서드에 시간 측정 코드를 직접 추가하는 방법의 문제점 3가지는?**
   - 핵심 로직과 공통 코드 혼재로 가독성 저하, 코드 중복(100개 메서드 = 100번 복붙), 변경 시 모든 메서드 수정 필요

2. **AOP에서 Pointcut이란 무엇인가?**
   - Advice를 어디에 적용할지 선별하는 표현식. `execution(* hello.hello_spring..*(..))` 같은 형태로 패키지, 클래스, 메서드 범위를 지정

3. **`* hello.hello_spring..*(..)` 에서 `..`(점 두 개)의 의미는?**
   - 해당 패키지와 그 하위 패키지 모두를 포함. 점 하나(`.`)는 해당 패키지만 의미

4. **`joinPoint.proceed()`가 하는 역할은?**
   - 실제 타겟 메서드를 실행하고 결과를 반환. 이 호출이 없으면 원본 메서드가 실행되지 않음

5. **AOP 적용 후 `MemberController`에 주입되는 `MemberService`는 진짜 객체인가?**
   - 아니다. 스프링이 CGLIB로 생성한 프록시 객체가 주입된다. `memberService.getClass()` 출력 시 `$$SpringCGLIB$$` 접미어 확인 가능

6. **TimeTraceAop를 `@Bean`으로 등록하는 것을 선호하는 이유는?**
   - AOP는 프록시 생성 등 특수한 동작을 하므로, `SpringConfig`에 명시적으로 등록하여 어디서 빈이 만들어지는지 의도를 명확히 드러내기 위함

7. **AOP가 스프링 빈에만 적용되는 이유는?**
   - 스프링 컨테이너가 빈을 생성할 때 프록시 객체를 대신 등록하는 방식으로 동작하기 때문. `new`로 직접 생성한 객체는 컨테이너가 관리하지 않아 프록시가 만들어지지 않음

### 7.5 강의 전체 마무리

이번 강의(스프링 입문)에서 배운 내용을 돌아봅니다:

```
[강의 전체 흐름]

01. 프로젝트 환경설정
        ↓
02. 스프링 웹 개발 기초 (정적 컨텐츠, MVC, API)
        ↓
03. 회원 관리 예제 - 백엔드 개발 (도메인, 리포지토리, 서비스, 테스트)
        ↓
04. 스프링 빈과 의존관계 (컴포넌트 스캔, @Bean 직접 등록)
        ↓
05. 회원 관리 예제 - 웹 MVC 개발 (컨트롤러, 타임리프)
        ↓
06. 스프링 DB 접근 기술 (JDBC → JdbcTemplate → JPA → Spring Data JPA)
        ↓
07. AOP ← 현재 위치
    → 공통 관심사를 핵심 관심사에서 분리
    → 스프링 입문 강의 완료!
```

---

*작성일: 2026-02-22*
*강의: 인프런 - 김영한의 스프링 입문*
