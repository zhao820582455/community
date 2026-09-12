package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.ActivityMessage;
import com.zbtech.community.service.ActivityMessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 活动消息 - 管理端（对齐原 foxbook adminapi/controller/ActivityMessage）
 */
@RestController
@RequestMapping({"/adminapi/activityMessage", "/adminapi/ActivityMessage"})
@Tag(name = "活动消息管理")
public class ActivityMessageAdminController extends BaseController {

    @Autowired
    private ActivityMessageService service;

    /** GET /adminapi/activityMessage/getListByPage 分页列表 */
    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(name = "queryForm[title]", required = false) String title,
                                    @RequestParam(name = "queryForm[status]", required = false) Integer status,
                                    @RequestParam(name = "queryForm[target_type]", required = false) Integer targetType,
                                    @RequestParam(name = "queryForm[content_type]", required = false) Integer contentType,
                                    @RequestParam(name = "queryForm[created_at][0]", required = false) String createdStart,
                                    @RequestParam(name = "queryForm[created_at][1]", required = false) String createdEnd) {
        getAdminId();
        return pageResult(service.getListByPage(page, pageSize, title, status, targetType, contentType, createdStart, createdEnd));
    }

    /** GET /adminapi/activityMessage/getList 全部活动消息 */
    @GetMapping("/getList")
    public Result<?> getList() {
        getAdminId();
        return Result.success(service.list());
    }

    /** GET /adminapi/activityMessage/getDetail 活动消息详情 */
    @GetMapping("/getDetail")
    public Result<ActivityMessage> getDetail(@RequestParam Integer id) {
        getAdminId();
        return Result.success(service.getActivityOrFail(id));
    }

    /**
     * POST /adminapi/activityMessage/save
     * 创建活动消息并分发
     */
    @PostMapping("/save")
    public Result<?> save(@RequestBody Map<String, Object> params) {
        Integer adminId = getAdminId();
        String title = strOf(params.get("title"));
        String cover = strOf(params.get("cover"));
        Integer contentType = intOf(params.get("content_type"));
        Integer targetType = intOf(params.get("target_type"));
        String url = strOf(params.get("url"));
        String content = strOf(params.get("content"));

        @SuppressWarnings("unchecked")
        List<Integer> userIds = params.get("user_ids") instanceof List
                ? ((List<?>) params.get("user_ids")).stream()
                        .map(this::intOf).filter(java.util.Objects::nonNull).distinct().toList()
                : List.of();

        if (contentType == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "内容类型不能为空");
        }
        if (targetType == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "推送范围不能为空");
        }

        ActivityMessage activity = service.createAndDispatch(
                title, cover, contentType, url, content, targetType, userIds, adminId);
        return Result.success("发送成功", Map.of("id", String.valueOf(activity.getId())));
    }

    /**
     * GET /adminapi/activityMessage/searchUsers
     * 搜索用户（用于选择推送目标用户）
     */
    @GetMapping("/searchUsers")
    public Result<?> searchUsers(@RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "20") int limit) {
        getAdminId();
        return Result.success(service.searchUsers(keyword, limit));
    }

    // ===================== 参数解析 =====================

    private Integer intOf(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try { return Integer.valueOf(s); } catch (NumberFormatException e) { return null; }
    }

    private String strOf(Object o) {
        return o == null ? null : o.toString();
    }
}
