package com.example.a22;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.ServletRequestParameterPropertyValues;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory;

import java.util.Date;

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
}
