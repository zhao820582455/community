-- =========================================================
-- zbtech community 建表 + 初始化数据
-- 由 foxbook 翻译而来：表前缀 lb_，主键 INT 自增，foxbook->zbtech
-- MySQL 5.7+ (utf8mb4)
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for lb_admins
-- ----------------------------
DROP TABLE IF EXISTS `lb_admins`;
CREATE TABLE `lb_admins`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `role_id` INT NOT NULL COMMENT '角色ID',
  `username` VARCHAR(32) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `real_name` VARCHAR(100) NOT NULL COMMENT '姓名',
  `email` VARCHAR(100) NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) NULL DEFAULT NULL COMMENT '头像',
  `bio` VARCHAR(255) NULL DEFAULT NULL COMMENT '个人简介',
  `status` TINYINT(1) NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  `last_login_time` DATETIME NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(45) NULL DEFAULT NULL COMMENT '最后登录IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_username`(`username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '管理员表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_admins_permission
-- ----------------------------
DROP TABLE IF EXISTS `lb_admins_permission`;
CREATE TABLE `lb_admins_permission`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` INT NULL DEFAULT 0 COMMENT '父级菜单ID',
  `name` VARCHAR(100) NOT NULL COMMENT '菜单标识',
  `title` VARCHAR(6) NULL DEFAULT NULL COMMENT '菜单标题',
  `icon` VARCHAR(50) NULL DEFAULT NULL COMMENT '菜单图标',
  `path` VARCHAR(255) NULL DEFAULT NULL COMMENT '菜单路径或API路径',
  `component` VARCHAR(255) NULL DEFAULT NULL COMMENT '组件路径',
  `meta` VARCHAR(255) NULL DEFAULT NULL COMMENT 'meta(JSON)',
  `sort` INT(11) NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT(1) NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  `type` TINYINT(1) NULL DEFAULT 1 COMMENT '1 菜单，2接口',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_parent_id`(`parent_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单/接口权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_admins_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `lb_admins_role_permission`;
CREATE TABLE `lb_admins_role_permission`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` INT NOT NULL COMMENT '角色ID',
  `menu_id` INT NOT NULL COMMENT '菜单ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_role_menu`(`role_id`, `menu_id`) USING BTREE,
INDEX `idx_role_id`(`role_id`) USING BTREE,
INDEX `idx_menu_id`(`menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色菜单关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_admins_roles
-- ----------------------------
DROP TABLE IF EXISTS `lb_admins_roles`;
CREATE TABLE `lb_admins_roles`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) NULL DEFAULT NULL COMMENT '角色描述',
  `status` TINYINT(1) NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  `is_founder` TINYINT(1) NULL DEFAULT 0 COMMENT '是否创建人：1 创建人，拥有系统所有权限',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_agreement
-- ----------------------------
DROP TABLE IF EXISTS `lb_agreement`;
CREATE TABLE `lb_agreement`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '协议ID',
  `title` VARCHAR(50) NOT NULL COMMENT '协议标题',
  `content` LONGTEXT NOT NULL COMMENT '协议内容(HTML)',
  `updated_at` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '协议表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_auth_tag
-- ----------------------------
DROP TABLE IF EXISTS `lb_auth_tag`;
CREATE TABLE `lb_auth_tag`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` VARCHAR(20) NOT NULL COMMENT '标签名称',
  `color` VARCHAR(20) NOT NULL DEFAULT '#1677ff' COMMENT '标签颜色',
  `sort` INT(11) NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_auth_tag_name`(`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '认证标签表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_category
-- ----------------------------
DROP TABLE IF EXISTS `lb_category`;
CREATE TABLE `lb_category`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '类目ID',
  `name` VARCHAR(10) NOT NULL COMMENT '类目名称',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '圈子类目表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_comment_likes
-- ----------------------------
DROP TABLE IF EXISTS `lb_comment_likes`;
CREATE TABLE `lb_comment_likes`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `comment_id` INT NOT NULL COMMENT '评论ID',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_comment_likes_user_comment`(`user_id`, `comment_id`) USING BTREE,
INDEX `idx_comment_likes_comment_id`(`comment_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论点赞表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_comments
-- ----------------------------
DROP TABLE IF EXISTS `lb_comments`;
CREATE TABLE `lb_comments`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `parent_id` INT NULL DEFAULT 0 COMMENT '父评论ID',
  `root_id` INT NULL DEFAULT 0 COMMENT '根评论ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `to_user_id` INT NOT NULL COMMENT '回复目标用户ID',
  `post_id` INT NOT NULL COMMENT '帖子ID',
  `content` VARCHAR(255) NOT NULL COMMENT '评论内容',
  `like_count` INT(11) NULL DEFAULT 0 COMMENT '点赞数量',
  `reply_count` INT(11) NULL DEFAULT 0 COMMENT '回复数量',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_comments_post_parent`(`post_id`, `parent_id`) USING BTREE,
INDEX `idx_comments_root_id`(`root_id`) USING BTREE,
INDEX `idx_comments_parent_id`(`parent_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论表(树形)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_discuss
-- ----------------------------
DROP TABLE IF EXISTS `lb_discuss`;
CREATE TABLE `lb_discuss`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '话题ID',
  `user_id` INT NOT NULL COMMENT '发起人ID',
  `title` VARCHAR(30) NOT NULL COMMENT '话题标题',
  `content` TEXT NULL DEFAULT NULL COMMENT '话题内容',
  `media` VARCHAR(255) NULL DEFAULT NULL COMMENT '媒体内容',
  `view_count` INT(11) NULL DEFAULT 0 COMMENT '浏览量',
  `post_count` INT(11) NULL DEFAULT 0 COMMENT '帖子数量',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '话题/讨论表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_favorites
-- ----------------------------
DROP TABLE IF EXISTS `lb_favorites`;
CREATE TABLE `lb_favorites`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `post_id` INT NOT NULL COMMENT '帖子ID',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_favorites_user_post`(`user_id`, `post_id`) USING BTREE,
INDEX `idx_favorites_post_id`(`post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '帖子收藏表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_likes
-- ----------------------------
DROP TABLE IF EXISTS `lb_likes`;
CREATE TABLE `lb_likes`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `post_id` INT NOT NULL COMMENT '帖子ID',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_likes_user_post`(`user_id`, `post_id`) USING BTREE,
INDEX `idx_likes_post_id`(`post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '帖子点赞表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_link
-- ----------------------------
DROP TABLE IF EXISTS `lb_link`;
CREATE TABLE `lb_link`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(10) NULL DEFAULT NULL COMMENT '标题',
  `url` VARCHAR(255) NULL DEFAULT NULL COMMENT '跳转地址',
  `cover_img` VARCHAR(255) NULL DEFAULT NULL COMMENT '封面图',
  `app_id` VARCHAR(100) NULL DEFAULT NULL COMMENT '外部小程序appid',
  `type` INT(1) NULL DEFAULT 1 COMMENT '1当前小程序 2外部小程序 3webview',
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '广告/Banner表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_media_check
-- ----------------------------
DROP TABLE IF EXISTS `lb_media_check`;
CREATE TABLE `lb_media_check`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trace_id` VARCHAR(50) NOT NULL COMMENT '微信trace_id',
  `post_id` INT NOT NULL COMMENT '帖子ID',
  `is_risky` VARCHAR(255) NULL DEFAULT '等待检测结果' COMMENT '风险状态',
  `media_src` VARCHAR(255) NULL DEFAULT NULL COMMENT '媒体地址',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '微信图片审核结果表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_activity_message
-- ----------------------------
DROP TABLE IF EXISTS `lb_activity_message`;
CREATE TABLE `lb_activity_message`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '活动消息ID',
  `title` VARCHAR(100) NOT NULL COMMENT '活动标题',
  `cover` VARCHAR(500) NOT NULL COMMENT '活动封面',
  `summary` VARCHAR(255) NOT NULL COMMENT '摘要',
  `content_type` TINYINT(1) NOT NULL COMMENT '1 URL，2 富文本',
  `url` VARCHAR(500) NULL DEFAULT NULL COMMENT '活动地址',
  `content` LONGTEXT NULL DEFAULT NULL COMMENT '富文本内容',
  `target_type` TINYINT(1) NOT NULL COMMENT '1 全部用户，2 指定用户',
  `target_user_ids` LONGTEXT NULL DEFAULT NULL COMMENT '指定用户ID(JSON)',
  `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0待发送 1已发送 2失败',
  `recipient_count` INT(11) NOT NULL DEFAULT 0 COMMENT '接收人数',
  `failed_reason` VARCHAR(255) NULL DEFAULT NULL COMMENT '失败原因',
  `created_by` INT NOT NULL COMMENT '创建管理员ID',
  `sent_at` DATETIME NULL DEFAULT NULL COMMENT '发送时间',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_created_by`(`created_by`) USING BTREE,
INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '活动消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_message
-- ----------------------------
DROP TABLE IF EXISTS `lb_message`;
CREATE TABLE `lb_message`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id` INT NOT NULL COMMENT '接收人ID',
  `to_user_id` INT NOT NULL COMMENT '发送人ID',
  `post_id` INT NULL DEFAULT NULL COMMENT '帖子ID',
  `activity_message_id` INT NULL DEFAULT NULL COMMENT '活动消息ID',
  `content` VARCHAR(255) NOT NULL COMMENT '消息内容',
  `type` INT(1) NOT NULL COMMENT '1赞 2收藏 3评论 4关注 9系统 10活动',
  `is_read` INT(1) NULL DEFAULT 0 COMMENT '0未读 1已读',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_activity_delivery`(`activity_message_id`, `to_user_id`, `type`) USING BTREE,
INDEX `idx_activity_message_id`(`activity_message_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '站内消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_operation_logs
-- ----------------------------
DROP TABLE IF EXISTS `lb_operation_logs`;
CREATE TABLE `lb_operation_logs`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT NULL DEFAULT NULL COMMENT '用户ID',
  `admin_id` INT NULL DEFAULT NULL COMMENT '管理员ID',
  `user_type` VARCHAR(20) NULL DEFAULT 'guest' COMMENT '访问者类型',
  `method` VARCHAR(10) NULL DEFAULT NULL COMMENT '请求方法',
  `url` VARCHAR(500) NOT NULL COMMENT '请求URI',
  `request_data` TEXT NULL DEFAULT NULL COMMENT '请求参数',
  `response_code` INT(11) NULL DEFAULT NULL COMMENT '响应状态码',
  `is_success` TINYINT(1) NULL DEFAULT 1 COMMENT '是否成功',
  `duration` DECIMAL(10,2) NULL DEFAULT NULL COMMENT '响应时间(毫秒)',
  `ip` VARCHAR(45) NULL DEFAULT NULL COMMENT 'IP地址',
  `user_agent` TEXT NULL DEFAULT NULL COMMENT '用户代理',
  `updated_at` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_is_success`(`is_success`) USING BTREE,
INDEX `idx_created_at`(`created_at`) USING BTREE,
INDEX `idx_user_id`(`user_id`) USING BTREE,
INDEX `idx_admin_id`(`admin_id`) USING BTREE,
INDEX `idx_user_type`(`user_type`) USING BTREE,
INDEX `idx_method`(`method`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_platform_config
-- ----------------------------
DROP TABLE IF EXISTS `lb_platform_config`;
CREATE TABLE `lb_platform_config`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '配置名称',
  `key` VARCHAR(100) NOT NULL COMMENT '配置键名',
  `value` JSON NOT NULL COMMENT '配置值(JSON)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_platform_config`(`key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '平台配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_post
-- ----------------------------
DROP TABLE IF EXISTS `lb_post`;
CREATE TABLE `lb_post`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `category_id` INT NULL DEFAULT NULL COMMENT '类目ID',
  `discuss_id` INT NULL DEFAULT NULL COMMENT '话题ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(20) NULL DEFAULT NULL COMMENT '帖子标题',
  `content` TEXT NULL DEFAULT NULL COMMENT '帖子内容',
  `media` TEXT NULL DEFAULT NULL COMMENT '媒体内容',
  `like_count` INT(11) NULL DEFAULT 0 COMMENT '点赞数量',
  `favorite_count` INT(11) NULL DEFAULT 0 COMMENT '收藏数量',
  `view_count` INT(11) NULL DEFAULT 0 COMMENT '浏览量',
  `comment_count` INT(11) NULL DEFAULT 0 COMMENT '评论量',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_top` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `top_at` DATETIME NULL DEFAULT NULL COMMENT '置顶时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '帖子表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `lb_sensitive_word`;
CREATE TABLE `lb_sensitive_word`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `word` VARCHAR(50) NOT NULL COMMENT '敏感词',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_sensitive_word`(`word`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '敏感词表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_user_auth_tag
-- ----------------------------
DROP TABLE IF EXISTS `lb_user_auth_tag`;
CREATE TABLE `lb_user_auth_tag`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `tag_id` INT NOT NULL COMMENT '标签ID',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_user_auth_tag`(`user_id`, `tag_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户认证标签关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_user_follow
-- ----------------------------
DROP TABLE IF EXISTS `lb_user_follow`;
CREATE TABLE `lb_user_follow`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `follow_user_id` INT NOT NULL COMMENT '关注的用户ID',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_user_follow_relation`(`user_id`, `follow_user_id`) USING BTREE,
INDEX `idx_follow_user_id`(`follow_user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户关注表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_users
-- ----------------------------
DROP TABLE IF EXISTS `lb_users`;
CREATE TABLE `lb_users`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `sn` VARCHAR(15) NOT NULL COMMENT '用户编号',
  `username` VARCHAR(24) NULL DEFAULT NULL COMMENT '用户名',
  `password` VARCHAR(64) NULL DEFAULT NULL COMMENT '密码',
  `nickname` VARCHAR(24) NOT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) NULL DEFAULT NULL COMMENT '头像',
  `phone` VARCHAR(20) NULL DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(20) NULL DEFAULT NULL COMMENT '邮箱',
  `gender` TINYINT(1) NULL DEFAULT 0 COMMENT '性别0未知1男2女',
  `birthday` DATE NULL DEFAULT NULL COMMENT '生日',
  `openid` VARCHAR(100) NULL DEFAULT NULL COMMENT '微信openid',
  `unionid` VARCHAR(100) NULL DEFAULT NULL COMMENT '微信unionid',
  `post_count` INT(11) NULL DEFAULT 0 COMMENT '帖子数量',
  `follow_count` INT(11) NULL DEFAULT 0 COMMENT '关注数量',
  `fans_count` INT(11) NULL DEFAULT 0 COMMENT '粉丝数量',
  `post_thumb_count` INT(11) NULL DEFAULT 0 COMMENT '获赞数量',
  `post_collect_count` INT(11) NULL DEFAULT 0 COMMENT '被收藏总量',
  `introduction` VARCHAR(100) NULL DEFAULT '这人很懒，没有留下什么...' COMMENT '个人简介',
  `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '状态：-1禁用，0正常',
  `last_login_time` DATETIME NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(45) NULL DEFAULT NULL COMMENT '最后登录IP',
  `terminal` TINYINT(1) NULL DEFAULT 1 COMMENT '登录终端1小程序2H53PC4安卓5iOS',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_sn`(`sn`) USING BTREE,
UNIQUE INDEX `uk_phone`(`phone`) USING BTREE,
UNIQUE INDEX `uk_openid`(`openid`) USING BTREE,
UNIQUE INDEX `uk_username`(`username`) USING BTREE,
UNIQUE INDEX `uk_unionid`(`unionid`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for lb_web_top_nav
-- ----------------------------
DROP TABLE IF EXISTS `lb_web_top_nav`;
CREATE TABLE `lb_web_top_nav`  (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '导航ID',
  `title` VARCHAR(12) NOT NULL COMMENT '菜单名称',
  `url` VARCHAR(255) NOT NULL COMMENT '跳转地址',
  `nav_key` VARCHAR(50) NULL DEFAULT NULL COMMENT '导航高亮标识',
  `target` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1当前窗口 2新窗口',
  `sort` INT(11) NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_web_top_nav_status_sort`(`status`, `sort`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'PC Web 顶部导航配置表' ROW_FORMAT = DYNAMIC;

-- =========================================================
-- 初始化数据
-- =========================================================

INSERT INTO `lb_admins_roles` (`id`, `name`, `description`, `status`, `is_founder`, `created_at`, `updated_at`) VALUES
  (1, '超级管理员', '拥有系统所有权限', 1, 1, NOW(), NOW());

INSERT INTO `lb_admins` (`id`, `role_id`, `username`, `password`, `real_name`, `email`, `avatar`, `bio`, `status`, `created_at`, `updated_at`) VALUES
  (1, 1, 'admin', '$2b$10$aRNbw.tIJ2udeqC6kI.SKu1enLPTzXms9PFtKZzWQdsvkZiNvVjua', '超级管理员', NULL, NULL, NULL, 1, NOW(), NOW());

INSERT INTO `lb_admins_permission` (`id`, `parent_id`, `name`, `title`, `icon`, `path`, `component`, `meta`, `sort`, `status`, `type`) VALUES
  (1, 0, 'Dashboard', '仪表盘', 'PieChartOutlined', '/dashboard', 'Dashboard', '{"title":"仪表盘"}', 0, 1, 1),
  (2, 0, 'System', '系统管理', 'SettingOutlined', '#', '#', '{"title":"系统管理"}', 1, 1, 1),
  (3, 0, 'platform', '平台管理', 'DesktopOutlined', '##', '##', '{"title":"平台管理"}', 0, 1, 1),
  (4, 0, 'content', '内容管理', 'ReadOutlined', '##', '##', '{"title":"内容管理"}', 0, 1, 1),
  (5, 0, 'platformConfigs', '配置管理', 'ControlOutlined', '##', '##', '{"title":"配置管理"}', 0, 1, 1),
  (6, 2, 'Permission', '权限管理', 'UserOutlined', '/system/permission', 'system/Permission', '{"title":"权限管理"}', 0, 1, 1),
  (7, 2, 'systemRole', '角色管理', NULL, '/system/role', 'system/Role', '{"title":"角色管理"}', 0, 1, 1),
  (8, 2, 'systemUser', '用户管理', NULL, '/system/user', 'system/User', '{"title":"用户管理"}', 0, 1, 1),
  (9, 2, 'systemLogs', '操作日志', 'BarChartOutlined', '/system/logs', 'system/Logs', '{"title":"操作日志"}', 0, 1, 1),
  (10, 3, 'platformUser', '用户管理', NULL, '/platform/user', 'platform/User', '{"title":"用户管理"}', 0, 1, 1),
  (11, 3, 'platformAuthTag', '认证标签', NULL, '/platform/auth-tag', 'platform/AuthTag', '{"title":"认证标签"}', 1, 1, 1),
  (12, 3, 'platformActivityMessage', '活动消息', NULL, '/platform/activity-message', 'platform/ActivityMessage', '{"title":"活动消息"}', 2, 1, 1),
  (13, 3, 'platformBanner', '广告管理', NULL, '/platform/banner', 'platform/Banner', '{"title":"广告管理"}', 0, 1, 1),
  (14, 3, 'platformAgreement', '协议管理', NULL, '/platform/agreement', 'platform/Agreement', '{"title":"协议管理"}', 0, 1, 1),
  (15, 4, 'contentReply', '评论管理', NULL, '/content/reply', 'content/Reply', '{"title":"评论管理"}', 0, 1, 1),
  (16, 4, 'contentCategory', '类目管理', NULL, '/content/category', 'content/Category', '{"title":"圈子类目"}', 0, 1, 1),
  (17, 4, 'contentPost', '帖子管理', NULL, '/content/post', 'content/Post', '{"title":"帖子管理"}', 0, 1, 1),
  (18, 4, 'contentSensitiveWord', '敏感词', NULL, '/content/sensitive-word', 'content/SensitiveWord', '{"title":"敏感词"}', 2, 1, 1),
  (19, 5, 'config:siteConfig', '站点配置', NULL, '/platform_configs/site_config', 'config/SiteConfig', '{"title":"站点配置"}', 0, 1, 1),
  (20, 5, 'config:aliyunoss', '阿里云OSS', NULL, '/platform_configs/aliyunoss_config', 'config/AliyunOss', '{"title":"阿里云OSS"}', 0, 1, 1),
  (21, 5, 'config:mnpWechat', '微信小程序', NULL, '/platform_configs/mnp_wechat', 'config/MnpWechat', '{"title":"微信小程序"}', 0, 1, 1),
  (22, 5, 'config:emailConfig', '邮件配置', NULL, '/platform_configs/email_config', 'config/EmailConfig', '{"title":"邮件配置"}', 0, 1, 1),
  (23, 5, 'config:webTopNav', '顶部导航', NULL, '/platform_configs/web_top_nav', 'config/WebTopNav', '{"title":"顶部导航"}', 3, 1, 1),
  (24, 5, 'common:ossCredential', '上传凭证', NULL, '/common/AlibabaCloud/credential', '', '{"title":"上传凭证"}', 0, 1, 2);

INSERT INTO `lb_admins_role_permission` (`role_id`, `menu_id`, `created_at`, `updated_at`) VALUES
  (1, 1, NOW(), NOW()),
  (1, 2, NOW(), NOW()),
  (1, 3, NOW(), NOW()),
  (1, 4, NOW(), NOW()),
  (1, 5, NOW(), NOW()),
  (1, 6, NOW(), NOW()),
  (1, 7, NOW(), NOW()),
  (1, 8, NOW(), NOW()),
  (1, 9, NOW(), NOW()),
  (1, 10, NOW(), NOW()),
  (1, 11, NOW(), NOW()),
  (1, 12, NOW(), NOW()),
  (1, 13, NOW(), NOW()),
  (1, 14, NOW(), NOW()),
  (1, 15, NOW(), NOW()),
  (1, 16, NOW(), NOW()),
  (1, 17, NOW(), NOW()),
  (1, 18, NOW(), NOW()),
  (1, 19, NOW(), NOW()),
  (1, 20, NOW(), NOW()),
  (1, 21, NOW(), NOW()),
  (1, 22, NOW(), NOW()),
  (1, 23, NOW(), NOW()),
  (1, 24, NOW(), NOW());

INSERT INTO `lb_platform_config` (`id`, `name`, `key`, `value`, `created_at`, `updated_at`) VALUES
  (1, '微信小程序配置', 'wechat_mnp', '{"token":"","app_id":"","secret":"","aes_key":""}', NOW(), NOW()),
  (2, '阿里云OSS配置', 'aliyun_oss', '{"bucket":"","roleArn":"","regionId":"","accessKeyId":"","accessKeySecret":""}', NOW(), NOW()),
  (3, '站点配置', 'site_settings', '{"icp":"","logo":"/logo.png","title":"zbtech","subtitle":"开源的社区论坛系统","description":"开源的社区论坛系统"}', NOW(), NOW()),
  (4, '阿里云邮件推送配置', 'aliyun_direct_mail', '{"from_alias":"zbtech","ssl_verify":true,"account_name":"hi@zbtech.com","access_key_id":"","reply_to_address":false,"access_key_secret":""}', NOW(), NOW());

INSERT INTO `lb_auth_tag` (`id`, `name`, `color`, `sort`, `status`, `created_at`, `updated_at`) VALUES
  (1, '官方', '#1677ff', 0, 1, NOW(), NOW());

INSERT INTO `lb_web_top_nav` (`id`, `title`, `url`, `nav_key`, `target`, `sort`, `status`, `created_at`, `updated_at`) VALUES
  (1, '首页', '/', 'home', 1, 0, 1, NOW(), NOW()),
  (2, '话题', '/discuss', 'discuss', 1, 1, 1, NOW(), NOW()),
  (3, '文档', 'https://www.zbtech.com/', NULL, 1, 2, 1, NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;
