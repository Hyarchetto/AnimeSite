-- 番剧管理网站 演示数据
--
-- 执行方式
--   mysql -u root -P 3307 < data.sql
--
-- 先跑 schema.sql 再跑这份

USE anime_db;

-- 图片路径与 frontend/public 下实际存在的文件一一对应
--   /covers/     7 张封面，竖版 2:3
--   /banners/    3 张轮播大图，横版，与封面不是同一张
--   /avatars/    默认头像，用户的 avatar 为空时由前端补上
--
-- 全部是小写 kebab-case。系统里还有一类图是后台上传的，路径形如 /uploads/xxx.jpg，
-- 存在后端磁盘上由后端托管，文件名是 UUID
--
-- 两类图的路径都是站内相对形式。前者浏览器按页面来源解析正好找到 Vite，
-- 后者要补上后端的源，判断收口在前端的 resolveImageUrl 里

INSERT INTO anime (id, title, cover_image, status, release_date, `desc`) VALUES
(1, '葬送的芙莉莲',       '/covers/frieren.jpg',                    '已完结',       '2023-09-29', '魔王讨伐之后，精灵魔法使芙莉莲踏上理解人类短暂一生的旅程'),
(2, '我独自升级',         '/covers/solo-leveling.jpg',              '已完结',       '2024-01-07', '最弱的猎人成振宇在死亡边缘获得系统，独自变强的故事'),
(3, '我心里危险的东西',   '/covers/the-dangers-in-my-heart.jpg',    '已完结',       '2023-04-02', '内向少年与开朗少女之间别扭又笨拙的青春物语'),
(4, '末日酒店',           '/covers/apocalypse-hotel.jpg',           '更新至第 12 集', '2025-04-09', '末日之后的酒店里，人类与妖怪共处的奇谭'),
(5, '败犬女主太多了',     '/covers/make-heroine-ga-oosugiru.jpg',   '已完结',       '2024-07-14', '总是输给女主角的配角少女们，也值得被认真对待'),
(6, '琉璃的宝石',         '/covers/ruri-rocks.jpg',                 '更新至第 24 集', '2025-04-06', '痴迷矿物的少女琉璃，在山野之间寻找属于自己的宝石'),
(7, '我怎么可能成为你的恋人', '/covers/watanare.jpg',                '更新至第 11 集', '2025-07-08', '一心憧憬挚友的少女，被卷入了意料之外的告白');

-- 故意把 3 号的简介清空，它在轮播里，用来验证接口的「暂无简介」兜底真的生效
UPDATE anime SET `desc` = NULL WHERE id = 3;

-- 7 号故意下架，用来验证公开列表过滤 is_visible 而管理后台仍然看得到它
UPDATE anime SET is_visible = 0 WHERE id = 7;


-- sort_order 决定展示顺序，界面上不显示这个数字
INSERT INTO banner_poster (anime_id, image_url, is_active, sort_order) VALUES
(1, '/banners/frieren-banner.jpg',                   1, 1),
(2, '/banners/solo-leveling-banner.jpg',             1, 2),
(3, '/banners/the-dangers-in-my-heart-banner.jpg',   1, 3);


-- 预置账号
--   admin / admin123   管理员
--   liu   / user123    普通用户，带追番和观看记录，用来测用户自助那几个接口
--
-- 账号是登录用的凭据，nickname 才是界面上显示的用户名
--
-- 这两个密码是公开的，写在这里是为了本地能直接登录演示
-- 任何真实部署前必须改掉，否则等于把管理员权限公开挂在网上
INSERT INTO app_user (id, account, password, nickname, role, is_owner) VALUES
(1, 'admin', '$2b$10$tK/Yk4SYZZ1PxFBtdCBaHuEgZVfyZeNxAukXB9M3GwwvQL8UUQ0V6', '站长', 'ADMIN', 1),
(2, 'liu',   '$2b$10$3Gc3H7RBQDBe7DQKECAIo.2b9xVJE0K9k9aZhyda5tWAxc4aGDibC', '小刘', 'USER',  0);


INSERT INTO tag (id, name) VALUES
(1, '奇幻'), (2, '冒险'), (3, '战斗'), (4, '恋爱'),
(5, '校园'), (6, '悬疑'), (7, '喜剧'), (8, '日常'),
(9, '治愈'), (10, '百合');

INSERT INTO anime_tag (anime_id, tag_id) VALUES
(1, 1), (1, 2),   -- 葬送的芙莉莲         奇幻 冒险
(2, 1), (2, 3),   -- 我独自升级           奇幻 战斗
(3, 4), (3, 5),   -- 我心里危险的东西      恋爱 校园
(4, 1), (4, 6),   -- 末日酒店             奇幻 悬疑
(5, 4), (5, 7),   -- 败犬女主太多了        恋爱 喜剧
(6, 8), (6, 9),   -- 琉璃的宝石           日常 治愈
(7, 4), (7, 10);  -- 我怎么可能成为你的恋人 恋爱 百合


-- 111 条剧集一次生成
--
-- 手写这么多行只会变成噪音。递归 CTE 把「集号序列」和「哪部番有多少集」分成两件事：
-- nums 生成 1 到 28 的序列，targets 声明每部番的总集数和首播日，join 一下就是全部剧集
-- 加一部番只需要在 targets 里加一行，不用碰 nums
--
-- 28 是所有番里最大的总集数，nums 只需要生成到它
--
-- watch_url 是占位地址。真实的正规观看链接要由管理员在后台逐集填写，
-- 演示数据里没有来源可考，如实填 example.com 而不是编一个像真的域名
INSERT INTO episode (anime_id, episode_no, title, air_date, watch_url)
WITH RECURSIVE nums (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM nums WHERE n < 28
),
targets (anime_id, total, first_air, slug) AS (
    SELECT 1, 28, '2023-09-29', 'frieren'             UNION ALL
    SELECT 2, 12, '2024-01-07', 'solo-leveling'       UNION ALL
    SELECT 3, 12, '2023-04-02', 'dangers-in-my-heart' UNION ALL
    SELECT 4, 12, '2025-04-09', 'apocalypse-hotel'    UNION ALL
    SELECT 5, 12, '2024-07-14', 'make-heroine'        UNION ALL
    SELECT 6, 24, '2025-04-06', 'ruri-rocks'          UNION ALL
    SELECT 7, 11, '2025-07-08', 'watanare'
)
SELECT t.anime_id,
       n.n,
       CONCAT('第 ', n.n, ' 话'),
       DATE_ADD(t.first_air, INTERVAL (n.n - 1) * 7 DAY),
       CONCAT('https://example.com/anime/', t.slug, '/ep', n.n)
FROM targets t
JOIN nums n ON n.n <= t.total;


-- 小刘的追番和观看记录
-- 观看记录是「一部番一行」，只记最新看到的那一集：
-- 芙莉莲看到第 3 集、末日后酒店看到第 1 集
INSERT INTO user_watchlist (user_id, anime_id) VALUES
(2, 1), (2, 4), (2, 6);

INSERT INTO user_history (user_id, anime_id, episode_id, watched_at)
SELECT 2, e.anime_id, e.id, DATE_ADD('2026-09-10 20:00:00', INTERVAL e.episode_no DAY)
FROM episode e
WHERE (e.anime_id = 1 AND e.episode_no = 3)
   OR (e.anime_id = 4 AND e.episode_no = 1);
