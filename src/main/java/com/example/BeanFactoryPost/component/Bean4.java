package com.example.BeanFactoryPost.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bean4 {

    private static final Logger log = LoggerFactory.getLogger(Bean4.class);

    public Bean4() {
        log.debug("我被 Spring 管理啦");
    }
}
