package com.example.Application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Component1 {

    @Autowired
    private ApplicationEventPublisher context;

    public void register() {
        //解耦,不需要关心component2是谁
        log.info("用户注册");
        context.publishEvent(new UserRegisteredEvent(this));
    }
}
