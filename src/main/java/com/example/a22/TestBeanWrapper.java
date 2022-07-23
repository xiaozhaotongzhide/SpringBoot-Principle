package com.example.a22;

import lombok.Data;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Date;

public class TestBeanWrapper {

    public static void main(String[] args) {
        MyBean myBean = new MyBean();
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(myBean);
        beanWrapper.setPropertyValue("a","10");
        beanWrapper.setPropertyValue("b","hello");
        beanWrapper.setPropertyValue("c","1999/03/04");
        System.out.println(myBean);
    }

    @Data
    static class MyBean {
        private Integer a;
        private String b;
        private Date c;
    }
}
