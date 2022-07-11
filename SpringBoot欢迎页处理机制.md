### 默认欢迎页

springboot在启动时会加载一系列的bean，其中WebMvcAutoConfiguration中的EnableWebMvcConfiguration配置类中会注册一个WelcomePageHandlerMapping，该bean的主要作用就是设置欢迎页。

通过源码来看一下具体的实现过程
```java
@Bean
public WelcomePageHandlerMapping welcomePageHandlerMapping(ApplicationContext applicationContext,
				FormattingConversionService mvcConversionService, 
									ResourceUrlProvider mvcResourceUrlProvider) {
	WelcomePageHandlerMapping welcomePageHandlerMapping = new WelcomePageHandlerMapping(
			new TemplateAvailabilityProviders(applicationContext),
				applicationContext,
				getWelcomePage(),
				this.mvcProperties.getStaticPathPattern());
	//注册Interceptors
	welcomePageHandlerMapping.setInterceptors(
				getInterceptors(mvcConversionService, mvcResourceUrlProvider));
	//配置跨域请求处理
	welcomePageHandlerMapping.setCorsConfigurations(getCorsConfigurations());
	return welcomePageHandlerMapping;
}

//WebMvcConfigurationSupport中的一个类
protected final Object[] getInterceptors(
			FormattingConversionService mvcConversionService,
			ResourceUrlProvider mvcResourceUrlProvider) {
	if (this.interceptors == null) {
		InterceptorRegistry registry = new InterceptorRegistry();
		addInterceptors(registry);
		

	//用来注册springmvc的类型转换服务，该类型转换服务会在请求处理过程中用于
	//请求参数或者返回值的类型转换。
	registry.addInterceptor(new ConversionServiceExposingInterceptor(mvcConversionService));
	
	//Spring MVC配置定义的一个资源URL提供者对象ResourceUrlProvider
	registry.addInterceptor(new ResourceUrlProviderExposingInterceptor(mvcResourceUrlProvider));
	this.interceptors = registry.getInterceptors();
}
return this.interceptors.toArray();
```
从上述代码中可以看到，当注册WelcomePageHandlerMapping为bean时,会用构造器初始化，初始化过程如下：

```java
WelcomePageHandlerMapping(
		TemplateAvailabilityProviders templateAvailabilityProviders,
		ApplicationContext applicationContext, 
		Optional<Resource> welcomePage, 
		String staticPathPattern) {
		//判断是否存在欢迎页，如果存在且设置的静态资源映射url为/**，则欢迎页为index.html
		//当请求到来时如果没有任何控制器处理，则根据此配置进行跳转-也就是跳转到index.html
		if (welcomePage.isPresent() && "/**".equals(staticPathPattern)) {
			logger.info("Adding welcome page: " + welcomePage.get());
			setRootViewName("forward:index.html");
		}
		else if (welcomeTemplateExists(templateAvailabilityProviders, applicationContext)) {
			logger.info("Adding welcome page template: index");
			setRootViewName("index");
		}
	}
```


从上述代码可以看到，设置的默认欢迎页面为index.html；如果请求没有任何控制器进行处理，则会跳转到默认的index.html页面。
这也是为什么，直接输入项目地址可以进行跳转的原因：例如localhost:8080/，则会直接跳转到index.html.

### 自行设置欢迎页

通过继承WebMvcConfigurer重写其addViewControllers方法,添加默认欢迎页

```java
@Configuration
public class MyMvcConfig implements WebMvcConfigurer {
/**
 * 默认首页设置，当请求时项目地址的时候 返回login
 */
@Override
public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("login");
    registry.addViewController("/index.html").setViewName("login");
    registry.addViewController("/main.html").setViewName("dashboard");
    super.addViewControllers(registry);
}
```

如代码所示，当访问项目地址时localhost:8080/会跳转到login页面

和springboot设置默认欢迎页原理一样，通过addViewControllers()方法添加的控制器最终都需要通过设置viewName进行跳转：

```java
springboot默认：setRootViewName()
addViewController手动配置:setViewName()

//ViewControllerRegistration 中记录着所有设置的view
public ViewControllerRegistration addViewController(String urlPath) {
		ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);
		registration.setApplicationContext(this.applicationContext);
		this.registrations.add(registration);
		return registration;
	}
//setViewName()方法往ViewControllerRegistration.ParameterizableViewController
//ParameterizableViewController就是根据view名字，返回view
public void ViewControllerRegistration$setViewName(String viewName) {
		this.controller.setViewName(viewName);
	}
```

TIPS：在pom文件中务必将前端文件和index.html配置到classes文件夹中，如果未配置，则会出现访问失败，出现404的现象；

### 适配自己设定的静态资源

我们会发现在spring中他的index.html的路径是写死的只能是他的配置文件的4个路径但是我们自己设置的静态资源路径就不可能，于是我们试试修改一下源码

修改springboot中的

```java

WelcomePageHandlerMapping(
		TemplateAvailabilityProviders templateAvailabilityProviders,
		ApplicationContext applicationContext, 
		Optional<Resource> welcomePage, 
		String staticPathPattern) {
		//判断是否存在欢迎页，如果存在且设置的静态资源映射url为/**，则欢迎页为index.html
		//当请求到来时如果没有任何控制器处理，则根据此配置进行跳转-也就是跳转到index.html
		if (welcomePage.isPresent() && "/**".equals(staticPathPattern)) {
			logger.info("Adding welcome page: " + welcomePage.get());
			setRootViewName("forward:index.html");
		}
		else if (welcomeTemplateExists(templateAvailabilityProviders, applicationContext)) {
			logger.info("Adding welcome page template: index");
			setRootViewName("index");
		}
	}
```

我暂时的思路是给这个类加yml配置和spring:  mvc:   static-path-pattern:和他的值一样了，然和用一个判断如果有这个值那么就从这里面找，这样就可以

@Conditional 使用他进行条件注入