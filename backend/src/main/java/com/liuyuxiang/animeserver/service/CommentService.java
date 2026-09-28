package com.liuyuxiang.animeserver.service;

import com.liuyuxiang.animeserver.dto.CommentItem;
import com.liuyuxiang.animeserver.dto.PostCommentRequest;
import com.liuyuxiang.animeserver.exception.ApiException;
import com.liuyuxiang.animeserver.mapper.AnimeMapper;
import com.liuyuxiang.animeserver.mapper.CommentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 评论的业务层
 *
 * <p>只有一级评论，没有回复。要加回复的话表上要多一个指向父评论的字段，
 * 还得处理「删了父评论子评论怎么办」和「展示时怎么缩进」，不是一个字段的事
 */
@Service
public class CommentService {

    private static final int MAX_CONTENT_LENGTH = 500;
    private static final String ROLE_ADMIN = "ADMIN";

    private final CommentMapper commentMapper;
    private final AnimeMapper animeMapper;

    public CommentService(CommentMapper commentMapper, AnimeMapper animeMapper) {
        this.commentMapper = commentMapper;
        this.animeMapper = animeMapper;
    }

    /** 读评论不需要登录，谁都能看 */
    public List<CommentItem> list(Long animeId) {
        return commentMapper.selectByAnime(animeId);
    }

    public CommentItem post(Long userId, PostCommentRequest request) {
        Long animeId = request.getAnimeId();
        if (animeId == null || !animeMapper.existsById(animeId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "番剧不存在");
        }

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "评论内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "评论不能超过 " + MAX_CONTENT_LENGTH + " 个字");
        }

        CommentItem comment = new CommentItem();
        comment.setUserId(userId);
        comment.setContent(content);
        commentMapper.insert(animeId, comment);

        // 回读一次，把昵称和头像一起带回去。前端拿到就能直接插到列表最前面，
        // 不用为了显示作者再去查一次
        return commentMapper.selectById(comment.getId());
    }

    /**
     * 删评论。自己的随便删，管理员能删任何人的
     *
     * <p>role 由调用方从 request attribute 取，和 AdminInterceptor 读的是同一个值
     */
    public void delete(Long userId, String role, Long commentId) {
        CommentItem comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "评论不存在");
        }

        // 用 Objects.equals 而不是 comment.getUserId().equals(userId)。
        // 作者注销之后 user_id 是空的，直接调 equals 会抛空指针
        boolean own = Objects.equals(comment.getUserId(), userId);
        if (!own && !ROLE_ADMIN.equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只能删除自己的评论");
        }

        commentMapper.delete(commentId);
    }
}
