package com.zbtech.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册鉴权拦截器（对齐原 foxbook AuthCheck 中间件）
 * - ApiAuthInterceptor 负责 /api 用户端
 * - AdminAuthInterceptor 负责 /adminapi 管理端
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiAuthInterceptor apiAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebConfig(ApiAuthInterceptor apiAuthInterceptor, AdminAuthInterceptor adminAuthInterceptor) {
        this.apiAuthInterceptor = apiAuthInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiAuthInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/adminapi/**");
    }
}
