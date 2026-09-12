package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 站内消息表
 */
@Data
@TableName("lb_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Integer id; // 消息ID
    private Integer user_id; // 接收人ID
    private Integer to_user_id; // 发送人ID
    private Integer post_id; // 帖子ID
    private Integer activity_message_id; // 活动消息ID
    private String content; // 消息内容
    private Integer type; // 1赞 2收藏 3评论 4关注 9系统 10活动
    private Integer is_read; // 0未读 1已读
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
