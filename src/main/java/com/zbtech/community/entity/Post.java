package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 帖子表
 */
@Data
@TableName("lb_post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Integer id; // 帖子ID
    private Integer category_id; // 类目ID
    private Integer discuss_id; // 话题ID
    private Integer user_id; // 用户ID
    private String title; // 帖子标题
    private String content; // 帖子内容
    private String media; // 媒体内容
    private Integer like_count; // 点赞数量
    private Integer favorite_count; // 收藏数量
    private Integer view_count; // 浏览量
    private Integer comment_count; // 评论量
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
    private Integer is_top; // 是否置顶
    private LocalDateTime top_at; // 置顶时间

    /** 以下为返回给前端的虚拟字段（数据库无对应列） */
    @TableField(exist = false)
    @JsonProperty("is_liked")
    private Boolean is_liked; // 当前用户是否已点赞

    @TableField(exist = false)
    @JsonProperty("is_favorited")
    private Boolean is_favorited; // 当前用户是否已收藏

    @TableField(exist = false)
    @JsonProperty("content_html")
    private String content_html; // 内容渲染后的 html（PostContentService.render 生成，Markdown 或纯文本）

}
