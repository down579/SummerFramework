package io.summer.core;

import io.summer.annotation.ComponentScan;
import io.summer.annotation.Configuration;

public class ApplicationContext {
    private final DefaultBeanFactory beanFactory;
    private final ClassPathScanner scanner = new ClassPathScanner();
    public ApplicationContext() {
        Injector injector = new Injector();
        this.beanFactory = new DefaultBeanFactory(injector);
    }
    /** 설정 클래스로 부트스트랩 */
    public ApplicationContext(Class<?> configClass) {
        this();
        scan(configClass);
        refresh();
    }
    public void scan(Class<?> configClass) {
        ComponentScan scan = configClass.getAnnotation(ComponentScan.class);
        if (scan == null) {
            throw new SummerException(
                    "@ComponentScan required on " + configClass.getName());
        }
        String[] packages = scan.value();
        if (packages.length == 0) {
            packages = new String[]{ configClass.getPackageName() };
        }
        for (String basePackage : packages) {
            for (Class<?> beanClass : scanner.scan(basePackage)) {
                if (beanClass.isAnnotationPresent(Configuration.class)) {
                    beanFactory.registerConfiguration(beanClass);
                } else {
                    beanFactory.register(beanClass);
                }
            }
        }
        // AppConfig 자체에 @Bean이 있는 경우 (스캔 결과와 중복될 수 있으니 registerConfiguration 쪽에서 방어)
        //beanFactory.registerConfiguration(configClass);
    }
    public void register(Class<?>... beanClasses) {
        beanFactory.register(beanClasses);
    }
    public void refresh() {
        beanFactory.instantiateSingletons();
    }
    public <T> T getBean(Class<T> type) {
        return beanFactory.getBean(type);
    }
    public DefaultBeanFactory getBeanFactory() {
        return beanFactory;
    }

    public Object getBean(String name) {
        return beanFactory.getBean(name);
    }
    public <T> T getBean(String name, Class<T> type) {
        return beanFactory.getBean(name, type);
    }
}
