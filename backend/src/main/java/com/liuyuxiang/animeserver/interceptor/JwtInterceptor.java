package com.liuyuxiang.animeserver.interceptor;

import com.liuyuxiang.animeserver.entity.User;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.UserMapper;
import com.liuyuxiang.animeserver.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 校验登录状态
 *
 * <p>解析 Authorization 头里的 token 拿到 userId，再按 userId 查一次库。
 * 这一次查询不能省：token 里只有 userId，角色和启用状态都在库里，
 * 每次现查才能让禁用和降级立刻生效，而不是等 7 天有效期过去
 *
 * <p>通过后把 userId 和 role 放进 request attribute 供后续使用。
 * Controller 用 @RequestAttribute 取，AdminInterceptor 直接读
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public JwtInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检必须放行，两个理由
        // 一是浏览器的预检按定义不携带 Authorization 头，要求认证就没法通过
        // 二是 Spring 给预检请求保留了这个拦截器但把 handler 换成了 PreFlightHandler，
        //    它不是 Controller 方法，GlobalExceptionHandler 接不住这里抛的异常，
        //    结果会是 500 加一个空响应体，浏览器拿不到 CORS 头，所有跨域请求全挂
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "请先登录");
        }

        Long userId = jwtUtil.findUserId(header.substring(BEARER_PREFIX.length()).trim());
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号不存在");
        }
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号已被禁用");
        }

        request.setAttribute("userId", userId);
        request.setAttribute("role", user.getRole());
        request.setAttribute("owner", user.isOwner());
        return true;
    }
}
