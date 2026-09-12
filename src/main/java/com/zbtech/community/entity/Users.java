package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("lb_users")
public class Users {

    @TableId(type = IdType.AUTO)
    private Integer id; // 用户ID
    private String sn; // 用户编号
    private String username; // 用户名
    private String password; // 密码
    private String nickname; // 昵称
    private String avatar; // 头像
    private String phone; // 手机号
    private String email; // 邮箱
    private Integer gender; // 性别0未知1男2女
    private LocalDate birthday; // 生日
    private String openid; // 微信openid
    private String unionid; // 微信unionid
    private Integer post_count; // 帖子数量
    private Integer follow_count; // 关注数量
    private Integer fans_count; // 粉丝数量
    private Integer post_thumb_count; // 获赞数量
    private Integer post_collect_count; // 被收藏总量
    private String introduction; // 个人简介
    private Integer status; // 状态：-1禁用，0正常
    private LocalDateTime last_login_time; // 最后登录时间
    private String last_login_ip; // 最后登录IP
    private Integer terminal; // 登录终端1小程序2H53PC4安卓5iOS
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
