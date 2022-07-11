package com.example.BeanFactoryPost;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;


import java.io.IOException;
import java.util.Set;

/*
    BeanFactory 后处理器的作用
 */
public class A05 {
    private static final Logger log = LoggerFactory.getLogger(A05.class);

    public static void main(String[] args) throws IOException {

        // ⬇️GenericApplicationContext 是一个【干净】的容器
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("config", Config.class);
        // 处理
        /*context.registerBean(ConfigurationClassPostProcessor.class); //@ComponentScan @Bean @Import @ImportResource
        context.registerBean(MapperScannerConfigurer.class , bd -> {
            bd.getPropertyValues().add("basePackage","com.example.BeanFactoryPost.mapper");
        }); //@MapperScanner*/
        //将自己写的处理@Component的bean后处理器添加进来
//        context.registerBean(ComponentScanPostProcessor.class);
        //将自己写的处理@Bean的后处理添加进来
        context.registerBean(AtBeanPostProcessor.class);
        //将自己写的处理@Mapper的后处理添加进来
        context.registerBean(MapperPostProcessor.class);
        // ⬇️初始化容器
        context.refresh();

        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);

        }


        // ⬇️销毁容器
        context.close();

        /*
            学到了什么
                a. @ComponentScan, @Bean, @Mapper 等注解的解析属于核心容器(即 BeanFactory)的扩展功能
                b. 这些扩展功能由不同的 BeanFactory 后处理器来完成, 其实主要就是补充了一些 bean 定义
         */
    }
}
