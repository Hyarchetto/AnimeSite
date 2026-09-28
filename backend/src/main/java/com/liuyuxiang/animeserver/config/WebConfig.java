package com.liuyuxiang.animeserver.config;

import com.liuyuxiang.animeserver.interceptor.AdminInterceptor;
import com.liuyuxiang.animeserver.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 层配置
 *
 * <p>只放行本地开发时前端的两个来源。前端跑在 Vite 的 5173 端口上，没有配代理，
 * 请求是跨域直连到 3001 的，所以必须开 CORS
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 需要登录的路径。显式列出而不是拦住全部再排除白名单
     *
     * <p>反过来的写法在新增公开接口时很容易忘掉排除，而这类错误本地测不出来——
     * 本地调试时通常是登录状态
     *
     * <p>**代价是新增受保护接口时必须回到这里加一行。** 漏了不会报错，
     * 那个接口会静默变成公开的。加完记得不带 token 打一次，确认返回 401
     */
    private static final String[] AUTHENTICATED_PATHS = {
            "/api/auth/me",
            "/api/auth/profile",
            "/api/auth/password",
            "/api/auth/avatar",
            "/api/auth/account",
            "/api/watchlist/**",
            "/api/history/**",
            "/api/comments/**",
            "/api/admin/**"
    };

    private static final String[] ADMIN_PATHS = {
            "/api/admin/**"
    };

    private final JwtInterceptor jwtInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final String uploadLocation;

    public WebConfig(JwtInterceptor jwtInterceptor, AdminInterceptor adminInterceptor,
                     @Value("${app.upload.dir}") String uploadDir) {
        this.jwtInterceptor = jwtInterceptor;
        this.adminInterceptor = adminInterceptor;
        this.uploadLocation = toDirectoryUri(uploadDir);
    }

    /**
     * 把上传目录转成 Spring 认识的资源位置
     *
     * <p>toUri 而不是字符串拼 file: 前缀，是因为项目路径里有空格，
     * 拼出来的地址不转义会解析失败。toUri 会把它变成 %20
     *
     * <p>结尾必须有斜杠，否则 Spring 会把最后一段当成文件名前缀而不是目录
     */
    private static String toDirectoryUri(String dir) {
        String uri = Paths.get(dir).toAbsolutePath().toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }

    /**
     * 必须写 allowedMethods 和 allowedHeaders
     *
     * <p>不写就用默认值，只放行 GET POST 和几个简单头。登录要发 JSON 的 POST，
     * Content-Type: application/json 不在 CORS 安全列表里；认证要带 Authorization 头，
     * 两者都会触发 OPTIONS 预检，而预检会挂。浏览器只报一个笼统的 CORS 错误，看不出原因
     *
     * <p>不开 allowCredentials。那是给 Cookie 用的，JWT 走 Authorization 头，
     * 开了反而要求 allowedOrigins 不能是通配符
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /**
     * 注册顺序就是执行顺序
     *
     * <p>AdminInterceptor 读的 role 是 JwtInterceptor 放进 request attribute 的，
     * 顺序反了它拿到的一直是 null，结果所有管理员请求都 403
     */
    /**
     * 让上传的图片能被浏览器取到
     *
     * <p>这是本项目第一次由后端托管静态资源。预置图片仍然在 frontend/public 下
     * 由 Vite 提供，两套并存。库里的路径都是 /uploads/xxx.jpg 这种相对形式，
     * 前端用 resolveImageUrl 判断要不要补上后端的源
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor).addPathPatterns(AUTHENTICATED_PATHS);
        registry.addInterceptor(adminInterceptor).addPathPatterns(ADMIN_PATHS);
    }
}
