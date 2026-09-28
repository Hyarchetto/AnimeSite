package com.liuyuxiang.animeserver.dto;

import com.liuyuxiang.animeserver.entity.User;

/**
 * 对外的用户信息
 *
 * <p>和 User 实体分开，是为了让 password 字段不可能被序列化出去。
 * 直接给实体加 @JsonIgnore 也能达到目的，但那等于把数据库模型和接口契约焊死在一起，
 * 以后实体多一个内部字段就要检查一遍有没有漏掉注解
 *
 * <p>带 role 是因为前端要靠它决定要不要显示管理后台的入口。
 * 真正拦住越权的是后端的 AdminInterceptor，前端这个只影响显示
 */
public class UserInfo {

    private Long id;

    private String account;

    private String nickname;

    /**
     * 头像的站内相对路径。**为空表示用默认头像**，由前端补上那张图
     *
     * <p>默认头像不存进库是有意的：它是一张前端资源，换样式只要换文件，
     * 不用去改所有用户的数据
     */
    private String avatar;

    /** 个性签名。设置页里能改，资料页上给别人看 */
    private String signature;

    private String role;

    /**
     * 站长标记
     *
     * <p>前端靠它决定要不要显示只有站长能用的操作。和 role 一样只影响显示，
     * 真正拦住越权的是 AdminUserService 里的判断
     */
    private boolean owner;

    public static UserInfo from(User user) {
        UserInfo info = new UserInfo();
        info.id = user.getId();
        info.account = user.getAccount();
        info.nickname = user.getNickname();
        info.avatar = user.getAvatar();
        info.signature = user.getSignature();
        info.role = user.getRole();
        info.owner = user.isOwner();
        return info;
    }

    public Long getId() {
        return id;
    }

    public String getAccount() {
        return account;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getSignature() {
        return signature;
    }

    public String getRole() {
        return role;
    }

    public boolean isOwner() {
        return owner;
    }
}
