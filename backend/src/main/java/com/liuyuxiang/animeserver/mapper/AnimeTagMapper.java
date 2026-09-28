package com.liuyuxiang.animeserver.mapper;

import com.liuyuxiang.animeserver.dto.AnimeTagPair;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 番剧与标签的关联
 *
 * <p>没有单独的主键，用 (anime_id, tag_id) 做联合主键。
 * 这个表没有任何自己的属性，就是两张表的关系
 */
@Mapper
public interface AnimeTagMapper {

    /**
     * 取全部关联，用来给管理页的列表挂上每部番的标签
     *
     * <p>一次全取而不是按番剧逐条查。番剧数量小的时候这样最省事；
     * 数据量大起来要改成按 page 里的 id 过滤，见备注
     */
    @Select("SELECT anime_id, tag_id FROM anime_tag")
    List<AnimeTagPair> selectAllPairs();

    @Select("SELECT anime_id, tag_id FROM anime_tag WHERE anime_id = #{animeId}")
    List<AnimeTagPair> selectPairsByAnime(@Param("animeId") Long animeId);

    /**
     * 清掉某部番的全部标签
     *
     * <p>改标签用的是**整组替换**：先全删再插新的，而不是逐个比对增删。
     * 标签最多几个，全删重插比算差集简单得多，也不会漏
     */
    @Delete("DELETE FROM anime_tag WHERE anime_id = #{animeId}")
    int deleteByAnime(@Param("animeId") Long animeId);

    @Insert("INSERT INTO anime_tag (anime_id, tag_id) VALUES (#{animeId}, #{tagId})")
    int insert(@Param("animeId") Long animeId, @Param("tagId") Long tagId);
}
