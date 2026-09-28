package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.ChangePasswordRequest;
import com.liuyuxiang.animeserver.dto.LoginRequest;
import com.liuyuxiang.animeserver.dto.LoginResponse;
import com.liuyuxiang.animeserver.dto.RegisterRequest;
import com.liuyuxiang.animeserver.dto.UpdateProfileRequest;
import com.liuyuxiang.animeserver.dto.UserInfo;
import com.liuyuxiang.animeserver.entity.User;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.UserMapper;
import com.liuyuxiang.animeserver.util.JwtUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 注册与登录的业务层
 *
 * <p>密码在这里做 BCrypt 比对和加密。哈希和明文都不离开这一层，
 * Controller 拿到的只有 token 和 UserInfo
 */
@Service
public class AuthService {

    /** 注册只产出这个角色。管理员由后台提升，没有第二条注册成管理员的路径 */
    private static final String REGISTER_ROLE = "USER";

    private static final String ROLE_ADMIN = "ADMIN";

    /** 没填用户名时的默认前缀，拼上用户 id 就是 用户_12 这样 */
    private static final String DEFAULT_NICKNAME_PREFIX = "用户_";

    private static final int MIN_ACCOUNT_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_SIGNATURE_LENGTH = 200;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ImageService imageService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       ImageService imageService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.imageService = imageService;
    }

    public LoginResponse register(RegisterRequest request) {
        String account = request.getAccount() == null ? "" : request.getAccount().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();

        validateAccount(account);
        validatePassword(password);

        String nickname = request.getNickname() == null ? "" : request.getNickname().trim();
        if (nickname.length() > MAX_NAME_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户名不能超过 " + MAX_NAME_LENGTH + " 个字符");
        }

        User user = new User();
        user.setAccount(account);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname.isEmpty() ? null : nickname);
        user.setRole(REGISTER_ROLE);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            // 不做先查再插的预检。两个人同时注册同一个账号时，那次预检会双双通过，
            // 真正拦住重名的只有数据库的唯一键。这里把它的报错翻译成一句人话
            throw new ApiException(HttpStatus.CONFLICT, "账号已被占用");
        }

        // 没填用户名就补一个 用户_{id}
        //
        // 只能放在插入之后：id 是 AUTO_INCREMENT 生成的，插入前拿不到，
        // 没法在同一句 INSERT 里算出来
        if (user.getNickname() == null) {
            user.setNickname(DEFAULT_NICKNAME_PREFIX + user.getId());
            userMapper.updateNickname(user.getId(), user.getNickname());
        }

        return new LoginResponse(jwtUtil.issue(user.getId()), UserInfo.from(user));
    }

    public LoginResponse login(LoginRequest request) {
        String account = request.getAccount() == null ? "" : request.getAccount().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();

        if (account.isEmpty() || password.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请输入账号和密码");
        }

        User user = userMapper.selectByAccount(account);

        // 用户不存在和密码错误回同一句话，不透露这个账号是否注册过。
        // 分开报的话，这里就成了一个能枚举账号的接口
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        // 禁用能立刻生效，而不是等手上那张 7 天的 token 过期
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        return new LoginResponse(jwtUtil.issue(user.getId()), UserInfo.from(user));
    }

    public UserInfo currentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号不存在");
        }
        return UserInfo.from(user);
    }

    /**
     * 改昵称和签名。不验证原密码，它们不是账户凭证
     *
     * <p>两个字段都是可选的。**没传的字段保持原值**，而不是清空——
     * 只改昵称的请求不该把签名抹掉
     *
     * <p>这里读回完整的 User 再返回，而改密码不返回任何东西。
     * 因为前端改完要立刻显示新名字，改密码则不需要回显
     */
    public UserInfo updateProfile(Long userId, UpdateProfileRequest request) {
        User current = userMapper.selectById(userId);
        if (current == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号不存在");
        }

        String nickname = request.getNickname() == null
                ? current.getNickname()
                : request.getNickname().trim();
        if (nickname == null || nickname.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户名不能为空");
        }
        if (nickname.length() > MAX_NAME_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户名不能超过 " + MAX_NAME_LENGTH + " 个字符");
        }

        String signature = request.getSignature() == null
                ? current.getSignature()
                : request.getSignature().trim();
        if (signature != null && signature.length() > MAX_SIGNATURE_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "签名不能超过 " + MAX_SIGNATURE_LENGTH + " 个字符");
        }
        // 空串存成 null，不然资料页要同时判 null 和空串才知道该不该显示占位文案
        if (signature != null && signature.isEmpty()) {
            signature = null;
        }

        userMapper.updateProfile(userId, nickname, signature);

        User user = userMapper.selectById(userId);
        return UserInfo.from(user);
    }

    /**
     * 换头像。图片先落盘拿到地址，再写进库，最后清掉旧图
     *
     * <p>顺序不能改：先保证库里的记录指向新图，再去动磁盘上的旧文件。
     * 反过来做的话，写库失败就会出现记录指向一张已经被删掉的图
     */
    public UserInfo updateAvatar(Long userId, MultipartFile file) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号不存在");
        }

        String oldAvatar = user.getAvatar();
        String avatar = imageService.store(file);
        userMapper.updateAvatar(userId, avatar);

        // 旧图可能已经没人用了。删之前 ImageService 会查一遍引用，
        // 而且只认 uploads 下的路径，不会碰仓库里的预置图
        if (oldAvatar != null && !oldAvatar.equals(avatar)) {
            imageService.deleteIfOrphaned(oldAvatar);
        }

        return currentUser(userId);
    }

    /**
     * 改密码。必须带原密码并且比对通过
     *
     * <p>光有合法 token 不足以改密码。token 能被偷，要求原密码让偷到 token
     * 和拿到账户之间还差一步
     *
     * <p>改密码之后旧 token 仍然有效——JWT 是无状态的，签出去就收不回来，
     * 要做到失效得再加一层黑名单或版本号，本项目不做
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {
        String oldPassword = request.getOldPassword() == null ? "" : request.getOldPassword();
        String newPassword = request.getNewPassword() == null ? "" : request.getNewPassword();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号不存在");
        }
        if (oldPassword.isEmpty() || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "原密码不正确");
        }

        validatePassword(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "新密码不能与原密码相同");
        }

        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    /**
     * 注销自己的账号
     *
     * <p>**必须验证密码。** 这是整套功能里唯一不可逆的操作——收藏和观看记录
     * 连同账号一起没了。光有合法 token 不够，token 能被偷
     *
     * <p>评论不删，作者被置空显示成「已注销用户」。已经发出去的话属于那部番的讨论，
     * 拿回来会在一串对话里留个洞
     *
     * <p>**最后一个能用的管理员不能注销。** 不拦的话站点就没管理员了，
     * 得改数据库才救得回来
     *
     * <p>**站长也不能注销。** 上面那条只看管理员数量，有第二个管理员时站长就能把自己
     * 注销掉，站点从此没有站长——而调整角色只有站长能做，等于权限永久锁死
     */
    public void deleteAccount(Long userId, String rawPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号不存在");
        }
        if (rawPassword == null || rawPassword.isEmpty()
                || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码不正确");
        }
        if (user.isOwner()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "站长账号不能注销");
        }
        if (ROLE_ADMIN.equals(user.getRole()) && userMapper.countEnabledByRole(ROLE_ADMIN) <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "你是唯一可用的管理员，请先提升另一个人为管理员再注销");
        }

        String avatar = user.getAvatar();
        userMapper.delete(userId);

        // 数据库落定之后再动磁盘。反过来做的话，删除失败就会出现
        // 记录还在图片没了的裂图
        imageService.deleteIfOrphaned(avatar);
    }

    private void validateAccount(String account) {
        if (account.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请输入账号");
        }
        if (account.length() < MIN_ACCOUNT_LENGTH || account.length() > MAX_NAME_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "账号长度需在 " + MIN_ACCOUNT_LENGTH + " 到 " + MAX_NAME_LENGTH + " 个字符之间");
        }
        // 账号带空格会让「账号」和「显示名」的边界变模糊，也容易在前后端各处 trim 不一致
        if (account.chars().anyMatch(Character::isWhitespace)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号不能包含空格");
        }
    }

    private void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "密码至少 " + MIN_PASSWORD_LENGTH + " 位");
        }
    }
}
