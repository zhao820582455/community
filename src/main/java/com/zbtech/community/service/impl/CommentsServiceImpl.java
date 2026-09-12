package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.CommentLikes;
import com.zbtech.community.entity.Comments;
import com.zbtech.community.entity.Post;
import com.zbtech.community.entity.Users;
import com.zbtech.community.mapper.CommentsMapper;
import com.zbtech.community.mapper.CommentLikesMapper;
import com.zbtech.community.mapper.PostMapper;
import com.zbtech.community.service.CommentsService;
import com.zbtech.community.service.UsersService;
import com.zbtech.community.vo.CommentVO;
import com.zbtech.community.vo.UserBriefVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments> implements CommentsService {

    private final PostMapper postMapper;
    private final CommentLikesMapper commentLikesMapper;
    private final UsersService usersService;

    public CommentsServiceImpl(PostMapper postMapper, CommentLikesMapper commentLikesMapper,
                              UsersService usersService) {
        this.postMapper = postMapper;
        this.commentLikesMapper = commentLikesMapper;
        this.usersService = usersService;
    }

    @Override
    @Transactional
    public Comments createComment(Integer userId, Integer postId, String content,
                                  Integer parentId, Integer toUserId) {
        if (content == null) {
            throw new BizException(ErrorCode.FAIL, "评论内容不能为空");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 255) {
            trimmed = trimmed.substring(0, 255);
        }
        if (trimmed.isEmpty()) {
            throw new BizException(ErrorCode.FAIL, "评论内容不能为空");
        }

        int pid = (parentId == null) ? 0 : parentId;

        Post post = postMapper.selectByIdForUpdate(postId);
        if (post == null) {
            throw new BizException(ErrorCode.FAIL, "帖子不存在");
        }

        int rootId = 0;
        Comments parentComment = null;
        if (pid != 0) {
            parentComment = baseMapper.selectByIdForUpdate(pid);
            if (parentComment == null || !Objects.equals(parentComment.getPost_id(), post.getId())) {
                throw new BizException(ErrorCode.FAIL, "父评论不存在或不属于当前帖子");
            }
            rootId = (parentComment.getRoot_id() != null && parentComment.getRoot_id() != 0)
                    ? parentComment.getRoot_id() : parentComment.getId();
        }

        int resolvedToUserId = (toUserId != null) ? toUserId
                : (parentComment != null ? parentComment.getUser_id()
                : (post.getUser_id() != null ? post.getUser_id() : 0));
        if (resolvedToUserId == 0 || usersService.getById(resolvedToUserId) == null) {
            throw new BizException(ErrorCode.FAIL, "回复目标用户不存在");
        }

        Comments comment = new Comments();
        comment.setParent_id(pid);
        comment.setRoot_id(rootId);
        comment.setUser_id(userId);
        comment.setTo_user_id(resolvedToUserId);
        comment.setPost_id(post.getId());
        comment.setContent(trimmed);
        comment.setLike_count(0);
        comment.setReply_count(0);
        save(comment);

        post.setComment_count((post.getComment_count() == null ? 0 : post.getComment_count()) + 1);
        postMapper.updateById(post);

        if (rootId != 0) {
            Comments rootComment = baseMapper.selectByIdForUpdate(rootId);
            if (rootComment != null) {
                rootComment.setReply_count((rootComment.getReply_count() == null ? 0 : rootComment.getReply_count()) + 1);
                baseMapper.updateById(rootComment);
            }
        }
        return comment;
    }

    @Override
    @Transactional
    public Map<String, Object> toggleLike(Integer userId, Integer commentId) {
        Comments comment = baseMapper.selectByIdForUpdate(commentId);
        Map<String, Object> res = new HashMap<>(4);
        if (comment == null) {
            res.put("comment", null);
            res.put("active", false);
            return res;
        }
        CommentLikes existing = commentLikesMapper.selectByUserAndCommentForUpdate(userId, commentId);
        boolean active;
        if (existing != null) {
            commentLikesMapper.deleteById(existing.getId());
            if (comment.getLike_count() != null && comment.getLike_count() > 0) {
                comment.setLike_count(comment.getLike_count() - 1);
                baseMapper.updateById(comment);
            }
            active = false;
        } else {
            CommentLikes like = new CommentLikes();
            like.setUser_id(userId);
            like.setComment_id(commentId);
            commentLikesMapper.insert(like);
            comment.setLike_count((comment.getLike_count() == null ? 0 : comment.getLike_count()) + 1);
            baseMapper.updateById(comment);
            active = true;
        }
        res.put("comment", comment);
        res.put("active", active);
        return res;
    }

    @Override
    public IPage<CommentVO> getParentComments(Integer postId, Integer viewerUserId, int page, int pageSize) {
        Page<Comments> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Comments> w = new LambdaQueryWrapper<>();
        w.eq(Comments::getPost_id, postId).eq(Comments::getParent_id, 0)
                .orderByDesc(Comments::getCreated_at);
        IPage<Comments> pageResult = page(p, w);
        return toVoPage(pageResult, viewerUserId);
    }

    @Override
    public IPage<CommentVO> getThreadReplies(Integer rootId, Integer viewerUserId, int page, int pageSize,
                                            Integer excludeCommentId) {
        Page<Comments> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Comments> w = new LambdaQueryWrapper<>();
        w.eq(Comments::getRoot_id, rootId).orderByDesc(Comments::getCreated_at);
        if (excludeCommentId != null && excludeCommentId != 0) {
            w.ne(Comments::getId, excludeCommentId);
        }
        IPage<Comments> pageResult = page(p, w);
        return toVoPage(pageResult, viewerUserId);
    }

    @Override
    @Transactional
    public int deleteCommentsByPostId(Integer postId) {
        if (postId == null) {
            return 0;
        }
        List<Integer> ids = list(new LambdaQueryWrapper<Comments>()
                .eq(Comments::getPost_id, postId)
                .select(Comments::getId))
                .stream().map(Comments::getId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return 0;
        }
        // 复用级联删除逻辑（删除子孙 + 回退帖子/根评论计数）
        return deleteComments(ids);
    }

    @Override
    @Transactional
    public int deleteComments(List<Integer> ids) {
        List<Integer> seedIds = ids.stream()
                .filter(Objects::nonNull)
                .map(id -> {
                    String s = String.valueOf(id).trim();
                    return s.isEmpty() ? null : Integer.valueOf(s);
                })
                .filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (seedIds.isEmpty()) {
            return 0;
        }

        Set<Integer> allIds = new LinkedHashSet<>();
        Map<Integer, Integer> countByPost = new HashMap<>();
        Map<Integer, Integer> replyCountByRoot = new HashMap<>();

        List<Integer> frontier = seedIds;
        while (!frontier.isEmpty()) {
            List<Comments> batch = list(new LambdaQueryWrapper<Comments>()
                    .in(Comments::getId, frontier)
                    .select(Comments::getId, Comments::getPost_id, Comments::getRoot_id));
            List<Integer> nextParents = new ArrayList<>();
            for (Comments c : batch) {
                if (allIds.contains(c.getId())) {
                    continue;
                }
                allIds.add(c.getId());
                Integer pid = c.getPost_id();
                if (pid != null) {
                    countByPost.merge(pid, 1, Integer::sum);
                }
                Integer root = c.getRoot_id();
                if (root != null && root != 0 && !allIds.contains(root)) {
                    replyCountByRoot.merge(root, 1, Integer::sum);
                }
                nextParents.add(c.getId());
            }
            if (nextParents.isEmpty()) {
                break;
            }
            List<Comments> children = list(new LambdaQueryWrapper<Comments>()
                    .in(Comments::getParent_id, nextParents)
                    .select(Comments::getId, Comments::getPost_id, Comments::getRoot_id));
            frontier = children.stream().map(Comments::getId)
                    .filter(id -> !allIds.contains(id)).collect(Collectors.toList());
        }

        if (allIds.isEmpty()) {
            return 0;
        }

        commentLikesMapper.delete(new LambdaQueryWrapper<CommentLikes>()
                .in(CommentLikes::getComment_id, allIds));
        removeByIds(allIds);

        for (Map.Entry<Integer, Integer> entry : countByPost.entrySet()) {
            Post post = postMapper.selectByIdForUpdate(entry.getKey());
            if (post != null) {
                int decrement = Math.min(entry.getValue(),
                        Math.max(post.getComment_count() == null ? 0 : post.getComment_count(), 0));
                if (decrement > 0) {
                    post.setComment_count(post.getComment_count() - decrement);
                    postMapper.updateById(post);
                }
            }
        }
        for (Map.Entry<Integer, Integer> entry : replyCountByRoot.entrySet()) {
            Comments root = baseMapper.selectByIdForUpdate(entry.getKey());
            if (root != null) {
                int decrement = Math.min(entry.getValue(),
                        Math.max(root.getReply_count() == null ? 0 : root.getReply_count(), 0));
                if (decrement > 0) {
                    root.setReply_count(root.getReply_count() - decrement);
                    baseMapper.updateById(root);
                }
            }
        }
        return allIds.size();
    }

    // ===================== 私有：VO 组装 =====================

    @Override
    public CommentVO getCommentVO(Integer commentId, Integer viewerUserId) {
        Comments c = getById(commentId);
        if (c == null) {
            return null;
        }
        return toVoList(Collections.singletonList(c), viewerUserId).get(0);
    }

    private IPage<CommentVO> toVoPage(IPage<Comments> pageResult, Integer viewerUserId) {
        List<CommentVO> vos = toVoList(pageResult.getRecords(), viewerUserId);
        Page<CommentVO> result = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        result.setRecords(vos);
        return result;
    }

    private List<CommentVO> toVoList(List<Comments> comments, Integer viewerUserId) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> userIds = new HashSet<>();
        for (Comments c : comments) {
            if (c.getUser_id() != null) userIds.add(c.getUser_id());
            if (c.getTo_user_id() != null) userIds.add(c.getTo_user_id());
        }
        Map<Integer, UserBriefVO> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : usersService.listByIds(userIds).stream()
                .collect(Collectors.toMap(Users::getId, this::toBrief, (a, b) -> a));

        Set<Integer> commentIds = comments.stream().map(Comments::getId).collect(Collectors.toSet());
        Set<Integer> likedIds = new HashSet<>();
        if (viewerUserId != null && !commentIds.isEmpty()) {
            List<CommentLikes> liked = commentLikesMapper.selectList(
                    new LambdaQueryWrapper<CommentLikes>()
                            .eq(CommentLikes::getUser_id, viewerUserId)
                            .in(CommentLikes::getComment_id, commentIds));
            liked.forEach(l -> likedIds.add(l.getComment_id()));
        }

        List<CommentVO> vos = new ArrayList<>();
        for (Comments c : comments) {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setUser_id(c.getUser_id());
            vo.setTo_user_id(c.getTo_user_id());
            vo.setPost_id(c.getPost_id());
            vo.setContent(c.getContent());
            vo.setLike_count(c.getLike_count() == null ? 0 : c.getLike_count());
            vo.setReply_count(c.getReply_count() == null ? 0 : c.getReply_count());
            vo.setIs_liked(likedIds.contains(c.getId()));
            vo.setUser(userMap.get(c.getUser_id()));
            vo.setTo_user(userMap.get(c.getTo_user_id()));
            vo.setCreated_at(c.getCreated_at());
            vos.add(vo);
        }
        return vos;
    }

    private UserBriefVO toBrief(Users u) {
        UserBriefVO b = new UserBriefVO();
        b.setId(u.getId());
        b.setNickname(u.getNickname());
        b.setAvatar(u.getAvatar());
        return b;
    }
}
