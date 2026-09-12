package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 圈子类目表
 */
@Data
@TableName("lb_category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Integer id; // 类目ID
    private String name; // 类目名称
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
