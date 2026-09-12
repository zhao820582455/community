package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 协议表
 */
@Data
@TableName("lb_agreement")
public class Agreement {

    @TableId(type = IdType.AUTO)
    private Integer id; // 协议ID
    private String title; // 协议标题
    private String content; // 协议内容(HTML)
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
