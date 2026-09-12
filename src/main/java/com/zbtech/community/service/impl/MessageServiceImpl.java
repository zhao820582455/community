package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.entity.ActivityMessage;
import com.zbtech.community.entity.Message;
import com.zbtech.community.entity.Post;
import com.zbtech.community.entity.Users;
import com.zbtech.community.mapper.MessageMapper;
import com.zbtech.community.mapper.PostMapper;
import com.zbtech.community.service.ActivityMessageService;
import com.zbtech.community.service.MessageService;
import com.zbtech.community.service.UsersService;
import com.zbtech.community.vo.MessageVO;
import com.zbtech.community.vo.PostBriefVO;
import com.zbtech.community.vo.UserBriefVO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final UsersService usersService;
    private final PostMapper postMapper;
    private final ActivityMessageService activityMessageService;

    public MessageServiceImpl(UsersService usersService, PostMapper postMapper,
                             ActivityMessageService activityMessageService) {
        this.usersService = usersService;
        this.postMapper = postMapper;
        this.activityMessageService = activityMessageService;
    }

    @Override
    public boolean sendMessage(Integer userId, Integer toUserId, String content, int type, Integer postId) {
        // 基础参数校验
        if (userId == null || toUserId == null || content == null || content.isBlank()
                || content.length() > 255) {
            return false;
        }
        if (type != TYPE_LIKE && type != TYPE_FAVORITE && type != TYPE_COMMENT
                && type != TYPE_FOLLOW && type != TYPE_SYSTEM && type != TYPE_ACTIVITY) {
            return false;
        }
        // 不能自己给自己发（系统消息除外）
        if (userId.equals(toUserId) && type != TYPE_SYSTEM) {
            return false;
        }
        // 点赞/收藏必须有 postId
        if ((type == TYPE_LIKE || type == TYPE_FAVORITE) && postId == null) {
            return false;
        }
        // 关注不应带 postId
        if (type == TYPE_FOLLOW && postId != null) {
            return false;
        }

        // 去重（评论/系统/活动消息允许重复，不去重）
        Message existing = checkDuplicate(userId, toUserId, type, postId);
        if (existing != null) {
            existing.setUpdated_at(java.time.LocalDateTime.now());
            updateById(existing);
            return true;
        }

        Message msg = new Message();
        msg.setUser_id(userId);
        msg.setTo_user_id(toUserId);
        msg.setPost_id(postId);
        msg.setContent(content);
        msg.setType(type);
        msg.setIs_read(0);
        save(msg);
        return true;
    }

    @Override
    public Map<String, Object> getUnreadCount(Integer userId, List<Integer> type) {
        List<Message> unread = list(new LambdaQueryWrapper<Message>()
                .eq(Message::getTo_user_id, userId).eq(Message::getIs_read, 0));
        Map<Integer, Long> counts = unread.stream()
                .collect(Collectors.groupingBy(Message::getType, Collectors.counting()));

        Map<String, Object> res = new HashMap<>(10);
        res.put("total", (long) unread.size());
        res.put("like_count", counts.getOrDefault(TYPE_LIKE, 0L));
        res.put("favorite_count", counts.getOrDefault(TYPE_FAVORITE, 0L));
        res.put("comment_count", counts.getOrDefault(TYPE_COMMENT, 0L));
        res.put("follow_count", counts.getOrDefault(TYPE_FOLLOW, 0L));
        res.put("system_count", counts.getOrDefault(TYPE_SYSTEM, 0L));
        res.put("activity_count", counts.getOrDefault(TYPE_ACTIVITY, 0L));

        Message newSystem = getOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getTo_user_id, userId).eq(Message::getType, TYPE_SYSTEM)
                .orderByDesc(Message::getCreated_at).last("LIMIT 1"));
        res.put("new_system", newSystem == null ? null : toVO(newSystem));

        Message newActivity = getOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getTo_user_id, userId).eq(Message::getType, TYPE_ACTIVITY)
                .orderByDesc(Message::getCreated_at).last("LIMIT 1"));
        res.put("new_activity", newActivity == null ? null : toVO(newActivity));

        return res;
    }

    @Override
    public IPage<MessageVO> getUserMsgList(Integer userId, List<Integer> type, int page, int pageSize) {
        Page<Message> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Message> w = new LambdaQueryWrapper<>();
        w.eq(Message::getTo_user_id, userId).orderByDesc(Message::getCreated_at);
        applyTypeFilter(w, type);
        IPage<Message> pr = page(p, w);
        List<MessageVO> vos = toVOList(pr.getRecords());
        Page<MessageVO> result = new Page<>(pr.getCurrent(), pr.getSize(), pr.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public int markAllAsRead(Integer userId, List<Integer> type) {
        LambdaQueryWrapper<Message> w = new LambdaQueryWrapper<>();
        w.eq(Message::getTo_user_id, userId).eq(Message::getIs_read, 0);
        applyTypeFilter(w, type);
        Message upd = new Message();
        upd.setIs_read(1);
        return baseMapper.update(upd, w);
    }

    @Override
    public boolean markAsRead(Integer userId, Integer messageId) {
        Message m = getById(messageId);
        if (m == null || !Objects.equals(m.getTo_user_id(), userId)) {
            return false;
        }
        if (m.getIs_read() != null && m.getIs_read() == 1) {
            return true;
        }
        m.setIs_read(1);
        updateById(m);
        return true;
    }

    // ===================== 私有 =====================

    private void applyTypeFilter(LambdaQueryWrapper<Message> w, List<Integer> type) {
        if (type != null && !type.isEmpty()) {
            w.in(Message::getType, type);
        }
    }

    /**
     * 重复消息检查（对齐原 PHP MessageService::checkDuplicateMessage）
     * 去重规则：
     * - 点赞/收藏：基于用户关系 + 具体帖子去重
     * - 关注：仅基于用户关系去重（忽略 post_id）
     * - 评论/系统/活动：不去重，允许重复发送
     */
    private Message checkDuplicate(Integer userId, Integer toUserId, int type, Integer postId) {
        LambdaQueryWrapper<Message> w = new LambdaQueryWrapper<Message>()
                .eq(Message::getUser_id, userId)
                .eq(Message::getTo_user_id, toUserId)
                .eq(Message::getType, type);
        if (type == TYPE_LIKE || type == TYPE_FAVORITE) {
            w.eq(Message::getPost_id, postId);
        } else if (type != TYPE_FOLLOW) {
            // 评论/系统/活动消息不去重
            return null;
        }
        return getOne(w, false);
    }

    private List<MessageVO> toVOList(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> userIds = new HashSet<>();
        Set<Integer> postIds = new HashSet<>();
        Set<Integer> actIds = new HashSet<>();
        for (Message m : messages) {
            if (m.getUser_id() != null) userIds.add(m.getUser_id());
            if (m.getTo_user_id() != null) userIds.add(m.getTo_user_id());
            if (m.getPost_id() != null) postIds.add(m.getPost_id());
            if (m.getType() != null && m.getType() == TYPE_ACTIVITY && m.getActivity_message_id() != null) {
                actIds.add(m.getActivity_message_id());
            }
        }
        Map<Integer, UserBriefVO> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : usersService.listByIds(userIds).stream()
                .collect(Collectors.toMap(Users::getId, this::toBrief, (a, b) -> a));
        Map<Integer, PostBriefVO> postMap = postIds.isEmpty() ? Collections.emptyMap()
                : postMapper.selectBatchIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, this::toPostBrief, (a, b) -> a));
        Map<Integer, ActivityMessage> actMap = actIds.isEmpty() ? Collections.emptyMap()
                : activityMessageService.listByIds(actIds).stream()
                .collect(Collectors.toMap(ActivityMessage::getId, Function.identity(), (a, b) -> a));

        List<MessageVO> vos = new ArrayList<>();
        for (Message m : messages) {
            vos.add(buildVO(m, userMap, postMap, actMap));
        }
        return vos;
    }

    private MessageVO toVO(Message m) {
        return buildVO(m, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    private MessageVO buildVO(Message m, Map<Integer, UserBriefVO> userMap,
                              Map<Integer, PostBriefVO> postMap, Map<Integer, ActivityMessage> actMap) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setUser_id(m.getUser_id());
        vo.setTo_user_id(m.getTo_user_id());
        vo.setPost_id(m.getPost_id());
        vo.setContent(m.getContent());
        vo.setType(m.getType());
        vo.setIs_read(m.getIs_read());
        vo.setCreated_at(m.getCreated_at());
        if (m.getUser_id() != null) {
            UserBriefVO u = userMap.get(m.getUser_id());
            vo.setUser(u != null ? u : toBrief(usersService.getById(m.getUser_id())));
        }
        if (m.getTo_user_id() != null) {
            UserBriefVO t = userMap.get(m.getTo_user_id());
            vo.setTo_user(t != null ? t : toBrief(usersService.getById(m.getTo_user_id())));
        }
        if (m.getPost_id() != null) {
            PostBriefVO p = postMap.get(m.getPost_id());
            PostBriefVO built = p != null ? p : toPostBrief(postMapper.selectById(m.getPost_id()));
            vo.setPost(built);
        }
        if (m.getType() != null && m.getType() == TYPE_ACTIVITY && m.getActivity_message_id() != null) {
            ActivityMessage act = actMap.get(m.getActivity_message_id());
            if (act == null) {
                act = activityMessageService.getById(m.getActivity_message_id());
            }
            if (act != null) {
                vo.setActivity_meta(buildActivityMeta(act));
            }
        }
        return vo;
    }

    private Map<String, Object> buildActivityMeta(ActivityMessage act) {
        Map<String, Object> meta = new HashMap<>(8);
        meta.put("id", act.getId());
        meta.put("title", act.getTitle());
        meta.put("cover", act.getCover());
        meta.put("summary", act.getSummary());
        meta.put("content_type", act.getContent_type());
        meta.put("content_type_text", act.getContent_type() != null && act.getContent_type() == 1 ? "URL" : "富文本");
        meta.put("url", act.getUrl());
        return meta;
    }

    private UserBriefVO toBrief(Users u) {
        if (u == null) {
            return null;
        }
        UserBriefVO b = new UserBriefVO();
        b.setId(u.getId());
        b.setNickname(u.getNickname());
        b.setAvatar(u.getAvatar());
        return b;
    }

    private PostBriefVO toPostBrief(Post p) {
        if (p == null) {
            return null;
        }
        PostBriefVO b = new PostBriefVO();
        b.setId(p.getId());
        b.setTitle(p.getTitle());
        return b;
    }
}
