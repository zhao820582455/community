package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.service.PostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 帖子管理端（对齐原 foxbook app/adminapi/controller/Post.php）
 * 路径规则：/adminapi/post/{action}
 */
@RestController
@RequestMapping("/adminapi/post")
@Tag(name = "帖子管理")
public class PostAdminController extends BaseController {

    @Autowired
    private PostService postService;

    /** GET /adminapi/post/getListByPage 管理端列表（标题/内容 like、user_id/is_top 精确、created_at 日期范围） */
    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(required = false) String title,
                                   @RequestParam(required = false) String content,
                                   @RequestParam(required = false) Integer userId,
                                   @RequestParam(required = false) Integer isTop,
                                   @RequestParam(required = false) String createdAtStart,
                                   @RequestParam(required = false) String createdAtEnd) {
        Map<String, Object> queryForm = new HashMap<>(6);
        if (title != null) {
            queryForm.put("title", title);
        }
        if (content != null) {
            queryForm.put("content", content);
        }
        if (userId != null) {
            queryForm.put("user_id", userId);
        }
        if (isTop != null) {
            queryForm.put("is_top", isTop);
        }
        if (createdAtStart != null) {
            queryForm.put("created_at_start", createdAtStart);
        }
        if (createdAtEnd != null) {
            queryForm.put("created_at_end", createdAtEnd);
        }
        return pageResult(postService.getAdminListByPage(page, pageSize, queryForm));
    }

    /** GET /adminapi/post/getPostDetail 帖子详情（含作者简要） */
    @GetMapping("/getPostDetail")
    public Result<?> getPostDetail(@RequestParam Integer id) {
        return Result.success(postService.getAdminPostDetail(id));
    }

    /** POST /adminapi/post/setTop 置顶/取消置顶 */
    @PostMapping("/setTop")
    public Result<?> setTop(@RequestBody Map<String, Object> body) {
        Integer id = toInt(body.get("id"));
        Integer isTop = toInt(body.get("is_top"));
        if (id == null || isTop == null) {
            return Result.error("帖子ID与置顶状态必填");
        }
        postService.setTop(id, isTop);
        return Result.success(isTop == 1 ? "置顶成功" : "已取消置顶");
    }

    /** POST /adminapi/post/deletePost 删除帖子（级联） */
    @PostMapping("/deletePost")
    public Result<?> deletePost(@RequestParam Integer id) {
        postService.deletePost(id);
        return Result.success("删除成功");
    }

    /** POST /adminapi/post/batchDeletePosts 批量删除帖子（级联） */
    @PostMapping("/batchDeletePosts")
    public Result<?> batchDeletePosts(@RequestBody List<Integer> ids) {
        postService.batchDeletePosts(ids);
        return Result.success("批量删除成功");
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
