package com.zbtech.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.Users;

import java.util.Map;

/**
 * 微信小程序业务（对齐原 foxbook api/controller/Auth::mnpLogin + WeChatMnpService）
 */
public interface WechatMpService extends IService<Users> {

    /**
     * 微信小程序登录：js_code -> code2Session 换 openid ->
     * 按 openid 查/建用户（统一 users 表）-> 签发 token 存 Redis
     * 返回 {token, user}
     */
    Map<String, Object> mnpLogin(String jsCode);

    /**
     * 用登录时缓存的 session_key 解密微信加密数据（手机号/用户信息）。
     * encryptedData / iv 由小程序前端通过 getPhoneNumber / getUserProfile 回传。
     * 返回解密后的原始 Map（含 phoneNumber / purePhoneNumber 等字段）。
     */
    Map<String, Object> decryptUserInfo(String openid, String encryptedData, String iv);

    /**
     * 微信内容安全 - 图片异步审核（对齐原 PHP WeChatMnpService::mediaCheckAsync）。
     * 调用微信 wxa/media_check_async（media_type=2 图片, version=2, scene=3），
     * 返回异步审核任务 trace_id。
     * 注：当前为桩实现（日志记录 + 生成 trace_id），真实对接需微信平台开通内容安全并配置 access_token。
     *
     * @param openid   用户 openid
     * @param mediaUrl 媒体完整 URL
     * @return trace_id
     */
    String mediaCheckAsync(String openid, String mediaUrl);
}
