package io.summer.core;

public class NoUniqueBeanException extends SummerException {
    public NoUniqueBeanException(Class<?> type, int count) {
        super("Expected single bean of type " + type.getName() + " but found " + count);
    }
}
