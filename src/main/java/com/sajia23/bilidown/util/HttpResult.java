package com.sajia23.bilidown.util;

import lombok.Data;

@Data
public class HttpResult<T> {
    private Boolean success;
    private int code;
    private String message;
    private T data;

    public HttpResult(boolean success, int code){
        this.setSuccess(success);
        this.setCode(code);
    }

    public HttpResult(boolean success, int code, String message){
        this.setSuccess(success);
        this.setCode(code);
        this.setMessage(message);
    }

    public HttpResult(boolean success, int code, T data){
        this.setSuccess(success);
        this.setCode(code);
        this.setData(data);
    }

    public static <T> HttpResult<T> success(T data){
        return new HttpResult<T>(true,200,data);
    }
    public static <T> HttpResult<T> success(String message){
        return new HttpResult<T>(true,200, message);
    }

    public static <T> HttpResult<T> fail(int code, T data){
        return new HttpResult<T>(false,code,data);
    }

    public static <T> HttpResult<T> fail(int code, String message){
        return new HttpResult<T>(false,code,message);
    }

}
