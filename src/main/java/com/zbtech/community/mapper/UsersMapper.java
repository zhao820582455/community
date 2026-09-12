package com.zbtech.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbtech.community.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UsersMapper extends BaseMapper<Users> {

    /** 按登录终端 terminal 分组统计用户数，返回 {terminal, count}
     *  注意：terminal 列为 tinyint(1)，mysql-connector-j 8.x 会把 tinyint(1) 映射为 Boolean，
     *  自定义 SQL 返回 Map 时 terminal 会变成 true/false → 必须用 `terminal + 0` 强制转数字。 */
    @Select("SELECT terminal + 0 AS terminal, COUNT(*) AS count FROM lb_users GROUP BY terminal")
    List<Map<String, Object>> selectCountByTerminal();

    /** 行锁查询用户（防并发计数回退/扣减） */
    @Select("SELECT * FROM lb_users WHERE id = #{id} FOR UPDATE")
    Users selectByIdForUpdate(@Param("id") Integer id);
}
