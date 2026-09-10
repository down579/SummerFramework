package io.summer.core;

import io.summer.annotation.ComponentScan;

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
                beanFactory.register(beanClass);
            }
        }
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
