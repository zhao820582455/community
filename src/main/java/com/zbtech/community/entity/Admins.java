package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员表
 */
@Data
@TableName("lb_admins")
public class Admins {

    @TableId(type = IdType.AUTO)
    private Integer id; // 管理员ID
    private Integer role_id; // 角色ID
    private String username; // 用户名
    private String password; // 密码
    private String real_name; // 姓名
    private String email; // 邮箱
    private String avatar; // 头像
    private String bio; // 个人简介
    private Integer status; // 状态：1启用，0禁用
    private LocalDateTime last_login_time; // 最后登录时间
    private String last_login_ip; // 最后登录IP
    private LocalDateTime created_at; // 创建时间
    private LocalDateTime updated_at; // 更新时间
}
