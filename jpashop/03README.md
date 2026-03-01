# 3. 애플리케이션 구현 준비

> 무엇을 만들지(요구사항)와 어떻게 만들지(아키텍처)를 정의하는 구현 설계도

## 📚 강의 출처

**인프런 - 김영한의 "실전! 스프링 부트와 JPA 활용 1 - 웹 애플리케이션 개발"**

이 문서는 강의의 세 번째 챕터인 "애플리케이션 구현 준비"를 학습하며 작성한 노트입니다.
챕터 2에서 완성된 도메인 모델을 바탕으로, **실제 구현에 착수하기 전에 요구사항과 아키텍처를 명확히 정의**하는 단계를 다룹니다.
코드 작성보다 선행되는 이 설계 단계가 전체 개발의 뼈대가 됩니다.

**챕터 간 흐름:**

| 항목 | 02 - 도메인 분석 설계 | 03 - 애플리케이션 구현 준비 | 04+ - 본격 구현 |
|------|----------------------|---------------------------|----------------|
| **목표** | 엔티티 모델 완성 | 요구사항 + 아키텍처 확정 | 기능별 코드 작성 |
| **핵심 질문** | "어떤 데이터 구조를 쓸 것인가?" | "어떤 기능을 어떤 구조로 만들 것인가?" | "어떻게 코드로 구현하는가?" |
| **결과물** | 16개 엔티티 클래스 | 기능 목록 + 계층 아키텍처 | 레포지토리/서비스/웹 계층 |

---

## 🎯 학습 목표

이 문서를 통해 다음을 이해합니다:

1. **구현 범위 확정**
   - 쇼핑몰(HELLO SHOP)의 구체적인 기능 목록 정의
   - 학습 효율을 위해 단순화할 기능과 그 이유

2. **계층형 아키텍처 이해**
   - Controller → Service → Repository → DB 흐름
   - Domain이 모든 계층을 관통하는 이유

3. **패키지 구조 설계**
   - 각 계층을 패키지로 분리하는 이유
   - 실제 `jpabook.jpashop` 하위 패키지 구성

4. **개발 순서와 이유**
   - Service/Repository 계층을 먼저 개발하는 이유
   - 테스트를 통한 검증 우선 전략

5. **도메인과 계층의 연결**
   - 챕터 2에서 만든 엔티티가 각 계층에서 어떻게 활용되는지

---

## 🗺️ 학습 로드맵

이 챕터는 **요구사항 → 아키텍처 → 패키지 → 개발 순서** 순으로 구성됩니다.

```
1. 구현 요구사항 확정
   - 회원/상품/주문 기능 목록 나열
   - 단순화 범위 결정 (로그인/권한/검증 등 제외)
   ↓
2. 애플리케이션 아키텍처 설계
   - 계층형 구조 (Controller → Service → Repository)
   - 각 계층의 책임 정의
   ↓
3. 패키지 구조 확정
   - domain, repository, service, web, exception 분리
   ↓
4. 개발 순서 결정
   - Service/Repository → 테스트 → Web 계층 순서
   - 왜 이 순서인지 근거 확인
   ↓
5. 도메인과 계층의 연결 이해
   - 챕터 2 엔티티가 각 계층에서 어떻게 사용되는지 미리보기
```

---

## 📖 목차

1. [구현 요구사항](#1-구현-요구사항)
   - [1.1 전체 기능 목록](#11-전체-기능-목록)
   - [1.2 단순화 범위](#12-단순화-범위)
   - [1.3 왜 단순화하는가](#13-왜-단순화하는가)
2. [애플리케이션 아키텍처](#2-애플리케이션-아키텍처)
   - [2.1 계층형 구조](#21-계층형-구조)
   - [2.2 패키지 구조](#22-패키지-구조)
   - [2.3 개발 순서와 이유](#23-개발-순서와-이유)
3. [도메인 모델과 계층 관계](#3-도메인-모델과-계층-관계)
4. [Best Practice 및 주의사항](#4-best-practice-및-주의사항)
5. [부록](#5-부록)

---

## 1. 구현 요구사항

### 1.1 전체 기능 목록

이번 강의에서 구현할 HELLO SHOP의 기능은 크게 세 가지 도메인으로 분류됩니다.

| 도메인 | 기능 | 화면 URL |
|--------|------|---------|
| **회원** | 회원 등록 | `/members/new` |
| **회원** | 회원 조회 | `/members` |
| **상품** | 상품 등록 | `/items/new` |
| **상품** | 상품 수정 | `/items/{id}/edit` |
| **상품** | 상품 조회 | `/items` |
| **주문** | 상품 주문 | `/order` |
| **주문** | 주문 내역 조회 | `/orders` |
| **주문** | 주문 취소 | `/orders/{id}/cancel` |

**화면 구성 (HELLO SHOP 기준):**

```
홈 화면 (/)
├── 회원 메뉴
│   ├── 회원 등록
│   └── 회원 목록
├── 상품 메뉴
│   ├── 상품 등록
│   └── 상품 목록
│       └── (각 상품 수정 버튼)
└── 주문 메뉴
    ├── 상품 주문
    └── 주문 목록
        └── (주문 취소 버튼)
```

---

### 1.2 단순화 범위

실무에서는 필요하지만, 이 강의에서는 **JPA 핵심 학습에 집중**하기 위해 아래 기능을 제외합니다.

| 제외 항목 | 실무에서의 의미 | 제외 이유 |
|----------|---------------|---------|
| 로그인/권한 관리 | Spring Security, JWT/Session | JPA와 무관한 보안 기술 영역 |
| 파라미터 검증 | `@Valid`, BindingResult, 예외 처리 | 입력 처리 로직이 JPA 학습을 가림 |
| 예외 처리 | `@ExceptionHandler`, GlobalExceptionHandler | 에러 처리 체계는 별도 주제 |
| 상품 다양성 | Book, Album, Movie 전부 구현 | Book 하나로 상속 구조 학습 충분 |
| 카테고리 기능 | 계층형 카테고리 탐색 | `@ManyToMany` 실습은 설계에서 완료 |
| 배송 상세 정보 | 배송지 변경, 배송 상태 추적 | 핵심 흐름(주문-배송 관계)만 필요 |

**구현 대상:**
- 상품 종류: **Book(도서)만** 등록/수정/조회
- 연관관계 중 Category, Delivery는 **엔티티 구조는 존재**하지만 기능 구현은 최소화

---

### 1.3 왜 단순화하는가

> 복잡성을 줄여야 핵심을 볼 수 있다.

강의의 핵심 주제는 **"JPA를 활용한 비즈니스 로직 구현"** 입니다.
만약 로그인, 검증, 예외 처리까지 모두 구현하면:

- 코드량이 3~5배 증가
- 스프링 시큐리티, Bean Validation 등 별도 학습 필요
- JPA 관련 코드(연관관계 매핑, 영속성 컨텍스트, 지연 로딩)가 부각되지 않음

**단순화는 포기가 아니라 집중입니다.**
핵심 흐름(요청 → 비즈니스 로직 → DB)을 깨끗하게 보여주기 위한 의도적 선택입니다.

---

## 2. 애플리케이션 아키텍처

### 2.1 계층형 구조

이 프로젝트는 **계층형 아키텍처(Layered Architecture)** 를 따릅니다.
각 계층은 자신의 바로 아래 계층에만 의존하며, 역할이 명확히 분리됩니다.

```
  [ 클라이언트 (브라우저) ]
          ↕ HTTP
  ┌──────────────────────────────────┐
  │         Controller 계층           │  ← 요청 수신 / 응답 반환 / 뷰 선택
  │         (web 패키지)              │
  └─────────────┬────────────────────┘
                ↓ 메서드 호출
  ┌──────────────────────────────────┐
  │          Service 계층             │  ← 비즈니스 로직 / 트랜잭션 관리
  │         (service 패키지)          │
  └─────────────┬────────────────────┘
                ↓ 메서드 호출
  ┌──────────────────────────────────┐
  │         Repository 계층           │  ← JPA (EntityManager) / DB 접근
  │        (repository 패키지)        │
  └─────────────┬────────────────────┘
                ↓ SQL
  ┌──────────────────────────────────┐
  │              DB (H2)              │
  └──────────────────────────────────┘

  ※ Domain(엔티티)은 모든 계층에서 공통으로 사용
     (Controller → Service → Repository 어디서나 Member, Order 등 사용)
```

**각 계층의 책임:**

| 계층 | 패키지 | 핵심 책임 | 주요 어노테이션 |
|------|--------|----------|--------------|
| **Controller** | `web` | HTTP 요청 파싱, Model에 데이터 담기, View 이름 반환 | `@Controller`, `@GetMapping`, `@PostMapping` |
| **Service** | `service` | 비즈니스 규칙 실행, 여러 Repository 조합, 트랜잭션 경계 | `@Service`, `@Transactional` |
| **Repository** | `repository` | EntityManager를 통한 CRUD, JPQL 작성 | `@Repository`, `EntityManager` |
| **Domain** | `domain` | 비즈니스 데이터와 핵심 로직 캡슐화 | `@Entity`, `@Embeddable` |

---

### 2.2 패키지 구조

```
src/main/java/jpabook/jpashop/
├── JpashopApplication.java          ← 스프링 부트 진입점
│
├── domain/                          ← 엔티티 (모든 계층 공통 사용)
│   ├── Member.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── OrderStatus.java             (enum)
│   ├── Address.java                 (@Embeddable 값 타입)
│   ├── Delivery.java
│   ├── DeliveryStatus.java          (enum)
│   ├── Category.java
│   └── item/                        ← 상품 상속 구조
│       ├── Item.java                (추상 클래스, @Inheritance)
│       ├── Book.java
│       ├── Album.java
│       └── Movie.java
│
├── repository/                      ← JPA 데이터 접근 계층
│   ├── MemberRepository.java
│   ├── OrderRepository.java
│   └── ItemRepository.java
│
├── service/                         ← 비즈니스 로직 계층
│   ├── MemberService.java
│   ├── OrderService.java
│   └── ItemService.java
│
├── web/                             ← 웹 계층 (Controller)
│   ├── HomeController.java
│   ├── MemberController.java
│   ├── ItemController.java
│   └── OrderController.java
│
└── exception/                       ← 도메인 예외 (최소화)
    └── NotEnoughStockException.java
```

**왜 이렇게 패키지를 나누는가?**

| 원칙 | 설명 |
|------|------|
| **관심사 분리(SoC)** | 화면 처리/비즈니스 로직/DB 접근을 섞으면 변경 시 영향 범위가 커짐 |
| **단일 책임(SRP)** | Controller는 요청만, Service는 로직만, Repository는 DB만 담당 |
| **테스트 용이성** | Service 계층만 단독 테스트 가능 (Controller/DB 없이) |
| **유지보수성** | 기능 추가/수정 시 어느 파일을 찾아야 하는지 명확 |

---

### 2.3 개발 순서와 이유

강의에서는 아래 순서로 개발을 진행합니다.

```
1단계: Service + Repository 구현
       ↓
2단계: 단위 테스트 (JUnit) 작성 및 검증
       ↓
3단계: Web 계층 (Controller + Thymeleaf) 구현
       ↓
4단계: 통합 테스트
```

**왜 Web 계층을 마지막에 만드는가?**

```
❌ 잘못된 순서 (Top-Down):
   Controller 먼저 → 화면은 되지만 비즈니스 로직 검증 불가
   → Controller 없이는 테스트가 어려움
   → 브라우저로만 테스트 → 느리고 반복하기 어려움

✅ 올바른 순서 (Bottom-Up):
   Service/Repository 먼저 → JUnit으로 빠른 단위 테스트 가능
   → 비즈니스 로직 검증 후 Controller 연결
   → 이미 검증된 서비스를 Controller가 그냥 호출
```

**Service 계층을 먼저 개발하면:**

1. **Controller가 없어도 테스트 가능** - `@SpringBootTest` + `@Transactional`로 순수 비즈니스 로직만 검증
2. **빠른 피드백 사이클** - 브라우저 실행 없이 JUnit으로 수 초 내 결과 확인
3. **책임 명확화** - 로직이 Service에 있으면 Controller는 단순 파라미터 전달자가 됨
4. **재사용성** - 동일 Service를 REST API Controller와 Thymeleaf Controller 모두 재사용 가능

---

## 3. 도메인 모델과 계층 관계

챕터 2에서 완성된 엔티티 클래스들이 각 계층에서 어떻게 활용될지 미리 정리합니다.

```
  Domain 엔티티         Repository         Service              Controller
  ────────────         ──────────         ───────              ──────────
  Member          →  MemberRepository  →  MemberService   →  MemberController
  Order           →  OrderRepository   →  OrderService    →  OrderController
  Item (Book)     →  ItemRepository    →  ItemService     →  ItemController
```

**계층을 관통하는 데이터 흐름 예시 (회원 등록):**

```
[브라우저] POST /members/new (form data)
    ↓
[MemberController] MemberForm 파라미터 수신
    → memberService.join(member) 호출
    ↓
[MemberService] 중복 회원 검증 (비즈니스 로직)
    → memberRepository.save(member) 호출
    ↓
[MemberRepository] EntityManager.persist(member)
    ↓
[DB] INSERT INTO member ...
    ↓
[MemberController] redirect:/members 반환
```

**핵심 포인트:** `Member` 엔티티 객체가 Controller → Service → Repository를 거쳐 DB까지 전달되며,
계층마다 엔티티를 그대로 사용합니다 (이 강의 범위에서는 DTO 변환 최소화).

---

## 4. Best Practice 및 주의사항

### 계층 간 의존 방향 원칙

```
✅ 허용: Controller → Service → Repository
❌ 금지: Repository → Service (역방향 의존)
❌ 금지: Service → Controller (역방향 의존)
```

계층 간 의존이 역방향이 되면 계층 분리의 의미가 없어지고 순환 참조 위험이 생깁니다.

### Domain 로직의 위치

```java
// ❌ Service에 비즈니스 로직을 몰아넣는 방식 (트랜잭션 스크립트 패턴)
public void cancelOrder(Long orderId) {
    Order order = orderRepository.findOne(orderId);
    order.setStatus(OrderStatus.CANCEL);  // Service에서 직접 상태 변경
    for (OrderItem orderItem : order.getOrderItems()) {
        orderItem.getItem().addStock(orderItem.getCount());  // 재고 복구도 Service에서
    }
}

// ✅ 도메인 엔티티에 핵심 로직을 두는 방식 (도메인 모델 패턴)
public void cancelOrder(Long orderId) {
    Order order = orderRepository.findOne(orderId);
    order.cancel();  // 취소 로직은 Order 엔티티 내부에 캡슐화
}
```

이 강의는 **도메인 모델 패턴**을 지향합니다. 핵심 비즈니스 로직(재고 증감, 주문 취소 상태 변경 등)은
Service가 아닌 **엔티티 내부 메서드**로 구현합니다.

### @Transactional 위치

```java
@Service
@Transactional(readOnly = true)  // 기본은 읽기 전용 (성능 최적화)
public class MemberService {

    public List<Member> findMembers() { ... }  // 읽기 전용 트랜잭션

    @Transactional  // 쓰기 작업은 readOnly = false (기본값)
    public Long join(Member member) { ... }
}
```

- `readOnly = true` : 더티 체킹(변경 감지) 비활성화 → 성능 향상
- 쓰기 메서드에는 `@Transactional` (readOnly = false)을 별도로 붙임

### 엔티티를 Controller에서 직접 쓰는 것의 한계

이 강의에서는 엔티티를 Controller까지 직접 전달하지만, 실무에서는 DTO를 사용해야 합니다.

| 방식 | 강의에서의 사용 | 실무 권장 |
|------|--------------|---------|
| 엔티티 직접 반환 | ✅ 단순함, 학습 집중 | ❌ API 스펙 노출, 양방향 관계 직렬화 문제 |
| DTO 변환 | - | ✅ API 스펙 분리, 불필요 데이터 제외 |

---

## 5. 부록

### 핵심 용어 정리

| 용어 | 설명 |
|------|------|
| **계층형 아키텍처** | 역할을 계층(Layer)으로 분리하는 설계 패턴. 각 계층은 인접 계층에만 의존 |
| **도메인 모델 패턴** | 비즈니스 로직을 엔티티(도메인 객체) 내부에 두는 방식 |
| **트랜잭션 스크립트** | 비즈니스 로직을 Service에 절차적으로 작성하는 방식 |
| **@Transactional** | 해당 메서드를 하나의 트랜잭션으로 묶음. 예외 발생 시 자동 롤백 |
| **JPQL** | JPA에서 사용하는 객체지향 쿼리 언어. SQL과 유사하나 엔티티/필드 이름 사용 |

### 챕터별 구현 계획 미리보기

| 챕터 | 구현 대상 | 핵심 기술 |
|------|----------|---------|
| **04** | 회원 도메인 (Repository + Service + Test) | EntityManager, JPQL, @Transactional |
| **05** | 상품 도메인 (Repository + Service) | 상속 매핑 활용, 재고 관리 로직 |
| **06** | 주문 도메인 (Repository + Service) | 복잡한 연관관계 활용, 도메인 로직 |
| **07** | 웹 계층 (Controller + Thymeleaf) | MVC, Form 객체, PRG 패턴 |

### 참고 자료

- [Spring 공식 문서 - MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [JPA 공식 문서 (Jakarta EE)](https://jakarta.ee/specifications/persistence/)
- 강의 PDF: `docs/3. 애플리케이션 구현 준비.pdf`
