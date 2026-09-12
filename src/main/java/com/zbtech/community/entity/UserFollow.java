package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户关注表
 */
@Data
@TableName("lb_user_follow")
public class UserFollow {

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID
    private Integer user_id; // 用户ID
    private Integer follow_user_id; // 关注的用户ID
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
