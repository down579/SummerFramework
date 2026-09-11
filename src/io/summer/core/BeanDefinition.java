package io.summer.core;

import java.lang.reflect.Method;

public final class BeanDefinition {
    private final String name;
    private final Class<?> beanClass;

    // @Bean 전용 (일반 @Component면 null)
    private final Class<?> configurationClass;
    private final Method factoryMethod;

    /** 일반 컴포넌트 */
    public BeanDefinition(String name, Class<?> beanClass) {
        this(name, beanClass, null, null);
    }
    /** @Bean 메서드 */
    public BeanDefinition(String name, Class<?> beanClass,
                          Class<?> configurationClass, Method factoryMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.configurationClass = configurationClass;
        this.factoryMethod = factoryMethod;
    }

    public String getName() {
        return name;
    }
    public Class<?> getBeanClass() {
        return beanClass;
    }
    public Class<?> getConfigurationClass() { return configurationClass; }
    public Method getFactoryMethod() { return factoryMethod; }

    public boolean isFactoryBean() {
        return factoryMethod != null;
    }
}
