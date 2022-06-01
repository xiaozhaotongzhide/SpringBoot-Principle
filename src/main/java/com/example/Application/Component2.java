package com.example.Application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Component2 {

    @EventListener
    public void a(UserRegisteredEvent event) {
        log.info("{}", event);
        log.info("发送验证码");
    }
}
