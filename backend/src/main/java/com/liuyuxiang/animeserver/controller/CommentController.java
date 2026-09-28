package com.liuyuxiang.animeserver.controller;

import com.liuyuxiang.animeserver.dto.CommentItem;
import com.liuyuxiang.animeserver.dto.PostCommentRequest;
import com.liuyuxiang.animeserver.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 写评论
 *
 * <p>和读评论分开成两个 Controller，是因为它们的认证要求不同：
 * 读是公开的，挂在 AnimeController 下的 /api/anime/{id}/comments；
 * 写要登录，路径统一在 /api/comments 下，拦截器按这一前缀拦
 *
 * <p>不把写也放在 /api/anime/{id}/comments，是因为拦截器只能按路径前缀配，
 * 区分不了同一个路径上的 GET 和 POST
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** 返回完整的评论对象，前端拿到直接插到列表最前面，不用重拉整页 */
    @PostMapping
    public CommentItem post(@RequestAttribute("userId") Long userId,
                            @RequestBody PostCommentRequest request) {
        return commentService.post(userId, request);
    }

    /** 自己的评论可以删，管理员能删任何人的。role 由拦截器放进 attribute */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute("userId") Long userId,
                       @RequestAttribute("role") String role,
                       @PathVariable Long id) {
        commentService.delete(userId, role, id);
    }
}
