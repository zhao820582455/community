package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.Comments;
import com.zbtech.community.vo.CommentVO;

import java.util.List;
import java.util.Map;

/**
 * 评论业务（对齐原 foxbook service/CommentService）
 */
public interface CommentsService extends IService<Comments> {

    /**
     * 发布评论（支持楼中楼回复，事务内行锁防计数漂移）
     *
     * @param userId     评论人ID
     * @param postId     帖子ID
     * @param content    评论内容
     * @param parentId   父评论ID（根评论传 0）
     * @param toUserId   被回复用户ID（可选，缺省取父评论作者或帖子作者）
     * @return 新建的评论（含解析后的 to_user_id）
     */
    Comments createComment(Integer userId, Integer postId, String content,
                           Integer parentId, Integer toUserId);

    /**
     * 点赞/取消点赞评论（行锁 + 计数 ±1）
     *
     * @return {comment: Comments(带关联), active: boolean}
     */
    Map<String, Object> toggleLike(Integer userId, Integer commentId);

    /** 根评论分页列表（parent_id = 0），附带 user/to_user 与当前用户点赞态 */
    IPage<CommentVO> getParentComments(Integer postId, Integer viewerUserId, int page, int pageSize);

    /** 某根评论下的回复分页列表，可排除某条评论 */
    IPage<CommentVO> getThreadReplies(Integer rootId, Integer viewerUserId, int page, int pageSize,
                                      Integer excludeCommentId);

    /** 级联删除评论及其所有子孙，并回退帖子/根评论计数，返回实际删除条数 */
    int deleteComments(List<Integer> ids);

    /** 级联删除某帖子下的全部评论（含子孙）并回退计数，返回实际删除条数 */
    int deleteCommentsByPostId(Integer postId);

    /** 查询单条评论并组装为 VO（附带 user/to_user/is_liked） */
    CommentVO getCommentVO(Integer commentId, Integer viewerUserId);
}
