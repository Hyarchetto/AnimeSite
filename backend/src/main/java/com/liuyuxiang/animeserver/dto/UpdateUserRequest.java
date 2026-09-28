package com.liuyuxiang.animeserver.dto;

/**
 * 改用户角色或启用状态的请求体
 *
 * <p>两个接口共用一个。各自只读自己要的字段，另一个留空即可
 */
public class UpdateUserRequest {

    /** USER 或 ADMIN */
    private String role;

    private Boolean enabled;

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
}
