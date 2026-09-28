package com.liuyuxiang.animeserver.mapper;

import com.liuyuxiang.animeserver.dto.AdminAnimeItem;
import com.liuyuxiang.animeserver.dto.AnimeDetail;
import com.liuyuxiang.animeserver.dto.AnimeItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 番剧表的读写。只放 SQL，不做任何业务加工
 *
 * <p>@Mapper 让 MyBatis 在启动时为这个接口生成实现类并注册成 Spring Bean，
 * 所以不需要写实现，也不需要 XML 映射文件
 *
 * <p>cover / latest 这两个别名不是随手起的，前端就是按这两个 key 读的，
 * 改名字前端立刻白屏
 */
@Mapper
public interface AnimeMapper {

    /** 公开列表。下架的番剧不出现在这里 */
    @Select("""
            SELECT id,
                   title,
                   cover_image  AS cover,
                   status       AS latest,
                   release_date
            FROM anime
            WHERE is_visible = 1
            ORDER BY release_date DESC
            """)
    List<AnimeItem> selectVisible();

    /**
     * 管理后台的番剧列表，带筛选、排序、分页
     *
     * <p>四个条件都可以不传。全都不传就是「全部番剧，按首播日期倒序，第一页」，
     * 也就是这个接口原来的行为
     *
     * <p>**排序不拼字符串。** ORDER BY 那段用 choose 写死几种组合，
     * 而不是把前端传的字段名拼进 SQL——拼的话就是一个注入点，
     * 而且要挡住它还得先维护一份白名单，不如直接写死
     *
     * <p>下架状态的三态靠 visible 是不是 null 表达：null 全都要，true/false 分别筛
     */
    @Select("""
            <script>
            SELECT id,
                   title,
                   cover_image  AS cover,
                   status       AS latest,
                   release_date,
                   is_visible   AS visible,
                   `desc`
            FROM anime
            <where>
                <if test="q != null and q != ''">
                    AND title LIKE CONCAT('%', #{q}, '%')
                </if>
                <if test="visible != null">
                    AND is_visible = #{visible}
                </if>
            </where>
            <choose>
                <when test="sort == 'title' and order == 'asc'">ORDER BY title ASC, id ASC</when>
                <when test="sort == 'title'">ORDER BY title DESC, id ASC</when>
                <when test="sort == 'release_date' and order == 'asc'">ORDER BY release_date ASC, id ASC</when>
                <otherwise>ORDER BY release_date DESC, id ASC</otherwise>
            </choose>
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<AdminAnimeItem> selectPageForAdmin(@Param("q") String q,
                                            @Param("visible") Boolean visible,
                                            @Param("sort") String sort,
                                            @Param("order") String order,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);

    /** 配套的总数。条件必须和上面那条完全一致，否则页码会和实际对不上 */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM anime
            <where>
                <if test="q != null and q != ''">
                    AND title LIKE CONCAT('%', #{q}, '%')
                </if>
                <if test="visible != null">
                    AND is_visible = #{visible}
                </if>
            </where>
            </script>
            """)
    long countForAdmin(@Param("q") String q, @Param("visible") Boolean visible);

    /** 删除前要先拿到封面路径，行删掉之后就查不到了 */
    @Select("""
            SELECT id,
                   title,
                   cover_image  AS cover,
                   status       AS latest,
                   release_date,
                   is_visible   AS visible,
                   `desc`
            FROM anime
            WHERE id = #{id}
            """)
    AdminAnimeItem selectById(@Param("id") Long id);

    /**
     * 加追番前确认这部番真的存在
     *
     * <p>不查 is_visible。番剧被下架后又从收藏里移除的话，用户就再也加不回来了，
     * 这个规则说不通。而且它已经不在公开列表里，正常操作根本碰不到
     */
    @Select("SELECT EXISTS(SELECT 1 FROM anime WHERE id = #{id})")
    boolean existsById(@Param("id") Long id);

    /**
     * 详情页要的那一条
     *
     * <p>**不过滤 is_visible。** 用户可能从自己的追番或观看记录点进来，
     * 那里面下架的番剧仍然在。是不是上架由 visible 字段带出去，前端自己标「已下架」
     */
    @Select("""
            SELECT id,
                   title,
                   cover_image  AS cover,
                   status       AS latest,
                   release_date,
                   is_visible   AS visible,
                   `desc`
            FROM anime
            WHERE id = #{id}
            """)
    AnimeDetail selectDetailById(@Param("id") Long id);

    /**
     * 按名称和标签搜番剧
     *
     * <p>两个条件都可以不传，都不传就等于「全部上架的番剧」，
     * 所以番剧页不需要两个接口——同一个接口既浏览又筛选
     *
     * <p>**多个标签是 AND 不是 OR。** 搜「奇幻 + 冒险」要的是同时满足的，
     * OR 会返回一大堆只沾一个边的。每个标签一个 IN 子查询就是这个语义
     *
     * <p>关键词走 #{}
     *
     * <p>只返回上架的，下架的搜不到
     */
    @Select("""
            <script>
            SELECT a.id,
                   a.title,
                   a.cover_image  AS cover,
                   a.status       AS latest,
                   a.release_date
            FROM anime a
            WHERE a.is_visible = 1
            <if test="keyword != null and keyword != ''">
                AND a.title LIKE CONCAT('%', #{keyword}, '%')
            </if>
            <foreach collection="tagIds" item="tagId">
                AND a.id IN (SELECT at.anime_id FROM anime_tag at WHERE at.tag_id = #{tagId})
            </foreach>
            ORDER BY a.release_date DESC
            </script>
            """)
    List<AnimeItem> search(@Param("keyword") String keyword, @Param("tagIds") List<Long> tagIds);

    /** 删除前查引用：这张封面还有没有别的番在用 */
    @Select("SELECT COUNT(*) FROM anime WHERE cover_image = #{url}")
    int countByCoverImage(@Param("url") String url);

    @Insert("""
            INSERT INTO anime (title, cover_image, status, release_date, `desc`)
            VALUES (#{title}, #{cover}, #{latest}, #{releaseDate}, #{desc})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdminAnimeItem anime);

    /**
     * 改番剧
     *
     * <p>**不动 is_visible。** 上下架走单独的接口，见 AdminAnimeService
     */
    @Update("""
            UPDATE anime
            SET title        = #{title},
                cover_image  = #{cover},
                status       = #{latest},
                release_date = #{releaseDate},
                `desc`       = #{desc}
            WHERE id = #{id}
            """)
    int update(AdminAnimeItem anime);

    @Update("UPDATE anime SET is_visible = #{visible} WHERE id = #{id}")
    int updateVisibility(@Param("id") Long id, @Param("visible") boolean visible);

    /**
     * 删番剧。剧集、标签关联、轮播、用户的追番和观看记录都由外键级联清掉
     *
     * <p>这是不可逆操作。调用方必须先把它的图片路径取出来，行没了就查不到了
     */
    @Delete("DELETE FROM anime WHERE id = #{id}")
    int delete(@Param("id") Long id);
}
