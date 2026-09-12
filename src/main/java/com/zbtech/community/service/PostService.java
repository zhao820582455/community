package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.Post;

import java.util.List;
import java.util.Map;

/**
 * 帖子业务（对齐原 foxbook api/controller/Post + PostInteractionService）
 */
public interface PostService extends IService<Post> {

    /** 帖子列表（分页，支持 category_id/user_id/discuss_id 过滤，置顶优先） */
    IPage<Post> getListByPage(int page, int pageSize, Map<String, Object> queryForm);

    /** 发布帖子 */
    Post publish(Integer userId, Map<String, Object> params);

    /** 帖子详情（含当前用户点赞/收藏状态，浏览量 +1） */
    Post getInfo(Integer userId, Integer postId);

    /** 点赞/取消点赞（事务 + 行锁，计数防漂移） */
    Map<String, Object> like(Integer userId, Integer postId);

    /** 收藏/取消收藏（事务 + 行锁，计数防漂移） */
    Map<String, Object> favorite(Integer userId, Integer postId);

    /** 当前用户发布的帖子列表 */
    IPage<Post> currentUserPosts(int page, int pageSize, Integer userId);

    /** 某用户收藏的帖子列表 */
    IPage<Post> favoritesByUser(int page, int pageSize, Integer userId);

    /** 某用户点赞的帖子列表 */
    IPage<Post> likesByUser(int page, int pageSize, Integer userId);

    /** 搜索帖子（标题/内容 like，limit 10），返回简要 VO */
    List<com.zbtech.community.vo.PostBriefVO> searchPosts(String keyword);

    /** 管理端帖子列表（分页，支持 title/content like、user_id/is_top 精确、created_at 日期范围） */
    IPage<Post> getAdminListByPage(int page, int pageSize, Map<String, Object> queryForm);

    /** 管理端帖子详情（含作者简要 user_id/nickname/avatar） */
    Map<String, Object> getAdminPostDetail(Integer id);

    /** 管理端置顶/取消置顶（置顶写 top_at，取消置 null） */
    void setTop(Integer id, Integer isTop);

    /** 管理端删除帖子（事务内级联删除评论/收藏/点赞，回退作者 post_count，发送系统消息） */
    void deletePost(Integer id);

    /** 管理端批量删除帖子（逻辑同 deletePost） */
    void batchDeletePosts(List<Integer> ids);
}
