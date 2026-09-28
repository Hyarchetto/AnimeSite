package com.liuyuxiang.animeserver.dto;

/**
 * POST /api/auth/register 的请求体
 *
 * <p>故意没有 role 字段。注册只产出普通用户，与其拿到 role 再判断要不要忽略，
 * 不如让它根本传不进来——这是最省心的写法，也堵死了越权注册管理员的路
 */
public class RegisterRequest {

    private String account;

    private String password;

    private String nickname;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
