package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色菜单关联表
 */
@Data
@TableName("lb_admins_role_permission")
public class AdminsRolePermission {

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID
    private Integer role_id; // 角色ID
    private Integer menu_id; // 菜单ID
    private LocalDateTime created_at; // 创建时间
    private LocalDateTime updated_at; // 更新时间
}
