package com.zbtech.community.vo;

import com.zbtech.community.entity.Discuss;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 话题对外视图（对齐原 foxbook Discuss 控制器，含发起人简要信息）
 */
@Data
public class DiscussVO {

    private Integer id;
    private Integer user_id;
    private String title;
    private String content;
    private String media;
    private Integer view_count;
    private Integer post_count;
    private LocalDateTime updated_at;
    private LocalDateTime created_at;
    private UserBriefVO user;

    public static DiscussVO from(Discuss d, UserBriefVO user) {
        if (d == null) {
            return null;
        }
        DiscussVO v = new DiscussVO();
        v.setId(d.getId());
        v.setUser_id(d.getUser_id());
        v.setTitle(d.getTitle());
        v.setContent(d.getContent());
        v.setMedia(d.getMedia());
        v.setView_count(d.getView_count());
        v.setPost_count(d.getPost_count());
        v.setUpdated_at(d.getUpdated_at());
        v.setCreated_at(d.getCreated_at());
        v.setUser(user);
        return v;
    }
}
