package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbtech.community.entity.Admins;
import com.zbtech.community.entity.OperationLogs;
import com.zbtech.community.entity.Post;
import com.zbtech.community.entity.Users;
import com.zbtech.community.mapper.UsersMapper;
import com.zbtech.community.service.AdminsService;
import com.zbtech.community.service.DashboardService;
import com.zbtech.community.service.OperationLogsService;
import com.zbtech.community.service.PostService;
import com.zbtech.community.service.UsersService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘管理端业务实现（对齐原 foxbook Dashboard.php）
 * 全部为只读统计，复用各 Service 的 count() 与 UsersMapper 分组
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final UsersService usersService;
    private final AdminsService adminsService;
    private final OperationLogsService operationLogsService;
    private final PostService postService;
    private final UsersMapper usersMapper;

    public DashboardServiceImpl(UsersService usersService, AdminsService adminsService,
                                OperationLogsService operationLogsService, PostService postService,
                                UsersMapper usersMapper) {
        this.usersService = usersService;
        this.adminsService = adminsService;
        this.operationLogsService = operationLogsService;
        this.postService = postService;
        this.usersMapper = usersMapper;
    }

    @Override
    public List<Map<String, Object>> getRecentLogs(int limit) {
        if (limit < 1) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }
        List<OperationLogs> logs = operationLogsService.list(
                new LambdaQueryWrapper<OperationLogs>()
                        .orderByDesc(OperationLogs::getCreated_at)
                        .last("LIMIT " + limit));

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        // 关联用户信息（user_id）
        Set<Integer> userIds = logs.stream()
                .map(OperationLogs::getUser_id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Users> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : usersService.listByIds(userIds).stream()
                .collect(Collectors.toMap(Users::getId, u -> u, (a, b) -> a));

        List<Map<String, Object>> res = new ArrayList<>(logs.size());
        for (OperationLogs log : logs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", log.getId());
            m.put("user_id", log.getUser_id());
            m.put("admin_id", log.getAdmin_id());
            m.put("user_type", log.getUser_type());
            m.put("method", log.getMethod());
            m.put("url", log.getUrl());
            m.put("request_data", log.getRequest_data());
            m.put("response_code", log.getResponse_code());
            m.put("is_success", log.getIs_success());
            m.put("duration", log.getDuration());
            m.put("ip", log.getIp());
            m.put("user_agent", log.getUser_agent());
            m.put("created_at", log.getCreated_at());
            m.put("updated_at", log.getUpdated_at());

            Users u = log.getUser_id() != null ? userMap.get(log.getUser_id()) : null;
            if (u != null) {
                Map<String, Object> ub = new LinkedHashMap<>();
                ub.put("id", u.getId());
                ub.put("nickname", u.getNickname());
                ub.put("avatar", u.getAvatar());
                m.put("user", ub);
            } else {
                m.put("user", null);
            }
            res.add(m);
        }
        return res;
    }

    @Override
    public Map<String, Object> getStatistics() {
        LocalDate now = LocalDate.now();
        String today = now.toString();
        String yesterday = now.minusDays(1).toString();
        String weekStart = now.minusDays(7).toString();

        // 用户统计
        long totalUsers = usersService.count();
        long todayUsers = countUsersByDate(today);
        long activeUsers = usersService.count(new LambdaQueryWrapper<Users>().eq(Users::getStatus, 1));
        long weekUsers = usersService.count(new LambdaQueryWrapper<Users>()
                .between(Users::getCreated_at, LocalDate.parse(weekStart).atStartOfDay(),
                        LocalDate.parse(today).atTime(23, 59, 59)));

        // 管理员统计
        long totalAdmins = adminsService.count();
        long activeAdmins = adminsService.count(new LambdaQueryWrapper<Admins>().eq(Admins::getStatus, 1));

        // 操作日志统计
        long totalLogs = operationLogsService.count();
        long todayLogs = countLogsByDate(today);
        long successLogs = operationLogsService.count(new LambdaQueryWrapper<OperationLogs>().eq(OperationLogs::getIs_success, 1));
        long errorLogs = operationLogsService.count(new LambdaQueryWrapper<OperationLogs>().eq(OperationLogs::getIs_success, 0));

        // 增长率（与昨日对比）
        long yesterdayUsers = countUsersByDate(yesterday);
        double userGrowthRate = yesterdayUsers > 0
                ? round2((todayUsers - yesterdayUsers) * 100.0 / yesterdayUsers) : 0;
        long yesterdayLogs = countLogsByDate(yesterday);
        double logGrowthRate = yesterdayLogs > 0
                ? round2((todayLogs - yesterdayLogs) * 100.0 / yesterdayLogs) : 0;

        Map<String, Object> users = new LinkedHashMap<>();
        users.put("total", totalUsers);
        users.put("today", todayUsers);
        users.put("active", activeUsers);
        users.put("week", weekUsers);
        users.put("growth_rate", userGrowthRate);

        Map<String, Object> admins = new LinkedHashMap<>();
        admins.put("total", totalAdmins);
        admins.put("active", activeAdmins);

        Map<String, Object> logs = new LinkedHashMap<>();
        logs.put("total", totalLogs);
        logs.put("today", todayLogs);
        logs.put("success", successLogs);
        logs.put("error", errorLogs);
        logs.put("success_rate", totalLogs > 0 ? round2(successLogs * 100.0 / totalLogs) : 0);
        logs.put("growth_rate", logGrowthRate);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", users);
        data.put("admins", admins);
        data.put("logs", logs);
        return data;
    }

    @Override
    public List<Map<String, Object>> getUserGrowth() {
        List<Map<String, Object>> data = new ArrayList<>(7);
        LocalDate now = LocalDate.now();
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = now.minusDays(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.toString());
            m.put("count", countUsersByDate(d.toString()));
            m.put("label", d.format(labelFmt));
            data.add(m);
        }
        return data;
    }

    @Override
    public Map<String, Object> getSystemInfo(HttpServletRequest request) {
        Runtime rt = Runtime.getRuntime();
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMem = rt.maxMemory() / 1024 / 1024;
        File root = new File(".");
        long diskFree = root.getFreeSpace() / 1024 / 1024 / 1024;
        long diskTotal = root.getTotalSpace() / 1024 / 1024 / 1024;

        // server_software：优先取 Servlet 容器信息，兜底为 Spring Boot
        String serverSoftware = "Spring Boot";
        if (request != null) {
            try {
                String info = request.getServletContext().getServerInfo();
                if (info != null && !info.isEmpty()) {
                    serverSoftware = info;
                }
            } catch (Exception ignored) {
                // 非 Web 上下文时忽略
            }
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("java_version", System.getProperty("java.version"));
        m.put("server_software", serverSoftware);
        m.put("memory_usage", usedMem + " MB");
        m.put("memory_peak", maxMem + " MB");
        m.put("server_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        m.put("timezone", TimeZone.getDefault().getID());
        m.put("disk_free", diskFree + " GB");
        m.put("disk_total", diskTotal + " GB");
        return m;
    }

    @Override
    public List<Map<String, Object>> getUserDistribution() {
        List<Map<String, Object>> rows = usersMapper.selectCountByTerminal();
        Map<Integer, String> typeMap = new HashMap<>(5);
        typeMap.put(1, "微信小程序");
        typeMap.put(2, "H5网页");
        typeMap.put(3, "PC网页");
        typeMap.put(4, "安卓APP");
        typeMap.put(5, "苹果APP");

        List<Map<String, Object>> res = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object termObj = row.get("terminal");
            Integer terminal = (termObj == null) ? 0
                    : (termObj instanceof Number ? ((Number) termObj).intValue()
                    : Integer.parseInt(termObj.toString()));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("terminal", terminal);
            m.put("name", typeMap.getOrDefault(terminal, "未知"));
            m.put("count", row.get("count"));
            res.add(m);
        }
        return res;
    }

    @Override
    public Map<String, Object> getUserStatistics() {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate lastMonthStart = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastMonthEnd = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth());

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("today", countUsersBetween(now.atStartOfDay(), now.atTime(23, 59, 59)));
        stat.put("week", countUsersBetween(now.minusDays(7).atStartOfDay(), now.atTime(23, 59, 59)));
        stat.put("month", countUsersBetween(monthStart.atStartOfDay(), now.atTime(23, 59, 59)));
        stat.put("lastMonth", countUsersBetween(lastMonthStart.atStartOfDay(), lastMonthEnd.atTime(23, 59, 59)));
        return stat;
    }

    @Override
    public Map<String, Object> getPostStatistics() {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate lastMonthStart = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastMonthEnd = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth());

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("today", countPostsBetween(now.atStartOfDay(), now.atTime(23, 59, 59)));
        stat.put("week", countPostsBetween(now.minusDays(7).atStartOfDay(), now.atTime(23, 59, 59)));
        stat.put("month", countPostsBetween(monthStart.atStartOfDay(), now.atTime(23, 59, 59)));
        stat.put("lastMonth", countPostsBetween(lastMonthStart.atStartOfDay(), lastMonthEnd.atTime(23, 59, 59)));
        return stat;
    }

    // ===================== 私有统计工具 =====================

    private long countUsersByDate(String date) {
        return usersService.count(new LambdaQueryWrapper<Users>().apply("DATE(created_at) = {0}", date));
    }

    private long countLogsByDate(String date) {
        return operationLogsService.count(new LambdaQueryWrapper<OperationLogs>().apply("DATE(created_at) = {0}", date));
    }

    private long countUsersBetween(LocalDateTime start, LocalDateTime end) {
        return usersService.count(new LambdaQueryWrapper<Users>().between(Users::getCreated_at, start, end));
    }

    private long countPostsBetween(LocalDateTime start, LocalDateTime end) {
        return postService.count(new LambdaQueryWrapper<Post>().between(Post::getCreated_at, start, end));
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
