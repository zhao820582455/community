package com.zbtech.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbtech.community.entity.Likes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LikesMapper extends BaseMapper<Likes> {

    @Select("SELECT * FROM lb_likes WHERE user_id = #{userId} AND post_id = #{postId} FOR UPDATE")
    Likes selectByUserAndPostForUpdate(@Param("userId") Integer userId, @Param("postId") Integer postId);
}
