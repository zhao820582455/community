package com.zbtech.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbtech.community.entity.Favorites;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FavoritesMapper extends BaseMapper<Favorites> {

    @Select("SELECT * FROM lb_favorites WHERE user_id = #{userId} AND post_id = #{postId} FOR UPDATE")
    Favorites selectByUserAndPostForUpdate(@Param("userId") Integer userId, @Param("postId") Integer postId);
}
