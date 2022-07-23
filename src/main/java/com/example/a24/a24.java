package com.example.a24;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.method.annotation.*;
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.util.UrlPathHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.List;
import java.util.Locale;

@Slf4j
public class a24 {
    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(WebConfig.class);

        // 1. 测试返回值类型为 ModelAndView

        // 2. 测试返回值类型为 String 时, 把它当做视图名

        // 3. 测试返回值添加了 @ModelAttribute 注解时, 此时需找到默认视图名

        // 4. 测试返回值不加 @ModelAttribute 注解且返回非简单类型时, 此时需找到默认视图名

        // 5. 测试返回值类型为 ResponseEntity 时, 此时不走视图流程

        // 6. 测试返回值类型为 HttpHeaders 时, 此时不走视图流程

        // 7. 测试返回值添加了 @ResponseBody 注解时, 此时不走视图流程
        text7(context);
        /*
            学到了什么
                a. 每个返回值处理器能干啥
                    1) 看是否支持某种返回值
                    2) 返回值或作为模型、或作为视图名、或作为响应体 ...
                b. 组合模式在 Spring 中的体现 + 1
         */
    }
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

    //省略@ModelAttribute
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

    //有@ModelAttribute
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

    //返回值为ModelAndView
    public static void text1(AnnotationConfigApplicationContext context) throws Exception {
        Method method = Controller.class.getMethod("test1");
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

    //添加参数解析器,用组合设计模式
    public static HandlerMethodReturnValueHandlerComposite getReturnValueHandler() {
        HandlerMethodReturnValueHandlerComposite composite = new HandlerMethodReturnValueHandlerComposite();
        //处理ModelAndView
        composite.addHandler(new ModelAndViewMethodReturnValueHandler());
        composite.addHandler(new ViewNameMethodReturnValueHandler());
        composite.addHandler(new ServletModelAttributeMethodProcessor(false));
        composite.addHandler(new HttpEntityMethodProcessor(List.of(new MappingJackson2HttpMessageConverter())));
        composite.addHandler(new HttpHeadersReturnValueHandler());
        composite.addHandler(new RequestResponseBodyMethodProcessor(List.of(new MappingJackson2HttpMessageConverter())));
        composite.addHandler(new ServletModelAttributeMethodProcessor(true));
        return composite;
    }

    @SuppressWarnings("all")
    private static void renderView(AnnotationConfigApplicationContext context, ModelAndViewContainer container,
                                   ServletWebRequest webRequest) throws Exception {
        log.debug(">>>>>> 渲染视图");
        FreeMarkerViewResolver resolver = context.getBean(FreeMarkerViewResolver.class);
        String viewName = container.getViewName() != null ? container.getViewName() : new DefaultRequestToViewNameTranslator().getViewName(webRequest.getRequest());
        log.debug("没有获取到视图名, 采用默认视图名: {}", viewName);
        // 每次渲染时, 会产生新的视图对象, 它并非被 Spring 所管理, 但确实借助了 Spring 容器来执行初始化
        View view = resolver.resolveViewName(viewName, Locale.getDefault());
        view.render(container.getModel(), webRequest.getRequest(), webRequest.getResponse());
        System.out.println(new String(((MockHttpServletResponse) webRequest.getResponse()).getContentAsByteArray(), StandardCharsets.UTF_8));
    }

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
}
