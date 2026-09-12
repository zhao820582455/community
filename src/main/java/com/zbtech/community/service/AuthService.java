package com.zbtech.community.service;

import com.zbtech.community.entity.Users;

import java.util.Map;

/**
 * 用户端鉴权业务（对齐原 foxbook api/controller/Auth + UserAccountService）
 */
public interface AuthService {

    /** 账号密码登录，返回 {token, user} */
    Map<String, Object> accountLogin(String username, String password);

    /** 注册（需先调 sendEmailCode 拿到验证码） */
    void accountRegister(String username, String email, String password, String code);

    /** 发送邮箱验证码（login/register 场景） */
    void sendEmailCode(String email, String scene);

    /** 微信小程序登录（当前未集成，抛友好异常） */
    Map<String, Object> mnpLogin(String code);

    /** 当前登录用户信息（密码置空） */
    Users currentUserInfo(Integer userId);
}
