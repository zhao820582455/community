package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户认证标签关联表
 */
@Data
@TableName("lb_user_auth_tag")
public class UserAuthTag {

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID
    private Integer user_id; // 用户ID
    private Integer tag_id; // 标签ID
    private LocalDateTime created_at; // 创建时间
    private LocalDateTime updated_at; // 更新时间
}
