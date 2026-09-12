package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.AuthTag;
import com.zbtech.community.entity.UserAuthTag;
import com.zbtech.community.entity.UserFollow;
import com.zbtech.community.entity.Users;
import com.zbtech.community.mapper.AuthTagMapper;
import com.zbtech.community.mapper.UserAuthTagMapper;
import com.zbtech.community.mapper.UsersMapper;
import com.zbtech.community.service.MessageService;
import com.zbtech.community.service.UserFollowService;
import com.zbtech.community.service.UsersService;
import com.zbtech.community.vo.UserBriefVO;
import com.zbtech.community.vo.UserVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户业务实现（对齐原 foxbook app/service + adminapi/controller/User.php）
 * 管理端方法沿用数据表 0=正常 / -1=禁用 的存储约定，对外接口统一以 1=正常 / 0=禁用 展示。
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements UsersService {

    /** 系统消息发送人占位 ID（对应原 PHP MessageService::sendMessage('system', ...)） */
    private static final int SYSTEM_SENDER_ID = 0;

    private final UserFollowService userFollowService;
    private final MessageService messageService;
    private final UserAuthTagMapper userAuthTagMapper;
    private final AuthTagMapper authTagMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsersServiceImpl(UserFollowService userFollowService, @Lazy MessageService messageService,
                           UserAuthTagMapper userAuthTagMapper, AuthTagMapper authTagMapper) {
        this.userFollowService = userFollowService;
        this.messageService = messageService;
        this.userAuthTagMapper = userAuthTagMapper;
        this.authTagMapper = authTagMapper;
    }

    @Override
    public UserVO getUserInfo(Integer userId) {
        Users u = getById(userId);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return UserVO.from(u);
    }

    @Override
    public UserVO getUserById(Integer viewerId, Integer targetId) {
        Users u = getById(targetId);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        UserVO v = UserVO.from(u);
        v.setFollow_count((int) followCount(targetId));
        v.setFans_count((int) fansCount(targetId));
        if (viewerId != null && !viewerId.equals(targetId)) {
            v.setIs_following(userFollowService.isFollowing(viewerId, targetId));
            v.setIs_mutual_follow(userFollowService.isMutualFollow(viewerId, targetId));
        }
        return v;
    }

    @Override
    public List<UserVO> getHotUsers() {
        List<Users> list = list(new QueryWrapper<Users>()
                .eq("status", 0)
                .orderByDesc("post_thumb_count")
                .orderByDesc("post_count")
                .last("LIMIT 10"));
        return list.stream().map(UserVO::from).collect(Collectors.toList());
    }

    @Override
    public void updateProfile(Integer userId, String field, String value) {
        Set<String> allowed = Set.of("nickname", "avatar", "introduction", "phone", "email");
        if (field == null || !allowed.contains(field) || value == null) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        if ("phone".equals(field) && isDuplicate("phone", value, userId)) {
            throw new BizException(ErrorCode.FAIL, "手机号已存在");
        }
        if ("email".equals(field) && isDuplicate("email", value, userId)) {
            throw new BizException(ErrorCode.FAIL, "邮箱已存在");
        }
        update(new UpdateWrapper<Users>().eq("id", userId).set(field, value));
    }

    @Override
    public void saveInfo(Integer userId, String nickname, String avatar, String introduction, String phone, String email) {
        UpdateWrapper<Users> uw = new UpdateWrapper<Users>().eq("id", userId);
        boolean has = false;
        if (nickname != null) { uw.set("nickname", nickname); has = true; }
        if (avatar != null) { uw.set("avatar", avatar); has = true; }
        if (introduction != null) { uw.set("introduction", introduction); has = true; }
        if (phone != null) {
            if (isDuplicate("phone", phone, userId)) throw new BizException(ErrorCode.FAIL, "手机号已存在");
            uw.set("phone", phone); has = true;
        }
        if (email != null) {
            if (isDuplicate("email", email, userId)) throw new BizException(ErrorCode.FAIL, "邮箱已存在");
            uw.set("email", email); has = true;
        }
        if (has) {
            update(uw);
        }
    }

    @Override
    public List<UserBriefVO> searchUsers(String keyword) {
        List<Users> list = list(new QueryWrapper<Users>()
                .eq("status", 0)
                .and(w -> w.like("nickname", keyword).or().like("username", keyword).or().like("sn", keyword))
                .last("LIMIT 10"));
        return list.stream().map(UserBriefVO::from).collect(Collectors.toList());
    }

    // ===================== 管理端用户管理 =====================

    @Override
    public IPage<Users> getAdminUserList(Map<String, Object> queryForm, int page, int pageSize) {
        Page<Users> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Users> w = new LambdaQueryWrapper<>();
        String nickname = getStr(queryForm, "nickname");
        if (!isBlank(nickname)) {
            w.like(Users::getNickname, nickname);
        }
        String username = getStr(queryForm, "username");
        if (!isBlank(username)) {
            w.like(Users::getUsername, username);
        }
        String email = getStr(queryForm, "email");
        if (!isBlank(email)) {
            w.like(Users::getEmail, email);
        }
        String phone = getStr(queryForm, "phone");
        if (!isBlank(phone)) {
            w.like(Users::getPhone, phone);
        }
        Integer displayStatus = getInt(queryForm, "status");
        if (displayStatus != null) {
            w.eq(Users::getStatus, toStoredStatus(displayStatus));
        }
        Object start = queryForm == null ? null : queryForm.get("created_at_start");
        Object end = queryForm == null ? null : queryForm.get("created_at_end");
        if (start != null && !start.toString().trim().isEmpty()) {
            w.apply("created_at >= {0}", start.toString().trim());
        }
        if (end != null && !end.toString().trim().isEmpty()) {
            w.apply("created_at <= {0}", end.toString().trim());
        }
        w.orderByDesc(Users::getCreated_at);
        IPage<Users> result = page(p, w);
        // 对外以 1正常 / 0禁用 展示
        for (Users u : result.getRecords()) {
            u.setStatus(toDisplayStatus(u.getStatus()));
        }
        return result;
    }

    @Override
    @Transactional
    public void saveFrontUser(Map<String, Object> params) {
        String nickname = getStr(params, "nickname");
        if (isBlank(nickname) || nickname.length() > 50) {
            throw new BizException(ErrorCode.PARAM_ERROR, "昵称必填且不超过50个字符");
        }
        String email = getStr(params, "email");
        Integer gender = getInt(params, "gender");
        if (gender != null && (gender < 0 || gender > 2)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "性别取值非法");
        }
        Integer displayStatus = getInt(params, "status");
        if (displayStatus == null) displayStatus = 1;
        String username = getStr(params, "username");
        String password = getStr(params, "password");
        Integer id = getInt(params, "id");

        // 用户名与密码须同时提供或同时不提供
        boolean hasUsername = !isBlank(username);
        boolean hasPassword = !isBlank(password);
        if (hasUsername != hasPassword) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名和密码必须同时提供");
        }
        if (hasPassword && (password.length() < 6 || password.length() > 50)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "密码长度需为6-50位");
        }

        if (id != null) {
            Users user = getById(id);
            if (user == null) {
                throw new BizException(ErrorCode.FAIL, "用户不存在");
            }
            if (hasUsername && count(new LambdaQueryWrapper<Users>()
                    .eq(Users::getUsername, username).ne(Users::getId, id)) > 0) {
                throw new BizException(ErrorCode.FAIL, "用户名已存在");
            }
            if (!isBlank(email) && count(new LambdaQueryWrapper<Users>()
                    .eq(Users::getEmail, email).ne(Users::getId, id)) > 0) {
                throw new BizException(ErrorCode.FAIL, "邮箱已存在");
            }
            user.setNickname(nickname);
            user.setEmail(isBlank(email) ? null : email);
            user.setGender(gender == null ? 0 : gender);
            user.setBirthday(parseBirthday(getStr(params, "birthday")));
            user.setStatus(toStoredStatus(displayStatus));
            if (hasUsername) {
                user.setUsername(username);
            }
            if (hasPassword) {
                user.setPassword(passwordEncoder.encode(password));
            }
            updateById(user);
        } else {
            if (hasUsername && count(new LambdaQueryWrapper<Users>()
                    .eq(Users::getUsername, username)) > 0) {
                throw new BizException(ErrorCode.FAIL, "用户名已存在");
            }
            if (!isBlank(email) && count(new LambdaQueryWrapper<Users>()
                    .eq(Users::getEmail, email)) > 0) {
                throw new BizException(ErrorCode.FAIL, "邮箱已存在");
            }
            Users user = new Users();
            user.setSn("U" + System.currentTimeMillis());
            user.setNickname(nickname);
            user.setEmail(isBlank(email) ? null : email);
            user.setGender(gender == null ? 0 : gender);
            user.setBirthday(parseBirthday(getStr(params, "birthday")));
            user.setStatus(toStoredStatus(displayStatus));
            user.setAvatar("/avatar/" + (new java.security.SecureRandom().nextInt(50) + 1) + ".png");
            user.setPost_count(0);
            user.setFollow_count(0);
            user.setFans_count(0);
            user.setPost_thumb_count(0);
            user.setPost_collect_count(0);
            user.setTerminal(2);
            if (hasUsername) {
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(password));
            }
            save(user);
        }
    }

    @Override
    @Transactional
    public void deleteFrontUser(Integer id) {
        Users user = getById(id);
        if (user == null) {
            throw new BizException(ErrorCode.FAIL, "用户不存在");
        }
        removeById(id);
    }

    @Override
    @Transactional
    public void updateFrontUserStatus(Integer id, Integer displayStatus) {
        Users user = getById(id);
        if (user == null) {
            throw new BizException(ErrorCode.FAIL, "用户不存在");
        }
        int storedStatus = toStoredStatus(displayStatus);
        update(new LambdaUpdateWrapper<Users>()
                .eq(Users::getId, id).set(Users::getStatus, storedStatus));

        // 发送系统消息通知用户（与原 PHP toggleUserStatus 保持一致）
        String msgText;
        if (storedStatus == 0) {
            msgText = "你的账号已被管理员恢复使用，现在可以继续正常登录和发帖。";
        } else {
            msgText = "你的账号已被管理员禁用，如有疑问请联系平台管理员。";
        }
        messageService.sendMessage(SYSTEM_SENDER_ID, id, msgText, MessageService.TYPE_SYSTEM, null);
    }

    @Override
    @Transactional
    public Map<String, Object> setAuthTags(Integer userId, List<Integer> tagIds) {
        Users user = getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.FAIL, "用户不存在");
        }
        // 校验标签合法性
        List<Integer> validTagIds = new ArrayList<>();
        if (tagIds != null && !tagIds.isEmpty()) {
            List<AuthTag> tags = authTagMapper.selectList(new LambdaQueryWrapper<AuthTag>()
                    .in(AuthTag::getId, tagIds));
            Set<Integer> existIds = tags.stream().map(AuthTag::getId).collect(Collectors.toSet());
            for (Integer tid : tagIds) {
                if (tid != null && existIds.contains(tid) && !validTagIds.contains(tid)) {
                    validTagIds.add(tid);
                }
            }
        }
        // 先删除原有关联，再批量写入
        userAuthTagMapper.delete(new LambdaQueryWrapper<UserAuthTag>()
                .eq(UserAuthTag::getUser_id, userId));
        if (!validTagIds.isEmpty()) {
            List<UserAuthTag> list = new ArrayList<>(validTagIds.size());
            LocalDateTime now = LocalDateTime.now();
            for (Integer tid : validTagIds) {
                UserAuthTag uat = new UserAuthTag();
                uat.setUser_id(userId);
                uat.setTag_id(tid);
                uat.setCreated_at(now);
                uat.setUpdated_at(now);
                list.add(uat);
            }
            // MyBatis-Plus 无 insertIgnore，逐条写入
            saveBatchUserAuthTag(list);
        }
        List<AuthTag> authTags = validTagIds.isEmpty() ? Collections.emptyList()
                : authTagMapper.selectList(new LambdaQueryWrapper<AuthTag>().in(AuthTag::getId, validTagIds));

        Map<String, Object> result = new HashMap<>(4);
        result.put("user_id", userId);
        result.put("tag_ids", validTagIds);
        result.put("auth_tags", authTags);
        return result;
    }

    private void saveBatchUserAuthTag(List<UserAuthTag> list) {
        for (UserAuthTag uat : list) {
            userAuthTagMapper.insert(uat);
        }
    }

    // ===================== 私有：状态映射 / 参数 =====================

    /** 对外展示状态(1正常/0禁用) -> 库内状态(0正常/-1禁用) */
    private int toStoredStatus(Integer displayStatus) {
        return (displayStatus != null && displayStatus == 1) ? 0 : -1;
    }

    /** 库内状态(0正常/-1禁用) -> 对外展示状态(1正常/0禁用) */
    private int toDisplayStatus(Integer storedStatus) {
        return (storedStatus != null && storedStatus == 0) ? 1 : 0;
    }

    private LocalDate parseBirthday(String s) {
        if (isBlank(s)) return null;
        try {
            return LocalDate.parse(s.trim().substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isDuplicate(String column, String value, Integer userId) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return count(new QueryWrapper<Users>().eq(column, value).ne("id", userId)) > 0;
    }

    private long followCount(Integer userId) {
        return userFollowService.count(new QueryWrapper<UserFollow>().eq("user_id", userId));
    }

    private long fansCount(Integer userId) {
        return userFollowService.count(new QueryWrapper<UserFollow>().eq("follow_user_id", userId));
    }

    private String getStr(Map<String, Object> m, String k) {
        if (m == null) return null;
        Object v = m.get(k);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private Integer getInt(Map<String, Object> m, String k) {
        if (m == null) return null;
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
