package com.liuyuxiang.animeserver.mapper;

import com.liuyuxiang.animeserver.dto.EpisodeItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 剧集表的查询
 *
 * <p>管理员的剧集增删改在 4.2 加
 */
@Mapper
public interface EpisodeMapper {

    /**
     * 取这一集属于哪部番，不存在就返回 null
     *
     * <p>观看记录是按「一部番一行」存的，写之前得知道这一集归哪部番。
     * 返回 null 同时兼任了「这一集存不存在」的检查——两件事一次查询办完
     */
    @Select("SELECT anime_id FROM episode WHERE id = #{id}")
    Long findAnimeIdById(@Param("id") Long id);

    @Select("""
            SELECT id,
                   episode_no,
                   title,
                   air_date,
                   watch_url
            FROM episode
            WHERE anime_id = #{animeId}
            ORDER BY episode_no
            """)
    List<EpisodeItem> selectByAnime(@Param("animeId") Long animeId);

    @Select("""
            SELECT id,
                   episode_no,
                   title,
                   air_date,
                   watch_url
            FROM episode
            WHERE id = #{id}
            """)
    EpisodeItem selectById(@Param("id") Long id);

    /** 改集号前要确认同一部番里没占用，唯一键会拦住，但报错信息没法给用户看 */
    @Select("SELECT EXISTS(SELECT 1 FROM episode WHERE anime_id = #{animeId} AND episode_no = #{episodeNo})")
    boolean existsByAnimeAndNo(@Param("animeId") Long animeId, @Param("episodeNo") Integer episodeNo);

    @Insert("""
            INSERT INTO episode (anime_id, episode_no, title, air_date, watch_url)
            VALUES (#{animeId}, #{episode.episodeNo}, #{episode.title}, #{episode.airDate}, #{episode.watchUrl})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "episode.id")
    int insert(@Param("animeId") Long animeId, @Param("episode") EpisodeItem episode);

    @Update("""
            UPDATE episode
            SET episode_no = #{episodeNo},
                title      = #{title},
                air_date   = #{airDate},
                watch_url  = #{watchUrl}
            WHERE id = #{id}
            """)
    int update(EpisodeItem episode);

    @Delete("DELETE FROM episode WHERE id = #{id}")
    int delete(@Param("id") Long id);
}
