package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.Admins;
import com.zbtech.community.entity.AdminsRoles;

import java.util.List;
import java.util.Map;

/**
 * 管理员业务（对齐原 foxbook adminapi/controller/Auth.php）
 * 涵盖：管理员登录登出、当前信息、菜单/权限、角色、管理员用户 CRUD、改密码、上传头像。
 * 由于原 PHP 把管理员用户、角色、权限、菜单的维护都放在 Auth 控制器下，这里统一收敛到 AdminsService。
 */
public interface AdminsService extends IService<Admins> {

    /** 管理员登录：校验用户名密码（BCrypt），成功签发 admin token 存 Redis，返回 {token, info} */
    Map<String, Object> adminLogin(String username, String password, Boolean remember, String ip);

    /** 管理员登出：删除 Redis 中的 admin token */
    void adminLogout(String token);

    /** 当前管理员信息（脱敏，密码置空） */
    Admins getAdminInfo(Integer adminId);

    /** 当前管理员可访问的菜单树（创建人角色返回全量 type=1 菜单） */
    List<Map<String, Object>> getAdminMenus(Integer adminId);

    /** 全部菜单树（支持按 queryForm 过滤 title/name/path/type/status） */
    List<Map<String, Object>> getAllMenus(Map<String, Object> queryForm);

    /** 父级菜单树（parent_id=0 起的全量树，用于菜单选择器） */
    List<Map<String, Object>> getParentMenus();

    /** 保存菜单（新增/编辑，菜单标识 name 唯一校验） */
    void saveMenu(Map<String, Object> params);

    /** 删除菜单（校验无子菜单、清理角色关联） */
    void deleteMenu(Integer id);

    /** 角色分页列表（支持 name 模糊、status 精确） */
    IPage<AdminsRoles> getRolesPage(Map<String, Object> queryForm, int page, int pageSize);

    /** 保存角色（新增/编辑，角色名唯一校验） */
    void saveRole(Map<String, Object> params);

    /** 删除角色（校验未被管理员使用，清理角色权限关联） */
    void deleteRole(Integer id);

    /** 角色已分配的菜单ID列表 */
    List<Integer> getRoleMenus(Integer roleId);

    /** 保存角色权限（覆盖式：删除原有权限后重新写入，创建人角色禁止修改） */
    void saveRolePermission(Integer roleId, List<Integer> menuIds);

    /** 管理员用户分页列表（支持 username/real_name/role_id/status 过滤） */
    IPage<Admins> getAdminUsersPage(Map<String, Object> queryForm, int page, int pageSize);

    /** 保存管理员用户（新增/编辑，密码 BCrypt，用户名/邮箱唯一，角色必须存在） */
    void saveAdminUser(Map<String, Object> params);

    /** 删除管理员用户（不能删自己、不能删创建人角色） */
    void deleteAdminUser(Integer id, Integer currentAdminId);

    /** 更新当前管理员个人资料（real_name/email/bio） */
    void updateAdminProfile(Integer adminId, Map<String, Object> params);

    /** 修改当前管理员密码（校验原密码、两次一致、与原密码不同） */
    void changeAdminPassword(Integer adminId, String currentPassword, String newPassword, String confirmPassword);

    /** 上传管理员头像，返回访问 URL */
    String uploadAvatar(Integer adminId, org.springframework.web.multipart.MultipartFile file);
}
