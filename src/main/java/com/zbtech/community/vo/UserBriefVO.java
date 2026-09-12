package com.zbtech.community.vo;

import com.zbtech.community.entity.Users;
import lombok.Data;

/**
 * 用户简要信息（列表/消息关联返回，避免泄露 password 等敏感字段）
 */
@Data
public class UserBriefVO {
    private Integer id;
    private String nickname;
    private String avatar;

    public static UserBriefVO from(Users u) {
        if (u == null) {
            return null;
        }
        UserBriefVO v = new UserBriefVO();
        v.setId(u.getId());
        v.setNickname(u.getNickname());
        v.setAvatar(u.getAvatar());
        return v;
    }
}
