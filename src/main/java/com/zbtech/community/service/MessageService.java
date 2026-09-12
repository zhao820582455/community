package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.Message;
import com.zbtech.community.vo.MessageVO;

import java.util.List;
import java.util.Map;

/**
 * 站内消息业务（对齐原 foxbook service/MessageService）
 */
public interface MessageService extends IService<Message> {

    int TYPE_LIKE = 1;
    int TYPE_FAVORITE = 2;
    int TYPE_COMMENT = 3;
    int TYPE_FOLLOW = 4;
    int TYPE_SYSTEM = 9;
    int TYPE_ACTIVITY = 10;

    /**
     * 发送站内消息
     *
     * @param userId   发起动作的用户ID（点赞人/评论人等）
     * @param toUserId 接收用户ID（帖子作者等）
     * @param content  消息内容
     * @param type     消息类型
     * @param postId   相关帖子ID（点赞/收藏/评论必填）
     * @return 是否处理成功（参数非法、自己发给自己、重复去重命中均视为已处理返回 true）
     */
    boolean sendMessage(Integer userId, Integer toUserId, String content, int type, Integer postId);

    /**
     * 未读消息统计：总量 + 按类型分组 + 最新系统/活动消息
     * @param type 类型过滤（null 表示全部）
     */
    Map<String, Object> getUnreadCount(Integer userId, List<Integer> type);

    /** 消息分页列表（组装 user/to_user/post/activity_meta） */
    IPage<MessageVO> getUserMsgList(Integer userId, List<Integer> type, int page, int pageSize);

    /** 标记全部未读为已读，返回影响行数 */
    int markAllAsRead(Integer userId, List<Integer> type);

    /** 标记单条消息已读，返回是否成功（消息不存在或无权限返回 false） */
    boolean markAsRead(Integer userId, Integer messageId);
}
