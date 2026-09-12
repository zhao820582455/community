package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbtech.community.entity.PlatformConfig;
import com.zbtech.community.mapper.PlatformConfigMapper;
import com.zbtech.community.service.PlatformConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PlatformConfigServiceImpl extends ServiceImpl<PlatformConfigMapper, PlatformConfig> implements PlatformConfigService {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public Map<String, Object> getConfigPayload(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        Map<String, Object> meta = getConfigMeta(key);
        String defaultName = meta != null ? (String) meta.get("name") : key;
        Map<String, Object> defaults = meta != null ? (Map<String, Object>) meta.get("defaults") : Map.of();

        LambdaQueryWrapper<PlatformConfig> w = new LambdaQueryWrapper<>();
        w.eq(PlatformConfig::getKey, key);
        PlatformConfig record = getOne(w);

        Map<String, Object> value = new LinkedHashMap<>(defaults);
        if (record != null && record.getValue() != null && !record.getValue().isBlank()) {
            Map<String, Object> stored = parseJsonToMap(record.getValue());
            value.putAll(stored);
        }

        // 特殊处理：阿里云邮件配置规范化
        if (ALIYUN_DIRECT_MAIL_KEY.equals(key)) {
            value = normalizeAliyunDirectMailSettings(value);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", record != null ? record.getId() : null);
        result.put("name", record != null && record.getName() != null && !record.getName().isBlank()
                ? record.getName() : defaultName);
        result.put("key", key);
        result.put("value", value);
        return result;
    }

    @Override
    @Transactional
    public PlatformConfig saveConfig(String key, String name, Map<String, Object> value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("配置键不能为空");
        }

        LambdaQueryWrapper<PlatformConfig> w = new LambdaQueryWrapper<>();
        w.eq(PlatformConfig::getKey, key);
        PlatformConfig record = getOne(w);

        Map<String, Object> meta = getConfigMeta(key);
        String resolvedName = (name != null && !name.trim().isEmpty()) ? name.trim()
                : (meta != null ? (String) meta.get("name") : key);

        String jsonValue;
        try {
            jsonValue = JSON.writeValueAsString(value != null ? value : Map.of());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("配置值序列化失败", e);
        }

        if (record == null) {
            record = new PlatformConfig();
            record.setName(resolvedName);
            record.setKey(key);
            record.setValue(jsonValue);
            record.setCreated_at(LocalDateTime.now());
            record.setUpdated_at(LocalDateTime.now());
            save(record);
        } else {
            record.setName(resolvedName);
            record.setValue(jsonValue);
            record.setUpdated_at(LocalDateTime.now());
            updateById(record);
        }
        return getById(record.getId());
    }

    @Override
    public Map<String, Object> getPublicSiteSettings() {
        Map<String, Object> site = getSiteSettings();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", site.get("title"));
        result.put("subtitle", site.get("subtitle"));
        result.put("description", site.get("description"));
        result.put("logo", site.get("logo"));
        result.put("logo_url", site.get("logo_url"));
        result.put("icp", site.get("icp"));
        return result;
    }

    @Override
    public List<Map<String, Object>> getAllConfigs() {
        List<PlatformConfig> configs = list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (PlatformConfig c : configs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("name", c.getName());
            item.put("key", c.getKey());
            item.put("value", parseJsonToMap(c.getValue()));
            result.add(item);
        }
        return result;
    }

    // ===================== 私有辅助 =====================

    private Map<String, Object> getSiteSettings() {
        Map<String, Object> payload = getConfigPayload(SITE_SETTINGS_KEY);
        Map<String, Object> site = payload != null
                ? (Map<String, Object>) payload.get("value")
                : getSiteDefaults();

        Map<String, Object> defaults = getSiteDefaults();
        site = new LinkedHashMap<>(site);

        // 填充默认值
        for (String field : new String[]{"title", "subtitle", "description", "logo"}) {
            Object val = site.get(field);
            if (val == null || val.toString().trim().isEmpty()) {
                site.put(field, defaults.get(field));
            }
        }
        if (!site.containsKey("icp") || site.get("icp") == null) {
            site.put("icp", "");
        }

        String logo = site.get("logo") != null ? site.get("logo").toString().trim() : "";
        site.put("logo_url", resolveFileUrl(logo));

        return site;
    }

    private Map<String, Object> getConfigMeta(String key) {
        return switch (key) {
            case SITE_SETTINGS_KEY -> Map.of(
                    "name", "站点配置",
                    "defaults", getSiteDefaults());
            case ALIYUN_DIRECT_MAIL_KEY -> Map.of(
                    "name", "阿里云邮件推送配置",
                    "defaults", getAliyunDirectMailDefaults());
            case "wechat_mnp" -> Map.of(
                    "name", "微信小程序配置",
                    "defaults", Map.of(
                            "app_id", "",
                            "secret", "",
                            "token", "",
                            "aes_key", ""));
            case "aliyun_oss" -> Map.of(
                    "name", "阿里云OSS配置",
                    "defaults", Map.of(
                            "accessKeyId", "",
                            "accessKeySecret", "",
                            "regionId", "",
                            "roleArn", "",
                            "bucket", ""));
            default -> null;
        };
    }

    private Map<String, Object> getSiteDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("title", "zbtech");
        defaults.put("subtitle", "IT 技术交流社区");
        defaults.put("description", "IT 技术交流社区，分享经验、记录实践，一起成长。");
        defaults.put("logo", "/logo.png");
        defaults.put("icp", "");
        return defaults;
    }

    private Map<String, Object> getAliyunDirectMailDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("access_key_id", "");
        defaults.put("access_key_secret", "");
        defaults.put("account_name", "");
        defaults.put("from_alias", "");
        defaults.put("reply_to_address", false);
        defaults.put("ssl_verify", true);
        return defaults;
    }

    private Map<String, Object> normalizeAliyunDirectMailSettings(Map<String, Object> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("access_key_id", strOrEmpty(config.get("access_key_id")));
        result.put("access_key_secret", strOrEmpty(config.get("access_key_secret")));
        result.put("account_name", strOrEmpty(config.get("account_name")));
        result.put("from_alias", strOrEmpty(config.get("from_alias")));
        result.put("reply_to_address", config.containsKey("reply_to_address")
                ? toBool(config.get("reply_to_address")) : false);
        result.put("ssl_verify", config.containsKey("ssl_verify")
                ? toBool(config.get("ssl_verify")) : true);
        return result;
    }

    private String resolveFileUrl(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/")) {
            return path;
        }
        // 简化：非 URL 路径原样返回（PHP 版调 FileService::getFileUrl，Java 版暂无 OSS 集成）
        return path;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Object parsed = JSON.readValue(json, Object.class);
            if (parsed instanceof Map) {
                return (Map<String, Object>) parsed;
            }
        } catch (JsonProcessingException e) {
            // 忽略解析错误
        }
        return new LinkedHashMap<>();
    }

    private String strOrEmpty(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private boolean toBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.parseBoolean(o.toString());
    }
}
