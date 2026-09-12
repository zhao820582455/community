package com.zbtech.community.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘管理端业务（对齐原 foxbook app/adminapi/controller/Dashboard.php）
 * 只读 UsersService / AdminsService / OperationLogsService / PostService 及 UsersMapper 分组，不修改其定义
 */
public interface DashboardService {

    /** 最近操作日志（联 user），limit 默认10 最大100 */
    List<Map<String, Object>> getRecentLogs(int limit);

    /** 仪表盘统计：用户/管理员/日志 */
    Map<String, Object> getStatistics();

    /** 近7天每日新增用户数（date, count, label=m-d） */
    List<Map<String, Object>> getUserGrowth();

    /** Java 运行环境信息 */
    Map<String, Object> getSystemInfo(HttpServletRequest request);

    /** 用户分布（按 terminal 分组并映射名称） */
    List<Map<String, Object>> getUserDistribution();

    /** 今日/本周/本月/上月新增用户数 */
    Map<String, Object> getUserStatistics();

    /** 今日/本周/本月/上月新增帖子数 */
    Map<String, Object> getPostStatistics();
}
