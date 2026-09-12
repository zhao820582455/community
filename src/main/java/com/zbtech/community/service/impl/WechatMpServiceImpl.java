package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.RedisUtil;
import com.zbtech.community.entity.PlatformConfig;
import com.zbtech.community.entity.Users;
import com.zbtech.community.mapper.WechatMpMapper;
import com.zbtech.community.service.PlatformConfigService;
import com.zbtech.community.service.TokenService;
import com.zbtech.community.service.UsersService;
import com.zbtech.community.service.WechatMpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
/**
 * 微信小程序业务实现（对齐原 foxbook WeChatMnpService.getMnpResByCode + Auth::mnpLogin）
 *
 * 流程：
 *  1. js_code 经微信 code2Session 接口换取 openid / session_key / unionid
 *  2. 按 openid 在 users 表查用户，不存在则创建（terminal=1 小程序）
 *  3. 刷新登录信息并签发 API token（与账号登录一致，存入 Redis）
 *  4. session_key 缓存到 Redis，供后续解密手机号/用户信息使用
 */
@Service
public class WechatMpServiceImpl extends ServiceImpl<WechatMpMapper, Users> implements WechatMpService {

    /** 微信 code2Session 接口地址（HTTPS GET） */
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    /** session_key 在 Redis 中的前缀，TTL 与 API token 一致（7 天） */
    private static final String SESSION_KEY_PREFIX = "wx:session_key:";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WechatMpServiceImpl.class);

    /** 小程序端固定终端标识 */
    private static final int TERMINAL_MNP = 1;

    private final UsersService usersService;
    private final TokenService tokenService;
    private final PlatformConfigService platformConfigService;
    private final RedisUtil redis;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 可选：application.yml 中的 wx.mp.* 配置，作为数据库配置的兜底。
     * 若数据库 platform_config(key=wechat_mnp) 已配置则优先使用数据库。
     */
    @Value("${wx.mp.appid:}")
    private String appIdFromYml;
    @Value("${wx.mp.secret:}")
    private String secretFromYml;
    @Value("${wx.mp.aes-key:}")
    private String aesKeyFromYml;

    public WechatMpServiceImpl(UsersService usersService, TokenService tokenService,
                               PlatformConfigService platformConfigService, RedisUtil redis) {
        this.usersService = usersService;
        this.tokenService = tokenService;
        this.platformConfigService = platformConfigService;
        this.redis = redis;
    }

    @Override
    public Map<String, Object> mnpLogin(String jsCode) {
        if (jsCode == null || jsCode.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "微信登录 code 不能为空");
        }

        // 1. 读取小程序配置（appid / secret）
        WechatConfig cfg = loadConfig();
        if (cfg == null || isBlank(cfg.getAppId()) || isBlank(cfg.getSecret())) {
            throw new BizException(ErrorCode.FAIL, "微信小程序配置缺失，请联系管理员");
        }

        // 2. 调用 code2Session 换取 openid / session_key / unionid
        Code2SessionResp resp = code2Session(cfg, jsCode);
        if (resp == null || isBlank(resp.getOpenid())) {
            throw new BizException(ErrorCode.FAIL, "微信授权失败，请重试");
        }

        String openid = resp.getOpenid();
        String unionid = resp.getUnionid();
        String sessionKey = resp.getSessionKey();

        // 2.1 缓存 session_key（解密手机号/用户信息时使用）
        if (!isBlank(sessionKey)) {
            redis.set(SESSION_KEY_PREFIX + openid, sessionKey, TokenService.API_TOKEN_TTL);
        }

        // 3. 按 openid 查用户，不存在则创建（统一 users 表）
        Users user = baseMapper.selectByOpenid(openid);
        if (user == null) {
            user = new Users();
            user.setSn("U" + System.currentTimeMillis());
            user.setNickname("用户" + System.currentTimeMillis());
            // 初始密码：随机串 + BCrypt 加密（小程序用户走 token 登录，无需密码）
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setAvatar("/avatar/" + (new SecureRandom().nextInt(50) + 1) + ".png");
            user.setOpenid(openid);
            if (!isBlank(unionid)) {
                user.setUnionid(unionid);
            }
            user.setStatus(0);
            user.setTerminal(TERMINAL_MNP);
            // 仅调用 UsersService 已有的 save / getById
            usersService.save(user);
            user = usersService.getById(user.getId());
        } else {
            // 已存在用户：若此前无 unionid 则补充（unionid 用于跨应用打通）
            if (!isBlank(unionid) && isBlank(user.getUnionid())) {
                user.setUnionid(unionid);
                // 仅更新 unionid 字段，复用 IService 已有方法（不改 UsersService 定义）
                Users patch = new Users();
                patch.setId(user.getId());
                patch.setUnionid(unionid);
                usersService.updateById(patch);
            }
        }

        // 4. 刷新登录信息（terminal=1 小程序），复用 IService 已有方法
        Users loginPatch = new Users();
        loginPatch.setId(user.getId());
        loginPatch.setLast_login_time(LocalDateTime.now());
        loginPatch.setLast_login_ip("");
        loginPatch.setTerminal(TERMINAL_MNP);
        usersService.updateById(loginPatch);

        // 5. 签发 token 存 Redis（与账号登录一致）
        String token = tokenService.issueApiToken(user.getId());

        Map<String, Object> result = new HashMap<>(4);
        result.put("token", token);
        result.put("user", hidePassword(usersService.getById(user.getId())));
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> decryptUserInfo(String openid, String encryptedData, String iv) {
        if (isBlank(openid) || isBlank(encryptedData) || isBlank(iv)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "解密参数不完整");
        }
        String sessionKey = redis.get(SESSION_KEY_PREFIX + openid);
        if (isBlank(sessionKey)) {
            throw new BizException(ErrorCode.FAIL, "微信会话已失效，请重新登录");
        }
        try {
            // AES-128-CBC + PKCS7（Java 的 PKCS5Padding 对 16 字节块等价于 PKCS7）
            byte[] key = Base64.getDecoder().decode(sessionKey);
            byte[] data = Base64.getDecoder().decode(encryptedData);
            byte[] ivBytes = Base64.getDecoder().decode(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(ivBytes));
            byte[] decrypted = cipher.doFinal(data);
            String json = new String(decrypted, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, Map.class);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.FAIL, "微信数据解密失败：" + e.getMessage());
        }
    }

    @Override
    public String mediaCheckAsync(String openid, String mediaUrl) {
        // 桩实现：对齐 PHP WeChatMnpService::mediaCheckAsync（微信 wxa/media_check_async，
        // media_type=2 图片、version=2、scene=3）。真实对接需开通内容安全并配置 access_token，
        // 此处生成 trace_id 并记录日志，审核结果由微信异步回调（暂未接入）。
        String traceId = "mc_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[桩] 微信图片异步审核 media_check_async: openid={}, media_url={}, media_type=2, scene=3 -> trace_id={}",
                openid, mediaUrl, traceId);
        return traceId;
    }

    /**
     * 调用微信 code2Session 接口
     * 文档：GET https://api.weixin.qq.com/sns/jscode2session
     *      ?appid=APPID&secret=SECRET&js_code=JSCODE&grant_type=authorization_code
     */
    private Code2SessionResp code2Session(WechatConfig cfg, String jsCode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(CODE2SESSION_URL)
                .queryParam("appid", cfg.getAppId())
                .queryParam("secret", cfg.getSecret())
                .queryParam("js_code", jsCode)
                .queryParam("grant_type", "authorization_code")
                .build().toUri();
        try {
            String body = restTemplate.getForObject(uri, String.class);
            if (body == null) {
                return null;
            }
            JsonNode node = objectMapper.readTree(body);
            // 微信错误返回体含 errcode（非 0 为失败），成功返回 openid/session_key/unionid
            if (node.has("errcode") && node.get("errcode").asInt(0) != 0) {
                String errmsg = node.has("errmsg") ? node.get("errmsg").asText() : "微信接口返回错误";
                throw new BizException(ErrorCode.FAIL, "微信登录失败：" + errmsg);
            }
            Code2SessionResp resp = new Code2SessionResp();
            resp.setOpenid(node.has("openid") ? node.get("openid").asText() : null);
            resp.setSessionKey(node.has("session_key") ? node.get("session_key").asText() : null);
            resp.setUnionid(node.has("unionid") ? node.get("unionid").asText() : null);
            return resp;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.FAIL, "调用微信接口异常：" + e.getMessage());
        }
    }

    /**
     * 读取微信小程序配置：优先数据库 platform_config(key=wechat_mnp)，
     * 数据库未配置时回退到 application.yml 的 wx.mp.*。
     * 不修改 PlatformConfigService，仅读取。
     */
    private WechatConfig loadConfig() {
        WechatConfig cfg = new WechatConfig();
        PlatformConfig pc = platformConfigService.lambdaQuery()
                .eq(PlatformConfig::getKey, "wechat_mnp")
                .one();
        if (pc != null && pc.getValue() != null && !pc.getValue().isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(pc.getValue());
                cfg.setAppId(node.has("app_id") ? node.get("app_id").asText() : null);
                cfg.setSecret(node.has("secret") ? node.get("secret").asText() : null);
                cfg.setAesKey(node.has("aes_key") ? node.get("aes_key").asText() : null);
            } catch (Exception ignored) {
                // 解析失败则继续走 yml 兜底
            }
        }
        if (isBlank(cfg.getAppId()) && !isBlank(appIdFromYml)) {
            cfg.setAppId(appIdFromYml);
        }
        if (isBlank(cfg.getSecret()) && !isBlank(secretFromYml)) {
            cfg.setSecret(secretFromYml);
        }
        if (isBlank(cfg.getAesKey()) && !isBlank(aesKeyFromYml)) {
            cfg.setAesKey(aesKeyFromYml);
        }
        return cfg;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 返回用户信息并抹掉密码字段 */
    private Users hidePassword(Users user) {
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    /** 小程序配置载体 */
    private static class WechatConfig {
        private String appId;
        private String secret;
        private String aesKey;

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getAesKey() { return aesKey; }
        public void setAesKey(String aesKey) { this.aesKey = aesKey; }
    }

    /** code2Session 响应载体 */
    private static class Code2SessionResp {
        private String openid;
        private String sessionKey;
        private String unionid;

        public String getOpenid() { return openid; }
        public void setOpenid(String openid) { this.openid = openid; }
        public String getSessionKey() { return sessionKey; }
        public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
        public String getUnionid() { return unionid; }
        public void setUnionid(String unionid) { this.unionid = unionid; }
    }
}
