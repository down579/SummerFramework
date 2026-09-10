# SummerFramework

Spring의 IoC/DI 핵심을 학습 목적으로 직접 구현한 경량 컨테이너입니다.  
JDK 21 기준이며, 애노테이션 기반 싱글톤 빈 관리에 집중합니다.

## 현재 지원 기능

| Phase | 내용 | 상태 |
| --- | --- | --- |
| 1 | 수동 등록, 생성자 주입, 싱글톤 | 완료 |
| 2 | `@ComponentScan`, 순환 의존성 감지 | 완료 |
| 3 | 필드/세터 주입, `@Qualifier`, `@PostConstruct`, `@Component` 이름 | 완료 |

### 애노테이션

- `@Component` / `@Component("beanName")` — 빈 등록 대상
- `@ComponentScan` / `@ComponentScan("base.package")` — 패키지 스캔
- `@Inject` — 생성자 · 필드 · 세터 · 파라미터 주입
- `@Qualifier("name")` — 동일 타입 빈이 여러 개일 때 이름 지정
- `@PostConstruct` — 주입 완료 후 초기화 콜백

### 컨테이너 동작

```text
scan / register  →  BeanDefinition 등록
refresh / getBean → 인스턴스 생성
                 → 생성자 주입
                 → 필드 / 세터 주입
                 → @PostConstruct
                 → 싱글톤 캐시 저장
```

- `ApplicationContext(configClass)`는 스캔 후 `refresh()`까지 수행합니다.
- 실제 생성 지점은 `getBean`이며, `refresh()`는 등록된 싱글톤을 미리 모두 생성합니다.
- 생성자 순환 의존성은 `currentlyCreating`으로 감지해 명확한 예외를 던집니다.

## 패키지 구조

```text
src/io/summer
  annotation/     # @Component, @Inject, @Qualifier, ...
  core/
    ApplicationContext   # 진입점 (scan / refresh / getBean)
    DefaultBeanFactory   # 정의 저장, 싱글톤, 순환 감지
    Injector             # 생성자·필드·세터·PostConstruct
    ClassPathScanner     # classpath 패키지 스캔
    BeanDefinition
    *Exception
```

## 빠른 시작

### 1. 빈 정의

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

    public void notifyUser(String msg) {
        sender.send(msg);
    }
}
```

### 2. 설정 클래스

```java
@ComponentScan("com.example.demo")
public class AppConfig {
}
```

### 3. 실행

```java
ApplicationContext ctx = new ApplicationContext(AppConfig.class);
NoticeService notice = ctx.getBean(NoticeService.class);
notice.notifyUser("hello");

// 이름으로 조회
MessageSender email = ctx.getBean("emailSender", MessageSender.class);
```

### 수동 등록

```java
ApplicationContext ctx = new ApplicationContext();
ctx.register(OrderRepository.class, NoticeService.class);
ctx.refresh();
```

## 빈 이름 규칙

1. `@Component("customName")`이 있으면 그 값
2. 없으면 클래스 simple name의 camelCase  
   예: `OrderService` → `orderService`

동일 타입 빈이 2개 이상이면 `getBean(Type.class)`는 `NoUniqueBeanException`을 던집니다.  
이때는 `@Qualifier` 또는 `getBean(name, type)`을 사용하세요.

## 아직 없는 기능

학습 로드맵상 이후 Phase에서 다룰 예정입니다.

- `@Configuration` / `@Bean`
- `BeanPostProcessor` / AOP
- `@Value` / 프로퍼티
- Prototype scope
- Web MVC

## 요구 사항

- JDK 21+
- IntelliJ IDEA 등으로 `src`를 소스 루트로 빌드 (현재 Maven/Gradle 빌드 파일 없음)

## 라이선스

학습/개인 프로젝트용입니다.
