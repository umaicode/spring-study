# API 개발 기본

> REST API 설계 시 엔티티 직접 노출의 위험성과 DTO 활용 패턴을 익힌다.

## 📚 강의 출처

**인프런 - 김영한의 "실전! 스프링 부트와 JPA 활용 2 - API 개발과 성능 최적화"**

이 문서는 강의의 첫 번째 챕터인 "API 개발 기본"을 학습하며 작성한 노트입니다.
기존 jpashop 웹 애플리케이션(Thymeleaf 뷰) 위에 **REST API 계층**을 추가하는 방식으로 진행됩니다.

- 강의 PDF: `docs/1. API 개발 기본.pdf`
- 관련 코드: `src/main/java/jpabook/jpashop_2/api/MemberApiController.java`

---

## 🎯 학습 목표

1. **엔티티 직접 노출의 위험성** 이해
   - 엔티티 변경 → API 스펙 자동 변경이라는 강결합 문제 파악
   - 불필요한 필드 노출(보안 문제) 및 `@JsonIgnore` 남용 이유 이해

2. **DTO(Data Transfer Object) 패턴** 적용
   - 요청/응답 각각에 별도 DTO 클래스를 만드는 이유와 방법 습득
   - API 스펙과 DTO가 1:1로 대응되어 유지보수성이 높아지는 원리 이해

3. **회원 등록 API V1 vs V2** 비교 분석
   - V1(엔티티 직접 수신)의 문제점과 V2(DTO 수신)의 장점 명확화

4. **CQS(Command-Query Separation) 패턴** 으로 회원 수정 API 구현
   - `update()`(커맨드)와 `findOne()`(쿼리)를 명확히 분리하는 실전 스타일 학습

5. **Result 제네릭 래퍼**로 컬렉션 응답 유연화
   - JSON 배열 직접 반환의 한계와 래퍼 객체를 통한 확장성 확보

---

## 🗺️ 학습 로드맵

```
[챕터 1: API 개발 기본]
         │
         ▼
┌─────────────────────┐
│   1. 회원 등록 API  │
│   POST /api/v1/members  ──► V1: 엔티티 직접 수신 (❌ 안티패턴)
│   POST /api/v2/members  ──► V2: DTO 수신 (✅ 권장)
└─────────────────────┘
         │
         ▼
┌─────────────────────┐
│   2. 회원 수정 API  │
│   PUT /api/v2/members/{id}
│   ├─ 변경 감지(Dirty Checking)으로 UPDATE
│   └─ CQS 패턴: update(커맨드) + findOne(쿼리) 분리
└─────────────────────┘
         │
         ▼
┌─────────────────────┐
│   3. 회원 조회 API  │
│   GET /api/v1/members  ──► V1: 엔티티 직접 반환 (❌ 안티패턴)
│   GET /api/v2/members  ──► V2: DTO + Result 래퍼 (✅ 권장)
└─────────────────────┘
         │
         ▼
  [핵심 원칙 확립]
  "API에서 엔티티를 절대 노출하거나 받지 마라!"
```

---

## 📖 목차

1. [회원 등록 API](#1-회원-등록-api)
2. [회원 수정 API](#2-회원-수정-api)
3. [회원 조회 API](#3-회원-조회-api)

---

## 1. 회원 등록 API

### V1 vs V2 비교

| 구분 | V1 (엔티티 직접 수신) | V2 (DTO 수신) |
|------|----------------------|---------------|
| `@RequestBody` 대상 | `Member` 엔티티 | `CreateMemberRequest` DTO |
| API 스펙 명확성 | 불명확 (엔티티 필드 전체) | 명확 (DTO 필드만) |
| 엔티티 필드 변경 시 | API 스펙도 함께 변경됨 ❌ | API 스펙 유지됨 ✅ |
| 검증 어노테이션 위치 | 엔티티에 종속됨 (`@NotEmpty`) | DTO에 독립적으로 위치 |
| 파라미터 파악 난이도 | 엔티티 전체를 봐야 함 | DTO만 보면 됨 |

### V1 — 엔티티 직접 수신 (❌ 안티패턴)

```java
@PostMapping("/api/v1/members")
public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
    Long id = memberService.join(member);
    return new CreateMemberResponse(id);
}
```

**문제점:**
- `Member` 엔티티에 `@NotEmpty` 같은 **프레젠테이션 검증 로직**이 들어감
- `Member.name` → `Member.username` 으로 바꾸면 **API 스펙 자체가 바뀜**
- API가 많아질수록 엔티티에 각종 어노테이션이 범람하여 관리가 불가능해짐

### V2 — DTO 수신 (✅ 권장)

```java
@PostMapping("/api/v2/members")
public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
    Member member = new Member();
    member.setName(request.getName());

    Long id = memberService.join(member);
    return new CreateMemberResponse(id);
}

@Data
static class CreateMemberRequest {
    @NotEmpty
    private String name;
}

@Data
static class CreateMemberResponse {
    private Long id;

    public CreateMemberResponse(Long id) {
        this.id = id;
    }
}
```

**장점:**
- `CreateMemberRequest` DTO만 보면 어떤 값이 넘어오는지 즉시 파악 가능
- 엔티티 필드명이 바뀌어도 컴파일 타임에 오류를 감지 (`request.getName()` 사용 부분)
- API 스펙(`CreateMemberRequest`)과 도메인 모델(`Member`)이 **독립적으로 진화 가능**

> **핵심 원칙**: API를 만들 때는 항상 엔티티를 파라미터로 받지 마라. 엔티티를 외부에 노출해서도 안 된다.

---

## 2. 회원 수정 API

### PUT과 멱등성(Idempotency)

`@PutMapping`을 사용하는 이유:

| HTTP 메서드 | 의미 | 멱등성 |
|------------|------|--------|
| `PUT` | 리소스 전체 교체 | ✅ 멱등 (같은 요청 여러 번 → 같은 결과) |
| `PATCH` | 리소스 부분 수정 | ❌ 비멱등 (표준상 멱등하지 않음) |
| `POST` | 리소스 생성 | ❌ 비멱등 |

> **참고**: 회원 이름만 바꾸는 **부분 업데이트**이므로 REST 표준으로는 `PATCH`가 더 정확합니다. 강의에서는 편의상 `PUT`을 사용합니다.

### 변경 감지(Dirty Checking)로 UPDATE 구현

```java
// MemberService.java
@Transactional
public void update(Long id, String name) {
    Member member = memberRepository.findOne(id);
    member.setName(name);  // 트랜잭션 종료 시 JPA가 변경 감지 → UPDATE 쿼리 자동 실행
}
```

- 트랜잭션 내에서 영속 상태의 엔티티를 변경하면 JPA가 커밋 시점에 자동으로 `UPDATE` 쿼리를 실행합니다.
- `merge()`(병합)를 사용하지 않고 **변경 감지**를 권장하는 이유: 병합은 모든 필드를 교체하므로 `null` 필드가 의도치 않게 덮어써질 수 있습니다.

### CQS(Command-Query Separation) 패턴

```java
// MemberApiController.java
@PutMapping("/api/v2/members/{id}")
public UpdateMemberResponse updateMemberV2(
        @PathVariable("id") Long id,
        @RequestBody @Valid UpdateMemberRequest request) {

    memberService.update(id, request.getName());    // Command: 변경만 수행, void 반환
    Member findMember = memberService.findOne(id);  // Query: 조회만 수행
    return new UpdateMemberResponse(findMember.getId(), findMember.getName());
}

@Data
static class UpdateMemberRequest {
    private String name;
}

@Data
@AllArgsConstructor
static class UpdateMemberResponse {
    private Long id;
    private String name;
}
```

**CQS 원칙:**

| 구분 | 역할 | 반환값 |
|------|------|--------|
| **커맨드(Command)** | 상태를 변경 | `void` |
| **쿼리(Query)** | 상태를 조회 | 데이터 반환 |

- `update()`에서 `Member`를 반환하면 "업데이트하면서 조회"가 되어 커맨드와 쿼리가 섞임
- 이를 분리하면 메서드의 **의도가 명확**해지고 **유지보수성이 높아짐**
- 강사의 실전 스타일: `update()`는 `void` 반환 → 이후 별도 `findOne()`으로 조회

---

## 3. 회원 조회 API

### V1 vs V2 비교

| 구분 | V1 (엔티티 직접 반환) | V2 (DTO + Result 래퍼) |
|------|----------------------|------------------------|
| 반환 타입 | `List<Member>` | `Result<List<MemberDto>>` |
| 불필요한 필드 노출 | 있음 (보안 위험) ❌ | 없음 (필요한 필드만) ✅ |
| `@JsonIgnore` 필요 | 있음 (엔티티 오염) ❌ | 없음 ✅ |
| JSON 배열 직접 반환 | 배열 그대로 반환 ❌ | 래퍼 객체로 감쌈 ✅ |
| 향후 필드 추가 | 스펙 파괴 (배열이므로) ❌ | `count` 등 자유롭게 추가 ✅ |

### V1 — 엔티티 직접 반환 (❌ 안티패턴)

```java
@GetMapping("/api/v1/members")
public List<Member> membersV1() {
    return memberService.findMembers();
}
```

**문제점:**
- `Member` 엔티티의 모든 필드(주문 정보 포함)가 JSON으로 노출됨
- 숨기고 싶은 필드에 `@JsonIgnore`를 붙이면 **엔티티에 프레젠테이션 로직이 침투**
- 클라이언트마다 다른 API 스펙을 요구할 때 엔티티 하나로 대응 불가능
- JSON 배열(`[{...}, {...}]`)을 직접 반환하면 `count` 같은 메타데이터를 **추가할 수 없음**

### V2 — DTO + Result 래퍼 (✅ 권장)

```java
@GetMapping("/api/v2/members")
public Result memberV2() {
    List<Member> findMembers = memberService.findMembers();
    List<MemberDto> collect = findMembers.stream()
            .map(m -> new MemberDto(m.getName()))
            .collect(Collectors.toList());

    return new Result(collect);
}

@Data
@AllArgsConstructor
static class Result<T> {
    // 필요하면 count 등을 여기에 추가 가능
    // private int count;
    private T data;
}

@Data
@AllArgsConstructor
static class MemberDto {
    private String name;
}
```

**V2 응답 구조:**

```json
{
    "data": [
        { "name": "홍길동" },
        { "name": "김영희" }
    ]
}
```

향후 `count` 필드 추가 시:

```json
{
    "count": 2,
    "data": [
        { "name": "홍길동" },
        { "name": "김영희" }
    ]
}
```

- `Result<T>` 제네릭 래퍼로 감싸면 **JSON 스펙을 깨지 않고** 언제든 새 필드를 추가 가능
- `MemberDto`로 **노출할 필드를 명시적으로 선언**하므로 보안 위험 없음
- 엔티티의 `name` → `username` 변경 시 `m.getName()` 부분에서 컴파일 오류 발생 → **조기 감지**

---

## Best Practice 및 주의사항

### ❌ vs ✅ 비교

| 상황 | ❌ 안티패턴 | ✅ 권장 패턴 |
|------|------------|------------|
| **요청 수신** | 엔티티를 `@RequestBody`로 직접 받기 | DTO(Request 클래스)로 받기 |
| **응답 반환** | 엔티티를 직접 반환 | DTO(Response 클래스)로 변환 후 반환 |
| **컬렉션 반환** | `List<Entity>` 그대로 반환 | `Result<T>` 래퍼로 감싸서 반환 |
| **수정 구현** | `update()`에서 수정 결과를 바로 반환 | CQS: `update(커맨드)` → `findOne(쿼리)` 분리 |

### 추가 주의사항

- **Lombok 사용 지침**: 엔티티에는 `@Getter` 정도만, DTO에는 `@Data`, `@AllArgsConstructor` 등 자유롭게 사용
- **DTO 위치**: 간단한 API의 경우 컨트롤러 내부 `static class`로 선언해도 무방, 복잡해지면 별도 패키지로 분리
- **검증 어노테이션**: `@NotEmpty`, `@NotNull` 등은 엔티티가 아닌 **Request DTO**에 위치시켜야 API별로 다른 검증 규칙을 적용 가능

---

## 부록

### API 엔드포인트 목록

| 메서드 | URL | 버전 | 설명 |
|--------|-----|------|------|
| `POST` | `/api/v1/members` | V1 | 회원 등록 (엔티티 직접 수신 — 안티패턴) |
| `POST` | `/api/v2/members` | V2 | 회원 등록 (DTO 수신 — 권장) |
| `PUT` | `/api/v2/members/{id}` | V2 | 회원 수정 (변경 감지 + CQS) |
| `GET` | `/api/v1/members` | V1 | 회원 조회 (엔티티 직접 반환 — 안티패턴) |
| `GET` | `/api/v2/members` | V2 | 회원 조회 (DTO + Result 래퍼 — 권장) |

### 핵심 용어 정리

| 용어 | 설명 |
|------|------|
| **DTO (Data Transfer Object)** | 계층 간 데이터 전달을 위한 전용 객체. API 요청/응답 스펙과 1:1로 대응 |
| **변경 감지 (Dirty Checking)** | JPA가 영속 컨텍스트 내 엔티티의 변경을 추적하여 트랜잭션 커밋 시 자동으로 UPDATE를 수행하는 기능 |
| **멱등성 (Idempotency)** | 동일한 요청을 여러 번 보내도 결과가 같은 성질. PUT은 멱등, POST는 비멱등 |
| **CQS (Command-Query Separation)** | 메서드를 상태 변경(Command, void 반환)과 데이터 조회(Query, 값 반환)로 명확히 분리하는 설계 원칙 |
| **Result 래퍼** | 컬렉션을 오브젝트로 감싸는 제네릭 클래스. 향후 메타데이터(count 등) 추가를 위한 확장성 확보 |

### 핵심 어노테이션 정리

| 어노테이션 | 위치 | 역할 |
|-----------|------|------|
| `@RestController` | 클래스 | `@Controller` + `@ResponseBody` 결합. 모든 메서드 반환값을 JSON으로 직렬화 |
| `@RequestBody` | 파라미터 | HTTP 요청 바디(JSON)를 자바 객체로 역직렬화 |
| `@Valid` | 파라미터 | `@NotEmpty`, `@NotNull` 등 Bean Validation 어노테이션 활성화 |
| `@PutMapping` | 메서드 | HTTP PUT 요청 매핑. 멱등한 리소스 수정에 사용 |
| `@PathVariable` | 파라미터 | URL 경로 변수(`{id}`) 추출 |
| `@Data` | 클래스(Lombok) | `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode`, `@RequiredArgsConstructor` 한 번에 적용 |
| `@AllArgsConstructor` | 클래스(Lombok) | 모든 필드를 인자로 받는 생성자 자동 생성 |

### 학습 확인 질문

1. 회원 등록 API에서 `Member` 엔티티를 `@RequestBody`로 직접 받으면 어떤 문제가 발생하는가?
2. `saveMemberV2`에서 DTO를 사용했을 때, `Member.name`이 `Member.username`으로 변경되면 어느 시점에 오류를 감지할 수 있는가?
3. `MemberService.update()`가 `void`를 반환하는 이유는 무엇인가? CQS와 연결지어 설명하라.
4. 회원 조회 V1에서 `List<Member>`를 직접 반환하면 향후 API 스펙 변경(count 추가 등)에 어떤 문제가 생기는가?
5. `Result<T>` 제네릭 래퍼를 사용하면 어떤 확장이 가능해지는가?

### 다음 챕터 예고

**챕터 2: API 개발 고급 - 준비**

회원처럼 단순한 단일 테이블 조회가 아닌, **여러 테이블을 조인**하는 복잡한 API를 다룹니다.
지연 로딩(`FetchType.LAZY`)이 어떤 문제를 일으키는지, 그리고 이를 어떻게 성능 최적화하며 해결하는지를 학습합니다.

- N+1 문제
- 지연 로딩과 즉시 로딩
- 페이징 처리
- 실무에서 복잡한 API를 성능을 챙기며 만드는 방법
