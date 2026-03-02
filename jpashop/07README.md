# 웹 계층 개발

> Thymeleaf 뷰 템플릿으로 회원·상품·주문 UI를 완성하고, 변경 감지(Dirty Checking) vs 병합(Merge)의 차이를 실전 코드로 이해하기

## 📚 강의 출처

**인프런 - 김영한의 "실전! 스프링 부트와 JPA 활용 1 - 웹 애플리케이션 개발"**

이 문서는 강의의 챕터 7 "웹 계층 개발"을 학습하며 작성한 노트입니다.
챕터 4~6까지 구축한 회원(Member)·상품(Item)·주문(Order) 도메인 위에
**Thymeleaf 기반 웹 UI**를 완성하는 최종 챕터입니다.
이 챕터의 핵심은 **변경 감지(Dirty Checking) vs 병합(merge)** 이며,
엔티티를 화면에 직접 노출하지 않고 **폼 객체(DTO)** 를 사용하는 이유도 함께 다룹니다.

| 챕터 | 내용 |
|------|------|
| 챕터 6 (이전) | 주문 도메인 개발 — CascadeType.ALL, 생성 메서드, Dirty Checking, 동적 쿼리 |
| **챕터 7 (현재)** | **웹 계층 개발 — Thymeleaf 뷰, 폼 객체, @Valid, 변경 감지 vs 병합** |

---

## 🎯 학습 목표

이 문서를 통해 다음을 이해하고 실습합니다:

1. **Thymeleaf fragments** — `th:replace`로 공통 레이아웃(헤더·푸터)을 재사용하는 방법
2. **폼 객체(Form DTO) 분리** — 엔티티를 웹 계층에 직접 노출하지 않아야 하는 이유와 패턴
3. **`@Valid` + `BindingResult`** — Spring MVC의 Bean Validation 연동과 에러 처리 방법
4. **준영속 엔티티와 병합(merge)** — `em.merge()`의 동작 원리와 **모든 필드 null 덮어쓰기** 위험성
5. **변경 감지(Dirty Checking) 수정 패턴** — 영속 상태 엔티티를 조회한 뒤 setter로 변경하는 올바른 수정 방법
6. **컨트롤러에서 식별자만 넘기기 패턴** — 서비스 계층에 엔티티 대신 ID만 전달해야 하는 이유

---

## 🗺️ 학습 로드맵

챕터 7은 **UI 구성 → 회원 → 상품 → 상품 수정(핵심) → 주문** 순으로 진행됩니다.

```
1단계: 홈 화면과 레이아웃
   - Thymeleaf fragments (header / bodyHeader / footer)
   - Bootstrap jumbotron 레이아웃
   - th:replace 문법으로 공통 레이아웃 재사용
   ↓
2단계: 회원 등록 / 목록 조회
   - MemberForm DTO (엔티티 대신 폼 객체 사용)
   - @Valid + BindingResult (유효성 검증 + 에러 메시지)
   - th:each (목록 반복 렌더링)
   ↓
3단계: 상품 등록 / 목록 / 수정
   - BookForm DTO
   - updateItemForm (GET): 기존 데이터 폼에 채우기
   - updateItem (POST): 준영속 엔티티 처리 ← 핵심 문제 발생
   - merge vs 변경 감지 — 왜 변경 감지를 써야 하는가
   ↓
4단계: 상품 주문 / 목록 검색 / 취소
   - OrderController (식별자만 서비스로 전달)
   - @ModelAttribute로 검색 폼 바인딩
   - T() 연산자로 Enum select 동적 생성
   - JavaScript DOM 조작으로 POST 취소 구현
```

**왜 이 순서인가?**
- **레이아웃 우선**: 공통 fragments를 먼저 만들어야 모든 페이지가 동일한 레이아웃을 재사용할 수 있습니다.
- **상품 수정이 핵심**: 이 챕터에서 가장 중요한 개념인 변경 감지 vs 병합은 상품 수정 기능에서 등장합니다.

---

## 📖 목차

1. [홈 화면과 레이아웃](#1-홈-화면과-레이아웃)
   - 1.1 Thymeleaf fragments
   - 1.2 th:replace 문법
   - 1.3 홈 화면 구성
2. [회원 등록](#2-회원-등록)
   - 2.1 왜 엔티티 대신 폼 객체를 사용하는가
   - 2.2 MemberForm DTO
   - 2.3 @Valid + BindingResult 검증
   - 2.4 createMemberForm.html Thymeleaf 바인딩
   - 2.5 전체 코드
3. [회원 목록 조회](#3-회원-목록-조회)
   - 3.1 th:each 반복 렌더링
   - 3.2 엔티티를 뷰에 직접 넘기는 것의 한계
4. [상품 등록](#4-상품-등록)
   - 4.1 BookForm DTO
   - 4.2 ItemController.create()
5. [상품 목록 조회](#5-상품-목록-조회)
6. [상품 수정 — 변경 감지 vs 병합](#6-상품-수정--변경-감지-vs-병합)
   - 6.1 준영속(Detached) 엔티티란
   - 6.2 병합(merge) 동작 원리와 위험성
   - 6.3 변경 감지(Dirty Checking) 수정 패턴
   - 6.4 컨트롤러에서 어설프게 엔티티 생성하지 말 것
   - 6.5 권장 패턴: ID + 필드값만 서비스로 전달
   - 6.6 비교 정리
7. [상품 주문](#7-상품-주문)
   - 7.1 주문 폼
   - 7.2 컨트롤러에서 식별자만 전달하는 이유
8. [주문 목록 검색 및 취소](#8-주문-목록-검색-및-취소)
   - 8.1 @ModelAttribute로 검색 폼 자동 바인딩
   - 8.2 Enum select 동적 생성 (T() 연산자)
   - 8.3 주문 취소 — JavaScript POST 폼
9. [Best Practice 및 주의사항](#9-best-practice-및-주의사항)
10. [부록](#10-부록)

---

## 1. 홈 화면과 레이아웃

### 1.1 Thymeleaf fragments

**fragments**는 Thymeleaf의 레이아웃 재사용 메커니즘입니다.
공통 HTML 조각(헤더, 푸터 등)을 별도 파일로 분리하고, 각 페이지에서 `th:replace`로 삽입합니다.

```
templates/
├── fragments/
│   ├── header.html       ← <head> 영역 (Bootstrap CSS, 공통 스타일)
│   ├── bodyHeader.html   ← 내비게이션 바
│   └── footer.html       ← 하단 영역
├── home.html
├── members/
│   ├── createMemberForm.html
│   └── memberList.html
├── items/
│   ├── createItemForm.html
│   ├── itemList.html
│   └── updateItemForm.html
└── order/
    ├── orderForm.html
    └── orderList.html
```

**header.html — fragment 정의:**

```html
<!-- fragments/header.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:fragment="header">              <!-- ← 이 이름으로 참조됨 -->
    <meta charset="utf-8">
    <link rel="stylesheet" href="/css/bootstrap.min.css" ...>
    <link href="/css/jumbotron-narrow.css" rel="stylesheet">
    <title>Hello, world!</title>
</head>
```

---

### 1.2 th:replace 문법

```html
<!-- 사용하는 페이지에서 -->
<head th:replace="~{fragments/header :: header}">
    <title>Hello</title>  <!-- ← 이 내용은 fragment로 완전히 대체됨 -->
</head>
```

| 문법 | 설명 |
|------|------|
| `th:replace="~{파일경로 :: fragment이름}"` | 현재 태그 전체를 fragment로 교체 |
| `th:insert="~{파일경로 :: fragment이름}"` | 현재 태그 내부에 fragment를 삽입 (태그는 유지) |

> `th:replace`가 더 일반적으로 사용됩니다. 현재 태그(예: `<head>`)를 fragment의 태그로 완전히 대체하기 때문에, HTML 구조가 깔끔하게 유지됩니다.

---

### 1.3 홈 화면 구성

```html
<!-- home.html -->
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/header :: header}">
    <title>Hello</title>
</head>
<body>
<div class="container">
    <div th:replace="~{fragments/bodyHeader :: bodyHeader}" />

    <div class="jumbotron">
        <h1>HELLO SHOP</h1>
        <p class="lead">회원 기능</p>
        <p>
            <a class="btn btn-lg btn-secondary" href="/members/new">회원 가입</a>
            <a class="btn btn-lg btn-secondary" href="/members">회원 목록</a>
        </p>
        <p class="lead">상품 기능</p>
        <p>
            <a class="btn btn-lg btn-dark" href="/items/new">상품 등록</a>
            <a class="btn btn-lg btn-dark" href="/items">상품 목록</a>
        </p>
        <p class="lead">주문 기능</p>
        <p>
            <a class="btn btn-lg btn-info" href="/order">상품 주문</a>
            <a class="btn btn-lg btn-info" href="/orders">주문 내역</a>
        </p>
    </div>

    <div th:replace="~{fragments/footer :: footer}" />
</div>
</body>
</html>
```

```java
// HomeController.java
@Controller
@Slf4j
public class HomeController {

    @RequestMapping("/")
    public String home() {
        log.info("home controller");
        return "home";       // → templates/home.html 렌더링
    }
}
```

---

## 2. 회원 등록

### 2.1 왜 엔티티 대신 폼 객체를 사용하는가

웹 계층에서 엔티티를 그대로 폼 데이터로 사용하지 않는 이유:

| 문제 | 설명 |
|------|------|
| **Validation 불일치** | 화면에서 요구하는 검증 규칙과 도메인이 요구하는 검증 규칙이 다를 수 있음 |
| **엔티티 오염** | 엔티티에 `@NotEmpty` 같은 화면 전용 어노테이션이 누적되어 지저분해짐 |
| **API 스펙 노출** | 엔티티를 그대로 반환하면 내부 구조가 외부에 노출되고, 엔티티 변경 시 API 스펙이 바뀜 |
| **설계 불일치** | 실무에서 폼 화면의 필드 구성은 엔티티의 필드 구성과 다를 때가 대부분 |

```
권장 흐름:
화면 폼 → [Form DTO] → Controller → (변환) → Service/Domain Entity
                              ↑
                    여기서 Form → Entity 변환
```

> **실무 원칙**: 엔티티는 핵심 비즈니스 로직만 가지고 있어야 합니다.
> 화면을 위한 로직은 폼 객체나 DTO를 사용하세요.
> API를 만들 때는 절대 엔티티를 웹으로 반환하면 안 됩니다.

---

### 2.2 MemberForm DTO

```java
// controller/MemberForm.java
package jpabook.jpashop.controller;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MemberForm {

    @NotEmpty(message = "회원 이름은 필수 입니다.")  // ← 화면 전용 검증
    private String name;

    private String city;
    private String street;
    private String zipcode;
}
```

**Member 엔티티 vs MemberForm 비교:**

| | `Member` 엔티티 | `MemberForm` DTO |
|--|----------------|-----------------|
| 역할 | JPA 영속성 관리, 비즈니스 로직 보유 | 화면의 입력 데이터를 받는 전용 객체 |
| 어노테이션 | `@Entity`, `@Id`, `@Column` 등 JPA 어노테이션 | `@NotEmpty` 등 Bean Validation |
| 사용 계층 | 서비스, 리포지토리 | 컨트롤러(웹 계층) |

---

### 2.3 @Valid + BindingResult 검증

```java
// MemberController.java

@PostMapping("/members/new")
public String create(@Valid MemberForm form, BindingResult result) {
    //               ↑ Bean Validation 실행    ↑ 오류 정보 담기는 객체

    if (result.hasErrors()) {
        return "members/createMemberForm";  // 오류 시 폼 화면으로 다시 이동
    }

    Address address = new Address(form.getCity(), form.getStreet(), form.getZipcode());

    Member member = new Member();
    member.setName(form.getName());
    member.setAddress(address);

    memberService.join(member);
    return "redirect:/";   // PRG 패턴: 성공 시 리다이렉트
}
```

**핵심 포인트:**

| 요소 | 역할 |
|------|------|
| `@Valid` | 파라미터 앞에 붙이면 해당 객체의 Bean Validation 어노테이션(`@NotEmpty` 등) 실행 |
| `BindingResult` | `@Valid` 뒤에 선언하면, 오류가 있어도 메서드가 실행됨 (없으면 예외 발생) |
| `result.hasErrors()` | 오류가 있으면 `true` 반환 — 폼 화면으로 다시 이동 |
| `redirect:/` | PRG 패턴 — 성공 시 리다이렉트로 중복 제출 방지 |

**BindingResult가 없을 때 vs 있을 때:**

```java
// ❌ BindingResult 없이
@PostMapping("/members/new")
public String create(@Valid MemberForm form) {
    // @Valid 오류 발생 시 → BindException 예외 → 400 Bad Request 페이지
    ...
}

// ✅ BindingResult 있이
@PostMapping("/members/new")
public String create(@Valid MemberForm form, BindingResult result) {
    // @Valid 오류 발생 시 → result에 오류 정보 담김 → 메서드 실행 계속
    if (result.hasErrors()) {
        return "members/createMemberForm";  // 폼 화면에서 오류 메시지 표시
    }
    ...
}
```

---

### 2.4 createMemberForm.html Thymeleaf 바인딩

```html
<!-- members/createMemberForm.html -->
<form role="form" action="/members/new" th:object="${memberForm}" method="post">
    <!-- th:object: 폼 전체에 바인딩할 객체 지정 -->

    <div class="form-group">
        <label th:for="name">이름</label>
        <input type="text"
               th:field="*{name}"
               class="form-control"
               placeholder="이름을 입력하세요"
               th:class="${#fields.hasErrors('name')}? 'form-control fieldError' : 'form-control'">
        <!-- 오류 있으면 fieldError 클래스 추가 → 빨간 테두리 -->

        <p th:if="${#fields.hasErrors('name')}"
           th:errors="*{name}">에러 메시지</p>
        <!-- @NotEmpty의 message 출력: "회원 이름은 필수 입니다." -->
    </div>

    <div class="form-group">
        <label th:for="city">도시</label>
        <input type="text" th:field="*{city}" class="form-control" placeholder="도시를 입력하세요">
    </div>

    <div class="form-group">
        <label th:for="street">거리</label>
        <input type="text" th:field="*{street}" class="form-control" placeholder="거리를 입력하세요">
    </div>

    <div class="form-group">
        <label th:for="zipcode">우편번호</label>
        <input type="text" th:field="*{zipcode}" class="form-control" placeholder="우편번호를 입력하세요">
    </div>

    <button type="submit" class="btn btn-primary">Submit</button>
</form>
```

**Thymeleaf 폼 바인딩 문법 정리:**

| 문법 | 설명 | 예시 |
|------|------|------|
| `th:object="${객체}"` | 폼에 바인딩할 모델 객체 지정 | `th:object="${memberForm}"` |
| `*{필드명}` | th:object로 지정한 객체의 필드 참조 (선택 표현식) | `*{name}` → `memberForm.name` |
| `th:field="*{필드명}"` | `id`, `name`, `value` 속성을 자동 생성 | `th:field="*{name}"` |
| `#fields.hasErrors('필드명')` | 특정 필드에 오류가 있는지 확인 | `${#fields.hasErrors('name')}` |
| `th:errors="*{필드명}"` | 해당 필드의 오류 메시지 출력 | `th:errors="*{name}"` |

---

### 2.5 전체 코드

```java
// controller/MemberController.java
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/members/new")
    public String createForm(Model model) {
        model.addAttribute("memberForm", new MemberForm());  // 빈 폼 객체 전달
        return "members/createMemberForm";
    }

    @PostMapping("/members/new")
    public String create(@Valid MemberForm form, BindingResult result) {
        if (result.hasErrors()) {
            return "members/createMemberForm";
        }

        Address address = new Address(form.getCity(), form.getStreet(), form.getZipcode());

        Member member = new Member();
        member.setName(form.getName());
        member.setAddress(address);

        memberService.join(member);
        return "redirect:/";
    }

    @GetMapping("/members")
    public String list(Model model) {
        List<Member> members = memberService.findMembers();
        model.addAttribute("members", members);
        return "members/memberList";
    }
}
```

**GET /members/new에서 빈 폼 객체를 전달하는 이유:**

```java
model.addAttribute("memberForm", new MemberForm());
```

Thymeleaf의 `th:object="${memberForm}"`이 모델에서 객체를 찾기 때문에,
비어있더라도 반드시 모델에 객체를 넣어야 합니다.
없으면 렌더링 시 `NullPointerException`이 발생합니다.

---

## 3. 회원 목록 조회

### 3.1 th:each 반복 렌더링

```html
<!-- members/memberList.html (일부) -->
<tr th:each="member : ${members}">
    <td th:text="${member.id}"></td>
    <td th:text="${member.name}"></td>
    <td th:text="${member.address?.city}"></td>
    <td th:text="${member.address?.street}"></td>
    <td th:text="${member.address?.zipcode}"></td>
</tr>
```

| 문법 | 설명 |
|------|------|
| `th:each="변수 : ${컬렉션}"` | 컬렉션을 반복하여 각 요소를 변수에 바인딩 |
| `th:text="${변수.필드}"` | 텍스트 출력 (HTML 이스케이프 처리됨) |
| `${변수.필드?.중첩필드}` | null-safe 접근 (`?.` 연산자 — address가 null이면 null 반환) |

---

### 3.2 엔티티를 뷰에 직접 넘기는 것의 한계

```java
// MemberController.java
@GetMapping("/members")
public String list(Model model) {
    List<Member> members = memberService.findMembers();
    model.addAttribute("members", members);  // ← 엔티티를 직접 전달 (예제에서는 허용)
    return "members/memberList";
}
```

> **강의의 코멘트**: 가장 권장하는 것은 템플릿 엔진에서조차 멤버 DTO나 화면에 맞는 DTO로 변환하여 반환하는 것이 제일 깔끔합니다.
> 예제이므로 단순화하기 위해 엔티티를 직접 전달했지만, **실무에서는 엔티티 대신 DTO를 사용해야 합니다.**

**실무에서 엔티티 직접 노출의 문제점:**

| 문제 | 설명 |
|------|------|
| 보안 취약 | 내부 필드(비밀번호 등)가 실수로 노출될 수 있음 |
| API 스펙 불안정 | 엔티티 변경 시 API 응답 스펙이 함께 바뀜 |
| 양방향 연관관계 | JSON 직렬화 시 무한 루프 발생 위험 |

---

## 4. 상품 등록

### 4.1 BookForm DTO

```java
// controller/BookForm.java
@Getter @Setter
public class BookForm {

    private Long id;           // 수정 시 필요한 식별자

    // 상품 공통 속성
    private String name;
    private int price;
    private int stockQuantity;

    // 책 전용 속성
    private String author;
    private String isbn;
}
```

`id` 필드를 포함하는 이유: 동일한 폼을 **등록(id=null)**과 **수정(id=값)** 모두에 재사용합니다.

---

### 4.2 ItemController.create()

```java
// controller/ItemController.java
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
        Book book = new Book();
        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());

        itemService.saveItem(book);
        return "redirect:/";
    }
    ...
}
```

> **강의 코멘트**: 예제에서는 setter를 사용했지만, 실제로는 `Book.createBook(name, price, ...)` 같은 정적 팩토리 메서드를 만드는 것이 더 나은 설계입니다.

---

## 5. 상품 목록 조회

```java
@GetMapping("/items")
public String list(Model model) {
    List<Item> items = itemService.findItems();
    model.addAttribute("items", items);
    return "items/itemList";
}
```

```html
<!-- items/itemList.html (일부) -->
<tr th:each="item : ${items}">
    <td th:text="${item.id}"></td>
    <td th:text="${item.name}"></td>
    <td th:text="${item.price}"></td>
    <td th:text="${item.stockQuantity}"></td>
    <td>
        <a href="#" th:href="@{/items/{id}/edit (id=${item.id})}"
           class="btn btn-primary" role="button">수정</a>
    </td>
</tr>
```

**`@{/items/{id}/edit (id=${item.id})}` 문법:**

Thymeleaf의 링크 URL 표현식으로 경로 변수를 동적으로 생성합니다.
`item.id`가 1이면 → `/items/1/edit` URL이 생성됩니다.

---

## 6. 상품 수정 — 변경 감지 vs 병합

이 섹션이 **챕터 7의 가장 중요한 핵심 내용**입니다.

### 6.1 준영속(Detached) 엔티티란

```
영속 상태  : JPA 영속성 컨텍스트가 관리하는 엔티티 (Dirty Checking 대상)
비영속 상태: 처음 생성된, 아직 persist하지 않은 엔티티
준영속 상태: 한번 영속 상태였다가, 영속성 컨텍스트에서 분리된 엔티티
```

**상품 수정 시나리오에서 준영속 엔티티가 발생하는 과정:**

```
1. GET /items/1/edit 요청
   → DB에서 Item(id=1) 조회 (영속 상태)
   → BookForm에 값 채워서 뷰로 전달
   → 트랜잭션 종료 → 엔티티가 준영속 상태로 전환

2. 사용자가 폼 수정 후 POST /items/1/edit 요청
   → 컨트롤러에서 BookForm 수신
   → BookForm의 id(=1)를 가진 새 Book 객체 생성
       Book book = new Book();
       book.setId(form.getId()); ← id=1 설정
   → 이 book 객체는 "id는 있지만 영속성 컨텍스트가 모르는" 준영속 엔티티
```

**준영속 엔티티의 특징:**
- DB에 이미 존재하는 데이터를 가리키는 id를 보유
- **JPA 영속성 컨텍스트가 관리하지 않음** → Dirty Checking 불가
- `em.persist()`를 호출해도 아무 일도 일어나지 않음 (이미 있는 데이터라 INSERT 발생 안 함)

---

### 6.2 병합(merge) 동작 원리와 위험성

병합은 준영속 엔티티를 영속 상태로 만들어 업데이트하는 방법입니다.

```java
// ItemRepository.java에서 merge 방식
public void save(Item item) {
    if (item.getId() == null) {
        em.persist(item);   // 새 엔티티 → INSERT
    } else {
        em.merge(item);     // 준영속 엔티티 → UPDATE (merge 방식)
    }
}
```

**`em.merge()` 내부 동작:**

```
1. merge(item) 호출
   ↓
2. item.id로 DB에서 기존 엔티티 조회 (또는 영속성 컨텍스트에서 찾기)
   ↓
3. 조회한 영속 엔티티의 모든 필드를 merge 대상 엔티티(item)의 값으로 덮어씀
   ↓
4. 덮어쓴 영속 엔티티 반환
```

**merge의 치명적인 문제 — 모든 필드 null 덮어쓰기:**

```java
// ❌ 위험한 merge 사용 예
Book book = new Book();
book.setId(form.getId());
book.setName(form.getName());
book.setPrice(form.getPrice());
// book.setStockQuantity(form.getStockQuantity());  ← 이 줄을 빠뜨렸다면?
// → stockQuantity가 null(0)로 DB에 UPDATE됨!

itemService.saveItem(book);  // 내부에서 em.merge(book) 호출
```

```
merge 동작:
기존 DB:  name="JPA책", price=10000, stockQuantity=100, author="김영한"
merge 값: name="JPA책", price=15000, stockQuantity=0,   author=null
결과 DB:  name="JPA책", price=15000, stockQuantity=0,   author=null
                                               ↑ 재고 0으로 초기화!   ↑ 저자 사라짐!
```

> merge는 변경하지 않을 필드도 모두 넘겨야 합니다.
> 실무에서는 수정할 필드만 선택적으로 변경하는 경우가 많아,
> merge를 올바르게 사용하기 매우 어렵습니다.

---

### 6.3 변경 감지(Dirty Checking) 수정 패턴

**올바른 수정 방법 — `ItemService.updateItem()`:**

```java
// service/ItemService.java

@Transactional
public void updateItem(Long itemId, String name, int price, int stockQuantity) {
    // 1. 영속성 컨텍스트에서 영속 상태 엔티티 조회
    Item findItem = itemRepository.findOne(itemId);

    // 2. 필요한 필드만 set
    findItem.setName(name);
    findItem.setPrice(price);
    findItem.setStockQuantity(stockQuantity);

    // 3. 메서드 종료 → @Transactional에 의해 트랜잭션 커밋
    //    → JPA Dirty Checking 수행 → 변경된 필드만 UPDATE SQL 자동 실행
}
```

**변경 감지 수정의 동작 흐름:**

```
@Transactional 시작
    ↓
itemRepository.findOne(itemId) → 영속성 컨텍스트에 엔티티 + 스냅샷 저장
    ↓
findItem.setName(name) / setPrice(price) / setStockQuantity(stockQuantity)
    ↓
메서드 반환 → 트랜잭션 커밋
    ↓
JPA: 영속성 컨텍스트의 모든 엔티티를 스냅샷과 비교 (Dirty Checking)
    ↓
변경된 필드: name, price, stockQuantity
    ↓
UPDATE item SET name=?, price=?, stock_quantity=? WHERE item_id=?
(변경된 필드만 — author, isbn 등 변경 안 된 필드는 그대로)
```

---

### 6.4 컨트롤러에서 어설프게 엔티티 생성하지 말 것

```java
// ❌ 잘못된 패턴 — 컨트롤러에서 준영속 엔티티 생성
@PostMapping("items/{itemId}/edit")
public String updateItem(@PathVariable Long itemId, BookForm form) {
    Book book = new Book();           // ← 컨트롤러에서 엔티티 생성
    book.setId(form.getId());         // ← id 설정 (준영속 엔티티)
    book.setName(form.getName());
    book.setPrice(form.getPrice());
    book.setStockQuantity(form.getStockQuantity());
    book.setAuthor(form.getAuthor());
    book.setIsbn(form.getIsbn());

    itemService.saveItem(book);       // ← merge 방식 (위험!)
    return "redirect:/items";
}
```

**문제점:**

1. **보안 취약점**: `itemId`를 임의로 조작하면 다른 상품의 데이터를 수정할 수 있습니다.
   서비스 계층에서 현재 사용자가 해당 아이템에 권한이 있는지 검증해야 합니다.

2. **merge의 null 덮어쓰기 위험**: 위에서 설명한 것처럼, 모든 필드를 채우지 않으면 null로 업데이트됩니다.

3. **유지보수 어려움**: 컨트롤러에 도메인 로직이 섞여 계층 분리가 무너집니다.

---

### 6.5 권장 패턴: ID + 필드값만 서비스로 전달

```java
// ✅ 올바른 패턴 — 식별자와 필드값만 서비스로 전달
@PostMapping("items/{itemId}/edit")
public String updateItem(@PathVariable Long itemId,
                         @ModelAttribute("form") BookForm form) {
    // 컨트롤러는 식별자(itemId)와 필요한 데이터만 서비스로 전달
    itemService.updateItem(itemId, form.getName(), form.getPrice(), form.getStockQuantity());
    return "redirect:/items";
}
```

```java
// service/ItemService.java
@Transactional
public void updateItem(Long itemId, String name, int price, int stockQuantity) {
    Item findItem = itemRepository.findOne(itemId);  // 트랜잭션 내에서 영속 상태로 조회
    findItem.setName(name);
    findItem.setPrice(price);
    findItem.setStockQuantity(stockQuantity);
    // 트랜잭션 커밋 → 변경 감지 → UPDATE 자동 실행
}
```

**업데이트 필드가 많을 때: 서비스 DTO 생성**

```java
// service/UpdateItemDto.java
public class UpdateItemDto {
    private String name;
    private int price;
    private int stockQuantity;
    // ... 기타 필드
}

// 서비스 메서드
@Transactional
public void updateItem(Long itemId, UpdateItemDto dto) {
    Item findItem = itemRepository.findOne(itemId);
    findItem.setName(dto.getName());
    // ...
}
```

> 업데이트할 데이터가 많다면 서비스 계층에 DTO를 별도로 만들면 됩니다.
> 컨트롤러 폼 DTO와 서비스 DTO를 분리함으로써 각 계층의 책임이 명확해집니다.

**더 나아가 — 의미있는 메서드로 변경:**

```java
// 현재 예제 (set, set, set...)
findItem.setName(name);
findItem.setPrice(price);
findItem.setStockQuantity(stockQuantity);

// 더 나은 설계 — 의미있는 메서드로 캡슐화
findItem.change(name, price, stockQuantity);
// 또는
findItem.addStock(count);
// → 변경 지점이 엔티티로 집중됨 → 어디서 바꾸는지 찾기 쉬움
```

---

### 6.6 비교 정리

| 구분 | 병합(merge) | 변경 감지(Dirty Checking) |
|------|------------|--------------------------|
| **방식** | `em.merge(준영속엔티티)` | 영속 엔티티 조회 → setter 호출 |
| **필드 처리** | **모든 필드 덮어씀** (null 포함) | **변경된 필드만** 업데이트 |
| **null 위험** | ❌ 있음 — 빠뜨린 필드는 null로 저장 | ✅ 없음 — 변경한 필드만 처리 |
| **실무 권장** | ❌ 사용 지양 | ✅ 권장 |
| **사용 조건** | 모든 필드를 반드시 채워야 할 때만 | 항상 가능 |

```
머릿속에 "merge는 쓰지 않는다"로 가져가세요.
실무는 되게 복잡해서 merge로 깔끔하게 할 수 있는 경우가 없습니다.
조금 귀찮더라도 직접 영속 엔티티를 조회해서 필요한 필드만 set하고 반환하세요.
— 강의 김영한
```

---

## 7. 상품 주문

### 7.1 주문 폼

```html
<!-- order/orderForm.html -->
<form role="form" action="/order" method="post">
    <div class="form-group">
        <label for="member">주문회원</label>
        <select name="memberId" id="member" class="form-control">
            <option value="">회원선택</option>
            <option th:each="member : ${members}"
                    th:value="${member.id}"
                    th:text="${member.name}" />
        </select>
    </div>

    <div class="form-group">
        <label for="item">상품명</label>
        <select name="itemId" id="item" class="form-control">
            <option value="">상품선택</option>
            <option th:each="item : ${items}"
                    th:value="${item.id}"
                    th:text="${item.name}" />
        </select>
    </div>

    <div class="form-group">
        <label for="count">주문수량</label>
        <input type="number" name="count" class="form-control"
               id="count" placeholder="주문 수량을 입력하세요">
    </div>

    <button type="submit" class="btn btn-primary">Submit</button>
</form>
```

**`th:value`와 `th:text` 분리 이유:**
- `th:value="${member.id}"`: 폼 제출 시 서버로 전송되는 값 (식별자)
- `th:text="${member.name}"`: 화면에 사용자에게 보여지는 텍스트

---

### 7.2 컨트롤러에서 식별자만 전달하는 이유

```java
// OrderController.java

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

    // 컨트롤러: 식별자만 서비스로 전달
    orderService.order(memberId, itemId, count);
    return "redirect:/orders";
}
```

**왜 엔티티 대신 식별자를 넘기는가:**

| 방법 | 설명 | 문제점 |
|------|------|--------|
| **컨트롤러에서 엔티티 조회 후 전달** | `Member member = memberService.findOne(memberId)` → `orderService.order(member, ...)` | 컨트롤러에서 조회한 엔티티는 트랜잭션 밖에서 로드됨 → 준영속 상태 → Dirty Checking 불가 |
| **식별자만 전달 (권장)** | `orderService.order(memberId, itemId, count)` | 없음 — 서비스의 `@Transactional` 안에서 엔티티를 조회하므로 영속 상태 유지 |

```
컨트롤러 (트랜잭션 없음)
    ↓
orderService.order(memberId, itemId, count) 호출
    ↓
@Transactional 시작 ← 여기서 영속성 컨텍스트 생성
    ↓
Member member = memberRepository.findOne(memberId)  ← 영속 상태로 조회
Item item = itemRepository.findOne(itemId)           ← 영속 상태로 조회
    ↓
Order.createOrder(member, delivery, orderItem)
    ↓
orderRepository.save(order)  ← member, item 모두 영속 상태에서 연동됨
    ↓
@Transactional 커밋 → Dirty Checking 정상 동작
```

> 특히 command성 비즈니스 로직(주문, 결제 등)에서는
> 컨트롤러 계층에서는 그냥 식별자만 넘기고,
> 핵심 비즈니스 로직은 서비스 계층의 트랜잭션 안에서 처리하세요.

---

## 8. 주문 목록 검색 및 취소

### 8.1 @ModelAttribute로 검색 폼 자동 바인딩

```java
// OrderController.java
@GetMapping("/orders")
public String orderList(@ModelAttribute("orderSearch") OrderSearch orderSearch,
                        Model model) {

    List<Order> orders = orderService.findOrders(orderSearch);
    model.addAttribute("orders", orders);
    // @ModelAttribute("orderSearch")는 다음과 동일:
    // model.addAttribute("orderSearch", orderSearch);  ← 자동으로 생략됨

    return "order/orderList";
}
```

**`@ModelAttribute`의 두 가지 역할:**

1. **요청 파라미터 → 객체 바인딩**: URL 쿼리 스트링(`?memberName=김&orderStatus=ORDER`)을 `OrderSearch` 객체의 필드로 자동 매핑
2. **모델에 자동 추가**: `model.addAttribute("orderSearch", orderSearch)`를 자동으로 수행 → 뷰에서 `${orderSearch}`로 접근 가능

```
GET /orders?memberName=김&orderStatus=ORDER
        ↓
@ModelAttribute가 OrderSearch orderSearch에 자동 바인딩:
  orderSearch.memberName = "김"
  orderSearch.orderStatus = OrderStatus.ORDER
        ↓
model에 "orderSearch"로 자동 추가
        ↓
뷰에서 th:object="${orderSearch}"로 폼 재렌더링
```

---

### 8.2 Enum select 동적 생성 (T() 연산자)

```html
<!-- order/orderList.html (검색 폼 부분) -->
<form th:object="${orderSearch}" class="form-inline">
    <div class="form-group mb-2">
        <input type="text" th:field="*{memberName}" class="form-control" placeholder="회원명"/>
    </div>
    <div class="form-group mx-sm-1 mb-2">
        <select th:field="*{orderStatus}" class="form-control">
            <option value="">주문상태</option>
            <option th:each="status : ${T(jpabook.jpashop.domain.OrderStatus).values()}"
                    th:value="${status}"
                    th:text="${status}">option
            </option>
        </select>
    </div>
    <button type="submit" class="btn btn-primary mb-2">검색</button>
</form>
```

**`T()` 연산자 설명:**

```html
${T(jpabook.jpashop.domain.OrderStatus).values()}
```

| 구성 요소 | 설명 |
|-----------|------|
| `T(클래스명)` | Spring EL(SpEL)의 타입 참조 연산자 — 클래스의 정적 메서드 호출 가능 |
| `.values()` | Enum의 모든 값을 배열로 반환 |
| 결과 | `[ORDER, CANCEL]` 배열 → `th:each`로 반복 → select 옵션 자동 생성 |

**이 방식의 장점:**
- Enum에 새 값(`PENDING` 등)을 추가하면 select 옵션이 자동으로 늘어납니다.
- 하드코딩 없이 Enum의 모든 값이 동적으로 렌더링됩니다.

```html
<!-- 렌더링 결과 (OrderStatus.values() = [ORDER, CANCEL]) -->
<select name="orderStatus">
    <option value="">주문상태</option>
    <option value="ORDER">ORDER</option>
    <option value="CANCEL">CANCEL</option>
</select>
```

---

### 8.3 주문 취소 — JavaScript POST 폼

```html
<!-- order/orderList.html (취소 버튼 부분) -->
<tr th:each="item : ${orders}">
    ...
    <td>
        <a th:if="${item.status.name() == 'ORDER'}"
           href="#"
           th:href="'javascript:cancel('+${item.id}+')'"
           class="btn btn-danger">CANCEL</a>
    </td>
</tr>
```

```javascript
// JavaScript POST 폼 생성
function cancel(id) {
    var form = document.createElement("form");
    form.setAttribute("method", "post");
    form.setAttribute("action", "/orders/" + id + "/cancel");
    document.body.appendChild(form);
    form.submit();
}
```

```java
// OrderController.java
@PostMapping("/orders/{orderId}/cancel")
public String cancelOrder(@PathVariable("orderId") Long orderId) {
    orderService.cancelOrder(orderId);
    return "redirect:/orders";
}
```

**왜 a 태그에 GET이 아닌 JavaScript로 POST를 보내는가:**

HTML에서 `<a>` 태그는 기본적으로 GET 요청을 합니다.
그러나 취소는 데이터를 변경하는 작업이므로 POST(또는 DELETE)를 사용해야 합니다.
HTML form은 GET/POST만 지원하므로, JavaScript로 동적으로 form을 생성하여 POST 요청을 만듭니다.

```
취소 버튼 클릭
    ↓
cancel(orderId) 실행
    ↓
<form method="post" action="/orders/1/cancel"> 동적 생성
    ↓
form.submit() → POST /orders/1/cancel 요청
    ↓
@PostMapping("/orders/{orderId}/cancel") → orderService.cancelOrder(1)
    ↓
redirect:/orders
```

**`th:if="${item.status.name() == 'ORDER'}"` 조건:**

이미 취소된 주문(CANCEL 상태)에는 CANCEL 버튼을 표시하지 않습니다.
`status.name()`은 Enum의 이름 문자열을 반환합니다.

---

## 9. Best Practice 및 주의사항

### ✅ 폼 객체(DTO)로 엔티티를 보호하라

```java
// ✅ 올바른 방법 — 폼 객체 분리
@PostMapping("/members/new")
public String create(@Valid MemberForm form, BindingResult result) {
    // MemberForm으로 데이터 받기 → Member 엔티티로 변환
    Member member = new Member();
    member.setName(form.getName());
    ...
}

// ❌ 잘못된 방법 — 엔티티를 폼 데이터로 직접 사용
@PostMapping("/members/new")
public String create(@Valid Member member, BindingResult result) {
    // Member 엔티티가 화면 전용 어노테이션을 가짐
    // API 스펙이 엔티티와 결합됨
    ...
}
```

### ✅ 변경 감지로 수정, merge는 쓰지 않는다

```java
// ✅ 올바른 수정 — 변경 감지
@Transactional
public void updateItem(Long itemId, String name, int price, int stockQuantity) {
    Item findItem = itemRepository.findOne(itemId);  // 영속 상태 조회
    findItem.setName(name);         // 필요한 필드만 변경
    findItem.setPrice(price);
    findItem.setStockQuantity(stockQuantity);
    // 커밋 시 자동 UPDATE
}

// ❌ 잘못된 수정 — merge (모든 필드 덮어쓰기)
public void saveItem(Item item) {
    em.merge(item);  // 설정하지 않은 필드가 null로 덮어써질 위험
}
```

### ✅ 컨트롤러에서 서비스로 식별자만 전달

```java
// ✅ 올바른 방법
@PostMapping("/order")
public String order(@RequestParam Long memberId,
                    @RequestParam Long itemId,
                    @RequestParam int count) {
    orderService.order(memberId, itemId, count);  // 식별자만 전달
    return "redirect:/orders";
}

// ❌ 잘못된 방법 (트랜잭션 밖에서 엔티티 조회 후 전달)
@PostMapping("/order")
public String order(@RequestParam Long memberId, ...) {
    Member member = memberService.findOne(memberId);  // 트랜잭션 밖에서 조회 → 준영속
    orderService.order(member, ...);   // 준영속 엔티티 전달 → Dirty Checking 불가
    return "redirect:/orders";
}
```

### ✅ @Valid와 BindingResult는 항상 쌍으로 사용

```java
// ✅ 올바른 방법
@PostMapping("/members/new")
public String create(@Valid MemberForm form, BindingResult result) {
    if (result.hasErrors()) {  // 오류 시 폼으로 복귀
        return "members/createMemberForm";
    }
    ...
}

// ❌ BindingResult 없이 @Valid만 사용
@PostMapping("/members/new")
public String create(@Valid MemberForm form) {
    // 검증 실패 시 BindException 예외 발생 → 500 에러 페이지
    ...
}
```

### ⚠️ API 만들 때는 절대 엔티티를 반환하지 말 것

```java
// ❌ API에서 엔티티 직접 반환 — 위험!
@GetMapping("/api/members")
@ResponseBody
public List<Member> membersV1() {
    return memberService.findMembers();  // 엔티티 직접 반환
    // 문제 1: 엔티티 필드 추가/삭제 시 API 스펙 변경
    // 문제 2: 양방향 관계에서 무한 루프 JSON 직렬화
    // 문제 3: 필요 없는 필드까지 노출
}

// ✅ API에서 DTO 반환
@GetMapping("/api/members")
@ResponseBody
public List<MemberDto> membersV2() {
    return memberService.findMembers().stream()
        .map(m -> new MemberDto(m.getName()))
        .collect(Collectors.toList());
}
```

### ⚠️ 상품 수정 시 보안 취약점 인지

```java
@PostMapping("items/{itemId}/edit")
public String updateItem(@PathVariable Long itemId, BookForm form) {
    itemService.updateItem(itemId, form.getName(), ...);
    // ⚠️ itemId를 공격자가 다른 값으로 바꿔서 요청하면?
    // → 서비스 계층에서 현재 사용자가 해당 아이템에 권한이 있는지 검증 필요
    // 예: if (!item.getOwner().equals(currentUser)) throw new AccessDeniedException();
}
```

---

## 10. 부록

### 10.1 프로젝트 구조 (챕터 7 추가분)

```
src/main/java/jpabook/jpashop/
├── controller/                              ← 웹 계층 (챕터 7에서 추가)
│   ├── HomeController.java                  ← 홈 화면
│   ├── MemberController.java               ← 회원 등록, 목록
│   ├── MemberForm.java                     ← 회원 폼 DTO
│   ├── ItemController.java                 ← 상품 등록, 목록, 수정
│   ├── BookForm.java                       ← 상품 폼 DTO
│   └── OrderController.java               ← 주문, 목록 검색, 취소
├── service/
│   ├── ItemService.java                    ← updateItem() 변경 감지 수정
│   └── UpdateItemDto.java                  ← 서비스 레이어 DTO (업데이트 필드 많을 때)
└── ...

src/main/resources/templates/
├── fragments/
│   ├── header.html                         ← 공통 <head> fragment
│   ├── bodyHeader.html                     ← 공통 내비게이션 fragment
│   └── footer.html                         ← 공통 하단 fragment
├── home.html                               ← 홈 화면
├── members/
│   ├── createMemberForm.html              ← 회원 등록 폼
│   └── memberList.html                    ← 회원 목록
├── items/
│   ├── createItemForm.html                ← 상품 등록 폼
│   ├── itemList.html                      ← 상품 목록
│   └── updateItemForm.html                ← 상품 수정 폼
└── order/
    ├── orderForm.html                     ← 주문 폼
    └── orderList.html                     ← 주문 목록 + 검색 + 취소
```

---

### 10.2 핵심 용어 정리

| 용어 | 설명 |
|------|------|
| **Thymeleaf fragment** | 공통 HTML 조각을 파일로 분리하고, `th:replace`로 재사용하는 레이아웃 메커니즘 |
| **폼 객체(Form DTO)** | 웹 계층의 입력 데이터를 받기 위한 전용 객체. 엔티티를 직접 사용하지 않기 위해 분리 |
| **@Valid** | 메서드 파라미터에 선언된 Bean Validation 어노테이션(`@NotEmpty` 등)을 실행하도록 지시 |
| **BindingResult** | `@Valid` 실행 결과(오류 정보)를 담는 객체. 선언 시 예외 대신 오류를 결과에 담아 메서드를 계속 실행 |
| **준영속(Detached) 엔티티** | 한번 영속 상태였다가 영속성 컨텍스트에서 분리된 엔티티. id가 있지만 JPA가 관리하지 않음 |
| **병합(merge)** | 준영속 엔티티를 영속 상태로 만드는 방법. 모든 필드를 덮어쓰기 때문에 null 위험 존재 |
| **변경 감지(Dirty Checking)** | 영속 상태 엔티티의 변경을 트랜잭션 커밋 시 JPA가 감지하여 UPDATE SQL을 자동 실행하는 메커니즘 |
| **@ModelAttribute** | 요청 파라미터를 객체에 바인딩하고, 모델에도 자동으로 추가해주는 Spring MVC 어노테이션 |
| **T() 연산자** | Spring EL(SpEL)에서 클래스 타입을 참조하는 연산자. Thymeleaf에서 Enum의 정적 메서드 호출에 사용 |
| **PRG 패턴** | POST 처리 후 redirect를 수행하여 브라우저 새로고침 시 중복 제출을 방지하는 패턴 |

---

### 10.3 Thymeleaf 주요 문법 정리

| 문법 | 설명 | 예시 |
|------|------|------|
| `th:replace="~{파일 :: 이름}"` | 현재 태그를 fragment로 교체 | `th:replace="~{fragments/header :: header}"` |
| `th:object="${객체}"` | 폼 바인딩 객체 지정 | `th:object="${memberForm}"` |
| `*{필드명}` | th:object의 필드 선택 표현식 | `*{name}` |
| `th:field="*{필드명}"` | id, name, value 속성 자동 생성 | `th:field="*{name}"` |
| `th:each="변수 : ${컬렉션}"` | 반복 렌더링 | `th:each="member : ${members}"` |
| `th:text="${값}"` | 텍스트 출력 (HTML 이스케이프) | `th:text="${member.name}"` |
| `th:if="${조건}"` | 조건부 렌더링 | `th:if="${item.status.name() == 'ORDER'}"` |
| `th:href="@{URL (변수=값)}"` | 동적 URL 링크 생성 | `th:href="@{/items/{id}/edit (id=${item.id})}"` |
| `${#fields.hasErrors('필드')}` | 폼 필드 오류 여부 확인 | `${#fields.hasErrors('name')}` |
| `th:errors="*{필드}"` | 필드 오류 메시지 출력 | `th:errors="*{name}"` |
| `th:class="${조건} ? 'A' : 'B'"` | 조건부 클래스 적용 | `th:class="${#fields.hasErrors('name')} ? 'form-control fieldError' : 'form-control'"` |
| `${T(패키지.클래스).메서드()}` | 정적 메서드 호출 | `${T(jpabook.jpashop.domain.OrderStatus).values()}` |
| `${객체?.필드}` | null-safe 접근 | `${member.address?.city}` |

---

### 10.4 어노테이션 정리

| 어노테이션 | 패키지 | 역할 |
|-----------|--------|------|
| `@Controller` | `org.springframework.stereotype` | Spring MVC 컨트롤러로 등록 — 메서드 반환값을 뷰 이름으로 처리 |
| `@GetMapping("/path")` | `org.springframework.web.bind.annotation` | HTTP GET 요청 핸들러 |
| `@PostMapping("/path")` | `org.springframework.web.bind.annotation` | HTTP POST 요청 핸들러 |
| `@PathVariable` | `org.springframework.web.bind.annotation` | URL 경로 변수 바인딩 (`/items/{itemId}/edit`) |
| `@RequestParam` | `org.springframework.web.bind.annotation` | 요청 파라미터 바인딩 (`?memberId=1`) |
| `@ModelAttribute` | `org.springframework.web.bind.annotation` | 요청 파라미터 → 객체 바인딩 + 모델 자동 추가 |
| `@Valid` | `jakarta.validation` | Bean Validation 실행 지시 |
| `@NotEmpty` | `jakarta.validation.constraints` | 문자열이 null이거나 비어있으면 검증 실패 |
| `BindingResult` | `org.springframework.validation` | @Valid 결과(오류 정보)를 담는 객체 |

---

### 10.5 학습 점검 Q&A

**Q1. 왜 엔티티를 폼 데이터로 직접 사용하면 안 되나요?**

화면의 Validation 요구사항과 도메인의 Validation 요구사항이 다를 수 있습니다.
엔티티에 화면 전용 어노테이션(`@NotEmpty` 등)이 누적되면 엔티티가 오염됩니다.
또한 API를 만들 때 엔티티를 직접 반환하면 API 스펙이 엔티티 구조와 결합되어,
엔티티 변경 시 의도치 않게 API 스펙도 변경됩니다.

**Q2. @Valid와 BindingResult를 함께 써야 하는 이유는?**

`@Valid`만 있으면 검증 실패 시 `BindException`이 발생하여 500 에러 페이지가 나옵니다.
`BindingResult`를 `@Valid` 바로 뒤에 선언하면, 오류가 있어도 예외 대신 `BindingResult`에 오류 정보를 담아 메서드를 계속 실행합니다. 이를 통해 오류 메시지를 폼 화면에 표시할 수 있습니다.

**Q3. 준영속 엔티티란 무엇이고 왜 문제가 되나요?**

준영속 엔티티는 DB에 이미 있는 데이터를 가리키는 id를 보유하지만, JPA 영속성 컨텍스트가 관리하지 않는 엔티티입니다. 영속성 컨텍스트가 관리하지 않으므로 Dirty Checking(변경 감지)이 동작하지 않습니다. 상품 수정 시 폼에서 받아온 데이터로 Book 객체를 만들면 이것이 준영속 엔티티가 됩니다.

**Q4. merge가 위험한 이유는 무엇인가요?**

`em.merge()`는 준영속 엔티티의 **모든 필드**를 기존 엔티티에 덮어씁니다.
만약 수정 폼에서 price 필드를 설정하지 않았다면, DB의 price가 null(또는 0)으로 덮어써집니다.
실무에서는 10개 필드 중 4~5개만 수정하는 경우가 많아, merge를 올바르게 사용하기 어렵습니다.

**Q5. 컨트롤러에서 엔티티 대신 식별자만 서비스로 전달해야 하는 이유는?**

컨트롤러에서 엔티티를 조회하면 트랜잭션 밖에서 로딩되므로 준영속 상태가 됩니다.
이 엔티티를 서비스로 넘기면 서비스의 트랜잭션 안에서도 Dirty Checking이 적용되지 않습니다.
반면 식별자만 넘기면 서비스의 `@Transactional` 안에서 엔티티를 조회하게 되어,
영속 상태로 유지되며 Dirty Checking이 정상 동작합니다.

**Q6. JavaScript로 POST 요청을 만드는 이유는?**

HTML의 `<a>` 태그는 GET 요청만 보낼 수 있습니다.
데이터를 변경하는 취소 작업은 POST(또는 DELETE)를 사용해야 합니다.
HTML form은 GET/POST만 지원하므로, JavaScript로 동적으로 form 엘리먼트를 생성하여
POST 요청을 만드는 방식을 사용합니다.

---

### 10.6 챕터 완료 — 다음 단계

챕터 7(웹 계층 개발)로 "실전! 스프링 부트와 JPA 활용 1" 강의가 완료되었습니다.

**이 강의에서 배운 핵심 기술 스택:**

```
도메인 설계 → 엔티티 구현 → 리포지토리/서비스 → 웹 계층
```

| 챕터 | 핵심 개념 |
|------|---------|
| 2. 도메인 분석 설계 | 엔티티 모델링, 연관관계 매핑, 테이블 설계 |
| 4. 회원 도메인 개발 | EntityManager, @Transactional, 테스트 |
| 5. 상품 도메인 개발 | JPA 상속 매핑, 도메인 모델 패턴, persist vs merge |
| 6. 주문 도메인 개발 | CascadeType.ALL, 생성 메서드, Dirty Checking, 동적 쿼리 |
| **7. 웹 계층 개발** | **Thymeleaf, 폼 객체, @Valid, 변경 감지 vs 병합** |

**다음 강의 예고 — 실전! 스프링 부트와 JPA 활용 2:**

| 개념 | 설명 |
|------|------|
| **API 개발 기본** | REST API 설계, DTO 완전 분리, `@JsonIgnore` 대신 DTO 사용 |
| **API 개발 고급** | 지연 로딩(N+1 문제), 페치 조인, DTO 조회 최적화 |
| **Querydsl** | 타입 안전한 동적 쿼리, 실무 표준 |
| **Spring Data JPA** | JpaRepository, 쿼리 메서드, 페이징 |
