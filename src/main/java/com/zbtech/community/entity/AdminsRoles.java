package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色表
 */
@Data
@TableName("lb_admins_roles")
public class AdminsRoles {

    @TableId(type = IdType.AUTO)
    private Integer id; // 角色ID
    private String name; // 角色名称
    private String description; // 角色描述
    private Integer status; // 状态：1启用，0禁用
    private Integer is_founder; // 是否创建人：1 创建人，拥有系统所有权限
    private LocalDateTime created_at; // 创建时间
    private LocalDateTime updated_at; // 更新时间
}
