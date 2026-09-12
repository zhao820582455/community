package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 菜单/接口权限表
 */
@Data
@TableName("lb_admins_permission")
public class AdminsPermission {

    @TableId(type = IdType.AUTO)
    private Integer id; // 菜单ID
    private Integer parent_id; // 父级菜单ID
    private String name; // 菜单标识
    private String title; // 菜单标题
    private String icon; // 菜单图标
    private String path; // 菜单路径或API路径
    private String component; // 组件路径
    private String meta; // meta(JSON)
    private Integer sort; // 排序
    private Integer status; // 状态：1启用，0禁用
    private Integer type; // 1 菜单，2接口
    private LocalDateTime created_at; // 创建时间
    private LocalDateTime updated_at; // 更新时间
}
