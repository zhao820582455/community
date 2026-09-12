package com.zbtech.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.UserFollow;

/**
 * 用户关注业务（对齐原 foxbook api/controller/User 的 follow/unfollow/checkFollowStatus）
 */
public interface UserFollowService extends IService<UserFollow> {

    /** 关注：已关注返回 true；自关/目标不存在抛异常；同步双方计数 */
    boolean follow(Integer userId, Integer targetId);

    /** 取消关注：未关注返回 true；同步双方计数 */
    boolean unfollow(Integer userId, Integer targetId);

    /** 是否关注了 targetId */
    boolean isFollowing(Integer userId, Integer targetId);

    /** 是否互相关注 */
    boolean isMutualFollow(Integer userId, Integer targetId);
}
