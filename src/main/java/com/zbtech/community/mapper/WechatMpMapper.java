package com.zbtech.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbtech.community.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 微信小程序 Mapper（对齐原 foxbook WeChatMnpService）
 * 业务实体复用 users 表，故继承 BaseMapper<Users>。
 * 仅承载“按 openid 查用户”这类微信专用查询，不改动 UsersMapper / UsersService。
 */
@Mapper
public interface WechatMpMapper extends BaseMapper<Users> {

    /** 根据微信 openid 查询用户（openid 在 users 表中唯一） */
    @Select("SELECT * FROM lb_users WHERE openid = #{openid} LIMIT 1")
    Users selectByOpenid(@Param("openid") String openid);
}
