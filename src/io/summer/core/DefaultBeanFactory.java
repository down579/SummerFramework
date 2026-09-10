package io.summer.core;

import io.summer.annotation.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultBeanFactory {
    private final Map<String, BeanDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();
    private final Injector injector;
    private final Set<String> currentlyCreating = ConcurrentHashMap.newKeySet();

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
        if (!currentlyCreating.add(name)) {
            throw new BeanCreationException(
                    definition.getBeanClass(),
                    new IllegalStateException(
                            "Circular dependency detected while creating: " + name
                                    + " (in-progress: " + currentlyCreating + ")"
                    )
            );
        }
        try {
            Object instance = injector.createInstance(definition.getBeanClass());
            // 도전 옵션: 필드 순환을 일부 허용하려면 여기서 먼저 put
            // singletons.put(name, instance);
            injector.injectFieldsAndSetters(instance);
            injector.invokePostConstruct(instance); // 선택
            singletons.put(name, instance); // early put 안 했으면 여기서 저장
            return (T) instance;
        } finally {
            currentlyCreating.remove(name);
        }
    }
    public Object getBean(String name) {
        BeanDefinition definition = definitions.get(name);
        if (definition == null) {
            throw new SummerException("No bean named: " + name);
        }
        return getBean(name, definition.getBeanClass());
    }


    public Collection<Object> getBeans() {
        return new ArrayList<>(singletons.values());
    }
    private String resolveBeanName(Class<?> beanClass) {
        Component component = beanClass.getAnnotation(Component.class);
        if (component != null && !component.value().isEmpty()) {
            return component.value();
        }
        String simple = beanClass.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }
}
