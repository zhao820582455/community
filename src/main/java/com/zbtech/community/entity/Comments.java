package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论表(树形)
 */
@Data
@TableName("lb_comments")
public class Comments {

    @TableId(type = IdType.AUTO)
    private Integer id; // 评论ID
    private Integer parent_id; // 父评论ID
    private Integer root_id; // 根评论ID
    private Integer user_id; // 用户ID
    private Integer to_user_id; // 回复目标用户ID
    private Integer post_id; // 帖子ID
    private String content; // 评论内容
    private Integer like_count; // 点赞数量
    private Integer reply_count; // 回复数量
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
