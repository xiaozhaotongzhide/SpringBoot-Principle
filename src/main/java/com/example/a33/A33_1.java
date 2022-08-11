package com.example.a33;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * BOOT程序的构建
 */
@Configuration
public class A33_1 {

    public static void main(String[] args) throws Exception {

        System.out.println("1. 演示获取 Bean Definition 源");
        //Bean Definition可以是配置类可以是xml配置文件
        SpringApplication spring = new SpringApplication(A33_1.class);
        spring.setSources(Set.of("classpath:b01.xml"));
        System.out.println("2. 演示推断应用类型");
        //SERVLET类型
        Method deduceFromClasspath = WebApplicationType.class.getDeclaredMethod("deduceFromClasspath");
        deduceFromClasspath.setAccessible(true);
        System.out.println("应用类型为" + deduceFromClasspath.invoke(null));
        System.out.println("3. 演示 ApplicationContext 初始化器");
        //创建ApplicationContext
        //调用添加的初始化器对ApplicationContext做扩展
        spring.addInitializers(applicationContext -> {
            GenericApplicationContext genericApplicationContext = (GenericApplicationContext) applicationContext;
            genericApplicationContext.registerBean("bean3", Bean3.class);
        });
        System.out.println("4. 演示监听器与事件");
        spring.addListeners(event -> System.out.println("\t事件为:" + event.getClass()));
        System.out.println("5. 演示主类推断");
        Method deduceMainApplicationClass = SpringApplication.class.getDeclaredMethod("deduceMainApplicationClass");
        deduceMainApplicationClass.setAccessible(true);
        System.out.println("主类为" + deduceMainApplicationClass.invoke(spring));
        //打印所有的类
        ConfigurableApplicationContext context = spring.run(args);
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println("name:" + name + "来源" + context.getBeanFactory().getBeanDefinition(name).getResourceDescription());
        }

        context.close();
        /*
            学到了什么
            a. SpringApplication 构造方法中所做的操作
                1. 可以有多种源用来加载 bean 定义
                2. 应用类型推断
                3. 容器初始化器
                4. 演示启动各阶段事件
                5. 演示主类推断
         */
    }

    static class Bean1 {

    }

    static class Bean2 {

    }

    static class Bean3 {

    }

    @Bean
    public Bean2 bean2() {
        return new Bean2();
    }

    @Bean
    public TomcatServletWebServerFactory servletWebServerFactory() {
        return new TomcatServletWebServerFactory();
    }
}
