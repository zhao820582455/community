# zbtech community

> 社区/社群管理后端服务：Spring Boot + MyBatis-Plus 实现，提供用户、帖子、评论、分类、点赞、收藏、关注、消息、敏感词、协议、运营日志及管理后台 API。

> 原 **foxbook**（开源社区论坛系统）后端的 **Java（Spring Boot 3.0）** 翻译版。
> 本文档与代码由 AI 依据 `foxbook-php` 的数据库结构与接口逻辑生成，作为后续业务接口（点赞/评论/消息/鉴权等）翻译的基础骨架。

## 技术栈

| 组件 | 版本 / 说明 |
|---|---|
| JDK | 17 |
| Spring Boot | 3.0.13 |
| 持久层 | MyBatis-Plus 3.5.5（`mybatis-plus-spring-boot3-starter`） |
| 数据库 | MySQL 5.7+（utf8mb4），地址 `10.10.9.171:3306` |
| 缓存 / 队列 | Redis（`10.10.9.171:6379`） |
| 接口文档 | springdoc-openapi（Swagger UI：`/swagger-ui.html`） |

## 与原 foxbook 的关键改造

| 项 | 原 foxbook | 本工程 zbtech |
|---|---|---|
| 包名 / 项目名 | `foxbook` | `com.zbtech.community` / `zbtech` |
| 表前缀 | `fox_` | `lb_` |
| 主键 | `char(36)` UUID 字符串（非自增） | `INT` **自增** |
| 外键类字段 | `char(36)` | `INT` |
| 站点 / 品牌名 | 狐书 / foxbook.cn | zbtech / zbtech.com |
| 数据库名 | `foxbook` | `zbtech` |
| 统一响应 | `{code,message,result}` | `Result<T>`（见 `common/Result.java`），分页 `PageResult` |
| 状态码 | 200/400/2001/403 | `common/ErrorCode` 保持一致 |

> 说明：原库所有主键为 UUID v7 字符串，翻译为 Java 后统一改为 `INT AUTO_INCREMENT`，
> 因此所有关联字段（user_id / post_id 等）也同步改为 `INT`。
> 涉及 `foxbook` 字样的文案、配置值（邮箱 `hi@zbtech.com`、站点 title、文档地址等）已替换为 `zbtech`。

## 目录结构

```
community/
├── pom.xml
├── sql/
│   ├── lb_community.sql     # 建表 + 初始化数据（lb_ 前缀 / int 自增）
│   ├── _gen.py              # 代码生成器（实体/Mapper/Service/Controller）
│   └── _createdb.py         # 一键连库建库建表脚本
└── src/main/java/com/zbtech/community/
    ├── CommunityApplication.java     # 启动类
    ├── common/                       # Result / PageResult / 异常 / 错误码 / BaseController
    ├── config/                       # MyBatis-Plus 分页 / CORS / Redis
    ├── entity/                       # 24 张表的实体（@TableName("lb_xxx")）
    ├── mapper/                       # 25 个 Mapper（BaseMapper，含 for update 悲观锁方法）
    ├── service/ (+ impl)             # 28 个 Service（IService / ServiceImpl，含完整业务逻辑）
    ├── vo/                           # 视图对象（UserBriefVO / UserVO / CommentVO / MessageVO / PostBriefVO / DiscussVO）
    └── controller/
        ├── api/                      # 用户端 13 个控制器（Auth/Post/Comments/Message/User/Search/Category/Discuss/Agreement/Link/PlatformConfig/ActivityMessage/WechatMp）
        └── adminapi/                 # 管理端 26 个控制器（14 个完整业务 + 12 个通用 CRUD）
```

## 数据库

- 库名：`zbtech`（已在 `10.10.9.171` 直接创建，账号 `root` / `ttk123456`）。
- 24 张表，前缀 `lb_`；主键 `INT AUTO_INCREMENT`。
- 初始数据：超级管理员 `admin`（密码 `admin123`，bcrypt）、角色/菜单（24 条菜单树 + 角色全授权）、
  平台配置（微信小程序 / 阿里云 OSS / 站点 / 邮件）、认证标签、PC 顶部导航。

如需在其它环境重建：

```bash
# 方式一：直接用 MySQL 客户端执行
mysql -h10.10.9.171 -uroot -pttk123456 < sql/lb_community.sql

# 方式二：用自带脚本（需 pip install pymysql）
python sql/_createdb.py
```

## 如何运行

```bash
# 1) 用你本机正常的 Maven（环境里的 mvn 有故障，需自行使用可联网的 Maven）
mvn clean package
java -jar target/community-1.0.0.jar
# 或 IDE 中直接运行 CommunityApplication
```

启动后：

- 接口根路径：`/`
- 管理端通用 CRUD 示例：`GET /adminapi/post/getListByPage`、`POST /adminapi/post/save` 等
- Swagger：`http://localhost:8080/swagger-ui.html`

## 已翻译业务接口（用户端 /api）

> 鉴权方式：请求头 `Authorization: <token>`，由 `ApiAuthInterceptor` 解析并在 `request` 注入 `user_id`；
> 未登录访问需鉴权接口返回 `code=2001`。token 存于 Redis，TTL 7 天。

### 鉴权 Auth（`controller/api/AuthApiController`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/auth/accountLogin` | POST | 账号/邮箱 + 密码登录（BCrypt 校验，兼容 PHP `$2y$`），返回 `{token, user}` |
| `/api/auth/sendEmailCode` | POST | 发送邮箱验证码（login/register 场景；Redis 存码 + 60s 冷却；暂未接邮件商，仅打印日志） |
| `/api/auth/accountRegister` | POST | 邮箱注册（验证码校验 + BCrypt 加密） |
| `/api/auth/mnpLogin` | POST | 微信小程序登录（**暂未集成**，返回友好提示） |
| `/api/auth/info` | GET | 当前登录用户信息（需 token，密码已置空） |

### 微信小程序 WechatMp（`controller/api/WechatMpApiController` + `service/WechatMpService`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/wechatMp/login` | POST | 微信小程序登录（code2session 换 openid/session_key，查找/创建用户，签发 token） |

### 帖子 Post（`controller/api/PostApiController`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/post/getListByPage` | GET | 列表（置顶优先；支持 `categoryId` / `userId` / `discussId` 过滤；每项 `content_html` 由 PostContentService 渲染） |
| `/api/post/publish` | POST | 发布帖子（需登录；`media` 存 JSON；敏感词/微信审核暂跳过） |
| `/api/post/getInfo` | GET | 详情（浏览量 +1；带当前用户 `is_liked` / `is_favorited`；`content_html` Markdown 渲染） |
| `/api/post/like` | POST | 点赞 / 取消（事务 + 行锁 + 计数防漂移 + 发消息通知） |
| `/api/post/favorite` | POST | 收藏 / 取消（同上） |
| `/api/post/currentUser` | GET | 当前用户发布的帖子（含 `content_html` 渲染） |
| `/api/post/favorites` | GET | 当前用户收藏的帖子（含 `content_html` 渲染） |
| `/api/post/likes` | GET | 当前用户点赞的帖子（含 `content_html` 渲染） |
| `/api/post/favoritesByUserId` | GET | 某用户收藏的帖子（含 `content_html` 渲染） |
| `/api/post/likesByUserId` | GET | 某用户点赞的帖子（含 `content_html` 渲染） |

### 评论 Comments（`controller/api/CommentsApiController` + `service/CommentsService`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/comments/publish` | POST | 发布评论（支持楼中楼；事务内行锁，自动解析被回复人，帖子 `comment_count` +1） |
| `/api/comments/getParentComments` | GET | 根评论分页（带 `user` / `to_user` / 当前用户 `is_liked`） |
| `/api/comments/getChildComments` | GET | 某根评论的回复分页（`exclude_comment_id` 可排除某条） |
| `/api/comments/like` | POST | 评论点赞 / 取消（事务 + 行锁 + 计数防漂移 + 发点赞消息） |

### 消息 Message（`controller/api/MessageApiController` + `service/MessageService`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/message/getUnreadCount` | GET | 未读统计（总量 + 按类型分组 + 最新系统/活动消息） |
| `/api/message/getUserMsgList` | GET | 消息分页列表（组装 `user` / `to_user` / `post` / `activity_meta`） |
| `/api/message/markAsRead` | POST | 标记单条已读 |
| `/api/message/markAllAsRead` | POST | 标记全部已读（可按 `type` 过滤） |

### 用户 User（`controller/api/UserApiController` + `service/UsersService` / `UserFollowService`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/user/whoami` | GET | 当前登录用户资料（脱敏） |
| `/api/user/getInfo` | GET | 同 whoami（兼容前端） |
| `/api/user/updateProfile` | POST | 单字段更新（白名单 nickname/avatar/introduction/phone/email） |
| `/api/user/saveInfo` | POST | 批量更新资料 |
| `/api/user/follow` | POST | 关注（同步双方计数；自关/目标不存在抛错） |
| `/api/user/unfollow` | POST | 取消关注（同步双方计数） |
| `/api/user/checkFollowStatus` | GET | 关注状态（是否关注/互关） |
| `/api/user/getUserById` | GET | 他人主页（实时关注/粉丝数 + 当前用户关注状态） |
| `/api/user/getHotUsers` | GET | 热门用户（status=0，按获赞+帖子数） |

### 搜索 Search（`controller/api/SearchApiController`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/search/query` | GET | 关键词搜索：用户/帖子/话题，各 limit 10；帖子带 80 字内容摘要，话题内容截断 80 字；返回 `{keyword, users, posts, discusses, user_count, post_count, discuss_count, count}` |

### 类目 Category（`controller/api/CategoryApiController`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/category/getList` | GET | 类目列表 |

### 话题 Discuss（`controller/api/DiscussApiController` + `service/DiscussService`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/discuss/publish` | POST | 发布话题 |
| `/api/discuss/getInfo` | GET | 话题详情（浏览量 +1） |
| `/api/discuss/getListByPage` | GET | 话题分页 |

### 协议 Agreement（`controller/api/AgreementApiController`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/agreement/getAgreement` | GET | 按ID获取协议内容（公开接口） |

### 广告 Link（`controller/api/LinkApiController`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/link/getListByPage` | GET | 广告分页列表 |
| `/api/link/getList` | GET | 全部广告 |
| `/api/link/getDetail` | GET | 广告详情 |

### 平台配置 PlatformConfig（`controller/api/PlatformConfigApiController`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/platformConfig/getPublicSiteConfig` | GET | 获取公开站点配置（title/subtitle/description/logo/icp） |

### 活动消息 ActivityMessage（`controller/api/ActivityMessageApiController`）
| 端点 | method | 说明 |
|---|---|---|
| `/api/activityMessage/getDetail` | GET | 活动消息详情（标记已读 + 返回活动元数据） |

## 已翻译业务接口（管理端 /adminapi）

> 鉴权方式：请求头 `Authorization: <admin_token>`，由 `AdminAuthInterceptor` 解析并在 `request` 注入 `admin_id`；
> 管理员登录通过 `/adminapi/auth/login` 签发 admin token 存 Redis。

### 管理员鉴权 Auth（`controller/adminapi/AdminAuthController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/auth/login` | POST | 管理员登录（用户名+密码，BCrypt 校验，签发 admin token） |
| `/adminapi/auth/logout` | POST | 管理员登出（删除 Redis token） |
| `/adminapi/auth/info` | GET | 当前管理员信息 |
| `/adminapi/auth/changePassword` | POST | 修改密码（校验旧密码） |
| `/adminapi/auth/menu` | GET | 获取当前管理员菜单树 |
| `/adminapi/auth/roles` | GET/POST | 角色 CRUD |
| `/adminapi/auth/permissions` | GET/POST | 权限 CRUD |
| `/adminapi/auth/admins` | GET/POST | 管理员 CRUD |

### 仪表盘 Dashboard（`controller/adminapi/DashboardAdminController` + `service/DashboardService`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/dashboard/getStatistics` | GET | 用户/管理员/日志统计（总量/今日/活跃/增长率） |
| `/adminapi/dashboard/getUserGrowth` | GET | 近7天用户增长趋势 |
| `/adminapi/dashboard/getSystemInfo` | GET | 系统信息（Java版本/内存/磁盘） |
| `/adminapi/dashboard/getUserDistribution` | GET | 用户终端分布 |
| `/adminapi/dashboard/getRecentLogs` | GET | 最近操作日志 |

### 帖子管理 Post（`controller/adminapi/PostAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/post/getListByPage` | GET | 帖子搜索列表（标题/内容/用户/置顶过滤） |
| `/adminapi/post/getDetail` | GET | 帖子详情（含作者信息） |
| `/adminapi/post/setTop` | POST | 置顶/取消置顶 |
| `/adminapi/post/delete` | POST | 删除帖子（级联删除评论/收藏/点赞，回退用户帖子数） |
| `/adminapi/post/batchDelete` | POST | 批量删除 |

### 评论管理 Comment（`controller/adminapi/CommentsAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/comment/getListByPage` | GET | 评论搜索列表 |
| `/adminapi/comment/getDetail` | GET | 评论详情 |
| `/adminapi/comment/delete` | POST | 删除评论（级联删除子评论，回退计数） |
| `/adminapi/comment/batchDelete` | POST | 批量删除 |

### 用户管理 User（`controller/adminapi/UsersAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/user/getListByPage` | GET | 用户搜索列表（昵称/手机/状态） |
| `/adminapi/user/getDetail` | GET | 用户详情 |
| `/adminapi/user/updateStatus` | POST | 封禁/解禁 |

### 类目管理 Category（`controller/adminapi/CategoryAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/category/getListByPage` | GET | 类目搜索列表 |
| `/adminapi/category/createCategory` | POST | 创建类目（名称唯一校验） |
| `/adminapi/category/updateCategory` | POST | 更新类目（名称唯一校验） |
| `/adminapi/category/deleteCategory` | POST | 删除类目 |
| `/adminapi/category/batchDeleteCategories` | POST | 批量删除 |
| `/adminapi/category/getCategoryOptions` | GET | 类目选项列表 |

### 广告管理 Link（`controller/adminapi/LinkAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/link/getListByPage` | GET | 广告搜索列表 |
| `/adminapi/link/getBannerDetail` | GET | 广告详情 |
| `/adminapi/link/save` | POST | 保存广告（字段校验：标题≤10/链接≤255/类型1-3/AppId） |
| `/adminapi/link/deleteBanner` | POST | 删除广告 |
| `/adminapi/link/batchDeleteBanners` | POST | 批量删除 |

### 活动消息管理 ActivityMessage（`controller/adminapi/ActivityMessageAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/activityMessage/getListByPage` | GET | 活动消息搜索列表 |
| `/adminapi/activityMessage/getDetail` | GET | 活动消息详情 |
| `/adminapi/activityMessage/save` | POST | 创建活动消息并分发（全量/指定用户，URL/富文本） |
| `/adminapi/activityMessage/searchUsers` | GET | 搜索用户（选择推送目标） |

### 协议管理 Agreement（`controller/adminapi/AgreementAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/agreement/getListByPage` | GET | 协议搜索列表 |
| `/adminapi/agreement/getAgreement` | GET | 协议详情 |
| `/adminapi/agreement/saveAgreement` | POST | 更新协议 |
| `/adminapi/agreement/delete` | POST | 删除协议 |

### 平台配置 PlatformConfig（`controller/adminapi/PlatformConfigAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/platformConfig/getPlatformConfig` | GET | 按key获取配置（不传key返回全部） |
| `/adminapi/platformConfig/saveConfig` | POST | 保存配置（键值对，含默认值合并） |

### 顶部导航 WebTopNav（`controller/adminapi/WebTopNavAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/webTopNav/getListByPage` | GET | 导航搜索列表 |
| `/adminapi/webTopNav/createNav` | POST | 创建导航（名称≤12/URL≤255/打开方式1-2） |
| `/adminapi/webTopNav/updateNav` | POST | 更新导航 |
| `/adminapi/webTopNav/deleteNav` | POST | 删除导航 |
| `/adminapi/webTopNav/batchDeleteNavs` | POST | 批量删除 |

### 认证标签 AuthTag（`controller/adminapi/AuthTagAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/authTag/getListByPage` | GET | 标签搜索列表 |
| `/adminapi/authTag/save` | POST | 保存标签（名称唯一校验，≤20字符） |
| `/adminapi/authTag/deleteTag` | POST | 删除标签 |
| `/adminapi/authTag/toggleStatus` | POST | 切换状态 |

### 敏感词 SensitiveWord（`controller/adminapi/SensitiveWordAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/sensitiveWord/getListByPage` | GET | 敏感词搜索列表 |
| `/adminapi/sensitiveWord/save` | POST | 保存敏感词（词唯一校验，≤50字符） |
| `/adminapi/sensitiveWord/deleteWord` | POST | 删除敏感词 |
| `/adminapi/sensitiveWord/toggleStatus` | POST | 切换状态 |

> `SensitiveWordService.replaceText(content)` 提供敏感词替换功能（正则匹配+星号替换），可供帖子/评论发布时调用。

### 操作日志 OperationLog（`controller/adminapi/OperationLogsAdminController`）
| 端点 | method | 说明 |
|---|---|---|
| `/adminapi/operationLog/getListByPage` | GET | 日志搜索列表（IP/URI/方法/成功/类型） |
| `/adminapi/operationLog/getLogDetail` | GET | 日志详情 |
| `/adminapi/operationLog/clearLogs` | POST | 清理日志（全部/N天前） |
| `/adminapi/operationLog/getStatistics` | GET | 统计信息（总量/成功率/平均耗时/每日趋势/方法分布） |

## 通用 CRUD（管理端，PHP 无专属控制器）

以下表在 PHP 端通过 `BaseAdminApi` 通用 CRUD 处理，Java 端保留等效的 56 行通用 CRUD 控制器：

| 控制器 | 表 | 说明 |
|---|---|---|
| `AdminsAdminController` | `lb_admins` | 管理员 CRUD（RBAC 管理在 AdminAuthController） |
| `AdminsRolesAdminController` | `lb_admins_roles` | 角色 CRUD |
| `AdminsPermissionAdminController` | `lb_admins_permission` | 权限 CRUD |
| `AdminsRolePermissionAdminController` | `lb_admins_role_permission` | 角色权限关联 CRUD |
| `CommentLikesAdminController` | `lb_comment_likes` | 评论点赞 CRUD |
| `DiscussAdminController` | `lb_discuss` | 话题 CRUD |
| `FavoritesAdminController` | `lb_favorites` | 收藏 CRUD |
| `LikesAdminController` | `lb_likes` | 点赞 CRUD |
| `MediaCheckAdminController` | `lb_media_check` | 内容审核记录 CRUD |
| `MessageAdminController` | `lb_message` | 消息 CRUD |
| `UserAuthTagAdminController` | `lb_user_auth_tag` | 用户认证标签关联 CRUD |
| `UserFollowAdminController` | `lb_user_follow` | 关注关系 CRUD |

## 翻译完成状态

**全部 PHP 控制器已翻译完成：**
- /api 用户端：14/14 控制器（Auth、Post、Comments、Message、User、Search、Category、Discuss、Agreement、Link、PlatformConfig、ActivityMessage、WechatMp，不含 BaseApi 基类）
- /adminapi 管理端：14/14 控制器（Auth/RBAC、Dashboard、Post、Comment、User、Category、Link、ActivityMessage、Agreement、PlatformConfig、WebTopNav、AuthTag、SensitiveWord、OperationLog）

**外部集成状态：**
- 微信小程序登录：已翻译（`WechatMpService`，jscode2session + 用户创建 + token 签发）
- 敏感词过滤：已翻译（`SensitiveWordService.replaceText`，正则匹配+星号替换）
- 阿里云邮件/短信：暂未集成（`sendEmailCode` 仅打印日志）
- 阿里云 OSS 上传：暂未集成
- 内容安全审核：暂未集成（帖子/评论发布时跳过）

> 完整接口清单与业务逻辑见桌面文件：`foxbook-master/foxbook后端接口与功能说明.md`
