package io.summer.core;

public class ApplicationContext {
    private final DefaultBeanFactory beanFactory;

    public ApplicationContext() {
        Injector injector = new Injector();
        this.beanFactory = new DefaultBeanFactory(injector);
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
}
