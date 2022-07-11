package com.example.a17;

import org.aopalliance.intercept.MethodInterceptor;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;

public class A17 {
    public static void main(String[] args) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("target1",Target1.class);
        context.registerBean("aspect1",Aspect1.class);
        context.registerBean("config",Config.class);
        context.registerBean(ConfigurationClassPostProcessor.class);

        /**
         * 这个类实现BeanPostProcessor
         * 在bean的各个声明阶段 创建 (*) -> 依赖注入 -> 初始化(*)
         */
        context.registerBean(AnnotationAwareAspectJAutoProxyCreator.class);
        context.refresh();
        /**
         * AnnotationAwareAspectJAutoProxyCreator中有两个重要的方法,
         * 第一个方法findEligibleAdvisors,这个方法的作用是找到有资格的Advisors,也就是找到存在的切点
         * 第二个方法wrapIfNecessary,这个方法用来创建代理对象
         */
        for (String beanDefinitionName : context.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }
        Target1 aspect1 = (Target1) context.getBean("target1");
        aspect1.foo();
    }

    static class Target1 {
        public void foo() {
            System.out.println("target1 foo");
        }
    }

    static class Target2 {
        public void bar() {
            System.out.println("target1 bar");
        }
    }

    /*
    高级切面类
     */
    @Aspect
    @Order(1)
    static class Aspect1 {

        @Before("execution(* foo())")
        public void before() {
            System.out.println("aspect1 before...");
        }

        @After("execution(* foo())")
        public void after() {
            System.out.println("aspect1 after...");
        }
    }

    @Configuration
    static class Config {

        @Bean
        public Advisor advisor3(MethodInterceptor advisor3) {
            AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
            pointcut.setExpression("execution(* foo())");
            DefaultPointcutAdvisor defaultPointcutAdvisor = new DefaultPointcutAdvisor(pointcut, advisor3);
            defaultPointcutAdvisor.setOrder(2);
            return defaultPointcutAdvisor;
        }

        @Bean
        public MethodInterceptor advice3() {
            /**
             * 内联变量
             */
            return invocation -> {
                System.out.println("before...");
                Object result = invocation.proceed();
                System.out.println("after...");
                return result;
            };
        }
    }
}
