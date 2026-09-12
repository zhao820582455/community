package com.zbtech.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbtech.community.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("SELECT * FROM lb_post WHERE id = #{id} FOR UPDATE")
    Post selectByIdForUpdate(@Param("id") Integer id);
}
