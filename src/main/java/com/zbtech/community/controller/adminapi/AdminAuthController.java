package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.service.AdminsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 管理端鉴权与系统管理（对齐原 foxbook adminapi/controller/Auth.php）
 * 路径：/adminapi/auth
 * - 登录/登出免鉴权（由 AdminAuthInterceptor 放行）
 * - 其余接口通过 getAdminId() 鉴权，依赖 AdminAuthInterceptor 注入 admin_id
 */
@RestController
@RequestMapping("/adminapi/auth")
@Tag(name = "管理端鉴权")
public class AdminAuthController extends BaseController {

    @Resource
    private AdminsService adminsService;

    /** 管理员登录：校验用户名密码(BCrypt)，成功签发 admin token 存 Redis */
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, Object> params) {
        String username = getStr(params, "username");
        String password = getStr(params, "password");
        Boolean remember = getBool(params, "remember");
        if (username == null || password == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名和密码必填");
        }
        String ip = getRequest() != null ? getRequest().getRemoteAddr() : "";
        Map<String, Object> result = adminsService.adminLogin(username, password, remember, ip);
        return Result.success("登录成功", result);
    }

    /** 管理员登出：删除 Redis 中的 admin token */
    @PostMapping("/logout")
    public Result<?> logout() {
        String token = getRequest() != null ? getRequest().getHeader("Authorization") : null;
        if (token == null || token.isBlank()) {
            token = getRequest() != null ? getRequest().getHeader("authorization") : null;
        }
        adminsService.adminLogout(token);
        return Result.success();
    }

    /** 当前管理员信息 */
    @GetMapping("/getInfo")
    public Result<?> getInfo() {
        Integer adminId = getAdminId();
        return Result.success(adminsService.getAdminInfo(adminId));
    }

    /** 当前管理员菜单 */
    @GetMapping("/getMenus")
    public Result<?> getMenus() {
        Integer adminId = getAdminId();
        return Result.success(adminsService.getAdminMenus(adminId));
    }

    /** 全部菜单树（支持过滤） */
    @GetMapping("/getAllMenus")
    public Result<?> getAllMenus(@RequestParam(required = false) Map<String, Object> queryForm) {
        return Result.success(adminsService.getAllMenus(queryForm));
    }

    /** 父级菜单树 */
    @GetMapping("/getParentMenus")
    public Result<?> getParentMenus() {
        return Result.success(adminsService.getParentMenus());
    }

    /** 保存菜单（新增/编辑） */
    @PostMapping("/saveMenus")
    public Result<?> saveMenus(@RequestBody Map<String, Object> params) {
        adminsService.saveMenu(params);
        return Result.success("菜单保存成功");
    }

    /** 删除菜单 */
    @PostMapping("/deleteMenus")
    public Result<?> deleteMenus(@RequestParam Integer id) {
        adminsService.deleteMenu(id);
        return Result.success("菜单删除成功");
    }

    /** 角色列表（分页） */
    @GetMapping("/getRoles")
    public Result<?> getRoles(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) Map<String, Object> queryForm) {
        return pageResult(adminsService.getRolesPage(queryForm, page, pageSize));
    }

    /** 保存角色（新增/编辑） */
    @PostMapping("/saveRole")
    public Result<?> saveRole(@RequestBody Map<String, Object> params) {
        adminsService.saveRole(params);
        return Result.success("角色保存成功");
    }

    /** 删除角色 */
    @PostMapping("/deleteRole")
    public Result<?> deleteRole(@RequestParam Integer id) {
        adminsService.deleteRole(id);
        return Result.success("角色删除成功");
    }

    /** 角色已分配菜单ID */
    @GetMapping("/getRoleMenus")
    public Result<?> getRoleMenus(@RequestParam Integer role_id) {
        return Result.success(adminsService.getRoleMenus(role_id));
    }

    /** 保存角色权限（覆盖式） */
    @PostMapping("/saveRolePermission")
    public Result<?> saveRolePermission(@RequestBody Map<String, Object> params) {
        Integer roleId = intOf(params.get("id"));
        if (roleId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色ID必填");
        }
        List<Integer> menuIds = toIntList(params.get("menu_ids"));
        adminsService.saveRolePermission(roleId, menuIds);
        return Result.success("权限设置成功");
    }

    /** 管理员用户列表（分页） */
    @GetMapping("/getUsers")
    public Result<?> getUsers(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) Map<String, Object> queryForm) {
        return pageResult(adminsService.getAdminUsersPage(queryForm, page, pageSize));
    }

    /** 保存管理员用户（新增/编辑） */
    @PostMapping("/saveUser")
    public Result<?> saveUser(@RequestBody Map<String, Object> params) {
        adminsService.saveAdminUser(params);
        return Result.success("用户保存成功");
    }

    /** 删除管理员用户 */
    @PostMapping("/deleteUser")
    public Result<?> deleteUser(@RequestParam Integer id) {
        adminsService.deleteAdminUser(id, getAdminId());
        return Result.success("用户删除成功");
    }

    /** 更新当前管理员个人资料 */
    @PostMapping("/updateProfile")
    public Result<?> updateProfile(@RequestBody Map<String, Object> params) {
        adminsService.updateAdminProfile(getAdminId(), params);
        return Result.success("个人信息更新成功");
    }

    /** 修改当前管理员密码 */
    @PostMapping("/changePassword")
    public Result<?> changePassword(@RequestBody Map<String, Object> params) {
        String currentPassword = getStr(params, "current_password");
        String newPassword = getStr(params, "new_password");
        String confirmPassword = getStr(params, "confirm_password");
        adminsService.changeAdminPassword(getAdminId(), currentPassword, newPassword, confirmPassword);
        return Result.success("密码修改成功");
    }

    /** 上传管理员头像 */
    @PostMapping("/uploadAvatar")
    public Result<?> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        String url = adminsService.uploadAvatar(getAdminId(), file);
        return Result.success(url);
    }

    // ===================== 私有：参数提取 =====================

    private String getStr(Map<String, Object> m, String k) {
        if (m == null) return null;
        Object v = m.get(k);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private Boolean getBool(Map<String, Object> m, String k) {
        if (m == null) return null;
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Boolean) return (Boolean) v;
        String s = v.toString().trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }

    private Integer intOf(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Integer> toIntList(Object o) {
        if (o == null) return java.util.Collections.emptyList();
        if (o instanceof List) {
            List<?> raw = (List<?>) o;
            List<Integer> res = new java.util.ArrayList<>(raw.size());
            for (Object x : raw) {
                if (x == null) continue;
                if (x instanceof Number) res.add(((Number) x).intValue());
                else {
                    try {
                        res.add(Integer.parseInt(x.toString().trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return res;
        }
        return java.util.Collections.emptyList();
    }
}
