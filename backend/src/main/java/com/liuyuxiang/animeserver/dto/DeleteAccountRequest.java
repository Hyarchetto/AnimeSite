package com.liuyuxiang.animeserver.dto;

/**
 * 注销账号的请求体
 *
 * <p>必须带密码。注销是这一整套功能里**唯一不可逆**的操作——
 * 收藏、观看记录连同账号一起没了。光有合法 token 不足以做这件事，
 * token 是能被偷的
 */
public class DeleteAccountRequest {

    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
