package com.example.BeanLife;

import java.util.ArrayList;
import java.util.List;

public class TestMethodTemplate {

    public static void main(String[] args) {
        MyBeanFactory beanFactory = new MyBeanFactory();
        beanFactory.addBeanPostProcessor(bean -> System.out.println("解析 @Autowired"));
        beanFactory.addBeanPostProcessor(bean -> System.out.println("解析 @Resource"));
        beanFactory.getBean();
    }

    //模板方法 Template Method Pattern
    static class MyBeanFactory {
        public Object getBean() {
            Object bean = new Object();
            System.out.println("构造" + bean);
            System.out.println("依赖注入" + bean);
            for (BeanPostProcessor beans : processors) {
                beans.inject(bean);
            }
            System.out.println("初始化" + bean);
            return bean;
        }

        private List<BeanPostProcessor> processors = new ArrayList<BeanPostProcessor>();

        public void addBeanPostProcessor(BeanPostProcessor processor) {
            processors.add(processor);
        }
    }

    interface BeanPostProcessor {
        void inject(Object bean);
    }
}
