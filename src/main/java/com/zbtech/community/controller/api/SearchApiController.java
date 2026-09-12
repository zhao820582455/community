package com.zbtech.community.controller.api;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.service.DiscussService;
import com.zbtech.community.service.PostService;
import com.zbtech.community.service.UsersService;
import com.zbtech.community.vo.DiscussVO;
import com.zbtech.community.vo.PostBriefVO;
import com.zbtech.community.vo.UserBriefVO;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端搜索模块（对齐原 foxbook api/controller/Search::query）
 * 路由前缀 /api/search
 */
@RestController
@RequestMapping("/api/search")
public class SearchApiController extends BaseController {

    private final UsersService usersService;
    private final PostService postService;
    private final DiscussService discussService;

    public SearchApiController(UsersService usersService, PostService postService, DiscussService discussService) {
        this.usersService = usersService;
        this.postService = postService;
        this.discussService = discussService;
    }

    /** 关键词搜索：用户 / 帖子 / 话题，各 limit 10 */
    @GetMapping("/query")
    public Result<Map<String, Object>> query(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        List<UserBriefVO> users = usersService.searchUsers(keyword);
        List<PostBriefVO> posts = postService.searchPosts(keyword);
        List<DiscussVO> discusses = discussService.searchDiscusses(keyword);
        // 对齐 PHP Search：话题内容截断为 80 字摘要
        discusses.forEach(d -> {
            if (d != null) {
                d.setContent(buildSummary(d.getContent(), 80));
            }
        });
        Map<String, Object> res = new HashMap<>();
        res.put("keyword", keyword);
        res.put("users", users);
        res.put("posts", posts);
        res.put("discusses", discusses);
        res.put("user_count", users.size());
        res.put("post_count", posts.size());
        res.put("discuss_count", discusses.size());
        res.put("count", users.size() + posts.size() + discusses.size());
        return Result.success(res);
    }

    /** 对齐 PHP Search::buildSummary：纯文本压缩空白后按显示宽度截断，空返回空串 */
    private String buildSummary(String content, int limit) {
        if (content == null) {
            return "";
        }
        String plain = content.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
        if (plain.isEmpty()) {
            return "";
        }
        int width = 0;
        for (int i = 0; i < plain.length(); i++) {
            width += plain.charAt(i) > 0xFF ? 2 : 1;
            if (width > limit) {
                return plain.substring(0, i) + "...";
            }
        }
        return plain;
    }
}
