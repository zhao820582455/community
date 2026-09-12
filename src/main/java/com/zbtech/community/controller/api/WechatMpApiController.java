package com.zbtech.community.controller.api;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.Users;
import com.zbtech.community.service.UsersService;
import com.zbtech.community.service.WechatMpService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 微信小程序接口（对齐原 foxbook app/api/controller/WechatMp + Auth::mnpLogin）
 * 路径前缀：/api/wechatMp
 */
@RestController
@RequestMapping("/api/wechatMp")
public class WechatMpApiController extends BaseController {

    @Resource
    private WechatMpService wechatMpService;

    @Resource
    private UsersService usersService;

    /**
     * 微信小程序登录
     * POST /api/wechatMp/login
     * body: { "js_code": "xxx" }  （兼容 "code" 字段）
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> params) {
        Object codeObj = params.get("js_code");
        if (codeObj == null) {
            codeObj = params.get("code");
        }
        String jsCode = codeObj == null ? null : (String) codeObj;
        return Result.success(wechatMpService.mnpLogin(jsCode));
    }

    /**
     * 解密微信加密数据（手机号 / 用户信息）
     * POST /api/wechatMp/decrypt  （需登录态）
     * body: { "encryptedData": "xxx", "iv": "xxx" }
     * openid 取当前登录用户，session_key 取登录时缓存值
     */
    @PostMapping("/decrypt")
    public Result<Map<String, Object>> decrypt(@RequestBody Map<String, Object> params) {
        Integer userId = getUserId();
        String encryptedData = (String) params.get("encryptedData");
        String iv = (String) params.get("iv");

        Users user = usersService.getById(userId);
        if (user == null || isBlank(user.getOpenid())) {
            throw new BizException(ErrorCode.FAIL, "当前账号未绑定微信");
        }
        return Result.success(wechatMpService.decryptUserInfo(user.getOpenid(), encryptedData, iv));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
