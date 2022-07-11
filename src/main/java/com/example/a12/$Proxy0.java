package com.example.a12;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * jdk生成的动态源码
 */
public class $Proxy0 extends Proxy implements Foo {


    public $Proxy0(InvocationHandler h) {
        super(h);
    }

    static public Method foo;
    static public Method bar;

    static {
        try {
            bar = Foo.class.getMethod("bar");
            foo = Foo.class.getMethod("foo");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void foo() {
        try {
            h.invoke(this, foo, new Object[0]);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int bar() {
        try {
            Object invoke = h.invoke(this, bar, new Object[0]);
            return (int) invoke;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


}
