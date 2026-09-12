package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 活动消息表
 */
@Data
@TableName("lb_activity_message")
public class ActivityMessage {

    @TableId(type = IdType.AUTO)
    private Integer id; // 活动消息ID
    private String title; // 活动标题
    private String cover; // 活动封面
    private String summary; // 摘要
    private Integer content_type; // 1 URL，2 富文本
    private String url; // 活动地址
    private String content; // 富文本内容
    private Integer target_type; // 1 全部用户，2 指定用户
    private String target_user_ids; // 指定用户ID(JSON)
    private Integer status; // 0待发送 1已发送 2失败
    private Integer recipient_count; // 接收人数
    private String failed_reason; // 失败原因
    private Integer created_by; // 创建管理员ID
    private LocalDateTime sent_at; // 发送时间
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
