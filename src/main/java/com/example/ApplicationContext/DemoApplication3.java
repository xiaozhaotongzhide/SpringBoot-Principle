package com.example.ApplicationContext;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;


/*
    ApplicationContext
 */
@SpringBootApplication
@Slf4j
public class DemoApplication3 {

    public static void main(String[] args) {
        testFileSystemXmlApplicationContext();
    }

    // ⬇️较为经典的容器, 基于 classpath 下 xml 格式的配置文件来创建
    private static void testClassPathXmlApplicationContext() {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("b01.xml");

        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }

        System.out.println(context.getBean(Bean2.class).getBean1());
    }

    // ⬇️基于磁盘路径下 xml 格式的配置文件来创建
    private static void testFileSystemXmlApplicationContext() {
        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(
                        "src\\main\\resources\\b01.xml");
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }

        System.out.println(context.getBean(Bean2.class).getBean1());
    }
    // ⬇️如何用BeanFactory 实现ClassPathXmlApplication
    private static void testBeanFactoryClassPathXmlApplication() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    }

    static class Bean1 {
        public Bean1() {
            log.info("构造 Bean1()");
        }


    }

    static class Bean2 {

        public Bean2() {
            log.info("构造 Bean2()");
        }

        @Autowired
        private Bean1 bean1;

        public Bean1 getBean1() {
            return bean1;
        }

        public void setBean1(Bean1 bean1) {
            this.bean1 = bean1;
        }
    }
}
