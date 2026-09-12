package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 敏感词表
 */
@Data
@TableName("lb_sensitive_word")
public class SensitiveWord {

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID
    private String word; // 敏感词
    private Integer status; // 状态
    private LocalDateTime created_at; // 创建时间
    private LocalDateTime updated_at; // 更新时间
}
