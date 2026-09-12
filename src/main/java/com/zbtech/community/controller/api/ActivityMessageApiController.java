package com.zbtech.community.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.ActivityMessage;
import com.zbtech.community.entity.Message;
import com.zbtech.community.service.ActivityMessageService;
import com.zbtech.community.service.MessageService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户端活动消息接口（对齐原 foxbook api/controller/ActivityMessage）
 */
@RestController
@RequestMapping("/api/activityMessage")
public class ActivityMessageApiController extends BaseController {

    @Resource
    private ActivityMessageService activityMessageService;

    @Resource
    private MessageService messageService;

    /**
     * GET /api/activityMessage/getDetail
     * 获取活动消息详情，同时标记消息为已读
     */
    @GetMapping("/getDetail")
    public Result<?> getDetail(@RequestParam Integer messageId) {
        Integer userId = getUserId();

        // 查询当前用户的活动类型消息
        LambdaQueryWrapper<Message> w = new LambdaQueryWrapper<>();
        w.eq(Message::getId, messageId)
                .eq(Message::getTo_user_id, userId)
                .eq(Message::getType, MessageService.TYPE_ACTIVITY);
        Message message = messageService.getOne(w);

        if (message == null || message.getActivity_message_id() == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "活动消息不存在");
        }

        // 标记已读
        if (message.getIs_read() == null || message.getIs_read() != 1) {
            message.setIs_read(1);
            message.setUpdated_at(LocalDateTime.now());
            messageService.updateById(message);
        }

        // 获取活动消息详情
        ActivityMessage activity = activityMessageService.getById(message.getActivity_message_id());
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "活动消息不存在");
        }

        // 构建响应（合并 meta + content + created_at）
        Map<String, Object> activityData = new LinkedHashMap<>(activityMessageService.buildActivityMeta(activity));
        activityData.put("content", activity.getContent() != null ? activity.getContent() : "");
        activityData.put("created_at", activity.getCreated_at());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message_id", String.valueOf(message.getId()));
        result.put("activity", activityData);
        return Result.success(result);
    }
}
