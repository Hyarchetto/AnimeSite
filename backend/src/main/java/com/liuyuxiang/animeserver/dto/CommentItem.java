package com.liuyuxiang.animeserver.dto;

import java.time.LocalDateTime;

/**
 * 一条评论
 *
 * <p>作者信息摊平放在顶层，不嵌一个 user 对象。评论列表要的只是昵称和头像，
 * 嵌一层会让前端多写一层取值，也让这个接口的形状和 UserInfo 耦合上
 */
public class CommentItem {

    private Long id;

    private String content;

    private LocalDateTime createdAt;

    /** 作者。前端拿它和当前登录用户比对，决定要不要显示删除按钮 */
    private Long userId;

    private String nickname;

    private String avatar;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
}
