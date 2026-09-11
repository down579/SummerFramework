package io.summer.aop;

import io.summer.annotation.Component;
import io.summer.annotation.Log;
import io.summer.core.BeanPostProcessor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Component
public class LoggingBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();
        if (!needsProxy(beanClass)) {
            return bean;
        }
        Class<?>[] interfaces = beanClass.getInterfaces();
        if (interfaces.length == 0) {
            // JDK Proxy는 인터페이스 필요 — Phase 5에서는 스킵하거나 예외
            System.out.println("[AOP] skip (no interface): " + beanName);
            return bean;
        }
        Object target = bean;
        return Proxy.newProxyInstance(
                beanClass.getClassLoader(),
                interfaces,
                (proxy, method, args) -> {
                    if (shouldLog(beanClass, method)) {
                        System.out.println("[LOG] → " + beanName + "." + method.getName());
                        try {
                            Object result = method.invoke(target, args);
                            System.out.println("[LOG] ← " + beanName + "." + method.getName());
                            return result;
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            throw e.getCause();
                        }
                    }
                    return method.invoke(target, args);
                }
        );
    }
    private boolean needsProxy(Class<?> beanClass) {
        if (beanClass.isAnnotationPresent(Log.class)) {
            return true;
        }
        for (Method m : beanClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Log.class)) {
                return true;
            }
        }
        return false;
    }
    private boolean shouldLog(Class<?> beanClass, Method method) {
        if (beanClass.isAnnotationPresent(Log.class)) {
            return true;
        }
        try {
            Method impl = beanClass.getMethod(method.getName(), method.getParameterTypes());
            return impl.isAnnotationPresent(Log.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
