package com.zbtech.community.service;

import com.zbtech.community.common.RedisUtil;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * API Token 签发与校验（对齐原 foxbook UserAccountService.issueApiToken + RequestAuthService）
 * token 作为 Redis key，value 为 user_id/admin_id，TTL 默认 7 天
 *
 * 管理端 token 与用户端 token 共用 RedisUtil，但使用独立前缀 admin: 进行隔离，
 * 避免两套体系的 token 互相串号。签发逻辑参考 AuthServiceImpl.accountLogin。
 */
@Service
public class TokenService {

    /** 用户端 token 默认 TTL：7 天 */
    public static final long API_TOKEN_TTL = 7L * 24 * 3600;

    /** 管理端 token 默认 TTL：1 小时 */
    public static final long ADMIN_TOKEN_TTL = 3600L;

    /** 管理端「记住我」token TTL：7 天 */
    public static final long ADMIN_TOKEN_TTL_REMEMBER = 7L * 24 * 3600;

    /** 管理端 token 的 Redis key 前缀 */
    private static final String ADMIN_TOKEN_PREFIX = "admin:";

    private final RedisUtil redis;

    public TokenService(RedisUtil redis) {
        this.redis = redis;
    }

    // ===================== 用户端 token =====================

    public String issueApiToken(Integer userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.set(token, String.valueOf(userId), API_TOKEN_TTL);
        return token;
    }

    public Integer resolveApiUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String uid = redis.get(token);
        if (uid == null) {
            return null;
        }
        try {
            return Integer.valueOf(uid);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void revoke(String token) {
        redis.delete(token);
    }

    // ===================== 管理端 token =====================

    /** 签发管理员 token；remember=true 时使用 7 天 TTL，否则 1 小时 */
    public String issueAdminToken(Integer adminId, boolean remember) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long ttl = remember ? ADMIN_TOKEN_TTL_REMEMBER : ADMIN_TOKEN_TTL;
        redis.set(ADMIN_TOKEN_PREFIX + token, String.valueOf(adminId), ttl);
        return token;
    }

    /** 解析管理员 token，返回 admin_id；无效或过期返回 null */
    public Integer resolveAdminId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String aid = redis.get(ADMIN_TOKEN_PREFIX + token);
        if (aid == null) {
            return null;
        }
        try {
            return Integer.valueOf(aid);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 吊销管理员 token（登出） */
    public void revokeAdminToken(String token) {
        if (token != null) {
            redis.delete(ADMIN_TOKEN_PREFIX + token);
        }
    }
}
