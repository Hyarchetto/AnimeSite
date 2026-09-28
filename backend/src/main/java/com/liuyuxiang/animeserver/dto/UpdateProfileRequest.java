package com.liuyuxiang.animeserver.dto;

/**
 * PUT /api/auth/profile 的请求体
 *
 * <p>昵称和签名一起提交。改这些不需要验证原密码，它们不是账户凭证——
 * 只有改密码才需要
 *
 * <p>两个字段都是可选的：只传昵称就只改昵称，签名原样保留。
 * 这样前端不用为了改一个字段把另一个也读出来带上
 */
public class UpdateProfileRequest {

    private String nickname;

    private String signature;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
