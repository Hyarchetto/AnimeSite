package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.AdminUserItem;
import com.liuyuxiang.animeserver.dto.AdminUserQuery;
import com.liuyuxiang.animeserver.dto.PageResult;
import com.liuyuxiang.animeserver.dto.UpdateUserRequest;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.service.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理
 *
 * <p>**没有删除接口。** 用户只禁用不删除，数据都留着
 *
 * <p>「不能改自己」的检查在 Service 里，用 `@RequestAttribute` 拿到当前操作人的 id——
 * 这个值来自 JWT，不是请求体里的，改不了
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public PageResult<AdminUserItem> list(AdminUserQuery query) {
        return adminUserService.list(query);
    }

    @PutMapping("/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setRole(@RequestAttribute("userId") Long operatorId,
                        @RequestAttribute("owner") boolean operatorIsOwner,
                        @PathVariable Long id,
                        @RequestBody UpdateUserRequest request) {
        adminUserService.setRole(operatorId, operatorIsOwner, id, request.getRole());
    }

    @PutMapping("/{id}/enabled")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setEnabled(@RequestAttribute("userId") Long operatorId,
                           @PathVariable Long id,
                           @RequestBody UpdateUserRequest request) {
        if (request.getEnabled() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "缺少 enabled");
        }
        adminUserService.setEnabled(operatorId, id, request.getEnabled());
    }
}
