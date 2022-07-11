package com.example.a11;

import java.io.IOException;
import java.lang.reflect.Proxy;

public class JdkProxyDemo {

    interface Foo {
        void foo();
    }

    static final class Target implements Foo {
        public void foo() {
            System.out.println("target foo");
        }
    }

    // jdk 只能针对接口代理
    // cglib
    public static void main(String[] param) throws IOException {
        //目标对象
        Target target = new Target();
        Target.class.getName();
        ClassLoader loader = JdkProxyDemo.class.getClassLoader();//用来加载运行时动态生成的字节码
        Foo proxyDemo = (Foo) Proxy.newProxyInstance(loader, new Class[]{Foo.class}, (proxy, method, args) -> {
            System.out.println("before...");
            // 目标.方法(参数)
            // 方法.invoke(目标, 参数)
            Object result = method.invoke(target, args);
            System.out.println("after...");
            return result;
        });
        System.out.println(proxyDemo.getClass());
        proxyDemo.foo();
        System.in.read();
    }
}
