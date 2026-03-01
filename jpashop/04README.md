# 회원 도메인 개발

> EntityManager부터 트랜잭션 전략까지 — JPA 기반 회원 도메인을 계층별로 완성하는 법

## 📚 강의 출처

**인프런 - 김영한의 "실전! 스프링 부트와 JPA 활용 1 - 웹 애플리케이션 개발"**

이 문서는 강의의 챕터 4 "회원 도메인 개발"을 학습하며 작성한 노트입니다.
챕터 3에서 설계한 계층형 아키텍처(Repository → Service → Controller)를 **실제 코드로 구현하는 첫 번째 도메인**입니다.
EntityManager 를 직접 다루는 Repository 계층부터, @Transactional 전략을 적용한 Service 계층, 그리고 스프링 통합 테스트까지 전 과정을 따라갑니다.

| 챕터 | 내용 |
|------|------|
| 챕터 3 (이전) | 애플리케이션 구현 준비 — 계층 설계, 엔티티 구조 확정 |
| **챕터 4 (현재)** | **회원 도메인 개발 — Repository, Service, Test 구현** |
| 챕터 5 (다음) | 상품 도메인 개발 — Item 계층 구조와 재고 비즈니스 로직 |

---

## 🎯 학습 목표

이 문서를 통해 다음을 이해하고 실습합니다:

1. **EntityManager 의 역할과 주입 방식** — `@PersistenceContext` → `@Autowired` → 생성자 주입 진화 과정
2. **JPQL 기초** — SQL(테이블 대상)과 JPQL(엔티티 대상)의 차이, 파라미터 바인딩
3. **@Transactional 계층 전략** — 클래스 레벨 `readOnly = true` + 메서드 레벨 오버라이드 패턴
4. **중복 회원 검증과 실무 주의사항** — 비즈니스 로직 검증 + DB 유니크 제약 조건 필요성
5. **의존성 주입 3가지 방식 비교** — 필드 주입 / Setter 주입 / 생성자 주입 장단점
6. **스프링 통합 테스트 작성법** — `@SpringBootTest`, `@Transactional`, `em.flush()`, `@Rollback(false)`
7. **테스트 환경 분리** — `test/resources/application.yml` 독립 설정 + 인메모리 DB 자동 구성

---

## 🗺️ 학습 로드맵

챕터 4는 **계층별 Bottom-Up** 방식으로 구성됩니다. 가장 하위 계층인 Repository 부터 구현하고, 그 위에 Service 를 쌓은 뒤, 최종적으로 테스트로 검증합니다.

```
1단계: 회원 리포지토리 개발 (MemberRepository)
   - EntityManager 주입 방식 선택
   - save / findOne / findAll / findByName 구현
   - JPQL 기초 이해
   ↓
2단계: 회원 서비스 개발 (MemberService)
   - @Transactional 전략 설계 (readOnly 기본 + 쓰기 오버라이드)
   - 회원가입 비즈니스 로직 (중복 검증 포함)
   - 의존성 주입 방식 최종 결정 (생성자 주입 + @RequiredArgsConstructor)
   ↓
3단계: 회원 기능 테스트 (MemberServiceTest)
   - 테스트 환경 분리 (test/resources/application.yml)
   - @SpringBootTest + @Transactional 조합
   - 회원가입 / 중복 회원 예외 케이스 검증
```

**왜 이 순서인가?**
- **의존성 방향**: Service 는 Repository 를 의존하므로, Repository 가 먼저 완성되어야 Service 를 올바르게 구현할 수 있습니다.
- **테스트 마지막**: 구현이 완료된 후 전체 계층을 관통하는 통합 테스트로 마무리합니다.

---

## 📖 목차

1. [회원 리포지토리 개발](#1-회원-리포지토리-개발)
   - 1.1 EntityManager 란?
   - 1.2 @Repository 어노테이션 역할
   - 1.3 EntityManager 주입 방식 — 3단계 진화 과정
   - 1.4 핵심 메서드 구현
   - 1.5 JPQL vs SQL 비교
2. [회원 서비스 개발](#2-회원-서비스-개발)
   - 2.1 @Transactional 전략 — 클래스 레벨 readOnly + 메서드 레벨 오버라이드
   - 2.2 회원가입 구현 (join)
   - 2.3 중복 회원 검증 (validateDuplicateMember)
   - 2.4 조회 메서드 (findMembers / findOne)
3. [의존성 주입 방식 비교](#3-의존성-주입-방식-비교)
   - 3.1 필드 주입 (권장 안 함)
   - 3.2 Setter 주입 (권장 안 함)
   - 3.3 생성자 주입 (최종 권장)
   - 3.4 @RequiredArgsConstructor 활용
4. [회원 기능 테스트](#4-회원-기능-테스트)
   - 4.1 통합 테스트 구성
   - 4.2 Given-When-Then 패턴
   - 4.3 회원가입 테스트 케이스
   - 4.4 중복 회원 예외 테스트 케이스
   - 4.5 쿼리 눈으로 확인하기
5. [테스트 환경 분리](#5-테스트-환경-분리)
   - 5.1 왜 test/resources/application.yml 을 따로 두는가
   - 5.2 실제 설정 파일 내용
   - 5.3 스프링 부트 인메모리 DB 자동 설정
   - 5.4 ddl-auto 동작 방식
6. [JUnit4 → JUnit5 변경사항](#6-junit4--junit5-변경사항)
7. [Best Practice 및 주의사항](#7-best-practice-및-주의사항)
8. [부록](#8-부록)

---

## 1. 회원 리포지토리 개발

### 1.1 EntityManager 란?

`EntityManager` 는 JPA 의 핵심 인터페이스입니다. **영속성 컨텍스트(Persistence Context)** 를 통해 엔티티의 생명주기를 관리하며, DB CRUD 작업을 수행합니다.

```
애플리케이션 ─── EntityManager ─── 영속성 컨텍스트 ─── DB
```

| 주요 메서드 | 역할 |
|------------|------|
| `em.persist(entity)` | 엔티티를 영속성 컨텍스트에 저장 (INSERT 예약) |
| `em.find(Type.class, id)` | PK 로 단건 조회 (1차 캐시 활용) |
| `em.createQuery(jpql, Type)` | JPQL 로 다건 조회 |
| `em.flush()` | 영속성 컨텍스트 변경사항을 DB 에 강제 반영 |

> **중요**: `em.persist()` 를 호출한다고 즉시 INSERT 가 나가지 않습니다. 트랜잭션이 커밋될 때 플러시가 실행되면서 실제 SQL 이 DB 에 전달됩니다.

### 1.2 @Repository 어노테이션 역할

```java
@Repository
public class MemberRepository {
    ...
}
```

`@Repository` 는 두 가지 역할을 합니다:

1. **컴포넌트 스캔 대상** — `@ComponentScan` 에 의해 자동으로 스프링 빈으로 등록됩니다.
2. **예외 변환 (AOP 기반)** — JPA 관련 예외(`PersistenceException` 등)를 스프링의 추상화된 `DataAccessException` 계층으로 자동 변환합니다. 이 덕분에 상위 계층(Service)은 JPA 구현체에 종속되지 않아도 됩니다.

### 1.3 EntityManager 주입 방식 — 3단계 진화 과정

강의에서는 EntityManager 주입을 단계별로 발전시키며 최종 패턴을 도출합니다.

**1단계: 표준 JPA 방식 (@PersistenceContext)**

```java
@PersistenceContext
private EntityManager em;
```

- JPA 표준 스펙의 방식
- `@Autowired` 를 사용하면 EntityManager 는 주입되지 않고, 반드시 `@PersistenceContext` 를 써야 함 (순수 JPA 환경 기준)

**2단계: Spring Data JPA 지원 방식 (@Autowired)**

```java
@Autowired
private EntityManager em;
```

- Spring Data JPA 를 사용하면 `@PersistenceContext` 대신 `@Autowired` 도 동작하도록 지원
- 하지만 필드 주입이므로 테스트에서 mock 주입이 어려움

**3단계: 생성자 주입 + @RequiredArgsConstructor (최종 권장)**

```java
@RequiredArgsConstructor
public class MemberRepository {
    private final EntityManager em;   // final + 생성자 주입
}
```

- Spring Data JPA 가 `@Autowired` 를 지원하기 때문에 생성자 주입으로도 EntityManager 가 정상 주입됨
- `final` 키워드로 불변성 보장
- `@RequiredArgsConstructor` 가 `final` 필드만으로 생성자를 자동 생성

### 1.4 핵심 메서드 구현

```java
package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final EntityManager em;

    public void save(Member member) {
        em.persist(member);
    }

    // 단건 조회: em.find(타입, pk)
    public Member findOne(Long id) {
        return em.find(Member.class, id);
    }

    // JPQL: 엔티티 객체를 대상으로 쿼리 (테이블이 아닌 Member 엔티티 기준)
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    // :name → 이름 기반 파라미터 바인딩
    public List<Member> findByName(String name) {
        return em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
    }
}
```

### 1.5 JPQL vs SQL 비교

| 구분 | SQL | JPQL |
|------|-----|------|
| 쿼리 대상 | 테이블 (DB 물리 테이블) | 엔티티 클래스 (객체) |
| 전체 조회 | `SELECT * FROM member` | `select m from Member m` |
| 조건 조회 | `SELECT * FROM member WHERE name = ?` | `select m from Member m where m.name = :name` |
| 파라미터 | `?` (위치 기반) | `:name` (이름 기반, 가독성 ↑) |
| 결과 타입 | `ResultSet` (행/열) | 엔티티 객체 리스트 |

> **핵심**: JPQL 은 테이블을 모르고 **엔티티 객체**만 압니다. 따라서 `Member m` 은 DB 테이블명이 아니라 `Member` 클래스명입니다. 실제 SQL 변환은 JPA 가 담당합니다.

---

## 2. 회원 서비스 개발

### 2.1 @Transactional 전략 — 클래스 레벨 readOnly + 메서드 레벨 오버라이드

이 패턴이 챕터 4의 **핵심 설계 포인트**입니다.

```java
// 클래스 레벨: 모든 public 메서드에 readOnly = true 기본 적용
@Transactional(readOnly = true)
public class MemberService {

    // 조회 메서드 → 클래스 레벨 readOnly = true 그대로 적용됨
    public List<Member> findMembers() { ... }
    public Member findOne(Long memberId) { ... }

    // 쓰기 메서드 → 메서드 레벨 @Transactional 로 오버라이드 (readOnly = false 가 기본값)
    @Transactional
    public Long join(Member member) { ... }
}
```

**왜 클래스 레벨에 readOnly = true 를 걸고 쓰기 메서드만 오버라이드하는가?**

조회 메서드가 쓰기 메서드보다 훨씬 많은 일반적인 서비스의 경우, 기본값을 `readOnly = true` 로 설정하는 것이 더 효율적입니다.

`readOnly = true` 의 효과:
- JPA 영속성 컨텍스트에서 **변경 감지(Dirty Checking)** 를 수행하지 않아 성능 최적화
- **플러시(Flush)** 를 생략하여 DB 부하 감소
- DB 드라이버 레벨에서도 읽기 전용 최적화 가능 (MySQL, Oracle 등)

```
쓰기가 많다면 → 클래스 레벨에 @Transactional (readOnly = false 기본)
               → 조회 메서드에만 @Transactional(readOnly = true) 추가

조회가 많다면 → 클래스 레벨에 @Transactional(readOnly = true)   ← 현재 패턴
               → 쓰기 메서드에만 @Transactional 추가
```

### 2.2 회원가입 구현 (join)

```java
@Transactional   // 클래스 레벨 readOnly = true 를 오버라이드
public Long join(Member member) {
    validateDuplicateMember(member);    // 중복 회원 검증
    memberRepository.save(member);
    return member.getId();
}
```

`memberRepository.save(member)` 내부적으로 `em.persist(member)` 를 호출합니다.
`@GeneratedValue` 전략에서는 `persist()` 호출 시 JPA 가 즉시 PK 를 할당합니다. 따라서 `member.getId()` 로 저장된 PK 를 바로 반환할 수 있습니다.

### 2.3 중복 회원 검증 (validateDuplicateMember)

```java
private void validateDuplicateMember(Member member) {
    List<Member> findMembers = memberRepository.findByName(member.getName());
    if (!findMembers.isEmpty()) {
        throw new IllegalStateException("이미 존재하는 회원입니다.");
    }
}
```

> **실무 주의사항**: 이 검증 로직만으로는 충분하지 않습니다.
>
> **시나리오**: 동시에 같은 이름 "kim" 으로 두 요청이 들어온다면?
> - 스레드 A: `findByName("kim")` → 결과 없음 (아직 B가 저장 전)
> - 스레드 B: `findByName("kim")` → 결과 없음 (아직 A가 저장 전)
> - 스레드 A: `save()` 성공
> - 스레드 B: `save()` 성공 ← 중복 통과!
>
> **해결책**: DB 레벨의 **유니크 제약 조건(Unique Constraint)** 을 반드시 추가해야 합니다. 비즈니스 로직 검증은 1차 방어선, DB 제약 조건은 최후의 방어선입니다.

### 2.4 조회 메서드 (findMembers / findOne)

```java
// 클래스 레벨 @Transactional(readOnly = true) 가 적용됨
public List<Member> findMembers() {
    return memberRepository.findAll();
}

public Member findOne(Long memberId) {
    return memberRepository.findOne(memberId);
}
```

별도의 `@Transactional` 을 붙이지 않으면 클래스 레벨의 `readOnly = true` 가 그대로 적용됩니다.

---

## 3. 의존성 주입 방식 비교

강의에서는 3가지 DI 방식을 순서대로 소개하고, 최종적으로 생성자 주입을 권장합니다.

### 3.1 필드 주입 (권장 안 함)

```java
@Autowired
private MemberRepository memberRepository;
```

| 항목 | 내용 |
|------|------|
| 장점 | 코드가 짧고 간결함 |
| 단점 1 | 테스트에서 mock 객체를 주입할 방법이 없음 (리플렉션 외) |
| 단점 2 | `final` 을 사용할 수 없어 불변성 보장 불가 |
| 단점 3 | DI 컨테이너 없이는 동작하지 않아 순수 단위 테스트 불가 |

### 3.2 Setter 주입 (권장 안 함)

```java
private MemberRepository memberRepository;

@Autowired
public void setMemberRepository(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
}
```

| 항목 | 내용 |
|------|------|
| 장점 | 테스트에서 setter 로 mock 주입 가능 |
| 단점 1 | 애플리케이션 실행 중 의존 객체가 변경될 수 있어 안전하지 않음 |
| 단점 2 | 애플리케이션 로딩 후에는 변경할 이유가 없는데 열려 있음 |

> 실제로 로딩 시점에 조립이 완료되면 런타임에 `memberRepository` 를 바꿀 이유가 없습니다. 변경 가능성을 열어두는 것은 오히려 위험합니다.

### 3.3 생성자 주입 (최종 권장)

```java
private final MemberRepository memberRepository;

@Autowired   // 생성자가 하나뿐이면 @Autowired 생략 가능
public MemberService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
}
```

| 항목 | 내용 |
|------|------|
| 장점 1 | `final` 로 불변성 보장 — 한번 주입되면 변경 불가 |
| 장점 2 | 컴파일 시점에 의존성 누락 감지 |
| 장점 3 | 테스트에서 생성자로 mock 을 명시적으로 주입해야 하므로 의존성을 놓치지 않음 |
| 장점 4 | DI 컨테이너 없이도 `new MemberService(mockRepo)` 처럼 순수 단위 테스트 가능 |

### 3.4 @RequiredArgsConstructor 활용

Lombok 의 `@RequiredArgsConstructor` 를 사용하면 `final` 필드만으로 생성자를 자동 생성합니다.

```java
@RequiredArgsConstructor        // ← Lombok: final 필드로 생성자 자동 생성
public class MemberService {

    private final MemberRepository memberRepository;  // 생성자 주입 자동화

    // 아래 생성자를 자동으로 만들어 줌:
    // public MemberService(MemberRepository memberRepository) {
    //     this.memberRepository = memberRepository;
    // }
}
```

> **팁**: `@AllArgsConstructor` 는 모든 필드로 생성자를 만들지만, `@RequiredArgsConstructor` 는 `final` 또는 `@NonNull` 필드만 선택합니다. 의존성이 명확하게 제어되므로 `@RequiredArgsConstructor` 를 권장합니다.

---

## 4. 회원 기능 테스트

### 4.1 통합 테스트 구성 (@SpringBootTest + @Transactional)

```java
@SpringBootTest   // 스프링 전체 컨텍스트 로딩 (실제 빈 사용)
@Transactional    // 테스트 후 자동 롤백 → 테스트 반복 실행 가능
public class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired EntityManager em;   // flush() 직접 호출 목적
}
```

**@SpringBootTest**
- 스프링 애플리케이션 전체 컨텍스트를 로딩하는 통합 테스트
- 실제 DB(또는 인메모리 DB)와 연동하여 JPA 전체 흐름을 검증

**@Transactional (테스트 클래스에 사용)**
- 각 테스트 메서드 실행 후 트랜잭션을 **자동 롤백**
- DB 상태가 테스트 전으로 복원되므로 테스트를 반복 실행해도 안전

> **주의**: 서비스 계층의 `@Transactional` 과 다릅니다. 테스트의 `@Transactional` 은 테스트가 끝나면 롤백하지만, 서비스의 `@Transactional` 은 정상 완료 시 커밋합니다.

### 4.2 Given-When-Then 패턴

각 테스트를 3구간으로 나누는 구조적 패턴입니다:

```
Given  → 테스트에 필요한 초기 상태/데이터 준비
When   → 실제 테스트 대상 동작 실행
Then   → 실행 결과 검증 (assertEquals, assertThrows 등)
```

### 4.3 회원가입 테스트 케이스

```java
@Test
public void 회원가입() throws Exception {
    // given
    Member member = new Member();
    member.setName("kim");

    // when
    Long savedId = memberService.join(member);

    // then
    assertEquals(member, memberRepository.findOne(savedId));
}
```

**테스트 분석:**
- `memberService.join(member)` 내부에서 `em.persist(member)` 가 호출되어 영속성 컨텍스트에 등록됨
- `memberRepository.findOne(savedId)` 는 `em.find()` 를 호출하지만, 같은 트랜잭션 내 영속성 컨텍스트(1차 캐시)에서 동일 객체를 반환
- 따라서 `member == memberRepository.findOne(savedId)` (동일 객체 비교) 가 `true`

> **왜 INSERT 쿼리가 안 보이는가?**
>
> `@Transactional` 로 인해 테스트 종료 시 롤백이 발생합니다. 스프링은 롤백이 예정되어 있으면 커밋 전 플러시를 생략하므로 INSERT SQL 이 실행되지 않습니다. 쿼리를 눈으로 확인하려면 아래 4.5를 참고하세요.

### 4.4 중복 회원 예외 테스트 케이스

```java
@Test
public void 중복_회원_예외() throws Exception {
    // given
    Member member1 = new Member();
    member1.setName("kim");

    Member member2 = new Member();
    member2.setName("kim");

    // when
    memberService.join(member1);
    try {
        memberService.join(member2);    // 이 줄에서 예외가 발생해야 한다!
    } catch (IllegalStateException e) {
        return;    // 예외 발생 → 테스트 성공
    }

    // then
    fail("예외가 발생해야 한다.");   // 예외 없이 여기까지 오면 테스트 실패
}
```

**이 구조가 중요한 이유:**
- `memberService.join(member2)` 가 예외를 던지면 `catch` 블록에서 `return` → 테스트 성공
- 예외가 발생하지 않으면 `then` 구간의 `fail()` 에 도달 → 테스트 실패
- `memberService.join(member1)` 만 작성하고 `fail()` 만 두면 member2 검증이 빠진 잘못된 테스트가 됨

### 4.5 쿼리 눈으로 확인하기 (em.flush + @Rollback(false))

**방법 1: em.flush() — 롤백은 유지, SQL 만 DB 로 전달**

```java
@Test
public void 회원가입() throws Exception {
    Member member = new Member();
    member.setName("kim");

    Long savedId = memberService.join(member);

    em.flush();  // 영속성 컨텍스트 → DB 강제 반영 (하지만 트랜잭션은 나중에 롤백)

    assertEquals(member, memberRepository.findOne(savedId));
}
```

**방법 2: @Rollback(false) — 실제 커밋 (DB 에 데이터 남음)**

```java
@Test
@Rollback(false)   // 커밋 강제 → DB 에 INSERT 데이터 남음
public void 회원가입() throws Exception {
    ...
}
```

| 방법 | SQL 확인 | DB 데이터 남음 |
|------|----------|--------------|
| `em.flush()` | O | X (트랜잭션 롤백) |
| `@Rollback(false)` | O | O (커밋) |

---

## 5. 테스트 환경 분리

### 5.1 왜 test/resources/application.yml 을 따로 두는가

```
src/
├── main/resources/application.yml   ← 운영/개발 환경 설정
└── test/resources/application.yml   ← 테스트 전용 설정 (우선순위 높음)
```

테스트는 다음 이유로 독립된 설정이 필요합니다:
- 운영 DB 를 건드리지 않아야 함 → 인메모리 DB 사용
- 테스트마다 초기화되어야 함 → `create-drop` 자동 적용
- 실행 속도가 빨라야 함 → 불필요한 외부 의존 제거

> **우선순위**: `test/resources/application.yml` 이 `main/resources/application.yml` 보다 우선 적용됩니다.

### 5.2 실제 설정 파일 내용

```yaml
# test/resources/application.yml

spring:
#  datasource:
#    url: jdbc:h2:mem:test
#    username: sa
#    password:
#    driver-class-name: org.h2.Driver
#
#  jpa:
#    hibernate:
#      ddl-auto: create-drop
#    properties:
#      hibernate:
#        format_sql: true

logging:
  level:
    org.hibernate.SQL: debug
    org.hibernate.orm.jdbc.bind: trace
```

datasource 와 jpa 설정을 모두 주석 처리했습니다. 의도적으로 비운 것입니다.

### 5.3 스프링 부트 인메모리 DB 자동 설정

`datasource` 설정이 없으면 **스프링 부트가 자동으로 H2 인메모리 DB 를 구성**합니다.

```
설정 없음 → Spring Boot AutoConfiguration
         → H2 인메모리 DB (jdbc:h2:mem:...)
         → 애플리케이션 종료 시 데이터 자동 소멸
```

이는 Spring Boot 의 **Auto Configuration** 기능입니다. H2 의존성이 클래스패스에 있으면 별도 설정 없이 인메모리 DB 를 연결합니다.

### 5.4 ddl-auto 동작 방식

| 값 | 동작 |
|----|------|
| `create` | 기존 테이블 DROP 후 CREATE (시작 시) |
| `create-drop` | `create` 와 동일하나 **종료 시 DROP** 추가 |
| `update` | 변경된 스키마만 반영 (데이터 유지) |
| `validate` | 스키마 일치 여부만 검증 (변경 없음) |
| `none` | DDL 자동 실행 없음 |

테스트 환경에서 `datasource` 설정을 제거하면 스프링 부트는 H2 인메모리 DB + `create-drop` 을 기본값으로 자동 설정합니다.

---

## 6. JUnit4 → JUnit5 변경사항

강의 영상은 JUnit4 를 기준으로 작성되었지만, 이 프로젝트는 Spring Boot 4.x 를 사용하므로 JUnit5(Jupiter) 가 기본입니다.

| 항목 | JUnit4 (영상 기준) | JUnit5 (이 프로젝트) |
|------|-------------------|---------------------|
| 클래스 어노테이션 | `@RunWith(SpringRunner.class)` | 불필요 (제거) |
| @Test import | `org.junit.Test` | `org.junit.jupiter.api.Test` |
| Assertions import | `org.junit.Assert.*` | `org.junit.jupiter.api.Assertions.*` |
| 예외 검증 | `@Test(expected = IllegalStateException.class)` | `assertThrows(IllegalStateException.class, () -> {...})` |
| @Rollback import | `org.springframework.test.annotation.Rollback` | 동일 (변경 없음) |

**이 프로젝트에서 중복 예외를 JUnit5 방식으로 작성하면:**

```java
// JUnit4 방식 (이 프로젝트에서 사용 불가)
@Test(expected = IllegalStateException.class)
public void 중복_회원_예외_junit4() throws Exception {
    memberService.join(member1);
    memberService.join(member2);
}

// JUnit5 방식 (assertThrows 사용)
@Test
public void 중복_회원_예외_junit5() {
    memberService.join(member1);
    assertThrows(IllegalStateException.class, () -> memberService.join(member2));
}

// 또는 이 프로젝트처럼 try-catch 방식 (명시적이어서 이해하기 쉬움)
@Test
public void 중복_회원_예외() throws Exception {
    memberService.join(member1);
    try {
        memberService.join(member2);
    } catch (IllegalStateException e) {
        return;
    }
    fail("예외가 발생해야 한다.");
}
```

---

## 7. Best Practice 및 주의사항

### 의존성 주입

- **항상 생성자 주입을 사용하라** — `final` 필드 + `@RequiredArgsConstructor` 조합이 가장 안전하고 간결합니다.
- 필드 주입과 Setter 주입은 피하세요. 테스트 어려움, 불변성 위반, 런타임 오류 가능성이 있습니다.

### 트랜잭션 설계

- **서비스 클래스에 `@Transactional(readOnly = true)` 를 기본으로** — 조회가 많은 서비스라면 성능상 유리합니다.
- 쓰기 메서드에만 `@Transactional` (readOnly = false) 을 명시적으로 오버라이드하세요.
- JPA 의 모든 데이터 변경은 트랜잭션 안에서 실행되어야 합니다.

### 중복 검증

- 비즈니스 로직 중복 검증(`validateDuplicateMember`) + **DB 유니크 제약 조건** 을 반드시 함께 사용하세요.
- 멀티 스레드 환경에서 로직 검증만으로는 Race Condition 이 발생할 수 있습니다.

### 테스트

- **테스트용 `application.yml` 을 반드시 분리하세요.** 운영 DB 와 테스트 DB 는 격리되어야 합니다.
- `@SpringBootTest + @Transactional` 조합으로 테스트 후 자동 롤백을 활용하세요.
- `em.flush()` 는 롤백을 유지하면서 SQL 만 확인할 때 사용합니다.
- `@Rollback(false)` 는 실제 커밋이 필요할 때(쿼리 눈으로 확인, 실제 DB 검증) 사용합니다.

### JPQL

- JPQL 은 테이블이 아닌 엔티티를 대상으로 합니다.
- 파라미터 바인딩은 이름 기반(`:name`)을 사용하세요. 위치 기반(`?`)보다 가독성과 유지보수성이 좋습니다.

---

## 8. 부록

### 8.1 핵심 용어 정리

| 용어 | 설명 |
|------|------|
| EntityManager | JPA 의 핵심 인터페이스. 영속성 컨텍스트를 관리하고 엔티티 CRUD 를 수행 |
| 영속성 컨텍스트 | EntityManager 가 관리하는 엔티티의 생명주기 공간. 1차 캐시 역할 |
| JPQL | Java Persistence Query Language. SQL 과 유사하나 테이블이 아닌 엔티티 객체를 대상으로 함 |
| readOnly 트랜잭션 | 변경 감지, 플러시를 생략하여 성능 최적화. 조회 전용 트랜잭션 |
| Flush | 영속성 컨텍스트의 변경사항을 DB 에 반영하는 동작. 커밋 시 자동 실행 |
| 변경 감지 (Dirty Checking) | JPA 가 스냅샷과 현재 엔티티 상태를 비교하여 변경된 필드를 자동 UPDATE 하는 기능 |
| Race Condition | 동시 요청 시 검증 로직을 통과해 중복 데이터가 생기는 멀티 스레드 문제 |

### 8.2 어노테이션 정리

| 어노테이션 | 패키지 | 역할 |
|-----------|--------|------|
| `@Repository` | `org.springframework.stereotype` | 컴포넌트 스캔 + JPA 예외 변환 |
| `@PersistenceContext` | `jakarta.persistence` | EntityManager 표준 주입 방식 |
| `@Service` | `org.springframework.stereotype` | 컴포넌트 스캔 + 비즈니스 계층 명시 |
| `@Transactional` | `org.springframework.transaction.annotation` | 트랜잭션 경계 설정 |
| `@RequiredArgsConstructor` | `lombok` | final 필드 기반 생성자 자동 생성 |
| `@SpringBootTest` | `org.springframework.boot.test.context` | 스프링 전체 컨텍스트 통합 테스트 |
| `@Transactional` (테스트) | `org.springframework.transaction.annotation` | 테스트 후 자동 롤백 |
| `@Rollback` | `org.springframework.test.annotation` | 롤백 동작 제어 |

### 8.3 학습 확인 질문

1. `@Repository` 어노테이션이 JPA 예외를 스프링 예외로 변환하는 원리는 무엇인가?
2. `@PersistenceContext` 대신 `@Autowired` 로 EntityManager 주입이 가능한 이유는?
3. JPQL 에서 `:name` 파라미터 바인딩을 사용하는 이유는 위치 기반(`?`) 대비 어떤 장점이 있는가?
4. `@Transactional(readOnly = true)` 가 성능을 개선하는 구체적인 메커니즘은?
5. 필드 주입 대신 생성자 주입을 권장하는 이유를 테스트 관점에서 설명하라.
6. 중복 회원 검증을 서비스 계층에서 하면서도 DB 유니크 제약 조건이 필요한 이유는?
7. 테스트에서 `@Transactional` 과 `@Rollback(false)` 를 동시에 사용하면 어떻게 동작하는가?
8. `em.flush()` 와 트랜잭션 커밋의 차이점은 무엇인가?

### 8.4 다음 챕터 예고 — 챕터 5: 상품 도메인 개발

챕터 5에서는 `Item` 도메인을 구현합니다. 회원 도메인과 달리 **추상 클래스 상속 구조**가 핵심입니다.

```
Item (추상 클래스)
├── Book
├── Album
└── Movie
```

주요 학습 내용:
- `ItemRepository`, `ItemService` 구현 (회원 도메인과 동일한 패턴)
- `Item` 추상 클래스와 `@Inheritance` 전략 이해
- **재고 수량 변경 비즈니스 로직** — 도메인 모델 패턴으로 엔티티에 로직 배치
- `addStock()` / `removeStock()` 메서드와 재고 부족 예외 처리
