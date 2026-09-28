package com.liuyuxiang.animeserver.entity;

import java.time.LocalDateTime;

/**
 * app_user 表的一行
 *
 * <p>只在后端内部流转，不直接序列化成 JSON 回给前端——password 字段在里面。
 * 对外的形状是 dto 包里的 UserInfo
 */
public class User {

    private Long id;

    private String account;

    /** BCrypt 哈希。这个名字上不要加序列化注解，因为整个类就不该被序列化出去 */
    private String password;

    private String nickname;

    /** 头像的站内相对路径，如 /uploads/xxx.jpg。为空表示用前端那张默认头像 */
    private String avatar;

    /** 个性签名，显示在用户资料页上 */
    private String signature;

    /** USER 普通 / ADMIN 管理员 */
    private String role;

    /** 映射自列 is_enabled，禁用后登不上 */
    private boolean enabled;

    /** 映射自列 is_owner。全站唯一的顶级管理员，只有他能调整别人的角色 */
    private boolean owner;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
