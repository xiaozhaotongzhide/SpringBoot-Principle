package com.example.a15;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;

public class A15 {
    /*
    两个切面概念
    aspect =
        通知1(advice) +  切点1(pointcut)
        通知2(advice) +  切点2(pointcut)
        通知3(advice) +  切点3(pointcut)
        ...
    advisor = 更细粒度的切面，包含一个通知和切点
    */
    /*@Aspect
    static class MyAspect {

        @Before("execution(* foo())")
        public void before() {
            System.out.println("前置增强");
        }

        @After("execution(* foo())")
        public void after() {
            System.out.println("后置增强");
        }
    }*/
    public static void main(String[] args) {
        //1.准备好切点
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(* foo())");

        //2.准备好通知
        MethodInterceptor adivce = invocation -> {
            System.out.println("before...");
            Object result = invocation.proceed();
            System.out.println("after...");
            return result;
        };

        //3.备好切面
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, adivce);

        //4.创建代理
        /*
            1.实现接口,用jdk1实现
            2.目标没有实现接口,用cglib实现
            3.proxyTargetClass = true,总是用cglib实现
         */
        Target1 target1 = new Target1();
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(target1);
        factory.addAdvisor(advisor);
        factory.setInterfaces(target1.getClass().getInterfaces());
        factory.setProxyTargetClass(true);

        //测试
        I1 proxy = (I1) factory.getProxy();
        System.out.println(proxy.getClass());
        proxy.foo();
        proxy.bar();

    }

    interface I1 {
        void foo();

        void bar();
    }

    static class Target1 implements I1 {
        public void foo() {
            System.out.println("target1 foo");
        }

        public void bar() {
            System.out.println("target1 bar");
        }
    }

    static class Target2 {
        public void foo() {
            System.out.println("target2 foo");
        }

        public void bar() {
            System.out.println("target2 bar");
        }
    }
}
