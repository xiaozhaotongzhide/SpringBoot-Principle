package com.example.a22;

import lombok.Data;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.ServletRequestDataBinder;

import java.util.Date;

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
