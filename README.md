# SpringBoot原理解析

### Springboot的启动过程

1、new了一个SpringApplication对象，使用SPI技术加载加载 ApplicationContextInitializer、ApplicationListener 接口实例

2、调用SpringApplication.run() 方法

3、调用createApplicationContext()方法创建上下文对象，创建上下文对象同时会注册spring的核心组件类（ConfigurationClassPostProcessor 、AutowiredAnnotationBeanPostProcessor 等）。

4、调用refreshContext() 方法启动Spring容器和内置的Servlet容器,(注册bean就是在这一步完成的)

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