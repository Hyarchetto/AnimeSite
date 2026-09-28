package com.liuyuxiang.animeserver.dto;

/**
 * PUT /api/auth/password 的请求体
 *
 * <p>必须带 oldPassword。光有合法 token 不足以改密码——
 * token 是能被偷的，要求原密码让偷到 token 和拿到账户之间还差一步
 */
public class ChangePasswordRequest {

    private String oldPassword;

    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
