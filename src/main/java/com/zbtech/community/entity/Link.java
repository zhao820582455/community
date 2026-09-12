package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 广告/Banner表
 */
@Data
@TableName("lb_link")
public class Link {

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID
    private String title; // 标题
    private String url; // 跳转地址
    private String cover_img; // 封面图
    private String app_id; // 外部小程序appid
    private Integer type; // 1当前小程序 2外部小程序 3webview
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
