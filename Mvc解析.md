## DispatcherServlet-MVC的核心

SpringMVC的核心就是DispatcherServlet，DispatcherServlet实质也是一个HttpServlet。DispatcherSevlet负责将请求分发，所有的请求都有经过它来统一分发。

### DispatcherServlet快速入门

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

### DispatcherServlet构造方法

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

#### RequestMappingHandlerMapping

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

#### RequestMappingHandlerAdapter

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

#### HandlerMethodArgumentResolver

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

#### HandlerMethodReturnValueHandler

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
## 参数解析器

MVC最重要的特征之一就是自动参数解析

### 不同的参数解析器

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

### 组合模式

如上面所说的每一个参数解析器都有supportsParameter方法和resolveArgument方法,只有支持这个参数才会解析。我们不能遍历每一个参数解析器于是这里用到了组合模式

组合模式（Composite Pattern），又叫部分整体模式，是用于把一组相似的对象当作一个单一的对象。组合模式依据树形结构来组合对象，用来表示部分以及整体层次。这种类型的设计模式属于结构型模式，它创建了对象组的树形结构。

这种模式创建了一个包含自己对象组的类。该类提供了修改相同对象组的方式。

### 数据绑定和类型转换

#### 类型转换接口

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

#### 高层接口实战

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

#### Web自定义参数绑定

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

#### ControllerAdvice

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

## 返回值处理器

### 不同的返回值处理

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

### 返回值格式处理

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

### 包装返回值

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

## 异常处理器

### MVC中的异常解析器

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

### @ExceptionHandler

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

### SpringBoot的异常的处理

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

## HandlerMapping与HandlerAdapter

前面我们说到了最基础的HandlerMapping与HandlerAdapte这一组映射器和适配器,

映射器负责解析@RequestMapping及其衍生注解,生成路径与控制器关系,在初始化时就生成

适配器有了之前的操作我们知道了该调用那个方法,那个路径,Adapter负责去调用，其最重要的方法invokeHandlerMethod因为是一个protected方法所以我们在我们的包下建立一个他的子类，重写修饰符。

下面要介绍不同的映射器

### 基于名字的规则映射

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

### 函数式控制器

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

### 静态资源映射

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

### 欢迎页处理

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

## MVC处理请求流程

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