package com.liuyuxiang.animeserver.dto;

/**
 * 出错时的响应体
 *
 * <p>和成功响应不是一套形状。成功的列表接口返回裸数组，这里是一个对象，
 * 因为错误必须带上文案，而数组表达不了
 */
public class ErrorResponse {

    private final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
