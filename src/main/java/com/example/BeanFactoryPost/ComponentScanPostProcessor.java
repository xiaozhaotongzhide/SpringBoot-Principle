package com.example.BeanFactoryPost;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.stereotype.Component;

import java.io.IOException;

public class ComponentScanPostProcessor implements BeanDefinitionRegistryPostProcessor {


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanFactory) throws BeansException {

        try {
            ComponentScan componentScan = AnnotationUtils.findAnnotation(Config.class, ComponentScan.class);
            if (componentScan != null) {
                for (String p : componentScan.basePackages()) {
                    System.out.println(p);
                    //现在的包名:com.example.BeanFactoryPost.component要变成 -> classpath*:com/example/BeanFactoryPost/component/**/*.class
                    String packages = "classpath*:" + p.replace(".", "/") + "/**/*.class";
                    CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
                    AnnotationBeanNameGenerator generator = new AnnotationBeanNameGenerator();
                    Resource[] resources = new PathMatchingResourcePatternResolver().getResources(packages);
                    for (Resource resource : resources) {
                        MetadataReader metadataReader = factory.getMetadataReader(resource);
                        if (metadataReader.getAnnotationMetadata().hasAnnotation(Component.class.getName()) || metadataReader.getAnnotationMetadata().hasMetaAnnotation(Component.class.getName())) {
                            //生成beandefinition
                            AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                                    .genericBeanDefinition(metadataReader.getClassMetadata().getClassName())
                                    .getBeanDefinition();
                            //生成bean的名字
                            String beanName = generator.generateBeanName(beanDefinition, beanFactory);
                            //注册bean
                            beanFactory.registerBeanDefinition(beanName, beanDefinition);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
