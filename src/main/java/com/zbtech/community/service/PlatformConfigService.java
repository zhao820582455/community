package com.zbtech.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.PlatformConfig;

import java.util.List;
import java.util.Map;

/**
 * 平台配置业务（对齐原 foxbook service/PlatformConfigService）
 */
public interface PlatformConfigService extends IService<PlatformConfig> {

    String SITE_SETTINGS_KEY = "site_settings";
    String ALIYUN_DIRECT_MAIL_KEY = "aliyun_direct_mail";

    /**
     * 按配置键获取配置载荷（含 id/name/key/value，value 合并默认值）
     *
     * @param key 配置键
     * @return 配置载荷，不存在返回 null
     */
    Map<String, Object> getConfigPayload(String key);

    /**
     * 保存配置（新增或更新）
     *
     * @param key   配置键
     * @param name  配置名称
     * @param value 配置值（Map）
     * @return 保存后的 PlatformConfig
     */
    PlatformConfig saveConfig(String key, String name, Map<String, Object> value);

    /**
     * 获取公开站点配置（对齐 PHP getPublicSiteSettings）
     */
    Map<String, Object> getPublicSiteSettings();

    /**
     * 获取全部配置列表（对齐 PHP PlatformConfigModel::getAllConfigs）
     */
    List<Map<String, Object>> getAllConfigs();
}
