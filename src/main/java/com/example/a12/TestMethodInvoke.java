package com.example.a12;

import java.lang.reflect.Method;

public class TestMethodInvoke {

    public static void main(String[] args) throws Exception {
        Method foo = TestMethodInvoke.class.getMethod("foo",int.class);

        for (int i = 1; i <= 17; i++) {
            foo.invoke(null, i);
        }
    }

    public static void foo(int i) {
        System.out.println(i);
    }
}
