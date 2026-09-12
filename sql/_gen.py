# -*- coding: utf-8 -*-
"""
zbtech community 代码生成器
- 基于 foxbook 数据库结构翻译为 Spring Boot 3.0 (MyBatis-Plus)
- 表前缀 fox_ -> lb_，主键 char(36) UUID -> INT AUTO_INCREMENT
- 关键字 foxbook -> zbtech
生成：实体 / Mapper / Service / ServiceImpl / adminapi 通用 CRUD 控制器 + 建表SQL(含种子)
"""
import os

# 占位常量：种子数据里表示 SQL NULL
NULL = None


def q(v):
    """SQL 字符串安全包裹：None->NULL，其余加单引号并转义单引号"""
    return "NULL" if v is None else "'%s'" % str(v).replace("'", "''")


BASE = "F:/zslwork/test/community"
JAVA = os.path.join(BASE, "src/main/java/com/zbtech/community")
SQL_OUT = os.path.join(BASE, "sql/lb_community.sql")

PKG = "com.zbtech.community"

# 预留字列（需要反引号包裹）
RESERVED = {"key", "value"}

# ---- 每张表的列定义 ----
# 列: (name, jtype, mysql_type, comment, pk)
# mysql_type 为列类型定义（不含列名与反引号），含 NULL/DEFAULT 等
def col(name, jtype, mysql, comment="", pk=False):
    return {"n": name, "j": jtype, "m": mysql, "c": comment, "pk": pk}

# 表定义: (table_snake, class_name, comment, resource_path, columns, indexes)
TABLES = [
    ("admins", "Admins", "管理员表", "admins", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "管理员ID", pk=True),
        col("role_id", "Integer", "INT NOT NULL", "角色ID"),
        col("username", "String", "VARCHAR(32) NOT NULL", "用户名"),
        col("password", "String", "VARCHAR(255) NOT NULL", "密码"),
        col("real_name", "String", "VARCHAR(100) NOT NULL", "姓名"),
        col("email", "String", "VARCHAR(100) NULL DEFAULT NULL", "邮箱"),
        col("avatar", "String", "VARCHAR(255) NULL DEFAULT NULL", "头像"),
        col("bio", "String", "VARCHAR(255) NULL DEFAULT NULL", "个人简介"),
        col("status", "Integer", "TINYINT(1) NULL DEFAULT 1", "状态：1启用，0禁用"),
        col("last_login_time", "LocalDateTime", "DATETIME NULL DEFAULT NULL", "最后登录时间"),
        col("last_login_ip", "String", "VARCHAR(45) NULL DEFAULT NULL", "最后登录IP"),
        col("created_at", "LocalDateTime", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        col("updated_at", "LocalDateTime", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
    ], [
        "UNIQUE INDEX `uk_username`(`username`) USING BTREE",
    ]),

    ("admins_permission", "AdminsPermission", "菜单/接口权限表", "permission", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "菜单ID", pk=True),
        col("parent_id", "Integer", "INT NULL DEFAULT 0", "父级菜单ID"),
        col("name", "String", "VARCHAR(100) NOT NULL", "菜单标识"),
        col("title", "String", "VARCHAR(6) NULL DEFAULT NULL", "菜单标题"),
        col("icon", "String", "VARCHAR(50) NULL DEFAULT NULL", "菜单图标"),
        col("path", "String", "VARCHAR(255) NULL DEFAULT NULL", "菜单路径或API路径"),
        col("component", "String", "VARCHAR(255) NULL DEFAULT NULL", "组件路径"),
        col("meta", "String", "VARCHAR(255) NULL DEFAULT NULL", "meta(JSON)"),
        col("sort", "Integer", "INT(11) NULL DEFAULT 0", "排序"),
        col("status", "Integer", "TINYINT(1) NULL DEFAULT 1", "状态：1启用，0禁用"),
        col("type", "Integer", "TINYINT(1) NULL DEFAULT 1", "1 菜单，2接口"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
    ], [
        "INDEX `idx_parent_id`(`parent_id`) USING BTREE",
    ]),

    ("admins_role_permission", "AdminsRolePermission", "角色菜单关联表", "role_permission", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "主键ID", pk=True),
        col("role_id", "Integer", "INT NOT NULL", "角色ID"),
        col("menu_id", "Integer", "INT NOT NULL", "菜单ID"),
        col("created_at", "LocalDateTime", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        col("updated_at", "LocalDateTime", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
    ], [
        "UNIQUE INDEX `uk_role_menu`(`role_id`, `menu_id`) USING BTREE",
        "INDEX `idx_role_id`(`role_id`) USING BTREE",
        "INDEX `idx_menu_id`(`menu_id`) USING BTREE",
    ]),

    ("admins_roles", "AdminsRoles", "角色表", "roles", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "角色ID", pk=True),
        col("name", "String", "VARCHAR(50) NOT NULL", "角色名称"),
        col("description", "String", "VARCHAR(255) NULL DEFAULT NULL", "角色描述"),
        col("status", "Integer", "TINYINT(1) NULL DEFAULT 1", "状态：1启用，0禁用"),
        col("is_founder", "Integer", "TINYINT(1) NULL DEFAULT 0", "是否创建人：1 创建人，拥有系统所有权限"),
        col("created_at", "LocalDateTime", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        col("updated_at", "LocalDateTime", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
    ], []),

    ("agreement", "Agreement", "协议表", "agreement", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "协议ID", pk=True),
        col("title", "String", "VARCHAR(50) NOT NULL", "协议标题"),
        col("content", "String", "LONGTEXT NOT NULL", "协议内容(HTML)"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT NULL", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT NULL", "创建时间"),
    ], []),

    ("auth_tag", "AuthTag", "认证标签表", "auth_tag", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "标签ID", pk=True),
        col("name", "String", "VARCHAR(20) NOT NULL", "标签名称"),
        col("color", "String", "VARCHAR(20) NOT NULL DEFAULT '#1677ff'", "标签颜色"),
        col("sort", "Integer", "INT(11) NOT NULL DEFAULT 0", "排序"),
        col("status", "Integer", "TINYINT(1) NOT NULL DEFAULT 1", "状态"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
    ], [
        "UNIQUE INDEX `uk_auth_tag_name`(`name`) USING BTREE",
    ]),

    ("category", "Category", "圈子类目表", "category", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "类目ID", pk=True),
        col("name", "String", "VARCHAR(10) NOT NULL", "类目名称"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], []),

    ("comment_likes", "CommentLikes", "评论点赞表", "comment_likes", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "主键ID", pk=True),
        col("user_id", "Integer", "INT NOT NULL", "用户ID"),
        col("comment_id", "Integer", "INT NOT NULL", "评论ID"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], [
        "UNIQUE INDEX `uk_comment_likes_user_comment`(`user_id`, `comment_id`) USING BTREE",
        "INDEX `idx_comment_likes_comment_id`(`comment_id`) USING BTREE",
    ]),

    ("comments", "Comments", "评论表(树形)", "comment", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "评论ID", pk=True),
        col("parent_id", "Integer", "INT NULL DEFAULT 0", "父评论ID"),
        col("root_id", "Integer", "INT NULL DEFAULT 0", "根评论ID"),
        col("user_id", "Integer", "INT NOT NULL", "用户ID"),
        col("to_user_id", "Integer", "INT NOT NULL", "回复目标用户ID"),
        col("post_id", "Integer", "INT NOT NULL", "帖子ID"),
        col("content", "String", "VARCHAR(255) NOT NULL", "评论内容"),
        col("like_count", "Integer", "INT(11) NULL DEFAULT 0", "点赞数量"),
        col("reply_count", "Integer", "INT(11) NULL DEFAULT 0", "回复数量"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], [
        "INDEX `idx_comments_post_parent`(`post_id`, `parent_id`) USING BTREE",
        "INDEX `idx_comments_root_id`(`root_id`) USING BTREE",
        "INDEX `idx_comments_parent_id`(`parent_id`) USING BTREE",
    ]),

    ("discuss", "Discuss", "话题/讨论表", "discuss", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "话题ID", pk=True),
        col("user_id", "Integer", "INT NOT NULL", "发起人ID"),
        col("title", "String", "VARCHAR(30) NOT NULL", "话题标题"),
        col("content", "String", "TEXT NULL DEFAULT NULL", "话题内容"),
        col("media", "String", "VARCHAR(255) NULL DEFAULT NULL", "媒体内容"),
        col("view_count", "Integer", "INT(11) NULL DEFAULT 0", "浏览量"),
        col("post_count", "Integer", "INT(11) NULL DEFAULT 0", "帖子数量"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], []),

    ("favorites", "Favorites", "帖子收藏表", "favorites", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "主键ID", pk=True),
        col("user_id", "Integer", "INT NOT NULL", "用户ID"),
        col("post_id", "Integer", "INT NOT NULL", "帖子ID"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], [
        "UNIQUE INDEX `uk_favorites_user_post`(`user_id`, `post_id`) USING BTREE",
        "INDEX `idx_favorites_post_id`(`post_id`) USING BTREE",
    ]),

    ("likes", "Likes", "帖子点赞表", "likes", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "主键ID", pk=True),
        col("user_id", "Integer", "INT NOT NULL", "用户ID"),
        col("post_id", "Integer", "INT NOT NULL", "帖子ID"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], [
        "UNIQUE INDEX `uk_likes_user_post`(`user_id`, `post_id`) USING BTREE",
        "INDEX `idx_likes_post_id`(`post_id`) USING BTREE",
    ]),

    ("link", "Link", "广告/Banner表", "link", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "主键ID", pk=True),
        col("title", "String", "VARCHAR(10) NULL DEFAULT NULL", "标题"),
        col("url", "String", "VARCHAR(255) NULL DEFAULT NULL", "跳转地址"),
        col("cover_img", "String", "VARCHAR(255) NULL DEFAULT NULL", "封面图"),
        col("app_id", "String", "VARCHAR(100) NULL DEFAULT NULL", "外部小程序appid"),
        col("type", "Integer", "INT(1) NULL DEFAULT 1", "1当前小程序 2外部小程序 3webview"),
        col("updated_at", "LocalDateTime", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], []),

    ("media_check", "MediaCheck", "微信图片审核结果表", "media_check", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "主键ID", pk=True),
        col("trace_id", "String", "VARCHAR(50) NOT NULL", "微信trace_id"),
        col("post_id", "Integer", "INT NOT NULL", "帖子ID"),
        col("is_risky", "String", "VARCHAR(255) NULL DEFAULT '等待检测结果'", "风险状态"),
        col("media_src", "String", "VARCHAR(255) NULL DEFAULT NULL", "媒体地址"),
        col("created_at", "LocalDateTime", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        col("updated_at", "LocalDateTime", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
    ], []),

    ("activity_message", "ActivityMessage", "活动消息表", "activity_message", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "活动消息ID", pk=True),
        col("title", "String", "VARCHAR(100) NOT NULL", "活动标题"),
        col("cover", "String", "VARCHAR(500) NOT NULL", "活动封面"),
        col("summary", "String", "VARCHAR(255) NOT NULL", "摘要"),
        col("content_type", "Integer", "TINYINT(1) NOT NULL", "1 URL，2 富文本"),
        col("url", "String", "VARCHAR(500) NULL DEFAULT NULL", "活动地址"),
        col("content", "String", "LONGTEXT NULL DEFAULT NULL", "富文本内容"),
        col("target_type", "Integer", "TINYINT(1) NOT NULL", "1 全部用户，2 指定用户"),
        col("target_user_ids", "String", "LONGTEXT NULL DEFAULT NULL", "指定用户ID(JSON)"),
        col("status", "Integer", "TINYINT(1) NOT NULL DEFAULT 0", "0待发送 1已发送 2失败"),
        col("recipient_count", "Integer", "INT(11) NOT NULL DEFAULT 0", "接收人数"),
        col("failed_reason", "String", "VARCHAR(255) NULL DEFAULT NULL", "失败原因"),
        col("created_by", "Integer", "INT NOT NULL", "创建管理员ID"),
        col("sent_at", "LocalDateTime", "DATETIME NULL DEFAULT NULL", "发送时间"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], [
        "INDEX `idx_created_by`(`created_by`) USING BTREE",
        "INDEX `idx_status`(`status`) USING BTREE",
    ]),

    ("message", "Message", "站内消息表", "message", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "消息ID", pk=True),
        col("user_id", "Integer", "INT NOT NULL", "接收人ID"),
        col("to_user_id", "Integer", "INT NOT NULL", "发送人ID"),
        col("post_id", "Integer", "INT NULL DEFAULT NULL", "帖子ID"),
        col("activity_message_id", "Integer", "INT NULL DEFAULT NULL", "活动消息ID"),
        col("content", "String", "VARCHAR(255) NOT NULL", "消息内容"),
        col("type", "Integer", "INT(1) NOT NULL", "1赞 2收藏 3评论 4关注 9系统 10活动"),
        col("is_read", "Integer", "INT(1) NULL DEFAULT 0", "0未读 1已读"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], [
        "UNIQUE INDEX `uk_activity_delivery`(`activity_message_id`, `to_user_id`, `type`) USING BTREE",
        "INDEX `idx_activity_message_id`(`activity_message_id`) USING BTREE",
    ]),

    ("operation_logs", "OperationLogs", "操作日志表", "operation_log", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "主键ID", pk=True),
        col("user_id", "Integer", "INT NULL DEFAULT NULL", "用户ID"),
        col("admin_id", "Integer", "INT NULL DEFAULT NULL", "管理员ID"),
        col("user_type", "String", "VARCHAR(20) NULL DEFAULT 'guest'", "访问者类型"),
        col("method", "String", "VARCHAR(10) NULL DEFAULT NULL", "请求方法"),
        col("url", "String", "VARCHAR(500) NOT NULL", "请求URI"),
        col("request_data", "String", "TEXT NULL DEFAULT NULL", "请求参数"),
        col("response_code", "Integer", "INT(11) NULL DEFAULT NULL", "响应状态码"),
        col("is_success", "Integer", "TINYINT(1) NULL DEFAULT 1", "是否成功"),
        col("duration", "java.math.BigDecimal", "DECIMAL(10,2) NULL DEFAULT NULL", "响应时间(毫秒)"),
        col("ip", "String", "VARCHAR(45) NULL DEFAULT NULL", "IP地址"),
        col("user_agent", "String", "TEXT NULL DEFAULT NULL", "用户代理"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT NULL", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT NULL", "创建时间"),
    ], [
        "INDEX `idx_is_success`(`is_success`) USING BTREE",
        "INDEX `idx_created_at`(`created_at`) USING BTREE",
        "INDEX `idx_user_id`(`user_id`) USING BTREE",
        "INDEX `idx_admin_id`(`admin_id`) USING BTREE",
        "INDEX `idx_user_type`(`user_type`) USING BTREE",
        "INDEX `idx_method`(`method`) USING BTREE",
    ]),

    ("platform_config", "PlatformConfig", "平台配置表", "platform_config", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "主键ID", pk=True),
        col("name", "String", "VARCHAR(100) NOT NULL", "配置名称"),
        col("key", "String", "VARCHAR(100) NOT NULL", "配置键名"),
        col("value", "String", "JSON NOT NULL", "配置值(JSON)"),
        col("created_at", "LocalDateTime", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        col("updated_at", "LocalDateTime", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
    ], [
        "UNIQUE INDEX `uk_platform_config`(`key`) USING BTREE",
    ]),

    ("post", "Post", "帖子表", "post", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "帖子ID", pk=True),
        col("category_id", "Integer", "INT NULL DEFAULT NULL", "类目ID"),
        col("discuss_id", "Integer", "INT NULL DEFAULT NULL", "话题ID"),
        col("user_id", "Integer", "INT NOT NULL", "用户ID"),
        col("title", "String", "VARCHAR(20) NULL DEFAULT NULL", "帖子标题"),
        col("content", "String", "TEXT NULL DEFAULT NULL", "帖子内容"),
        col("media", "String", "TEXT NULL DEFAULT NULL", "媒体内容"),
        col("like_count", "Integer", "INT(11) NULL DEFAULT 0", "点赞数量"),
        col("favorite_count", "Integer", "INT(11) NULL DEFAULT 0", "收藏数量"),
        col("view_count", "Integer", "INT(11) NULL DEFAULT 0", "浏览量"),
        col("comment_count", "Integer", "INT(11) NULL DEFAULT 0", "评论量"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        col("is_top", "Integer", "TINYINT(1) NOT NULL DEFAULT 0", "是否置顶"),
        col("top_at", "LocalDateTime", "DATETIME NULL DEFAULT NULL", "置顶时间"),
    ], []),

    ("sensitive_word", "SensitiveWord", "敏感词表", "sensitive_word", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "主键ID", pk=True),
        col("word", "String", "VARCHAR(50) NOT NULL", "敏感词"),
        col("status", "Integer", "TINYINT(1) NOT NULL DEFAULT 1", "状态"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
    ], [
        "UNIQUE INDEX `uk_sensitive_word`(`word`) USING BTREE",
    ]),

    ("user_auth_tag", "UserAuthTag", "用户认证标签关联表", "user_auth_tag", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "主键ID", pk=True),
        col("user_id", "Integer", "INT NOT NULL", "用户ID"),
        col("tag_id", "Integer", "INT NOT NULL", "标签ID"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
    ], [
        "UNIQUE INDEX `uk_user_auth_tag`(`user_id`, `tag_id`) USING BTREE",
    ]),

    ("user_follow", "UserFollow", "用户关注表", "user_follow", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "主键ID", pk=True),
        col("user_id", "Integer", "INT NOT NULL", "用户ID"),
        col("follow_user_id", "Integer", "INT NOT NULL", "关注的用户ID"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], [
        "UNIQUE INDEX `uk_user_follow_relation`(`user_id`, `follow_user_id`) USING BTREE",
        "INDEX `idx_follow_user_id`(`follow_user_id`) USING BTREE",
    ]),

    ("users", "Users", "用户表", "user", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "用户ID", pk=True),
        col("sn", "String", "VARCHAR(15) NOT NULL", "用户编号"),
        col("username", "String", "VARCHAR(24) NULL DEFAULT NULL", "用户名"),
        col("password", "String", "VARCHAR(64) NULL DEFAULT NULL", "密码"),
        col("nickname", "String", "VARCHAR(24) NOT NULL", "昵称"),
        col("avatar", "String", "VARCHAR(255) NULL DEFAULT NULL", "头像"),
        col("phone", "String", "VARCHAR(20) NULL DEFAULT NULL", "手机号"),
        col("email", "String", "VARCHAR(20) NULL DEFAULT NULL", "邮箱"),
        col("gender", "Integer", "TINYINT(1) NULL DEFAULT 0", "性别0未知1男2女"),
        col("birthday", "java.time.LocalDate", "DATE NULL DEFAULT NULL", "生日"),
        col("openid", "String", "VARCHAR(100) NULL DEFAULT NULL", "微信openid"),
        col("unionid", "String", "VARCHAR(100) NULL DEFAULT NULL", "微信unionid"),
        col("post_count", "Integer", "INT(11) NULL DEFAULT 0", "帖子数量"),
        col("follow_count", "Integer", "INT(11) NULL DEFAULT 0", "关注数量"),
        col("fans_count", "Integer", "INT(11) NULL DEFAULT 0", "粉丝数量"),
        col("post_thumb_count", "Integer", "INT(11) NULL DEFAULT 0", "获赞数量"),
        col("post_collect_count", "Integer", "INT(11) NULL DEFAULT 0", "被收藏总量"),
        col("introduction", "String", "VARCHAR(100) NULL DEFAULT '这人很懒，没有留下什么...'", "个人简介"),
        col("status", "Integer", "TINYINT(1) NOT NULL DEFAULT 0", "状态：-1禁用，0正常"),
        col("last_login_time", "LocalDateTime", "DATETIME NULL DEFAULT NULL", "最后登录时间"),
        col("last_login_ip", "String", "VARCHAR(45) NULL DEFAULT NULL", "最后登录IP"),
        col("terminal", "Integer", "TINYINT(1) NULL DEFAULT 1", "登录终端1小程序2H53PC4安卓5iOS"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], [
        "UNIQUE INDEX `uk_sn`(`sn`) USING BTREE",
        "UNIQUE INDEX `uk_phone`(`phone`) USING BTREE",
        "UNIQUE INDEX `uk_openid`(`openid`) USING BTREE",
        "UNIQUE INDEX `uk_username`(`username`) USING BTREE",
        "UNIQUE INDEX `uk_unionid`(`unionid`) USING BTREE",
    ]),

    ("web_top_nav", "WebTopNav", "PC Web 顶部导航配置表", "web_top_nav", [
        col("id", "Integer", "INT NOT NULL AUTO_INCREMENT", "导航ID", pk=True),
        col("title", "String", "VARCHAR(12) NOT NULL", "菜单名称"),
        col("url", "String", "VARCHAR(255) NOT NULL", "跳转地址"),
        col("nav_key", "String", "VARCHAR(50) NULL DEFAULT NULL", "导航高亮标识"),
        col("target", "Integer", "TINYINT(1) NOT NULL DEFAULT 1", "1当前窗口 2新窗口"),
        col("sort", "Integer", "INT(11) NOT NULL DEFAULT 0", "排序"),
        col("status", "Integer", "TINYINT(1) NOT NULL DEFAULT 1", "1显示 0隐藏"),
        col("updated_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        col("created_at", "LocalDateTime", "DATETIME NULL DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ], [
        "INDEX `idx_web_top_nav_status_sort`(`status`, `sort`) USING BTREE",
    ]),
]

# ---------------- Java 代码模板（用 replace 避免花括号转义问题） ----------------
ENTITY_TPL = """package {PKG}.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
{IMPORTS}

/**
 * {COMMENT}
 */
@Data
@TableName("lb_{TABLE}")
public class {CLASS} {{

{FIELDS}
}}
"""

MAPPER_TPL = """package {PKG}.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import {PKG}.entity.{CLASS};
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface {CLASS}Mapper extends BaseMapper<{CLASS}> {{
}}
"""

SERVICE_TPL = """package {PKG}.service;

import com.baomidou.mybatisplus.extension.service.IService;
import {PKG}.entity.{CLASS};

public interface {CLASS}Service extends IService<{CLASS}> {{
}}
"""

SERVICE_IMPL_TPL = """package {PKG}.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import {PKG}.entity.{CLASS};
import {PKG}.mapper.{CLASS}Mapper;
import {PKG}.service.{CLASS}Service;
import org.springframework.stereotype.Service;

@Service
public class {CLASS}ServiceImpl extends ServiceImpl<{CLASS}Mapper, {CLASS}> implements {CLASS}Service {{
}}
"""

CONTROLLER_TPL = """package {PKG}.controller.adminapi;

import {PKG}.common.BaseController;
import {PKG}.common.Result;
import {PKG}.entity.{CLASS};
import {PKG}.service.{CLASS}Service;
import com.baomidou.mybatisplus.extension.service.IService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * {COMMENT} - 管理端通用 CRUD
 */
@RestController
@RequestMapping("/adminapi/{RESOURCE}")
@Tag(name = "{COMMENT}")
public class {CLASS}AdminController extends BaseController {{

    @Autowired
    protected {CLASS}Service service;

    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize) {{
        return pageResult(service.page(toPage(page, pageSize)));
    }}

    @GetMapping("/getList")
    public Result<?> getList() {{
        return Result.success(service.list());
    }}

    @GetMapping("/getDetail")
    public Result<?> getDetail(@RequestParam Integer id) {{
        return Result.success(service.getById(id));
    }}

    @PostMapping("/save")
    public Result<?> save(@RequestBody {CLASS} entity) {{
        service.saveOrUpdate(entity);
        return Result.success();
    }}

    @PostMapping("/delete")
    public Result<?> delete(@RequestParam Integer id) {{
        service.removeById(id);
        return Result.success();
    }}

    @PostMapping("/batchDelete")
    public Result<?> batchDelete(@RequestBody java.util.List<Integer> ids) {{
        service.removeByIds(ids);
        return Result.success();
    }}
}}
"""


def java_imports(cols):
    imp = set()
    for c in cols:
        if c["j"] == "LocalDateTime":
            imp.add("import java.time.LocalDateTime;")
        elif c["j"] == "java.time.LocalDate":
            imp.add("import java.time.LocalDate;")
        elif c["j"] == "java.math.BigDecimal":
            imp.add("import java.math.BigDecimal;")
    return "\n".join(sorted(imp))


def java_fields(cols):
    lines = []
    for c in cols:
        jt = c["j"].split(".")[-1]  # 去掉包名前缀
        comment = (" // " + c["c"]) if c["c"] else ""
        if c["pk"]:
            lines.append("    @TableId(type = IdType.AUTO)\n    private Integer id;" + comment)
        elif c["n"] in RESERVED:
            lines.append('    @TableField("`%s`")\n    private %s %s;%s' % (c["n"], jt, c["n"], comment))
        else:
            lines.append("    private %s %s;%s" % (jt, c["n"], comment))
    return "\n".join(lines)


def gen_java():
    for (table, cls, comment, resource, cols, idx) in TABLES:
        fields = java_fields(cols)
        imports = java_imports(cols)
        kv = {"PKG": PKG, "TABLE": table, "CLASS": cls, "COMMENT": comment,
              "RESOURCE": resource, "FIELDS": fields, "IMPORTS": imports}

        entity = ENTITY_TPL.replace("{PKG}", PKG).replace("{TABLE}", table).replace("{CLASS}", cls) \
            .replace("{COMMENT}", comment).replace("{FIELDS}", fields).replace("{IMPORTS}", imports)
        entity = entity.replace("{{", "{").replace("}}", "}")
        write_file(os.path.join(JAVA, "entity", cls + ".java"), entity)

        mapper = MAPPER_TPL.replace("{PKG}", PKG).replace("{CLASS}", cls).replace("{{", "{").replace("}}", "}")
        write_file(os.path.join(JAVA, "mapper", cls + "Mapper.java"), mapper)

        service = SERVICE_TPL.replace("{PKG}", PKG).replace("{CLASS}", cls).replace("{{", "{").replace("}}", "}")
        write_file(os.path.join(JAVA, "service", cls + "Service.java"), service)

        impl = SERVICE_IMPL_TPL.replace("{PKG}", PKG).replace("{CLASS}", cls).replace("{{", "{").replace("}}", "}")
        write_file(os.path.join(JAVA, "service/impl", cls + "ServiceImpl.java"), impl)

        ctrl = CONTROLLER_TPL.replace("{PKG}", PKG).replace("{CLASS}", cls) \
            .replace("{COMMENT}", comment).replace("{RESOURCE}", resource) \
            .replace("{{", "{").replace("}}", "}")
        write_file(os.path.join(JAVA, "controller/adminapi", cls + "AdminController.java"), ctrl)


def ddl_columns(cols):
    out = []
    for c in cols:
        name = c["n"]
        line = "  `%s` %s" % (name, c["m"])
        if c["c"]:
            line += " COMMENT '%s'" % c["c"].replace("'", "''")
        out.append(line)
    return out


def gen_ddl(admin_password_hash):
    parts = []
    parts.append("-- =========================================================")
    parts.append("-- zbtech community 建表 + 初始化数据")
    parts.append("-- 由 foxbook 翻译而来：表前缀 lb_，主键 INT 自增，foxbook->zbtech")
    parts.append("-- MySQL 5.7+ (utf8mb4)")
    parts.append("-- =========================================================")
    parts.append("")
    parts.append("SET NAMES utf8mb4;")
    parts.append("SET FOREIGN_KEY_CHECKS = 0;")
    parts.append("")
    for (table, cls, comment, resource, cols, idx) in TABLES:
        parts.append("-- ----------------------------")
        parts.append("-- Table structure for lb_%s" % table)
        parts.append("-- ----------------------------")
        parts.append("DROP TABLE IF EXISTS `lb_%s`;" % table)
        col_lines = ddl_columns(cols)
        col_sql = ",\n".join(col_lines)
        idx_sql = ""
        if idx:
            idx_sql = ",\n  " + ",\n  ".join(idx)
        pk_line = "  PRIMARY KEY (`id`) USING BTREE"
        body = col_sql
        if idx_sql:
            body = col_sql + ",\n  " + idx_sql.lstrip(",\n  ")
        # 重新组织：列 + 主键 + 索引
        all_lines = col_lines + [pk_line] + idx
        body = ",\n".join(all_lines)
        create = ("CREATE TABLE `lb_%s`  (\n%s\n) ENGINE = InnoDB CHARACTER SET = utf8mb4 "
                  "COLLATE = utf8mb4_general_ci COMMENT = '%s' ROW_FORMAT = DYNAMIC;" % (
                      table, body, comment.replace("'", "''")))
        parts.append(create)
        parts.append("")

    # ---------------- 种子数据（整数化，foxbook->zbtech） ----------------
    parts.append("-- =========================================================")
    parts.append("-- 初始化数据")
    parts.append("-- =========================================================")
    parts.append("")
    parts.append("INSERT INTO `lb_admins_roles` (`id`, `name`, `description`, `status`, `is_founder`, `created_at`, `updated_at`) VALUES")
    parts.append("  (1, '超级管理员', '拥有系统所有权限', 1, 1, NOW(), NOW());")
    parts.append("")
    parts.append("INSERT INTO `lb_admins` (`id`, `role_id`, `username`, `password`, `real_name`, `email`, `avatar`, `bio`, `status`, `created_at`, `updated_at`) VALUES")
    parts.append("  (1, 1, 'admin', '%s', '超级管理员', NULL, NULL, NULL, 1, NOW(), NOW());" % admin_password_hash)
    parts.append("")

    # 菜单树
    menus = [
        (1, 0, 'Dashboard', '仪表盘', 'PieChartOutlined', '/dashboard', 'Dashboard', '{\"title\":\"仪表盘\"}', 0, 1, 1),
        (2, 0, 'System', '系统管理', 'SettingOutlined', '#', '#', '{\"title\":\"系统管理\"}', 1, 1, 1),
        (3, 0, 'platform', '平台管理', 'DesktopOutlined', '##', '##', '{\"title\":\"平台管理\"}', 0, 1, 1),
        (4, 0, 'content', '内容管理', 'ReadOutlined', '##', '##', '{\"title\":\"内容管理\"}', 0, 1, 1),
        (5, 0, 'platformConfigs', '配置管理', 'ControlOutlined', '##', '##', '{\"title\":\"配置管理\"}', 0, 1, 1),
        (6, 2, 'Permission', '权限管理', 'UserOutlined', '/system/permission', 'system/Permission', '{\"title\":\"权限管理\"}', 0, 1, 1),
        (7, 2, 'systemRole', '角色管理', NULL, '/system/role', 'system/Role', '{\"title\":\"角色管理\"}', 0, 1, 1),
        (8, 2, 'systemUser', '用户管理', NULL, '/system/user', 'system/User', '{\"title\":\"用户管理\"}', 0, 1, 1),
        (9, 2, 'systemLogs', '操作日志', 'BarChartOutlined', '/system/logs', 'system/Logs', '{\"title\":\"操作日志\"}', 0, 1, 1),
        (10, 3, 'platformUser', '用户管理', NULL, '/platform/user', 'platform/User', '{\"title\":\"用户管理\"}', 0, 1, 1),
        (11, 3, 'platformAuthTag', '认证标签', NULL, '/platform/auth-tag', 'platform/AuthTag', '{\"title\":\"认证标签\"}', 1, 1, 1),
        (12, 3, 'platformActivityMessage', '活动消息', NULL, '/platform/activity-message', 'platform/ActivityMessage', '{\"title\":\"活动消息\"}', 2, 1, 1),
        (13, 3, 'platformBanner', '广告管理', NULL, '/platform/banner', 'platform/Banner', '{\"title\":\"广告管理\"}', 0, 1, 1),
        (14, 3, 'platformAgreement', '协议管理', NULL, '/platform/agreement', 'platform/Agreement', '{\"title\":\"协议管理\"}', 0, 1, 1),
        (15, 4, 'contentReply', '评论管理', NULL, '/content/reply', 'content/Reply', '{\"title\":\"评论管理\"}', 0, 1, 1),
        (16, 4, 'contentCategory', '类目管理', NULL, '/content/category', 'content/Category', '{\"title\":\"圈子类目\"}', 0, 1, 1),
        (17, 4, 'contentPost', '帖子管理', NULL, '/content/post', 'content/Post', '{\"title\":\"帖子管理\"}', 0, 1, 1),
        (18, 4, 'contentSensitiveWord', '敏感词', NULL, '/content/sensitive-word', 'content/SensitiveWord', '{\"title\":\"敏感词\"}', 2, 1, 1),
        (19, 5, 'config:siteConfig', '站点配置', NULL, '/platform_configs/site_config', 'config/SiteConfig', '{\"title\":\"站点配置\"}', 0, 1, 1),
        (20, 5, 'config:aliyunoss', '阿里云OSS', NULL, '/platform_configs/aliyunoss_config', 'config/AliyunOss', '{\"title\":\"阿里云OSS\"}', 0, 1, 1),
        (21, 5, 'config:mnpWechat', '微信小程序', NULL, '/platform_configs/mnp_wechat', 'config/MnpWechat', '{\"title\":\"微信小程序\"}', 0, 1, 1),
        (22, 5, 'config:emailConfig', '邮件配置', NULL, '/platform_configs/email_config', 'config/EmailConfig', '{\"title\":\"邮件配置\"}', 0, 1, 1),
        (23, 5, 'config:webTopNav', '顶部导航', NULL, '/platform_configs/web_top_nav', 'config/WebTopNav', '{\"title\":\"顶部导航\"}', 3, 1, 1),
        (24, 5, 'common:ossCredential', '上传凭证', NULL, '/common/AlibabaCloud/credential', '', '{\"title\":\"上传凭证\"}', 0, 1, 2),
    ]
    parts.append("INSERT INTO `lb_admins_permission` (`id`, `parent_id`, `name`, `title`, `icon`, `path`, `component`, `meta`, `sort`, `status`, `type`) VALUES")
    menu_rows = []
    for m in menus:
        menu_rows.append("  (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)" % (
            m[0], m[1], q(m[2]), q(m[3]), q(m[4]), q(m[5]), q(m[6]), q(m[7]), m[8], m[9], m[10]))
    parts.append(",\n".join(menu_rows) + ";")
    parts.append("")

    # 角色-菜单全部授权
    all_ids = ", ".join(str(m[0]) for m in menus)
    parts.append("INSERT INTO `lb_admins_role_permission` (`role_id`, `menu_id`, `created_at`, `updated_at`) VALUES")
    rp_rows = []
    for m in menus:
        rp_rows.append("  (1, %s, NOW(), NOW())" % m[0])
    parts.append(",\n".join(rp_rows) + ";")
    parts.append("")

    # 平台配置
    cfg = [
        (1, '微信小程序配置', 'wechat_mnp', '{\"token\":\"\",\"app_id\":\"\",\"secret\":\"\",\"aes_key\":\"\"}'),
        (2, '阿里云OSS配置', 'aliyun_oss', '{\"bucket\":\"\",\"roleArn\":\"\",\"regionId\":\"\",\"accessKeyId\":\"\",\"accessKeySecret\":\"\"}'),
        (3, '站点配置', 'site_settings', '{\"icp\":\"\",\"logo\":\"/logo.png\",\"title\":\"zbtech\",\"subtitle\":\"开源的社区论坛系统\",\"description\":\"开源的社区论坛系统\"}'),
        (4, '阿里云邮件推送配置', 'aliyun_direct_mail', '{\"from_alias\":\"zbtech\",\"ssl_verify\":true,\"account_name\":\"hi@zbtech.com\",\"access_key_id\":\"\",\"reply_to_address\":false,\"access_key_secret\":\"\"}'),
    ]
    parts.append("INSERT INTO `lb_platform_config` (`id`, `name`, `key`, `value`, `created_at`, `updated_at`) VALUES")
    cfg_rows = []
    for c in cfg:
        cfg_rows.append("  (%s, '%s', '%s', '%s', NOW(), NOW())" % (c[0], c[1], c[2], c[3].replace("'", "''")))
    parts.append(",\n".join(cfg_rows) + ";")
    parts.append("")

    # 认证标签
    parts.append("INSERT INTO `lb_auth_tag` (`id`, `name`, `color`, `sort`, `status`, `created_at`, `updated_at`) VALUES")
    parts.append("  (1, '官方', '#1677ff', 0, 1, NOW(), NOW());")
    parts.append("")

    # 顶部导航
    nav = [
        (1, '首页', '/', 'home', 1, 0, 1),
        (2, '话题', '/discuss', 'discuss', 1, 1, 1),
        (3, '文档', 'https://www.zbtech.com/', '', 1, 2, 1),
    ]
    parts.append("INSERT INTO `lb_web_top_nav` (`id`, `title`, `url`, `nav_key`, `target`, `sort`, `status`, `created_at`, `updated_at`) VALUES")
    nav_rows = []
    for n in nav:
        nav_rows.append("  (%s, '%s', '%s', %s, %s, %s, %s, NOW(), NOW())" % (
            n[0], n[1], n[2], "'%s'" % n[3] if n[3] else "NULL", n[4], n[5], n[6]))
    parts.append(",\n".join(nav_rows) + ";")
    parts.append("")

    parts.append("SET FOREIGN_KEY_CHECKS = 1;")
    parts.append("")
    return "\n".join(parts)


def write_file(path, content):
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("written:", path)


if __name__ == "__main__":
    # 管理员默认密码：admin123（bcrypt），若环境无 bcrypt 则用占位符并提示
    admin_pwd = "$2a$10$NEED_SET_PASSWORD_VIA_UPDATE_SQL_PLEASE"
    try:
        import bcrypt
        admin_pwd = bcrypt.hashpw("admin123".encode("utf-8"), bcrypt.gensalt(rounds=10)).decode("utf-8")
        print("bcrypt ok, admin default password = admin123")
    except Exception as e:
        print("bcrypt not available, using placeholder password. Will need UPDATE. (%s)" % e)

    gen_java()
    sql = gen_ddl(admin_pwd)
    write_file(SQL_OUT, sql)
    print("ALL DONE. SQL at", SQL_OUT)
