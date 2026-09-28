package com.liuyuxiang.animeserver.mapper;

import com.liuyuxiang.animeserver.dto.TagItem;
import com.liuyuxiang.animeserver.dto.TagRef;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * 标签表的读写
 *
 * <p>查询要 JOIN 关联表数出每部标签被用了几次。用 LEFT JOIN 而不是 INNER，
 * 这样没人用的标签也会出现在列表里——管理页要能看到并清理它们
 */
@Mapper
public interface TagMapper {

    /**
     * 全部标签，带上「有几部**上架的**番剧在用」
     *
     * <p>**下架的不算进计数。** 这个接口是给公开搜索页用的，
     * 那边只搜得到上架的番。用全部番剧去数的话，标签上写着 3、
     * 点进去只有 2 条，看着像搜索坏了
     *
     * <p>条件写在 JOIN 的 ON 里而不是 WHERE 里。写进 WHERE 的话，
     * 没有任何上架番剧在用的标签会被整行滤掉，而管理页要能看到它们
     */
    @Select("""
            SELECT t.id,
                   t.name,
                   COUNT(a.id) AS anime_count
            FROM tag t
            LEFT JOIN anime_tag at ON at.tag_id = t.id
            LEFT JOIN anime a ON a.id = at.anime_id AND a.is_visible = 1
            GROUP BY t.id, t.name
            ORDER BY t.name
            """)
    List<TagItem> selectAll();

    @Select("""
            SELECT t.id,
                   t.name,
                   COUNT(at.anime_id) AS anime_count
            FROM tag t
            LEFT JOIN anime_tag at ON at.tag_id = t.id
            WHERE t.id = #{id}
            GROUP BY t.id, t.name
            """)
    TagItem selectById(@Param("id") Long id);

    /**
     * 某部番的标签，按标签名升序
     *
     * <p>驱动表是 anime_tag，但不数计数——详情页只是把这部番属于哪几类列出来，
     * 点一下跳到搜索页，不需要「这个标签下有几部番」
     */
    @Select("""
            SELECT t.id,
                   t.name
            FROM anime_tag at
            JOIN tag t ON t.id = at.tag_id
            WHERE at.anime_id = #{animeId}
            ORDER BY t.name
            """)
    List<TagRef> selectByAnime(@Param("animeId") Long animeId);

    /**
     * 管理端的标签列表：计数含下架的番剧
     *
     * <p>和公开的那个接口分开，是因为管理员要知道「这个标签被几部番用着」，
     * 下架的也算——不然删标签时会以为没人用，其实有几部下架的番挂着它
     */
    @Select("""
            SELECT t.id,
                   t.name,
                   COUNT(at.anime_id) AS anime_count
            FROM tag t
            LEFT JOIN anime_tag at ON at.tag_id = t.id
            GROUP BY t.id, t.name
            ORDER BY t.name
            """)
    List<TagItem> selectAllForAdmin();

    /**
     * 返回这批 id 里真实存在的那些
     *
     * <p>给番剧打标签前拿它做一次校验。不校验的话，传一个不存在的 id
     * 会撞外键报 500，用户看到的是一句「服务器内部错误」
     */
    @Select("""
            <script>
            SELECT id FROM tag WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Long> selectExistingIds(@Param("ids") Collection<Long> ids);

    @Insert("INSERT INTO tag (name) VALUES (#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TagItem tag);

    @Update("UPDATE tag SET name = #{name} WHERE id = #{id}")
    int update(@Param("id") Long id, @Param("name") String name);

    /**
     * 删标签
     *
     * <p>anime_tag 里引用它的行由外键级联清掉，番剧本身不受影响——
     * 只是那些番不再有这个标签了
     */
    @Delete("DELETE FROM tag WHERE id = #{id}")
    int delete(@Param("id") Long id);
}
