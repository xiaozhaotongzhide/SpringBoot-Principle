# SpringBoot原理解析

### BeanFactory与ApplicationContext的区别

#### 1.到底什么是BeanFactory
![在这里插入图片描述](https://img-blog.csdnimg.cn/d59046698499452fb0695c533fe8898d.png)



> ​	它是ApplicationContext的父接口
> ​	它才是Spring的核心容器,主要的ApplicationContext实现都是组合它的功能

![在这里插入图片描述](https://img-blog.csdnimg.cn/8394e1b652de4aa0957ca7f8297b5d1b.png)


通过这个图片我们可以看出来,在ApplicationContext中有一个beanFactory在它中有singletonObjects,这里面存放了我们的所有bean

#### 2.BeanFactory 能干啥


> ​	表面上只有getBean
> ​	实际上控制反转基本的依赖注入,直至bean的生命周期的各种功能,都由它实现类提供

接下来我们通过反射来获取到BeanFactory中的

```java
Field singletonFactories = DefaultSingletonBeanRegistry.class.getDeclaredField("singletonObjects");
singletonFactories.setAccessible(true);
ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
Map<String, Object> map = (Map<String, Object>) singletonFactories.get(beanFactory);
map.entrySet().stream().filter(e -> e.getKey().startsWith("component"))
        .forEach(e -> {
            System.out.println(e.getKey() + "=" + e.getValue());
        });
```

我们来看这段代码,先通过反射获取到了singletonObjects之后ApplicationContext的getBeanFactory()获取BeanFactory,之后singletonFactories成员变量调用get这个操作获取BeanFactory()的singletonFactories

```java
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

@Component
@Slf4j
public class Component2 {

    @EventListener
    public void a(UserRegisteredEvent event) {
        log.info("{}", event);
        log.info("发送验证码");
    }
}

```

因为我在代码中添加了这个两个Bean,所有他会打印。

#### 3.ApplicationContext能干啥

ApplicationContext的功能来自于它整合的接口下面分别来介绍一下这四个接口的主要功能

![在这里插入图片描述](https://img-blog.csdnimg.cn/ea6faf3cb1584da9bb8217aa2332dac2.png)


##### 1.MessageSource国际化

```java
System.out.println(context.getMessage("hi", null, Locale.CHINA));
System.out.println(context.getMessage("hi", null, Locale.ENGLISH));
System.out.println(context.getMessage("hi", null, Locale.JAPANESE));
```

控制台输出

你好
Hello
こんにちは

![在这里插入图片描述](https://img-blog.csdnimg.cn/ac1d5978a38c4f8aadc525bac466b168.png)


