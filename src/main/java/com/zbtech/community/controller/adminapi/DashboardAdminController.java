package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 仪表盘管理端（对齐原 foxbook app/adminapi/controller/Dashboard.php）
 * 路径规则：/adminapi/dashboard/{action}
 */
@RestController
@RequestMapping("/adminapi/dashboard")
@Tag(name = "仪表盘")
public class DashboardAdminController extends BaseController {

    @Autowired
    private DashboardService dashboardService;

    /** GET /adminapi/dashboard/getRecentLogs 最近操作日志 */
    @GetMapping("/getRecentLogs")
    public Result<?> getRecentLogs(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(dashboardService.getRecentLogs(limit));
    }

    /** GET /adminapi/dashboard/getStatistics 综合统计 */
    @GetMapping("/getStatistics")
    public Result<?> getStatistics() {
        return Result.success(dashboardService.getStatistics());
    }

    /** GET /adminapi/dashboard/getUserGrowth 近7天用户增长 */
    @GetMapping("/getUserGrowth")
    public Result<?> getUserGrowth() {
        return Result.success(dashboardService.getUserGrowth());
    }

    /** GET /adminapi/dashboard/getSystemInfo Java 环境信息 */
    @GetMapping("/getSystemInfo")
    public Result<?> getSystemInfo() {
        return Result.success(dashboardService.getSystemInfo(getRequest()));
    }

    /** GET /adminapi/dashboard/getUserDistribution 用户终端分布 */
    @GetMapping("/getUserDistribution")
    public Result<?> getUserDistribution() {
        return Result.success(dashboardService.getUserDistribution());
    }

    /** GET /adminapi/dashboard/getUserStatistics 用户新增统计 */
    @GetMapping("/getUserStatistics")
    public Result<?> getUserStatistics() {
        return Result.success(dashboardService.getUserStatistics());
    }

    /** GET /adminapi/dashboard/getPostStatistics 帖子新增统计 */
    @GetMapping("/getPostStatistics")
    public Result<?> getPostStatistics() {
        return Result.success(dashboardService.getPostStatistics());
    }
}
