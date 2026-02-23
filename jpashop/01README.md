# 프로젝트 환경설정

> Spring Boot + JPA 실전 프로젝트의 첫걸음 — 프로젝트 생성부터 H2 DB 연동, JPA 동작 확인까지

## 📚 강의 출처

**인프런 - 김영한의 "실전! 스프링 부트와 JPA 활용 1 - 웹 애플리케이션 개발"**

이 문서는 강의의 첫 번째 챕터인 "프로젝트 환경설정"을 학습하며 작성한 노트입니다. 스프링 입문 강의(hello-spring)에서 기본을 배웠다면, 이제는 실전 프로젝트로 JPA와 DB를 연동하며 진짜 웹 애플리케이션을 만드는 여정을 시작합니다.

**hello-spring vs jpashop 비교:**

| 항목 | hello-spring (입문) | jpashop (실전) |
|------|---------------------|----------------|
| **강의 목표** | 스프링 기초 익히기 | JPA 실전 활용 |
| **데이터 저장** | 메모리 (MemoryRepository) | H2 Database + JPA |
| **도메인 복잡도** | 단순 (Member만) | 복잡 (Member, Order, Item, Delivery 등) |
| **주요 기술** | Spring MVC, DI | JPA, 영속성 컨텍스트, 연관관계 |
| **프로젝트 규모** | 학습용 예제 | 실무 유사 프로젝트 |

---

## 🎯 학습 목표

이 문서를 통해 다음을 이해하고 실습합니다:

1. **프로젝트 생성 및 의존성 설정**
   - start.spring.io에서 Spring Boot 4.x 프로젝트 생성
   - JPA, Thymeleaf, H2, Lombok 등 필수 의존성 추가

2. **라이브러리 분석 (Gradle 의존성 이해)**
   - implementation, compileOnly, runtimeOnly, annotationProcessor 차이
   - 각 스타터가 포함하는 하위 라이브러리 목록

3. **Lombok 설정 및 동작 원리**
   - 어노테이션 프로세서의 컴파일 타임 코드 생성 원리
   - @Getter, @Setter의 실제 동작 확인

4. **View 환경 설정 (Thymeleaf + Spring MVC)**
   - 템플릿 엔진과 정적 컨텐츠의 차이
   - devtools를 활용한 핫 리로딩 설정

5. **H2 데이터베이스 설치 및 설정**
   - 파일 모드 vs TCP 모드 차이 이해
   - 웹 콘솔 접속 및 DB 연결 확인

6. **JPA와 DB 연동 (application.yml 설정)**
   - datasource, JPA, logging 설정 항목 이해
   - ddl-auto 옵션의 의미와 환경별 사용법

7. **첫 엔티티와 Repository 구현**
   - @Entity, @PersistenceContext의 역할
   - 영속성 컨텍스트와 1차 캐시 동작 확인
   - p6spy를 통한 쿼리 파라미터 로깅

---

## 🗺️ 학습 로드맵

이 문서는 **프로젝트 초기 설정 6단계**로 구성되어 있습니다.

```
1. 프로젝트 생성
   - start.spring.io에서 의존성 선택
   - build.gradle 구조 이해
   ↓
2. 라이브러리 분석
   - Gradle 의존성 트리 확인
   - 의존성 범위(scope) 학습
   ↓
3. Lombok 확인
   - Hello.java 작성
   - JpashopApplication에서 동작 테스트
   ↓
4. View 설정
   - HelloController + hello.html
   - devtools로 핫 리로딩 확인
   ↓
5. H2 설치
   - 파일 모드로 DB 파일 생성
   - TCP 모드로 전환
   ↓
6. JPA 동작 확인
   - Member 엔티티 작성
   - MemberRepository + 테스트
   - 영속성 컨텍스트 확인
```

**왜 이 순서인가?**

- **생성 → 분석**: 먼저 만들고, 구조를 이해하는 방식으로 학습
- **Lombok 먼저**: JPA 엔티티에서 @Getter, @Setter를 많이 사용하므로 미리 확인
- **View → DB → JPA 순차 연동**: 각 단계마다 "설치 → 동작 확인" 반복하며 점진적 학습
- **실전 중심**: 이론보다 실제 동작하는 코드를 먼저 보고, 나중에 깊이 있게 이해

---

## 📖 목차

1. [프로젝트 생성 및 설정](#1-프로젝트-생성-및-설정)
2. [라이브러리 살펴보기](#2-라이브러리-살펴보기)
3. [Lombok 설정 및 동작 확인](#3-lombok-설정-및-동작-확인)
4. [View 환경 설정](#4-view-환경-설정)
5. [H2 데이터베이스 설치](#5-h2-데이터베이스-설치)
6. [JPA와 DB 설정, 동작 확인](#6-jpa와-db-설정-동작-확인)
7. [Spring Boot 3.x/4.x 주요 변경사항](#7-spring-boot-3x4x-주요-변경사항)
8. [Best Practice 및 주의사항](#8-best-practice-및-주의사항)
9. [부록](#9-부록)

---

## 1. 프로젝트 생성 및 설정

### 1.1 start.spring.io 설정

[Spring Initializr](https://start.spring.io)에서 다음과 같이 설정합니다.

| 항목 | 값 |
|------|-----|
| **Project** | Gradle - Groovy |
| **Language** | Java |
| **Spring Boot** | 4.0.3 |
| **Group** | jpabook |
| **Artifact** | jpashop |
| **Name** | jpashop |
| **Description** | Demo project for Spring Boot |
| **Package name** | jpabook.jpashop |
| **Packaging** | Jar |
| **Java** | 17 |

**추가한 의존성 6가지:**

1. **Spring Web** (spring-boot-starter-webmvc)
   - Spring MVC, Tomcat 내장 서버

2. **Thymeleaf** (spring-boot-starter-thymeleaf)
   - 서버 사이드 템플릿 엔진

3. **Spring Data JPA** (spring-boot-starter-data-jpa)
   - JPA, Hibernate, Spring Data JPA

4. **H2 Database** (h2)
   - 경량 인메모리/파일 데이터베이스

5. **Lombok** (lombok)
   - @Getter, @Setter 등 보일러플레이트 코드 자동 생성

6. **Validation** (spring-boot-starter-validation)
   - Bean Validation (JSR-380) 구현체

### 1.2 build.gradle 전체 구조

```gradle
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

	// cache 이런 것도 다 없애고 reloading도 되게 만들어 준다!
	implementation 'org.springframework.boot:spring-boot-devtools'

	// logging을 더 좋게 만들고 싶다.
	// 이런 라이브러리는 개발 단계에서는 편한데 운영에 배포할 때는 좀 고민을 해야 한다.
	// 운영에도 다루고 나면 좋겠지라고 생각하지만 이게 성능 테스트를 꼭 해봐야 한다.
	// 이런 것들이 잘못하면 성능을 확 저하시킬 수 있다.
	// 받아 들일만 하면 운영에서 써도 상관 없지만 그렇지 않다면 개발 정도에서만 쓰는 것을 권장한다.
	implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:2.0.0'

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

### 1.3 빌드 및 실행 명령어

```bash
# 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 테스트 전체 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "jpabook.jpashop.MemberRepositoryTest"

# 빌드 결과물 제거
./gradlew clean
```

---

## 2. 라이브러리 살펴보기

### 2.1 Gradle 의존성 범위 (Dependency Scope)

Gradle에서 `build.gradle`의 `dependencies` 블록에 라이브러리를 추가할 때 사용하는 키워드의 의미를 이해해야 합니다.

| 의존성 범위 | 컴파일 타임 | 런타임 | 테스트 | 패키징 | 사용 예시 |
|------------|-----------|--------|--------|--------|----------|
| **implementation** | ✅ | ✅ | ✅ | ✅ (jar 포함) | Spring Boot Starter 대부분 |
| **compileOnly** | ✅ | ❌ | ❌ | ❌ (jar 제외) | Lombok (컴파일 후 불필요) |
| **runtimeOnly** | ❌ | ✅ | ✅ | ✅ (jar 포함) | H2 Database (JDBC 드라이버) |
| **annotationProcessor** | ✅ (컴파일 시 실행) | ❌ | ❌ | ❌ | Lombok 어노테이션 프로세서 |
| **testImplementation** | ❌ | ❌ | ✅ | ❌ | JUnit, Mockito |
| **testRuntimeOnly** | ❌ | ❌ | ✅ | ❌ | JUnit Platform Launcher |

**왜 이렇게 구분하는가?**

- **compileOnly (Lombok)**: 컴파일 타임에 코드를 생성하고 나면, 런타임에는 Lombok 라이브러리가 필요 없습니다. 최종 jar 파일 크기를 줄입니다.
- **runtimeOnly (H2)**: 코드에서 `import com.h2.xxx`를 직접 쓰지 않고, `spring.datasource.driver-class-name`으로 문자열로만 지정합니다. 컴파일에는 필요 없지만 런타임에 JDBC 드라이버로 동작합니다.
- **annotationProcessor**: Lombok의 어노테이션(`@Getter`, `@Setter`)을 읽어서 컴파일 타임에 실제 Java 코드를 생성하는 프로세서입니다.

### 2.2 주요 Starter와 하위 라이브러리

**spring-boot-starter-data-jpa**가 포함하는 라이브러리:

```
spring-boot-starter-data-jpa
├── spring-boot-starter-jdbc       // JDBC, HikariCP 커넥션 풀
├── hibernate-core                 // JPA 구현체 (Hibernate 6.x)
├── spring-data-jpa                // Spring Data JPA (Repository 추상화)
├── jakarta.persistence-api        // JPA 표준 인터페이스 (@Entity, @Id 등)
└── spring-boot-starter-aop        // AOP (트랜잭션 프록시)
```

**spring-boot-starter-webmvc**가 포함하는 라이브러리:

```
spring-boot-starter-webmvc
├── spring-webmvc                  // Spring MVC (@Controller, @GetMapping 등)
├── spring-web                     // HTTP 클라이언트, 멀티파트 처리
├── tomcat-embed-core              // 내장 Tomcat 서버
└── jackson-databind               // JSON 직렬화/역직렬화
```

**spring-boot-starter-thymeleaf**가 포함하는 라이브러리:

```
spring-boot-starter-thymeleaf
├── thymeleaf                      // Thymeleaf 템플릿 엔진
└── thymeleaf-spring6              // Spring 통합 (th:field, th:object 등)
```

### 2.3 의존성 트리 확인 방법

```bash
# 전체 의존성 트리 출력
./gradlew dependencies

# compileClasspath만 확인
./gradlew dependencies --configuration compileClasspath

# runtimeClasspath만 확인
./gradlew dependencies --configuration runtimeClasspath
```

**의존성 트리 예시:**

```
compileClasspath
├── org.springframework.boot:spring-boot-starter-data-jpa
│   ├── org.springframework.boot:spring-boot-starter-jdbc
│   │   ├── com.zaxxer:HikariCP:5.1.0
│   │   └── org.springframework:spring-jdbc:6.2.1
│   ├── org.hibernate.orm:hibernate-core:6.6.4
│   │   ├── jakarta.persistence:jakarta.persistence-api:3.2.0
│   │   └── org.hibernate.common:hibernate-commons-annotations:7.0.2
│   └── org.springframework.data:spring-data-jpa:3.4.1
│       └── org.springframework.data:spring-data-commons:3.4.1
└── org.projectlombok:lombok:1.18.36 (compileOnly)
```

---

## 3. Lombok 설정 및 동작 확인

### 3.1 Lombok이란?

Lombok은 **컴파일 타임에 어노테이션을 읽어서 Java 코드를 자동 생성**하는 라이브러리입니다.

**Lombok 없이 작성한 코드:**

```java
public class Hello {
    private String data;

    // Getter 수동 작성
    public String getData() {
        return data;
    }

    // Setter 수동 작성
    public void setData(String data) {
        this.data = data;
    }
}
```

**Lombok 사용한 코드:**

```java
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Hello {
    private String data;
}
```

겉보기에는 `getData()`, `setData()` 메서드가 없지만, 컴파일하면 자동으로 생성됩니다!

### 3.2 어노테이션 프로세서의 동작 원리

```
[컴파일 전 - .java 파일]
@Getter @Setter
public class Hello {
    private String data;
}
        ↓
[컴파일 타임 - Lombok Annotation Processor 실행]
1. javac가 Hello.java를 읽음
2. @Getter, @Setter 어노테이션 발견
3. Lombok 프로세서가 AST(추상 구문 트리)에 메서드 추가
   - public String getData() { return data; }
   - public void setData(String data) { this.data = data; }
        ↓
[컴파일 후 - .class 파일]
public class Hello {
    private String data;

    public String getData() { return data; }       // 자동 생성됨
    public void setData(String data) { this.data = data; }  // 자동 생성됨
}
```

**핵심**: Lombok은 런타임이 아닌 **컴파일 타임**에 동작합니다. 그래서 `compileOnly` + `annotationProcessor`로 설정합니다.

### 3.3 Hello.java 작성

```java
package jpabook.jpashop;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Hello {
    private String data;
}
```

### 3.4 JpashopApplication에서 동작 확인

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

**실행 결과:**

```
data = hello

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

...

2026-02-23T14:30:15.123+09:00  INFO 12345 --- [jpashop] [main] j.jpashop.JpashopApplication : Started JpashopApplication in 2.345 seconds
```

`data = hello`가 출력되면 Lombok이 정상 동작하는 것입니다!

### 3.5 IntelliJ IDEA 설정

**플러그인 설치 (최신 IntelliJ는 기본 내장):**

1. Settings (Ctrl+Alt+S)
2. Plugins → "Lombok" 검색 → 설치

**어노테이션 프로세서 활성화:**

1. Settings → Build, Execution, Deployment → Compiler → Annotation Processors
2. ✅ Enable annotation processing 체크

이 설정을 안 하면 IDE에서 `hello.getData()` 호출 시 빨간 줄이 표시됩니다.

---

## 4. View 환경 설정

### 4.1 정적 컨텐츠 vs 템플릿 엔진

| 구분 | 정적 컨텐츠 (Static Content) | 템플릿 엔진 (Thymeleaf) |
|------|----------------------------|------------------------|
| **위치** | `resources/static/` | `resources/templates/` |
| **확장자** | `.html` | `.html` |
| **처리 방식** | 파일 그대로 반환 | 서버에서 동적 렌더링 후 반환 |
| **데이터 바인딩** | 불가능 | 가능 (`${data}`) |
| **사용 상황** | 이미지, CSS, JS, 단순 HTML | 회원 목록, 상품 상세 등 동적 페이지 |
| **URL 예시** | `http://localhost:8080/index.html` | `http://localhost:8080/hello` |

### 4.2 Spring MVC 요청-응답 흐름

```
[브라우저]              [스프링 컨테이너]               [View - Thymeleaf]

GET /hello      ────>  1. DispatcherServlet이 요청 수신
                             ↓
                       2. HandlerMapping이 컨트롤러 탐색
                          → HelloController.hello() 찾음
                             ↓
                       3. HelloController.hello() 실행
                          → model.addAttribute("data", "hello!!!")
                          → return "hello" (뷰 이름)
                             ↓
                       4. ViewResolver가 뷰 탐색
                          → "hello" → templates/hello.html
                             ↓                            ↓
                       5. ThymeleafViewResolver 실행  <──┘
                          → ${data}를 "hello!!!"로 치환
                             ↓
                <────  6. 렌더링된 HTML 반환
```

### 4.3 HelloController 작성

```java
package jpabook.jpashop;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    @GetMapping("hello")
    public String hello(Model model) {
        // springUI에 있는 model이란 얘가 어떤 데이터를 실어서 view에 넘길 수 있다.
        // Controller에서 데이터를 view로 넘길 수 있다.
        // return은 화면 이름이다.
        model.addAttribute("data", "hello!!!");
        return "hello";
    }
}
```

**코드 설명:**

| 코드 | 의미 |
|------|------|
| `@Controller` | 스프링 MVC 컨트롤러로 등록 (컴포넌트 스캔 대상) |
| `@GetMapping("hello")` | `GET /hello` 요청을 이 메서드에 매핑 |
| `Model model` | 스프링이 자동으로 주입하는 데이터 전달 객체 |
| `model.addAttribute("data", "hello!!!")` | 키 "data"에 값 "hello!!!" 저장 → 뷰에서 `${data}` 사용 가능 |
| `return "hello"` | 뷰 이름 반환 → `templates/hello.html` 렌더링 |

### 4.4 hello.html 작성 (템플릿)

```html
<!-- 템플릿 엔진을 가지고 뭔가 렌더링되어야 할 것들(templates) -->
<!-- recompile해도 안바뀐다. 어떻게 해야할까? -->
<!-- 라이브러리 중에 spring-boot-devtools 넣어주고 create an import -->
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Hello</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>
<p th:text="'안녕하세요. ' + ${data}" >안녕하세요. 손님</p>
</body>
</html>
```

**Thymeleaf 문법 설명:**

| 코드 | 의미 |
|------|------|
| `xmlns:th="http://www.thymeleaf.org"` | Thymeleaf 네임스페이스 선언 (IDE 자동완성용) |
| `th:text` | 태그 내용을 동적으로 치환 |
| `${data}` | Model에서 "data" 키로 저장된 값 ("hello!!!") 참조 |
| `'안녕하세요. ' + ${data}` | 문자열 결합 → "안녕하세요. hello!!!" |
| `>안녕하세요. 손님</p>` | Thymeleaf 미작동 시 표시될 기본 텍스트 (순수 HTML로 열 때) |

**렌더링 결과:**

```html
<p>안녕하세요. hello!!!</p>
```

### 4.5 index.html 작성 (정적 컨텐츠)

```html
<!-- 완전 순수한 html을 띄우고 싶을 때(정적 컨텐츠는 static) -->
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Hello</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>
Hello
<a href="/hello">hello</a>
</body>
</html>
```

**접속 URL:**

- `http://localhost:8080/index.html` → static/index.html 그대로 반환
- `http://localhost:8080/hello` → HelloController 실행 → templates/hello.html 렌더링

### 4.6 devtools - 핫 리로딩 설정

**문제 상황:**

HTML 파일을 수정하고 브라우저를 새로고침해도 변경사항이 반영되지 않습니다. 서버를 재시작해야만 반영됩니다.

**해결 방법 - spring-boot-devtools:**

```gradle
implementation 'org.springframework.boot:spring-boot-devtools'
```

**devtools가 제공하는 기능:**

1. **자동 재시작 (Automatic Restart)**
   - Java 코드 변경 시 자동으로 애플리케이션 재시작 (전체 재시작보다 빠름)

2. **LiveReload**
   - HTML, CSS, JS 변경 시 브라우저 자동 새로고침

3. **캐시 비활성화**
   - Thymeleaf 캐시 자동 비활성화 (`spring.thymeleaf.cache=false`)

**IntelliJ에서 핫 리로딩 활성화:**

1. Settings → Build, Execution, Deployment → Compiler
2. ✅ Build project automatically 체크
3. Settings → Advanced Settings
4. ✅ Allow auto-make to start even if developed application is currently running 체크

**사용 방법:**

1. HTML 파일 수정 후 저장
2. `Build → Recompile 'hello.html'` (Ctrl+Shift+F9)
3. 브라우저 새로고침 → 변경사항 즉시 반영!

---

## 5. H2 데이터베이스 설치

### 5.1 H2 Database란?

H2는 자바로 작성된 **경량 관계형 데이터베이스**입니다.

**H2의 특징:**

| 특징 | 설명 |
|------|------|
| **경량** | jar 파일 하나로 설치 완료 (약 2MB) |
| **인메모리 모드** | 메모리에서만 동작 (재시작 시 데이터 초기화) |
| **파일 모드** | 파일로 저장 (재시작해도 데이터 유지) |
| **TCP 모드** | 네트워크로 접속 가능 (애플리케이션과 동시 접속 가능) |
| **웹 콘솔** | 브라우저에서 SQL 실행 가능 |
| **호환성** | MySQL, PostgreSQL 등 주요 DB와 호환 모드 제공 |
| **용도** | 개발/테스트 환경 (운영에는 MySQL, PostgreSQL 사용 권장) |

**왜 H2를 사용하는가?**

- Oracle, MySQL 등 실제 DB를 설치하지 않고도 JPA를 학습할 수 있습니다.
- 설치가 간단하고 재시작이 빠릅니다.
- 실전에서는 MySQL/PostgreSQL을 사용하지만, JPA는 표준이므로 H2에서 배운 내용을 그대로 적용 가능합니다.

### 5.2 H2 설치 단계 (Windows 기준)

**1단계: H2 다운로드**

- https://www.h2database.com/html/download.html
- "All Platforms" zip 파일 다운로드

**2단계: 압축 해제 및 실행**

```bash
# 압축 해제 후 bin 폴더로 이동
cd h2/bin

# Windows - h2.bat 실행
h2.bat

# macOS/Linux - h2.sh 실행
chmod +x h2.sh
./h2.sh
```

**3단계: 웹 콘솔 접속**

브라우저가 자동으로 열리며 `http://localhost:8082` 접속

**4단계: 최초 파일 생성 (파일 모드)**

| 항목 | 값 |
|------|-----|
| **드라이버 클래스** | `org.h2.Driver` |
| **JDBC URL** | `jdbc:h2:~/jpashop` (홈 디렉터리에 jpashop.mv.db 파일 생성) |
| **사용자명** | `sa` |
| **비밀번호** | (공백) |

**연결** 버튼 클릭 → `~/jpashop.mv.db` 파일 생성 확인

**5단계: TCP 모드로 전환**

이후부터는 다음 URL로 접속:

| 항목 | 값 |
|------|-----|
| **JDBC URL** | `jdbc:h2:tcp://localhost/~/jpashop` |

**6단계: 접속 확인**

```sql
-- 테이블 생성 테스트
CREATE TABLE test (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255)
);

-- 데이터 삽입
INSERT INTO test VALUES (1, 'hello');

-- 조회
SELECT * FROM test;
```

### 5.3 파일 모드 vs TCP 모드

```
[파일 모드: jdbc:h2:~/jpashop]

애플리케이션 ──(파일 락)──> ~/jpashop.mv.db
웹 콘솔      ──(접속 불가)─X  (파일이 잠겨있음)

⚠️ 문제: 동시 접속 불가 (파일 락 때문)
✅ 용도: 최초 DB 파일 생성 시에만 사용
```

```
[TCP 모드: jdbc:h2:tcp://localhost/~/jpashop]

애플리케이션 ──(TCP)──┐
웹 콘솔      ──(TCP)──┼──> H2 서버(TCP) ──> ~/jpashop.mv.db
                     │
                     └──> 동시 접속 가능! ✅

✅ 용도: 개발 중 항상 사용 (애플리케이션과 웹 콘솔 동시 접속)
```

**핵심 원칙:**

1. **최초 1회**: 파일 모드(`jdbc:h2:~/jpashop`)로 접속해서 DB 파일 생성
2. **이후**: TCP 모드(`jdbc:h2:tcp://localhost/~/jpashop`)로 접속

---

## 6. JPA와 DB 설정, 동작 확인

### 6.1 application.yml 전체 설정

`src/main/resources/application.yml` (또는 application.properties) 파일을 생성합니다.

```yaml
# properties를 쓰던가 yml을 쓰던가 둘 중에 하나를 쓰면 된다.
# 설정파일이 많아지고 복잡해지면 yaml이 더 낫다.
# 이러한 설정들은 spring boot manual에서 공부해야 한다.
spring:
  datasource:
    url: jdbc:h2:tcp://localhost/~/jpashop
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        # 운영 환경에서는 로그들을 System.out으로 찍으면 안되고 logger를 통해서 찍어야 한다.
#        show_sql: true
        format_sql: true

# Hibernate가 남기는 모든 로그가 다 디버그 모드로 해서 JPA나 Hibernate가 생성하는 SQL이 다 보인다.
logging:
  level:
    org.hibernate.SQL: debug
    # 로깅하는 방법
    org.hibernate.orm.jdbc.bind: trace
```

### 6.2 설정 항목 상세 설명

#### 6.2.1 spring.datasource (데이터베이스 연결 설정)

| 항목 | 값 | 의미 |
|------|-----|------|
| `url` | `jdbc:h2:tcp://localhost/~/jpashop` | H2 DB 접속 URL (TCP 모드) |
| `username` | `sa` | 데이터베이스 사용자명 |
| `password` | (공백) | 비밀번호 (H2 기본값은 없음) |
| `driver-class-name` | `org.h2.Driver` | JDBC 드라이버 클래스 |

**이 설정이 없으면?**

```
Error creating bean with name 'dataSource': Failed to determine a suitable driver class
```

**HikariCP 커넥션 풀:**

Spring Boot는 기본적으로 **HikariCP**를 커넥션 풀로 사용합니다. 위 설정만으로 자동으로 커넥션 풀이 생성됩니다.

```
애플리케이션 시작
        ↓
HikariCP가 데이터베이스 연결 10개(기본값) 미리 생성
        ↓
JPA가 DB 작업 필요 시 풀에서 연결 가져다 씀
        ↓
작업 완료 후 연결을 풀에 반환 (close하지 않음)
```

#### 6.2.2 spring.jpa.hibernate.ddl-auto (DDL 자동 생성)

| 옵션 | 동작 | 용도 |
|------|------|------|
| `create` | **기존 테이블 삭제 → 재생성** (DROP + CREATE) | 개발 초기 |
| `create-drop` | create + 애플리케이션 종료 시 DROP | 테스트 |
| `update` | 변경된 스키마만 반영 (컬럼 추가만 가능, 삭제는 안 함) | 개발 중 |
| `validate` | 엔티티와 테이블이 일치하는지 검증만 (불일치 시 에러) | 스테이징, 운영 |
| `none` | 아무 것도 안 함 | 운영 |

**⚠️ 경고: 운영 환경에서는 절대 create, create-drop, update 사용 금지!**

```
운영 DB에서 ddl-auto: create를 쓰면?
        ↓
애플리케이션 시작 시 모든 테이블 DROP
        ↓
고객 데이터 전부 삭제! 😱
```

**안전한 환경별 설정:**

| 환경 | 권장 설정 | 이유 |
|------|----------|------|
| 로컬 개발 | `create` 또는 `update` | 편의성 우선 |
| 개발 서버 | `update` 또는 `validate` | 다른 개발자 데이터 보존 |
| 스테이징 | `validate` | 운영과 동일한 스키마 검증 |
| 운영 | `validate` 또는 `none` | 절대 자동 변경 금지 |

#### 6.2.3 spring.jpa.properties.hibernate.format_sql

```yaml
format_sql: true
```

SQL을 보기 좋게 포맷팅합니다.

**Before (format_sql: false):**

```sql
select member0_.id as id1_0_, member0_.username as username2_0_ from member member0_ where member0_.id=?
```

**After (format_sql: true):**

```sql
select
    member0_.id as id1_0_,
    member0_.username as username2_0_
from
    member member0_
where
    member0_.id=?
```

#### 6.2.4 logging.level.org.hibernate.SQL

```yaml
logging:
  level:
    org.hibernate.SQL: debug
```

**show_sql vs logging.level.org.hibernate.SQL 비교:**

| 방식 | 설정 | 출력 위치 | 추천 |
|------|------|----------|------|
| `show_sql: true` | hibernate 속성 | `System.out` (표준 출력) | ❌ |
| `logging.level.org.hibernate.SQL: debug` | Logger | 로그 프레임워크 (Logback) | ✅ |

**왜 Logger를 써야 하는가?**

```
운영 환경에서 System.out.println()을 쓰면?
        ↓
1. 로그 레벨 조절 불가 (항상 출력)
2. 파일로 저장 안 됨 (휘발성)
3. 성능 저하 (동기 I/O)
        ↓
Logger 사용 시 해결 ✅
- 운영에서는 INFO, 개발에서는 DEBUG
- 파일 저장 및 로그 로테이션
- 비동기 로깅 가능
```

#### 6.2.5 logging.level.org.hibernate.orm.jdbc.bind

```yaml
logging:
  level:
    org.hibernate.orm.jdbc.bind: trace
```

SQL의 `?` (바인딩 파라미터) 값을 로그로 출력합니다.

**Before:**

```sql
select member0_.id, member0_.username from member member0_ where member0_.id=?
```

**After:**

```sql
select member0_.id, member0_.username from member member0_ where member0_.id=?
binding parameter [1] as [BIGINT] - [1]
```

`?`에 `1`이라는 값이 바인딩되었음을 알 수 있습니다.

**Hibernate 6.x 변경사항:**

- Hibernate 5.x: `org.hibernate.type.descriptor.sql.BasicBinder`
- Hibernate 6.x: `org.hibernate.orm.jdbc.bind` (변경됨!)

### 6.3 Member 엔티티 작성

```java
package jpabook.jpashop;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Member {

    @Id @GeneratedValue
    private Long id;
    private String username;

}
```

**어노테이션 설명:**

| 어노테이션 | 의미 |
|-----------|------|
| `@Entity` | JPA가 관리하는 엔티티 클래스 (테이블과 매핑) |
| `@Id` | 기본 키(Primary Key) 매핑 |
| `@GeneratedValue` | 기본 키 자동 생성 (AUTO_INCREMENT) |
| `@Getter @Setter` | Lombok으로 getter/setter 자동 생성 |

**javax vs jakarta:**

```java
// ❌ Spring Boot 2.x 이하
import javax.persistence.Entity;

// ✅ Spring Boot 3.x 이상
import jakarta.persistence.Entity;
```

Spring Boot 3.0부터는 `javax.*` → `jakarta.*`로 변경되었습니다.

**테이블 자동 생성:**

애플리케이션 시작 시 `ddl-auto: create` 설정으로 다음 DDL이 자동 실행됩니다.

```sql
-- Member 엔티티 → member 테이블 생성
create table member (
    id bigint not null auto_increment,
    username varchar(255),
    primary key (id)
);
```

### 6.4 MemberRepository 작성

```java
package jpabook.jpashop;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRepository {

    // JPA를 쓰기 때문에 엔티티 매니저가 필요하다.
    // 이 annotation이 있으면 spring boot가 EntityManager를 주입을 해준다.
    @PersistenceContext
    private EntityManager em;

    public Long save(Member member) {
        // 왜 id만 반환하지?
        // -> command랑 query를 분리해라!
        // -> 저장을 하고 나면 가급적이면 사이드 이펙트를 일으키느 커맨드 성이기 때문에 리턴값을 거의 안만든다.
        // -> 대신에 id정도 있으면 다음에 다시 조회할 수 있으니까 아이디 정도만 조회하는 걸로 주로 설계.
        em.persist(member);
        return member.getId();
    }

    public Member find(Long id) {
        return em.find(Member.class, id);
    }
}
```

**코드 설명:**

| 코드 | 의미 |
|------|------|
| `@Repository` | 스프링 빈으로 등록 (컴포넌트 스캔) |
| `@PersistenceContext` | JPA EntityManager 자동 주입 |
| `EntityManager` | JPA의 핵심 인터페이스 (CRUD 담당) |
| `em.persist(member)` | 영속성 컨텍스트에 member 저장 (INSERT SQL은 트랜잭션 커밋 시점에 실행) |
| `em.find(Member.class, id)` | 기본 키로 조회 (영속성 컨텍스트 1차 캐시 확인 후 DB 조회) |

**왜 save()가 Member를 반환하지 않고 id만 반환하는가?**

이것은 **CQRS(Command Query Responsibility Segregation)** 패턴의 일종입니다.

```
Command (저장, 수정, 삭제) → 반환값 최소화 (side effect 명확화)
Query (조회) → 필요한 데이터 반환

save(member) → Long id 반환 (필요하면 id로 다시 조회)
find(id) → Member 반환
```

**@PersistenceContext vs @Autowired:**

```java
// 표준 JPA 방식
@PersistenceContext
private EntityManager em;

// Spring Data JPA 방식 (Spring Boot에서 지원)
@Autowired
private EntityManager em;
```

둘 다 동작하지만, `@PersistenceContext`가 **JPA 표준**이므로 권장됩니다.

### 6.5 MemberRepositoryTest 작성

```java
package jpabook.jpashop;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class MemberRepositoryTest {

    // MemberRepository injection 받기
    @Autowired MemberRepository memberRepository;

    @Test
    @Transactional
    @Rollback(false)
    // EntityManager를 통한 모든 데이터 변경은 항상 트랜잭션 안에서 이루어져야 한다.
    // @Transactional은 spring꺼로 추천.
    // @Transactional annotation이 testcase에 있으면 테스트가 끝난 다음에 바로 롤백을 한다.
    // assert만 가지고는 의심이 될 때 -> @Rollback(false) -> rollback 안하고 그냥 commit
    public void testMember() throws Exception {
        // given
        Member member = new Member();
        member.setUsername("memberA");

        // when
        Long savedId = memberRepository.save(member);
        Member findMember = memberRepository.find(savedId);

        // then
        // AssertJ라는 라이브러리를 Spring 테스트가 자동으로 가지고 있다.
        Assertions.assertThat(findMember.getId()).isEqualTo(member.getId());
        Assertions.assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        // == 비교라고 보면 된다.
        // 같은 트랜잭션 안에서 저장을 하고 조회하면 영속성 컨텍스트가 똑같다.
        // 같은 영속성 컨텍스트 안에서는 id 값이 같으면 같은 엔티티로 식별한다.
        // insert query만 나가고 select query는 안나가게 된다. -> 영속성 컨텍스트 안에 있으니까
        Assertions.assertThat(findMember).isEqualTo(member);
        System.out.println("findMember == member: " + (findMember == member));
    }
}
```

**테스트 실행 결과:**

```
Hibernate:
    create table member (
        id bigint not null auto_increment,
        username varchar(255),
        primary key (id)
    ) engine=InnoDB

Hibernate:
    insert
    into
        member
        (username, id)
    values
        (?, default)
binding parameter [1] as [VARCHAR] - [memberA]

findMember == member: true
```

**주목할 점:**

1. **INSERT SQL만 나가고 SELECT SQL은 안 나감!**
   - 영속성 컨텍스트의 1차 캐시 덕분

2. **findMember == member가 true**
   - 같은 트랜잭션 내에서는 같은 엔티티 보장 (동일성 보장)

### 6.6 영속성 컨텍스트 (Persistence Context)

영속성 컨텍스트는 **엔티티를 영구 저장하는 환경**입니다. JPA의 가장 중요한 개념입니다.

**영속성 컨텍스트의 동작:**

```
[영속성 컨텍스트 - 1차 캐시]

┌─────────────────────────────┐
│   @Id    │    Entity         │
├─────────────────────────────┤
│   1      │  Member(id=1, username="memberA")
│   2      │  Member(id=2, username="memberB")
└─────────────────────────────┘

[시나리오]

1. em.persist(member)
   → member를 1차 캐시에 저장 (아직 INSERT SQL 안 나감)

2. em.find(Member.class, id)
   → 1차 캐시에 있는지 확인
   → 있으면 DB 조회 안 하고 1차 캐시에서 반환! (SELECT SQL 안 나감)
   → 없으면 DB 조회 → 1차 캐시에 저장 → 반환

3. 트랜잭션 커밋
   → 쓰기 지연 SQL 저장소에 모인 INSERT SQL 일괄 실행
```

**테스트 코드 흐름 상세:**

```
@Transactional  // 트랜잭션 시작
public void testMember() {
    Member member = new Member();
    member.setUsername("memberA");

    // 1. persist 호출
    em.persist(member);
    // → 영속성 컨텍스트 1차 캐시에 저장
    // → INSERT SQL은 쓰기 지연 저장소에 보관 (아직 DB에 안 감)

    // 2. find 호출
    Member findMember = em.find(Member.class, savedId);
    // → 1차 캐시에서 찾음 (DB 조회 안 함!)
    // → SELECT SQL 안 나감 ✅

    // 3. 동일성 비교
    findMember == member  // true
    // → 1차 캐시에서 같은 객체 반환

}  // 트랜잭션 종료
   // → 커밋 시점에 INSERT SQL 실행 ✅
   // → 테스트에서는 @Rollback(false) 설정으로 커밋
```

**왜 SELECT SQL이 안 나가는가?**

```
일반적인 JDBC:
save(member) → INSERT SQL 즉시 실행
find(id)     → SELECT SQL 실행

JPA:
em.persist(member) → 1차 캐시에 저장 (SQL 안 나감)
em.find(id)        → 1차 캐시 확인 → 있으면 SELECT 안 함!
커밋 시점          → INSERT SQL 실행
```

### 6.7 @Transactional 동작 원리

**왜 테스트에서 @Transactional이 필요한가?**

```java
// ❌ @Transactional 없으면?
@Test
public void testMember() {
    memberRepository.save(member);  // Error!
}
```

```
Error: javax.persistence.TransactionRequiredException:
No EntityManager with actual transaction available for current thread
```

JPA는 **모든 데이터 변경이 트랜잭션 안에서 이루어져야** 합니다.

**@Transactional in Test vs Service:**

| 위치 | 동작 | 커밋 여부 |
|------|------|----------|
| **@Service** | 메서드 종료 시 커밋 | ✅ 커밋 (DB 반영) |
| **@Test** | 메서드 종료 시 롤백 | ❌ 롤백 (DB 반영 안 됨) |

**테스트에서 롤백이 기본인 이유:**

```
테스트1: 회원 A 저장 → 커밋 → DB에 남음
테스트2: 회원 A 저장 → 중복 키 에러! 😱

해결: 테스트는 자동 롤백 → 다음 테스트에 영향 없음 ✅
```

**@Rollback(false)를 언제 쓰는가?**

- 개발 중 실제로 DB에 데이터가 들어가는지 눈으로 확인하고 싶을 때
- 운영 코드에서는 절대 쓰지 않음

### 6.8 p6spy - 쿼리 파라미터 로깅

`org.hibernate.orm.jdbc.bind: trace` 설정으로도 파라미터를 볼 수 있지만, **한 줄로 깔끔하게** 보고 싶다면 **p6spy**를 사용합니다.

**build.gradle에 추가:**

```gradle
implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:2.0.0'
```

**Before (기본 로깅):**

```sql
Hibernate:
    insert
    into
        member
        (username, id)
    values
        (?, default)
binding parameter [1] as [VARCHAR] - [memberA]
```

**After (p6spy):**

```sql
insert into member (username, id) values ('memberA', default);
```

`?`가 실제 값으로 치환되어 한 줄로 출력됩니다!

**p6spy 주의사항:**

| ⚠️ 주의 | 설명 |
|---------|------|
| **성능 영향** | 모든 쿼리를 가로채서 파라미터를 치환하므로 오버헤드 발생 |
| **운영 환경** | 성능 테스트 없이 절대 사용 금지 |
| **권장 용도** | 개발 환경에서만 사용 |

---

## 7. Spring Boot 3.x/4.x 주요 변경사항

### 7.1 javax → jakarta 패키지 변경

**배경:**

Java EE가 Oracle에서 Eclipse Foundation으로 이관되며 `javax.*` 패키지를 `jakarta.*`로 변경했습니다.

| Spring Boot 버전 | JPA 패키지 | Validation 패키지 | Servlet 패키지 |
|-----------------|-----------|------------------|---------------|
| **2.x 이하** | `javax.persistence.*` | `javax.validation.*` | `javax.servlet.*` |
| **3.x 이상** | `jakarta.persistence.*` | `jakarta.validation.*` | `jakarta.servlet.*` |

**코드 변경 예시:**

```java
// ❌ Spring Boot 2.x
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

// ✅ Spring Boot 3.x/4.x
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
```

**IntelliJ 일괄 변경:**

1. `Ctrl + Shift + R` (Replace in Path)
2. Find: `import javax.persistence`
3. Replace: `import jakarta.persistence`
4. Replace All

### 7.2 Hibernate 6.x 변경사항

| 항목 | Hibernate 5.x | Hibernate 6.x |
|------|--------------|--------------|
| **패키지** | `org.hibernate.type.descriptor.sql.BasicBinder` | `org.hibernate.orm.jdbc.bind` |
| **@GeneratedValue 기본 전략** | `AUTO` (TABLE 방식) | `SEQUENCE` (MySQL은 AUTO_INCREMENT) |
| **@Query** | JPQL만 지원 | JPQL + Native Query 개선 |

**로깅 설정 변경:**

```yaml
# Hibernate 5.x
logging:
  level:
    org.hibernate.type.descriptor.sql.BasicBinder: trace

# Hibernate 6.x
logging:
  level:
    org.hibernate.orm.jdbc.bind: trace
```

### 7.3 Spring Boot 2.x vs 3.x/4.x 비교표

| 항목 | Spring Boot 2.x | Spring Boot 3.x/4.x |
|------|----------------|---------------------|
| **최소 Java 버전** | Java 8 | Java 17 |
| **Jakarta EE** | javax.* | jakarta.* |
| **Hibernate** | 5.x | 6.x |
| **Spring Framework** | 5.x | 6.x |
| **Tomcat** | 9.x | 10.x |
| **GraalVM Native** | 실험적 지원 | 공식 지원 |

---

## 8. Best Practice 및 주의사항

### 8.1 Lombok 사용 주의사항

| 항목 | ❌ 안티패턴 | ✅ 권장 패턴 |
|------|-----------|------------|
| **JPA 엔티티** | `@Data` 사용 | `@Getter @Setter` 개별 선언 |
| **이유** | `@EqualsAndHashCode`가 자동 포함 → 양방향 연관관계 시 무한 루프 | 필요한 것만 선언 |

**왜 @Data를 쓰면 안 되는가?**

```java
// ❌ 안티패턴
@Entity
@Data
public class Member {
    @Id
    private Long id;

    @OneToMany(mappedBy = "member")
    private List<Order> orders;
}

@Entity
@Data
public class Order {
    @Id
    private Long id;

    @ManyToOne
    private Member member;
}
```

```
member.equals(other)
  → @EqualsAndHashCode가 orders 필드 비교
    → order.equals(...)
      → @EqualsAndHashCode가 member 필드 비교
        → member.equals(...)
          → 무한 루프! StackOverflowError 😱
```

**✅ 해결:**

```java
@Entity
@Getter @Setter
public class Member {
    // @EqualsAndHashCode 안 씀 → 안전
}
```

### 8.2 H2 연결 모드 주의사항

| 항목 | ❌ 안티패턴 | ✅ 권장 패턴 |
|------|-----------|------------|
| **개발 중** | 항상 파일 모드 (`jdbc:h2:~/jpashop`) | 최초 1회만 파일 모드 → 이후 TCP 모드 |
| **문제** | 애플리케이션 실행 중 웹 콘솔 접속 불가 | 동시 접속 가능 |

```
# ❌ 안티패턴 (application.yml)
spring:
  datasource:
    url: jdbc:h2:~/jpashop  # 파일 모드 - 동시 접속 불가

# ✅ 권장
spring:
  datasource:
    url: jdbc:h2:tcp://localhost/~/jpashop  # TCP 모드 - 동시 접속 가능
```

### 8.3 ddl-auto 환경별 설정

| 환경 | ❌ 위험한 설정 | ✅ 권장 설정 |
|------|--------------|------------|
| **로컬 개발** | - | `create` 또는 `update` |
| **개발 서버** | `create` (다른 개발자 데이터 삭제) | `update` 또는 `validate` |
| **스테이징** | `create`, `update` (운영 데이터 구조와 불일치 위험) | `validate` |
| **운영** | `create` (데이터 전부 삭제!!) | `validate` 또는 `none` |

**운영 환경 안전 설정:**

```yaml
# 운영 환경 (application-prod.yml)
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 또는 none
    properties:
      hibernate:
        format_sql: false  # 성능 최적화

logging:
  level:
    org.hibernate.SQL: warn  # 운영에서는 warn 이상만
```

### 8.4 SQL 로깅 방식

| 항목 | ❌ 안티패턴 | ✅ 권장 패턴 |
|------|-----------|------------|
| **SQL 출력** | `show_sql: true` (System.out) | `logging.level.org.hibernate.SQL: debug` |
| **파라미터 출력** | p6spy를 운영 환경에 적용 | 개발에만 p6spy, 운영은 `org.hibernate.orm.jdbc.bind: trace` |

**왜 show_sql을 쓰면 안 되는가?**

```yaml
# ❌ 안티패턴
spring:
  jpa:
    properties:
      hibernate:
        show_sql: true  # System.out.println() 사용
```

```
문제점:
1. 로그 레벨 조절 불가 (항상 출력)
2. 파일로 저장 안 됨
3. 운영 환경에서 성능 저하

✅ 해결: Logger 사용
logging:
  level:
    org.hibernate.SQL: debug  # 로그 프레임워크 사용
```

### 8.5 p6spy 운영 환경 주의

| 항목 | 설명 |
|------|------|
| **개발 환경** | ✅ 사용 권장 (쿼리 디버깅 편리) |
| **운영 환경** | ⚠️ 성능 테스트 필수 |
| **주의사항** | 모든 쿼리를 가로채므로 오버헤드 발생 |

**성능 영향 예시:**

```
Without p6spy: 100 TPS
With p6spy:    85 TPS  (15% 성능 저하)

→ 트래픽이 많은 서비스는 부담
→ 성능 테스트 후 판단
```

### 8.6 종합 정리표

| 상황 | ❌ 안티패턴 | ✅ 권장 패턴 |
|------|-----------|------------|
| JPA 엔티티 Lombok | `@Data` | `@Getter @Setter` |
| H2 연결 | 항상 파일 모드 | 최초 1회 파일 모드 → TCP 모드 |
| ddl-auto (운영) | `create`, `update` | `validate` 또는 `none` |
| SQL 로깅 | `show_sql: true` | `logging.level.org.hibernate.SQL: debug` |
| p6spy | 운영에 무조건 적용 | 개발만 사용, 운영은 성능 테스트 후 판단 |

---

## 9. 부록

### 9.1 프로젝트 구조

```
jpashop/
├── build.gradle                              # Gradle 빌드 설정
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── jpabook/
│   │   │       └── jpashop/
│   │   │           ├── JpashopApplication.java    # 메인 클래스
│   │   │           ├── Hello.java                 # Lombok 테스트
│   │   │           ├── HelloController.java       # View 테스트 컨트롤러
│   │   │           ├── Member.java                # JPA 엔티티
│   │   │           └── MemberRepository.java      # JPA Repository
│   │   └── resources/
│   │       ├── static/
│   │       │   └── index.html                     # 정적 컨텐츠
│   │       ├── templates/
│   │       │   └── hello.html                     # Thymeleaf 템플릿
│   │       └── application.yml                    # Spring Boot 설정
│   └── test/
│       └── java/
│           └── jpabook/
│               └── jpashop/
│                   └── MemberRepositoryTest.java  # JPA 동작 테스트
└── docs/                                          # 강의 PDF
```

### 9.2 핵심 용어 정리

| 용어 | 설명 |
|------|------|
| **Spring Boot Starter** | 관련 의존성을 묶어놓은 패키지 (예: starter-data-jpa) |
| **Lombok** | 어노테이션 프로세서로 컴파일 타임에 코드를 자동 생성하는 라이브러리 |
| **Thymeleaf** | 서버 사이드 템플릿 엔진 (HTML에 데이터 바인딩) |
| **H2 Database** | 경량 관계형 데이터베이스 (인메모리/파일 모드 지원) |
| **EntityManager** | JPA의 핵심 인터페이스 (CRUD 담당) |
| **영속성 컨텍스트** | 엔티티를 영구 저장하는 환경 (1차 캐시, 쓰기 지연 등) |
| **ddl-auto** | 엔티티 기반 DDL 자동 생성 옵션 (create, update, validate, none) |
| **p6spy** | SQL 쿼리 파라미터를 로그로 출력하는 라이브러리 |

### 9.3 어노테이션 정리

| 어노테이션 | 위치 | 역할 |
|-----------|------|------|
| `@SpringBootApplication` | 메인 클래스 | 스프링 부트 자동 설정 활성화 |
| `@Getter` | 클래스 | getter 메서드 자동 생성 (Lombok) |
| `@Setter` | 클래스 | setter 메서드 자동 생성 (Lombok) |
| `@Controller` | 클래스 | 스프링 MVC 컨트롤러로 등록 |
| `@GetMapping` | 메서드 | HTTP GET 요청 매핑 |
| `@Entity` | 클래스 | JPA 엔티티로 등록 (테이블 매핑) |
| `@Id` | 필드 | 기본 키(Primary Key) 매핑 |
| `@GeneratedValue` | 필드 | 기본 키 자동 생성 (AUTO_INCREMENT) |
| `@Repository` | 클래스 | 스프링 빈으로 등록 (데이터 접근 계층) |
| `@PersistenceContext` | 필드 | EntityManager 자동 주입 |
| `@SpringBootTest` | 테스트 클래스 | 스프링 부트 통합 테스트 |
| `@Transactional` | 메서드 | 트랜잭션 시작/커밋/롤백 자동 처리 |
| `@Rollback` | 테스트 메서드 | 테스트 종료 후 롤백 여부 설정 |

### 9.4 application.yml vs application.properties

**형식 비교:**

**application.properties:**

```properties
spring.datasource.url=jdbc:h2:tcp://localhost/~/jpashop
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=debug
```

**application.yml:**

```yaml
spring:
  datasource:
    url: jdbc:h2:tcp://localhost/~/jpashop
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    org.hibernate.SQL: debug
```

**비교표:**

| 항목 | .properties | .yml |
|------|------------|------|
| **형식** | key=value | 들여쓰기 기반 |
| **가독성** | 보통 | 우수 |
| **중복 제거** | spring.datasource를 매번 반복 | spring: 한 번만 |
| **복잡도** | 단순 설정에 적합 | 복잡한 설정에 적합 |
| **권장** | - | ✅ (설정이 많아질수록 유리) |

### 9.5 학습 확인 질문

다음 질문에 답하며 학습 내용을 확인해보세요:

1. **Lombok의 @Getter가 동작하는 시점은?**
   - 컴파일 타임 (어노테이션 프로세서가 .java → .class 변환 시 메서드 생성)

2. **compileOnly와 runtimeOnly의 차이는?**
   - compileOnly: 컴파일에만 필요 (Lombok - 런타임에는 불필요)
   - runtimeOnly: 런타임에만 필요 (H2 - JDBC 드라이버로 동작)

3. **static/과 templates/ 디렉터리의 차이는?**
   - static: 정적 컨텐츠 (파일 그대로 반환)
   - templates: 동적 컨텐츠 (Thymeleaf로 렌더링 후 반환)

4. **H2 파일 모드와 TCP 모드의 차이는?**
   - 파일 모드: 파일 락 때문에 동시 접속 불가 (최초 DB 파일 생성 시에만)
   - TCP 모드: H2 서버를 통해 동시 접속 가능 (개발 중 항상 사용)

5. **em.persist(member) 실행 시 INSERT SQL이 바로 나가는가?**
   - 아니다. 영속성 컨텍스트에만 저장되고, 트랜잭션 커밋 시점에 SQL 실행

6. **같은 트랜잭션에서 save 후 find하면 SELECT 쿼리가 나가는가?**
   - 아니다. 1차 캐시에서 찾으므로 SELECT 안 나감

7. **ddl-auto: create를 운영 환경에서 쓰면 어떻게 되는가?**
   - 애플리케이션 시작 시 모든 테이블이 DROP → CREATE됨 (모든 데이터 삭제!)

### 9.6 다음 챕터 예고

**챕터 2: 도메인 분석 설계**

다음 챕터에서 배울 내용:

```
요구사항 분석
    ↓
ER 다이어그램 설계
    ↓
엔티티 클래스 구현 (Member, Order, OrderItem, Item, Delivery, Category)
    ↓
연관관계 설정 (@OneToMany, @ManyToOne, @OneToOne, @ManyToMany)
    ↓
엔티티 설계 시 주의점
```

**주요 학습 주제:**

1. **요구사항 분석**: 회원, 상품, 주문 기능 정의
2. **도메인 모델링**: UML 다이어그램으로 구조 설계
3. **엔티티 클래스 설계**: 실전 프로젝트 수준의 복잡한 도메인
4. **연관관계 매핑**: 일대다, 다대일, 일대일, 다대다
5. **테이블 설계**: 엔티티 → 테이블 매핑 전략

**예상 엔티티 구조:**

```
Member (회원)
  ↕ 1:N
Order (주문)
  ↕ 1:N
OrderItem (주문상품)
  ↕ N:1
Item (상품)

Order (주문)
  ↕ 1:1
Delivery (배송)

Item (상품)
  ↕ M:N
Category (카테고리)
```

**실전 프로젝트로 가는 첫걸음을 축하합니다!** 🎉

---

*작성일: 2026-02-23*
*강의: 인프런 - 김영한의 실전! 스프링 부트와 JPA 활용 1*
*챕터: 1. 프로젝트 환경설정*
