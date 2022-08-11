package com.example.a33;

import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.lang.reflect.Constructor;
import java.util.List;

public class A33_2 {
    public static void main(String[] args) throws Exception {

        // 添加 app 监听器
        SpringApplication app = new SpringApplication();
        app.addListeners(e -> System.out.println(e.getClass()));

        // 获取事件发送器实现类名
        List<String> names = SpringFactoriesLoader.loadFactoryNames(SpringApplicationRunListener.class, A33_2.class.getClassLoader());
        for (String name : names) {
            System.out.println(name);
            Class<?> aClass = Class.forName(name);
            Constructor<?> constructor = aClass.getConstructor(SpringApplication.class, String[].class);
            SpringApplicationRunListener springApplicationRunListener = (SpringApplicationRunListener) constructor.newInstance(app, args);

            //发布事件
            //发送一个事件Spring开始启动
            DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();
            springApplicationRunListener.starting(bootstrapContext);
            //环境信息准备完毕
            springApplicationRunListener.environmentPrepared(bootstrapContext, new StandardEnvironment());
            //在spring容器创建,并调用初始化器之后,发送此事件
            GenericApplicationContext context = new GenericApplicationContext();
            springApplicationRunListener.contextPrepared(context);
            //所以的bean definition 加载完毕
            springApplicationRunListener.contextLoaded(context);
            //spring容器初始化完成(refresh方法调用完毕)
            context.refresh();
            springApplicationRunListener.started(context);
            //SpringBoot启动完成
            springApplicationRunListener.running(context);
            //抛出异常
            springApplicationRunListener.failed(context, new Exception("出错啦"));

        }

        /*
            学到了什么
            a. 如何读取 spring.factories 中的配置
            b. run 方法内获取事件发布器 (得到 SpringApplicationRunListeners) 的过程, 对应步骤中
                1.获取事件发布器
                发布 application starting 事件1️⃣
                发布 application environment 已准备事件2️⃣
                发布 application context 已初始化事件3️⃣
                发布 application prepared 事件4️⃣
                发布 application started 事件5️⃣
                发布 application ready 事件6️⃣
                这其中有异常，发布 application failed 事件7️⃣
         */
    }


}
