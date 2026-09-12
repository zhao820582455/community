package com.zbtech.community.config;

import com.zbtech.community.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户端 /api 鉴权拦截器：解析 Authorization 头，查 Redis 得到 user_id 并注入请求属性。
 * 不强制拦截（未登录也能访问公开接口），需要登录的接口在 Controller 调 getUserId() 自行校验。
 */
@Component
public class ApiAuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    public ApiAuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = resolveToken(request);
        if (token != null) {
            Integer uid = tokenService.resolveApiUserId(token);
            if (uid != null) {
                request.setAttribute("user_id", uid);
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
