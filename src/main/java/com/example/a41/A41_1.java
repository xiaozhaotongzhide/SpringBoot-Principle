package com.example.a41;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class A41_1 {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(A41_1.class);
        context.getBean(MyService.class).doBusiness();
        context.close();
    }

    static class MyEvent extends ApplicationEvent{
        public MyEvent(Object source) {
            super(source);
        }
    }

    @Component
    @Slf4j
    static class MyService {

        @Autowired
        private ApplicationEventPublisher publisher;

        public void doBusiness(){
            log.info("主线业务");
            publisher.publishEvent(new MyEvent("myService.doBusiness"));
//            log.info("发送短信");
//            log.info("发送邮件");
        }
    }

    @Component
    @Slf4j
    static class smsApplicationListener implements ApplicationListener<MyEvent> {
        @Override
        public void onApplicationEvent(MyEvent event) {
            log.info("发送短信");
        }
    }

    @Component
    @Slf4j
    static class emailApplicationListener implements ApplicationListener<MyEvent> {
        @Override
        public void onApplicationEvent(MyEvent event) {
            log.info("发送邮件");
        }
    }
}
