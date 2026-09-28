package com.liuyuxiang.animeserver.mapper;

import com.liuyuxiang.animeserver.dto.AdminBannerItem;
import com.liuyuxiang.animeserver.dto.BannerItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 首页轮播的查询，需要 JOIN 番剧表取标题与封面。
 */
@Mapper
public interface BannerMapper {

    /**
     * 取启用的轮播，按 sort_order 升序。
     *
     * <p>别名一律写成下划线形式，交给 MyBatis 的 map-underscore-to-camel-case
     * 自动转成 Java 字段的驼峰名，这样就不必去关心 MySQL 返回列别名时的大小写行为。
     *
     * <p>状态与简介的兜底默认值不在这里做，放在 Service 层，理由见 BannerService。
     */
    /**
     * 首页要显示的轮播
     *
     * <p>**两个条件都要。** `b.is_active` 是管理员对这条轮播的开关，
     * `a.is_visible` 是番剧本身有没有下架——番剧下架时它的轮播也该跟着消失，
     * 重新上架又自动回来
     *
     * <p>用「查的时候过滤」而不是「下架番剧时顺手把轮播改成停用」，
     * 是因为后者会覆盖管理员自己的选择：一条被手动停用的轮播，
     * 番剧重新上架时不该被自动打开
     *
     * <p>顺序按 sort_order，它是后台用上下箭头维护的，不需要人工填
     */
    @Select("""
            SELECT a.title,
                   a.status,
                   a.`desc`      AS `desc`,
                   b.image_url   AS image_src,
                   a.cover_image AS cover_src
            FROM banner_poster b
            JOIN anime a ON b.anime_id = a.id
            WHERE b.is_active = 1
              AND a.is_visible = 1
            ORDER BY b.sort_order ASC
            """)
    List<BannerItem> selectActive();

    /**
     * 取某部番的轮播图路径
     *
     * <p>删番剧时要在删之前调用，删完这些行就被级联清掉了，再查是查不到的
     */
    @Select("SELECT image_url FROM banner_poster WHERE anime_id = #{animeId}")
    List<String> selectImageUrlsByAnime(@Param("animeId") Long animeId);

    /** 删除前查引用：这张轮播图还有没有别的记录在用 */
    @Select("SELECT COUNT(*) FROM banner_poster WHERE image_url = #{url}")
    int countByImageUrl(@Param("url") String url);

    /**
     * 管理端的全部轮播，含未启用的
     *
     * <p>JOIN 番剧表是为了拿到标题和封面——管理页要显示「这条轮播是哪部番的」，
     * 编辑时还要靠它回填下拉框
     *
     * <p>排序带上 id 做兜底：sort_order 相同的时候，不指定次级排序的话
     * 每次查询的顺序可能不一样
     */
    @Select("""
            SELECT b.id,
                   b.anime_id,
                   a.title       AS anime_title,
                   a.cover_image AS anime_cover,
                   b.image_url,
                   b.is_active   AS active,
                   b.sort_order,
                   a.is_visible  AS anime_visible
            FROM banner_poster b
            JOIN anime a ON a.id = b.anime_id
            ORDER BY b.sort_order, b.id
            """)
    List<AdminBannerItem> selectAllForAdmin();

    @Select("""
            SELECT b.id,
                   b.anime_id,
                   a.title       AS anime_title,
                   a.cover_image AS anime_cover,
                   b.image_url,
                   b.is_active   AS active,
                   b.sort_order,
                   a.is_visible  AS anime_visible
            FROM banner_poster b
            JOIN anime a ON a.id = b.anime_id
            WHERE b.id = #{id}
            """)
    AdminBannerItem selectById(@Param("id") Long id);

    @Insert("""
            INSERT INTO banner_poster (anime_id, image_url, is_active, sort_order)
            VALUES (#{animeId}, #{imageUrl}, #{active}, #{sortOrder})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdminBannerItem banner);

    @Update("""
            UPDATE banner_poster
            SET anime_id   = #{animeId},
                image_url  = #{imageUrl},
                is_active  = #{active},
                sort_order = #{sortOrder}
            WHERE id = #{id}
            """)
    int update(AdminBannerItem banner);

    @Delete("DELETE FROM banner_poster WHERE id = #{id}")
    int delete(@Param("id") Long id);
}
