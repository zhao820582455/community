package com.zbtech.community.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论视图对象：在原 Comments 基础上附加 发送人/目标用户 简要信息与当前用户点赞态
 * （对齐原 foxbook with(['user','to_user']) + appendLikeState）
 */
@Data
public class CommentVO {
    private Integer id;
    private Integer user_id;     // 评论人ID
    private Integer to_user_id;  // 被回复用户ID
    private Integer post_id;     // 帖子ID
    private String content;      // 评论内容
    private Integer like_count;  // 点赞数
    private Integer reply_count; // 回复数
    private Boolean is_liked;    // 当前浏览用户是否点赞
    private UserBriefVO user;    // 评论人简要
    private UserBriefVO to_user; // 被回复用户简要
    private LocalDateTime created_at;
}
