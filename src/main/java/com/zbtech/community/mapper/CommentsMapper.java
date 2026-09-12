package com.zbtech.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbtech.community.entity.Comments;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentsMapper extends BaseMapper<Comments> {

    @Select("SELECT * FROM lb_comments WHERE id = #{id} FOR UPDATE")
    Comments selectByIdForUpdate(@Param("id") Integer id);
}
