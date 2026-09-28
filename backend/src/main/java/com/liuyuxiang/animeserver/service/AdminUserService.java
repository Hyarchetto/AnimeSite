package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.AdminUserItem;
import com.liuyuxiang.animeserver.dto.AdminUserQuery;
import com.liuyuxiang.animeserver.dto.PageResult;
import com.liuyuxiang.animeserver.entity.User;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.UserMapper;
import com.liuyuxiang.animeserver.util.LikeEscaper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 管理后台的用户管理
 *
 * <p>只有改角色和改启用状态两件事。**没有删除**——用户只禁用不删除，
 * 他发过的评论、收藏、观看记录都留着
 *
 * <p>**站长是顶级管理员。** 只有站长能调整别人的角色，其他管理员只能封禁和恢复；
 * 站长本人谁都动不了，封不掉也降不掉。这条规则保证站点永远至少有一个管理员，
 * 不会出现「唯一的管理员把自己降成普通用户」之后没人能管的情况
 */
@Service
public class AdminUserService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final Set<String> ROLES = Set.of(ROLE_USER, ROLE_ADMIN);

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserMapper userMapper;

    public AdminUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public PageResult<AdminUserItem> list(AdminUserQuery query) {
        String keyword = LikeEscaper.escape(query.getQ() == null ? "" : query.getQ().trim());

        int size = query.getSize() == null || query.getSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(query.getSize(), MAX_PAGE_SIZE);
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();

        long total = userMapper.countForAdmin(keyword);
        // 页码可能超出实际范围（比如翻到后面又加了搜索条件），收回到最后一页。
        // 不纠正的话返回空列表，看着像「没有数据」
        int totalPages = Math.max(1, (int) ((total + size - 1) / size));
        page = Math.min(page, totalPages);

        List<AdminUserItem> items = userMapper.selectPageForAdmin(keyword, size, (page - 1) * size);
        return new PageResult<>(items, total, page, size);
    }

    /**
     * 提升或降级
     *
     * <p>**只有站长能调。** 其他管理员连这个接口都过不去，
     * 他们手上只有封禁那一个权力
     *
     * <p>**不能改自己的角色**，站长也一样——这一条现在只是礼貌，
     * 真正兜底的是下面「不能修改站长」那条
     */
    public void setRole(Long operatorId, boolean operatorIsOwner, Long userId, String rawRole) {
        if (!operatorIsOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有站长能调整别人的角色");
        }
        if (operatorId.equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能修改自己的角色");
        }

        String role = rawRole == null ? "" : rawRole.trim().toUpperCase();
        if (!ROLES.contains(role)) {
            // 白名单。不校验的话能写进任意字符串，而拦截器只认 ADMIN，
            // 结果就是这个人既不是管理员也不是普通用户，谁也说不清他是什么
            throw new ApiException(HttpStatus.BAD_REQUEST, "角色只能是 USER 或 ADMIN");
        }

        if (isOwner(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "不能修改站长");
        }

        if (userMapper.updateRole(userId, role) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        }
    }

    /**
     * 启用或禁用
     *
     * <p>**不能禁用自己**，理由和不能改自己的角色一样
     *
     * <p>**站长封不得**，操作者是站长本人也不行。封了站长就没人能再调整角色了，
     * 只能改数据库救回来
     *
     * <p>禁用是立刻生效的：拦截器每次请求都按 userId 查库看 is_enabled，
     * 不依赖 token 什么时候过期
     */
    public void setEnabled(Long operatorId, Long userId, boolean enabled) {
        if (operatorId.equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能禁用自己的账号");
        }

        if (isOwner(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "不能封禁站长");
        }

        if (userMapper.updateEnabled(userId, enabled) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        }
    }

    /**
     * 目标是不是站长，顺带兼任「这个用户存不存在」
     *
     * <p>要多查这一次库，是因为 update 的返回值只说明有没有更新成功，
     * 区分不出「用户不存在」和「他是站长所以不让动」——两种要给不同的状态码和提示
     */
    private boolean isOwner(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return user.isOwner();
    }
}
