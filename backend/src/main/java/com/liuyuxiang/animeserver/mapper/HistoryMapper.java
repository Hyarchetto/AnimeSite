package com.liuyuxiang.animeserver.mapper;

import com.liuyuxiang.animeserver.dto.HistoryItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 观看记录的读写
 *
 * <p>**一部番一行**，只记最新看到的那一集。所以读取不需要再按番剧去重——
 * 表里本来就没有重复的番剧
 */
@Mapper
public interface HistoryMapper {

    /**
     * 某人的观看记录，最近看的在前
     *
     * <p>JOIN 两次：一次取番剧的展示字段，一次取那一集的集号
     */
    @Select("""
            SELECT a.id,
                   a.title,
                   a.cover_image  AS cover,
                   a.status       AS latest,
                   a.release_date,
                   e.episode_no,
                   h.watched_at
            FROM user_history h
            JOIN anime a ON a.id = h.anime_id
            JOIN episode e ON e.id = h.episode_id
            WHERE h.user_id = #{userId}
            ORDER BY h.watched_at DESC
            """)
    List<HistoryItem> selectByUser(@Param("userId") Long userId);

    /**
     * 记一条观看，同一部番只留一行
     *
     * <p>主键冲突时更新集号和时间，而不是新增一行。
     * 往后看一集、回头重看一集，走的都是这一句——效果都是「这部番我看得更多了，时间更近了」
     */
    @Insert("""
            INSERT INTO user_history (user_id, anime_id, episode_id)
            VALUES (#{userId}, #{animeId}, #{episodeId})
            ON DUPLICATE KEY UPDATE episode_id = #{episodeId}, watched_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("userId") Long userId, @Param("animeId") Long animeId,
               @Param("episodeId") Long episodeId);

    /** 删观看记录。删不存在的行影响 0 行，不报错，是幂等的 */
    @Delete("DELETE FROM user_history WHERE user_id = #{userId} AND anime_id = #{animeId}")
    int delete(@Param("userId") Long userId, @Param("animeId") Long animeId);
}
