package com.zbtech.community.controller.api;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 消息 - 用户端接口（对齐原 foxbook app/api/controller/Message.php）
 * 路由前缀 /api/message
 */
@RestController
@RequestMapping("/api/message")
public class MessageApiController extends BaseController {

    @Autowired
    private MessageService messageService;

    /**
     * 未读消息统计（匿名调用返回零值，避免小程序启动阶段报错）
     */
    @GetMapping("/getUnreadCount")
    public Result<?> getUnreadCount(@RequestParam(required = false) String type) {
        Integer userId = getUserIdOptional();
        if (userId == null) {
            Map<String, Object> zero = new HashMap<>(10);
            zero.put("total", 0L);
            zero.put("like_count", 0L);
            zero.put("favorite_count", 0L);
            zero.put("comment_count", 0L);
            zero.put("follow_count", 0L);
            zero.put("system_count", 0L);
            zero.put("activity_count", 0L);
            zero.put("new_system", null);
            zero.put("new_activity", null);
            return Result.success(zero);
        }
        return Result.success(messageService.getUnreadCount(userId, parseType(type)));
    }

    /**
     * 消息列表（分页）
     */
    @RequestMapping(value = "/getUserMsgList", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<?> getUserMsgList(@RequestParam(required = false) String type,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestBody(required = false) Map<String, Object> body) {
        if (body != null) {
            if (type == null && body.get("type") != null) {
                type = normalizeType(body.get("type"));
            }
            Object bodyPage = body.get("page");
            if (bodyPage != null) {
                Integer v = toInt(bodyPage);
                if (v != null) page = v;
            }
            Object bodyPageSize = body.get("pageSize");
            if (bodyPageSize != null) {
                Integer v = toInt(bodyPageSize);
                if (v != null) pageSize = v;
            }
        }
        Integer userId = getUserId();
        return pageResult(messageService.getUserMsgList(userId, parseType(type), page, pageSize));
    }

    /** JSON body 中 type 可能是数字 / 字符串 / 数组，统一转成逗号分隔字符串 */
    private String normalizeType(Object type) {
        if (type == null) {
            return null;
        }
        if (type instanceof Iterable) {
            StringBuilder sb = new StringBuilder();
            for (Object o : (Iterable<?>) type) {
                if (o != null) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(o);
                }
            }
            return sb.length() == 0 ? null : sb.toString();
        }
        return String.valueOf(type);
    }

    /**
     * 标记单条消息已读
     */
    @PostMapping("/markAsRead")
    public Result<?> markAsRead(@RequestBody Map<String, Object> body) {
        Integer userId = getUserId();
        Integer messageId = toInt(body.get("message_id"));
        if (messageId == null) {
            return Result.error("消息ID必填");
        }
        boolean ok = messageService.markAsRead(userId, messageId);
        if (!ok) {
            return Result.error("消息不存在或无权限访问");
        }
        return Result.success("消息已标记为已读");
    }

    /**
     * 标记全部已读（可按类型）
     */
    @PostMapping("/markAllAsRead")
    public Result<?> markAllAsRead(@RequestBody(required = false) Map<String, Object> body) {
        Integer userId = getUserId();
        List<Integer> type = null;
        if (body != null && body.get("type") != null) {
            type = parseType(String.valueOf(body.get("type")));
        }
        int affected = messageService.markAllAsRead(userId, type);
        Map<String, Object> res = new HashMap<>(1);
        res.put("affected_rows", affected);
        return Result.success("消息已全部标记为已读", res);
    }

    private List<Integer> parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            if (type.contains(",")) {
                List<Integer> list = new ArrayList<>();
                for (String s : type.split(",")) {
                    s = s.trim();
                    if (!s.isEmpty()) {
                        list.add(Integer.parseInt(s));
                    }
                }
                return list.isEmpty() ? null : list;
            }
            return Collections.singletonList(Integer.parseInt(type.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
