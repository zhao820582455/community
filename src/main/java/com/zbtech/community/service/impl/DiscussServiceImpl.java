package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.Discuss;
import com.zbtech.community.entity.Users;
import com.zbtech.community.mapper.DiscussMapper;
import com.zbtech.community.service.DiscussService;
import com.zbtech.community.service.UsersService;
import com.zbtech.community.vo.DiscussVO;
import com.zbtech.community.vo.UserBriefVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscussServiceImpl extends ServiceImpl<DiscussMapper, Discuss> implements DiscussService {

    private final UsersService usersService;

    public DiscussServiceImpl(UsersService usersService) {
        this.usersService = usersService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Discuss publish(Integer userId, String title, String content, String media) {
        if (title == null || title.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "标题不能为空");
        }
        // 对齐 PHP Validator：v::length(6, 30)
        int titleLen = title.trim().length();
        if (titleLen < 6 || titleLen > 30) {
            throw new BizException(ErrorCode.PARAM_ERROR, "标题长度需在6-30个字符之间");
        }
        Discuss d = new Discuss();
        d.setUser_id(userId);
        d.setTitle(title);
        d.setContent(content);
        d.setMedia(media);
        d.setView_count(0);
        d.setPost_count(0);
        save(d);
        return d;
    }

    @Override
    public DiscussVO getDetail(Integer id) {
        Discuss d = getById(id);
        if (d == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        update(new UpdateWrapper<Discuss>().eq("id", id).setSql("view_count = view_count + 1"));
        d.setView_count((d.getView_count() == null ? 0 : d.getView_count()) + 1);
        Users u = usersService.getById(d.getUser_id());
        return DiscussVO.from(d, UserBriefVO.from(u));
    }

    @Override
    public List<DiscussVO> searchDiscusses(String keyword) {
        List<Discuss> list = list(new QueryWrapper<Discuss>()
                .and(w -> w.like("title", keyword).or().like("content", keyword))
                .last("LIMIT 10"));
        return list.stream()
                .map(d -> DiscussVO.from(d, UserBriefVO.from(usersService.getById(d.getUser_id()))))
                .collect(Collectors.toList());
    }

    @Override
    public IPage<DiscussVO> pageDiscuss(int page, int pageSize, String title, Integer userId) {
        Page<Discuss> p = new Page<>(page < 1 ? 1 : page, pageSize < 1 ? 10 : pageSize);
        QueryWrapper<Discuss> qw = new QueryWrapper<>();
        if (title != null && !title.isEmpty()) {
            qw.like("title", title);
        }
        if (userId != null) {
            qw.eq("user_id", userId);
        }
        qw.orderByDesc("id");
        IPage<Discuss> res = page(p, qw);
        Set<Integer> uids = res.getRecords().stream().map(Discuss::getUser_id).collect(Collectors.toSet());
        Map<Integer, Users> userMap = usersService.listByIds(uids).stream()
                .collect(Collectors.toMap(Users::getId, u -> u, (a, b) -> a));
        return res.convert(d -> DiscussVO.from(d, UserBriefVO.from(userMap.get(d.getUser_id()))));
    }
}
