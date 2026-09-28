-- 番剧管理网站 建表脚本
--
-- 执行方式
--   mysql -u root -P 3307 < schema.sql
--
-- 放在 db/ 而不是 resources 根目录，是为了避开 Spring Boot 的
-- spring.sql.init 自动执行机制，这两份脚本由人手动跑一次即可
--
-- 这份脚本会先 DROP 再建，库里的数据全部丢失

CREATE DATABASE IF NOT EXISTS anime_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE anime_db;

-- 表之间有外键依赖，DROP 顺序反了会报错
-- 暂时关掉外键检查让顺序无关，以后加表不用回头维护这个顺序
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS user_history;
DROP TABLE IF EXISTS user_watchlist;
DROP TABLE IF EXISTS anime_tag;
DROP TABLE IF EXISTS episode;
DROP TABLE IF EXISTS banner_poster;
DROP TABLE IF EXISTS tag;
DROP TABLE IF EXISTS app_user;
DROP TABLE IF EXISTS anime;
SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE anime (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title        VARCHAR(200) NOT NULL                COMMENT '番剧名称',
    cover_image  VARCHAR(500)                         COMMENT '封面图，站内相对路径或上传后的完整 URL',
    status       VARCHAR(100)                         COMMENT '更新状态，如「更新至第 12 集」，管理员手填',
    release_date DATE                                 COMMENT '首播日期',
    is_visible   TINYINT      NOT NULL DEFAULT 1      COMMENT '是否在网页显示，0 为下架',
    `desc`       TEXT                                 COMMENT '简介，desc 是保留字必须反引号',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (id),
    -- 公开列表恒为「上架 + 按首播日期倒序」，这条索引就是为它建的
    KEY idx_visible_date (is_visible, release_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '番剧';


CREATE TABLE banner_poster (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    anime_id   BIGINT   NOT NULL                COMMENT '关联的番剧',
    image_url  VARCHAR(500)                     COMMENT '轮播大图，横版，与竖版封面不是同一张',
    is_active  TINYINT  NOT NULL DEFAULT 1      COMMENT '是否启用，1 启用 0 停用',
    sort_order INT      NOT NULL DEFAULT 0      COMMENT '展示顺序，升序',
    PRIMARY KEY (id),
    CONSTRAINT fk_banner_anime FOREIGN KEY (anime_id) REFERENCES anime (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '首页轮播';

-- sort_order 只在后台用来定顺序，**界面上不显示这个数字**，
-- 管理员靠列表上的上下箭头调整。数字本身不用人工维护，也就没有维护负担


CREATE TABLE app_user (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    account    VARCHAR(50) NOT NULL                COMMENT '账号，登录用',
    password   VARCHAR(100) NOT NULL               COMMENT 'BCrypt 哈希，绝不存明文',
    nickname   VARCHAR(50)                         COMMENT '用户名，就是显示名。注册没填的话回填成 用户_{id}',
    avatar     VARCHAR(500)                        COMMENT '头像，站内相对路径。为空就用前端的默认头像',
    signature  VARCHAR(200)                        COMMENT '个性签名',
    role       VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER 普通 / ADMIN 管理员',
    is_owner   TINYINT     NOT NULL DEFAULT 0      COMMENT '站长标记，全站唯一。只有站长能调整别人的角色',
    is_enabled TINYINT     NOT NULL DEFAULT 1      COMMENT '是否启用，0 禁用后登不上',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account (account)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户';


CREATE TABLE episode (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    anime_id   BIGINT       NOT NULL                COMMENT '所属番剧',
    episode_no INT          NOT NULL                COMMENT '集号，从 1 开始',
    title      VARCHAR(200)                         COMMENT '单集标题',
    air_date   DATE                                 COMMENT '播出日期',
    watch_url  VARCHAR(500)                         COMMENT '正规观看地址，外链完整 URL',
    PRIMARY KEY (id),
    UNIQUE KEY uk_anime_episode (anime_id, episode_no),
    CONSTRAINT fk_episode_anime FOREIGN KEY (anime_id) REFERENCES anime (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '剧集';


CREATE TABLE tag (
    id   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(50) NOT NULL                COMMENT '标签名，唯一，防止「恋爱」「爱情」并存',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '标签';


CREATE TABLE anime_tag (
    anime_id BIGINT NOT NULL COMMENT '番剧',
    tag_id   BIGINT NOT NULL COMMENT '标签',
    PRIMARY KEY (anime_id, tag_id),
    -- 联合主键只能高效回答「这部番有哪些标签」，回答不了「哪些番有奇幻标签」
    -- 而搜索页问的正是后者
    KEY idx_tag (tag_id),
    CONSTRAINT fk_at_anime FOREIGN KEY (anime_id) REFERENCES anime (id) ON DELETE CASCADE,
    CONSTRAINT fk_at_tag   FOREIGN KEY (tag_id)   REFERENCES tag (id)   ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '番剧标签关联';


CREATE TABLE user_watchlist (
    user_id    BIGINT   NOT NULL                COMMENT '用户',
    anime_id   BIGINT   NOT NULL                COMMENT '番剧',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    -- 主键就是这两列，没有自增 id：这张表的行为身份就是「谁收了哪部」，
    -- 而且没有任何地方引用行 id，多一列只会是纯粹的负担。
    -- 「同一部番不能重复收藏」这条业务规则也就直接落在主键上，
    -- Java 里先查再插有并发漏洞，交给数据库
    PRIMARY KEY (user_id, anime_id),
    CONSTRAINT fk_wl_user  FOREIGN KEY (user_id)  REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_wl_anime FOREIGN KEY (anime_id) REFERENCES anime (id)    ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '我的追番';


CREATE TABLE comment (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    anime_id   BIGINT   NOT NULL                COMMENT '评论的是哪部番',
    user_id    BIGINT   NULL                    COMMENT '谁发的。为空表示账号已注销',
    content    VARCHAR(500) NOT NULL            COMMENT '评论内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发表时间',
    PRIMARY KEY (id),
    -- 详情页永远是「按番剧取，最新的在前」，这条索引就是为它建的
    KEY idx_anime_time (anime_id, created_at),
    CONSTRAINT fk_comment_anime FOREIGN KEY (anime_id) REFERENCES anime (id)    ON DELETE CASCADE,
    -- 用户注销时把评论的作者置空，评论本身留着——它已经属于那部番的讨论，
    -- 不是这个人的私人物品。置空之后显示成「已注销用户」
    CONSTRAINT fk_comment_user  FOREIGN KEY (user_id)  REFERENCES app_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '评论';


CREATE TABLE user_history (
    user_id    BIGINT   NOT NULL                COMMENT '用户',
    anime_id   BIGINT   NOT NULL                COMMENT '哪部番，主键的一环',
    episode_id BIGINT   NOT NULL                COMMENT '看到哪一集，随进度变化',
    watched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次观看时间',
    -- 这是进度表不是流水账：**一部番一行**，只记最新看到哪一集。
    -- 重看或往后看都走 upsert 更新同一行，不新增。
    -- 主键就是这两列，没有自增 id：集号是会变的那一个不能进主键，
    -- 这张表本身也没有任何被引用的行 id
    PRIMARY KEY (user_id, anime_id),
    KEY idx_user_time (user_id, watched_at),
    CONSTRAINT fk_hist_user    FOREIGN KEY (user_id)    REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_hist_anime   FOREIGN KEY (anime_id)   REFERENCES anime (id)    ON DELETE CASCADE,
    CONSTRAINT fk_hist_episode FOREIGN KEY (episode_id) REFERENCES episode (id)  ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '观看记录';
