package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.ChangePasswordRequest;
import com.liuyuxiang.animeserver.dto.DeleteAccountRequest;
import com.liuyuxiang.animeserver.dto.LoginRequest;
import com.liuyuxiang.animeserver.dto.LoginResponse;
import com.liuyuxiang.animeserver.dto.RegisterRequest;
import com.liuyuxiang.animeserver.dto.UpdateProfileRequest;
import com.liuyuxiang.animeserver.dto.UserInfo;
import com.liuyuxiang.animeserver.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 注册、登录与当前用户
 *
 * <p>userId 来自 JwtInterceptor 放进 request attribute 的值，不是从请求体或路径里取的。
 * 取错了就等于任何人都能查别人的信息
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 注册成功直接返回 token，前端不用再调一次登录 */
    @PostMapping("/register")
    public LoginResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserInfo me(@RequestAttribute("userId") Long userId) {
        return authService.currentUser(userId);
    }

    @PutMapping("/profile")
    public UserInfo updateProfile(@RequestAttribute("userId") Long userId,
                                  @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(userId, request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestAttribute("userId") Long userId,
                               @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
    }

    /**
     * 换头像。multipart 表单，文件字段名是 file
     *
     * <p>上传和应用合成一步，前端选完文件直接发过来就行。需要「先上传拿到地址、
     * 之后再应用」的场景是管理后台配封面，那走另一个接口
     *
     * <p>返回更新后的用户信息，前端拿到就能直接刷新界面
     */
    @PostMapping("/avatar")
    public UserInfo updateAvatar(@RequestAttribute("userId") Long userId,
                                 @RequestParam("file") MultipartFile file) {
        return authService.updateAvatar(userId, file);
    }

    /**
     * 注销账号。**不可逆**
     *
     * <p>用 DELETE 而不是 POST，因为语义就是「把这条资源删掉」。
     * 带请求体是必须的——要验证密码，光凭 token 不够
     */
    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@RequestAttribute("userId") Long userId,
                              @RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(userId, request.getPassword());
    }
}
