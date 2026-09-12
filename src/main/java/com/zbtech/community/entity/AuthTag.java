package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 认证标签表
 */
@Data
@TableName("lb_auth_tag")
public class AuthTag {

    @TableId(type = IdType.AUTO)
    private Integer id; // 标签ID
    private String name; // 标签名称
    private String color; // 标签颜色
    private Integer sort; // 排序
    private Integer status; // 状态
    private LocalDateTime created_at; // 创建时间
    private LocalDateTime updated_at; // 更新时间
}
