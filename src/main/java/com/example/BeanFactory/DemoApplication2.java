package com.example.BeanFactory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.io.IOException;



/*
    BeanFactory容器
        不会主动调用BeanFactory
        不会主动添加Bean后处理器
        不会主动初始化单例
        不会解析beanFactory
 */
@SpringBootApplication
@Slf4j
public class DemoApplication2 {

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException, IOException {

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        //bean的定义(class, scope, 初始化, 销毁)

        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(Config.class).setScope("singleton").getBeanDefinition();
        beanFactory.registerBeanDefinition("config", beanDefinition);
        String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);//只有config没有解析@Configuration的功能
        }

        //添加注解处理器
        AnnotationConfigUtils.registerAnnotationConfigProcessors(beanFactory);
        /*beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);//只有config没有解析@Configuration的功能
        }*/

        //BeanFactory 后处理器
        beanFactory.getBeansOfType(BeanFactoryPostProcessor.class).values().stream().forEach(beanFactoryPostProcessor -> {
            beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);//不同的bean处理器根据自己的特点
        });
        beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);//只有config没有解析@Configuration的功能
        }

        //@Autowired功能没有生效
        //System.out.println(beanFactory.getBean(Bean1.class).getBean2());//null

        //Bean 后处理器,针对bean的生命周期的各个阶段提供扩展,例如@Autowired @Resourcey 根据其中的Order来进行排序默认Autowired比Common的后处理器要先进行处理
        beanFactory.getBeansOfType(BeanPostProcessor.class).values().stream().forEach(beanFactory::addBeanPostProcessor);
        //预先处理,如果不加这个用的时候才创建
//        beanFactory.preInstantiateSingletons();
        System.out.println(">>>>>>>>");
        System.out.println(beanFactory.getBean(Bean1.class).getBean2());//com.example.BeanFactory.DemoApplication2$Bean2@3b2c72c2
        System.out.println(beanFactory.getBean(Bean1.class).getInter());
    }

    @Configuration
    static class Config {

        @Bean
        public Bean1 bean1() {
            return new Bean1();
        }

        @Bean
        public Bean2 bean2() {
            return new Bean2();
        }

        @Bean
        public Bean3 bean3() {
            return new Bean3();
        }

        @Bean
        public Bean4 bean4() {
            return new Bean4();
        }

    }

    interface Inter {

    }

    static class Bean3 implements Inter {

    }

    static class Bean4 implements Inter {

    }

    static class Bean1 {
        public Bean1() {
            log.info("构造 Bean1()");
        }

        @Autowired
        private Bean2 bean2;

        public Bean2 getBean2() {
            return bean2;
        }

        @Autowired
        @Resource(name = "bean4")
        private Inter bean3;

        public Inter getInter() {
            return bean3;
        }
    }

    static class Bean2 {
        public Bean2() {
            log.info("构造 Bean2()");
        }

    }
}
