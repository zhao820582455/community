package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 平台配置表
 */
@Data
@TableName("lb_platform_config")
public class PlatformConfig {

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID
    private String name; // 配置名称
    @TableField("`key`")
    private String key; // 配置键名
    @TableField("`value`")
    private String value; // 配置值(JSON)
    private LocalDateTime created_at; // 创建时间
    private LocalDateTime updated_at; // 更新时间
}
