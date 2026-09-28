package com.liuyuxiang.animeserver.mapper;

import com.liuyuxiang.animeserver.dto.AdminUserItem;
import com.liuyuxiang.animeserver.dto.UserProfile;
import com.liuyuxiang.animeserver.entity.User;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户表的查询与新增。只放 SQL，不做任何业务加工
 *
 * <p>is_enabled 必须写成 AS enabled。开了 map-underscore-to-camel-case 之后
 * MyBatis 会把列名 is_enabled 归一成 isenabled 去找属性，而实体里的属性名是 enabled，
 * 对不上。起个别名绕开这个名字转换，和 anime 表用 cover / latest 别名是同一个道理
 */
@Mapper
public interface UserMapper {

    @Select("""
            SELECT id,
                   account,
                   password,
                   nickname,
                   avatar,
                   signature,
                   role,
                   is_enabled AS enabled,
                   is_owner   AS owner,
                   created_at
            FROM app_user
            WHERE account = #{account}
            """)
    User selectByAccount(@Param("account") String account);

    @Select("""
            SELECT id,
                   account,
                   password,
                   nickname,
                   avatar,
                   signature,
                   role,
                   is_enabled AS enabled,
                   is_owner   AS owner,
                   created_at
            FROM app_user
            WHERE id = #{id}
            """)
    User selectById(@Param("id") Long id);

    /**
     * 注册专用。role 由 Service 传固定值，永远不来自请求体
     *
     * <p>不写 is_enabled 和 is_owner，交给数据库的默认值。
     * 新注册的人不可能是站长，也不该是封禁状态
     */
    @Insert("""
            INSERT INTO app_user (account, password, nickname, role)
            VALUES (#{account}, #{password}, #{nickname}, #{role})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 只改昵称。不能写成整行 UPDATE——
     * 那样要先把整个 User 读出来再写回去，中途被别的请求改了密码就会被覆盖掉
     */
    @Update("UPDATE app_user SET nickname = #{nickname} WHERE id = #{id}")
    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    /**
     * 用户资料页要的那一份
     *
     * <p>**不查 account 也不查 password。** 这是个公开接口，
     * 查出来就得靠 DTO 挡着不序列化出去，不如一开始就别取
     */
    @Select("""
            SELECT id,
                   nickname,
                   avatar,
                   signature,
                   created_at
            FROM app_user
            WHERE id = #{id}
            """)
    UserProfile selectProfileById(@Param("id") Long id);

    /** 昵称和签名一起改。分两次 UPDATE 的话中间会有一个两份数据不一致的瞬间 */
    @Update("""
            UPDATE app_user
            SET nickname  = #{nickname},
                signature = #{signature}
            WHERE id = #{id}
            """)
    int updateProfile(@Param("id") Long id, @Param("nickname") String nickname,
                      @Param("signature") String signature);

    @Update("UPDATE app_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE app_user SET avatar = #{avatar} WHERE id = #{id}")
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);

    /** 删除前查引用：这张头像还有没有别的用户在用的可能性 */
    @Select("SELECT COUNT(*) FROM app_user WHERE avatar = #{url}")
    int countByAvatar(@Param("url") String url);

    /**
     * 管理端的用户列表，带搜索和分页
     *
     * <p>收藏数和观看记录数用子查询算。这两个数管理员要看——
     * 禁用或删除之前得知道会影响到什么
     *
     * <p>按 id 升序，先注册的在前。不带次级排序的话顺序不稳定
     */
    @Select("""
            <script>
            SELECT u.id,
                   u.account,
                   u.nickname,
                   u.avatar,
                   u.signature,
                   u.role,
                   u.is_enabled AS enabled,
                   u.is_owner   AS owner,
                   u.created_at,
                   (SELECT COUNT(*) FROM user_watchlist w WHERE w.user_id = u.id) AS watchlist_count,
                   (SELECT COUNT(*) FROM user_history h WHERE h.user_id = u.id)   AS history_count
            FROM app_user u
            <where>
                <if test="q != null and q != ''">
                    AND (u.account LIKE CONCAT('%', #{q}, '%')
                         OR u.nickname LIKE CONCAT('%', #{q}, '%'))
                </if>
            </where>
            ORDER BY u.id ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<AdminUserItem> selectPageForAdmin(@Param("q") String q,
                                           @Param("limit") int limit, @Param("offset") int offset);

    /** 配套的总数。条件必须和上面那条完全一致，否则页码会和实际对不上 */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM app_user u
            <where>
                <if test="q != null and q != ''">
                    AND (u.account LIKE CONCAT('%', #{q}, '%')
                         OR u.nickname LIKE CONCAT('%', #{q}, '%'))
                </if>
            </where>
            </script>
            """)
    long countForAdmin(@Param("q") String q);

    @Update("UPDATE app_user SET role = #{role} WHERE id = #{id}")
    int updateRole(@Param("id") Long id, @Param("role") String role);

    @Update("UPDATE app_user SET is_enabled = #{enabled} WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);

    /**
     * 注销账号
     *
     * <p>收藏、观看记录由外键级联清掉；评论**不删**，作者会被置空
     * 显示成「已注销用户」——评论已经属于那部番的讨论，不是这个人的私人物品
     */
    @Delete("DELETE FROM app_user WHERE id = #{id}")
    int delete(@Param("id") Long id);

    /**
     * 数有几个**能用的**管理员
     *
     * <p>只数启用的。算上被封禁的会让检查失效——剩两个管理员但有一个被封着的话，
     * 另一个注销之后就真的没人能管站点了
     */
    @Select("SELECT COUNT(*) FROM app_user WHERE role = #{role} AND is_enabled = 1")
    int countEnabledByRole(@Param("role") String role);
}
