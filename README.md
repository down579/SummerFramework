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
| 5 | `BeanPostProcessor`, `@Log` + JDK 동적 프록시 AOP | 완료 |

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
| `@Log` | 타입/메서드에 붙이면 JDK 프록시로 호출 로그 |

### 확장 포인트

| 타입 | 역할 |
| --- | --- |
| `BeanPostProcessor` | 초기화 전후 훅 (`before` / `after`) |
| `LoggingBeanPostProcessor` | `@Log` 대상에 JDK Proxy 적용 |
| `PrintBeanPostProcessor` | 생성 파이프라인 디버그 출력 |

### 컨테이너 흐름

```text
scan / register
  → @Component       → BeanDefinition (생성자 방식)
  → @Configuration   → 설정 클래스 + @Bean 메서드 정의

refresh
  → BeanPostProcessor 빈을 먼저 생성·수집
  → 나머지 싱글톤 eager 생성

getBean / 생성
  → [일반] createInstance → 필드/세터 주입
  → [@Bean] Configuration 인스턴스 → 파라미터 주입 → method.invoke
  → BeanPostProcessor.before
  → @PostConstruct
  → BeanPostProcessor.after   ← 프록시 교체 가능
  → 싱글톤 캐시 저장
```

- `new ApplicationContext(AppConfig.class)` → `scan` + `refresh`
- 실제 생성 엔진은 `getBean`
- 생성 중 순환 의존성은 `currentlyCreating`으로 감지 후 예외
- `getBean(Class)`는 **요청한 타입**으로 cast (프록시를 인터페이스로 조회 가능)

## 패키지 구조

```text
src/io/summer
  annotation/
    Component, ComponentScan, Inject, Qualifier
    PostConstruct, Configuration, Bean, Log
  core/
    ApplicationContext     # 진입점
    DefaultBeanFactory     # 정의·싱글톤·@Bean·BPP·순환 감지
    Injector               # 생성자·필드·세터·PostConstruct
    ClassPathScanner
    BeanDefinition
    BeanPostProcessor
    *Exception
  aop/
    LoggingBeanPostProcessor
    PrintBeanPostProcessor
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

```java
@Configuration
@ComponentScan("io.summer.demo")
public class AppConfig {

    @Bean
    public Clock clock() {
        return new Clock();
    }

    @Bean
    public GreetingClient greetingClient(Clock clock) {
        return new GreetingClient(clock);
    }

    // BPP는 데모 스캔 범위 밖이면 @Bean으로 등록
    @Bean
    public LoggingBeanPostProcessor loggingBeanPostProcessor() {
        return new LoggingBeanPostProcessor();
    }
}
```

### AOP (`@Log` + JDK Proxy)

인터페이스 + 구현체 구조가 필요합니다. (JDK Proxy 제약)

```java
public interface AOPService {
    void run();
}

@Component
@Log
public class AOPServiceImpl implements AOPService {
    @Override
    public void run() {
        System.out.println("running");
    }
}
```

```java
ApplicationContext ctx = new ApplicationContext(AppConfig.class);

// ✅ 프록시는 인터페이스로 조회
AOPService service = ctx.getBean(AOPService.class);
service.run();

// ❌ 구현체로 조회하면 ClassCastException
// ctx.getBean(AOPServiceImpl.class);
```

기대 출력 예:

```text
[BPP:before] aOPServiceImpl : AOPServiceImpl
[BPP:after]  aOPServiceImpl : $Proxy...
[LOG] → aOPServiceImpl.run
running
[LOG] ← aOPServiceImpl.run
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

## Phase 5 참고

- `BeanPostProcessor`는 `refresh` 시 다른 빈보다 **먼저** 생성·수집됩니다.
- BPP 자신이 다른 빈에 의존하면, 그 빈은 BPP 목록이 비어 있는 채 만들어질 수 있습니다. (의존 최소화)
- `@Log`는 **구현 클래스**(또는 구현 메서드)에 붙입니다. 인터페이스만 붙이면 프록시가 안 만들어집니다.
- 데모 패키지만 스캔할 경우 `io.summer.aop`의 BPP는 `@Bean`/`register`로 따로 등록하세요.

## 아직 없는 기능

- CGLIB / `@Configuration` full 모드 (같은 설정 클래스 내부 `@Bean` 호출 가로채기)
- 포인트컷 표현식, Advisor 체인
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
