package com.example.a17;


import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJMethodBeforeAdvice;
import org.springframework.aop.aspectj.SingletonAspectInstanceFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class A17_1 {
    public static void main(String[] args) {
        ArrayList<Advisor> list = new ArrayList<>();
        for (Method m : Aspect1.class.getDeclaredMethods()) {
            //解析结点
            if (m.isAnnotationPresent(Before.class)) {
                //解析切点
                String expression = m.getAnnotation(Before.class).value();
                AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
                pointcut.setExpression(expression);
                //通知类
                AspectJMethodBeforeAdvice advice = new AspectJMethodBeforeAdvice(m, pointcut, new SingletonAspectInstanceFactory(new Aspect1()));
                //切面
                Advisor advisor = new DefaultPointcutAdvisor(pointcut, advice);
                list.add(advisor);
            }
        }
        for (Advisor advisor : list) {
            System.out.println(advisor);
        }

    }

    @Aspect
    static class Aspect1 {

        @Before("execution(* foo())")
        public void before() {
            System.out.println("aspect1 before...");
        }

        @Before("execution(* foo())")
        public void after() {
            System.out.println("aspect1 after...");
        }
    }
}
