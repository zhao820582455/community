package com.zbtech.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 操作日志表
 */
@Data
@TableName("lb_operation_logs")
public class OperationLogs {

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID
    private Integer user_id; // 用户ID
    private Integer admin_id; // 管理员ID
    private String user_type; // 访问者类型
    private String method; // 请求方法
    private String url; // 请求URI
    private String request_data; // 请求参数
    private Integer response_code; // 响应状态码
    private Integer is_success; // 是否成功
    private BigDecimal duration; // 响应时间(毫秒)
    private String ip; // IP地址
    private String user_agent; // 用户代理
    private LocalDateTime updated_at; // 更新时间
    private LocalDateTime created_at; // 创建时间
}
