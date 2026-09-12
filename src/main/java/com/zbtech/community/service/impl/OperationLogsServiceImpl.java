package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.OperationLogs;
import com.zbtech.community.mapper.OperationLogsMapper;
import com.zbtech.community.service.OperationLogsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OperationLogsServiceImpl extends ServiceImpl<OperationLogsMapper, OperationLogs> implements OperationLogsService {

    @Override
    public IPage<OperationLogs> getListByPage(int page, int pageSize, String ip, String uri,
                                               Integer isSuccess, String userType, String method,
                                               String createdStart, String createdEnd) {
        Page<OperationLogs> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<OperationLogs> w = new LambdaQueryWrapper<>();
        if (ip != null && !ip.isBlank()) {
            w.like(OperationLogs::getIp, ip.trim());
        }
        if (uri != null && !uri.isBlank()) {
            w.like(OperationLogs::getUrl, uri.trim());
        }
        if (isSuccess != null) {
            w.eq(OperationLogs::getIs_success, isSuccess);
        }
        if (userType != null && !userType.isBlank()) {
            w.eq(OperationLogs::getUser_type, userType.trim());
        }
        if (method != null && !method.isBlank()) {
            w.eq(OperationLogs::getMethod, method.trim());
        }
        if (createdStart != null && !createdStart.isBlank()) {
            w.ge(OperationLogs::getCreated_at, createdStart.trim() + " 00:00:00");
        }
        if (createdEnd != null && !createdEnd.isBlank()) {
            w.le(OperationLogs::getCreated_at, createdEnd.trim() + " 23:59:59");
        }
        w.orderByDesc(OperationLogs::getCreated_at);
        return page(p, w);
    }

    @Override
    public OperationLogs getLogOrFail(Integer id) {
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "日志ID不能为空");
        }
        OperationLogs log = getById(id);
        if (log == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "日志不存在");
        }
        return log;
    }

    @Override
    @Transactional
    public int clearLogs(boolean all, int days) {
        if (all) {
            List<OperationLogs> allLogs = list();
            int count = allLogs.size();
            remove(new LambdaQueryWrapper<>());
            return count;
        }
        int effectiveDays = Math.max(1, days);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(effectiveDays);
        LambdaQueryWrapper<OperationLogs> w = new LambdaQueryWrapper<>();
        w.lt(OperationLogs::getCreated_at, cutoff);
        List<OperationLogs> toDelete = list(w);
        remove(w);
        return toDelete.size();
    }

    @Override
    public Map<String, Object> getStatistics(int days, String userType) {
        int effectiveDays = Math.max(1, days);
        LocalDateTime startDate = LocalDate.now().minusDays(effectiveDays).atStartOfDay();
        LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);

        LambdaQueryWrapper<OperationLogs> baseQuery = new LambdaQueryWrapper<>();
        baseQuery.ge(OperationLogs::getCreated_at, startDate)
                .le(OperationLogs::getCreated_at, endDate);
        if (userType != null && !userType.isBlank()) {
            baseQuery.eq(OperationLogs::getUser_type, userType.trim());
        }

        List<OperationLogs> allLogs = list(baseQuery);
        int totalRequests = allLogs.size();
        int successRequests = 0;
        double totalDuration = 0;
        int durationCount = 0;

        // 按天统计
        Map<String, int[]> dailyMap = new TreeMap<>();
        // 按请求方法统计
        Map<String, Integer> methodMap = new LinkedHashMap<>();
        // 按访问者类型统计
        Map<String, Integer> userTypeMap = new LinkedHashMap<>();

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (OperationLogs log : allLogs) {
            // 成功统计
            if (log.getIs_success() != null && log.getIs_success() == 1) {
                successRequests++;
            }
            // 耗时统计
            if (log.getDuration() != null) {
                totalDuration += log.getDuration().doubleValue();
                durationCount++;
            }
            // 按天
            if (log.getCreated_at() != null) {
                String day = log.getCreated_at().format(dayFmt);
                dailyMap.computeIfAbsent(day, k -> new int[]{0, 0});
                dailyMap.get(day)[0]++;
                if (log.getIs_success() != null && log.getIs_success() == 1) {
                    dailyMap.get(day)[1]++;
                }
            }
            // 按方法
            if (log.getMethod() != null && !log.getMethod().isBlank()) {
                methodMap.merge(log.getMethod(), 1, Integer::sum);
            }
            // 按访问者类型
            if (log.getUser_type() != null && !log.getUser_type().isBlank()) {
                userTypeMap.merge(log.getUser_type(), 1, Integer::sum);
            }
        }

        int failedRequests = totalRequests - successRequests;
        double successRate = totalRequests > 0
                ? Math.round((double) successRequests / totalRequests * 10000) / 100.0
                : 0;
        double avgDuration = durationCount > 0
                ? Math.round(totalDuration / durationCount * 100) / 100.0
                : 0;

        // 组装每日统计
        List<Map<String, Object>> dailyStats = new ArrayList<>();
        for (Map.Entry<String, int[]> e : dailyMap.entrySet()) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", e.getKey());
            day.put("total", e.getValue()[0]);
            day.put("success", e.getValue()[1]);
            dailyStats.add(day);
        }

        // 方法统计排序（按 count 降序）
        List<Map<String, Object>> methodStats = new ArrayList<>();
        methodMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("method", e.getKey());
                    m.put("count", e.getValue());
                    methodStats.add(m);
                });

        // 用户类型统计排序
        List<Map<String, Object>> userTypeStats = new ArrayList<>();
        userTypeMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("user_type", e.getKey());
                    m.put("count", e.getValue());
                    userTypeStats.add(m);
                });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_requests", totalRequests);
        result.put("success_requests", successRequests);
        result.put("failed_requests", failedRequests);
        result.put("success_rate", successRate);
        result.put("avg_duration", avgDuration);
        result.put("method_stats", methodStats);
        result.put("daily_stats", dailyStats);
        result.put("user_type_stats", userTypeStats);
        return result;
    }
}
