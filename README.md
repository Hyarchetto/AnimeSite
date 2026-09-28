# 番剧管理网站

一个前后端分离的番剧信息展示站点。

- `frontend/` — Vue 3 + Vue Router + Vite
- `backend/` — Spring Boot + MyBatis + MySQL
- `scripts/` — 本地开发脚本，目前是启动 3307 那个 MySQL 实例

## 技术栈

| 层 | 选型 |
| --- | --- |
| 前端 | Vue 3、Vue Router、Vite |
| 后端 | Java 21、Spring Boot 3.5.16、MyBatis 3.0.5 |
| 数据库 | MySQL 9.5 |
| 构建 | Maven，用项目自带的 `mvnw` 包装器，无需预装 Maven |

## 快速开始

### 1. 启动数据库

MySQL 9.5 在本机是 Windows 服务 `MySQL95`，但启动类型是**手动**，开机不自启。
重启电脑后要先起它：

```bash
scripts\start-mysql.cmd
```

脚本会自己请求管理员权限、启动服务，并等 3307 真正开始监听才返回。
已经在跑时直接返回，重复执行是安全的。**不要直接敲 `net start`**，
那样不确认端口有没有真的起来。

3306 上跑的是 WAMP 自带的 MySQL 5.6.12，和本项目无关，不要动它。

### 2. 建库建表

```bash
mysql -u root -p -P 3307 < backend/src/main/resources/db/schema.sql
mysql -u root -p -P 3307 < backend/src/main/resources/db/data.sql
```

**`-P 3307` 不能省。** 不加的话默认连 3306，那是 WAMP 的实例，会在错误的库上建表。

两张表：

- `anime` — 番剧，字段 `id` / `title` / `cover_image` / `status` / `release_date` / `desc`
- `banner_poster` — 首页轮播，字段 `id` / `anime_id` / `image_url` / `is_active` / `sort_order`

### 3. 配置连接

`backend/src/main/resources/application.yml` 里的连接串默认指向
`localhost:3307/anime_db`。数据库地址或端口不同就改这里。

**密码不要写进这个文件**，它是要入库的。用环境变量：

```bash
export DB_USERNAME=root
export DB_PASSWORD=你的密码
```

或者建一个 `application-local.yml` 放同目录，它已经在 `.gitignore` 里。

### 4. 起后端

```bash
cd backend
./mvnw spring-boot:run
```

Windows 上按终端不同写法不一样，容易踩：

```powershell
# PowerShell —— 不从当前目录找可执行文件，必须加 .\
.\mvnw.cmd spring-boot:run

# CMD —— 不需要 .\
mvnw.cmd spring-boot:run
```

跑在 **3001** 端口。

### 5. 起前端

```bash
cd frontend
npm install
npm run dev
```

打开 http://localhost:5173

## 接口

番剧与轮播两个接口返回**裸 JSON 数组**，没有 `{code, data, msg}` 外壳 —— 前端拿到响应直接
`data.sort(...)`，包了外壳就会报错。

出错时统一返回 `{"message": "..."}`，**和成功响应不是一套形状**。

### 认证与用户

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 否 | 注册，只产出普通账号。`{account, password, nickname?}` |
| POST | `/api/auth/login` | 否 | 登录。`{account, password}` |
| GET | `/api/auth/me` | 是 | 当前用户 |
| PUT | `/api/auth/profile` | 是 | 改用户名，不验证原密码 |
| PUT | `/api/auth/password` | 是 | 改密码，**必须带 `oldPassword`** |
| POST | `/api/auth/avatar` | 是 | 换头像，multipart 字段名 `file` |

**`account` 是登录账号，`nickname` 是界面上显示的「用户名」，两者不是一回事。**
注册时 `nickname` 可以留空，后端会回填成 `用户_{id}`。

### 用户自助

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/watchlist` | 我的追番 |
| POST | `/api/watchlist` | 加追番，body `{animeId}` |
| DELETE | `/api/watchlist/{animeId}` | 取消追番 |
| GET | `/api/history` | 观看进度，**按番剧去重只显最近一集** |
| POST | `/api/history` | 记一条，body `{episodeId}` |

三个写接口都是**幂等**的，返回 204 没有响应体。

### 管理后台

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/anime` | 番剧列表，含已下架的 |
| POST | `/api/admin/anime` | 新增 |
| PUT | `/api/admin/anime/{id}` | 修改，**不动上下架状态** |
| DELETE | `/api/admin/anime/{id}` | 删除，级联清剧集/标签/轮播/用户数据，并清理上传的图 |
| PUT | `/api/admin/anime/{id}/visibility` | 上下架，body `{visible}` |

列表分页，查询参数 `q` / `visible` / `sort` / `order` / `page` / `size`。

#### 剧集

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/admin/anime/{animeId}/episodes` | 新增，body `{episodeNo, title, airDate, watchUrl}` |
| PUT | `/api/admin/episodes/{id}` | 修改，字段同上 |
| DELETE | `/api/admin/episodes/{id}` | 删除 |

读取复用公开的 `GET /api/anime/{id}/episodes`。剧集没有上下架的概念，管理员和普通
用户看到的是同一份数据，没必要做两个接口。

写操作的路径一个挂在番剧下、一个平铺在 `episodes` 下：新建时要指定是哪部番，
改删时剧集 id 本身就够了。

#### 标签

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/tags` | 列表，带每个标签被几部番用着 |
| POST | `/api/admin/tags` | 新建，body `{name}` |
| PUT | `/api/admin/tags/{id}` | 重命名，字段同上 |
| DELETE | `/api/admin/tags/{id}` | 删除，只解除关联，不删番剧 |

计数**含已下架的番剧**，和公开的 `/api/tags` 不一样——管理员要的是「有多少部番挂着
这个标签」，下架的也算，删之前才知道会影响什么。

#### 轮播

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/banners` | 列表，**含未启用的** |
| POST | `/api/admin/banners` | 新建，body `{animeId, imageUrl, active, sortOrder}` |
| PUT | `/api/admin/banners/{id}` | 修改，只传要改的字段，没传的沿用原值 |
| DELETE | `/api/admin/banners/{id}` | 删除，番剧和图片都不动 |

列表含未启用的，否则管理员没法把停用的再打开。

轮播的启停字段叫 `active`，番剧的叫 `visible`，两者不统一，前端依赖各自的写法。

#### 用户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/users` | 分页列表，查询参数 `q` / `page` / `size` |
| PUT | `/api/admin/users/{id}/role` | 改角色，body `{role}` |
| PUT | `/api/admin/users/{id}/enabled` | 禁用或启用，body `{enabled}` |

**没有删除接口**，用户只禁用不删除，数据都留着。

「不能改自己」的检查在 Service 层，操作人 id 取自 JWT 而不是请求体，改不了。

#### 图片上传

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/admin/upload` | multipart 字段名 `file`，落 `backend/uploads/` |

给用户自己换头像走 `POST /api/auth/avatar`，同样是 multipart 字段名 `file`。

### 预置账号

`admin / admin123`（管理员，用户名「站长」）和 `liu / user123`（普通用户，用户名「小刘」）。

**这两个密码是公开的，只用于本地演示。上线前必须改掉。**

### 公开接口

以下都不需要登录。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/anime` | 上架的番剧，按首播日期倒序 |
| GET | `/api/search` | 搜索 |
| GET | `/api/anime/{id}` | 番剧详情 |
| GET | `/api/anime/{id}/episodes` | 剧集列表 |
| GET | `/api/anime/{id}/comments` | 评论列表 |
| GET | `/api/banner` | 启用的轮播 |
| GET | `/api/tags` | 标签列表，带计数 |
| GET | `/api/users/{id}` | 用户公开资料 |
| GET | `/api/users/{id}/comments` | 某用户的评论 |

发评论和删评论要登录，见下面的「评论」。

#### `GET /api/anime`

**上架**的番剧，按首播日期倒序。下架的走 `/api/admin/anime`。

```json
[
  {
    "id": 1,
    "title": "葬送的芙莉莲",
    "cover": "/covers/frieren.jpg",
    "latest": "已完结",
    "release_date": "2023-09-29"
  }
]
```

`cover` 映射自列 `cover_image`，`latest` 映射自列 `status`，这两个别名是前端依赖的契约。
`release_date` 保持下划线命名。

#### `GET /api/search`

两个参数都可以不传。`q` 是名称关键词，`tags` 是标签 id 列表，**多个标签是同时满足**
的关系。

标签用 `?tags=1&tags=2` 或 `?tags=1,2` 都行，Spring 两种都能绑到 `List`。
只搜得到上架的番剧。

`q` 里的 LIKE 通配符会被转义，用户搜一个 `%` 不会等于搜全部，见
`util/LikeEscaper.java`。

#### `GET /api/anime/{id}`

番剧详情。**下架的也能取到**，因为用户可能从自己的追番或观看记录点进来，
是不是上架看返回体里的 `visible`。

#### `GET /api/banner`

启用的轮播，按 `sort_order` 升序，JOIN 番剧表取标题与封面。

```json
[
  {
    "title": "葬送的芙莉莲",
    "status": "已完结",
    "desc": "魔王讨伐之后，精灵魔法使芙莉莲踏上理解人类短暂一生的旅程",
    "imageSrc": "/banners/frieren-banner.jpg",
    "coverSrc": "/covers/frieren.jpg"
  }
]
```

`status` 为空时回落到 `已完结`，`desc` 为空时回落到 `暂无简介` —— 兜底在 Service 层做。

注意这个接口是驼峰命名，上面那个是下划线命名。这个不一致前端依赖它，不做统一。

### 评论

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/comments` | 是 | 发评论，body `{animeId, content}` |
| DELETE | `/api/comments/{id}` | 是 | 删评论 |

**读和写拆成了两个 Controller**，因为认证要求不同：读是公开的，挂在
`/api/anime/{id}/comments` 下；写要登录，统一在 `/api/comments` 下。不把写也放在
同一条路径上，是因为拦截器只能按路径前缀配，区分不了同一路径上的 GET 和 POST。

发评论返回完整的评论对象，前端拿到直接插到列表最前面，不用重拉整页。

删评论时自己的可以删、管理员能删任何人的，`role` 由拦截器放进 attribute。

## 图片

两类图片，两处存储，**但数据库里存的都是站内相对路径**：

| 类别 | 位置 | 谁提供 | 路径形如 |
| --- | --- | --- | --- |
| 预置：封面、轮播、默认头像 | `frontend/public/` | Vite，5173 | `/covers/xxx.jpg` |
| 上传：头像 | `backend/uploads/` | 后端，3001 | `/uploads/<uuid>.jpg` |

预置图是源码的一部分，跟着仓库走。上传图是运行时数据，在 `.gitignore` 里。

地址形式一样但提供方不同，所以浏览器对 `/uploads/xxx.jpg` 会按页面来源解析到 5173，
那里没有这个文件，**图片会裂掉而且控制台不报错**。前端用
`src/utils/image.js` 里的 `resolveImageUrl()` 把这一类补上后端的源。

命名统一为**全小写 kebab-case**：封面 `<slug>.jpg`、轮播 `<slug>-banner.jpg`、
头像 `<slug>-avatar.avif`。上传的图是 UUID。

## 几个选型说明

**为什么是 Spring Boot 3.5 而不是 4.x。** `start.spring.io` 现在默认生成 4.x，
但 4.x 目前没有兼容的 MyBatis starter，而国内企业的主流仍是 3.x。因此
`pom.xml` 里的 parent 版本是手动钉的。

**为什么用 MyBatis 而不是 JPA。** 这里只有两条 SQL，其中一条带 JOIN，且需要
把列名重映射成前端要的字段名。用 MyBatis 可以直接把 SQL 与别名原样写下，
用 JPA 反而要额外处理 `@Column` 与 `@JsonProperty` 的映射。

**为什么不用 Lombok。** getter/setter 手写出来，字段与列名的对应关系才看得见。
要加随时可以加。
