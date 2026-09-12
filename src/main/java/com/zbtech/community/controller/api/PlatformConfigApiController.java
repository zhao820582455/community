package com.zbtech.community.controller.api;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.service.PlatformConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户端平台配置接口（对齐原 foxbook api/controller/PlatformConfig）
 * 仅提供公开站点配置（无需登录）
 */
@RestController
@RequestMapping("/api/platformConfig")
public class PlatformConfigApiController extends BaseController {

    @Resource
    private PlatformConfigService platformConfigService;

    /** GET /api/platformConfig/getPublicSiteConfig 获取公开站点配置 */
    @GetMapping("/getPublicSiteConfig")
    public Result<Map<String, Object>> getPublicSiteConfig() {
        return Result.success(platformConfigService.getPublicSiteSettings());
    }
}
