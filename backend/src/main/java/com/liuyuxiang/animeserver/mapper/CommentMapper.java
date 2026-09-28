package com.liuyuxiang.animeserver.mapper;

import com.liuyuxiang.animeserver.dto.CommentItem;
import com.liuyuxiang.animeserver.dto.UserCommentItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评论表的读写
 *
 * <p>读的时候要 JOIN 用户表取昵称和头像，因为评论列表要显示作者
 */
@Mapper
public interface CommentMapper {

    /**
     * 某部番的评论，最新的在前
     *
     * <p>排序里带 id 是因为 created_at 只到秒，同一秒发的两条光按时间排不稳定，
     * 顺序可能每次都不一样
     */
    @Select("""
            SELECT c.id,
                   c.content,
                   c.created_at,
                   c.user_id,
                   COALESCE(u.nickname, '已注销用户') AS nickname,
                   u.avatar
            FROM comment c
            LEFT JOIN app_user u ON u.id = c.user_id
            WHERE c.anime_id = #{animeId}
            ORDER BY c.created_at DESC, c.id DESC
            """)
    List<CommentItem> selectByAnime(@Param("animeId") Long animeId);

    /**
     * 删除前要先知道这条评论是谁的
     *
     * <p>LEFT JOIN 而不是 INNER——作者注销之后 user_id 是空的，
     * 用 INNER 的话管理员就再也看不到、也删不掉这条评论了
     */
    @Select("""
            SELECT c.id,
                   c.content,
                   c.created_at,
                   c.user_id,
                   COALESCE(u.nickname, '已注销用户') AS nickname,
                   u.avatar
            FROM comment c
            LEFT JOIN app_user u ON u.id = c.user_id
            WHERE c.id = #{id}
            """)
    CommentItem selectById(@Param("id") Long id);

    /**
     * 发一条评论，并把自增主键写回传进来的对象
     *
     * <p>animeId 单独作参数而不是塞进 CommentItem，是因为那个对象是对外的响应形状，
     * 多一个字段就会被序列化出去。CommentItem 只带用户和内容，番剧是这一句的上下文
     */
    @Insert("""
            INSERT INTO comment (anime_id, user_id, content)
            VALUES (#{animeId}, #{comment.userId}, #{comment.content})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "comment.id")
    int insert(@Param("animeId") Long animeId, @Param("comment") CommentItem comment);

    @Delete("DELETE FROM comment WHERE id = #{id}")
    int delete(@Param("id") Long id);

    /**
     * 某个用户发过的全部评论，带上番剧信息
     *
     * <p>资料页要显示「在《某某》下评论」并能点回去，所以这里 JOIN 番剧表。
     * 不复用 selectByAnime，那个是按番剧查的，维度不同
     */
    @Select("""
            SELECT c.id,
                   c.content,
                   c.created_at,
                   c.user_id,
                   u.nickname,
                   u.avatar,
                   a.id          AS anime_id,
                   a.title       AS anime_title,
                   a.cover_image AS anime_cover
            FROM comment c
            JOIN app_user u ON u.id = c.user_id
            JOIN anime a ON a.id = c.anime_id
            WHERE c.user_id = #{userId}
            ORDER BY c.created_at DESC, c.id DESC
            """)
    List<UserCommentItem> selectByUser(@Param("userId") Long userId);
}
