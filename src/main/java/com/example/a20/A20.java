package com.example.a20;

import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.charset.StandardCharsets;
import java.util.Map;

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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test4");
        request.addHeader("token", "令牌");
        MockHttpServletResponse response = new MockHttpServletResponse();

        //请求来了,获取控制器方法,返回一个处理器链对象
        HandlerExecutionChain chain = handlerMapping.getHandler(request);
        System.out.println(chain);

        //请求来了,MyRequestMappingHandlerAdapter,会根据request和HandlerExecutionChain来调用方法
        MyRequestMappingHandlerAdapter HandlerAdapter = context.getBean(MyRequestMappingHandlerAdapter.class);
        HandlerAdapter.invokeHandlerMethod(request, response, (HandlerMethod) chain.getHandler());

        //检查响应
        byte[] content = response.getContentAsByteArray();
        System.out.println(new String(content, StandardCharsets.UTF_8));
        /*System.out.println(">>>>>>>>>>>>> 所以参数解析器");
        for (HandlerMethodArgumentResolver resolver : HandlerAdapter.getArgumentResolvers()) {
            System.out.println(resolver);
        }

        System.out.println(">>>>>>>>>>>>> 所以返回值解析器");
        HandlerAdapter.getReturnValueHandlers().forEach(data -> {
            System.out.println(data);
        });*/

    }
}
