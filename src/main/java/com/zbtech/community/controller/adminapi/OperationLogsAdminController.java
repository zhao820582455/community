package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.OperationLogs;
import com.zbtech.community.service.OperationLogsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 操作日志 - 管理端（对齐原 foxbook adminapi/controller/OperationLog）
 */
@RestController
@RequestMapping({"/adminapi/operationLog", "/adminapi/OperationLog"})
@Tag(name = "操作日志管理")
public class OperationLogsAdminController extends BaseController {

    @Autowired
    private OperationLogsService service;

    /** GET /adminapi/operationLog/getListByPage 分页列表 */
    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(name = "queryForm[ip]", required = false) String ip,
                                    @RequestParam(name = "queryForm[uri]", required = false) String uri,
                                    @RequestParam(name = "queryForm[is_success]", required = false) Integer isSuccess,
                                    @RequestParam(name = "queryForm[user_type]", required = false) String userType,
                                    @RequestParam(name = "queryForm[method]", required = false) String method,
                                    @RequestParam(name = "queryForm[created_at][0]", required = false) String createdStart,
                                    @RequestParam(name = "queryForm[created_at][1]", required = false) String createdEnd) {
        getAdminId();
        return pageResult(service.getListByPage(page, pageSize, ip, uri, isSuccess, userType, method, createdStart, createdEnd));
    }

    /** GET /adminapi/operationLog/getList 全部日志 */
    @GetMapping("/getList")
    public Result<?> getList() {
        getAdminId();
        return Result.success(service.list());
    }

    /** GET /adminapi/operationLog/getLogDetail 日志详情 */
    @GetMapping("/getLogDetail")
    public Result<OperationLogs> getLogDetail(@RequestParam Integer id) {
        getAdminId();
        return Result.success(service.getLogOrFail(id));
    }

    /** GET /adminapi/operationLog/getDetail 日志详情（通用接口名） */
    @GetMapping("/getDetail")
    public Result<OperationLogs> getDetail(@RequestParam Integer id) {
        getAdminId();
        return Result.success(service.getLogOrFail(id));
    }

    /** POST /adminapi/operationLog/clearLogs 清理日志 */
    @PostMapping("/clearLogs")
    public Result<?> clearLogs(@RequestBody Map<String, Object> params) {
        getAdminId();
        boolean all = Boolean.TRUE.equals(params.get("all"));
        int days = params.get("days") != null ? Integer.parseInt(params.get("days").toString()) : 30;
        int count = service.clearLogs(all, days);
        String msg = all
                ? "成功清理所有日志记录，共 " + count + " 条"
                : "成功清理 " + count + " 条日志记录";
        return Result.success(msg);
    }

    /** GET /adminapi/operationLog/getStatistics 统计信息 */
    @GetMapping("/getStatistics")
    public Result<?> getStatistics(@RequestParam(defaultValue = "7") int days,
                                    @RequestParam(required = false) String userType) {
        getAdminId();
        return Result.success(service.getStatistics(days, userType));
    }
}
