package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 微信图片审核结果表
 */
@Data
@TableName("lb_media_check")
public class MediaCheck {

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID
    private String trace_id; // 微信trace_id
    private Integer post_id; // 帖子ID
    private String is_risky; // 风险状态
    private String media_src; // 媒体地址
    private LocalDateTime created_at; // 创建时间
    private LocalDateTime updated_at; // 更新时间
}
