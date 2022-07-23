package com.example.a22.sub;

import org.springframework.core.GenericTypeResolver;

import java.lang.reflect.Type;

public class TestGenericType {
    public static void main(String[] args) {
        // 小技巧
        // 1. java api
        Type type = StudentDao.class.getGenericSuperclass();
//        System.out.println(type);
        // 2. spring api 1
        Class<?> aClass = GenericTypeResolver.resolveTypeArgument(StudentDao.class, BaseDao.class);
        System.out.println(aClass);
        // 3. spring api 2

    }

}
