package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.PlatformConfig;
import com.zbtech.community.service.PlatformConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 平台配置 - 管理端（对齐原 foxbook adminapi/controller/PlatformConfig）
 */
@RestController
@RequestMapping({"/adminapi/platformConfig", "/adminapi/PlatformConfig"})
@Tag(name = "平台配置管理")
public class PlatformConfigAdminController extends BaseController {

    @Autowired
    private PlatformConfigService service;

    /**
     * GET /adminapi/platformConfig/getPlatformConfig
     * 按 key 获取配置载荷，不传 key 返回全部配置
     */
    @GetMapping("/getPlatformConfig")
    public Result<?> getPlatformConfig(@RequestParam(required = false) String key) {
        getAdminId();
        if (key != null && !key.isBlank()) {
            Map<String, Object> config = service.getConfigPayload(key.trim());
            if (config == null) {
                throw new BizException(ErrorCode.NOT_FOUND.getCode(), "配置不存在");
            }
            return Result.success(config);
        }
        return Result.success(service.getAllConfigs());
    }

    /** GET /adminapi/platformConfig/getList 全部配置 */
    @GetMapping("/getList")
    public Result<?> getList() {
        getAdminId();
        return Result.success(service.getAllConfigs());
    }

    /**
     * POST /adminapi/platformConfig/saveConfig
     * 保存配置（新增或更新）
     */
    @PostMapping("/saveConfig")
    public Result<PlatformConfig> saveConfig(@RequestBody Map<String, Object> params) {
        getAdminId();
        String name = params.get("name") != null ? params.get("name").toString() : null;
        String key = params.get("key") != null ? params.get("key").toString() : null;
        if (key == null || key.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "配置键不能为空");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "配置名称不能为空");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> value = (Map<String, Object>) params.get("value");
        if (value == null) {
            value = Map.of();
        }
        PlatformConfig saved = service.saveConfig(key.trim(), name.trim(), value);
        return Result.success("配置更新成功", saved);
    }
}
