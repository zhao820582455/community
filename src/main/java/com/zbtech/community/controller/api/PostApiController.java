package com.zbtech.community.controller.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.MediaCheck;
import com.zbtech.community.entity.Post;
import com.zbtech.community.entity.Users;
import com.zbtech.community.mapper.MediaCheckMapper;
import com.zbtech.community.service.PostService;
import com.zbtech.community.service.UsersService;
import com.zbtech.community.service.WechatMpService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端帖子接口（对齐原 foxbook api/controller/Post）
 * 路径规则：/api/post/{action}
 */
@RestController
@RequestMapping("/api/post")
public class PostApiController extends BaseController {

    @Resource
    private PostService postService;
    @Resource
    private UsersService usersService;
    @Resource
    private WechatMpService wechatMpService;
    @Resource
    private MediaCheckMapper mediaCheckMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** GET|POST /api/post/getListByPage（对齐 PHP：$request->input 不区分方法，前端统一 POST + JSON body） */
    @RequestMapping(value = "/getListByPage", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(required = false) Integer categoryId,
                                   @RequestParam(required = false) Integer userId,
                                   @RequestParam(required = false) Integer discussId,
                                   @RequestBody(required = false) Map<String, Object> body) {
        if (body != null) {
            Object bodyPage = body.get("page");
            if (bodyPage != null) {
                Integer v = intOf(bodyPage);
                if (v != null) page = v;
            }
            Object bodyPageSize = body.get("pageSize");
            if (bodyPageSize != null) {
                Integer v = intOf(bodyPageSize);
                if (v != null) pageSize = v;
            }
            Object qf = body.get("queryForm");
            if (qf instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> qm = (Map<String, Object>) qf;
                if (categoryId == null && qm.get("category_id") != null) {
                    categoryId = intOf(qm.get("category_id"));
                }
                if (userId == null && qm.get("user_id") != null) {
                    userId = intOf(qm.get("user_id"));
                }
                if (discussId == null && qm.get("discuss_id") != null) {
                    discussId = intOf(qm.get("discuss_id"));
                }
            }
        }
        Map<String, Object> queryForm = new HashMap<>(4);
        if (categoryId != null) {
            queryForm.put("category_id", categoryId);
        }
        if (userId != null) {
            queryForm.put("user_id", userId);
        }
        if (discussId != null) {
            queryForm.put("discuss_id", discussId);
        }
        IPage<Post> p = postService.getListByPage(page, pageSize, queryForm);
        return pageResult(p);
    }

    /** POST /api/post/publish */
    @PostMapping("/publish")
    public Result<Post> publish(@RequestBody Map<String, Object> params) {
        Integer userId = getUserId();
        return Result.success(postService.publish(userId, params));
    }

    /** GET /api/post/getInfo */
    @GetMapping("/getInfo")
    public Result<Post> getInfo(@RequestParam Integer id) {
        Integer userId = getUserIdOptional();
        return Result.success(postService.getInfo(userId, id));
    }

    /** POST /api/post/like */
    @PostMapping("/like")
    public Result<?> like(@RequestBody Map<String, Object> params) {
        Integer userId = getUserId();
        Integer postId = intOf(params.get("post_id"));
        return Result.success(postService.like(userId, postId));
    }

    /** POST /api/post/favorite */
    @PostMapping("/favorite")
    public Result<?> favorite(@RequestBody Map<String, Object> params) {
        Integer userId = getUserId();
        Integer postId = intOf(params.get("post_id"));
        return Result.success(postService.favorite(userId, postId));
    }

    /**
     * POST /api/post/wechatCheckImg 发帖图片微信内容安全异步审核
     * （对齐原 PHP Post::wechatCheckImg：取帖子 media 图片列表，逐张提交 media_check_async，
     * 审核记录写入 lb_media_check）
     */
    @PostMapping("/wechatCheckImg")
    public Result<?> wechatCheckImg(@RequestBody Map<String, Object> params) {
        Integer userId = getUserId();
        Integer postId = intOf(params.get("post_id"));
        if (postId == null) {
            return Result.error("ID必填");
        }
        Users user = usersService.getById(userId);
        Post post = postService.getById(postId);
        if (user == null || post == null) {
            return Result.error("数据不存在");
        }

        List<String> imgList = parseMediaList(post.getMedia());
        for (String src : imgList) {
            String fullUrl = normalizeMediaUrl(src);
            String traceId = wechatMpService.mediaCheckAsync(user.getOpenid(), fullUrl);

            MediaCheck record = new MediaCheck();
            record.setTrace_id(traceId);
            record.setPost_id(postId);
            record.setMedia_src(fullUrl);
            record.setCreated_at(LocalDateTime.now());
            mediaCheckMapper.insert(record);
        }
        return Result.success("图片已提交审核");
    }

    /** 解析 Post.media（JSON 数组字符串），非 JSON 数组时按单元素处理 */
    private List<String> parseMediaList(String media) {
        if (media == null || media.isBlank()) {
            return java.util.Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(media, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return java.util.Collections.singletonList(media.trim());
        }
    }

    /** 对齐 PHP FileService::getFileUrl：http(s) 开头原样返回，否则原样返回（OSS 相对路径场景待配置拼接） */
    private String normalizeMediaUrl(String src) {
        if (src == null) {
            return "";
        }
        String s = src.trim();
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return s;
        }
        return s;
    }

    /** GET /api/post/currentUser 当前用户发布的帖子 */
    @GetMapping("/currentUser")
    public Result<?> currentUser(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        Integer userId = getUserId();
        return pageResult(postService.currentUserPosts(page, pageSize, userId));
    }

    /** GET /api/post/favorites 当前用户收藏的帖子 */
    @GetMapping("/favorites")
    public Result<?> favorites(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int pageSize) {
        Integer userId = getUserId();
        return pageResult(postService.favoritesByUser(page, pageSize, userId));
    }

    /** GET /api/post/likes 当前用户点赞的帖子 */
    @GetMapping("/likes")
    public Result<?> likes(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Integer userId = getUserId();
        return pageResult(postService.likesByUser(page, pageSize, userId));
    }

    /** GET /api/post/favoritesByUserId 某用户收藏的帖子 */
    @GetMapping("/favoritesByUserId")
    public Result<?> favoritesByUserId(@RequestParam Integer userId,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        return pageResult(postService.favoritesByUser(page, pageSize, userId));
    }

    /** GET /api/post/likesByUserId 某用户点赞的帖子 */
    @GetMapping("/likesByUserId")
    public Result<?> likesByUserId(@RequestParam Integer userId,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize) {
        return pageResult(postService.likesByUser(page, pageSize, userId));
    }

    private Integer intOf(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
