package com.sajia23.bilidown.configuration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice(basePackages="com.sajia23.bilidown")  //SpringBoot的异常切入点。此包下的包揽了哈
public class GlobalExceptionCatch {

    @ResponseBody  //返回jsoon给客户端
    public Map<String, Object> exceptionHandler(){
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("errorCode", "500");
        map.put("errorMsg", "出现错误查看后台");
        return map;
    }

}