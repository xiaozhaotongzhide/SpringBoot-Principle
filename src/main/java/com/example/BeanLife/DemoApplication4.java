package com.example.BeanLife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Bean生命周期
 */
@SpringBootApplication
public class DemoApplication4 {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DemoApplication4.class, args);
        context.close();
    }
}
