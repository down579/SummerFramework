package io.summer.annotation;

import java.lang.annotation.*;

@Target({ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Inject {
}
