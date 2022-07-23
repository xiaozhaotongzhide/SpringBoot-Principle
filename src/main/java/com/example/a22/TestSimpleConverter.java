package com.example.a22;

import org.springframework.beans.SimpleTypeConverter;

import java.util.Date;

public class TestSimpleConverter {
    public static void main(String[] args) {
        // 仅有类型转换功能
        SimpleTypeConverter typeConverter = new SimpleTypeConverter();
        Integer integer = typeConverter.convertIfNecessary("13", Integer.class);
        Date date = typeConverter.convertIfNecessary("1999/03/04", Date.class);
        System.out.println(integer + 1);
        System.out.println(date);
    }
}
