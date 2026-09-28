package com.liuyuxiang.animeserver.mapper;

import com.liuyuxiang.animeserver.dto.AnimeItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 追番表的查询与增删
 *
 * <p>查询要 JOIN 回 anime 取展示字段，而不是只返回 anime_id——
 * 前端拿到就直接喂给 AnimeCard 了，只给 id 的话还得再查一轮
 *
 * <p>不按 is_visible 过滤。已经收藏的番剧被下架后仍然要出现在列表里，
 * 前端自己加一个「已下架」标记
 */
@Mapper
public interface WatchlistMapper {

    @Select("""
            SELECT a.id,
                   a.title,
                   a.cover_image  AS cover,
                   a.status       AS latest,
                   a.release_date
            FROM user_watchlist w
            JOIN anime a ON a.id = w.anime_id
            WHERE w.user_id = #{userId}
            ORDER BY w.created_at DESC
            """)
    List<AnimeItem> selectByUser(@Param("userId") Long userId);

    /**
     * 重复收藏不报错
     *
     * <p>主键冲突时更新一下自己，net effect 是不做任何事。
     * 前端那个追番按钮是切换式的，用户连点两下不应该弹错误——
     * 这个操作要的是「保证它在我的列表里」，本来就应该幂等
     */
    @Insert("""
            INSERT INTO user_watchlist (user_id, anime_id)
            VALUES (#{userId}, #{animeId})
            ON DUPLICATE KEY UPDATE anime_id = anime_id
            """)
    int insert(@Param("userId") Long userId, @Param("animeId") Long animeId);

    /** 取消追番。删不存在的行影响 0 行，不报错，同样是幂等的 */
    @Delete("DELETE FROM user_watchlist WHERE user_id = #{userId} AND anime_id = #{animeId}")
    int delete(@Param("userId") Long userId, @Param("animeId") Long animeId);
}
