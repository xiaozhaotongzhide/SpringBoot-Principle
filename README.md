# Spring原理解析

## IOC


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

#### 3.ApplicationContext增强了什么

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

##### 2.Resources通配符

```java
Resource[] resources = context.getResources("classpath*:META-INF/spring.factories");
//Resource对资源文件的抽象
for (Resource r : resources) {
    System.out.println(r);
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/a58781d0f52a4c579afbae653f2199c0.png)
##### 3.Environment配置文件的健值

```java
System.out.println(context.getEnvironment().getProperty("server.port"));
System.out.println(context.getEnvironment().getProperty("java_home"));
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/163bccc0d800444d910a0d63b636d09f.png)

##### 4.PublishEvent事件发布器
最后一个是事件发布器类似于消息队列
这种场景可以用在购物车上,比如一个被用户添加到购物车的商品被店家下架这时用户购物车可以监听到事件发送者的发出的商品被下架来进行解耦。
在DemoApplication1文件中添加

```java
//调用注册
context.getBean(Component1.class).register();
```
 Component2
```java
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
Component1
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

```
UserRegisteredEvent
```java
public class UserRegisteredEvent extends ApplicationEvent {
    public UserRegisteredEvent(Object source) {
        super(source);
    }
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/3d33ad0cded848f49ec1be52aec0ec13.png)
### BeanFactory如何实现ApplicationContext功能
```java
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(Config.class).setScope("singleton").getBeanDefinition();
            @Configuration
    static class Config {

        @Bean
        public Bean1 bean1() {
            return new Bean1();
        }

        @Bean
        public Bean2 bean2() {
            return new Bean2();
        }

        @Bean
        public Bean3 bean3() {
            return new Bean3();
        }

        @Bean
        public Bean4 bean4() {
            return new Bean4();
        }

    }
```
创建一个beanFactory，并且把Config交给beanFactory

打印出来只有config没有解析@Configuration的功能

```java
AnnotationConfigUtils.registerAnnotationConfigProcessors(beanFactory);
```
这时我们添加一个这样注解处理器
![](https://img-blog.csdnimg.cn/549ec1b57ccb4824aef60a6c42893f2a.png)
这样我们的注解处理器就添加成功了,但是还是没有我们想要的bean
因为这只是把注解处理器给添加进来了,此时还需要调用一个postProcessBeanFactory后处理方法
![在这里插入图片描述](https://img-blog.csdnimg.cn/de58a20f9bea4b4e981587c1936705df.png)
此时我们来看一个bean1这个类
```java
    static class Bean1 {
        public Bean1() {
            log.info("构造 Bean1()");
        }

        @Autowired
        private Bean2 bean2;

        public Bean2 getBean2() {
            return bean2;
        }

        @Autowired
        @Resource(name = "bean4")
        private Inter bean3;

        public Inter getInter() {
            return bean3;
        }
    }
```
查看一下
```java
System.out.println(beanFactory.getBean(Bean1.class).getBean2());//null
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/0eae246bc7ca4b9b81bfb1c1ec9a258f.png)
说明现在@Bean注解被处理了,但是@Autowired这个注解任然没有被处理

```java
        //Bean 后处理器,针对bean的生命周期的各个阶段提供扩展,例如@Autowired @Resourcey 根据其中的Order来进行排序默认Autowired比Common的后处理器要先进行处理
        beanFactory.getBeansOfType(BeanPostProcessor.class).values().stream().forEach(beanFactory::addBeanPostProcessor);
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/8c64b2a4bdc8436696129ee0e95ef413.png)
这样就使用@Autowired

### ApplicationContext的常见实现
我们要用到的两个bean
```java
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
```

#### ClassPathXmlApplicationContext
较为经典的容器, 基于 classpath 下 xml 格式的配置文件来创建
```java
    // ⬇️较为经典的容器, 基于 classpath 下 xml 格式的配置文件来创建
    private static void testClassPathXmlApplicationContext() {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("b01.xml");

        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }

        System.out.println(context.getBean(Bean2.class).getBean1());
    }
```
xml配置文件
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean id="bean1" class="com.example.ApplicationContext.DemoApplication3.Bean1"/>

    <bean id="bean2" class="com.example.ApplicationContext.DemoApplication3.Bean2">
        <property name="bean1" ref="bean1"/>
    </bean>
</beans>
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/af93e9fa18834d469f7e7872b1831aa9.png)


#### FileSystemXmlApplicationContext
基于磁盘路径下 xml 格式的配置文件来创建

```java
    private static void testFileSystemXmlApplicationContext() {
        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(
                        "src\\main\\resources\\b01.xml");
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }

        System.out.println(context.getBean(Bean2.class).getBean1());
    }
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/679f2b30e51a46b9aadb59d03508807f.png)
#### 如何用BeanFactory 实现ClassPathXmlApplication
我们说ApplicationContext是beanfactory的功能增强,那么我们是否能利用beanfactory来完成ClassPathXmlApplicationContext的功能吗
步骤是这样的:先创建一个beanfactory之后创建一个XmlBeanDefinitionReader绑定beanFactory,XmlBeanDefinitionReader读取配置文件

```java
    // ⬇️如何用BeanFactory 实现ClassPathXmlApplication
    private static void testBeanFactoryClassPathXmlApplication() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        log.info("读取之前");
        for (String name : beanFactory.getBeanDefinitionNames()) {
            log.info(name);
        }
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
        reader.loadBeanDefinitions(new ClassPathResource("b01.xml"));
        log.info("读取之后");
        for (String name : beanFactory.getBeanDefinitionNames()) {
            log.info(name);
        }
    }
```

#### AnnotationConfigApplicationContext
较为经典的容器, 基于 java 配置类来创建

```java
    // ⬇️较为经典的容器, 基于 java 配置类来创建
    private static void testAnnotationConfigApplicationContext() {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(Config.class);

        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }

        System.out.println(context.getBean(Bean2.class).getBean1());
    }
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/d96e78751bad45bb8add8f2d4ea58bea.png)

#### AnnotationConfigServletWebServerApplicationContext
较为经典的容器, 基于 java 配置类来创建, 用于 web 环境
```java
    private static void testAnnotationConfigServletWebServerApplicationContext() {
        AnnotationConfigServletWebServerApplicationContext context =
                new AnnotationConfigServletWebServerApplicationContext(WebConfig.class);
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }
    }

```
web配置类

```java
    @Configuration
    static class WebConfig {
        @Bean
        public ServletWebServerFactory servletWebServerFactory(){
            return new TomcatServletWebServerFactory();
        }
        @Bean
        public DispatcherServlet dispatcherServlet() {
            return new DispatcherServlet();
        }
        @Bean
        public DispatcherServletRegistrationBean registrationBean(DispatcherServlet dispatcherServlet) {
            return new DispatcherServletRegistrationBean(dispatcherServlet, "/");
        }
        @Bean("/hello")
        public Controller controller1() {
            return (request, response) -> {
                response.getWriter().print("hello");
                return null;
            };
        }
    }
```
内嵌tomcat容器
![在这里插入图片描述](https://img-blog.csdnimg.cn/3f800a263b3042278a2e9601e3014532.png)

```java
        @Bean
        public ServletWebServerFactory servletWebServerFactory(){
            return new TomcatServletWebServerFactory();
        }
```
### SpringBean的生命周期
#### 自己设计一个Bean后处理器
Boot启动类
```java
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
```
Component类
```java
@Component
public class LifeCycleBean {
    private static final Logger log = LoggerFactory.getLogger(LifeCycleBean.class);

    public LifeCycleBean() {
        log.debug("构造");
    }

    @Autowired
    public void autowire(@Value("${JAVA_HOME}") String home) {
        log.debug("依赖注入: {}", home);
    }

    @PostConstruct
    public void init() {
        log.debug("初始化");
    }

    @PreDestroy
    public void destroy() {
        log.debug("销毁");
    }
}
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/ef4e5f1d123e4352882e04ced92af55a.png)
建立一个Bean后处理器
```java
@Component
public class MyBeanPostProcessor implements InstantiationAwareBeanPostProcessor, DestructionAwareBeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(MyBeanPostProcessor.class);

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        if (beanName.equals("lifeCycleBean"))
            log.debug("<<<<<< 销毁之前执行, 如 @PreDestroy");
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        if (beanName.equals("lifeCycleBean"))
            log.debug("<<<<<< 实例化之前执行, 这里返回的对象会替换掉原本的 bean");
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        if (beanName.equals("lifeCycleBean")) {
            log.debug("<<<<<< 实例化之后执行, 这里如果返回 false 会跳过依赖注入阶段");
//            return false;
        }
        return true;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        if (beanName.equals("lifeCycleBean"))
            log.debug("<<<<<< 依赖注入阶段执行, 如 @Autowired、@Value、@Resource");
        return pvs;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (beanName.equals("lifeCycleBean"))
            log.debug("<<<<<< 初始化之前执行, 这里返回的对象会替换掉原本的 bean, 如 @PostConstruct、@ConfigurationProperties");
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (beanName.equals("lifeCycleBean"))
            log.debug("<<<<<< 初始化之后执行, 这里返回的对象会替换掉原本的 bean, 如代理增强");
        return bean;
    }
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/29071e6fe9804840add00bbf133d8a4d.png)
#### 设计模式-模板方法
这是一种通过提前定义接口来做到增强方法的设计模式
```java
public class TestMethodTemplate {

    public static void main(String[] args) {
        MyBeanFactory beanFactory = new MyBeanFactory();
        beanFactory.addBeanPostProcessor(bean -> System.out.println("解析 @Autowired"));
        beanFactory.addBeanPostProcessor(bean -> System.out.println("解析 @Resource"));
        beanFactory.getBean();
    }

    //模板方法 Template Method Pattern
    static class MyBeanFactory {
        public Object getBean() {
            Object bean = new Object();
            System.out.println("构造" + bean);
            System.out.println("依赖注入" + bean);
            for (BeanPostProcessor beans : processors) {
                beans.inject(bean);
            }
            System.out.println("初始化" + bean);
            return bean;
        }

        private List<BeanPostProcessor> processors = new ArrayList<BeanPostProcessor>();

        public void addBeanPostProcessor(BeanPostProcessor processor) {
            processors.add(processor);
        }
    }

    interface BeanPostProcessor {
        void inject(Object bean);
    }
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/209b79f8c4d947e7a53800db2430da5e.png)
### 常见的Bean后处理器
#### AutowiredAnnotationBeanPostProcessor
这个后处理器主要处理的是,@Autowired自动注入,@Value值注入这两个注解
这个后处理器中最重要的一个方法就是findAutowiringMetadata他可以找到自动注入数据的方法或者属性

```java
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        // 1. 查找哪些属性、方法加了 @Autowired, 这称之为 InjectionMetadata
        AutowiredAnnotationBeanPostProcessor processor = new AutowiredAnnotationBeanPostProcessor();
        processor.setBeanFactory(beanFactory);

        Bean1 bean1 = new Bean1();
        System.out.println(bean1);
        processor.postProcessProperties(null, bean1, "bean1"); // 执行依赖注入 @Autowired @Value
        System.out.println(bean1);
```
还可以写成

```java
        Method findAutowiringMetadata = AutowiredAnnotationBeanPostProcessor.class.getDeclaredMethod("findAutowiringMetadata", String.class, Class.class, PropertyValues.class);
        findAutowiringMetadata.setAccessible(true);
        InjectionMetadata metadata = (InjectionMetadata) findAutowiringMetadata.invoke(processor, "bean1", Bean1.class, null);//获取Bean1上有@Value的方法
        System.out.println(metadata);
        metadata.inject(bean1, "bean1", null);
        System.out.println(bean1);
```
通过这个例子我们可以发现,@Autowired自动注入,@Value值注入这两个注解被解析分为两步.
1.findAutowiringMetadata先找到有这个两个注解的属性或者方法
2.inject对有这个注解的进行注入

#### CommonAnnotationBeanPostProcessor

主要处理

@PreDestroy,@PostConstruct,@Resource

```java
context.registerBean(CommonAnnotationBeanPostProcessor.class);
```

#### ConfigurationPropertiesBindingPostProcessor

@ConfigurationProperties解析这Springboot个注解

```java
ConfigurationPropertiesBindingPostProcessor.register(context.getDefaultListableBeanFactory());//@ConfigurationProperties解析这个注解
```

### 常见的beanFactoryPost

#### ComponentScanPostProcessor

这个注解主要扫描@ComponentScan @Bean @Import @ImportResource

```java
context.registerBean(ConfigurationClassPostProcessor.class); 
```

#### MapperScannerConfigurer

```java
context.registerBean(MapperScannerConfigurer.class , bd -> {
    bd.getPropertyValues().add("basePackage","com.example.BeanFactoryPost.mapper");
});
```

### 自制一个扫描@Component的BeanFactiryPost

步骤首先明确一下我们的需求要可以实现扫描ComponentScan

1.首先要先得到ComponentScan这个注解里面的值,同时把他拼装成类路径

2.获取这个类路径下的所有类,并且检查所有有@component,注解或者是派生注解

3.将这些有的beandefinition,注册到beanfactory

```java
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
```

#### 自制一个扫描@bean的BeanFactoryPost

思路

1.首先我们要获取那个Config配置类,同时找到被@Bean注解的成员变量

2.利用setFactoryMethodOnBean为工厂方法生成bean

3.利用registerBeanDefinition方法注册bean到beanfactory

```java
public class AtBeanPostProcessor implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanFactory) throws BeansException {
        try {
            CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
            MetadataReader metadataReader = factory.getMetadataReader(new ClassPathResource("com/example/BeanFactoryPost/Config.class"));
            //取出被@Bean注解的成员变量
            Set<MethodMetadata> methods = metadataReader.getAnnotationMetadata().getAnnotatedMethods(Bean.class.getName());
            for (MethodMetadata method : methods) {
                System.out.println(method);
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
                builder.setFactoryMethodOnBean(method.getMethodName(), "config");
                // 处理initMethod
                String initMethod = method.getAnnotationAttributes(Bean.class.getName()).get("initMethod").toString();
                if (initMethod.length() > 0) {
                    builder.setInitMethodName(initMethod);
                }
                // 配置自动装配的功能
                builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
                AbstractBeanDefinition bd = builder.getBeanDefinition();
                beanFactory.registerBeanDefinition(method.getMethodName(), bd);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```
#### 自制一个扫描@Mapper的BeanFactoryPost

分析

这个扫描@Mapper和扫描@Bean的最大的区别就是,@Mapper是一个接口,而@Bean是一个实现类,并且在我们生成@@Mapper的Bean的时候需要向里面注入SqlSessionFactory

在spring整合mybatis的过程中为我们提供了这样的Bean工厂类MapperFactoryBean

只需要在配置类中这样

```java
@Bean
public MapperFactoryBean<Mapper2> mapper2(SqlSessionFactory sqlSessionFactory) {
    MapperFactoryBean<Mapper2> factory = new MapperFactoryBean<>(Mapper2.class);
    factory.setSqlSessionFactory(sqlSessionFactory);
    return factory;
}
```

思路

1.先找出所有被@Mapper注解的接口

2.使用BeanDefinitionBuilder.genericBeanDefinition,同时调用setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE),自动注入sqlSessionFactory。

3.和之前的步骤一样进行beanFactory.registerBeanDefinition(classMetadata.getClassName(), beanDefinition);

```java
public class MapperPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanFactory) throws BeansException {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:com/example/BeanFactoryPost/mapper/**/*.class");
            CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
            for (Resource resource : resources) {
                MetadataReader metadataReader = factory.getMetadataReader(resource);
                ClassMetadata classMetadata = metadataReader.getClassMetadata();
                //判断是否是接口
                if (classMetadata.isInterface()) {
                    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(MapperFactoryBean.class)
                            .addConstructorArgValue(classMetadata.getClassName())
                            .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
                            .getBeanDefinition();
                    beanFactory.registerBeanDefinition(classMetadata.getClassName(), beanDefinition);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
```
### Aware 接口

1. Aware 接口用于注入一些与容器相关信息, 例如    a. BeanNameAware 注入 bean 的名字    b. BeanFactoryAware 注入 BeanFactory 容器    c. ApplicationContextAware 注入 ApplicationContext 容器    d. EmbeddedValueResolverAware ${}

### Bean初始化方法和销毁方法

#### 初始化

1.通过@PreDestroy注解

```
@PreDestroy
public void destroy1() {
    log.debug("销毁1");
}
```

2.通过实现InitializingBean

```
	implements InitializingBean 
    
    
    @Override
    public void afterPropertiesSet() throws Exception {
        log.debug("初始化2");
    }
```

3.通过@Bean(initMethod = "init3")

```
@Bean(initMethod = "init3")
public Bean1 bean1() {
    return new Bean1();
}
```

他们的顺序是从上到下

#### 销毁

1.通过@PreDestroy注解

```
@PreDestroy
public void destroy1() {
    log.debug("销毁1");
}
```

2.实现DisposableBean

```
implements DisposableBean
    @Override
    public void destroy() throws Exception {
        log.debug("销毁2");
    }
```

3.通过@Bean(destroyMethod = "destroy3")

```
@Bean(destroyMethod = "destroy3")
public Bean2 bean2() {
    return new Bean2();
}
```

### Scope

singleton, prototype, request, session, application

这五种域

**spring Bean的作用域:**

**scope=singleton(默认，单例，生成一个实例） 不是线程安全，性能高**

**scope=prototype(多线程, 生成多个实例）**

作用域：当把一个Bean定义设置为singleton作用域是，Spring IoC容器中只会存在一个共享的Bean实例，并且所有对Bean的

请求，只要id与该Bean定义相匹配，则只会返回该Bean的同一实例。值得强调的是singleton作用域是Spring中的缺省作用域。
prototype作用域：prototype作用域的Bean会导致在每次对该Bean请求（将其注入到另一个Bean中，或者以程序的方式调用容器的getBean

()方法）时都会创建一个新的Bean实例。根据经验，对有状态的Bean应使用prototype作用域，而对无状态的Bean则应该使用singleton作用

域。
对于具有prototype作用域的Bean，有一点很重要，即Spring不能对该Bean的整个生命周期负责。具有prototype作用域的Bean创建后交由调

用者负责销毁对象回收资源。
简单的说：
singleton 只有一个实例，也即是单例模式。
prototype访问一次创建一个实例，相当于new。 
应用场合：
1.需要回收重要资源(数据库连接等)的事宜配置为singleton，如果配置为prototype需要应用确保资源正常回收。
2.有状态的Bean配置成singleton会引发未知问题，可以考虑配置为prototyp

## AOP

### ajc增强

优点:不是调用代理来增强,可以增强代理类

刚开始运行这个demo的时候没有增强效果,记住一定要先编译



上网查询之后发现

这是由于`idea`中在执行代码之前会默认编译一遍代码，这本来是正常的，可是，如果使用`maven`来编译代码，会在执行代码前将`maven`编译的代码覆盖，这就会导致`maven`的`ajc`编译器增强的代码被覆盖，所以会看不到最终的运行效果。 

### agent增强

这两种方法都没有流行起来



### 动态代理

通过动态代理有两种方法一种是jdk动态代理和cglib动态代理。这两种最大的区别就是,jdk产生的代理类和目标类是同级关系,cglib产生的代理类和目标类是父子关系。

说到AOP我们就来说一下,aop是切面控制,切面是由切点和通知组成的一种控制器

这是spring中的一种环绕通知。

```java
MethodInterceptor adivce = invocation -> {
    System.out.println("before...");
    Object result = invocation.proceed();
    System.out.println("after...");
    return result;
};
```

这是spring中的切点实现。

```java
AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
pointcut.setExpression("execution(* foo())");
```

他们可以组成一个切面

```java
DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, adivce);
```

由于spring中有jdk和动态代理两种,他们在不同的情况会有不同的实现

> 1.实现接口,用jdk1实现
> 2.目标没有实现接口,用cglib实现
> 3.proxyTargetClass = true,总是用cglib实现

下面来看一下spring中aop的动态代理实现。

```java
public class A15 {
   
    public static void main(String[] args) {
        //1.准备好切点
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(* foo())");
        //2.准备好通知
        MethodInterceptor adivce = invocation -> {
            System.out.println("before...");
            Object result = invocation.proceed();
            System.out.println("after...");
            return result;
        };
        //3.备好切面
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, adivce);
        //4.创建代理
        /*
            1.实现接口,用jdk1实现
            2.目标没有实现接口,用cglib实现
            3.proxyTargetClass = true,总是用cglib实现
         */
        Target1 target1 = new Target1();
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(target1);
        factory.addAdvisor(advisor);
        factory.setInterfaces(target1.getClass().getInterfaces());
        factory.setProxyTargetClass(true);
        I1 proxy = (I1) factory.getProxy();
        System.out.println(proxy.getClass());
        proxy.foo();
        proxy.bar();

    }

    interface I1 {
        void foo();

        void bar();
    }

    static class Target1 implements I1 {
        public void foo() {
            System.out.println("target1 foo");
        }

        public void bar() {
            System.out.println("target1 bar");
        }
    }

    static class Target2 {
        public void foo() {
            System.out.println("target2 foo");
        }

        public void bar() {
            System.out.println("target2 bar");
        }
    }
}
```

### 切点匹配

切面匹配基础的有两种方式,一种是匹配方法的名字另一种是根据注解比配,例子如下

```java
public class A16 {

    public static void main(String[] args) throws NoSuchMethodException {
        AspectJExpressionPointcut pointcut1 = new AspectJExpressionPointcut();
        pointcut1.setExpression("execution(* bar())");
        System.out.println(pointcut1.matches(T1.class.getMethod("foo"), T1.class));
        System.out.println(pointcut1.matches(T1.class.getMethod("bar"), T1.class));

        AspectJExpressionPointcut pointcut2 = new AspectJExpressionPointcut();
        pointcut2.setExpression("@annotation(org.springframework.transaction.annotation.Transactional)");
        System.out.println(pointcut2.matches(T1.class.getMethod("foo"), T1.class));
        System.out.println(pointcut2.matches(T1.class.getMethod("bar"), T1.class));
    }

    static class T1 {
        @Transactional
        public void foo() {
        }

        public void bar() {
        }
    }

}
```

但是spring中的切点匹配都不是,比如@Transactional这个注解不只能放在方法上还可以放在类上。

```java
@Transactional
static class T2 {
    public void foo() {
    }
}

@Transactional
interface I3 {
    void foo();
}

static class T3 implements I3 {
    public void foo() {
    }
}
```

spring是通过实现StaticMethodMatcherPointcut这个接口来完成对于类上注解的。

```java
StaticMethodMatcherPointcut pt3 = new StaticMethodMatcherPointcut() {
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        //找到方法上的注解信息
        MergedAnnotations from = MergedAnnotations.from(method);
        if (from.isPresent(Transactional.class)) {
            return true;
        }
        MergedAnnotations classFrom = MergedAnnotations.from(targetClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
        if (classFrom.isPresent(Transactional.class)) {
            return true;
        }
        return false;
    }
};

System.out.println(pt3.matches(T1.class.getMethod("foo"), T1.class));
System.out.println(pt3.matches(T1.class.getMethod("bar"), T1.class));
System.out.println(pt3.matches(T1.class.getMethod("foo"), T2.class));
System.out.println(pt3.matches(T1.class.getMethod("foo"), T3.class));
```

### Spring关于AOP的处理

Spring基于注解的Aop操作,主要是通过一个后处理器来自动配置的**AnnotationAwareAspectJAutoProxyCreator**他有两个特别重要的方法

 * 第一个方法findEligibleAdvisors,这个方法的作用是找到有资格的Advisors,也就是找到存在的切点
 * 第二个方法wrapIfNecessary,这个方法用来创建代理对象

```java
public class A17 {
    public static void main(String[] args) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("target1",Target1.class);
        context.registerBean("aspect1",Aspect1.class);
        context.registerBean("config",Config.class);
        context.registerBean(ConfigurationClassPostProcessor.class);

        /**
         * 这个类实现BeanPostProcessor
         * 在bean的各个声明阶段 创建 (*) -> 依赖注入 -> 初始化(*)
         */
        context.registerBean(AnnotationAwareAspectJAutoProxyCreator.class);
        context.refresh();
        /**
         * AnnotationAwareAspectJAutoProxyCreator中有两个重要的方法,
         * 第一个方法findEligibleAdvisors,这个方法的作用是找到有资格的Advisors,也就是找到存在的切点
         * 第二个方法wrapIfNecessary,这个方法用来创建代理对象
         */
        for (String beanDefinitionName : context.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }
        Target1 aspect1 = (Target1) context.getBean("target1");
        aspect1.foo();
    }

    static class Target1 {
        public void foo() {
            System.out.println("target1 foo");
        }
    }

    static class Target2 {
        public void bar() {
            System.out.println("target1 bar");
        }
    }

    /*
    高级切面类
     */
    @Aspect
    static class Aspect1 {

        @Before("execution(* foo())")
        public void before() {
            System.out.println("aspect1 before...");
        }

        @After("execution(* foo())")
        public void after() {
            System.out.println("aspect1 after...");
        }
    }

    @Configuration
    static class Config {

        @Bean
        public Advisor advisor3(MethodInterceptor advisor3) {
            AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
            pointcut.setExpression("execution(* foo())");
            return new DefaultPointcutAdvisor(pointcut, advisor3);
        }

        @Bean
        public MethodInterceptor advice3() {
            /**
             * 内联变量
             */
            return invocation -> {
                System.out.println("before...");
                Object result = invocation.proceed();
                System.out.println("after...");
                return result;
            };
        }
    }
}
```

对于spring来说高级切面也会转换成为低级切面,

AnnotationAwareAspectJAutoProxyCreator**创建代理的时机**

* 1.初始化之后(无循环依赖时)

* 2.实例创建之后,依赖注入前(有循环依赖)

关于切面顺序,高级切面在低级切面之后触发。

![image-20220707153644403](http://cdn.zhaodapiaoliang.top/PicGo/image-20220707153644403.png)

```java
/*
高级切面类
 */
@Aspect
@Order(1)
static class Aspect1 {

    @Before("execution(* foo())")
    public void before() {
        System.out.println("aspect1 before...");
    }

    @After("execution(* foo())")
    public void after() {
        System.out.println("aspect1 after...");
    }
}

@Configuration
static class Config {

    @Bean
    public Advisor advisor3(MethodInterceptor advisor3) {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(* foo())");
        DefaultPointcutAdvisor defaultPointcutAdvisor = new DefaultPointcutAdvisor(pointcut, advisor3);
        defaultPointcutAdvisor.setOrder(2);
        return defaultPointcutAdvisor;
    }

    @Bean
    public MethodInterceptor advice3() {
        /**
         * 内联变量
         */
        return invocation -> {
            System.out.println("before...");
            Object result = invocation.proceed();
            System.out.println("after...");
            return result;
        };
    }
}
```

![image-20220707154023090](http://cdn.zhaodapiaoliang.top/PicGo/image-20220707154023090.png)

### 高级切面转换为低级切面

我们说最后spring容器中都是以低级切面的方式存在下面的代码简单模拟了高级切面转换为低级切面

```java
public class A17_1 {
    public static void main(String[] args) {
        ArrayList<Advisor> list = new ArrayList<>();
        for (Method m : Aspect1.class.getDeclaredMethods()) {
            //解析结点
            if (m.isAnnotationPresent(Before.class)) {
                //解析切点
                String expression = m.getAnnotation(Before.class).value();
                AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
                pointcut.setExpression(expression);
                //通知类
                AspectJMethodBeforeAdvice advice = new AspectJMethodBeforeAdvice(m, pointcut, new SingletonAspectInstanceFactory(new Aspect1()));
                //切面
                Advisor advisor = new DefaultPointcutAdvisor(pointcut, advice);
                list.add(advisor);
            }
        }
        for (Advisor advisor : list) {
            System.out.println(advisor);
        }

    }

    @Aspect
    static class Aspect1 {

        @Before("execution(* foo())")
        public void before() {
            System.out.println("aspect1 before...");
        }

        @Before("execution(* foo())")
        public void after() {
            System.out.println("aspect1 after...");
        }
    }
}
```

### 统一转换成环绕通知

其实无论 ProxyFactory 基于哪种方式创建代理, 最后干活(调用 advice)的是一个 MethodInvocation 对象
    a. 因为 advisor 有多个, 且一个套一个调用, 因此需要一个调用链对象, 即 MethodInvocation
    b. MethodInvocation 要知道 advice 有哪些, 还要知道目标, 调用次序如下

    将 MethodInvocation 放入当前线程
        |-> before1 ----------------------------------- 从当前线程获取 MethodInvocation
        |                                             |
        |   |-> before2 --------------------          | 从当前线程获取 MethodInvocation
        |   |                              |          |
        |   |   |-> target ------ 目标   advice2    advice1
        |   |                              |          |
        |   |-> after2 ---------------------          |
        |                                             |
        |-> after1 ------------------------------------
    c. 从上图看出, 环绕通知才适合作为 advice, 因此其他 before、afterReturning 都会被转换成环绕通知
    d. 统一转换为环绕通知, 体现的是设计模式中的适配器模式
        - 对外是为了方便使用要区分 before、afterReturning
        - 对内统一都是环绕通知, 统一用 MethodInterceptor 表示

此步获取所有执行时需要的 advice (静态)
    a. 即统一转换为 MethodInterceptor 环绕通知, 这体现在方法名中的 Interceptors 上
    b. 适配如下

      - MethodBeforeAdviceAdapter 将 @Before AspectJMethodBeforeAdvice 适配为 MethodBeforeAdviceInterceptor
      - AfterReturningAdviceAdapter 将 @AfterReturning AspectJAfterReturningAdvice 适配为 AfterReturningAdviceInterceptor

这个操作通过`proxyFactory.getInterceptorsAndDynamicInterceptionAdvice`来完成

这个方法返回了`methodInterceptorList`,之后根据这个创建调用链

```
MethodInvocation methodInvocation = new ReflectiveMethodInvocation(
        null, target, Target.class.getMethod("foo"), new Object[0], Target.class, methodInterceptorList
);
```

### 设计模式-适配器模式

在spring中AOP操作统一转换成环绕通知这个过程用到了设计模式中的**适配器模式**

适配器模式（Adapter Pattern）是作为两个不兼容的接口之间的桥梁。这种类型的设计模式属于结构型模式，它结合了两个独立接口的功能。

这种模式涉及到一个单一的类，该类负责加入独立的或不兼容的接口功能。举个真实的例子，读卡器是作为内存卡和笔记本之间的适配器。您将内存卡插入读卡器，再将读卡器插入笔记本，这样就可以通过笔记本来读取内存卡。

下面看一下spring源码，

```java
class MethodBeforeAdviceAdapter implements AdvisorAdapter, Serializable {

   @Override
   public boolean supportsAdvice(Advice advice) {
      return (advice instanceof MethodBeforeAdvice);
   }

   @Override
   public MethodInterceptor getInterceptor(Advisor advisor) {
      MethodBeforeAdvice advice = (MethodBeforeAdvice) advisor.getAdvice();
      return new MethodBeforeAdviceInterceptor(advice);
   }

}
```

```java
@SuppressWarnings("serial")
class AfterReturningAdviceAdapter implements AdvisorAdapter, Serializable {

   @Override
   public boolean supportsAdvice(Advice advice) {
      return (advice instanceof AfterReturningAdvice);
   }

   @Override
   public MethodInterceptor getInterceptor(Advisor advisor) {
      AfterReturningAdvice advice = (AfterReturningAdvice) advisor.getAdvice();
      return new AfterReturningAdviceInterceptor(advice);
   }

}
```

## WEB

### DispatcherServlet-MVC的核心

SpringMVC的核心就是DispatcherServlet，DispatcherServlet实质也是一个HttpServlet。DispatcherSevlet负责将请求分发，所有的请求都有经过它来统一分发。

#### DispatcherServlet快速入门

DispatcherServlet默认第一次访问的时候才创建,可以通过配置loadOnStartup来让mvc启动的时刻

@EnableConfigurationProperties({WebMvcProperties.class, ServerProperties.class})

我们可以通过在，配置类中添加这个注解来注入配置类

下面是模拟了一个DispatcherServlet的启动过程

```java
// ⬅️内嵌 web 容器工厂
@Bean
public TomcatServletWebServerFactory tomcatServletWebServerFactory(ServerProperties serverProperties) {
    return new TomcatServletWebServerFactory(serverProperties.getPort());
}

// ⬅️创建 DispatcherServlet
@Bean
public DispatcherServlet dispatcherServlet() {
    return new DispatcherServlet();
}

// ⬅️注册 DispatcherServlet, Spring MVC 的入口
// DispatcherServlet在tomcat中运行
@Bean
public DispatcherServletRegistrationBean dispatcherServletRegistrationBean(DispatcherServlet dispatcherServlet, WebMvcProperties mvcProperties) {
    DispatcherServletRegistrationBean registrationBean = new DispatcherServletRegistrationBean(dispatcherServlet, "/");
    int loadOnStartup = mvcProperties.getServlet().getLoadOnStartup();
    registrationBean.setLoadOnStartup(loadOnStartup);
    return registrationBean;
}
```

#### DispatcherServlet构造方法

```java
protected void initStrategies(ApplicationContext context) {
   initMultipartResolver(context);				//文件解析器
   initLocaleResolver(context);					//国际化解析器
   initThemeResolver(context);					//主题样式解析器
   initHandlerMappings(context);				//@Mapping注解解析器
   initHandlerAdapters(context);				//控制器方法适配器
   initHandlerExceptionResolvers(context);		//异常解析器
   initRequestToViewNameTranslator(context);	//请求名转换器
   initViewResolvers(context);					//试图解析器
   initFlashMapManager(context);				//flash映射器
}
```

##### RequestMappingHandlerMapping

作用解析@RequestMapping及其衍生注解,生成路径与控制器关系,在初始化时就生成

下面我们来看一下这个HandlerMapping

创建下面这个controller

```java
@Controller
public class Controller1 {

    private static final Logger log = LoggerFactory.getLogger(Controller1.class);

    @GetMapping("/test1")
    public ModelAndView test1() throws Exception {
        log.debug("test1()");
        return null;
    }

    @PostMapping("/test2")
    public ModelAndView test2(@RequestParam("name") String name) {
        log.debug("test2({})", name);
        return null;
    }

    @PutMapping("/test3")
    public ModelAndView test3(String token) {
        log.debug("test3({})", token);
        return null;
    }

    @RequestMapping("/test4")
    public User test4() {
        log.debug("test4");
        return new User("张三", 18);
    }

}
```

在主类中创建测试方法

```java
public class A20 {
    public static void main(String[] args) throws Exception {
        AnnotationConfigServletWebServerApplicationContext context =
                new AnnotationConfigServletWebServerApplicationContext(WebConfig.class);
        //作用解析@RequestMapping,生成路径与控制器关系,在初始化时就生成
        RequestMappingHandlerMapping handlerMapping = context.getBean(RequestMappingHandlerMapping.class);

        //获取映射结果,key是请求方式和路径,value是控制器方法
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        handlerMethods.forEach((k, v) -> {
            System.out.println(k + "=" + v);
        });

        //请求来了,获取控制器方法,返回一个处理器链对象
        HandlerExecutionChain chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/test1"));
        System.out.println(chain);
    }
}
```

这个是输出结果

```
{POST [/test2]}=com.example.a20.Controller1#test2(String)
{PUT [/test3]}=com.example.a20.Controller1#test3(String)
{GET [/test1]}=com.example.a20.Controller1#test1()
{ [/test4]}=com.example.a20.Controller1#test4()
15:47:25.449 [main] DEBUG org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping - Mapped to com.example.a20.Controller1#test1()
HandlerExecutionChain with [com.example.a20.Controller1#test1()] and 0 interceptors
```

##### RequestMappingHandlerAdapter

有了之前的操作我们知道了该调用那个方法,那个路径,Adapter负责去调用，其最重要的方法invokeHandlerMethod因为是一个protected方法所以我们在我们的包下建立一个他的子类，重写修饰符。

```java
public class MyRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {
    @Override
    public ModelAndView invokeHandlerMethod(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        return super.invokeHandlerMethod(request, response, handlerMethod);
    }
}
```

```java
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test1");
        MockHttpServletResponse response = new MockHttpServletResponse();

		//请求来了,MyRequestMappingHandlerAdapter,会根据request和HandlerExecutionChain来调用方法
		MyRequestMappingHandlerAdapter HandlerAdapter = context.getBean(MyRequestMappingHandlerAdapter.class);
		HandlerAdapter.invokeHandlerMethod(request, response, (HandlerMethod) chain.getHandler());
```

输出结果：

`16:17:33.502 [main] DEBUG com.example.a20.Controller1 - test1()`

由此可以看出我们的方法被调用了

##### HandlerMethodArgumentResolver

类实现这个接口可以做到自定义参数解析器

supportsParameter判断支持那种参数

resolveArgument完成对参数的解析

```java
// 例如经常需要用到请求头中的 token 信息, 用下面注解来标注由哪个参数来获取它
// token=令牌
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Token {
}
```

```java
public class TokenArgumentResolver implements HandlerMethodArgumentResolver {

    //是否支持某个参数
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Token token = parameter.getParameterAnnotation(Token.class);
        return token != null;
    }

    //解析参数
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        return webRequest.getHeader("token");
    }
}
```

需要在MyRequestMappingHandlerAdapter注册一下

```java
@Bean
public MyRequestMappingHandlerAdapter requestMappingHandlerAdapter() {
    // ⬅️3.1 创建自定义参数处理器
    TokenArgumentResolver tokenArgumentResolver = new TokenArgumentResolver();
    MyRequestMappingHandlerAdapter handlerAdapter = new MyRequestMappingHandlerAdapter();
    ArrayList<HandlerMethodArgumentResolver> resolverList = new ArrayList<>();
    resolverList.add(tokenArgumentResolver);
    handlerAdapter.setCustomArgumentResolvers(resolverList);;
    return handlerAdapter;
}
```

##### HandlerMethodReturnValueHandler

类实现这个接口可以做到自定义返回值类型

supportsReturnType判断支持那种参数

handleReturnValue完成对返回值的解析

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Yml {
}
```

```java
public class YmlReturnValueHandler implements HandlerMethodReturnValueHandler {

    //是否支持某个参数
    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        Yml yml = returnType.getMethodAnnotation(Yml.class);
        return yml != null;
    }

    //返回值
    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        //转换结果为yml
        String str = new Yaml().dump(returnValue);
        //返回想应
        HttpServletResponse response = webRequest.getNativeRequest(HttpServletResponse.class);
        response.setContentType("text/plain;charset=utf-8");
        response.getWriter().print(str);

        //设置请求处理完毕,这个不需要进行视图解析
        mavContainer.setRequestHandled(true);
    }
}
```

和上面一样需要RequestMappingHandlerAdapter注册

```java
@Bean
public MyRequestMappingHandlerAdapter requestMappingHandlerAdapter() {
	// ⬅️3.2 创建自定义返回值处理器
    MyRequestMappingHandlerAdapter handlerAdapter = new MyRequestMappingHandlerAdapter();
    YmlReturnValueHandler ymlReturnValueHandler = new YmlReturnValueHandler();
    ArrayList<HandlerMethodReturnValueHandler> returnList = new ArrayList<>();
    returnList.add(ymlReturnValueHandler);
    handlerAdapter.setCustomReturnValueHandlers(returnList);
    return handlerAdapter;
}
```

### 参数解析器

MVC最重要的特征之一就是自动参数解析

#### 不同的参数解析器

在进行解析之前首先要创建一个请求

```java
private static HttpServletRequest mockRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("name1", "zhangsan");
    request.setParameter("name2", "lisi");
    request.addPart(new MockPart("file", "abc", "hello".getBytes(StandardCharsets.UTF_8)));
    Map<String, String> map = new AntPathMatcher().extractUriTemplateVariables("/test/{id}", "/test/123");
    System.out.println(map);
    request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, map);
    request.setContentType("application/json");
    request.setCookies(new Cookie("token", "123456"));
    request.setParameter("name", "张三");
    request.setParameter("age", "18");
    request.setContent("""
                {
                    "name":"李四",
                    "age":20
                }
            """.getBytes(StandardCharsets.UTF_8));

    return new StandardServletMultipartResolver().resolveMultipart(request);
}
```

创建Controller

```java
    static class Controller {
        public void test(@RequestParam("name1") String name1, // name1=张三
                         String name2,                        // name2=李四
                         @RequestParam("age") int age,        // age=18
                         @RequestParam(name = "home", defaultValue = "${JAVA_HOME}") String home1, // spring 获取数据
                         @RequestParam("file") MultipartFile file, // 上传文件
                         @PathVariable("id") int id,               //  /test/124   /test/{id}
                         @RequestHeader("Content-Type") String header, @CookieValue("token") String token, @Value("${JAVA_HOME}") String home2, // spring 获取数据  ${} #{}
                         HttpServletRequest request,          // request, response, session ...
                         @ModelAttribute("abc") User user1,          // name=zhang&age=18
                         User user2,                          // name=zhang&age=18
                         @RequestBody User user3              // json
        ) {
        }
    }

    static class User {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{" + "name='" + name + '\'' + ", age=" + age + '}';
        }
    }
```

开始测试

```java
public static void main(String[] args) throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(WebConfig.class);
    DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();

    // 准备测试 Request
    HttpServletRequest request = mockRequest();

    // 要点1. 控制器方法被封装为 HandlerMethod
    HandlerMethod handlerMethod = new HandlerMethod(new Controller(), Controller.class.getMethod("test", String.class, String.class, int.class, String.class, MultipartFile.class, int.class, String.class, String.class, String.class, HttpServletRequest.class, User.class, User.class, User.class));

    // 要点2. 准备对象绑定与类型转换
    ServletRequestDataBinderFactory factory = new ServletRequestDataBinderFactory(null, null);

    // 要点3. 准备 ModelAndViewContainer 用来存储中间 Model 结果
    ModelAndViewContainer container = new ModelAndViewContainer();

    // 要点4. 解析每个参数值
    for (MethodParameter parameter : handlerMethod.getMethodParameters()) {

        //初始化参数名称解析器
        parameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());

        //组合解析器如果一个一个解析太复杂了
        //组合器模式
        HandlerMethodArgumentResolverComposite composite = new HandlerMethodArgumentResolverComposite();
        List list = new ArrayList();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        list.add(converter);
        composite.addResolvers(
                //解析@RequestParam
                new RequestParamMethodArgumentResolver(beanFactory, false),
                //解析@PathVariable
                new PathVariableMethodArgumentResolver(),
                //解析@CookieValue
                new ServletCookieValueMethodArgumentResolver(beanFactory),
                //解析@RequestHeader
                new RequestHeaderMethodArgumentResolver(beanFactory),
                //解析@Value
                new ExpressionValueMethodArgumentResolver(beanFactory),
                //解析HttpServletRequest
                new ServletRequestMethodArgumentResolver(),
                //解析@ModelAttribute
                new ServletModelAttributeMethodProcessor(false),
                //解析@RequestBody
                new RequestResponseBodyMethodProcessor(list),
                //解析省略@ModelAttribute(优先处理对象)
                new ServletModelAttributeMethodProcessor(true),
                //解析省略@RequestParam
                new RequestParamMethodArgumentResolver(beanFactory, true));
        if (composite.supportsParameter(parameter)) {
            //支持此参数
            Object v = composite.resolveArgument(parameter, container, new ServletWebRequest(request), factory);
            System.out.println("[" + parameter.getParameterIndex() + "] " + parameter.getParameterType().getSimpleName() + " " + parameter.getParameterName() + "->" + v);

        } else {
            System.out.println("[" + parameter.getParameterIndex() + "] " + parameter.getParameterType().getSimpleName() + " " + parameter.getParameterName());
        }
    }
}
```

从中学到了

a. 每个参数处理器能干啥
    1) 看是否支持某种参数
    2) 获取参数的值
b. 组合模式在 Spring 中的体现
c. @RequestParam, @CookieValue 等注解中的参数名、默认值, 都可以写成活的, 即从 ${ } #{ }中获取

#### 组合模式

如上面所说的每一个参数解析器都有supportsParameter方法和resolveArgument方法,只有支持这个参数才会解析。我们不能遍历每一个参数解析器于是这里用到了组合模式

组合模式（Composite Pattern），又叫部分整体模式，是用于把一组相似的对象当作一个单一的对象。组合模式依据树形结构来组合对象，用来表示部分以及整体层次。这种类型的设计模式属于结构型模式，它创建了对象组的树形结构。

这种模式创建了一个包含自己对象组的类。该类提供了修改相同对象组的方式。

#### 数据绑定和类型转换

##### 类型转换接口

SpringMVC提供的第一套接口

![image-20220714221135995](http://cdn.zhaodapiaoliang.top/PicGo/image-20220714221135995.png)

* Printer 把其它类型转为 String

* Parser 把 String 转为其它类型

* Formatter 综合 Printer 与 Parser 功能

* Converter 把类型 S 转为类型 T

* Printer、Parser、Converter 经过适配转换成 GenericConverter 放入 Converters 集合

* FormattingConversionService 利用其它们实现转换

      

JDK提供的第二套接口

![image-20220714221212763](http://cdn.zhaodapiaoliang.top/PicGo/image-20220714221212763.png)

* PropertyEditor 把 String 与其它类型相互转换
* PropertyEditorRegistry 可以注册多个 PropertyEditor 对象
* 与第一套接口直接可以通过 FormatterPropertyEditorAdapter 来进行适配

![image-20220714221235802](http://cdn.zhaodapiaoliang.top/PicGo/image-20220714221235802.png)

高层接口

* 它们都实现了 TypeConverter 这个高层转换接口，在转换时，会用到 TypeConverter Delegate 委派ConversionService 与 PropertyEditorRegistry 真正执行转换（Facade 门面模式）
    * 首先看是否有自定义转换器, @InitBinder 添加的即属于这种 (用了适配器模式把 Formatter 转为需要的 PropertyEditor)
    * 再看有没有 ConversionService 转换
    * 再利用默认的 PropertyEditor 转换
    * 最后有一些特殊处理
* SimpleTypeConverter 仅做类型转换
* BeanWrapperImpl 为 bean 的属性赋值，当需要时做类型转换，走 Property
* DirectFieldAccessor 为 bean 的属性赋值，当需要时做类型转换，走 Field
* ServletRequestDataBinder 为 bean 的属性执行绑定，当需要时做类型转换，根据 directFieldAccess 选择走 Property 还是 Field，具备校验与获取校验结果功能

##### 高层接口实战

准备一个实体类让其绑定

```java
@Data
static class MyBean {
    private Integer a;
    private String b;
    private Date c;

    @Override
    public String toString() {
        return "MyBean{" +
                "a=" + a +
                ", b='" + b + '\'' +
                ", c=" + c +
                '}';
    }
}
```

测试SimpleTypeConverter

```java
public class TestSimpleConverter {
    public static void main(String[] args) {
        // 仅有类型转换功能
        SimpleTypeConverter typeConverter = new SimpleTypeConverter();
        Integer integer = typeConverter.convertIfNecessary("13", Integer.class);
        Date date = typeConverter.convertIfNecessary("1999/03/04", Date.class);
        System.out.println(integer + 1);
        System.out.println(date);
    }
}
```

测试BeanWrapper

```java
public class TestBeanWrapper {

    public static void main(String[] args) {
        MyBean myBean = new MyBean();
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(myBean);
        beanWrapper.setPropertyValue("a","10");
        beanWrapper.setPropertyValue("b","hello");
        beanWrapper.setPropertyValue("c","1999/03/04");
        System.out.println(myBean);
    }

    @Data
    static class MyBean {
        private Integer a;
        private String b;
        private Date c;
    }
}
```

测试DataBinder

```java
public class TestDataBinder {

    public static void main(String[] args) {
        MyBean myBean = new MyBean();
        DataBinder dataBinder = new DataBinder(myBean);
        MutablePropertyValues pvs = new MutablePropertyValues();
        pvs.add("a","10");
        pvs.add("b","hello");
        pvs.add("c","1999/03/04");
        dataBinder.bind(pvs);
        System.out.println(myBean);
    }

    @Data
    static class MyBean {
        private Integer a;
        private String b;
        private Date c;

        @Override
        public String toString() {
            return "MyBean{" +
                    "a=" + a +
                    ", b='" + b + '\'' +
                    ", c=" + c +
                    '}';
        }
    }
}
```

测试FieldAccessor

```java
public class TestFieldAccessor {
    public static void main(String[] args) {
        MyBean myBean = new MyBean();
        DirectFieldAccessor beanWrapper = new DirectFieldAccessor(myBean);
        beanWrapper.setPropertyValue("a","10");
        beanWrapper.setPropertyValue("b","hello");
        beanWrapper.setPropertyValue("c","1999/03/04");
        System.out.println(myBean);
    }

    static class MyBean {
        private Integer a;
        private String b;
        private Date c;

        @Override
        public String toString() {
            return "MyBean{" +
                    "a=" + a +
                    ", b='" + b + '\'' +
                    ", c=" + c +
                    '}';
        }
    }
}



```

##### Web自定义参数绑定

基于web请求的参数绑定

```java
public class TestDataBinder {

    public static void main(String[] args) {
        MyBean myBean = new MyBean();
        ServletRequestDataBinder dataBinder = new ServletRequestDataBinder(myBean);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("a", "12");
        request.setParameter("b", "西安");
        request.setParameter("c", "1990/01/01");
        dataBinder.bind(request);
        System.out.println(myBean);
    }

    @Data
    static class MyBean {
        private Integer a;
        private String b;
        private Date c;

        @Override
        public String toString() {
            return "MyBean{" +
                    "a=" + a +
                    ", b='" + b + '\'' +
                    ", c=" + c +
                    '}';
        }
    }
}
```

下面是关于web的自定义类型

```java
//控制器类
static class MyController {
    @InitBinder
    public void aaa(WebDataBinder binder){
        // 扩展 dataBinder 的转换器
        binder.addCustomFormatter(new MyDateFormatter("用 @InitBinder 方式扩展的"));
    }
}

@Data
public static class User {
    @DateTimeFormat(pattern = "yyyy|MM|dd")
    private Date birthday;
    private Address address;
}

@Data
public static class Address {
    private String name;
}
```

写一个转换器

```java
public class MyDateFormatter implements Formatter<Date> {
    private static final Logger log = LoggerFactory.getLogger(MyDateFormatter.class);
    private final String desc;

    public MyDateFormatter(String desc) {
        this.desc = desc;
    }

    @Override
    public String print(Date date, Locale locale) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy|MM|dd");
        return sdf.format(date);
    }

    @Override
    public Date parse(String text, Locale locale) throws ParseException {
        log.debug(">>>>>> 进入了: {}", desc);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy|MM|dd");
        return sdf.parse(text);
    }


}
```

```java
public class TestServletDataBinderFactory {
    public static void main(String[] args) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("birthday", "1999|01|02");
        request.setParameter("address.name", "西安");
        User user = new User();
        //1.使用dataBinder工厂创建dataBinder,无转换功能
        //ServletRequestDataBinderFactory factory = new ServletRequestDataBinderFactory(null, null);

        //2.用@InitBinder
        /*InvocableHandlerMethod method = new InvocableHandlerMethod(new MyController(),MyController.class.getMethod("aaa",WebDataBinder.class));
        ServletRequestDataBinderFactory factory = new ServletRequestDataBinderFactory(List.of(method), null);*/

        //3.用ConversionService转换
        /*FormattingConversionService service = new FormattingConversionService();
        service.addFormatter(new MyDateFormatter("用ConversionService方法扩展转换功能"));
        ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
        initializer.setConversionService(service);
        ServletRequestDataBinderFactory factory = new ServletRequestDataBinderFactory(null, initializer);*/

        //4.使用默认ConversionService
        DefaultFormattingConversionService defaultConversionService = new DefaultFormattingConversionService();
        ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
        initializer.setConversionService(defaultConversionService);
        ServletRequestDataBinderFactory factory = new ServletRequestDataBinderFactory(null, initializer);

        WebDataBinder binder = factory.createBinder(new ServletWebRequest(request), user, "user");
        binder.bind(new ServletRequestParameterPropertyValues(request));
        System.out.println(user);
    }
}
```

##### ControllerAdvice

@ExceptionHandler		  统一异常处理

@InitBinder						初始化参数绑定

@ModelAttribute			   处理模板数据

```java
@ControllerAdvice
public class MyControllerAdvice {
    @InitBinder
    public void binder3(WebDataBinder webDataBinder) {
        webDataBinder.addCustomFormatter(new MyDateFormatter("binder3 转换器"));
    }

    @ExceptionHandler
    public void Exception() {

    }
	/**
	* 返回值Modeal中就会有一个叫a的aa字符串
	**/
    @ModelAttribute("a")
    public String aa() {
        return "aa";
    }
    
}
```

### 返回值处理器

#### 不同的返回值处理

关于返回值处理器我们肯定能想到不通过返回值注解比如@ResponseBody@ModelAttribute还有返回值是String,ModelAndView,处理这些返回值有不同的返回值处理器

先提前写好User类和Controller

```java
static class Controller {
    public ModelAndView test1() {
        log.debug("test1()");
        ModelAndView mav = new ModelAndView("view1");
        mav.addObject("name", "张三");
        return mav;
    }

    public String test2() {
        log.debug("test2()");
        return "view2";
    }

    @ModelAttribute
    @RequestMapping("/test3")
    public User test3() {
        log.debug("test3()");
        return new User("李四", 20);
    }

    public User test4() {
        log.debug("test4()");
        return new User("王五", 30);
    }

    public HttpEntity<User> test5() {
        log.debug("test5()");
        return new HttpEntity<>(new User("赵六", 40));
    }

    public HttpHeaders test6() {
        log.debug("test6()");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/html");
        return headers;
    }

    @ResponseBody
    public User test7() {
        log.debug("test7()");
        return new User("钱七", 50);
    }
}

// 必须用 public 修饰, 否则 freemarker 渲染其 name, age 属性时失败
@Data
public static class User {
    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public User() {
    }
}
```

测试返回值为**ModelAndView**

```java
//返回值为ModelAndView
public static void text1(AnnotationConfigApplicationContext context) throws Exception {
    //获取test1
    Method method = Controller.class.getMethod("test1");
    Controller controller = new Controller();
    //获取返回值
    Object returnValue = method.invoke(controller);

    HandlerMethod handlerMethod = new HandlerMethod(controller, method);
    //获取组合返回值处理器
    HandlerMethodReturnValueHandlerComposite composite = getReturnValueHandler();
    ModelAndViewContainer container = new ModelAndViewContainer();
    //创建请求
    ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
    //判断谁能处理该返回值
    if (composite.supportsReturnType(handlerMethod.getReturnType())) {
        composite.handleReturnValue(returnValue, handlerMethod.getReturnType(), container, webRequest);
        System.out.println(container.getModel());
        System.out.println(container.getViewName());
        renderView(context, container, webRequest);
    }
}
```

结果

![image-20220716202908920](http://cdn.zhaodapiaoliang.top/PicGo/image-20220716202908920.png)

返回值为**String**

跟上面一样的流程

```java
//返回值为String
public static void text2(AnnotationConfigApplicationContext context) throws Exception {
    Method method = Controller.class.getMethod("test2");
    Controller controller = new Controller();
    //获取返回值
    Object returnValue = method.invoke(controller);

    HandlerMethod handlerMethod = new HandlerMethod(controller, method);
    HandlerMethodReturnValueHandlerComposite composite = getReturnValueHandler();
    ModelAndViewContainer container = new ModelAndViewContainer();
    ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
    if (composite.supportsReturnType(handlerMethod.getReturnType())) {
        composite.handleReturnValue(returnValue, handlerMethod.getReturnType(), container, webRequest);
        System.out.println(container.getModel());
        System.out.println(container.getViewName());
        renderView(context, container, webRequest);
    }
}
```

![image-20220716203134089](http://cdn.zhaodapiaoliang.top/PicGo/image-20220716203134089.png)

返回值为**@ModelAttribute**值得注意这个必须要添加请求的url

```java
public static void text3(AnnotationConfigApplicationContext context) throws Exception {
    Method method = Controller.class.getMethod("test3");
    Controller controller = new Controller();
    //获取返回值
    Object returnValue = method.invoke(controller);

    HandlerMethod handlerMethod = new HandlerMethod(controller, method);
    HandlerMethodReturnValueHandlerComposite composite = getReturnValueHandler();
    ModelAndViewContainer container = new ModelAndViewContainer();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/test3");
    UrlPathHelper.defaultInstance.resolveAndCacheLookupPath(request);
    ServletWebRequest webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

    if (composite.supportsReturnType(handlerMethod.getReturnType())) {
        composite.handleReturnValue(returnValue, handlerMethod.getReturnType(), container, webRequest);
        System.out.println(container.getModel());
        System.out.println(container.getViewName());
        renderView(context, container, webRequest);
    }
}
```

渲染的前端页面

```html
<!doctype html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <title>test3</title>
</head>
<body>
    <h1>Hello! ${user.name} ${user.age}</h1>
</body>
</html>
```

![image-20220716203504649](http://cdn.zhaodapiaoliang.top/PicGo/image-20220716203504649.png)

返回值省略**@ModelAttribute**

```java
public static void text4(AnnotationConfigApplicationContext context) throws Exception {
    Method method = Controller.class.getMethod("test4");
    Controller controller = new Controller();
    //获取返回值
    Object returnValue = method.invoke(controller);

    HandlerMethod handlerMethod = new HandlerMethod(controller, method);
    HandlerMethodReturnValueHandlerComposite composite = getReturnValueHandler();
    ModelAndViewContainer container = new ModelAndViewContainer();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/test4");
    UrlPathHelper.defaultInstance.resolveAndCacheLookupPath(request);
    ServletWebRequest webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

    if (composite.supportsReturnType(handlerMethod.getReturnType())) {
        composite.handleReturnValue(returnValue, handlerMethod.getReturnType(), container, webRequest);
        System.out.println(container.getModel());
        System.out.println(container.getViewName());
        renderView(context, container, webRequest);
    }
}
```

前端渲染模板

```html
<!doctype html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <title>test4</title>
</head>
<body>
    <h1>Hello! ${user.name} ${user.age}</h1>
</body>
</html>
```

![image-20220716203550026](http://cdn.zhaodapiaoliang.top/PicGo/image-20220716203550026.png)

返回值为**HttpEntity**

后3种返回值和前面的有所不一样这个不再需要进行渲染需要加一个这样的判断:!container.isRequestHandled()

```java
//返回值为HttpEntity
public static void text5(AnnotationConfigApplicationContext context) throws Exception {
    Method method = Controller.class.getMethod("test5");
    Controller controller = new Controller();
    //获取返回值
    Object returnValue = method.invoke(controller);

    HandlerMethod handlerMethod = new HandlerMethod(controller, method);
    HandlerMethodReturnValueHandlerComposite composite = getReturnValueHandler();
    ModelAndViewContainer container = new ModelAndViewContainer();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);

    if (composite.supportsReturnType(handlerMethod.getReturnType())) {
        composite.handleReturnValue(returnValue, handlerMethod.getReturnType(), container, webRequest);
        System.out.println(container.getModel());
        System.out.println(container.getViewName());
        if (!container.isRequestHandled()) {
            renderView(context, container, webRequest);
        } else {
            System.out.println(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
        }
    }
}
```

![image-20220716204027050](http://cdn.zhaodapiaoliang.top/PicGo/image-20220716204027050.png)

返回值为**HttpHeaders**

```java
//返回值为HttpHeaders
public static void text6(AnnotationConfigApplicationContext context) throws Exception {
    Method method = Controller.class.getMethod("test6");
    Controller controller = new Controller();
    //获取返回值
    Object returnValue = method.invoke(controller);

    HandlerMethod handlerMethod = new HandlerMethod(controller, method);
    HandlerMethodReturnValueHandlerComposite composite = getReturnValueHandler();
    ModelAndViewContainer container = new ModelAndViewContainer();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);
    if (composite.supportsReturnType(handlerMethod.getReturnType())) {
        composite.handleReturnValue(returnValue, handlerMethod.getReturnType(), container, webRequest);
        System.out.println(container.getModel());
        System.out.println(container.getViewName());
        if (!container.isRequestHandled()) {
            renderView(context, container, webRequest);
        } else {
            response.getHeaderNames().forEach(
                    data -> System.out.println(data + "=" + response.getHeader(data))
            );
        }

    }
}
```

![image-20220716203959275](http://cdn.zhaodapiaoliang.top/PicGo/image-20220716203959275.png)

返回值为**@ResponseBody**

```java
//返回值为@ResponseBody
public static void text7(AnnotationConfigApplicationContext context) throws Exception {
    Method method = Controller.class.getMethod("test7");
    Controller controller = new Controller();
    //获取返回值
    Object returnValue = method.invoke(controller);

    HandlerMethod handlerMethod = new HandlerMethod(controller, method);
    HandlerMethodReturnValueHandlerComposite composite = getReturnValueHandler();
    ModelAndViewContainer container = new ModelAndViewContainer();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);
    if (composite.supportsReturnType(handlerMethod.getReturnType())) {
        composite.handleReturnValue(returnValue, handlerMethod.getReturnType(), container, webRequest);
        System.out.println(container.getModel());
        System.out.println(container.getViewName());

        if (!container.isRequestHandled()) {
            renderView(context, container, webRequest);
        } else {
            response.getHeaderNames().forEach(
                    data -> System.out.println(data + "=" + response.getHeader(data))
            );
            System.out.println(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));

        }
    }
}
```

![image-20220716204119667](http://cdn.zhaodapiaoliang.top/PicGo/image-20220716204119667.png)

#### 返回值格式处理

首先可以指定返回值的类型主要有(json,xml)

```java
private static void test2() throws IOException {
    MockHttpOutputMessage message = new MockHttpOutputMessage();
    MappingJackson2XmlHttpMessageConverter converter = new MappingJackson2XmlHttpMessageConverter();
    if (converter.canWrite(User.class, MediaType.APPLICATION_XML)) {
        converter.write(new User("李四", 20), MediaType.APPLICATION_XML, message);
        System.out.println(message.getBodyAsString());
    }
}

public static void test1() throws IOException {
    MockHttpOutputMessage message = new MockHttpOutputMessage();
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    if (converter.canWrite(User.class, MediaType.APPLICATION_JSON)) {
        converter.write(new User("张三", 18), MediaType.APPLICATION_JSON, message);
        System.out.println(message.getBodyAsString());
    }
}
```

但是在实际应用中,服务器往往是根据客户端浏览器的请求头来自动的选择返回值类型

```java
    private static void test4() throws IOException, HttpMediaTypeNotAcceptableException, NoSuchMethodException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletWebRequest webRequest = new ServletWebRequest(request, response);
        //根据application和contentType来判断返回什么类型格式
//        request.addHeader("Accept", "application/xml");
        response.setContentType("application/json");

        RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
                List.of(
                        new MappingJackson2HttpMessageConverter(), new MappingJackson2XmlHttpMessageConverter()
                ));
        processor.handleReturnValue(
                new User("张三", 18),
                new MethodParameter(A25.class.getMethod("user"), -1),
                new ModelAndViewContainer(),
                webRequest
        );
        System.out.println(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
    }
```

#### 包装返回值

在日常的项目中往往需要添加一个别的参数进入返回值里面,比如需要

```json
{"name":"王五","age":18}
{"code":xx, "msg":xx, data: {"name":"王五","age":18} }
```

来创建一个返回会模板类

```java
public class Result {
    private int code;
    private String msg;
    private Object data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @JsonCreator
    private Result(@JsonProperty("code") int code, @JsonProperty("data") Object data) {
        this.code = code;
        this.data = data;
    }

    private Result(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static Result ok() {
        return new Result(200, null);
    }

    public static Result ok(Object data) {
        return new Result(200, data);
    }

    public static Result error(String msg) {
        return new Result(500, "服务器内部错误:" + msg);
    }
}
```

创建一个被@ControllerAdvice修饰的类使他继承ResponseBodyAdvice

```java
@ControllerAdvice
static class MyControllerAdvice implements ResponseBodyAdvice<Object> {
    // 满足条件才转换
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (returnType.getMethodAnnotation(ResponseBody.class) != null ||
            AnnotationUtils.findAnnotation(returnType.getContainingClass(), ResponseBody.class) != null) {
            return true;
        }
        return false;
    }

    // 将 User 或其它类型统一为 Result 类型
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result) {
            return body;
        }
        return Result.ok(body);
    }
}
```

请求测试

![image-20220716204935442](http://cdn.zhaodapiaoliang.top/PicGo/image-20220716204935442.png)

### 异常处理器

#### MVC中的异常解析器

在MVC中有一个**ExceptionHandlerExceptionResolver**,这样的异常处理器

**json格式**

```java
    static class Controller1 {
        public void foo() {

        }

        @ExceptionHandler
        @ResponseBody
        public Map<String, Object> handle(ArithmeticException e) {
            return Map.of("error", e.getMessage());
        }
    }

		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
        resolver.setMessageConverters(List.of(new MappingJackson2HttpMessageConverter()));
        resolver.afterPropertiesSet();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new Controller1(), Controller1.class.getMethod("foo"));
        Exception e = new ArithmeticException("被零除");
        //执行方法
        resolver.resolveException(request, response, handlerMethod, e);
        System.out.println(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
```

测试结果

```
{"error":"被零除"}
```

**ModelAndView**格式

```java
   static class Controller2 {
        public void foo() {

        }

        @ExceptionHandler
        public ModelAndView handle(ArithmeticException e) {
            return new ModelAndView("test2", Map.of("error", e.getMessage()));
        }
    }

		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
        resolver.setMessageConverters(List.of(new MappingJackson2HttpMessageConverter()));
        resolver.afterPropertiesSet();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new Controller2(), Controller2.class.getMethod("foo"));
        Exception e = new ArithmeticException("被零除");
        ModelAndView modelAndView = resolver.resolveException(request, response, handlerMethod, e);
        System.out.println(modelAndView.getModel());
        System.out.println(modelAndView.getViewName());
```

返回值

```
{error=被零除}
test2
```

**嵌套异常**

```java
    static class Controller3 {
        public void foo() {

        }

        @ExceptionHandler
        @ResponseBody
        public Map<String, Object> handle(IOException e3) {
            return Map.of("error", e3.getMessage());
        }
    }

		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
        resolver.setMessageConverters(List.of(new MappingJackson2HttpMessageConverter()));
        resolver.afterPropertiesSet();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new Controller3(), Controller3.class.getMethod("foo"));
        Exception e = new Exception("e1", new RuntimeException("e2", new IOException("e3")));
        resolver.resolveException(request, response, handlerMethod, e);
        System.out.println(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
```

结果

```
{"error":"e3"}
```

**测试异常处理方法的时候,仍然可以进行参数解析**

```java
        ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
        resolver.setMessageConverters(List.of(new MappingJackson2HttpMessageConverter()));
        resolver.afterPropertiesSet();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new Controller4(), Controller4.class.getMethod("foo"));
        Exception e = new Exception("e1");
        resolver.resolveException(request, response, handlerMethod, e);
        System.out.println(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
```

结果

```
org.springframework.mock.web.MockHttpServletRequest@4a7f959b
{"error":"e1"}
```

#### @ExceptionHandler

@ExceptionHandler注解我们一般是用来自定义异常的。 可以认为它是一个异常拦截器（处理器）。

添加一个配置类

```java
@Configuration
public class WebConfig {
    @ControllerAdvice
    static class MyControllerAdvice {
        @ExceptionHandler
        @ResponseBody
        public Map<String, Object> handle(Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @Bean
    public ExceptionHandlerExceptionResolver resolver() {
        ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
        resolver.setMessageConverters(List.of(new MappingJackson2HttpMessageConverter()));
        return resolver;
    }
}
```

向里面添加配置类和解析器

```java
public class A28 {
    public static void main(String[] args) throws NoSuchMethodException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(WebConfig.class);
        ExceptionHandlerExceptionResolver resolver = context.getBean(ExceptionHandlerExceptionResolver.class);

        HandlerMethod handlerMethod = new HandlerMethod(new Controller5(), Controller5.class.getMethod("foo"));
        Exception e = new Exception("e1");
        resolver.resolveException(request, response, handlerMethod, e);
        System.out.println(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    static class Controller5 {
        public void foo() {

        }
    }
}
```

结果

```
{"error":"e1"}
```

由此可以看出,可以在@ControllerAdvice添加@ExceptionHandler来对它进行统一的处理

#### SpringBoot的异常的处理

首先在配置文件中添加这些Bean,完成异常处理的组件注入

```java
@Bean
public TomcatServletWebServerFactory servletWebServerFactory() {
    return new TomcatServletWebServerFactory();
}

@Bean
public DispatcherServlet dispatcherServlet() {
    return new DispatcherServlet();
}

@Bean
public DispatcherServletRegistrationBean servletRegistrationBean(DispatcherServlet dispatcherServlet) {
    DispatcherServletRegistrationBean registrationBean = new DispatcherServletRegistrationBean(dispatcherServlet, "/");
    registrationBean.setLoadOnStartup(1);
    return registrationBean;
}

@Bean // @RequestMapping
public RequestMappingHandlerMapping requestMappingHandlerMapping() {
    return new RequestMappingHandlerMapping();
}

@Bean // 注意默认的 RequestMappingHandlerAdapter 不会带 jackson 转换器
public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
    RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();
    handlerAdapter.setMessageConverters(List.of(new MappingJackson2HttpMessageConverter()));
    return handlerAdapter;
}

@Bean // 修改了 Tomcat 服务器默认错误地址
public ErrorPageRegistrar errorPageRegistrar() { // 出现错误，会使用请求转发 forward 跳转到 error 地址
    return webServerFactory -> webServerFactory.addErrorPages(new ErrorPage("/error"));
}
@Controller
public static class MyController {
    @RequestMapping("test")
    public ModelAndView test() {
        int i = 1 / 0;
        return null;
    }
}
@Bean
public ViewResolver viewResolver() {
    return new BeanNameViewResolver();
}
```

在SpringBoot中有一个基础的处理异常的类BasicErrorController当请求

```java
@Bean
public BasicErrorController basicErrorController() {
    ErrorProperties errorProperties = new ErrorProperties();
    errorProperties.setIncludeException(true);
    return new BasicErrorController(new DefaultErrorAttributes(), errorProperties);
}
```

```java
@Bean
public View error() {
    return new View() {
        @Override
        public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
            System.out.println(model);
            response.setContentType("text/html;charset=utf-8");
            response.getWriter().print("""
                    <h3>服务器内部错误</h3>
                    """);
        }
    };
}
```

当使用浏览器访问时由于请求头返回结果

![image-20220718230137179](http://cdn.zhaodapiaoliang.top/PicGo/image-20220718230137179.png)

![image-20220718230159406](http://cdn.zhaodapiaoliang.top/PicGo/image-20220718230159406.png)

当直接请求的时候返回的是json

![image-20220718230247756](http://cdn.zhaodapiaoliang.top/PicGo/image-20220718230247756.png)

这就是**BasicErrorController**默认配置

### HandlerMapping与HandlerAdapter

前面我们说到了最基础的HandlerMapping与HandlerAdapte这一组映射器和适配器,

映射器负责解析@RequestMapping及其衍生注解,生成路径与控制器关系,在初始化时就生成

适配器有了之前的操作我们知道了该调用那个方法,那个路径,Adapter负责去调用，其最重要的方法invokeHandlerMethod因为是一个protected方法所以我们在我们的包下建立一个他的子类，重写修饰符。

下面要介绍不同的映射器

#### 基于名字的规则映射

BeanNameUrlHandlerMapping和SimpleControllerHandlerAdapter

```java
@Configuration
public class WebConfig {
    @Bean // ⬅️内嵌 web 容器工厂
    public TomcatServletWebServerFactory servletWebServerFactory() {
        return new TomcatServletWebServerFactory(8080);
    }

    @Bean // ⬅️创建 DispatcherServlet
    public DispatcherServlet dispatcherServlet() {
        return new DispatcherServlet();
    }

    @Bean // ⬅️注册 DispatcherServlet, Spring MVC 的入口
    public DispatcherServletRegistrationBean servletRegistrationBean(DispatcherServlet dispatcherServlet) {
        return new DispatcherServletRegistrationBean(dispatcherServlet, "/");
    }

    // /c1  -->  /c1
    // /c2  -->  /c2
    @Bean
    public BeanNameUrlHandlerMapping beanNameUrlHandlerMapping() {
        return new BeanNameUrlHandlerMapping();
    }

    @Bean
    public SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
        return new SimpleControllerHandlerAdapter();
    }

    @Component("/c1")
    public static class Controller1 implements Controller {
        @Override
        public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
            response.getWriter().print("this is c1");
            return null;
        }
    }

    @Component("/c2")
    public static class Controller2 implements Controller {
        @Override
        public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
            response.getWriter().print("this is c2");
            return null;
        }
    }

    @Bean("/c3")
    public Controller controller3() {
        return (request, response) -> {
            response.getWriter().print("this is c3");
            return null;
        };
    }
}
```

#### 函数式控制器

RouterFunctionMapping和HandlerFunctionAdapter(平时用的不多)

```java
@Configuration
public class WebConfig {
    @Bean // ⬅️内嵌 web 容器工厂
    public TomcatServletWebServerFactory servletWebServerFactory() {
        return new TomcatServletWebServerFactory(8080);
    }

    @Bean // ⬅️创建 DispatcherServlet
    public DispatcherServlet dispatcherServlet() {
        return new DispatcherServlet();
    }

    @Bean // ⬅️注册 DispatcherServlet, Spring MVC 的入口
    public DispatcherServletRegistrationBean servletRegistrationBean(DispatcherServlet dispatcherServlet) {
        return new DispatcherServletRegistrationBean(dispatcherServlet, "/");
    }

    @Bean
    public RouterFunctionMapping routerFunctionMapping() {
        return new RouterFunctionMapping();
    }

    @Bean
    public HandlerFunctionAdapter handlerFunctionAdapter() {
        return new HandlerFunctionAdapter();
    }

    @Bean
    public RouterFunction<ServerResponse> r1() {
        return route(GET("/r1"), request -> ok().body("this is r1"));
    }

    @Bean
    public RouterFunction<ServerResponse> r2() {
        return route(GET("/r2"), request -> ok().body("this is r2"));
    }

}
```

#### 静态资源映射

SimpleUrlHandlerMapping与HttpRequestHandlerAdapter

```java
@Configuration
public class WebConfig {
    @Bean // ⬅️内嵌 web 容器工厂
    public TomcatServletWebServerFactory servletWebServerFactory() {
        return new TomcatServletWebServerFactory(8080);
    }

    @Bean // ⬅️创建 DispatcherServlet
    public DispatcherServlet dispatcherServlet() {
        return new DispatcherServlet();
    }

    @Bean // ⬅️注册 DispatcherServlet, Spring MVC 的入口
    public DispatcherServletRegistrationBean servletRegistrationBean(DispatcherServlet dispatcherServlet) {
        return new DispatcherServletRegistrationBean(dispatcherServlet, "/");
    }

    @Bean
    public SimpleUrlHandlerMapping simpleUrlHandlerMapping(ApplicationContext context) {
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        Map<String, ResourceHttpRequestHandler> map = context.getBeansOfType(ResourceHttpRequestHandler.class);
        handlerMapping.setUrlMap(map);
        System.out.println(map);
        return handlerMapping;
    }

    @Bean
    public HttpRequestHandlerAdapter httpRequestHandlerAdapter() {
        return new HttpRequestHandlerAdapter();
    }

    /*
        /index.html
        /r1.html
        /r2.html

        /**
     */

    @Bean("/**")
    public ResourceHttpRequestHandler handler1() {
        ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
        handler.setLocations(List.of(new ClassPathResource("static/")));
        handler.setResourceResolvers(List.of(
                //缓存
                new CachingResourceResolver(new ConcurrentMapCache("cache1")),
                //处理压缩文件
                new EncodedResourceResolver(),
                //基础解析器
                new PathResourceResolver()
        ));
        return handler;
    }

    /*
        /img/1.jpg
        /img/2.jpg
        /img/3.jpg

        /img/**
     */

    @Bean("/img/**")
    public ResourceHttpRequestHandler handler2() {
        ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
        handler.setLocations(List.of(new ClassPathResource("images/")));
        return handler;
    }

    /*@Bean
    public WelcomePageHandlerMapping welcomePageHandlerMapping(ApplicationContext context) {
        Resource resource = context.getResource("classpath:static/index.html");
        return new WelcomePageHandlerMapping(null, context, resource, "/**");
        // Controller 接口
    }*/

    @Bean
    public SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
        return new SimpleControllerHandlerAdapter();
    }

    //压缩
    @PostConstruct
    @SuppressWarnings("all")
    public void initGzip() throws IOException {
        Resource resource = new ClassPathResource("static");
        File dir = resource.getFile();
        for (File file : dir.listFiles(pathname -> pathname.getName().endsWith(".html"))) {
            System.out.println(file);
            try (FileInputStream fis = new FileInputStream(file); GZIPOutputStream fos = new GZIPOutputStream(new FileOutputStream(file.getAbsoluteFile() + ".gz"))) {
                byte[] bytes = new byte[8 * 1024];
                int len;
                while ((len = fis.read(bytes)) != -1) {
                    fos.write(bytes, 0, len);
                }
            }
        }
    }
}
```

#### 欢迎页处理

a. WelcomePageHandlerMapping, 映射欢迎页(即只映射 '/')
    - 它内置了 handler ParameterizableViewController 作用是不执行逻辑, 仅根据视图名找视图
    - 视图名固定为 forward:index.html       /**
b. SimpleControllerHandlerAdapter, 调用 handler
    - 转发至 /index.html
    - 处理 /index.html 又会走上面的静态资源处理流程

### 小结

​	**a. HandlerMapping 负责建立请求与控制器之间的映射关系**

   - RequestMappingHandlerMapping (与 @RequestMapping 匹配)

   - WelcomePageHandlerMapping    (/)

   - BeanNameUrlHandlerMapping    (与 bean 的名字匹配 以 / 开头)

   - RouterFunctionMapping        (函数式 RequestPredicate, HandlerFunction)

   - SimpleUrlHandlerMapping      (静态资源 通配符 /** /img/**)
     之间也会有顺序问题, boot 中默认顺序如上

     **b. HandlerAdapter 负责实现对各种各样的 handler 的适配调用**

   - RequestMappingHandlerAdapter 处理：@RequestMapping 方法
     参数解析器、返回值处理器体现了组合模式

   - SimpleControllerHandlerAdapter 处理：Controller 接口

   - HandlerFunctionAdapter 处理：HandlerFunction 函数式接口

   - HttpRequestHandlerAdapter 处理：HttpRequestHandler 接口 (静态资源处理)
     这也是典型适配器模式体现
     **c. ResourceHttpRequestHandler.setResourceResolvers 这是典型责任链模式体现**

### MVC处理请求流程

1.请求发送到DispatcherServlet,boot初始化的时候向里面创建了多组HandlerMapping和HandlerAdapter

2.请求会到HandlerMapping找到对应的控制器方法

3.调用拦截器的 preHandle 方法

4.HandlerAdapter调用handle方法准备数据绑定工厂、模型工厂、ModelAndViewContainer、将 HandlerMethod 完善为 (在这个过程中@ControllerAdvice会对控制器进行增强)

* @ControllerAdvice 全局增强点1：补充模型数据
* @ControllerAdvice 全局增强点2：补充自定义类型转换器
* @ControllerAdvice 全局增强点3：RequestBody 增强
* @ControllerAdvice 全局增强点4：ResponseBody 增强

5.调用拦截器的 postHandle 方法

6.处理异常或视图渲染

* @ControllerAdvice 全局增强点5：@ExceptionHandler 异常处理

7.调用拦截器的 afterCompletion 方法