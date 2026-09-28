package com.liuyuxiang.animeserver.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 的签发与解析
 *
 * <p>payload 里只放 userId。不放 role，也不放 is_enabled——
 * token 签出去就收不回来，有效期 7 天意味着权限变更最多要 7 天才生效。
 * 角色和启用状态每次由拦截器查库得到
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final Duration expire;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire-days}") long expireDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = Duration.ofDays(expireDays);
    }

    public String issue(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire.toMillis()))
                .signWith(key)
                .compact();
    }

    /**
     * 取出 token 里的 userId，签名不对或已过期都返回 null
     *
     * <p>签名不对、格式不对、过期这三种情况对调用方是同一件事——这张票不能用了，
     * 所以不区分，一律返回 null 让上层回 401
     */
    public Long findUserId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.valueOf(subject);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}
