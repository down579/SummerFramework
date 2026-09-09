package io.summer.core;

public final class BeanDefinition {
    private final String name;
    private final Class<?> beanClass;

    public BeanDefinition(String name, Class<?> beanClass) {
        this.name = name;
        this.beanClass = beanClass;
    }

    public String getName() {
        return name;
    }
    public Class<?> getBeanClass() {
        return beanClass;
    }
}
