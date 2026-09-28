package com.liuyuxiang.animeserver.exception;

import com.liuyuxiang.animeserver.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 把异常统一转成 HTTP 响应
 *
 * <p>拦截器里抛的 ApiException 也会走到这里。DispatcherServlet 对 preHandle 抛出的异常
 * 走的是和 Controller 相同的异常解析链，所以不需要在拦截器里手写 JSON
 *
 * <p>继承 ResponseEntityExceptionHandler 是为了把 404、405、请求体解析失败这些
 * 框架异常的**状态码**留给 Spring 决定。不继承而是直接 @ExceptionHandler(Exception.class)
 * 兜底的话，这些异常会被一并吞掉变成 500，整套标准错误语义就没了
 *
 * <p>这里只覆写最终组装响应的那一步，把响应体换成和 ApiException 一样的形状，
 * 前端拿到错误只需要读 message 一个字段
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
                                                             HttpHeaders headers, HttpStatusCode status,
                                                             WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("请求处理失败", ex);
        }
        return new ResponseEntity<>(new ErrorResponse(describe(status.value())), headers, status);
    }

    private String describe(int status) {
        return switch (status) {
            case 400 -> "请求格式不正确";
            case 404 -> "接口不存在";
            case 405 -> "请求方法不支持";
            case 413 -> "文件太大";
            case 415 -> "不支持的内容类型";
            default  -> "请求无法处理";
        };
    }

    /**
     * 兜底。真正没预料到的异常才走到这里
     *
     * <p>不接这一层的话，堆栈会直接回给客户端，等于把内部结构暴露出去。
     * 堆栈仍然打到日志，不然本地调试时看不到真正的原因
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("未处理的异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("服务器内部错误"));
    }
}
