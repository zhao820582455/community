package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论点赞表
 */
@Data
@TableName("lb_comment_likes")
public class CommentLikes {

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID
    private Integer user_id; // 用户ID
    private Integer comment_id; // 评论ID
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
