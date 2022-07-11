package com.example.a12;


import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

interface Foo {
    void foo();

    int bar();
}


/*interface InvocationHandler {
    void invoke(Method method, Object[] args) throws RuntimeException, IllegalAccessException, InvocationTargetException;
}*/

public class A13 {

    static class Target implements Foo {

        @Override
        public void foo() {
            System.out.println("foo..");
        }

        @Override
        public int bar() {
            System.out.println("bar..");
            return 100;
        }
    }

    public static void main(String[] args) throws IOException {
        $Proxy0 proxy = new $Proxy0(new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                Target target = new Target();
                System.out.println("增强");
//            new Target().foo();
                return method.invoke(target, args);
            }
        });
        System.out.println(proxy.getClass());
        proxy.foo();
        int bar = proxy.bar();
        System.out.println(bar);
        System.in.read();
    }
}
