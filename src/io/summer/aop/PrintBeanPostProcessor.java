package io.summer.aop;

import io.summer.annotation.Component;
import io.summer.core.BeanPostProcessor;

@Component
public class PrintBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("[BPP:before] " + beanName + " : " + bean.getClass().getSimpleName());
        return bean;
    }
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("[BPP:after]  " + beanName + " : " + bean.getClass().getSimpleName());
        return bean;
    }
}
