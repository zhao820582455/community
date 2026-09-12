package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.UserFollow;
import com.zbtech.community.entity.Users;
import com.zbtech.community.mapper.UserFollowMapper;
import com.zbtech.community.mapper.UsersMapper;
import com.zbtech.community.service.UserFollowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 关注实现：计数同步直接走 UsersMapper，避免与 UsersService 形成构造器循环依赖
 */
@Service
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements UserFollowService {

    private final UsersMapper usersMapper;

    public UserFollowServiceImpl(UsersMapper usersMapper) {
        this.usersMapper = usersMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean follow(Integer userId, Integer targetId) {
        if (targetId == null || targetId.equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        if (usersMapper.selectById(targetId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (isFollowing(userId, targetId)) {
            return true;
        }
        UserFollow uf = new UserFollow();
        uf.setUser_id(userId);
        uf.setFollow_user_id(targetId);
        save(uf);
        incr(userId, "follow_count", 1);
        incr(targetId, "fans_count", 1);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfollow(Integer userId, Integer targetId) {
        if (targetId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        UserFollow uf = getOne(new QueryWrapper<UserFollow>()
                .eq("user_id", userId).eq("follow_user_id", targetId));
        if (uf == null) {
            return true;
        }
        removeById(uf.getId());
        incr(userId, "follow_count", -1);
        incr(targetId, "fans_count", -1);
        return true;
    }

    @Override
    public boolean isFollowing(Integer userId, Integer targetId) {
        return count(new QueryWrapper<UserFollow>()
                .eq("user_id", userId).eq("follow_user_id", targetId)) > 0;
    }

    @Override
    public boolean isMutualFollow(Integer userId, Integer targetId) {
        return isFollowing(userId, targetId) && isFollowing(targetId, userId);
    }

    private void incr(Integer userId, String column, int delta) {
        UpdateWrapper<Users> uw = new UpdateWrapper<>();
        uw.eq("id", userId);
        if (delta > 0) {
            uw.setSql(column + " = " + column + " + 1");
        } else {
            uw.setSql(column + " = GREATEST(0, " + column + " - 1)");
        }
        usersMapper.update(null, uw);
    }
}
