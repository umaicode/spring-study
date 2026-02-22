# 프로젝트 환경설정

> Spring Boot + JPA 기반 쇼핑몰 프로젝트의 첫걸음 — 의존성 설정과 Lombok 동작 확인

---

> **⚠️ 임시 기록 안내**
>
> 이 문서는 챕터 1이 완전히 완료되기 전 임시로 작성된 학습 기록입니다.
> 다음 강의에서 일부 코드(Lombok 테스트용 `Hello.java`, `main()` 내 테스트 코드 등)가 삭제될 수 있어
> 현재 상태를 기록해둡니다.
>
> 챕터 완료 후 정식 README로 대체될 예정입니다.

---

## 📚 강의 출처

**인프런 - 김영한의 "실전! 스프링 부트와 JPA 활용 1 - 웹 애플리케이션 개발"**

이 문서는 강의의 "프로젝트 환경설정" 섹션을 학습하며 작성한 노트입니다.
`start.spring.io`로 프로젝트를 생성하고, 핵심 의존성을 파악한 뒤, Lombok이 정상 동작하는지 확인하는 것까지 다룹니다.
Hello-spring 입문 강의와 달리 이번에는 **JPA + H2** 가 핵심 스택으로 추가됩니다.

---

## 🎯 학습 목표

이 문서를 통해 다음을 이해하고 실습합니다:

1. **프로젝트 생성 - start.spring.io 설정**
   - Group, Artifact, Java 버전, Spring Boot 버전 설정
   - 필요한 의존성 선택 (JPA, H2, Thymeleaf, Validation, Lombok 등)

2. **라이브러리 살펴보기 - build.gradle 분석**
   - 각 스타터(Starter)가 어떤 라이브러리를 포함하는지
   - `implementation` / `compileOnly` / `runtimeOnly` / `annotationProcessor` 차이

3. **Lombok 동작 확인**
   - `@Getter` / `@Setter` 어노테이션이 하는 일
   - 컴파일 타임에 바이트코드 레벨에서 메서드를 자동 생성하는 원리
   - `Hello.java` + `JpashopApplication.java` 테스트 코드로 확인

4. **application.properties 기본 설정**
   - 현재 최소 설정 상태 파악
   - 향후 추가될 H2 / JPA 설정 예고

---

## 🗺️ 학습 로드맵

이 챕터는 **프로젝트 생성 → 의존성 파악 → 동작 확인** 순으로 구성됩니다.

```
[챕터 1 학습 흐름]

1. 프로젝트 생성
   start.spring.io → 의존성 선택 → IntelliJ에서 열기
          ↓
2. build.gradle 분석
   스타터 라이브러리 목록 확인 → 각 의존성의 역할 파악
          ↓
3. Lombok 설정 확인
   Hello.java (@Getter @Setter) → JpashopApplication main()에서 동작 테스트
          ↓
4. 기본 설정 확인
   application.properties 현재 상태 파악
   → H2 / JPA 설정은 다음 챕터부터 추가 예정
```

**왜 이 순서인가?**
- **토대 먼저**: 코드를 짜기 전에 어떤 라이브러리가 들어있는지 먼저 파악합니다.
- **Lombok 검증**: JPA 엔티티에서 Lombok을 많이 쓰게 되므로, 초반에 동작 여부를 확인합니다.

---

## 📖 목차

1. [프로젝트 생성](#1-프로젝트-생성)
2. [라이브러리 살펴보기](#2-라이브러리-살펴보기)
3. [Lombok 설정 확인](#3-lombok-설정-확인)
4. [프로젝트 기본 설정](#4-프로젝트-기본-설정)
5. [Best Practice 및 주의사항](#5-best-practice-및-주의사항)
6. [부록](#6-부록)

---

## 1. 프로젝트 생성

### 1.1 start.spring.io 설정

| 항목 | 값 |
|------|-----|
| **Project** | Gradle - Groovy |
| **Language** | Java |
| **Spring Boot** | 4.0.3 |
| **Group** | `jpabook` |
| **Artifact** | `jpashop` |
| **Packaging** | Jar |
| **Java** | 17 |

**추가한 의존성:**

| 의존성 | 역할 |
|--------|------|
| `Spring Data JPA` | JPA + Hibernate 스타터 |
| `H2 Database` | 인메모리 DB (개발/테스트용) |
| `Thymeleaf` | 서버사이드 템플릿 엔진 |
| `Validation` | Bean Validation (`@NotNull` 등) |
| `Spring Web MVC` | Spring MVC 웹 레이어 |
| `Lombok` | 보일러플레이트 코드 자동 생성 |

### 1.2 hello-spring과의 차이점

```
[hello-spring 입문]                    [jpashop 활용]
─────────────────                      ──────────────
JPA 없음 (JDBC/JdbcTemplate)  →        Spring Data JPA + Hibernate
H2 (단순 설정)                →        H2 + JPA 설정 (entity, DDL 등)
간단한 POJO 도메인             →        @Entity 기반 도메인 설계
```

---

## 2. 라이브러리 살펴보기

### 2.1 build.gradle 전체 코드

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.3'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'jpabook'
version = '0.0.1-SNAPSHOT'
description = 'Demo project for Spring Boot'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-h2console'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'com.h2database:h2'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-thymeleaf-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-validation-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### 2.2 핵심 라이브러리 역할

| 의존성 선언 | 역할 | 내부 포함 라이브러리 |
|------------|------|-------------------|
| `spring-boot-starter-data-jpa` | JPA/Hibernate 사용 | Hibernate, Spring Data JPA, JDBC |
| `spring-boot-h2console` | H2 웹 콘솔 제공 | H2 Console Servlet |
| `spring-boot-starter-thymeleaf` | 템플릿 엔진 | Thymeleaf, Spring 통합 |
| `spring-boot-starter-validation` | 입력값 검증 | Hibernate Validator, Jakarta Validation |
| `spring-boot-starter-webmvc` | 웹 MVC | Spring MVC, Tomcat |
| `lombok` (compileOnly) | 코드 생성 | 컴파일 타임만 필요 (런타임 불필요) |
| `h2` (runtimeOnly) | H2 DB 드라이버 | 런타임에만 필요 |

### 2.3 의존성 범위(Scope) 설명

```
implementation    → 컴파일 + 런타임 모두 필요 (일반 스타터)
compileOnly       → 컴파일 타임에만 필요 (런타임 클래스패스에 없음)
                   Lombok: 컴파일 시 코드 생성 후 어노테이션 프로세서 불필요
runtimeOnly       → 런타임에만 필요 (컴파일 클래스패스에 없음)
                   H2 드라이버: 실행할 때만 필요
annotationProcessor → 컴파일 타임 어노테이션 처리기 등록
                   Lombok의 @Getter/@Setter 등 처리
```

> **Lombok을 `compileOnly` + `annotationProcessor` 두 곳 모두에 선언하는 이유:**
> - `compileOnly`: 소스 코드에서 Lombok 어노테이션 타입 인식용 (컴파일 오류 방지)
> - `annotationProcessor`: 어노테이션을 처리하여 실제로 getter/setter 코드를 생성하는 프로세서 등록

---

## 3. Lombok 설정 확인

### 3.1 Hello.java - Lombok 동작 확인용 POJO

```java
package jpabook.jpashop;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Hello {
    private String data;
}
```

**`@Getter @Setter`가 하는 일:**

```
컴파일 전 (소스 코드)          컴파일 후 (바이트코드에 생성된 메서드)
───────────────────           ────────────────────────────────────
@Getter @Setter         →     public String getData() {
public class Hello {               return this.data;
    private String data;      }
}                             public void setData(String data) {
                                   this.data = data;
                              }
```

- 소스 코드에는 `getData()`, `setData()` 메서드가 없지만
- 컴파일된 `.class` 파일에는 자동으로 생성되어 있습니다.
- 즉, **런타임에는 일반 getter/setter와 완전히 동일하게 동작**합니다.

### 3.2 JpashopApplication.java - Lombok 테스트 코드

```java
package jpabook.jpashop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JpashopApplication {

    public static void main(String[] args) {

        // lombok 확인 코드
        // lombok은 @Getter, @Setter를 통해 자동으로 만들어준다.
        // 기존의 getData(), setData()를 만들지 않아도 된다!
        Hello hello = new Hello();
        hello.setData("hello");
        String data = hello.getData();
        System.out.println("data = " + data);

        SpringApplication.run(JpashopApplication.class, args);
    }

}
```

**실행 결과 (콘솔):**

```
data = hello
```

> `Hello.java`에 `getData()`나 `setData()`를 직접 작성하지 않았는데도
> `hello.setData("hello")`, `hello.getData()` 호출이 정상 동작합니다.
> 이것이 Lombok 어노테이션 프로세서의 컴파일 타임 코드 생성입니다.

### 3.3 IntelliJ Lombok 플러그인 설정

Lombok이 IDE에서 정상 인식되려면 두 가지가 필요합니다:

| 설정 | 경로 |
|------|------|
| **Lombok 플러그인 설치** | IntelliJ → Plugins → "Lombok" 검색 후 설치 |
| **어노테이션 프로세싱 활성화** | Settings → Build → Compiler → Annotation Processors → Enable |

> 플러그인이 없거나 어노테이션 프로세싱이 꺼져 있으면,
> IDE에서는 `getData()`를 찾을 수 없다는 빨간 줄이 뜨지만 빌드는 되는 이상한 상황이 생깁니다.

---

## 4. 프로젝트 기본 설정

### 4.1 application.properties 현재 상태

```properties
spring.application.name=jpashop
```

현재는 애플리케이션 이름만 설정된 최소 상태입니다.

### 4.2 향후 추가 예정 설정 (다음 챕터부터)

다음 강의에서 H2 DB 연결 및 JPA 설정이 추가될 예정입니다:

```properties
# H2 데이터소스 설정 (예정)
spring.datasource.url=jdbc:h2:mem:jpashop
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate 설정 (예정)
spring.jpa.hibernate.ddl-auto=create
spring.jpa.properties.hibernate.show_sql=true
spring.jpa.properties.hibernate.format_sql=true

# H2 콘솔 활성화 (예정)
spring.h2.console.enabled=true
```

> 현재 코드에는 위 설정들이 없습니다. 다음 챕터에서 진행됩니다.

### 4.3 스프링 컨테이너 로드 테스트

```java
package jpabook.jpashop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JpashopApplicationTests {

    @Test
    void contextLoads() {
    }

}
```

`contextLoads()` 테스트는 스프링 컨테이너가 오류 없이 시작되는지 확인합니다.
테스트 본문이 비어 있더라도, `@SpringBootTest`가 붙어 있으므로 컨텍스트 로드에 실패하면 테스트가 실패합니다.

---

## 5. Best Practice 및 주의사항

### 5.1 Lombok 관련

```java
// ✅ 권장: 필요한 어노테이션만 정확히 명시
@Getter @Setter
public class Hello {
    private String data;
}

// ⚠️ 주의: @Data는 편하지만 JPA 엔티티에는 부작용이 있음
// @Data          → @Getter @Setter @ToString @EqualsAndHashCode @RequiredArgsConstructor 포함
// @EqualsAndHashCode가 JPA 엔티티에서 문제를 일으킬 수 있음 (다음 챕터에서 설명 예정)
```

### 5.2 스프링 부트 버전 주의

이번 강의는 `Spring Boot 4.0.3`을 사용합니다. 강의 원본 자료는 구버전(2.x, 3.x) 기준일 수 있으므로:

| 주의 사항 | 내용 |
|----------|------|
| 패키지 변경 | `javax.*` → `jakarta.*` (스프링 부트 3.x부터) |
| H2 콘솔 설정 | 버전별 설정 방식 차이 있을 수 있음 |
| Hibernate 버전 | Boot 4.x = Hibernate 6.x (쿼리 방언 변경) |

### 5.3 build.gradle에서 h2console 설정

```groovy
// H2 웹 콘솔은 별도 스타터로 추가됨
implementation 'org.springframework.boot:spring-boot-h2console'
// runtimeOnly 'com.h2database:h2' 는 드라이버만 (DB 자체)
runtimeOnly 'com.h2database:h2'
```

두 개가 분리된 이유:
- `h2console`: H2의 웹 기반 관리 콘솔 UI 제공
- `h2`: 실제 H2 Database 드라이버 (런타임에 JDBC 연결용)

---

## 6. 부록

### 6.1 프로젝트 구조 (현재 상태)

```
jpashop/
├── build.gradle                            ← 의존성 관리
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── jpabook/jpashop/
│   │   │       ├── Hello.java              ← Lombok 테스트용 (임시)
│   │   │       └── JpashopApplication.java ← 메인 클래스 + Lombok 확인 코드
│   │   └── resources/
│   │       └── application.properties     ← 최소 설정 (이름만)
│   └── test/
│       └── java/
│           └── jpabook/jpashop/
│               └── JpashopApplicationTests.java ← 컨텍스트 로드 테스트
└── 01README.md                             ← 이 문서 (임시 기록)
```

### 6.2 핵심 어노테이션 정리

| 어노테이션 | 위치 | 역할 |
|-----------|------|------|
| `@SpringBootApplication` | 클래스 | 스프링 부트 자동 설정 + 컴포넌트 스캔 시작점 |
| `@Getter` | 클래스/필드 | Lombok: 모든 필드(또는 해당 필드)의 getter 자동 생성 |
| `@Setter` | 클래스/필드 | Lombok: 모든 필드(또는 해당 필드)의 setter 자동 생성 |
| `@SpringBootTest` | 테스트 클래스 | 스프링 컨테이너 전체를 띄워 통합 테스트 실행 |

### 6.3 다음 챕터 예고

챕터 2에서는 **도메인 분석 설계**를 진행합니다:

```
[챕터 2 예정 내용]

도메인 모델 설계 (ER 다이어그램)
        ↓
@Entity 기반 엔티티 클래스 구현
  - Member, Order, OrderItem, Item, Delivery, Category
        ↓
연관관계 설정
  - @ManyToOne, @OneToMany, @ManyToMany
        ↓
테이블 설계 → 엔티티 설계 비교
```

### 6.4 학습 확인 질문

1. **Lombok의 `@Getter`가 동작하는 시점은 언제인가?**
   - 런타임이 아닌 **컴파일 타임**. 어노테이션 프로세서가 `.java` → `.class` 변환 시 getter 메서드를 바이트코드에 삽입

2. **`compileOnly`와 `runtimeOnly`의 차이는?**
   - `compileOnly`: 컴파일 시만 클래스패스에 존재, 배포 JAR에 포함 안 됨 (Lombok 등)
   - `runtimeOnly`: 컴파일 시 없고 실행 시에만 존재 (H2 드라이버 등)

3. **`@SpringBootApplication`이 하는 3가지 역할은?**
   - `@SpringBootConfiguration`: 스프링 설정 클래스 선언
   - `@EnableAutoConfiguration`: 자동 설정 활성화
   - `@ComponentScan`: 해당 패키지부터 컴포넌트 스캔 시작

4. **`contextLoads()` 테스트가 본문이 비어도 의미 있는 이유는?**
   - `@SpringBootTest`가 스프링 컨텍스트 전체를 로드하기 때문. 설정 오류나 빈 생성 실패가 있으면 이 테스트가 실패

---

*작성일: 2026-02-23*
*강의: 인프런 - 김영한의 실전! 스프링 부트와 JPA 활용 1*
*상태: ⚠️ 임시 기록 (챕터 1 진행 중)*
