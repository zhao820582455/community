package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.Favorites;
import com.zbtech.community.entity.Likes;
import com.zbtech.community.entity.Post;
import com.zbtech.community.entity.Users;
import com.zbtech.community.mapper.FavoritesMapper;
import com.zbtech.community.mapper.LikesMapper;
import com.zbtech.community.mapper.PostMapper;
import com.zbtech.community.mapper.UsersMapper;
import com.zbtech.community.service.CommentsService;
import com.zbtech.community.service.MessageService;
import com.zbtech.community.service.PostContentService;
import com.zbtech.community.service.PostService;
import com.zbtech.community.vo.PostBriefVO;
import com.zbtech.community.vo.UserBriefVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统消息发送者哨兵：原 foxbook 以字面量 'system' 作为发送方 user_id，
 * Java 端 user_id 为整型，约定 0 表示系统（自增主键从 1 开始，0 安全）。
 */
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private static final int SYSTEM_USER_ID = 0;

    private final LikesMapper likesMapper;
    private final FavoritesMapper favoritesMapper;
    private final MessageService messageService;
    private final CommentsService commentsService;
    private final UsersMapper usersMapper;
    private final PostContentService postContentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PostServiceImpl(LikesMapper likesMapper, FavoritesMapper favoritesMapper, MessageService messageService,
                           CommentsService commentsService, UsersMapper usersMapper, PostContentService postContentService) {
        this.likesMapper = likesMapper;
        this.favoritesMapper = favoritesMapper;
        this.messageService = messageService;
        this.commentsService = commentsService;
        this.usersMapper = usersMapper;
        this.postContentService = postContentService;
    }

    @Override
    public IPage<Post> getListByPage(int page, int pageSize, Map<String, Object> queryForm) {
        Page<Post> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<>();
        if (queryForm != null) {
            if (queryForm.get("category_id") != null) {
                w.eq(Post::getCategory_id, toInt(queryForm.get("category_id")));
            }
            if (queryForm.get("user_id") != null) {
                w.eq(Post::getUser_id, toInt(queryForm.get("user_id")));
            }
            if (queryForm.get("discuss_id") != null) {
                w.eq(Post::getDiscuss_id, toInt(queryForm.get("discuss_id")));
            }
        }
        w.orderByDesc(Post::getIs_top).orderByDesc(Post::getTop_at).orderByDesc(Post::getCreated_at);
        IPage<Post> result = page(p, w);
        result.getRecords().forEach(this::appendContentHtml);
        return result;
    }

    @Override
    @Transactional
    public Post publish(Integer userId, Map<String, Object> params) {
        Post post = new Post();
        post.setUser_id(userId);
        post.setTitle((String) params.get("title"));
        post.setCategory_id(toInt(params.get("category_id")));
        post.setDiscuss_id(toInt(params.get("discuss_id")));
        post.setContent((String) params.get("content"));
        Object media = params.get("media");
        if (media != null) {
            try {
                post.setMedia(objectMapper.writeValueAsString(media));
            } catch (JsonProcessingException e) {
                post.setMedia(media.toString());
            }
        }
        post.setLike_count(0);
        post.setFavorite_count(0);
        post.setView_count(0);
        post.setComment_count(0);
        post.setIs_top(0);
        save(post);
        post.setContent_html(post.getContent());
        return post;
    }

    @Override
    public Post getInfo(Integer userId, Integer postId) {
        Post post = getById(postId);
        if (post == null) {
            throw new BizException(ErrorCode.FAIL, "帖子不存在");
        }
        if (userId != null) {
            boolean liked = likesMapper.selectCount(new LambdaQueryWrapper<Likes>()
                    .eq(Likes::getUser_id, userId).eq(Likes::getPost_id, postId)) > 0;
            boolean favorited = favoritesMapper.selectCount(new LambdaQueryWrapper<Favorites>()
                    .eq(Favorites::getUser_id, userId).eq(Favorites::getPost_id, postId)) > 0;
            post.setIs_liked(liked);
            post.setIs_favorited(favorited);
        } else {
            post.setIs_liked(false);
            post.setIs_favorited(false);
        }
        post.setView_count((post.getView_count() == null ? 0 : post.getView_count()) + 1);
        updateById(post);
        appendContentHtml(post);
        return post;
    }

    @Override
    @Transactional
    public Map<String, Object> like(Integer userId, Integer postId) {
        Post post = baseMapper.selectByIdForUpdate(postId);
        if (post == null) {
            throw new BizException(ErrorCode.FAIL, "帖子不存在");
        }
        Likes existing = likesMapper.selectByUserAndPostForUpdate(userId, postId);
        Map<String, Object> res = new HashMap<>(2);
        if (existing != null) {
            likesMapper.deleteById(existing.getId());
            if (post.getLike_count() != null && post.getLike_count() > 0) {
                post.setLike_count(post.getLike_count() - 1);
                updateById(post);
            }
            res.put("is_liked", false);
            return res;
        }
        Likes like = new Likes();
        like.setUser_id(userId);
        like.setPost_id(postId);
        likesMapper.insert(like);
        post.setLike_count((post.getLike_count() == null ? 0 : post.getLike_count()) + 1);
        updateById(post);
        messageService.sendMessage(userId, post.getUser_id(), "赞了你的帖子", MessageService.TYPE_LIKE, postId);
        res.put("is_liked", true);
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> favorite(Integer userId, Integer postId) {
        Post post = baseMapper.selectByIdForUpdate(postId);
        if (post == null) {
            throw new BizException(ErrorCode.FAIL, "帖子不存在");
        }
        Favorites existing = favoritesMapper.selectByUserAndPostForUpdate(userId, postId);
        Map<String, Object> res = new HashMap<>(2);
        if (existing != null) {
            favoritesMapper.deleteById(existing.getId());
            if (post.getFavorite_count() != null && post.getFavorite_count() > 0) {
                post.setFavorite_count(post.getFavorite_count() - 1);
                updateById(post);
            }
            res.put("is_favorited", false);
            return res;
        }
        Favorites fav = new Favorites();
        fav.setUser_id(userId);
        fav.setPost_id(postId);
        favoritesMapper.insert(fav);
        post.setFavorite_count((post.getFavorite_count() == null ? 0 : post.getFavorite_count()) + 1);
        updateById(post);
        messageService.sendMessage(userId, post.getUser_id(), "收藏了你的帖子", MessageService.TYPE_FAVORITE, postId);
        res.put("is_favorited", true);
        return res;
    }

    @Override
    public IPage<Post> currentUserPosts(int page, int pageSize, Integer userId) {
        Page<Post> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<>();
        w.eq(Post::getUser_id, userId).orderByDesc(Post::getCreated_at);
        IPage<Post> result = page(p, w);
        result.getRecords().forEach(this::appendContentHtml);
        return result;
    }

    @Override
    public IPage<Post> favoritesByUser(int page, int pageSize, Integer userId) {
        Page<Favorites> fp = new Page<>(page, pageSize);
        favoritesMapper.selectPage(fp, new LambdaQueryWrapper<Favorites>()
                .eq(Favorites::getUser_id, userId).orderByDesc(Favorites::getCreated_at));
        return toPostPage(fp, Favorites::getPost_id);
    }

    @Override
    public IPage<Post> likesByUser(int page, int pageSize, Integer userId) {
        Page<Likes> lp = new Page<>(page, pageSize);
        likesMapper.selectPage(lp, new LambdaQueryWrapper<Likes>()
                .eq(Likes::getUser_id, userId).orderByDesc(Likes::getCreated_at));
        return toPostPage(lp, Likes::getPost_id);
    }

    private <T> IPage<Post> toPostPage(Page<T> relPage, Function<T, Integer> postIdGetter) {
        List<T> records = relPage.getRecords();
        List<Integer> postIds = records.stream().map(postIdGetter).collect(Collectors.toList());
        Map<Integer, Post> postMap = postIds.isEmpty() ? Collections.emptyMap()
                : listByIds(postIds).stream().collect(Collectors.toMap(Post::getId, p -> p, (a, b) -> a));
        List<Post> posts = records.stream()
                .map(r -> {
                    Post p = postMap.get(postIdGetter.apply(r));
                    if (p != null) {
                        appendContentHtml(p);
                    }
                    return p;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Page<Post> result = new Page<>(relPage.getCurrent(), relPage.getSize(), relPage.getTotal());
        result.setRecords(posts);
        return result;
    }

    @Override
    public List<PostBriefVO> searchPosts(String keyword) {
        List<Post> list = list(new LambdaQueryWrapper<Post>()
                .and(w -> w.like(Post::getTitle, keyword).or().like(Post::getContent, keyword))
                .last("LIMIT 10"));
        return list.stream().map(p -> {
            PostBriefVO v = new PostBriefVO();
            v.setId(p.getId());
            v.setTitle(p.getTitle());
            // 对齐 PHP Search::buildSummary(content, 80)
            v.setContent(postContentService.buildSummary(p.getContent(), 80));
            return v;
        }).collect(Collectors.toList());
    }

    /** 对齐 PHP Post::appendContentHtml：用 Markdown 渲染器生成 content_html */
    private void appendContentHtml(Post post) {
        if (post != null) {
            post.setContent_html(postContentService.render(post.getContent()));
        }
    }

    // ===================== 管理端业务 =====================

    @Override
    public IPage<Post> getAdminListByPage(int page, int pageSize, Map<String, Object> queryForm) {
        Page<Post> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<>();
        if (queryForm != null) {
            if (queryForm.get("title") != null && !queryForm.get("title").toString().isEmpty()) {
                w.like(Post::getTitle, queryForm.get("title").toString());
            }
            if (queryForm.get("content") != null && !queryForm.get("content").toString().isEmpty()) {
                w.like(Post::getContent, queryForm.get("content").toString());
            }
            if (queryForm.get("user_id") != null) {
                w.eq(Post::getUser_id, toInt(queryForm.get("user_id")));
            }
            if (queryForm.get("is_top") != null) {
                w.eq(Post::getIs_top, toInt(queryForm.get("is_top")));
            }
            // created_at 日期范围：支持 start/end 单端或双端
            Object start = queryForm.get("created_at_start");
            Object end = queryForm.get("created_at_end");
            LocalDateTime startLdt = (start != null && !start.toString().isEmpty()) ? toStart(start.toString()) : null;
            LocalDateTime endLdt = (end != null && !end.toString().isEmpty()) ? toEnd(end.toString()) : null;
            if (startLdt != null && endLdt != null) {
                w.between(Post::getCreated_at, startLdt, endLdt);
            } else if (startLdt != null) {
                w.ge(Post::getCreated_at, startLdt);
            } else if (endLdt != null) {
                w.le(Post::getCreated_at, endLdt);
            }
        }
        w.orderByDesc(Post::getIs_top).orderByDesc(Post::getTop_at).orderByDesc(Post::getCreated_at);
        return page(p, w);
    }

    @Override
    public Map<String, Object> getAdminPostDetail(Integer id) {
        Post post = getById(id);
        if (post == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        Map<String, Object> res = new HashMap<>(2);
        res.put("post", post);
        UserBriefVO brief = null;
        if (post.getUser_id() != null) {
            Users u = usersMapper.selectById(post.getUser_id());
            if (u != null) {
                brief = new UserBriefVO();
                brief.setId(u.getId());
                brief.setNickname(u.getNickname());
                brief.setAvatar(u.getAvatar());
            }
        }
        res.put("user", brief);
        return res;
    }

    @Override
    public void setTop(Integer id, Integer isTop) {
        Post post = getById(id);
        if (post == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        int top = (isTop == null || isTop == 0) ? 0 : 1;
        post.setIs_top(top);
        // 置顶写当前时间，取消置顶置 null
        post.setTop_at(top == 1 ? LocalDateTime.now() : null);
        updateById(post);
    }

    @Override
    @Transactional
    public void deletePost(Integer id) {
        Post post = getById(id);
        if (post == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        deleteManagedPost(post);
    }

    @Override
    @Transactional
    public void batchDeletePosts(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择要删除的帖子");
        }
        List<Integer> postIds = ids.stream()
                .filter(Objects::nonNull)
                .map(v -> {
                    if (v instanceof Number) {
                        return ((Number) v).intValue();
                    }
                    String s = v.toString().trim();
                    return s.isEmpty() ? null : Integer.valueOf(s);
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (postIds.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择要删除的帖子");
        }
        List<Post> posts = list(new LambdaQueryWrapper<Post>().in(Post::getId, postIds));
        if (posts.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        for (Post post : posts) {
            deleteManagedPost(post);
        }
    }

    /**
     * 管理端统一删除逻辑：级联删除评论/收藏/点赞 -> 删除帖子 ->
     * 回退作者 post_count（行锁防并发）-> 给作者发系统消息
     */
    private void deleteManagedPost(Post post) {
        Integer postId = post.getId();

        // 1. 级联删除该帖全部评论（含子孙）并回退计数
        commentsService.deleteCommentsByPostId(postId);
        // 2. 删除收藏
        favoritesMapper.delete(new LambdaQueryWrapper<Favorites>().eq(Favorites::getPost_id, postId));
        // 3. 删除点赞
        likesMapper.delete(new LambdaQueryWrapper<Likes>().eq(Likes::getPost_id, postId));
        // 4. 删除帖子
        removeById(postId);

        // 5. 回退作者 post_count 并发系统消息
        Integer userId = post.getUser_id();
        if (userId != null) {
            Users user = usersMapper.selectByIdForUpdate(userId);
            if (user != null && user.getPost_count() != null && user.getPost_count() > 0) {
                user.setPost_count(user.getPost_count() - 1);
                usersMapper.updateById(user);
            }
            String content = buildDeletedPostSystemContent(post.getTitle(), post.getContent());
            messageService.sendMessage(SYSTEM_USER_ID, userId, content, MessageService.TYPE_SYSTEM, postId);
        }
    }

    /**
     * 构造「帖子被删除」系统消息内容（对齐 PHP MessageService::buildDeletedPostSystemContent）
     * 优先返回 JSON 负载 {k:'pd', t, p:{t,s}}，超 255 字符则退化为纯文本
     */
    private String buildDeletedPostSystemContent(String title, String summary) {
        String t = (title == null) ? "" : title.trim();
        if (t.length() > 40) {
            t = t.substring(0, 40) + "...";
        }
        String s = (summary == null) ? "" : summary.trim().replaceAll("\\s+", " ");
        if (s.length() > 56) {
            s = s.substring(0, 56) + "...";
        }
        String text = t.isEmpty()
                ? "你发布的一条帖子已被管理员删除，如有疑问请联系平台管理员。"
                : "你发布的帖子《" + t + "》已被管理员删除，如有疑问请联系平台管理员。";

        Map<String, Object> payload = new HashMap<>(3);
        payload.put("k", "pd");
        payload.put("t", text);
        Map<String, String> p = new HashMap<>(2);
        p.put("t", t);
        p.put("s", s);
        payload.put("p", p);
        try {
            String encoded = objectMapper.writeValueAsString(payload);
            if (encoded.length() <= 255) {
                return encoded;
            }
        } catch (JsonProcessingException ignored) {
            // 序列化失败时退化为纯文本
        }
        return text;
    }

    // ===================== 私有工具 =====================

    /** 解析日期：yyyy-MM-dd 视为当天 00:00:00，否则按 yyyy-MM-dd HH:mm:ss 解析 */
    private LocalDateTime toStart(String s) {
        try {
            return s.length() <= 10 ? LocalDate.parse(s).atStartOfDay() : LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** 解析结束日期：yyyy-MM-dd 视为当天 23:59:59（闭区间） */
    private LocalDateTime toEnd(String s) {
        try {
            return s.length() <= 10 ? LocalDate.parse(s).atTime(23, 59, 59) : LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
