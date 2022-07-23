package com.example.a22;


import org.springframework.beans.DirectFieldAccessor;

import java.util.Date;

public class TestFieldAccessor {
    public static void main(String[] args) {
        MyBean myBean = new MyBean();
        DirectFieldAccessor beanWrapper = new DirectFieldAccessor(myBean);
        beanWrapper.setPropertyValue("a","10");
        beanWrapper.setPropertyValue("b","hello");
        beanWrapper.setPropertyValue("c","1999/03/04");
        System.out.println(myBean);
    }

    static class MyBean {
        private Integer a;
        private String b;
        private Date c;

        @Override
        public String toString() {
            return "MyBean{" +
                    "a=" + a +
                    ", b='" + b + '\'' +
                    ", c=" + c +
                    '}';
        }
    }
}
