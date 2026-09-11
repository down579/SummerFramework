package io.summer.core;

import io.summer.annotation.Bean;
import io.summer.annotation.Component;

import java.lang.reflect.Method;
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
            Object instance;
            if (definition.isFactoryBean()) {
                instance = createFromFactory(definition);
            } else {
                instance = injector.createInstance(definition.getBeanClass());
                injector.injectFieldsAndSetters(instance);
                injector.invokePostConstruct(instance);
            }
            singletons.put(name, instance);
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
        // @Configuration만 있고 @Component meta가 없다면 여기로
        String simple = beanClass.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    public void registerConfiguration(Class<?> configClass) {
        String configName = resolveBeanName(configClass);
        boolean alreadyRegistered = definitions.containsKey(configName);
        // 설정 클래스 자체도 빈으로 등록 (다른 @Bean이 이걸 호출해야 함)
        if (!alreadyRegistered) {
            // @Configuration이면 이름으로 등록
            register(configClass);
        }
        else {
            return;
        }
        for (Method method : configClass.getDeclaredMethods()) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean == null) continue;
            String name = bean.value().isEmpty() ? method.getName() : bean.value();
            if (definitions.containsKey(name)) {
                throw new SummerException("Duplicate bean name: " + name);
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == void.class || returnType == Void.class) {
                throw new SummerException("@Bean method must return a value: " + method);
            }
            definitions.put(name,
                    new BeanDefinition(name, returnType, configClass, method));
        }
    }

    private Object createFromFactory(BeanDefinition definition) {
        try {
            // 1) Configuration 인스턴스
            Object config = getBean(definition.getConfigurationClass());
            Method method = definition.getFactoryMethod();
            method.setAccessible(true);
            // 2) 파라미터 주입 (@Qualifier 포함 — Injector 로직 재사용 권장)
            Object[] args = injector.resolveArguments(method);
            // 3) 호출
            Object bean = method.invoke(config, args);
            if (bean == null) {
                throw new BeanCreationException(definition.getBeanClass(),
                        new IllegalStateException("@Bean method returned null: " + method));
            }
            return bean;
        } catch (SummerException e) {
            throw e;
        } catch (Exception e) {
            throw new BeanCreationException(definition.getBeanClass(), e);
        }
    }
}
