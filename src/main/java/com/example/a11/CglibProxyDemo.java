package com.example.a11;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

public class CglibProxyDemo {

    static class Target {
        public void foo() {
            System.out.println("target foo");
        }

        public void foo2() {
            System.out.println("target foo");
        }
    }

    // 代理是子类型, 目标是父类型
    public static void main(String[] args) {
        Target target = new Target();

        Target targetDemo = (Target) Enhancer.create(Target.class, (MethodInterceptor) (o, method, objects, methodProxy) -> {
            System.out.println("before...");
//            Object result = method.invoke(target, objects); //用方法反射来调用目标
            //methodProxy可以避免反射调用
//            Object result = methodProxy.invoke(target, objects);  //内部没有用反射,需要目标 (spring用的这一种)
            Object result = methodProxy.invokeSuper(o, args);   //内部没有用反射,需要代理
            System.out.println("after...");
            return result;
        });
        targetDemo.foo2();
    }
}