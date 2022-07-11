package com.example.a06;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;

@Slf4j
public class MyBean implements BeanNameAware, ApplicationContextAware, InitializingBean {

    @Override
    public void setBeanName(String name) {
        log.info("当前bean" + this + "名字叫" + name);

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("当前bean" + this + "容器叫" + applicationContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("当前bean初始化");
    }

    @Autowired
    public void aaa(ApplicationContext applicationContext) {
        log.info("当前bean" + this + "使用@Autowired容器是" + applicationContext);
    }

    @PostConstruct
    public void init() {
        log.info("当前bean" + this + "使用@PostConstruct初始化");
    }
}
