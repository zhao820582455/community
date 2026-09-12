package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 话题/讨论表
 */
@Data
@TableName("lb_discuss")
public class Discuss {

    @TableId(type = IdType.AUTO)
    private Integer id; // 话题ID
    private Integer user_id; // 发起人ID
    private String title; // 话题标题
    private String content; // 话题内容
    private String media; // 媒体内容
    private Integer view_count; // 浏览量
    private Integer post_count; // 帖子数量
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
