# SummerFramework

Spring IoC/DI 핵심을 학습 목적으로 직접 구현한 경량 컨테이너입니다.  
JDK 21 기준, 애노테이션 기반 **싱글톤** 빈 관리에 집중합니다.

## Phase 현황

| Phase | 내용 | 상태 |
| --- | --- | --- |
| 1 | 수동 등록, 생성자 주입, 싱글톤 | 완료 |
| 2 | `@ComponentScan`, 순환 의존성 감지 | 완료 |
| 3 | 필드/세터 주입, `@Qualifier`, `@PostConstruct`, `@Component` 이름 | 완료 |
| 4 | `@Configuration`, `@Bean` (lite 모드) | 완료 |

## 지원 기능

### 애노테이션

| 애노테이션 | 역할 |
| --- | --- |
| `@Component` / `@Component("name")` | 빈 등록 대상 |
| `@ComponentScan` / `@ComponentScan("pkg")` | 패키지 스캔 |
| `@Inject` | 생성자 · 필드 · 세터 · 파라미터 주입 |
| `@Qualifier("name")` | 동일 타입 빈 중 이름 지정 |
| `@PostConstruct` | 주입 완료 후 초기화 |
| `@Configuration` | 설정 클래스 (`@Component` 메타) |
| `@Bean` / `@Bean("name")` | 팩토리 메서드로 빈 등록 |

### 컨테이너 흐름

```text
scan / register
  → @Component  → BeanDefinition (생성자 방식)
  → @Configuration → 설정 클래스 + @Bean 메서드 정의 등록

refresh / getBean
  → [일반] createInstance → 필드/세터 주입 → @PostConstruct
  → [@Bean] Configuration 인스턴스 → 메서드 파라미터 주입 → method.invoke
  → 싱글톤 캐시 저장
```

- `new ApplicationContext(AppConfig.class)` → `scan` + `refresh`까지 수행
- 실제 생성 엔진은 `getBean`, `refresh`는 등록된 싱글톤을 eager 생성
- 생성 중 순환 의존성은 `currentlyCreating`으로 감지 후 예외

## 패키지 구조

```text
src/io/summer
  annotation/
    Component, ComponentScan, Inject, Qualifier
    PostConstruct, Configuration, Bean
  core/
    ApplicationContext    # 진입점
    DefaultBeanFactory    # 정의·싱글톤·@Bean 팩토리·순환 감지
    Injector              # 생성자·필드·세터·PostConstruct
    ClassPathScanner      # classpath 스캔
    BeanDefinition        # 일반 / 팩토리 메서드 정의
    *Exception
```

## 빠른 시작

### `@Component` 방식

```java
@Component
public class OrderRepository {
    public String findLatest() {
        return "order-42";
    }
}

@Component("emailSender")
public class EmailSender implements MessageSender {
    @Override
    public void send(String message) {
        System.out.println("[EMAIL] " + message);
    }
}

@Component
public class NoticeService {
    private final MessageSender sender;

    @Inject
    public NoticeService(@Qualifier("emailSender") MessageSender sender) {
        this.sender = sender;
    }

    @PostConstruct
    void init() {
        System.out.println("NoticeService ready");
    }
}
```

### `@Configuration` + `@Bean` 방식

`@Component`가 없는 타입도 팩토리 메서드로 등록할 수 있습니다.

```java
public class Clock {
    public long now() {
        return System.currentTimeMillis();
    }
}

public class GreetingClient {
    private final Clock clock;

    public GreetingClient(Clock clock) {
        this.clock = clock;
    }

    public String hello(String name) {
        return "hello " + name + " @ " + clock.now();
    }
}

@Configuration
@ComponentScan("com.example.demo")
public class AppConfig {

    @Bean
    public Clock clock() {
        return new Clock();
    }

    @Bean
    public GreetingClient greetingClient(Clock clock) {
        return new GreetingClient(clock);
    }
}
```

### 실행

```java
ApplicationContext ctx = new ApplicationContext(AppConfig.class);

NoticeService notice = ctx.getBean(NoticeService.class);
GreetingClient client = ctx.getBean("greetingClient", GreetingClient.class);
Clock clock = ctx.getBean(Clock.class);
```

### 수동 등록

```java
ApplicationContext ctx = new ApplicationContext();
ctx.register(OrderRepository.class, NoticeService.class);
ctx.refresh();
```

## 빈 이름 규칙

1. `@Component("name")` / `@Bean("name")`이 있으면 그 값  
2. `@Component`만 있으면 클래스 simple name의 camelCase (`OrderService` → `orderService`)  
3. `@Bean`만 있으면 **메서드명** (`clock()` → `clock`)

동일 타입 빈이 2개 이상이면 `getBean(Type.class)`는 `NoUniqueBeanException`을 던집니다.  
`@Qualifier` 또는 `getBean(name, type)`을 사용하세요.

## 아직 없는 기능

- `BeanPostProcessor` / AOP / 프록시 (`@Configuration` full 모드 포함)
- `@Value` / 프로퍼티
- Prototype scope
- `@Import`
- Web MVC
- Maven/Gradle 빌드 설정

## 요구 사항

- JDK 21+
- IntelliJ 등에서 `src`를 소스 루트로 빌드

## 라이선스

학습/개인 프로젝트용입니다.
