# 회원 관리 예제 - 웹 MVC 개발

> 회원 등록과 조회를 통해 배우는 스프링 MVC 실전 패턴

## 📚 강의 출처

**인프런 - 김영한의 "스프링 입문 - 코드로 배우는 스프링 부트, 웹 MVC, DB 접근 기술"**

이 문서는 강의의 "회원 관리 예제 - 웹 MVC 개발" 섹션을 학습하며 작성한 노트입니다. HomeController와 MemberController를 직접 구현하면서 스프링 MVC의 실전 패턴을 익히는 것이 목표입니다.

---

## 🎯 학습 목표

이 문서를 통해 다음을 이해하고 실습합니다:

1. **HomeController와 정적 컨텐츠 우선순위 이해**
   - 컨트롤러가 정적 파일(index.html)보다 먼저 검색되는 이유
   - @GetMapping("/")으로 홈 화면을 제어하는 방법

2. **GET/POST 분리 (@GetMapping / @PostMapping)**
   - GET: 폼 화면을 보여주는 역할
   - POST: 폼 데이터를 처리하는 역할
   - 같은 URL에 HTTP 메서드만 다르게 매핑하는 패턴

3. **폼 데이터 바인딩 (MemberForm DTO)**
   - HTML `name` 속성 ↔ Java 필드명 자동 매핑 원리
   - 도메인 객체 대신 별도 Form 객체를 사용하는 이유

4. **Post-Redirect-Get (PRG) 패턴**
   - `redirect:/`의 의미와 필요성
   - 브라우저 새로고침 시 중복 submit을 방지하는 방법

5. **Thymeleaf `th:each`를 통한 목록 렌더링**
   - Model에 데이터를 담아 View에 전달하는 패턴
   - 서버 사이드 템플릿 엔진으로 동적 HTML 생성

---

## 🗺️ 학습 로드맵

이 문서는 **HTTP 요청 흐름 중심**으로 구성되어 있습니다. 사용자의 행동(홈 접속 → 회원 등록 → 목록 조회)에 따라 어떤 코드가 실행되는지를 순서대로 따라갑니다.

```
1. 홈 화면 접속 (GET /)
   - HomeController.home()
   - home.html 반환
   ↓
2. 회원 등록 폼 열기 (GET /members/new)
   - MemberController.createForm()
   - createMemberForm.html 반환
   ↓
3. 회원 등록 제출 (POST /members/new)
   - MemberController.create(MemberForm)
   - MemberForm 바인딩 → Member 생성 → memberService.join()
   ↓
4. PRG 패턴 (redirect:/)
   - 중복 submit 방지
   - 홈 화면으로 리다이렉트
   ↓
5. 회원 목록 조회 (GET /members)
   - MemberController.list(Model)
   - Model에 members 추가 → memberList.html 렌더링
```

**왜 이 순서인가?**
- **사용자 흐름 중심**: 실제 사용자가 겪는 흐름 그대로 학습하면 각 컴포넌트의 역할이 명확해집니다.
- **HTTP 메서드 이해**: GET/POST가 어떤 상황에 쓰이는지를 실전 예제로 체득합니다.

---

## 📖 목차

1. [전체 흐름 이해](#1-전체-흐름-이해)
2. [HomeController - 홈 화면 설정](#2-homecontroller---홈-화면-설정)
3. [회원 등록 - GET (폼 보여주기)](#3-회원-등록---get-폼-보여주기)
4. [회원 등록 - POST (데이터 처리)](#4-회원-등록---post-데이터-처리)
5. [폼 데이터 바인딩 (MemberForm)](#5-폼-데이터-바인딩-memberform)
6. [Post-Redirect-Get 패턴](#6-post-redirect-get-패턴)
7. [회원 목록 조회](#7-회원-목록-조회)
8. [Best Practice 및 안티패턴](#8-best-practice-및-안티패턴)
9. [부록](#9-부록)

---

## 1. 전체 흐름 이해

### 1.1 HTTP 요청/응답 전체 다이어그램

```
[브라우저]                [스프링 컨테이너]               [View - Thymeleaf]

GET /            ──────> HomeController.home()   ──────> home.html
                 <────── "home" (뷰 이름 반환)

GET /members/new ──────> MemberController        ──────> createMemberForm.html
                         .createForm()
                 <────── "members/createMemberForm"

POST /members/new──────> MemberController        (HTML 없음, 리다이렉트)
  [name=김철수]           .create(MemberForm)
                         → member.setName("김철수")
                         → memberService.join(member)
                 <────── "redirect:/"

GET /members     ──────> MemberController        ──────> memberList.html
                         .list(Model)                     th:each로 목록 렌더링
                         → model.addAttribute(
                             "members", members)
                 <────── "members/memberList"
```

### 1.2 GET vs POST 비교표

| 구분 | GET | POST |
|------|-----|------|
| **역할** | 데이터 조회 / 화면 출력 | 데이터 제출 / 처리 |
| **URL 예시** | `GET /members/new` | `POST /members/new` |
| **어노테이션** | `@GetMapping` | `@PostMapping` |
| **파라미터 위치** | URL 쿼리스트링 (`?name=...`) | HTTP 요청 본문 (body) |
| **멱등성** | 멱등 (여러 번 요청해도 결과 동일) | 비멱등 (요청할 때마다 새 데이터 생성) |
| **브라우저 새로고침** | 안전 (같은 데이터 조회) | 위험 (중복 제출 가능) |
| **사용 상황** | 폼 보여주기, 목록 조회 | 회원 등록, 데이터 저장 |

---

## 2. HomeController - 홈 화면 설정

### 2.1 HomeController 코드

```java
package hello.hello_spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }
}
```

### 2.2 왜 static/index.html이 무시되는가?

이전까지는 `resources/static/index.html`이 홈 화면으로 표시됐습니다. 그런데 HomeController를 추가하니 `index.html` 대신 `home.html`이 표시됩니다.

**스프링의 요청 처리 우선순위:**

```
브라우저가 GET / 요청
        ↓
1순위: 스프링 컨테이너에 등록된 컨트롤러 중 "/" 매핑이 있는지 탐색
        ↓  (있음! HomeController의 @GetMapping("/"))
        → HomeController.home() 실행 → "home" 반환
        → templates/home.html 렌더링

(만약 컨트롤러가 없었다면)
        ↓
2순위: resources/static/index.html 반환
```

**핵심 원칙**: 스프링은 항상 **컨트롤러를 먼저 탐색**하고, 없을 경우에만 정적 파일을 서빙합니다.

### 2.3 home.html 분석

```html
<!DOCTYPE HTML>
<html xmls:th="http://www.thymeleaf.org">
<body>

<div class="container">
    <div>
        <h1>Hello Spring</h1>
        <p>회원 기능</p>
        <p>
            <a href="/members/new">회원 가입</a>
            <a href="/members">회원 목록</a>
        </p>
    </div>
</div>
</body>
</html>
```

| 링크 | URL | HTTP 메서드 | 처리 컨트롤러 |
|------|-----|-------------|--------------|
| 회원 가입 | `/members/new` | GET | `MemberController.createForm()` |
| 회원 목록 | `/members` | GET | `MemberController.list()` |

---

## 3. 회원 등록 - GET (폼 보여주기)

### 3.1 @GetMapping 코드

```java
// 얘는 createMemberForm으로 이동
@GetMapping("/members/new")
public String createForm() {
    return "members/createMemberForm";
}
```

**동작 흐름:**
```
GET /members/new 요청
        ↓
MemberController.createForm() 호출
        ↓
"members/createMemberForm" 문자열 반환
        ↓
ViewResolver가 templates/members/createMemberForm.html 탐색
        ↓
Thymeleaf가 HTML 렌더링 → 브라우저에 반환
```

이 메서드는 단순히 **빈 폼 화면을 보여주는** 역할만 합니다. 어떤 데이터도 처리하지 않습니다.

### 3.2 createMemberForm.html 분석

```html
<!DOCTYPE HTML>
<html xmls:th="http://www.thymeleaf.org">
<body>

<div class="container">
    <form action="/members/new" method="post">
        <div class="form-group">
            <label for="name">이름</label>
            <input type="text" id="name" name="name" placeholder="이름을 입력하세요">
        </div>
        <button type="submit">등록</button>
    </form>
</div>
</body>
</html>
```

**HTML `<form>` 속성 분석:**

| 속성 | 값 | 의미 |
|------|----|------|
| `action` | `/members/new` | 제출할 URL |
| `method` | `post` | HTTP POST 메서드 사용 |

**`<input>` 속성 분석:**

| 속성 | 값 | 역할 |
|------|----|------|
| `type` | `text` | 텍스트 입력 필드 |
| `id` | `name` | `<label for="name">`과 연결 |
| `name` | `name` | **POST 요청 시 파라미터 키값** → MemberForm 필드와 매핑 |
| `placeholder` | `이름을 입력하세요` | 빈 필드 안내 텍스트 |

> ⚠️ **핵심**: `<input name="name">`의 `name` 속성값이 `MemberForm`의 Java 필드명(`private String name`)과 일치해야 자동 바인딩이 됩니다.

---

## 4. 회원 등록 - POST (데이터 처리)

### 4.1 @PostMapping 코드

```java
@PostMapping("/members/new")
public String create(MemberForm form) {
    Member member = new Member();
    member.setName(form.getName());

    memberService.join(member);

    return "redirect:/";
}
```

### 4.2 POST 요청 처리 흐름

```
사용자가 "김철수" 입력 후 [등록] 클릭
        ↓
브라우저가 POST /members/new 요청 전송
HTTP Body: name=김철수
        ↓
스프링이 MemberForm 객체를 자동 생성
MemberForm.setName("김철수") 자동 호출 (바인딩)
        ↓
MemberController.create(form) 실행
        ↓
Member member = new Member()     // 도메인 객체 생성
member.setName(form.getName())   // "김철수" 복사
        ↓
memberService.join(member)       // 비즈니스 로직 실행
→ 중복 체크 → 저장
        ↓
return "redirect:/"              // 홈으로 리다이렉트
```

### 4.3 Form → Domain 변환 패턴

```java
// Form에서 값을 꺼내서 Domain에 넣는 패턴
Member member = new Member();
member.setName(form.getName());  // form.getName() → "김철수"
```

`MemberForm`의 데이터를 `Member` 도메인 객체에 직접 복사합니다. 이렇게 하면 `MemberForm`과 `Member`의 구조가 달라도 유연하게 변환할 수 있습니다.

---

## 5. 폼 데이터 바인딩 (MemberForm)

### 5.1 MemberForm DTO 코드

```java
package hello.hello_spring.controller;

public class MemberForm {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

### 5.2 HTML name 속성 ↔ Java 필드명 매핑 원리

스프링은 POST 요청 시 HTTP Body의 파라미터를 **자바빈(JavaBean) 규칙**에 따라 객체 필드에 자동으로 주입합니다.

```
HTTP 요청 Body: name=김철수
                 ↓
스프링이 MemberForm 인스턴스 생성
                 ↓
파라미터 키 "name" → setName("김철수") 자동 호출
                 ↓
MemberForm.name = "김철수"
```

**매핑 규칙:**

```
HTML: <input name="name" ...>
                 ↕ 이름이 일치해야 함
Java: private String name;
      public void setName(String name) { ... }
```

| HTML `name` 속성 | Java 필드 | setter 메서드 | 결과 |
|------------------|-----------|--------------|------|
| `name` | `private String name` | `setName()` | ✅ 바인딩 성공 |
| `userName` | `private String name` | `setName()` | ❌ 바인딩 실패 |
| `userName` | `private String userName` | `setUserName()` | ✅ 바인딩 성공 |

### 5.3 왜 Member 도메인을 직접 쓰지 않고 별도 Form 객체를 사용하는가?

```java
// ❌ 안티패턴: 도메인 객체를 폼 파라미터로 직접 사용
@PostMapping("/members/new")
public String create(Member member) {  // Member를 직접 받으면?
    memberService.join(member);
    return "redirect:/";
}
```

언뜻 보면 편리해 보이지만, **실무에서는 여러 문제**가 생깁니다:

| 문제 | 설명 |
|------|------|
| **필드 불일치** | 폼에는 `confirmPassword` 같은 필드가 있지만 도메인에는 없음 |
| **검증 로직 혼재** | 폼 검증(공백 체크, 길이 제한)이 도메인에 섞임 |
| **보안 취약** | 도메인의 민감한 필드(id, role 등)가 외부에 노출될 수 있음 |
| **관심사 분리 위반** | 도메인 객체는 비즈니스 로직에 집중해야 함 |

```
✅ 권장 패턴:
HTML Form → MemberForm (DTO) → Member (Domain)
  웹 계층         변환 담당         비즈니스 계층
```

---

## 6. Post-Redirect-Get 패턴

### 6.1 redirect:/ 의 의미

```java
return "redirect:/";
```

이 반환값은 **Thymeleaf 뷰 이름이 아닙니다**. 스프링이 `redirect:` 접두사를 인식하고 HTTP 302 응답을 보냅니다.

```
스프링 → 브라우저: HTTP/1.1 302 Found
                   Location: /

브라우저 → 스프링: GET / (자동 재요청)
                   ↓
           HomeController.home() 실행
           → home.html 반환
```

### 6.2 redirect가 없을 때의 문제 (중복 submit)

```
[redirect 없는 경우 - 위험]

1. 사용자: POST /members/new (이름: 김철수) → 회원 등록됨
2. 서버: "members/memberList" 뷰 반환 (URL은 여전히 /members/new)
3. 사용자: F5(새로고침) 클릭
4. 브라우저: "POST /members/new 요청을 다시 보내시겠습니까?" 경고
5. 사용자: 확인 클릭 → 김철수가 또 등록됨! 😱
   → 새로고침할 때마다 중복 데이터 발생
```

### 6.3 redirect가 있을 때 (안전)

```
[PRG 패턴 - 안전]

1. 사용자: POST /members/new (이름: 김철수) → 회원 등록됨
2. 서버: HTTP 302 redirect → "/"
3. 브라우저: GET / 자동 요청 → 홈 화면 표시
   (현재 URL: /)
4. 사용자: F5(새로고침) 클릭
5. 브라우저: GET / 재요청 → 홈 화면 다시 표시
   → 중복 등록 없음! ✅
```

### 6.4 PRG 패턴 비교 다이어그램

```
[Without PRG - 위험]                    [With PRG - 안전]

POST /members/new                        POST /members/new
       ↓                                        ↓
  회원 등록 처리                           회원 등록 처리
       ↓                                        ↓
 뷰 직접 반환                           302 redirect → /
(URL: /members/new)                     (URL: /)
       ↓                                        ↓
  F5 새로고침                             GET /
       ↓                                        ↓
 POST 재전송 경고                       HomeController.home()
       ↓                                        ↓
  중복 등록 발생! ❌                     홈 화면 표시 ✅
```

---

## 7. 회원 목록 조회

### 7.1 @GetMapping("/members") 코드

```java
@GetMapping("/members")
public String list(Model model) {
    List<Member> members = memberService.findMembers();
    model.addAttribute("members", members);
    return "members/memberList";
}
```

### 7.2 Model.addAttribute() 패턴

`Model`은 스프링이 컨트롤러에 자동으로 주입해주는 객체입니다. 이 객체에 데이터를 담으면 View(Thymeleaf)에서 사용할 수 있습니다.

```
memberService.findMembers()  →  [Member(id=1, name=김철수), Member(id=2, name=이영희)]
                                                    ↓
                              model.addAttribute("members", members)
                                                    ↓
                              Thymeleaf에서 ${members}로 접근 가능
```

| `addAttribute()` 인자 | 설명 |
|----------------------|------|
| `"members"` (키) | Thymeleaf에서 `${members}`로 접근할 때 사용하는 이름 |
| `members` (값) | 실제 `List<Member>` 객체 |

### 7.3 memberList.html 분석

```html
<!DOCTYPE HTML>
<html xmls:th="http://www.thymeleaf.org">
<body>

<div class="container">
    <div>
        <table>
            <thead>
            <tr>
                <th>#</th>
                <th>이름</th>
            </tr>
            </thead>
            <tbody>
            <tr th:each="member : ${members}">
                <td th:text="${member.id}"></td>
                <td th:text="${member.name}"></td>
            </tr>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
```

### 7.4 Thymeleaf 표현식 설명

**`th:each` - 반복 렌더링:**

```html
<tr th:each="member : ${members}">
```

- `${members}`: Model에서 `"members"` 키로 꺼낸 `List<Member>` 객체
- `member`: 반복 변수 (각 반복에서 `Member` 객체 하나를 의미)
- Java의 `for (Member member : members)`와 동일한 개념

**`th:text` - 텍스트 출력:**

```html
<td th:text="${member.id}"></td>
<td th:text="${member.name}"></td>
```

- `${member.id}`: `member.getId()` 자동 호출
- `${member.name}`: `member.getName()` 자동 호출
- Thymeleaf는 **getter 메서드를 자동으로 찾아서** 호출합니다

**렌더링 결과 예시:**

회원이 2명(김철수, 이영희) 등록된 경우:

```html
<tbody>
  <tr>
    <td>1</td>
    <td>김철수</td>
  </tr>
  <tr>
    <td>2</td>
    <td>이영희</td>
  </tr>
</tbody>
```

---

## 8. Best Practice 및 안티패턴

### 8.1 GET/POST 명확한 분리

**✅ 권장: 같은 URL, 다른 메서드**

```java
// 폼 화면을 보여줄 때 → GET
@GetMapping("/members/new")
public String createForm() {
    return "members/createMemberForm";
}

// 데이터를 처리할 때 → POST
@PostMapping("/members/new")
public String create(MemberForm form) {
    ...
    return "redirect:/";
}
```

**❌ 안티패턴: GET으로 모든 것 처리**

```java
// 회원 등록을 GET으로 처리 - 절대 금지!
@GetMapping("/members/new")
public String create(String name) {
    // GET 요청으로 데이터를 변경하면:
    // 1. URL에 데이터가 노출됨 (name=김철수)
    // 2. 브라우저 캐시, 북마크 등에 데이터가 저장됨
    // 3. CSRF 공격에 취약
    Member member = new Member();
    member.setName(name);
    memberService.join(member);
    return "redirect:/";
}
```

### 8.2 Form 객체 vs 도메인 객체

**✅ 권장: 별도 Form 객체 사용**

```java
// Form 객체: 웹 계층에서만 사용
public class MemberForm {
    private String name;  // 화면에 필요한 필드만
    // 필요시: 검증 어노테이션, 확인용 필드 등 추가 가능
}

// 도메인 객체: 비즈니스 로직에 집중
public class Member {
    private Long id;      // DB에서 관리하는 식별자
    private String name;  // 비즈니스 핵심 데이터
}
```

**❌ 안티패턴: 도메인 객체를 폼으로 직접 사용**

```java
@PostMapping("/members/new")
public String create(Member member) {
    // Member의 id 필드까지 외부에서 주입 가능 → 보안 취약
    memberService.join(member);
    return "redirect:/";
}
```

### 8.3 redirect 필수 사용

**✅ 권장: POST 후 redirect**

```java
@PostMapping("/members/new")
public String create(MemberForm form) {
    ...
    return "redirect:/";  // PRG 패턴 적용 ✅
}
```

**❌ 안티패턴: POST 후 뷰 직접 반환**

```java
@PostMapping("/members/new")
public String create(MemberForm form) {
    ...
    return "members/memberList";  // 새로고침 시 중복 등록 위험 ❌
}
```

### 8.4 종합 정리표

| 상황 | ✅ 권장 | ❌ 안티패턴 |
|------|---------|------------|
| 화면 조회 | `@GetMapping` 사용 | `@PostMapping`으로 조회 |
| 데이터 저장 | `@PostMapping` 사용 | `@GetMapping`으로 저장 |
| POST 처리 후 | `redirect:` 반환 | 뷰 이름 직접 반환 |
| 폼 파라미터 | `MemberForm` DTO 사용 | `Member` 도메인 직접 사용 |
| 목록 전달 | `model.addAttribute()` | 정적 변수나 전역 공유 |

---

## 9. 부록

### 9.1 프로젝트 구조

```
hello-spring/
└── src/
    └── main/
        ├── java/
        │   └── hello/
        │       └── hello_spring/
        │           ├── HelloSpringApplication.java
        │           ├── controller/                  ← 이번 강의 추가
        │           │   ├── HomeController.java      ← 홈 화면 컨트롤러
        │           │   ├── MemberController.java    ← 회원 등록/조회 컨트롤러
        │           │   └── MemberForm.java          ← 폼 데이터 바인딩 DTO
        │           ├── domain/
        │           │   └── Member.java
        │           ├── repository/
        │           │   ├── MemberRepository.java
        │           │   └── MemoryMemberRepository.java
        │           └── service/
        │               └── MemberService.java
        └── resources/
            └── templates/
                ├── home.html                        ← 홈 화면 (이번 강의 추가)
                └── members/                         ← 이번 강의 추가
                    ├── createMemberForm.html        ← 회원 등록 폼
                    └── memberList.html              ← 회원 목록
```

### 9.2 핵심 용어 정리

| 용어 | 설명 |
|------|------|
| **GET** | 데이터 조회를 위한 HTTP 메서드. 서버 상태를 변경하지 않음 |
| **POST** | 데이터 제출을 위한 HTTP 메서드. 서버 상태를 변경함 |
| **PRG** | Post-Redirect-Get. POST 처리 후 GET으로 리다이렉트하는 패턴 |
| **MVC** | Model-View-Controller. 관심사를 세 계층으로 분리하는 패턴 |
| **DTO** | Data Transfer Object. 계층 간 데이터 전달을 위한 객체 |
| **Form 객체** | 웹 폼 데이터를 담는 DTO. 도메인과 분리하여 사용 |
| **th:each** | Thymeleaf의 반복 렌더링 문법 (`for-each`와 동일) |
| **th:text** | Thymeleaf의 텍스트 출력 문법. HTML 이스케이프 처리 포함 |
| **Model** | Controller → View 데이터 전달 객체. `addAttribute()`로 데이터 추가 |
| **ViewResolver** | 뷰 이름을 실제 템플릿 파일 경로로 변환하는 스프링 컴포넌트 |
| **리다이렉트** | 서버가 클라이언트에게 다른 URL로 재요청하도록 지시 (302 응답) |

### 9.3 어노테이션 정리

| 어노테이션 | 위치 | 역할 |
|-----------|------|------|
| `@Controller` | 클래스 | 스프링 MVC 컨트롤러로 등록 |
| `@GetMapping("/경로")` | 메서드 | HTTP GET 요청을 해당 메서드에 매핑 |
| `@PostMapping("/경로")` | 메서드 | HTTP POST 요청을 해당 메서드에 매핑 |
| `@Autowired` | 생성자 | 스프링 컨테이너에서 빈 자동 주입 |
| `@RequestParam` | 파라미터 | URL 쿼리 파라미터 또는 폼 데이터를 직접 받을 때 |

> **참고**: `@PostMapping`의 파라미터로 `MemberForm form`처럼 객체를 선언하면, `@ModelAttribute`가 자동으로 적용되어 폼 데이터가 객체에 바인딩됩니다.

### 9.4 학습 점검

다음 질문에 답하며 학습 내용을 확인해보세요:

1. **HomeController를 추가했더니 `static/index.html`이 더 이상 보이지 않는 이유는?**
   - 스프링은 컨트롤러를 먼저 탐색하고, 없을 때만 정적 파일을 서빙하기 때문

2. **같은 URL `/members/new`에 `@GetMapping`과 `@PostMapping`을 모두 사용할 수 있는 이유는?**
   - HTTP 메서드(GET/POST)가 다르기 때문에 별도의 메서드로 매핑 가능

3. **`<input name="name">`의 `name` 속성이 Java 필드명 `name`과 일치해야 하는 이유는?**
   - 스프링이 JavaBean 규칙에 따라 setter(`setName()`)를 찾아 자동 바인딩하기 때문

4. **POST 처리 후 `return "redirect:/"`를 사용하는 이유는?**
   - PRG 패턴으로 새로고침 시 중복 submit을 방지하기 위해

5. **`Model.addAttribute("members", members)`에서 첫 번째 인자 `"members"`의 역할은?**
   - Thymeleaf에서 `${members}`로 데이터에 접근할 때 사용하는 키값

6. **`th:each="member : ${members}"`에서 `${members}`는 어디서 오는가?**
   - 컨트롤러의 `model.addAttribute("members", ...)` 에서 담은 데이터

### 9.5 다음 학습 단계

이번 강의에서 구현한 회원 목록은 **메모리에 저장**됩니다. 서버를 재시작하면 모든 데이터가 사라집니다.

```
현재 상태:
MemberController → MemberService → MemoryMemberRepository (메모리)
                                        ↑
                                   서버 재시작 시 초기화 ⚠️

다음 목표: 실제 데이터베이스에 영구 저장
MemberController → MemberService → JdbcMemberRepository (H2 DB)
                                 → JpaMemberRepository (JPA/Hibernate)
                                 → SpringDataJpaMemberRepository (Spring Data JPA)
```

다음 단계에서 배울 내용:
- **스프링 DB 접근 기술**: 순수 JDBC → JdbcTemplate → JPA → Spring Data JPA
- **스프링 통합 테스트**: `@SpringBootTest`로 실제 DB와 연동한 테스트
- **AOP**: 공통 관심사(로깅, 트랜잭션)를 분리하는 기법

---

*작성일: 2026-02-21*
*강의: 인프런 - 김영한의 스프링 입문*
