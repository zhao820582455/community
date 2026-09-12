package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.Users;
import com.zbtech.community.vo.UserBriefVO;
import com.zbtech.community.vo.UserVO;

import java.util.List;
import java.util.Map;

public interface UsersService extends IService<Users> {

    /** 当前登录用户资料（脱敏） */
    UserVO getUserInfo(Integer userId);

    /** 他人主页：实时关注/粉丝数 + 当前用户是否关注/互关 */
    UserVO getUserById(Integer viewerId, Integer targetId);

    /** 热门用户：status=0，按获赞+帖子数排序，limit 10 */
    List<UserVO> getHotUsers();

    /** 单字段更新（白名单：nickname/avatar/introduction/phone/email） */
    void updateProfile(Integer userId, String field, String value);

    /** 批量更新个人资料（手机号/邮箱唯一校验） */
    void saveInfo(Integer userId, String nickname, String avatar, String introduction, String phone, String email);

    /** 搜索用户（nickname/username/sn like，status=0，limit 10） */
    List<UserBriefVO> searchUsers(String keyword);

    // ===================== 管理端用户管理（对齐原 foxbook adminapi/controller/User.php） =====================

    /** 管理端用户列表：搜索(nickname/username/email/phone/status/created_at) + 分页，status 以 1正常/0禁用 对外展示 */
    IPage<Users> getAdminUserList(Map<String, Object> queryForm, int page, int pageSize);

    /** 管理端保存前台用户（新增/编辑，密码 BCrypt，用户名与邮箱唯一，用户名密码须同时提供） */
    void saveFrontUser(Map<String, Object> params);

    /** 管理端删除前台用户 */
    void deleteFrontUser(Integer id);

    /** 封禁/解禁前台用户（displayStatus: 1正常 / 0禁用），并发送系统消息通知用户 */
    void updateFrontUserStatus(Integer id, Integer displayStatus);

    /** 设置用户的认证标签，返回 {user_id, tag_ids, auth_tags} */
    Map<String, Object> setAuthTags(Integer userId, List<Integer> tagIds);
}
