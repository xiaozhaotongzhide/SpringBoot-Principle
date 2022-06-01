package com.example.Application;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

/*
    BeanFactory与ApplicationContext的区别
 */
@SpringBootApplication
public class DemoApplication1 {

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException, IOException {

        ConfigurableApplicationContext context = SpringApplication.run(DemoApplication1.class, args);
        /**
         *      1.到底什么是BeanFactory
         *          - 它是ApplicationContext的父接口
         *          - 它才是Spring的核心容器,主要的ApplicationContext实现都是组合它的功能
         */
        //singletonObjects存放了我们的Bean
        System.out.println(context);
        /**
         *      2.BeanFactory 能干啥
         *          - 表面上只有getBean
         *          - 实际上控制反转基本的依赖注入,直至bean的生命周期的各种功能,都由它实现类提供
         */
        Field singletonFactories = DefaultSingletonBeanRegistry.class.getDeclaredField("singletonObjects");
        singletonFactories.setAccessible(true);
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        Map<String, Object> map = (Map<String, Object>) singletonFactories.get(beanFactory);
        map.entrySet().stream().filter(e -> e.getKey().startsWith("component"))
                .forEach(e -> {
                    System.out.println(e.getKey() + "=" + e.getValue());
                });

        //1.MessageSource国际化
        System.out.println(context.getMessage("hi", null, Locale.CHINA));
        System.out.println(context.getMessage("hi", null, Locale.ENGLISH));
        System.out.println(context.getMessage("hi", null, Locale.JAPANESE));

        //2.Resources通配符
        Resource[] resources = context.getResources("classpath*:META-INF/spring.factories");
        //Resource对资源文件的抽象
        for (Resource r : resources) {
            System.out.println(r);
        }

        //3.Environment配置文件的健值
        System.out.println(context.getEnvironment().getProperty("server.port"));
        System.out.println(context.getEnvironment().getProperty("java_home"));

        //4.PublishEvent事件发布器
//        context.publishEvent(new UserRegisteredEvent(context));
        //调用注册
        context.getBean(Component1.class).register();
    }
}
