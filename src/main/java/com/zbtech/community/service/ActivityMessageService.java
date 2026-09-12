package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.ActivityMessage;
import com.zbtech.community.entity.Users;

import java.util.List;
import java.util.Map;

/**
 * 活动消息业务（对齐原 foxbook service/ActivityMessageService + adminapi/controller/ActivityMessage）
 */
public interface ActivityMessageService extends IService<ActivityMessage> {

    // 内容类型
    int CONTENT_TYPE_URL = 1;
    int CONTENT_TYPE_RICHTEXT = 2;

    // 推送范围
    int TARGET_TYPE_ALL = 1;
    int TARGET_TYPE_USERS = 2;

    // 状态
    int STATUS_PENDING = 0;
    int STATUS_SENT = 1;
    int STATUS_FAILED = 2;

    /**
     * 分页列表，支持搜索：title 模糊、status/target_type/content_type 精确、created_at 日期区间
     */
    IPage<ActivityMessage> getListByPage(int page, int pageSize, String title,
                                          Integer status, Integer targetType, Integer contentType,
                                          String createdStart, String createdEnd);

    /** 获取活动消息，不存在抛异常 */
    ActivityMessage getActivityOrFail(Integer id);

    /**
     * 创建活动消息并分发给目标用户（对齐 PHP createAndDispatch）
     * 同步执行分发（PHP 用 Redis 队列异步，Java 版暂同步）
     *
     * @param title       标题
     * @param cover       封面
     * @param contentType 内容类型 1URL 2富文本
     * @param url         活动地址
     * @param content     富文本内容
     * @param targetType  推送范围 1全部 2指定用户
     * @param userIds     指定用户ID列表（targetType=2 时必填）
     * @param adminId     创建管理员ID
     * @return 创建的活动消息
     */
    ActivityMessage createAndDispatch(String title, String cover, int contentType,
                                       String url, String content, int targetType,
                                       List<Integer> userIds, Integer adminId);

    /**
     * 构建活动消息摘要（对齐 PHP buildSummary）
     */
    String buildSummary(int contentType, String url, String content);

    /**
     * 构建活动消息元数据（对齐 PHP buildActivityMeta）
     */
    Map<String, Object> buildActivityMeta(ActivityMessage activity);

    /**
     * 搜索用户（管理端选择推送目标用户）
     *
     * @param keyword 关键词（匹配 nickname/username）
     * @param limit   返回数量上限
     * @return 用户列表
     */
    List<Map<String, Object>> searchUsers(String keyword, int limit);
}
