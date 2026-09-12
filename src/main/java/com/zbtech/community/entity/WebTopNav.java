package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * PC Web 顶部导航配置表
 */
@Data
@TableName("lb_web_top_nav")
public class WebTopNav {

    @TableId(type = IdType.AUTO)
    private Integer id; // 导航ID
    private String title; // 菜单名称
    private String url; // 跳转地址
    private String nav_key; // 导航高亮标识
    private Integer target; // 1当前窗口 2新窗口
    private Integer sort; // 排序
    private Integer status; // 1显示 0隐藏
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
