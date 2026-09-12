package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.ActivityMessage;
import com.zbtech.community.entity.Message;
import com.zbtech.community.entity.Users;
import com.zbtech.community.mapper.ActivityMessageMapper;
import com.zbtech.community.service.ActivityMessageService;
import com.zbtech.community.service.MessageService;
import com.zbtech.community.service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActivityMessageServiceImpl extends ServiceImpl<ActivityMessageMapper, ActivityMessage> implements ActivityMessageService {

    @Autowired
    private UsersService usersService;

    @Autowired
    @Lazy
    private MessageService messageService;

    @Override
    public IPage<ActivityMessage> getListByPage(int page, int pageSize, String title,
                                                 Integer status, Integer targetType, Integer contentType,
                                                 String createdStart, String createdEnd) {
        Page<ActivityMessage> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<ActivityMessage> w = new LambdaQueryWrapper<>();
        if (title != null && !title.isBlank()) {
            w.like(ActivityMessage::getTitle, title.trim());
        }
        if (status != null) {
            w.eq(ActivityMessage::getStatus, status);
        }
        if (targetType != null) {
            w.eq(ActivityMessage::getTarget_type, targetType);
        }
        if (contentType != null) {
            w.eq(ActivityMessage::getContent_type, contentType);
        }
        if (createdStart != null && !createdStart.isBlank()) {
            w.ge(ActivityMessage::getCreated_at, createdStart.trim() + " 00:00:00");
        }
        if (createdEnd != null && !createdEnd.isBlank()) {
            w.le(ActivityMessage::getCreated_at, createdEnd.trim() + " 23:59:59");
        }
        w.orderByDesc(ActivityMessage::getCreated_at);
        return page(p, w);
    }

    @Override
    public ActivityMessage getActivityOrFail(Integer id) {
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "活动消息ID不能为空");
        }
        ActivityMessage activity = getById(id);
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "活动消息不存在");
        }
        return activity;
    }

    @Override
    @Transactional
    public ActivityMessage createAndDispatch(String title, String cover, int contentType,
                                              String url, String content, int targetType,
                                              List<Integer> userIds, Integer adminId) {
        // 参数校验
        String trimmedTitle = title == null ? "" : title.trim();
        if (trimmedTitle.isEmpty() || trimmedTitle.length() > 100) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "活动标题长度必须在 1-100 个字符之间");
        }
        String trimmedCover = cover == null ? "" : cover.trim();
        if (trimmedCover.isEmpty() || trimmedCover.length() > 500) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "活动封面不能为空且不超过500字符");
        }
        if (contentType != CONTENT_TYPE_URL && contentType != CONTENT_TYPE_RICHTEXT) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "内容类型无效");
        }
        if (targetType != TARGET_TYPE_ALL && targetType != TARGET_TYPE_USERS) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "推送范围无效");
        }
        if (contentType == CONTENT_TYPE_URL && (url == null || url.trim().isEmpty())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "请填写活动地址");
        }
        if (contentType == CONTENT_TYPE_RICHTEXT && (content == null || content.trim().isEmpty())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "请填写活动内容");
        }
        if (targetType == TARGET_TYPE_USERS && (userIds == null || userIds.isEmpty())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "请选择至少一个用户");
        }

        // 归一化目标用户ID
        List<Integer> normalizedUserIds = normalizeTargetUserIds(targetType, userIds);

        // 构建摘要
        String summary = buildSummary(contentType, url == null ? "" : url, content == null ? "" : content);

        // 创建活动消息
        ActivityMessage activity = new ActivityMessage();
        activity.setTitle(trimmedTitle);
        activity.setCover(trimmedCover);
        activity.setSummary(summary);
        activity.setContent_type(contentType);
        activity.setUrl(contentType == CONTENT_TYPE_URL ? url.trim() : null);
        activity.setContent(contentType == CONTENT_TYPE_RICHTEXT ? content : null);
        activity.setTarget_type(targetType);
        activity.setTarget_user_ids(targetType == TARGET_TYPE_USERS ? toJsonArray(normalizedUserIds) : "[]");
        activity.setStatus(STATUS_PENDING);
        activity.setRecipient_count(0);
        activity.setCreated_by(adminId);
        activity.setFailed_reason(null);
        activity.setSent_at(null);
        activity.setCreated_at(LocalDateTime.now());
        activity.setUpdated_at(LocalDateTime.now());
        save(activity);

        // 同步分发（PHP 用 Redis 队列异步，Java 版暂同步执行）
        try {
            dispatch(activity, normalizedUserIds);
        } catch (Exception e) {
            markFailed(activity, e);
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "发送失败：" + e.getMessage());
        }

        return getById(activity.getId());
    }

    /**
     * 分发活动消息给目标用户
     */
    private void dispatch(ActivityMessage activity, List<Integer> targetUserIds) {
        Integer activityId = activity.getId();
        String preview = (activity.getSummary() != null && !activity.getSummary().isBlank())
                ? activity.getSummary() : activity.getTitle();
        LocalDateTime now = LocalDateTime.now();
        int recipientCount = 0;

        List<Integer> recipientIds;
        if (activity.getTarget_type() == TARGET_TYPE_ALL) {
            // 全量发送：查询所有正常状态用户
            LambdaQueryWrapper<Users> w = new LambdaQueryWrapper<>();
            w.eq(Users::getStatus, 0).select(Users::getId);
            recipientIds = usersService.list(w).stream()
                    .map(Users::getId)
                    .collect(Collectors.toList());
        } else {
            // 指定用户：过滤出正常状态用户
            // 空目标 = 不发送（对齐 PHP insertActivityMessages 的 empty($userIds) 提前返回，
            // 且避免 MyBatis-Plus 对空集合生成 IN () 导致 SQL 语法错误）
            if (targetUserIds == null || targetUserIds.isEmpty()) {
                recipientIds = Collections.emptyList();
            } else {
                LambdaQueryWrapper<Users> w = new LambdaQueryWrapper<>();
                w.eq(Users::getStatus, 0)
                        .in(Users::getId, targetUserIds)
                        .select(Users::getId);
                recipientIds = usersService.list(w).stream()
                        .map(Users::getId)
                        .collect(Collectors.toList());
            }
        }

        // 批量插入消息
        for (Integer userId : recipientIds) {
            Message msg = new Message();
            msg.setUser_id(0); // system
            msg.setTo_user_id(userId);
            msg.setActivity_message_id(activityId);
            msg.setContent(preview);
            msg.setType(MessageService.TYPE_ACTIVITY);
            msg.setIs_read(0);
            msg.setCreated_at(now);
            msg.setUpdated_at(now);
            messageService.save(msg);
            recipientCount++;
        }

        // 更新活动消息状态
        activity.setStatus(STATUS_SENT);
        activity.setRecipient_count(recipientCount);
        activity.setFailed_reason(null);
        activity.setSent_at(now);
        activity.setUpdated_at(now);
        updateById(activity);
    }

    private void markFailed(ActivityMessage activity, Exception e) {
        String reason = e.getMessage();
        if (reason != null && reason.length() > 250) {
            reason = reason.substring(0, 250) + "...";
        }
        activity.setStatus(STATUS_FAILED);
        activity.setFailed_reason(reason);
        activity.setUpdated_at(LocalDateTime.now());
        updateById(activity);
    }

    @Override
    public String buildSummary(int contentType, String url, String content) {
        if (contentType == CONTENT_TYPE_URL) {
            return "点击查看活动详情";
        }
        // 去除 HTML 标签
        String plain = content == null ? "" : content.replaceAll("<[^>]+>", "").trim();
        plain = plain.replaceAll("\\s+", " ");
        if (plain.isEmpty()) {
            return "点击查看活动详情";
        }
        return plain.length() > 120 ? plain.substring(0, 120) + "..." : plain;
    }

    @Override
    public Map<String, Object> buildActivityMeta(ActivityMessage activity) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("id", String.valueOf(activity.getId()));
        meta.put("title", activity.getTitle() != null ? activity.getTitle().trim() : "");
        String cover = activity.getCover() != null ? activity.getCover().trim() : "";
        meta.put("cover", normalizeCoverUrl(cover));
        meta.put("summary", activity.getSummary() != null ? activity.getSummary().trim() : "");
        meta.put("content_type", activity.getContent_type() != null ? activity.getContent_type() : 0);
        meta.put("content_type_text", contentTypeText(activity.getContent_type()));
        meta.put("url", activity.getUrl() != null ? activity.getUrl().trim() : "");
        return meta;
    }

    @Override
    public List<Map<String, Object>> searchUsers(String keyword, int limit) {
        int effectiveLimit = Math.max(1, Math.min(50, limit));
        LambdaQueryWrapper<Users> w = new LambdaQueryWrapper<>();
        w.eq(Users::getStatus, 0)
                .select(Users::getId, Users::getNickname, Users::getUsername, Users::getAvatar);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            w.and(qw -> qw.like(Users::getNickname, kw).or().like(Users::getUsername, kw));
        }
        w.orderByDesc(Users::getCreated_at).last("LIMIT " + effectiveLimit);
        List<Users> users = usersService.list(w);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Users u : users) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(u.getId()));
            item.put("nickname", u.getNickname() != null ? u.getNickname() : "");
            item.put("username", u.getUsername() != null ? u.getUsername() : "");
            item.put("avatar", u.getAvatar() != null ? u.getAvatar() : "");
            String label = u.getNickname();
            if (label == null || label.isBlank()) {
                label = u.getUsername();
            }
            if (label == null || label.isBlank()) {
                label = "未知用户";
            }
            item.put("label", label.trim());
            result.add(item);
        }
        return result;
    }

    // ===================== 私有辅助 =====================

    private List<Integer> normalizeTargetUserIds(int targetType, List<Integer> userIds) {
        if (targetType != TARGET_TYPE_USERS) {
            return Collections.emptyList();
        }
        if (userIds == null) {
            return Collections.emptyList();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String toJsonArray(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ids.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private String contentTypeText(Integer contentType) {
        if (contentType == null) return "";
        return switch (contentType) {
            case CONTENT_TYPE_URL -> "链接";
            case CONTENT_TYPE_RICHTEXT -> "富文本";
            default -> "";
        };
    }

    private String normalizeCoverUrl(String cover) {
        if (cover == null || cover.isEmpty()) {
            return "";
        }
        // 简化：非 URL 路径原样返回（PHP 版调 FileService::getFileUrl）
        return cover;
    }
}
