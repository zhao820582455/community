package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.Comments;
import com.zbtech.community.service.CommentsService;
import com.zbtech.community.vo.CommentVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 评论表(树形) - 管理端
 * （对齐原 foxbook app/adminapi/controller/Comment.php；删除走级联删除并回退计数）
 */
@RestController
@RequestMapping("/adminapi/comment")
@Tag(name = "评论管理")
public class CommentsAdminController extends BaseController {

    @Autowired
    private CommentsService commentsService;

    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize) {
        return pageResult(commentsService.page(toPage(page, pageSize)));
    }

    @GetMapping("/getList")
    public Result<?> getList() {
        return Result.success(commentsService.list());
    }

    /** 评论详情（附带 user/to_user 简要） */
    @GetMapping("/getDetail")
    public Result<?> getDetail(@RequestParam Integer id) {
        CommentVO vo = commentsService.getCommentVO(id, null);
        if (vo == null) {
            return Result.error("评论不存在");
        }
        return Result.success(vo);
    }

    /** 新增评论（业务创建：校验帖子/父评论、解析被回复人、更新计数） */
    @PostMapping("/createComment")
    public Result<?> createComment(@RequestBody Comments entity) {
        if (entity.getPost_id() == null || entity.getContent() == null
                || entity.getContent().isBlank() || entity.getUser_id() == null) {
            return Result.error("用户ID、帖子ID、评论内容必填");
        }
        Comments saved = commentsService.createComment(entity.getUser_id(), entity.getPost_id(),
                entity.getContent(), entity.getParent_id(), entity.getTo_user_id());
        return Result.success("创建评论成功", saved);
    }

    /** 兼容通用组件的新增/编辑（编辑仅更新内容；新增走业务创建） */
    @PostMapping("/save")
    public Result<?> save(@RequestBody Comments entity) {
        if (entity.getId() == null) {
            if (entity.getPost_id() == null || entity.getContent() == null
                    || entity.getContent().isBlank() || entity.getUser_id() == null) {
                return Result.error("用户ID、帖子ID、评论内容必填");
            }
            Comments saved = commentsService.createComment(entity.getUser_id(), entity.getPost_id(),
                    entity.getContent(), entity.getParent_id(), entity.getTo_user_id());
            return Result.success(saved);
        }
        commentsService.updateById(entity);
        return Result.success();
    }

    @PostMapping("/updateComment")
    public Result<?> updateComment(@RequestBody Map<String, Object> body) {
        Integer id = toInt(body.get("id"));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        if (id == null || content == null || content.isBlank() || content.length() > 255) {
            return Result.error("评论ID与内容(<=255)必填");
        }
        Comments comment = commentsService.getById(id);
        if (comment == null) {
            return Result.error("评论不存在");
        }
        comment.setContent(content);
        commentsService.updateById(comment);
        return Result.success("更新评论成功", comment);
    }

    /** 删除单条评论（级联删除子孙并回退计数） */
    @PostMapping("/delete")
    public Result<?> delete(@RequestParam Integer id) {
        int n = commentsService.deleteComments(Collections.singletonList(id));
        if (n == 0) {
            return Result.error("评论不存在");
        }
        Map<String, Object> res = new HashMap<>(1);
        res.put("deleted_count", n);
        return Result.success("删除评论成功", res);
    }

    /** 批量删除评论 */
    @PostMapping("/batchDelete")
    public Result<?> batchDelete(@RequestBody java.util.List<Integer> ids) {
        int n = commentsService.deleteComments(ids);
        if (n == 0) {
            return Result.error("未找到要删除的评论");
        }
        Map<String, Object> res = new HashMap<>(1);
        res.put("deleted_count", n);
        return Result.success("批量删除成功", res);
    }

    /** 更新评论状态（桩：当前表结构无 status 字段，对齐 PHP 422） */
    @PostMapping("/updateCommentStatus")
    public Result<?> updateCommentStatus(@RequestBody Map<String, Object> body) {
        return Result.error(422, "当前评论数据结构未包含 status 字段，无法更新状态");
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
