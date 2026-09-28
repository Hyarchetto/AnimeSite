package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.UserCommentItem;
import com.liuyuxiang.animeserver.dto.UserProfile;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.CommentMapper;
import com.liuyuxiang.animeserver.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公开的用户资料
 *
 * <p>这两个接口不需要登录。**观看记录不在这里**——那是隐私，
 * 只有本人通过 /api/history 看得到自己的
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final CommentMapper commentMapper;

    public UserService(UserMapper userMapper, CommentMapper commentMapper) {
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
    }

    public UserProfile profile(Long userId) {
        UserProfile profile = userMapper.selectProfileById(userId);
        if (profile == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return profile;
    }

    public List<UserCommentItem> comments(Long userId) {
        // 先确认这个人存在。不查的话，不存在的 id 会返回空数组，
        // 和「这个人存在但没发过评论」看起来一模一样
        if (userMapper.selectProfileById(userId) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return commentMapper.selectByUser(userId);
    }
}
