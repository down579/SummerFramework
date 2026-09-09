package io.summer.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultBeanFactory {
    private final Map<String, BeanDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();
    private final Injector injector;
    public DefaultBeanFactory(Injector injector) {
        this.injector = injector;
        this.injector.setBeanFactory(this);
    }
    public void register(Class<?> beanClass) {
        String name = resolveBeanName(beanClass);
        if (definitions.containsKey(name)) {
            throw new SummerException("Duplicate bean name: " + name);
        }
        definitions.put(name, new BeanDefinition(name, beanClass));
    }
    public void register(Class<?>... beanClasses) {
        for (Class<?> beanClass : beanClasses) {
            register(beanClass);
        }
    }
    public void instantiateSingletons() {
        for (BeanDefinition definition : definitions.values()) {
            getBean(definition.getName(), definition.getBeanClass());
        }
    }
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        List<BeanDefinition> matched = definitions.values().stream()
                .filter(d -> type.isAssignableFrom(d.getBeanClass()))
                .collect(Collectors.toList());
        if (matched.isEmpty()) {
            throw new NoSuchBeanException(type);
        }
        if (matched.size() > 1) {
            throw new NoUniqueBeanException(type, matched.size());
        }
        BeanDefinition def = matched.get(0);
        return (T) getBean(def.getName(), def.getBeanClass());
    }
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> type) {
        Object existing = singletons.get(name);
        if (existing != null) {
            return type.cast(existing);
        }
        BeanDefinition definition = definitions.get(name);
        if (definition == null) {
            throw new NoSuchBeanException(type);
        }
        // 생성 중 표시용으로 먼저 null 넣을 수도 있지만,
        // P1은 단순 순환이면 StackOverflow → 나중에 개선
        Object instance = injector.createInstance(definition.getBeanClass());
        singletons.put(name, instance);
        return (T) instance;
    }
    public Collection<Object> getBeans() {
        return new ArrayList<>(singletons.values());
    }
    private String resolveBeanName(Class<?> beanClass) {
        String simple = beanClass.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }
}
