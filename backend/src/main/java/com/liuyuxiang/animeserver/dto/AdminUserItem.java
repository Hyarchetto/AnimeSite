package com.liuyuxiang.animeserver.dto;

import java.time.LocalDateTime;

/**
 * 管理端的用户项
 *
 * <p>比公开的 UserProfile 多了 `account`、`role`、`is_enabled`——
 * 管理员要看得见登录名和权限状态。这个接口在 `/api/admin/**` 下，
 * 普通用户够不到，所以不冲突
 */
public class AdminUserItem {

    private Long id;

    private String account;

    private String nickname;

    private String avatar;

    private String signature;

    /** USER 普通 / ADMIN 管理员 */
    private String role;

    /** 列名 is_enabled，开了下划线转驼峰要起别名才映射得上 */
    private Boolean enabled;

    /** 列名 is_owner，同上。站长那一行前端不给任何操作按钮 */
    private Boolean owner;

    private LocalDateTime createdAt;

    /** 收藏了几部番。管理员看到这个数才知道禁用和删除的影响面 */
    private Integer watchlistCount;

    private Integer historyCount;

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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getOwner() {
        return owner;
    }

    public void setOwner(Boolean owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getWatchlistCount() {
        return watchlistCount;
    }

    public void setWatchlistCount(Integer watchlistCount) {
        this.watchlistCount = watchlistCount;
    }

    public Integer getHistoryCount() {
        return historyCount;
    }

    public void setHistoryCount(Integer historyCount) {
        this.historyCount = historyCount;
    }
}
