package io.summer.core;

import io.summer.annotation.Inject;
import java.lang.reflect.Constructor;
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
    private Object[] resolveArguments(Constructor<?> constructor) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            args[i] = beanFactory.getBean(paramTypes[i]);
        }
        return args;
    }
}
