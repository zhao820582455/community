package com.zbtech.community.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 站内消息视图对象：附加 发送人/接收人/帖子 简要信息与活动消息元信息
 * （对齐原 foxbook with(['user','toUser','post','activityMessage']) + normalizeMessagePayload）
 */
@Data
public class MessageVO {
    private Integer id;
    private Integer user_id;       // 发起动作的用户ID（发送人）
    private Integer to_user_id;    // 接收用户ID
    private Integer post_id;       // 关联帖子ID
    private String content;        // 消息内容
    private Integer type;          // 1赞 2收藏 3评论 4关注 9系统 10活动
    private Integer is_read;       // 0未读 1已读
    private LocalDateTime created_at;
    private UserBriefVO user;      // 发送人简要
    private UserBriefVO to_user;   // 接收人简要
    private PostBriefVO post;      // 帖子简要
    private Map<String, Object> activity_meta; // 活动消息元信息（type=10 时填充）
}
