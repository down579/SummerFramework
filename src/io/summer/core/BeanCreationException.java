package io.summer.core;

public class BeanCreationException extends SummerException {
    public BeanCreationException(Class<?> type, Throwable cause) {
        super("Failed to create bean: " + type.getName(), cause);
    }
}
