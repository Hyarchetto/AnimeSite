package com.liuyuxiang.animeserver.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务层抛出的异常，携带要回给客户端的 HTTP 状态码和提示文案
 *
 * <p>业务代码只管抛出，比如账号重复就抛 409，由 GlobalExceptionHandler 统一转成响应。
 * 这样 Controller 里不会到处是 if-else 拼响应，提示文案也和判断逻辑挨在一起
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
