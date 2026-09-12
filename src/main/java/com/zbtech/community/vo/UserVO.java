package com.zbtech.community.vo;

import com.zbtech.community.entity.Users;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户对外公开资料（脱敏：不含 password / openid / unionid / 登录 IP 等敏感字段）
 * 对齐原 foxbook User.getInfo / getUserById / getHotUsers 的返回结构
 */
@Data
public class UserVO {

    private Integer id;
    private String sn;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private Integer gender;
    private LocalDate birthday;
    private String introduction;
    private Integer status;
    private Integer post_count;
    private Integer follow_count;
    private Integer fans_count;
    private Integer post_thumb_count;
    private Integer post_collect_count;
    private LocalDateTime created_at;

    /** 是否关注（查看他人时填充） */
    private Boolean is_following;
    /** 是否互相关注 */
    private Boolean is_mutual_follow;

    public static UserVO from(Users u) {
        if (u == null) {
            return null;
        }
        UserVO v = new UserVO();
        v.setId(u.getId());
        v.setSn(u.getSn());
        v.setNickname(u.getNickname());
        v.setAvatar(u.getAvatar());
        v.setPhone(u.getPhone());
        v.setEmail(u.getEmail());
        v.setGender(u.getGender());
        v.setBirthday(u.getBirthday());
        v.setIntroduction(u.getIntroduction());
        v.setStatus(u.getStatus());
        v.setPost_count(u.getPost_count());
        v.setFollow_count(u.getFollow_count());
        v.setFans_count(u.getFans_count());
        v.setPost_thumb_count(u.getPost_thumb_count());
        v.setPost_collect_count(u.getPost_collect_count());
        v.setCreated_at(u.getCreated_at());
        return v;
    }
}
