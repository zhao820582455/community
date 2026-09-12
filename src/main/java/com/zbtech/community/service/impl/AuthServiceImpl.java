package com.zbtech.community.service.impl;

import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.RedisUtil;
import com.zbtech.community.entity.Users;
import com.zbtech.community.service.AuthService;
import com.zbtech.community.service.TokenService;
import com.zbtech.community.service.UsersService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 鉴权实现：登录 / 注册 / 验证码 / token 签发
 * 密码使用 BCrypt，兼容原 foxbook(PHP password_hash) 的 $2y$ 格式
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String SCENE_LOGIN = "login";
    private static final String SCENE_REGISTER = "register";
    /** 万能验证码：仅限联调/测试环境使用，任何邮箱输入该验证码均可通过校验（上线前必须移除或改为配置开关） */
    private static final String UNIVERSAL_CODE = "369258";
    private static final long CODE_TTL = 10 * 60L;
    private static final long COOLDOWN = 60L;

    private final UsersService usersService;
    private final TokenService tokenService;
    private final RedisUtil redis;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UsersService usersService, TokenService tokenService, RedisUtil redis) {
        this.usersService = usersService;
        this.tokenService = tokenService;
        this.redis = redis;
    }

    @Override
    public Map<String, Object> accountLogin(String username, String password) {
        Users user = usersService.lambdaQuery()
                .eq(Users::getUsername, username)
                .or()
                .eq(Users::getEmail, username)
                .one();
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(ErrorCode.FAIL, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == -1) {
            throw new BizException(ErrorCode.FAIL, "该账号已被禁用，请联系管理员处理");
        }
        // 刷新登录信息
        user.setLast_login_time(LocalDateTime.now());
        user.setLast_login_ip("");
        user.setTerminal(2);
        usersService.updateById(user);

        String token = tokenService.issueApiToken(user.getId());
        Map<String, Object> result = new HashMap<>(4);
        result.put("token", token);
        result.put("user", hidePassword(user));
        return result;
    }

    @Override
    public void accountRegister(String username, String email, String password, String code) {
        if (usersService.lambdaQuery().eq(Users::getUsername, username).count() > 0) {
            throw new BizException(ErrorCode.FAIL, "用户名已存在");
        }
        if (usersService.lambdaQuery().eq(Users::getEmail, email).count() > 0) {
            throw new BizException(ErrorCode.FAIL, "邮箱已被注册");
        }
        if (!verifyCode(email, SCENE_REGISTER, code)) {
            throw new BizException(ErrorCode.FAIL, "验证码错误或已过期");
        }
        Users user = new Users();
        user.setSn(generateSn());
        user.setUsername(username);
        user.setNickname("用户" + System.currentTimeMillis());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setAvatar("/avatar/" + (new SecureRandom().nextInt(50) + 1) + ".png");
        user.setStatus(0);
        user.setTerminal(2);
        usersService.save(user);
    }

    @Override
    public void sendEmailCode(String email, String scene) {
        if (!SCENE_LOGIN.equals(scene) && !SCENE_REGISTER.equals(scene)) {
            throw new BizException(ErrorCode.FAIL, "验证码场景不正确");
        }
        if (SCENE_LOGIN.equals(scene) && usersService.lambdaQuery().eq(Users::getEmail, email).count() == 0) {
            throw new BizException(ErrorCode.FAIL, "该邮箱尚未注册账号");
        }
        if (SCENE_REGISTER.equals(scene) && usersService.lambdaQuery().eq(Users::getEmail, email).count() > 0) {
            throw new BizException(ErrorCode.FAIL, "该邮箱已被注册");
        }
        String cooldownKey = cooldownKey(email, scene);
        if (redis.hasKey(cooldownKey)) {
            throw new BizException(ErrorCode.FAIL, "请一分钟后再试");
        }
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        redis.set(codeKey(email, scene), email + "|" + scene + "|" + code, CODE_TTL);
        redis.set(cooldownKey, "1", COOLDOWN);
        // TODO: 接入邮件服务商（原 foxbook 用阿里云 DirectMail）实际发送。当前仅打印，便于联调。
        System.out.println("[zbtech] 邮箱验证码 email=" + email + " scene=" + scene + " code=" + code);
    }

    @Override
    public Map<String, Object> mnpLogin(String code) {
        throw new BizException(ErrorCode.FAIL, "微信小程序登录暂未集成，请使用账号登录");
    }

    @Override
    public Users currentUserInfo(Integer userId) {
        Users user = usersService.getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.FAIL, "用户不存在");
        }
        return hidePassword(user);
    }

    private boolean verifyCode(String email, String scene, String code) {
        // 万能验证码：联调环境免真实验证码（369258）
        if (UNIVERSAL_CODE.equals(code)) {
            return true;
        }
        String raw = redis.get(codeKey(email, scene));
        if (raw == null) {
            return false;
        }
        String[] parts = raw.split("\\|");
        if (parts.length < 3) {
            return false;
        }
        boolean ok = parts[2].equals(code);
        if (ok) {
            redis.delete(codeKey(email, scene));
        }
        return ok;
    }

    private String codeKey(String email, String scene) {
        return "email_verification:" + scene + ":" + md5(email.toLowerCase());
    }

    private String cooldownKey(String email, String scene) {
        return "email_verification_cooldown:" + scene + ":" + md5(email.toLowerCase());
    }

    private String md5(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] b = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (Exception e) {
            return s;
        }
    }

    private String generateSn() {
        return "U" + System.currentTimeMillis();
    }

    private Users hidePassword(Users user) {
        user.setPassword(null);
        return user;
    }
}
