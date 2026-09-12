package com.zbtech.community.controller.api;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.Comments;
import com.zbtech.community.service.CommentsService;
import com.zbtech.community.service.MessageService;
import com.zbtech.community.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 评论 - 用户端接口（对齐原 foxbook app/api/controller/Comments.php）
 * 路由前缀 /api/comments
 */
@RestController
@RequestMapping("/api/comments")
public class CommentsApiController extends BaseController {

    @Autowired
    private CommentsService commentsService;
    @Autowired
    private MessageService messageService;

    /**
     * 发布评论（支持楼中楼回复）
     */
    @PostMapping("/publish")
    public Result<?> publish(@RequestBody Map<String, Object> body) {
        Integer userId = getUserId();
        Integer postId = toInt(body.get("post_id"));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        Integer parentId = toInt(body.get("parent_id"));
        Integer toUserId = toInt(body.get("to_user_id"));

        if (postId == null || content == null || content.isBlank()) {
            return Result.error("帖子ID与评论内容必填");
        }
        // TODO 敏感词替换（对齐 SensitiveWordService::replaceText）与 内容安全审核（WeChatMnpService::msgSecCheck）待接入
        content = content.trim();

        Comments comment = commentsService.createComment(userId, postId, content, parentId, toUserId);
        if (!Objects.equals(userId, comment.getTo_user_id())) {
            messageService.sendMessage(userId, comment.getTo_user_id(), comment.getContent(),
                    MessageService.TYPE_COMMENT, postId);
        }

        CommentVO vo = commentsService.getCommentVO(comment.getId(), userId);
        return Result.success(vo);
    }

    /**
     * 根评论列表（分页）
     */
    @GetMapping("/getParentComments")
    public Result<?> getParentComments(@RequestParam Integer post_id,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        Integer viewer = getUserIdOptional();
        return pageResult(commentsService.getParentComments(post_id, viewer, page, pageSize));
    }

    /**
     * 某根评论下的回复列表（分页，可排除某条）
     */
    @GetMapping("/getChildComments")
    public Result<?> getChildComments(@RequestParam Integer parent_id,
                                      @RequestParam(required = false) Integer exclude_comment_id,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        Integer viewer = getUserIdOptional();
        return pageResult(commentsService.getThreadReplies(parent_id, viewer, page, pageSize, exclude_comment_id));
    }

    /**
     * 评论点赞 / 取消点赞
     */
    @PostMapping("/like")
    public Result<?> like(@RequestBody Map<String, Object> body) {
        Integer userId = getUserId();
        Integer commentId = toInt(body.get("comment_id"));
        if (commentId == null) {
            return Result.error("评论ID必填");
        }
        Map<String, Object> r = commentsService.toggleLike(userId, commentId);
        Comments comment = (Comments) r.get("comment");
        boolean active = Boolean.TRUE.equals(r.get("active"));

        if (active && comment != null && !Objects.equals(userId, comment.getUser_id())) {
            messageService.sendMessage(userId, comment.getUser_id(), "赞了你的评论",
                    MessageService.TYPE_LIKE, comment.getPost_id());
        }

        Map<String, Object> res = new HashMap<>(2);
        res.put("is_liked", active);
        res.put("like_count", comment == null ? 0 : (comment.getLike_count() == null ? 0 : comment.getLike_count()));
        return Result.success(active ? "点赞成功" : "取消点赞成功", res);
    }

    /**
     * 评论点赞/取消点赞 - 别名路由（对齐原 PHP Comments::toggleLike，前端实际调用此路径）
     */
    @PostMapping("/toggleLike")
    public Result<?> toggleLike(@RequestBody Map<String, Object> body) {
        return like(body);
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
