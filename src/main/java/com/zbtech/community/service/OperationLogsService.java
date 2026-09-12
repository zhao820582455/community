package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.OperationLogs;

import java.util.List;
import java.util.Map;

/**
 * 操作日志业务（对齐原 foxbook adminapi/controller/OperationLog）
 */
public interface OperationLogsService extends IService<OperationLogs> {

    /**
     * 分页列表，支持搜索：ip/uri 模糊、is_success/user_type/method 精确、created_at 日期区间
     */
    IPage<OperationLogs> getListByPage(int page, int pageSize, String ip, String uri,
                                        Integer isSuccess, String userType, String method,
                                        String createdStart, String createdEnd);

    /** 获取日志，不存在抛异常 */
    OperationLogs getLogOrFail(Integer id);

    /**
     * 清理日志
     *
     * @param all  true 清空全部
     * @param days 清理 N 天前的日志（all=false 时生效，默认 30）
     * @return 清理条数
     */
    int clearLogs(boolean all, int days);

    /**
     * 获取统计信息（对齐 PHP getStatistics）
     *
     * @param days     统计天数（默认 7）
     * @param userType 访问者类型过滤（可选）
     * @return 统计数据
     */
    Map<String, Object> getStatistics(int days, String userType);
}
