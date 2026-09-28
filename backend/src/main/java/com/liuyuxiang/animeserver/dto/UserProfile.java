package com.liuyuxiang.animeserver.dto;

import java.time.LocalDateTime;

/**
 * 对外的用户资料，别人点头像看到的就是这个
 *
 * <p>**没有 account。** 那是登录凭据，只有本人在自己的设置页里看得到。
 * 资料页是公开的，把登录名放在这里等于告诉所有人「这个名字没人用，去试着撞密码吧」
 */
public class UserProfile {

    private Long id;

    private String nickname;

    private String avatar;

    private String signature;

    /** 注册时间，资料页上显示「加入于」 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
