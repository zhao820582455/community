package com.zbtech.community.controller.api;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.Users;
import com.zbtech.community.service.AuthService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户端鉴权接口（对齐原 foxbook api/controller/Auth）
 * 路径规则：/api/auth/{action}
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController extends BaseController {

    @Resource
    private AuthService authService;

    /** POST /api/auth/accountLogin */
    @PostMapping("/accountLogin")
    public Result<Map<String, Object>> accountLogin(@RequestBody Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        return Result.success(authService.accountLogin(username, password));
    }

    /** POST /api/auth/sendEmailCode */
    @PostMapping("/sendEmailCode")
    public Result<Object> sendEmailCode(@RequestBody Map<String, Object> params) {
        String email = (String) params.get("email");
        Object sceneObj = params.get("scene");
        String scene = sceneObj == null ? "register" : (String) sceneObj;
        authService.sendEmailCode(email, scene);
        return Result.success("验证码已发送", (Object) null);
    }

    /** POST /api/auth/accountRegister */
    @PostMapping("/accountRegister")
    public Result<Object> accountRegister(@RequestBody Map<String, Object> params) {
        String username = (String) params.get("username");
        String email = (String) params.get("email");
        String password = (String) params.get("password");
        String code = (String) params.get("code");
        authService.accountRegister(username, email, password, code);
        return Result.success("注册成功", (Object) null);
    }

    /** POST /api/auth/mnpLogin */
    @PostMapping("/mnpLogin")
    public Result<Map<String, Object>> mnpLogin(@RequestBody Map<String, Object> params) {
        String code = (String) params.get("code");
        return Result.success(authService.mnpLogin(code));
    }

    /** GET /api/auth/info 当前登录用户 */
    @GetMapping("/info")
    public Result<Users> info() {
        Integer userId = getUserId();
        return Result.success(authService.currentUserInfo(userId));
    }
}
