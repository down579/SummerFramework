package io.summer.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ComponentScan {
    /** 스캔할 base package, 비우면 설정 클래스의 패키지 사용 */
    String[] value() default {};
}
