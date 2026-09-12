package com.zbtech.community.config;

import com.zbtech.community.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端 /adminapi 鉴权拦截器：解析 Authorization 头，查 Redis 得到 admin_id 并注入请求属性。
 * 登录 / 登出接口不强制鉴权（noNeedLogin），其余接口由 Controller 调 getAdminId() 自行校验。
 * 与用户端 ApiAuthInterceptor 平行，仅作用于 /adminapi/**。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    public AdminAuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        // 登录 / 登出 不需要鉴权（对应原 PHP Auth.php 的 noNeedLogin）
        if (uri != null && (uri.contains("/adminapi/auth/login")
                || uri.contains("/adminapi/auth/logout"))) {
            return true;
        }
        String token = resolveToken(request);
        if (token != null) {
            Integer adminId = tokenService.resolveAdminId(token);
            if (adminId != null) {
                request.setAttribute("admin_id", adminId);
            }
        }
        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            token = request.getHeader("authorization");
        }
        if (token == null) {
            return null;
        }
        token = token.trim();
        return token.isBlank() ? null : token;
    }
}
