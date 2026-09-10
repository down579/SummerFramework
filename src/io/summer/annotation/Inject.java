package io.summer.annotation;

import java.lang.annotation.*;

@Target({ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Inject {
}
