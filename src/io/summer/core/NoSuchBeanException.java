package io.summer.core;

public class NoSuchBeanException extends SummerException {
    public NoSuchBeanException(Class<?> type) {
        super("No bean of type: " + type.getName());
    }
}
