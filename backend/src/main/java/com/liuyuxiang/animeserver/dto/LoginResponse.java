package com.liuyuxiang.animeserver.dto;

/**
 * 注册和登录共用的响应
 *
 * <p>直接带上 token，注册完不用再登一次。返回的是同一个形状，
 * 前端两条路径可以共用一段处理逻辑
 */
public class LoginResponse {

    private final String token;

    private final UserInfo user;

    public LoginResponse(String token, UserInfo user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public UserInfo getUser() {
        return user;
    }
}
