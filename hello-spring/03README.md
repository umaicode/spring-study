# hello-spring 학습 프로젝트

> 스프링 4계층 아키텍처를 통한 백엔드 개발 입문

## 📚 강의 출처

**인프런 - 김영한의 "스프링 입문 - 코드로 배우는 스프링 부트, 웹 MVC, DB 접근 기술"**

이 프로젝트는 강의를 따라 작성한 학습용 코드이며, 스프링의 핵심 개념을 실습하기 위한 회원 관리 예제입니다.

---

## 🎯 프로젝트 소개

**hello-spring**는 회원 관리 시스템을 통해 스프링의 4계층 아키텍처를 학습하는 프로젝트입니다.

### 학습 목표

1. **스프링 웹 개발 3가지 방식 이해**
   - 정적 컨텐츠 (Static Content)
   - MVC와 템플릿 엔진 (Thymeleaf)
   - API (JSON 응답)

2. **4계층 아키텍처 설계**
   - Domain (도메인 모델)
   - Repository (데이터 접근 계층)
   - Service (비즈니스 로직 계층)
   - Controller (웹 요청 처리 계층)

3. **테스트 주도 개발 (TDD) 실습**
   - JUnit 5를 활용한 단위 테스트
   - Given-When-Then 패턴
   - 테스트 격리와 독립성

4. **의존성 주입 (Dependency Injection) 패턴 적용**
   - 생성자 주입
   - 인터페이스 기반 설계
   - 느슨한 결합

### 기술 스택

- **Spring Boot**: 4.0.2
- **Java**: 17
- **Build Tool**: Gradle
- **Template Engine**: Thymeleaf
- **Testing**: JUnit 5, AssertJ

---

## 🗺️ 학습 로드맵

이 문서는 다음 순서로 학습하도록 구성되어 있습니다:

```
1. 스프링 웹 개발 3가지 방식
   ↓
2. Domain 계층 (비즈니스 핵심 데이터 모델)
   ↓
3. Repository 계층 (데이터 접근 추상화)
   ↓
4. Service 계층 (비즈니스 로직 구현)
   ↓
5. Controller 계층 (웹 요청 처리)
   ↓
6. 테스트 전략 (각 계층의 검증)
   ↓
7. 통합 이해 (계층 간 협력과 데이터 흐름)
```

**왜 이 순서인가?**
- **Bottom-Up 접근**: 기초부터 쌓아 올리면 각 계층의 의존 관계를 자연스럽게 이해할 수 있습니다.
- **점진적 복잡도**: 단순한 Domain부터 복잡한 Controller까지 난이도가 점진적으로 증가합니다.

---

## 📖 목차

1. [스프링 웹 개발 3가지 방식](#1-스프링-웹-개발-3가지-방식)
2. [아키텍처 전체 구조](#2-아키텍처-전체-구조)
3. [계층별 상세 설명](#3-계층별-상세-설명)
   - [3.1 Domain 계층](#31-domain-계층)
   - [3.2 Repository 계층](#32-repository-계층)
   - [3.3 Service 계층](#33-service-계층)
   - [3.4 Controller 계층](#34-controller-계층)
4. [테스트 전략](#4-테스트-전략)
5. [계층 간 협력](#5-계층-간-협력)
6. [Best Practice 및 안티패턴](#6-best-practice-및-안티패턴)
7. [부록](#7-부록)

---

## 1. 스프링 웹 개발 3가지 방식

스프링은 웹 애플리케이션을 개발할 때 크게 3가지 방식을 제공합니다. 각각의 방식은 서로 다른 사용 목적과 동작 방식을 가집니다.

### 1.1 정적 컨텐츠 (Static Content)

**정의**: 서버가 HTML 파일을 그대로 웹 브라우저에 전달하는 방식

#### 동작 흐름

```
웹 브라우저
    ↓ (요청: localhost:8080/hello-static.html)
내장 톰캣 서버
    ↓
스프링 컨테이너
    ↓ (hello-static 관련 컨트롤러를 찾지 못함)
resources/static/hello-static.html
    ↓
웹 브라우저 (HTML 파일 그대로 반환)
```

#### 예시

```html
<!-- resources/static/hello-static.html -->
<!DOCTYPE HTML>
<html>
<head>
    <title>static content</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>
정적 컨텐츠 입니다.
</body>
</html>
```

#### 특징

- **장점**: 간단하고 빠름, 서버 부하 최소
- **단점**: 동적 데이터 표시 불가
- **사용 사례**: 회사 소개, 이용약관, 이미지/CSS/JS 파일

---

### 1.2 MVC와 템플릿 엔진 (Thymeleaf)

**정의**: Controller가 Model에 데이터를 담아 View 템플릿으로 전달하여 동적 HTML을 생성하는 방식

#### 동작 흐름

```
웹 브라우저
    ↓ (요청: localhost:8080/hello-mvc?name=spring)
내장 톰캣 서버
    ↓
스프링 컨테이너
    ↓
HelloController.hello()
    ↓ (Model에 "data" 속성 추가)
ViewResolver
    ↓ (templates/hello.html 템플릿 찾기)
Thymeleaf 템플릿 엔진
    ↓ (템플릿 렌더링 후 HTML 생성)
웹 브라우저 (변환된 HTML 반환)
```

#### 예시

**Controller 코드**:
```java
@Controller
public class HelloController {

    @GetMapping("hello")
    public String hello(Model model) {
        model.addAttribute("data", "spring!!");
        return "hello";  // templates/hello.html로 이동
    }

    @GetMapping("hello-mvc")
    public String helloMvc(@RequestParam(value = "name", required = false) String name,
                          Model model) {
        model.addAttribute("name", name);
        return "hello-template";  // templates/hello-template.html로 이동
    }
}
```

**View 템플릿**:
```html
<!-- resources/templates/hello.html -->
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Hello</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>
<p th:text="'안녕하세요. ' + ${data}">안녕하세요. 손님</p>
</body>
</html>
```

```html
<!-- resources/templates/hello-template.html -->
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<p th:text="'hello ' + ${name}">hello! empty</p>
</body>
</html>
```

#### 특징

- **장점**: 동적 데이터 표시 가능, SEO 유리, 서버 사이드 렌더링
- **단점**: 페이지 새로고침 필요, 서버 부하
- **사용 사례**: 게시판, 상품 목록, 사용자 정보 페이지

---

### 1.3 API (JSON 응답)

**정의**: `@ResponseBody`를 사용하여 객체를 JSON 형태로 직접 반환하는 방식

#### 동작 흐름

```
웹 브라우저
    ↓ (요청: localhost:8080/hello-api?name=spring)
내장 톰캣 서버
    ↓
스프링 컨테이너
    ↓
HelloController.helloApi()
    ↓ (@ResponseBody 감지)
HttpMessageConverter
    ↓ (객체 → JSON 변환)
    └─ StringHttpMessageConverter (문자열인 경우)
    └─ MappingJackson2HttpMessageConverter (객체인 경우)
웹 브라우저 (JSON 데이터 반환)
```

#### 예시

**Controller 코드**:
```java
@Controller
public class HelloController {

    // 문자열 반환
    @GetMapping("hello-string")
    @ResponseBody
    public String helloString(@RequestParam("name") String name) {
        return "hello " + name;  // 문자열 그대로 반환 (HTML X)
    }

    // 객체 반환 → JSON 변환
    @GetMapping("hello-api")
    @ResponseBody
    public Hello helloApi(@RequestParam("name") String name) {
        Hello hello = new Hello();
        hello.setName(name);
        return hello;  // {"name": "spring"} 형태의 JSON 반환
    }

    static class Hello {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
```

**응답 예시**:
```json
// GET localhost:8080/hello-api?name=spring
{
  "name": "spring"
}
```

#### 특징

- **장점**:
  - 프론트엔드(React, Vue)와 백엔드 분리 가능
  - 모바일 앱에서도 동일한 API 사용 가능
  - 네트워크 트래픽 최소화
- **단점**:
  - SEO 불리 (서버 사이드 렌더링 없음)
  - 브라우저에서 직접 보기 어려움
- **사용 사례**:
  - RESTful API 서버
  - SPA (Single Page Application)
  - 모바일 앱 백엔드

---

### 1.4 세 방식 비교

| 구분 | 정적 컨텐츠 | MVC + 템플릿 엔진 | API |
|------|------------|------------------|-----|
| **Controller 필요** | ✗ | ✓ | ✓ |
| **ViewResolver 사용** | ✗ | ✓ | ✗ |
| **HttpMessageConverter 사용** | ✗ | ✗ | ✓ |
| **반환 형태** | HTML 파일 | 렌더링된 HTML | JSON/XML |
| **동적 데이터** | ✗ | ✓ | ✓ |
| **사용 목적** | 고정 페이지 | 서버 렌더링 웹 | 데이터 API |

---

## 2. 아키텍처 전체 구조

### 2.1 4계층 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────┐
│                    웹 브라우저 / 클라이언트                   │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP 요청/응답
                     ↓
┌─────────────────────────────────────────────────────────┐
│                  Controller 계층                         │
│  - HTTP 요청 수신 및 응답                                  │
│  - URL 매핑 (@GetMapping)                                │
│  - 요청 파라미터 추출 (@RequestParam)                       │
│  - Service 계층 호출 후 결과 반환                           │
└────────────────────┬────────────────────────────────────┘
                     │ 의존
                     ↓
┌─────────────────────────────────────────────────────────┐
│                   Service 계층                           │
│  - 비즈니스 로직 구현                                       │
│  - 트랜잭션 관리                                           │
│  - 중복 회원 검증 등의 도메인 규칙                           │
│  - Repository 계층 호출                                   │
└────────────────────┬────────────────────────────────────┘
                     │ 의존
                     ↓
┌─────────────────────────────────────────────────────────┐
│                  Repository 계층                         │
│  - 데이터 접근 로직                                        │
│  - CRUD 연산 (save, findById, findByName, findAll)       │
│  - 데이터 저장소 추상화 (인터페이스)                         │
│  - 구현체 교체 가능 (메모리 → JPA → MyBatis)                │
└────────────────────┬────────────────────────────────────┘
                     │ 사용
                     ↓
┌─────────────────────────────────────────────────────────┐
│                   Domain 계층                            │
│  - 비즈니스 도메인 객체 (Member)                           │
│  - 엔티티, 값 객체                                         │
│  - 핵심 비즈니스 데이터와 규칙                               │
│  - 기술적 의존성 없음 (POJO)                               │
└─────────────────────────────────────────────────────────┘
```

### 2.2 계층별 역할 요약

| 계층 | 역할 | 책임 | 예시 |
|------|------|------|------|
| **Controller** | 웹 요청 처리 | HTTP 요청/응답, URL 매핑, 파라미터 추출 | `HelloController.java` |
| **Service** | 비즈니스 로직 | 중복 검증, 트랜잭션 관리, 도메인 규칙 | `MemberService.java` |
| **Repository** | 데이터 접근 | CRUD 연산, 데이터 저장소 추상화 | `MemberRepository.java` |
| **Domain** | 비즈니스 모델 | 핵심 데이터 구조, 엔티티 | `Member.java` |

### 2.3 의존성 방향 (Dependency Rule)

```
Controller → Service → Repository → Domain
(상위 계층)                        (하위 계층)

원칙: 상위 계층은 하위 계층을 의존하지만, 하위 계층은 상위 계층을 몰라야 한다.
```

**왜 이 방향인가?**
- **Domain은 순수해야 함**: 비즈니스 로직만 포함하고 프레임워크에 의존하지 않음
- **Repository는 저장 기술만**: Service가 무엇인지 몰라야 독립적으로 교체 가능
- **Service는 비즈니스만**: Controller가 바뀌어도 영향 없음
- **Controller는 웹 기술만**: API/MVC 방식을 쉽게 전환 가능

---

### 2.4 회원 가입 시나리오 데이터 흐름

실제 사용자가 회원 가입을 하는 과정을 통해 계층 간 협력을 이해해봅시다.

```
1. 사용자가 브라우저에서 "홍길동"을 입력하고 "가입" 버튼 클릭
   ↓
2. Controller가 HTTP 요청 수신
   POST /members/new
   Body: { "name": "홍길동" }
   ↓
3. Controller가 Member 객체 생성
   Member member = new Member();
   member.setName("홍길동");
   ↓
4. Controller가 Service.join(member) 호출
   ↓
5. Service가 중복 회원 검증
   memberRepository.findByName("홍길동")
   - 결과: Optional.empty() → 통과
   ↓
6. Service가 Repository.save(member) 호출
   ↓
7. Repository가 DB에 저장 (현재는 메모리)
   member.setId(1L);
   store.put(1L, member);
   ↓
8. Repository가 저장된 Member 반환
   ↓
9. Service가 member.getId() 반환 (1L)
   ↓
10. Controller가 "회원가입 성공!" 응답 반환
```

---

## 3. 계층별 상세 설명

이제 각 계층을 **Bottom-Up** 순서로 깊이 있게 학습합니다.

---

## 3.1 Domain 계층

### 3.1.1 전체 코드

```java
// src/main/java/hello/hello_spring/domain/Member.java
package hello.hello_spring.domain;

public class Member {

    private Long id;        // 시스템이 자동으로 부여하는 고유 식별자
    private String name;    // 사용자가 입력한 회원 이름

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

### 3.1.2 역할

**Domain 계층은 비즈니스의 핵심 개념을 표현하는 객체입니다.**

- **회원(Member)**이라는 개념을 코드로 표현
- `id`: 시스템이 관리하는 고유 번호
- `name`: 회원의 이름

### 3.1.3 왜 이렇게 설계했는가?

#### ✅ 1. 단순성 (Simplicity)

```java
// ❌ 나쁜 예: 불필요한 복잡성
public class Member {
    private Long id;
    private String name;
    private Date createdDate;
    private Date updatedDate;
    private String createdBy;
    private boolean deleted;
    // ... 너무 많은 필드
}

// ✅ 좋은 예: 필요한 필드만
public class Member {
    private Long id;
    private String name;
}
```

**이유**: 학습 단계에서는 핵심 개념만 포함합니다. 나중에 필요하면 추가할 수 있습니다.

#### ✅ 2. POJO (Plain Old Java Object)

```java
// ✅ 순수 자바 객체 - 어떤 프레임워크에도 의존하지 않음
public class Member {
    private Long id;
    private String name;
    // getter/setter
}

// ❌ 프레임워크 의존 (학습 단계에서는 나중에 배움)
@Entity
@Table(name = "members")
public class Member {
    @Id @GeneratedValue
    private Long id;
    // ...
}
```

**이유**:
- 테스트하기 쉬움 (스프링 없이도 테스트 가능)
- 재사용 가능 (다른 프로젝트에서도 사용 가능)
- 이해하기 쉬움 (자바만 알면 됨)

#### ✅ 3. 확장 가능성

```java
// 현재: 단순한 구조
public class Member {
    private Long id;
    private String name;
}

// 미래: 필요에 따라 확장
public class Member {
    private Long id;
    private String name;
    private String email;
    private Address address;  // 값 객체
    private List<Order> orders;  // 연관 관계
}
```

### 3.1.4 이 코드의 좋은 점

| 장점 | 설명 |
|------|------|
| **기술 독립성** | 프레임워크에 의존하지 않아 어디서든 사용 가능 |
| **테스트 용이성** | new Member()로 바로 생성 가능, Mock 불필요 |
| **명확한 책임** | 데이터 저장만 담당, 다른 책임 없음 |
| **재사용성** | Service, Repository, Controller 모두에서 사용 |

### 3.1.5 주의사항

#### ⚠️ 1. Getter/Setter 남용 주의

```java
// ❌ 안티패턴: 무분별한 setter 사용
public class MemberService {
    public void join(Member member) {
        member.setId(1L);  // Service가 ID를 직접 설정 (위험!)
        member.setName("변경된이름");  // 어디서나 변경 가능
    }
}

// ✅ 개선: 필요한 곳에서만 setter 사용
public class MemberRepository {
    public Member save(Member member) {
        member.setId(++sequence);  // Repository만 ID 설정
        return member;
    }
}
```

**이유**:
- ID는 Repository가 관리해야 함
- 무분별한 setter는 데이터 일관성을 해칠 수 있음

#### ⚠️ 2. Anemic Domain Model (빈약한 도메인 모델)

```java
// ❌ 현재: 데이터만 있고 행동이 없음 (Anemic)
public class Member {
    private Long id;
    private String name;
    // getter/setter만
}

// ✅ 이상적: 비즈니스 로직 포함 (Rich Domain Model)
public class Member {
    private Long id;
    private String name;

    public void changeName(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        this.name = newName;
    }

    public boolean isSameName(String name) {
        return this.name.equals(name);
    }
}
```

**이유**:
- 학습 단계에서는 Anemic도 괜찮음
- 실무에서는 도메인 로직을 Domain 계층에 넣는 것이 좋음

### 3.1.6 실무 팁

#### 💡 1. Lombok 사용

```java
// 실무에서는 Lombok으로 간결하게
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Member {
    private Long id;
    private String name;
}
```

#### 💡 2. JPA 어노테이션 추가

```java
// DB와 연동할 때
import javax.persistence.*;

@Entity
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;
}
```

#### 💡 3. DTO와 Entity 분리

```java
// Entity (DB 테이블과 매핑)
@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String password;  // 민감 정보
}

// DTO (API 응답용 - 비밀번호 제외)
public class MemberResponseDto {
    private Long id;
    private String name;
    // password 필드 없음!
}
```

---

## 3.2 Repository 계층

### 3.2.1 전체 코드

#### 인터페이스

```java
// src/main/java/hello/hello_spring/repository/MemberRepository.java
package hello.hello_spring.repository;

import hello.hello_spring.domain.Member;
import java.util.List;
import java.util.Optional;

public interface MemberRepository {
    Member save(Member member);              // 회원 저장
    Optional<Member> findById(Long id);      // ID로 회원 조회
    Optional<Member> findByName(String name);  // 이름으로 회원 조회
    List<Member> findAll();                  // 전체 회원 조회
}
```

#### 구현체

```java
// src/main/java/hello/hello_spring/repository/MemoryMemberRepository.java
package hello.hello_spring.repository;

import hello.hello_spring.domain.Member;
import java.util.*;

public class MemoryMemberRepository implements MemberRepository {

    // static: 모든 인스턴스가 공유하는 저장소
    private static Map<Long, Member> store = new HashMap<>();
    private static long sequence = 0L;  // ID 생성용 카운터

    @Override
    public Member save(Member member) {
        member.setId(++sequence);      // ID 자동 증가 (1, 2, 3, ...)
        store.put(member.getId(), member);
        return member;
    }

    @Override
    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(store.get(id));  // null일 수 있으므로 Optional
    }

    @Override
    public Optional<Member> findByName(String name) {
        return store.values().stream()
                .filter(member -> member.getName().equals(name))
                .findAny();  // 하나라도 찾으면 반환
    }

    @Override
    public List<Member> findAll() {
        return new ArrayList<>(store.values());  // 방어적 복사
    }

    public void clearStore() {
        store.clear();  // 테스트용: 데이터 초기화
    }
}
```

### 3.2.2 역할

**Repository 계층은 데이터 접근을 추상화하는 계층입니다.**

- **저장 기술 숨기기**: Service는 데이터가 메모리에 있는지, DB에 있는지 몰라도 됨
- **CRUD 연산 제공**: 저장, 조회, 수정, 삭제 기능
- **구현체 교체 가능**: 메모리 → JPA → MyBatis 전환 시 Service 코드는 변경 없음

### 3.2.3 왜 인터페이스인가?

```java
// ❌ 인터페이스 없이 구현체만 사용
public class MemberService {
    private final MemoryMemberRepository memberRepository = new MemoryMemberRepository();

    // 문제점:
    // 1. JPA로 바꾸려면? → MemberService 전체 수정 필요
    // 2. 테스트할 때 Mock을 만들기 어려움
}

// ✅ 인터페이스 사용
public class MemberService {
    private final MemberRepository memberRepository;  // 인터페이스 타입

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;  // 구현체는 외부에서 주입
    }

    // 장점:
    // 1. JPA로 바꿔도 MemberService 수정 불필요
    // 2. 테스트 시 Mock 객체 주입 가능
}
```

### 3.2.4 패턴 분석

#### 🎨 1. Repository Pattern

**정의**: 데이터 접근 로직을 캡슐화하여 도메인과 데이터 저장소를 분리

```
Service -----------> MemberRepository (인터페이스)
                            ↑
                            | 구현
          ┌─────────────────┴─────────────────┐
          │                                   │
MemoryMemberRepository              JpaMemberRepository
(메모리 저장)                         (DB 저장)
```

**장점**:
- Service는 저장 기술을 몰라도 됨
- 저장 기술 교체 시 Repository만 교체
- 테스트 시 Fake Repository 사용 가능

#### 🎨 2. Optional 패턴

```java
// ❌ null 반환 방식 (전통적)
public Member findById(Long id) {
    return store.get(id);  // null일 수 있음
}

// 사용하는 쪽
Member member = repository.findById(1L);
if (member != null) {  // null 체크 깜빡하면 NullPointerException
    System.out.println(member.getName());
}

// ✅ Optional 방식 (Java 8+)
public Optional<Member> findById(Long id) {
    return Optional.ofNullable(store.get(id));
}

// 사용하는 쪽
repository.findById(1L)
    .ifPresent(member -> System.out.println(member.getName()));  // null 안전
```

**장점**:
- NullPointerException 방지
- 명시적으로 "값이 없을 수 있음"을 표현
- 함수형 스타일 체이닝 가능

#### 🎨 3. Stream API

```java
@Override
public Optional<Member> findByName(String name) {
    return store.values().stream()              // 1. Stream 생성
            .filter(member -> member.getName().equals(name))  // 2. 필터링
            .findAny();                         // 3. 하나 찾기
}

// 전통적 방식과 비교
public Optional<Member> findByNameOldStyle(String name) {
    for (Member member : store.values()) {
        if (member.getName().equals(name)) {
            return Optional.of(member);
        }
    }
    return Optional.empty();
}
```

**장점**:
- 간결하고 읽기 쉬움
- 병렬 처리 가능 (parallelStream)
- 함수형 프로그래밍 스타일

#### 🎨 4. 방어적 복사 (Defensive Copy)

```java
@Override
public List<Member> findAll() {
    return new ArrayList<>(store.values());  // 새 리스트 생성
}

// ❌ 방어적 복사 없이 반환하면?
public List<Member> findAllBad() {
    return store.values();  // 내부 컬렉션 직접 노출
}

// 문제 상황
List<Member> members = repository.findAllBad();
members.clear();  // 외부에서 Repository 내부 데이터를 삭제할 수 있음!
```

**이유**:
- 내부 데이터 보호
- 의도하지 않은 수정 방지
- 캡슐화 유지

### 3.2.5 이 코드의 좋은 점

| 장점 | 설명 | 예시 |
|------|------|------|
| **구현체 교체 가능** | 인터페이스 덕분에 저장 기술 변경 시 Service 수정 불필요 | 메모리 → JPA 전환 |
| **테스트 용이성** | Mock Repository를 만들어 Service 테스트 가능 | `FakeMemberRepository` |
| **명확한 책임** | 데이터 접근만 담당, 비즈니스 로직 없음 | `save()`, `findById()` |
| **Optional 사용** | null 안전성 향상 | `findById()` |
| **Stream API** | 간결한 조회 로직 | `findByName()` |

### 3.2.6 주의사항

#### ⚠️ 1. static 사용의 문제

```java
private static Map<Long, Member> store = new HashMap<>();
private static long sequence = 0L;
```

**문제점**:
- 모든 인스턴스가 같은 데이터를 공유
- 테스트 시 데이터가 섞일 수 있음

```java
// 문제 상황
@Test
void test1() {
    MemoryMemberRepository repo1 = new MemoryMemberRepository();
    repo1.save(member1);  // store에 저장
}

@Test
void test2() {
    MemoryMemberRepository repo2 = new MemoryMemberRepository();
    // repo1과 repo2가 같은 store를 공유하므로
    // test1의 데이터가 남아있을 수 있음!
}
```

**해결책**:
- `@AfterEach`로 테스트마다 `clearStore()` 호출
- 실무에서는 싱글톤 패턴 또는 스프링 빈으로 관리

#### ⚠️ 2. 동시성 문제

```java
private static Map<Long, Member> store = new HashMap<>();  // Thread-Safe 하지 않음
private static long sequence = 0L;  // 동시에 ++하면 문제
```

**문제 상황**:
```java
// 두 스레드가 동시에 save() 호출
Thread1: member.setId(++sequence);  // sequence = 1
Thread2: member.setId(++sequence);  // sequence = 1 (중복!)
```

**해결책**:
```java
private static Map<Long, Member> store = new ConcurrentHashMap<>();
private static AtomicLong sequence = new AtomicLong(0L);

public Member save(Member member) {
    member.setId(sequence.incrementAndGet());  // Thread-Safe
    store.put(member.getId(), member);
    return member;
}
```

#### ⚠️ 3. findByName 중복 처리

```java
public Optional<Member> findByName(String name) {
    return store.values().stream()
            .filter(member -> member.getName().equals(name))
            .findAny();  // 첫 번째 것만 반환
}
```

**문제점**:
- 동명이인이 있으면 첫 번째 사람만 반환
- 실무에서는 `findAllByName()` 또는 `unique constraint` 필요

### 3.2.7 실무 팁

#### 💡 1. Spring Data JPA 사용

```java
// 인터페이스만 정의하면 스프링이 자동으로 구현체 생성
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByName(String name);  // 메서드 이름으로 쿼리 자동 생성
    List<Member> findByNameContaining(String keyword);  // LIKE 검색
}

// 사용하는 쪽
@Service
public class MemberService {
    private final MemberRepository memberRepository;  // 자동 주입

    public Long join(Member member) {
        memberRepository.save(member);  // JPA가 알아서 SQL 생성
        return member.getId();
    }
}
```

#### 💡 2. QueryDSL로 복잡한 쿼리

```java
// 동적 쿼리 예시
public List<Member> searchMembers(String name, Integer ageGoe) {
    return queryFactory
        .selectFrom(member)
        .where(
            nameEq(name),    // null이면 조건 무시
            ageGoe(ageGoe)   // null이면 조건 무시
        )
        .fetch();
}
```

#### 💡 3. Custom Repository 패턴

```java
// 복잡한 쿼리는 별도 인터페이스로 분리
public interface MemberRepositoryCustom {
    List<Member> findComplexQuery(...);
}

public interface MemberRepository extends JpaRepository<Member, Long>,
                                          MemberRepositoryCustom {
    // 기본 CRUD + 커스텀 메서드
}
```

---

## 3.3 Service 계층

### 3.3.1 전체 코드

```java
// src/main/java/hello/hello_spring/service/MemberService.java
package hello.hello_spring.service;

import hello.hello_spring.domain.Member;
import hello.hello_spring.repository.MemberRepository;
import java.util.List;
import java.util.Optional;

public class MemberService {

    private final MemberRepository memberRepository;

    // 생성자 주입 (Dependency Injection)
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * 회원 가입
     */
    public Long join(Member member) {
        validateDuplicateMember(member);    // 중복 회원 검증
        memberRepository.save(member);
        return member.getId();
    }

    // 메서드 추출: 중복 검증 로직 분리
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

    /**
     * 회원 한 명 조회
     */
    public Optional<Member> findOne(Long memberId) {
        return memberRepository.findById(memberId);
    }
}
```

### 3.3.2 역할

**Service 계층은 비즈니스 로직을 구현하는 계층입니다.**

- **비즈니스 규칙**: "중복 회원은 가입할 수 없다"와 같은 도메인 규칙 구현
- **트랜잭션 관리**: 여러 Repository 호출을 하나의 작업 단위로 묶음
- **Controller와 Repository 중재**: Controller는 Service를 통해서만 데이터 접근

### 3.3.3 의존성 주입 상세 분석

#### ❌ 의존성 주입 없이 (강한 결합)

```java
public class MemberService {
    private final MemberRepository memberRepository = new MemoryMemberRepository();

    // 문제점:
    // 1. MemoryMemberRepository가 변경되면 MemberService도 수정 필요
    // 2. 테스트 시 다른 Repository를 사용할 수 없음
    // 3. MemberService가 Repository의 생성 책임까지 가짐
}
```

#### ✅ 생성자 주입 (느슨한 결합)

```java
public class MemberService {
    private final MemberRepository memberRepository;

    // 외부에서 Repository를 주입받음
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // 장점:
    // 1. 어떤 Repository든 사용 가능 (메모리, JPA, MyBatis)
    // 2. 테스트 시 Mock Repository 주입 가능
    // 3. final로 선언 가능 → 불변성 보장
}
```

#### 사용 예시

```java
// 프로덕션 코드
MemberRepository repository = new MemoryMemberRepository();
MemberService service = new MemberService(repository);

// 테스트 코드
MemberRepository mockRepository = new FakeMemberRepository();
MemberService service = new MemberService(mockRepository);

// 스프링 사용 시
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    @Autowired  // 생략 가능 (생성자 하나면 자동 주입)
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
```

**왜 생성자 주입이 좋은가?**

| 이유 | 설명 |
|------|------|
| **불변성** | `final`로 선언 가능 → 런타임에 변경 불가 |
| **명시성** | 필요한 의존성이 생성자에 명확히 드러남 |
| **테스트 용이성** | new로 생성 시 의존성을 명시적으로 주입 |
| **순환 참조 방지** | 컴파일 시점에 순환 참조 감지 |

### 3.3.4 패턴 분석

#### 🎨 1. Facade Pattern (파사드 패턴)

**정의**: 복잡한 하위 시스템을 간단한 인터페이스로 감싸는 패턴

```java
public class MemberService {

    // Controller는 이 간단한 메서드만 호출
    public Long join(Member member) {
        validateDuplicateMember(member);    // 복잡한 검증 로직 숨김
        memberRepository.save(member);      // Repository 호출 숨김
        return member.getId();
    }

    // 복잡한 내부 로직
    private void validateDuplicateMember(Member member) {
        memberRepository.findByName(member.getName())
                .ifPresent(m -> {
                    throw new IllegalStateException("이미 존재하는 회원입니다.");
                });
    }
}
```

**장점**:
- Controller는 비즈니스 로직을 몰라도 됨
- 복잡한 로직을 캡슐화
- 변경 시 Service만 수정

#### 🎨 2. Strategy Pattern (전략 패턴)

```java
// 현재 코드: Repository를 전략으로 교체 가능
public class MemberService {
    private final MemberRepository memberRepository;  // 전략 인터페이스

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;  // 전략 주입
    }
}

// 다양한 전략 (구현체)
MemberService service1 = new MemberService(new MemoryMemberRepository());
MemberService service2 = new MemberService(new JpaMemberRepository());
MemberService service3 = new MemberService(new MyBatisMemberRepository());
```

#### 🎨 3. 메서드 추출 (Extract Method)

```java
// ❌ 리팩토링 전: 로직이 한 메서드에 모두 있음
public Long join(Member member) {
    // 중복 회원 검증
    Optional<Member> result = memberRepository.findByName(member.getName());
    result.ifPresent(m -> {
        throw new IllegalStateException("이미 존재하는 회원입니다.");
    });

    memberRepository.save(member);
    return member.getId();
}

// ✅ 리팩토링 후: 검증 로직을 별도 메서드로 분리
public Long join(Member member) {
    validateDuplicateMember(member);    // 의도가 명확
    memberRepository.save(member);
    return member.getId();
}

private void validateDuplicateMember(Member member) {
    memberRepository.findByName(member.getName())
            .ifPresent(m -> {
                throw new IllegalStateException("이미 존재하는 회원입니다.");
            });
}
```

**장점**:
- 메서드 이름으로 의도 표현
- 재사용 가능
- 테스트하기 쉬움

### 3.3.5 이 코드의 좋은 점

| 장점 | 설명 | 코드 위치 |
|------|------|----------|
| **단일 책임** | 비즈니스 로직만 담당 | 전체 |
| **DI 적용** | 구현체 교체 가능 | 생성자 |
| **명확한 메서드명** | `join`, `findMembers` 등 도메인 용어 사용 | 각 메서드 |
| **예외 처리** | 중복 회원 시 명확한 예외 메시지 | `validateDuplicateMember()` |
| **테스트 가능** | Mock Repository 주입으로 독립 테스트 | 생성자 주입 |

### 3.3.6 주의사항

#### ⚠️ 1. God Object (신 객체) 방지

```java
// ❌ 안티패턴: 모든 비즈니스 로직을 하나의 Service에
public class MemberService {
    public void join(Member member) { }
    public void updateMember(Member member) { }
    public void deleteMember(Long id) { }
    public void sendEmail(Member member) { }
    public void createOrder(Member member, Order order) { }
    public void processPayment(Payment payment) { }
    // ... 100개의 메서드
}

// ✅ 개선: 책임별로 Service 분리
public class MemberService {
    public void join(Member member) { }
    public void updateMember(Member member) { }
    public void deleteMember(Long id) { }
}

public class EmailService {
    public void sendEmail(Member member) { }
}

public class OrderService {
    public void createOrder(Member member, Order order) { }
}
```

#### ⚠️ 2. Controller 로직과의 명확한 분리

```java
// ❌ Service에 Controller 책임이 섞임
public class MemberService {
    public String joinAndReturnView(HttpServletRequest request) {
        String name = request.getParameter("name");  // HTTP 의존
        Member member = new Member();
        member.setName(name);
        memberRepository.save(member);
        return "redirect:/members";  // View 정보
    }
}

// ✅ Service는 비즈니스 로직만
public class MemberService {
    public Long join(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }
}

// Controller가 HTTP 처리
@Controller
public class MemberController {
    @PostMapping("/members/new")
    public String create(MemberForm form) {
        Member member = new Member();
        member.setName(form.getName());

        memberService.join(member);  // Service 호출
        return "redirect:/members";  // View 처리
    }
}
```

#### ⚠️ 3. 트랜잭션 경계

```java
// 현재 코드: 트랜잭션 없음 (메모리 저장이라 문제없음)
public Long join(Member member) {
    validateDuplicateMember(member);
    memberRepository.save(member);
    return member.getId();
}

// 실무: DB 사용 시 트랜잭션 필수
@Transactional
public Long join(Member member) {
    validateDuplicateMember(member);
    memberRepository.save(member);
    // 여기서 예외 발생 시 자동 롤백
    return member.getId();
}
```

### 3.3.7 실무 팁

#### 💡 1. @Service와 @Transactional 사용

```java
@Service
@Transactional(readOnly = true)  // 조회는 readOnly
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional  // 변경은 readOnly = false
    public Long join(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    // readOnly = true (클래스 레벨)
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }
}
```

#### 💡 2. DTO 변환

```java
// Entity를 직접 반환하지 않고 DTO로 변환
@Service
public class MemberService {

    public MemberResponseDto findMemberDto(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return new MemberResponseDto(member.getId(), member.getName());
    }
}

// DTO 클래스
public class MemberResponseDto {
    private Long id;
    private String name;

    public MemberResponseDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
```

#### 💡 3. 예외 처리 전략

```java
// Custom Exception 사용
public class DuplicateMemberException extends RuntimeException {
    public DuplicateMemberException(String message) {
        super(message);
    }
}

@Service
public class MemberService {

    public Long join(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
        memberRepository.findByName(member.getName())
                .ifPresent(m -> {
                    throw new DuplicateMemberException("이미 존재하는 회원입니다: " + member.getName());
                });
    }
}
```

---

## 3.4 Controller 계층

### 3.4.1 전체 코드

```java
// src/main/java/hello/hello_spring/controller/HelloController.java
package hello.hello_spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloController {

    // 방식 1: MVC 패턴 - View 반환
    @GetMapping("hello")
    public String hello(Model model) {
        model.addAttribute("data", "spring!!");
        return "hello";  // templates/hello.html
    }

    // 방식 2: MVC 패턴 - 파라미터 받기
    @GetMapping("hello-mvc")
    public String helloMvc(@RequestParam(value = "name", required = false) String name,
                          Model model) {
        model.addAttribute("name", name);
        return "hello-template";  // templates/hello-template.html
    }

    // 방식 3: API - 문자열 반환
    @GetMapping("hello-string")
    @ResponseBody
    public String helloString(@RequestParam("name") String name) {
        return "hello " + name;  // View 없이 문자열 그대로 반환
    }

    // 방식 4: API - JSON 반환
    @GetMapping("hello-api")
    @ResponseBody
    public Hello helloApi(@RequestParam("name") String name) {
        Hello hello = new Hello();
        hello.setName(name);
        return hello;  // 객체를 JSON으로 자동 변환
    }

    // 내부 클래스: API 응답용 DTO
    static class Hello {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
```

### 3.4.2 역할

**Controller 계층은 HTTP 요청을 받아 적절한 응답을 반환하는 계층입니다.**

- **URL 매핑**: `@GetMapping`, `@PostMapping` 등으로 URL과 메서드 연결
- **파라미터 추출**: `@RequestParam`, `@PathVariable` 등으로 요청 데이터 추출
- **Service 호출**: 비즈니스 로직은 Service에 위임
- **응답 반환**: View 이름 또는 데이터를 반환

### 3.4.3 4가지 응답 방식 비교

#### 방식 1: View 반환

```java
@GetMapping("hello")
public String hello(Model model) {
    model.addAttribute("data", "spring!!");
    return "hello";  // ViewResolver가 templates/hello.html을 찾음
}
```

**흐름**:
```
브라우저 → Controller → ViewResolver → Thymeleaf → HTML 반환
```

#### 방식 2: MVC + 파라미터

```java
@GetMapping("hello-mvc")
public String helloMvc(@RequestParam(value = "name", required = false) String name,
                      Model model) {
    model.addAttribute("name", name);
    return "hello-template";
}
```

**특징**:
- `required = false`: 파라미터 선택적 (기본값: true)
- `@RequestParam("name")`: URL의 `?name=값` 추출

#### 방식 3: 문자열 API

```java
@GetMapping("hello-string")
@ResponseBody
public String helloString(@RequestParam("name") String name) {
    return "hello " + name;
}
```

**특징**:
- `@ResponseBody`: View 없이 데이터 그대로 반환
- `StringHttpMessageConverter` 사용

#### 방식 4: JSON API

```java
@GetMapping("hello-api")
@ResponseBody
public Hello helloApi(@RequestParam("name") String name) {
    Hello hello = new Hello();
    hello.setName(name);
    return hello;  // {"name": "spring"}
}
```

**특징**:
- `@ResponseBody`: 객체를 JSON으로 변환
- `MappingJackson2HttpMessageConverter` 사용

### 3.4.4 비교표

| 방식 | 어노테이션 | 반환 타입 | 실제 응답 | 사용 목적 |
|------|-----------|----------|----------|----------|
| **View** | `@GetMapping` | `String` | HTML 페이지 | 웹 페이지 |
| **MVC + 파라미터** | `@GetMapping` | `String` | HTML 페이지 | 동적 웹 페이지 |
| **문자열 API** | `@GetMapping` + `@ResponseBody` | `String` | 문자열 | 간단한 응답 |
| **JSON API** | `@GetMapping` + `@ResponseBody` | 객체 | JSON | RESTful API |

### 3.4.5 패턴 분석

#### 🎨 1. MVC Pattern

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ HTTP 요청
       ↓
┌─────────────┐
│ Controller  │ ← URL 매핑, 파라미터 추출
└──────┬──────┘
       │ 데이터 전달
       ↓
┌─────────────┐
│    Model    │ ← 비즈니스 데이터
└──────┬──────┘
       │ 렌더링
       ↓
┌─────────────┐
│    View     │ ← HTML 생성 (Thymeleaf)
└──────┬──────┘
       │ HTTP 응답
       ↓
┌─────────────┐
│   Browser   │
└─────────────┘
```

#### 🎨 2. Front Controller Pattern

```java
@Controller  // 이 클래스가 Front Controller 역할
public class HelloController {

    @GetMapping("hello")       // URL: /hello
    public String hello() { }

    @GetMapping("hello-mvc")   // URL: /hello-mvc
    public String helloMvc() { }

    @GetMapping("hello-api")   // URL: /hello-api
    public Hello helloApi() { }
}

// 스프링의 DispatcherServlet이 모든 요청을 받아
// 적절한 Controller 메서드로 라우팅
```

#### 🎨 3. Template Method Pattern

```java
// 스프링이 자동으로 처리하는 흐름 (템플릿)
public void handleRequest(HttpServletRequest request) {
    // 1. 파라미터 추출 (스프링이 자동)
    String name = request.getParameter("name");

    // 2. Controller 메서드 호출 (개발자가 작성)
    String viewName = helloMvc(name, model);

    // 3. ViewResolver 호출 (스프링이 자동)
    View view = viewResolver.resolve(viewName);

    // 4. 렌더링 (스프링이 자동)
    view.render(model, response);
}
```

### 3.4.6 이 코드의 좋은 점

| 장점 | 설명 | 코드 |
|------|------|------|
| **명확한 URL 매핑** | 어노테이션으로 라우팅 설정 | `@GetMapping("hello")` |
| **자동 파라미터 바인딩** | 스프링이 자동으로 변환 | `@RequestParam` |
| **자동 JSON 변환** | 객체를 JSON으로 자동 변환 | `@ResponseBody` |
| **타입 안정성** | String, Integer 등 자동 변환 | `@RequestParam("id") Long id` |

### 3.4.7 주의사항

#### ⚠️ 1. Controller에 비즈니스 로직 금지

```java
// ❌ 안티패턴: Controller에 비즈니스 로직
@Controller
public class MemberController {

    @PostMapping("/members/new")
    public String create(MemberForm form) {
        Member member = new Member();
        member.setName(form.getName());

        // 비즈니스 로직이 Controller에 있음 (나쁨)
        Optional<Member> existing = memberRepository.findByName(member.getName());
        if (existing.isPresent()) {
            throw new IllegalStateException("중복 회원");
        }
        memberRepository.save(member);

        return "redirect:/";
    }
}

// ✅ 개선: Service에 위임
@Controller
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/members/new")
    public String create(MemberForm form) {
        Member member = new Member();
        member.setName(form.getName());

        memberService.join(member);  // Service에 위임

        return "redirect:/";
    }
}
```

#### ⚠️ 2. Entity와 DTO 분리

```java
// ❌ Entity를 직접 반환 (보안 위험)
@GetMapping("/api/members/{id}")
@ResponseBody
public Member getMember(@PathVariable Long id) {
    return memberService.findOne(id).get();
    // Member에 password 필드가 있다면? → 노출됨!
}

// ✅ DTO로 변환하여 반환
@GetMapping("/api/members/{id}")
@ResponseBody
public MemberResponseDto getMember(@PathVariable Long id) {
    Member member = memberService.findOne(id).get();
    return new MemberResponseDto(member.getId(), member.getName());
    // password는 DTO에 포함하지 않음
}
```

#### ⚠️ 3. required 파라미터 처리

```java
// ❌ required = true인데 값이 없으면?
@GetMapping("hello-mvc")
public String helloMvc(@RequestParam("name") String name, Model model) {
    // GET /hello-mvc → 400 Bad Request
}

// ✅ 옵션 1: required = false
@GetMapping("hello-mvc")
public String helloMvc(@RequestParam(value = "name", required = false) String name,
                      Model model) {
    // name이 null일 수 있음
}

// ✅ 옵션 2: defaultValue 사용
@GetMapping("hello-mvc")
public String helloMvc(@RequestParam(value = "name", defaultValue = "Guest") String name,
                      Model model) {
    // name이 없으면 "Guest"
}
```

### 3.4.8 실무 팁

#### 💡 1. @RestController 사용

```java
// @Controller + @ResponseBody를 모든 메서드에 붙이는 대신
@RestController  // 모든 메서드에 @ResponseBody 자동 적용
@RequestMapping("/api")
public class MemberApiController {

    @GetMapping("/members/{id}")
    public MemberResponseDto getMember(@PathVariable Long id) {
        // JSON 자동 반환
    }

    @PostMapping("/members")
    public MemberResponseDto createMember(@RequestBody MemberRequestDto dto) {
        // JSON 자동 파싱
    }
}
```

#### 💡 2. @ExceptionHandler로 예외 처리

```java
@RestController
public class MemberApiController {

    @GetMapping("/api/members/{id}")
    public MemberResponseDto getMember(@PathVariable Long id) {
        return memberService.findOne(id)
                .map(MemberResponseDto::new)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(MemberNotFoundException e) {
        ErrorResponse error = new ErrorResponse("NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

#### 💡 3. API 버전 관리

```java
@RestController
@RequestMapping("/api/v1/members")  // 버전 1
public class MemberApiControllerV1 {
    // 기존 API
}

@RestController
@RequestMapping("/api/v2/members")  // 버전 2
public class MemberApiControllerV2 {
    // 새로운 API (기존 클라이언트에 영향 없음)
}
```

---

## 4. 테스트 전략

테스트는 코드의 정확성을 보장하고, 리팩토링 시 안전망을 제공합니다.

### 4.1 Repository 테스트

#### 전체 코드

```java
// src/test/java/hello/hello_spring/repository/MemoryMemberRepositoryTest.java
package hello.hello_spring.repository;

import hello.hello_spring.domain.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class MemoryMemberRepositoryTest {

    MemoryMemberRepository repository = new MemoryMemberRepository();

    @AfterEach
    public void afterEach() {
        repository.clearStore();  // 각 테스트 후 데이터 초기화
    }

    @Test
    public void save() {
        // given
        Member member = new Member();
        member.setName("spring");

        // when
        repository.save(member);

        // then
        Member result = repository.findById(member.getId()).get();
        assertThat(member).isEqualTo(result);
    }

    @Test
    public void findByName() {
        // given
        Member member1 = new Member();
        member1.setName("spring1");
        repository.save(member1);

        Member member2 = new Member();
        member2.setName("spring2");
        repository.save(member2);

        // when
        Member result = repository.findByName("spring1").get();

        // then
        assertThat(result).isEqualTo(member1);
    }

    @Test
    public void findAll() {
        // given
        Member member1 = new Member();
        member1.setName("spring1");
        repository.save(member1);

        Member member2 = new Member();
        member2.setName("spring2");
        repository.save(member2);

        // when
        List<Member> result = repository.findAll();

        // then
        assertThat(result.size()).isEqualTo(2);
    }
}
```

#### AAA 패턴 (Arrange-Act-Assert)

```java
@Test
public void save() {
    // Arrange: 테스트 준비
    Member member = new Member();
    member.setName("spring");

    // Act: 테스트 실행
    repository.save(member);

    // Assert: 검증
    Member result = repository.findById(member.getId()).get();
    assertThat(member).isEqualTo(result);
}
```

#### @AfterEach의 중요성

```java
@AfterEach
public void afterEach() {
    repository.clearStore();  // 각 테스트 후 데이터 초기화
}

// 이유:
// test1: spring1 저장 → 성공
// test2: spring1 저장 → 실패 (이미 존재)
// clearStore()가 없으면 테스트 순서에 따라 결과가 달라짐!
```

### 4.2 Service 테스트

#### 전체 코드

```java
// src/test/java/hello/hello_spring/service/MemberServiceTest.java
package hello.hello_spring.service;

import hello.hello_spring.domain.Member;
import hello.hello_spring.repository.MemoryMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberServiceTest {

    MemberService memberService;
    MemoryMemberRepository memberRepository;

    // 각 테스트 전에 새로운 인스턴스 생성
    @BeforeEach
    public void beforeEach() {
        memberRepository = new MemoryMemberRepository();
        memberService = new MemberService(memberRepository);  // DI
    }

    @AfterEach
    public void afterEach() {
        memberRepository.clearStore();
    }

    @Test
    void 회원가입() {
        // given
        Member member = new Member();
        member.setName("spring");

        // when
        Long saveId = memberService.join(member);

        // then
        Member findMember = memberService.findOne(saveId).get();
        assertThat(member.getName()).isEqualTo(findMember.getName());
    }

    @Test
    public void 중복_회원_예외() {
        // given
        Member member1 = new Member();
        member1.setName("spring");

        Member member2 = new Member();
        member2.setName("spring");

        // when
        memberService.join(member1);
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> memberService.join(member2));

        // then
        assertThat(e.getMessage()).isEqualTo("이미 존재하는 회원입니다.");
    }
}
```

#### Given-When-Then 패턴

```java
@Test
void 회원가입() {
    // Given: 테스트를 위한 데이터 준비
    Member member = new Member();
    member.setName("spring");

    // When: 실제 테스트할 동작 실행
    Long saveId = memberService.join(member);

    // Then: 결과 검증
    Member findMember = memberService.findOne(saveId).get();
    assertThat(member.getName()).isEqualTo(findMember.getName());
}
```

#### @BeforeEach로 DI 재현

```java
@BeforeEach
public void beforeEach() {
    memberRepository = new MemoryMemberRepository();
    memberService = new MemberService(memberRepository);  // DI
}

// 이유:
// 1. Service와 Repository가 같은 인스턴스를 사용하도록 보장
// 2. 프로덕션 코드의 DI 구조를 테스트에서도 재현
// 3. 각 테스트마다 새로운 인스턴스로 독립성 보장
```

#### 예외 테스트

```java
@Test
public void 중복_회원_예외() {
    // given
    Member member1 = new Member();
    member1.setName("spring");

    Member member2 = new Member();
    member2.setName("spring");

    // when & then
    memberService.join(member1);

    // assertThrows: 예외가 발생하는지 검증
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> memberService.join(member2));

    // 예외 메시지 검증
    assertThat(e.getMessage()).isEqualTo("이미 존재하는 회원입니다.");
}
```

### 4.3 테스트 비교

| 항목 | Repository 테스트 | Service 테스트 |
|------|------------------|----------------|
| **패턴** | AAA (Arrange-Act-Assert) | Given-When-Then |
| **@BeforeEach** | 사용 안 함 | 사용 (DI 재현) |
| **@AfterEach** | 사용 (데이터 초기화) | 사용 (데이터 초기화) |
| **검증 대상** | 데이터 저장/조회 | 비즈니스 로직 |
| **예외 테스트** | 거의 없음 | 많음 (중복 검증 등) |

### 4.4 AssertJ 사용법

```java
// JUnit 기본 (Old Style)
assertEquals(expected, actual);
assertTrue(result);

// AssertJ (Modern Style)
assertThat(actual).isEqualTo(expected);
assertThat(result).isTrue();
assertThat(list).hasSize(2);
assertThat(member.getName()).isEqualTo("spring");

// 장점:
// 1. 가독성 향상 (자연어에 가까움)
// 2. IDE 자동완성 지원
// 3. 다양한 검증 메서드
```

### 4.5 실무 팁

#### 💡 1. Mockito로 Repository Mock

```java
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    MemberService memberService;

    @Test
    void join() {
        // given
        Member member = new Member();
        member.setName("spring");

        when(memberRepository.findByName("spring"))
            .thenReturn(Optional.empty());

        // when
        memberService.join(member);

        // then
        verify(memberRepository, times(1)).save(member);
    }
}
```

#### 💡 2. @DisplayName으로 명확한 테스트명

```java
@DisplayName("회원 가입 시 중복 회원이 있으면 예외가 발생한다")
@Test
void duplicateMemberException() {
    // ...
}
```

#### 💡 3. @ParameterizedTest로 여러 케이스 테스트

```java
@ParameterizedTest
@ValueSource(strings = {"spring", "김영한", "홍길동"})
void join(String name) {
    // given
    Member member = new Member();
    member.setName(name);

    // when
    Long saveId = memberService.join(member);

    // then
    assertThat(saveId).isNotNull();
}
```

---

## 5. 계층 간 협력

### 5.1 회원 가입 전체 흐름 (6단계)

```
[1단계: HTTP 요청]
POST /members/new
Body: { "name": "홍길동" }
         ↓
[2단계: Controller - HTTP 처리]
@PostMapping("/members/new")
public String create(MemberForm form) {
    Member member = new Member();
    member.setName(form.getName());  // "홍길동"
         ↓
[3단계: Service 호출]
    memberService.join(member);
}
         ↓
[4단계: Service - 비즈니스 로직]
public Long join(Member member) {
    // 중복 검증
    validateDuplicateMember(member);
         ↓
[5단계: Repository 호출]
    memberRepository.save(member);
    return member.getId();
}
         ↓
[6단계: Repository - 데이터 저장]
public Member save(Member member) {
    member.setId(++sequence);  // ID: 1
    store.put(1L, member);
    return member;
}
```

### 5.2 의존성 방향 (Dependency Rule)

```
┌─────────────────────────────────────────────┐
│          의존성 방향 (Dependency Flow)        │
└─────────────────────────────────────────────┘

Controller ────────┐
                   ↓
              Service ────────┐
                              ↓
                         Repository ────────┐
                                            ↓
                                         Domain

규칙:
1. 상위 계층은 하위 계층을 의존
2. 하위 계층은 상위 계층을 모름
3. Domain은 아무것도 의존하지 않음 (POJO)
```

**왜 이 방향인가?**

| 계층 | 의존하는 계층 | 이유 |
|------|-------------|------|
| **Controller** | Service | 비즈니스 로직을 Service에 위임 |
| **Service** | Repository | 데이터 접근을 Repository에 위임 |
| **Repository** | Domain | 저장할 데이터 타입으로 사용 |
| **Domain** | 없음 | 순수한 비즈니스 모델 (POJO) |

### 5.3 데이터 변환 전략

```
┌───────────────────────────────────────────────────┐
│          데이터 변환 전략 (DTO Pattern)             │
└───────────────────────────────────────────────────┘

[HTTP 요청] → [DTO] → [Domain] → [DTO] → [HTTP 응답]
    ↓           ↓         ↓         ↓          ↓
  JSON    MemberForm   Member  MemberDto    JSON

예시:
1. 클라이언트 → Controller: {"name": "홍길동"}
2. Controller → Service: Member(name="홍길동")
3. Service → Repository: Member(id=1, name="홍길동")
4. Repository → Service: Member(id=1, name="홍길동")
5. Service → Controller: Member(id=1, name="홍길동")
6. Controller → 클라이언트: {"id": 1, "name": "홍길동"}
```

### 5.4 왜 DTO를 사용하는가?

#### ❌ DTO 없이 Entity 직접 사용

```java
@RestController
public class MemberApiController {

    @GetMapping("/api/members/{id}")
    public Member getMember(@PathVariable Long id) {
        return memberService.findOne(id).get();
        // 문제:
        // 1. Entity의 모든 필드 노출 (password 등)
        // 2. Entity 변경 시 API 스펙 변경
        // 3. 순환 참조 가능성
    }
}
```

#### ✅ DTO 사용

```java
@RestController
public class MemberApiController {

    @GetMapping("/api/members/{id}")
    public MemberResponseDto getMember(@PathVariable Long id) {
        Member member = memberService.findOne(id).get();
        return new MemberResponseDto(member.getId(), member.getName());
        // 장점:
        // 1. 필요한 필드만 노출
        // 2. Entity 변경해도 API 스펙 안정적
        // 3. 순환 참조 없음
    }
}

// DTO 클래스
public class MemberResponseDto {
    private Long id;
    private String name;
    // password 필드 없음!

    public MemberResponseDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
```

### 5.5 설계 개선 로드맵

```
[1단계: 현재 프로젝트]
- 메모리 저장
- 단순한 구조
- 학습 목적

        ↓

[2단계: DB 연동]
- JPA/MyBatis 도입
- Repository 구현체 교체
- @Transactional 추가

        ↓

[3단계: DTO 분리]
- Entity와 DTO 분리
- Mapper 도입 (ModelMapper, MapStruct)
- API 스펙 안정화

        ↓

[4단계: 예외 처리]
- Custom Exception
- @ExceptionHandler
- 전역 예외 처리

        ↓

[5단계: 실무 수준]
- Spring Security
- 페이징, 정렬
- 검색 기능 (QueryDSL)
- 캐싱 (Redis)
```

---

## 6. Best Practice 및 안티패턴

### 6.1 Best Practice (좋은 관행)

#### ✅ 1. 계층별 책임 명확히

```java
// Controller: HTTP만
@Controller
public class MemberController {
    public String create(MemberForm form) {
        memberService.join(form.toEntity());  // Service에 위임
        return "redirect:/";
    }
}

// Service: 비즈니스 로직만
@Service
public class MemberService {
    public Long join(Member member) {
        validateDuplicateMember(member);  // 비즈니스 규칙
        return memberRepository.save(member).getId();
    }
}

// Repository: 데이터 접근만
public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findById(Long id);
}
```

#### ✅ 2. 인터페이스 기반 설계

```java
// 인터페이스 정의
public interface MemberRepository { }

// 구현체는 여러 개 가능
public class MemoryMemberRepository implements MemberRepository { }
public class JpaMemberRepository implements MemberRepository { }
public class MyBatisMemberRepository implements MemberRepository { }

// Service는 인터페이스에만 의존
public class MemberService {
    private final MemberRepository memberRepository;
}
```

#### ✅ 3. 생성자 주입

```java
@Service
public class MemberService {
    private final MemberRepository memberRepository;  // final 가능

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
```

#### ✅ 4. Optional 활용

```java
public Optional<Member> findById(Long id) {
    return Optional.ofNullable(store.get(id));
}

// 사용하는 쪽
memberRepository.findById(1L)
    .ifPresent(member -> System.out.println(member.getName()));
```

#### ✅ 5. 테스트 격리

```java
@AfterEach
public void afterEach() {
    repository.clearStore();  // 각 테스트 후 초기화
}
```

#### ✅ 6. 메서드 추출

```java
// 복잡한 로직은 메서드로 분리
public Long join(Member member) {
    validateDuplicateMember(member);  // 의도 명확
    memberRepository.save(member);
    return member.getId();
}

private void validateDuplicateMember(Member member) {
    // 검증 로직
}
```

### 6.2 안티패턴 (피해야 할 패턴)

#### ❌ 1. God Object

```java
// 나쁨: 모든 기능을 하나의 클래스에
public class MemberService {
    public void join() { }
    public void update() { }
    public void delete() { }
    public void sendEmail() { }
    public void createOrder() { }
    // ... 100개 메서드
}

// 좋음: 책임별로 분리
public class MemberService {
    public void join() { }
    public void update() { }
}

public class EmailService {
    public void sendEmail() { }
}
```

#### ❌ 2. 계층 간 책임 혼재

```java
// 나쁨: Controller에 비즈니스 로직
@Controller
public class MemberController {
    public String create(MemberForm form) {
        // 중복 검증 (Service가 해야 할 일)
        if (memberRepository.findByName(form.getName()).isPresent()) {
            throw new IllegalStateException("중복 회원");
        }
        memberRepository.save(member);
    }
}
```

#### ❌ 3. Entity 직접 노출

```java
// 나쁨: Entity를 API 응답으로 직접 반환
@GetMapping("/api/members/{id}")
public Member getMember(@PathVariable Long id) {
    return memberService.findOne(id).get();
}

// 좋음: DTO로 변환
@GetMapping("/api/members/{id}")
public MemberResponseDto getMember(@PathVariable Long id) {
    Member member = memberService.findOne(id).get();
    return new MemberResponseDto(member);
}
```

#### ❌ 4. null 반환

```java
// 나쁨: null 반환
public Member findById(Long id) {
    return store.get(id);  // null 가능
}

// 좋음: Optional 반환
public Optional<Member> findById(Long id) {
    return Optional.ofNullable(store.get(id));
}
```

#### ❌ 5. 테스트 의존성

```java
// 나쁨: 테스트 순서에 의존
@Test
void test1() {
    repository.save(member1);
}

@Test
void test2() {
    // test1이 먼저 실행되었다고 가정 (위험!)
    List<Member> members = repository.findAll();
    assertThat(members.size()).isEqualTo(1);
}

// 좋음: 각 테스트 독립적
@AfterEach
void afterEach() {
    repository.clearStore();
}
```

### 6.3 리팩토링 체크리스트

프로젝트를 개선할 때 다음 항목을 확인하세요:

- [ ] 각 계층이 단일 책임을 가지는가?
- [ ] Controller에 비즈니스 로직이 없는가?
- [ ] Service에 HTTP 관련 코드가 없는가?
- [ ] Repository에 비즈니스 로직이 없는가?
- [ ] Domain이 POJO인가?
- [ ] 인터페이스를 사용하고 있는가?
- [ ] 생성자 주입을 사용하는가?
- [ ] Optional을 활용하는가?
- [ ] 테스트가 독립적인가?
- [ ] Entity를 직접 노출하지 않는가?

### 6.4 확장 학습 주제

다음 단계로 학습할 주제들:

1. **Spring Data JPA**
   - JpaRepository 사용
   - 쿼리 메서드
   - @Query 어노테이션

2. **트랜잭션 관리**
   - @Transactional
   - 트랜잭션 전파
   - readOnly 최적화

3. **예외 처리**
   - @ExceptionHandler
   - @ControllerAdvice
   - Custom Exception

4. **DTO 패턴**
   - ModelMapper
   - MapStruct
   - Record (Java 14+)

5. **API 설계**
   - RESTful API
   - HATEOAS
   - API 버전 관리

6. **테스트**
   - Mockito
   - MockMvc
   - TestContainers

7. **보안**
   - Spring Security
   - JWT
   - OAuth2

---

## 7. 부록

### 7.1 프로젝트 구조

```
hello-spring/
├── src/
│   ├── main/
│   │   ├── java/hello/hello_spring/
│   │   │   ├── domain/
│   │   │   │   └── Member.java
│   │   │   ├── repository/
│   │   │   │   ├── MemberRepository.java
│   │   │   │   └── MemoryMemberRepository.java
│   │   │   ├── service/
│   │   │   │   └── MemberService.java
│   │   │   └── controller/
│   │   │       └── HelloController.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── index.html
│   │       │   └── hello-static.html
│   │       └── templates/
│   │           ├── hello.html
│   │           └── hello-template.html
│   └── test/
│       └── java/hello/hello_spring/
│           ├── repository/
│           │   └── MemoryMemberRepositoryTest.java
│           └── service/
│               └── MemberServiceTest.java
└── README.md (이 파일)
```

### 7.2 핵심 용어 정리

| 용어 | 영문 | 설명 |
|------|------|------|
| **POJO** | Plain Old Java Object | 프레임워크에 의존하지 않는 순수 자바 객체 |
| **DI** | Dependency Injection | 의존성 주입 (객체를 외부에서 주입) |
| **IoC** | Inversion of Control | 제어의 역전 (프레임워크가 객체 생성 관리) |
| **DTO** | Data Transfer Object | 계층 간 데이터 전달 객체 |
| **DAO** | Data Access Object | 데이터 접근 객체 (Repository와 유사) |
| **CRUD** | Create, Read, Update, Delete | 기본 데이터 연산 |
| **TDD** | Test-Driven Development | 테스트 주도 개발 |
| **AAA** | Arrange-Act-Assert | 테스트 패턴 (준비-실행-검증) |

### 7.3 스프링 어노테이션 정리

| 어노테이션 | 위치 | 역할 |
|-----------|------|------|
| `@Controller` | Class | 웹 컨트롤러 빈 등록 |
| `@Service` | Class | 서비스 빈 등록 |
| `@Repository` | Class | 레포지토리 빈 등록 |
| `@GetMapping` | Method | HTTP GET 요청 매핑 |
| `@PostMapping` | Method | HTTP POST 요청 매핑 |
| `@RequestParam` | Parameter | URL 파라미터 추출 |
| `@ResponseBody` | Method | View 대신 데이터 반환 |
| `@Transactional` | Method/Class | 트랜잭션 관리 |
| `@Test` | Method | JUnit 테스트 메서드 |
| `@BeforeEach` | Method | 각 테스트 전 실행 |
| `@AfterEach` | Method | 각 테스트 후 실행 |

### 7.4 참고 자료

#### 강의
- [인프런 - 스프링 입문 (김영한)](https://www.inflearn.com/course/스프링-입문-스프링부트)

#### 공식 문서
- [Spring Framework 공식 문서](https://spring.io/projects/spring-framework)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Thymeleaf 공식 문서](https://www.thymeleaf.org/)

#### 도서
- "스프링 입문을 위한 자바 객체 지향의 원리와 이해" - 김종민
- "토비의 스프링 3.1" - 이일민
- "스프링 부트와 AWS로 혼자 구현하는 웹 서비스" - 이동욱

### 7.5 다음 학습 단계

1. **스프링 입문 - 강의 완주**
   - 스프링 빈과 의존관계
   - 회원 관리 예제 - 웹 MVC 개발
   - 스프링 DB 접근 기술
   - AOP

2. **스프링 핵심 원리 - 기본편**
   - 객체 지향 설계와 스프링
   - 스프링 컨테이너와 빈
   - 싱글톤 컨테이너
   - 컴포넌트 스캔
   - 의존관계 자동 주입

3. **실전! 스프링 부트와 JPA 활용**
   - JPA 기본
   - 도메인 설계
   - 웹 계층 개발
   - API 개발

4. **실무 프로젝트**
   - 게시판 만들기
   - 쇼핑몰 만들기
   - RESTful API 서버

---

## 🎓 학습 마무리

### 이 프로젝트를 통해 배운 것

✅ **스프링 웹 개발 3가지 방식**
- 정적 컨텐츠, MVC 템플릿 엔진, API

✅ **4계층 아키텍처**
- Domain, Repository, Service, Controller의 역할과 책임

✅ **설계 원칙**
- 계층 간 의존성 방향
- 인터페이스 기반 설계
- 의존성 주입

✅ **테스트**
- AAA/Given-When-Then 패턴
- 테스트 격리와 독립성

### 다음 학습을 위한 조언

1. **코드를 직접 작성하세요**
   - 강의 코드를 그대로 따라 치는 것보다, 이해한 후 스스로 작성해보세요.

2. **왜?를 항상 물어보세요**
   - "왜 인터페이스를 사용하는가?"
   - "왜 Service와 Repository를 분리하는가?"

3. **실수를 두려워하지 마세요**
   - 틀린 코드를 작성하고 고치는 과정에서 더 많이 배웁니다.

4. **테스트를 습관화하세요**
   - 코드를 작성하면 바로 테스트를 작성하는 습관을 기르세요.

5. **점진적으로 발전하세요**
   - 한 번에 모든 것을 완벽하게 하려고 하지 마세요.
   - 단순한 코드 → 리팩토링 → 개선 순서로 진행하세요.

### 주니어 개발자에게

> "좋은 코드는 한 번에 나오지 않습니다.
> 계속 읽고, 쓰고, 고치면서 점점 좋아집니다.
> 이 프로젝트는 그 여정의 시작입니다."

**배움은 계속됩니다. 화이팅! 🚀**

---

## 📝 라이선스

이 프로젝트는 학습 목적으로 작성되었습니다.
강의 저작권은 인프런과 김영한 님에게 있습니다.
