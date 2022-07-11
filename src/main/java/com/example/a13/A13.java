package com.example.a13;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class A13 {
    public static void main(String[] args) {
        Proxy proxy = new Proxy();
        Target target = new Target();
        proxy.setMethodInterceptor(new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                System.out.println("before");
                return method.invoke(target, objects);//反射调用
//                return methodProxy.invoke(target, objects);//非反射调用
//                return methodProxy.invokeSuper(o, objects);
            }
        });
        proxy.save();
        proxy.save(1);
        proxy.save(1L);

    }
}
