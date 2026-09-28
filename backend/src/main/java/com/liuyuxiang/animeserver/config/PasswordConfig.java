package com.liuyuxiang.animeserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码哈希器
 *
 * <p>这里只有 spring-security-crypto 这一个包，没有引 Spring Security 全套，
 * 所以不会自动配出安全过滤器链
 *
 * <p>BCrypt 每次加密都会随机盐，同一个密码两次加密结果不同，比对只能靠 matches
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
