package com.zbtech.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbtech.community.entity.CommentLikes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentLikesMapper extends BaseMapper<CommentLikes> {

    @Select("SELECT * FROM lb_comment_likes WHERE user_id = #{userId} AND comment_id = #{commentId} FOR UPDATE")
    CommentLikes selectByUserAndCommentForUpdate(@Param("userId") Integer userId,
                                                 @Param("commentId") Integer commentId);
}
