package com.liuyuxiang.animeserver.interceptor;

import com.liuyuxiang.animeserver.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 校验管理员身份
 *
 * <p>挂在 /api/admin/** 上，必须排在 JwtInterceptor 后面——role 是它放进 request
 * attribute 的，没有前者就没有后者
 *
 * <p>不在这里重新查库，直接读 attribute。JwtInterceptor 已经查过一次，
 * 同一次请求里再查一遍是白花的
 *
 * <p>和 JwtInterceptor 拆成两个而不是合成一个，是因为职责不同：
 * 一个管有没有登录，一个管够不够权限。以后加审核员这类中间角色只需要换掉这一个
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private static final String ROLE_ADMIN = "ADMIN";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 和 JwtInterceptor 一样要放行预检。它放行之后不会放 role 进来，
        // 这里不跟着放行的话，管理员路径的预检会拿 role == null 判成 403
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        if (!ROLE_ADMIN.equals(request.getAttribute("role"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "需要管理员权限");
        }
        return true;
    }
}
