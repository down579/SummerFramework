package io.summer.core;

import io.summer.annotation.Inject;
import io.summer.annotation.PostConstruct;
import io.summer.annotation.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
public class Injector {

    private DefaultBeanFactory beanFactory;
    public void setBeanFactory(DefaultBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
    public Object createInstance(Class<?> beanClass) {
        try {
            Constructor<?> constructor = resolveConstructor(beanClass);
            Object[] args = resolveArguments(constructor);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (SummerException e) {
            throw e;
        } catch (Exception e) {
            throw new BeanCreationException(beanClass, e);
        }
    }
    private Constructor<?> resolveConstructor(Class<?> beanClass) {
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        List<Constructor<?>> injected = Arrays.stream(constructors)
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .collect(Collectors.toList());
        if (injected.size() == 1) {
            return injected.get(0);
        }
        if (injected.size() > 1) {
            throw new BeanCreationException(beanClass,
                    new IllegalStateException("Multiple @Inject constructors"));
        }
        // @Inject 없으면: 생성자 1개만 허용 (0-arg 포함)
        if (constructors.length == 1) {
            return constructors[0];
        }
        throw new BeanCreationException(beanClass,
                new IllegalStateException("Mark one constructor with @Inject"));
    }
    public Object[] resolveArguments(Executable executable) {
        Class<?>[] paramTypes = executable.getParameterTypes();
        Annotation[][] paramAnns = executable.getParameterAnnotations();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            String qualifier = findQualifier(paramAnns[i]);
            if (qualifier != null) {
                args[i] = beanFactory.getBean(qualifier, paramTypes[i]);
            } else {
                args[i] = beanFactory.getBean(paramTypes[i]);
            }
        }
        return args;
    }
    private String findQualifier(Annotation[] annotations) {
        for (Annotation a : annotations) {
            if (a instanceof Qualifier) {
                return ((Qualifier) a).value();
            }
        }
        return null;
    }

    public void injectFieldsAndSetters(Object instance) {
        Class<?> type = instance.getClass();
        injectFields(instance, type);
        injectSetters(instance, type);
    }
    private void injectFields(Object instance, Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Inject.class)) {
                continue;
            }
            Object dependency = resolveDependency(field.getType(), field.getAnnotation(Qualifier.class));
            field.setAccessible(true);
            try {
                field.set(instance, dependency);
            } catch (IllegalAccessException e) {
                throw new BeanCreationException(type, e);
            }
        }
    }
    private Object resolveDependency(Class<?> requiredType, Qualifier qualifier) {
        if (qualifier != null) {
            return beanFactory.getBean(qualifier.value(), requiredType);
        }
        return beanFactory.getBean(requiredType);
    }

    private void injectSetters(Object instance, Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Inject.class)) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                throw new BeanCreationException(type,
                        new IllegalStateException("@Inject method must have 1 parameter: " + method));
            }
            Object[] args = resolveArguments(method);
            method.setAccessible(true);
            try {
                method.invoke(instance, args);
            } catch (Exception e) {
                throw new BeanCreationException(type, e);
            }
        }
    }

    public void invokePostConstruct(Object instance) {
        Class<?> type = instance.getClass();
        List<Method> methods = Arrays.stream(type.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(PostConstruct.class))
                .collect(Collectors.toList());
        if (methods.size() > 1) {
            throw new BeanCreationException(type,
                    new IllegalStateException("Multiple @PostConstruct methods"));
        }
        if (methods.isEmpty()) {
            return;
        }
        Method m = methods.get(0);
        if (m.getParameterCount() != 0) {
            throw new BeanCreationException(type,
                    new IllegalStateException("@PostConstruct must have no args"));
        }
        m.setAccessible(true);
        try {
            m.invoke(instance);
        } catch (Exception e) {
            throw new BeanCreationException(type, e);
        }
    }
}
