package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.UserCommentItem;
import com.liuyuxiang.animeserver.dto.UserProfile;
import com.liuyuxiang.animeserver.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开的用户资料
 *
 * <p>**整个类都不在拦截器的名单里**，不需要登录就能看。
 * 这是「点头像看别人的资料」这个功能的前提
 *
 * <p>返回体里没有 account——登录名只在本人设置页里可见
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserProfile profile(@PathVariable Long id) {
        return userService.profile(id);
    }

    /** TA 发过的评论，带上番剧信息好点回去 */
    @GetMapping("/{id}/comments")
    public List<UserCommentItem> comments(@PathVariable Long id) {
        return userService.comments(id);
    }
}
