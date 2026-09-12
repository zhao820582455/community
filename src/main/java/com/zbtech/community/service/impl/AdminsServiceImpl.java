package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.Admins;
import com.zbtech.community.entity.AdminsPermission;
import com.zbtech.community.entity.AdminsRolePermission;
import com.zbtech.community.entity.AdminsRoles;
import com.zbtech.community.mapper.AdminsMapper;
import com.zbtech.community.service.AdminsPermissionService;
import com.zbtech.community.service.AdminsRolePermissionService;
import com.zbtech.community.service.AdminsRolesService;
import com.zbtech.community.service.AdminsService;
import com.zbtech.community.service.TokenService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员业务实现（对齐原 foxbook adminapi/controller/Auth.php）
 * 密码使用 BCrypt，管理端 token 通过 TokenService 签发并存入 Redis（前缀 admin:）。
 */
@Service
public class AdminsServiceImpl extends ServiceImpl<AdminsMapper, Admins> implements AdminsService {

    /** 管理员密码复杂度：6-20 位，含字母、数字、特殊字符 */
    private static final String ADMIN_PWD_REGEX =
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$";

    private final AdminsRolesService rolesService;
    private final AdminsPermissionService permissionService;
    private final AdminsRolePermissionService rolePermissionService;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminsServiceImpl(AdminsRolesService rolesService,
                             AdminsPermissionService permissionService,
                             AdminsRolePermissionService rolePermissionService,
                             TokenService tokenService) {
        this.rolesService = rolesService;
        this.permissionService = permissionService;
        this.rolePermissionService = rolePermissionService;
        this.tokenService = tokenService;
    }

    // ===================== 登录 / 登出 =====================

    @Override
    public Map<String, Object> adminLogin(String username, String password, Boolean remember, String ip) {
        Admins admin = getOne(new LambdaQueryWrapper<Admins>().eq(Admins::getUsername, username));
        if (admin == null || !passwordEncoder.matches(password, admin.getPassword())) {
            throw new BizException(ErrorCode.FAIL, "用户名或密码错误");
        }
        // 刷新登录信息
        admin.setLast_login_time(LocalDateTime.now());
        admin.setLast_login_ip(ip);
        updateById(admin);

        // 签发管理员 token（remember=true 时 7 天，否则 1 小时），存 Redis: admin:{token} -> adminId
        String token = tokenService.issueAdminToken(admin.getId(), remember != null && remember);

        Map<String, Object> result = new HashMap<>(4);
        result.put("token", token);
        admin.setPassword(null); // 脱敏
        result.put("info", admin);
        return result;
    }

    @Override
    public void adminLogout(String token) {
        tokenService.revokeAdminToken(token);
    }

    @Override
    public Admins getAdminInfo(Integer adminId) {
        Admins admin = getById(adminId);
        if (admin == null) {
            throw new BizException(ErrorCode.FAIL, "管理员不存在");
        }
        admin.setPassword(null);
        return admin;
    }

    // ===================== 菜单 / 权限 =====================

    @Override
    public List<Map<String, Object>> getAdminMenus(Integer adminId) {
        Admins admin = getById(adminId);
        if (admin == null) {
            throw new BizException(ErrorCode.FAIL, "管理员不存在");
        }
        AdminsRoles role = rolesService.getById(admin.getRole_id());
        if (role == null) {
            throw new BizException(ErrorCode.FAIL, "角色不存在");
        }

        // 创建人角色拥有系统全量菜单（type=1）；普通角色取角色权限关联的菜单
        List<Integer> menuIds;
        if (role.getIs_founder() != null && role.getIs_founder() == 1) {
            menuIds = permissionService.list(new LambdaQueryWrapper<AdminsPermission>()
                            .eq(AdminsPermission::getType, 1))
                    .stream().map(AdminsPermission::getId).collect(Collectors.toList());
        } else {
            menuIds = rolePermissionService.list(new LambdaQueryWrapper<AdminsRolePermission>()
                            .eq(AdminsRolePermission::getRole_id, admin.getRole_id()))
                    .stream().map(AdminsRolePermission::getMenu_id).collect(Collectors.toList());
        }
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<AdminsPermission> menus = permissionService.list(new LambdaQueryWrapper<AdminsPermission>()
                .in(AdminsPermission::getId, menuIds)
                .eq(AdminsPermission::getStatus, 1)
                .eq(AdminsPermission::getType, 1));
        return buildMenuTree(menus);
    }

    @Override
    public List<Map<String, Object>> getAllMenus(Map<String, Object> queryForm) {
        List<AdminsPermission> all = permissionService.list(
                new LambdaQueryWrapper<AdminsPermission>().orderByAsc(AdminsPermission::getSort));
        if (queryForm != null && !queryForm.isEmpty()) {
            String title = getStrMap(queryForm, "title");
            String name = getStrMap(queryForm, "name");
            String path = getStrMap(queryForm, "path");
            Integer type = getIntMap(queryForm, "type");
            Integer status = getIntMap(queryForm, "status");
            final List<AdminsPermission> filtered = new ArrayList<>();
            for (AdminsPermission p : all) {
                if (title != null && (p.getTitle() == null || !p.getTitle().contains(title))) continue;
                if (name != null && (p.getName() == null || !p.getName().contains(name))) continue;
                if (path != null && (p.getPath() == null || !p.getPath().contains(path))) continue;
                if (type != null && !Objects.equals(p.getType(), type)) continue;
                if (status != null && !Objects.equals(p.getStatus(), status)) continue;
                filtered.add(p);
            }
            return buildMenuTree(filtered);
        }
        return buildMenuTree(all);
    }

    @Override
    public List<Map<String, Object>> getParentMenus() {
        List<AdminsPermission> all = permissionService.list(
                new LambdaQueryWrapper<AdminsPermission>().orderByAsc(AdminsPermission::getSort));
        return buildMenuTree(all);
    }

    @Override
    @Transactional
    public void saveMenu(Map<String, Object> params) {
        String title = getStr(params, "title");
        String name = getStr(params, "name");
        String path = getStr(params, "path");
        if (isBlank(title) || isBlank(name) || isBlank(path)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "菜单标题、标识、访问URL均为必填");
        }
        if (title.length() > 6) {
            throw new BizException(ErrorCode.PARAM_ERROR, "菜单标题长度不能超过6个字符");
        }
        Integer id = getInt(params, "id");
        Integer parentId = getInt(params, "parent_id");
        if (parentId == null) parentId = 0;
        Integer status = getInt(params, "status");
        if (status == null) status = 1;
        Integer type = getInt(params, "type");
        if (type == null) type = 1;
        String icon = getStr(params, "icon");
        String component = getStr(params, "component");
        Integer sort = getInt(params, "sort");
        if (sort == null) sort = 0;
        // meta 仅保存标题，与原 PHP 保持一致
        String meta = "{\"title\":\"" + title.replace("\"", "\\\"") + "\"}";

        if (id != null) {
            AdminsPermission menu = permissionService.getById(id);
            if (menu == null) {
                throw new BizException(ErrorCode.FAIL, "菜单不存在");
            }
            if (permissionService.count(new LambdaQueryWrapper<AdminsPermission>()
                    .eq(AdminsPermission::getName, name).ne(AdminsPermission::getId, id)) > 0) {
                throw new BizException(ErrorCode.FAIL, "菜单标识已存在");
            }
            menu.setTitle(title);
            menu.setName(name);
            menu.setPath(path);
            menu.setComponent(component);
            menu.setStatus(status);
            menu.setIcon(icon);
            menu.setParent_id(parentId);
            menu.setSort(sort);
            menu.setType(type);
            menu.setMeta(meta);
            menu.setUpdated_at(LocalDateTime.now());
            permissionService.updateById(menu);
        } else {
            if (permissionService.count(new LambdaQueryWrapper<AdminsPermission>()
                    .eq(AdminsPermission::getName, name)) > 0) {
                throw new BizException(ErrorCode.FAIL, "菜单标识已存在");
            }
            AdminsPermission menu = new AdminsPermission();
            menu.setTitle(title);
            menu.setName(name);
            menu.setPath(path);
            menu.setComponent(component);
            menu.setStatus(status);
            menu.setIcon(icon);
            menu.setParent_id(parentId);
            menu.setSort(sort);
            menu.setType(type);
            menu.setMeta(meta);
            menu.setCreated_at(LocalDateTime.now());
            menu.setUpdated_at(LocalDateTime.now());
            permissionService.save(menu);
        }
    }

    @Override
    @Transactional
    public void deleteMenu(Integer id) {
        AdminsPermission menu = permissionService.getById(id);
        if (menu == null) {
            throw new BizException(ErrorCode.FAIL, "菜单不存在");
        }
        if (permissionService.count(new LambdaQueryWrapper<AdminsPermission>()
                .eq(AdminsPermission::getParent_id, id)) > 0) {
            throw new BizException(ErrorCode.FAIL, "该菜单下存在子菜单，请先删除子菜单");
        }
        // 清理角色菜单关联
        if (rolePermissionService.count(new LambdaQueryWrapper<AdminsRolePermission>()
                .eq(AdminsRolePermission::getMenu_id, id)) > 0) {
            rolePermissionService.remove(new LambdaQueryWrapper<AdminsRolePermission>()
                    .eq(AdminsRolePermission::getMenu_id, id));
        }
        permissionService.removeById(id);
    }

    // ===================== 角色 =====================

    @Override
    public IPage<AdminsRoles> getRolesPage(Map<String, Object> queryForm, int page, int pageSize) {
        Page<AdminsRoles> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<AdminsRoles> w = new LambdaQueryWrapper<>();
        String name = getStrMap(queryForm, "name");
        if (!isBlank(name)) {
            w.like(AdminsRoles::getName, name);
        }
        Integer status = getIntMap(queryForm, "status");
        if (status != null) {
            w.eq(AdminsRoles::getStatus, status);
        }
        w.orderByDesc(AdminsRoles::getCreated_at);
        return rolesService.page(p, w);
    }

    @Override
    @Transactional
    public void saveRole(Map<String, Object> params) {
        String name = getStr(params, "name");
        if (isBlank(name) || name.length() > 50) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色名称必填且不超过50个字符");
        }
        Integer id = getInt(params, "id");
        String description = getStr(params, "description");
        Integer status = getInt(params, "status");
        if (status == null) status = 1;

        if (id != null) {
            AdminsRoles role = rolesService.getById(id);
            if (role == null) {
                throw new BizException(ErrorCode.FAIL, "角色不存在");
            }
            if (rolesService.count(new LambdaQueryWrapper<AdminsRoles>()
                    .eq(AdminsRoles::getName, name).ne(AdminsRoles::getId, id)) > 0) {
                throw new BizException(ErrorCode.FAIL, "角色名称已存在");
            }
            role.setName(name);
            role.setDescription(description == null ? "" : description);
            role.setStatus(status);
            role.setUpdated_at(LocalDateTime.now());
            rolesService.updateById(role);
        } else {
            if (rolesService.count(new LambdaQueryWrapper<AdminsRoles>()
                    .eq(AdminsRoles::getName, name)) > 0) {
                throw new BizException(ErrorCode.FAIL, "角色名称已存在");
            }
            AdminsRoles role = new AdminsRoles();
            role.setName(name);
            role.setDescription(description == null ? "" : description);
            role.setStatus(status);
            role.setCreated_at(LocalDateTime.now());
            role.setUpdated_at(LocalDateTime.now());
            rolesService.save(role);
        }
    }

    @Override
    @Transactional
    public void deleteRole(Integer id) {
        AdminsRoles role = rolesService.getById(id);
        if (role == null) {
            throw new BizException(ErrorCode.FAIL, "角色不存在");
        }
        if (count(new LambdaQueryWrapper<Admins>().eq(Admins::getRole_id, id)) > 0) {
            throw new BizException(ErrorCode.FAIL, "该角色正在被管理员使用，无法删除");
        }
        rolePermissionService.remove(new LambdaQueryWrapper<AdminsRolePermission>()
                .eq(AdminsRolePermission::getRole_id, id));
        rolesService.removeById(id);
    }

    @Override
    public List<Integer> getRoleMenus(Integer roleId) {
        return rolePermissionService.list(new LambdaQueryWrapper<AdminsRolePermission>()
                        .eq(AdminsRolePermission::getRole_id, roleId))
                .stream().map(AdminsRolePermission::getMenu_id).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveRolePermission(Integer roleId, List<Integer> menuIds) {
        AdminsRoles role = rolesService.getById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.FAIL, "角色不存在");
        }
        if (role.getIs_founder() != null && role.getIs_founder() == 1) {
            throw new BizException(ErrorCode.FAIL, "系统创建人角色权限不可修改");
        }
        // 覆盖式：先删后插
        rolePermissionService.remove(new LambdaQueryWrapper<AdminsRolePermission>()
                .eq(AdminsRolePermission::getRole_id, roleId));
        if (menuIds != null && !menuIds.isEmpty()) {
            List<AdminsRolePermission> list = new ArrayList<>(menuIds.size());
            LocalDateTime now = LocalDateTime.now();
            for (Integer mid : menuIds) {
                if (mid == null) continue;
                AdminsRolePermission rp = new AdminsRolePermission();
                rp.setRole_id(roleId);
                rp.setMenu_id(mid);
                rp.setCreated_at(now);
                rp.setUpdated_at(now);
                list.add(rp);
            }
            rolePermissionService.saveBatch(list);
        }
    }

    // ===================== 管理员用户 =====================

    @Override
    public IPage<Admins> getAdminUsersPage(Map<String, Object> queryForm, int page, int pageSize) {
        Page<Admins> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Admins> w = new LambdaQueryWrapper<>();
        String username = getStrMap(queryForm, "username");
        if (!isBlank(username)) {
            w.like(Admins::getUsername, username);
        }
        String realName = getStrMap(queryForm, "real_name");
        if (!isBlank(realName)) {
            w.like(Admins::getReal_name, realName);
        }
        Integer roleId = getIntMap(queryForm, "role_id");
        if (roleId != null) {
            w.eq(Admins::getRole_id, roleId);
        }
        Integer status = getIntMap(queryForm, "status");
        if (status != null) {
            w.eq(Admins::getStatus, status);
        }
        w.orderByDesc(Admins::getCreated_at);
        return page(p, w);
    }

    @Override
    @Transactional
    public void saveAdminUser(Map<String, Object> params) {
        String username = getStr(params, "username");
        String realName = getStr(params, "real_name");
        Integer roleId = getInt(params, "role_id");
        if (isBlank(username) || username.length() < 4 || username.length() > 32) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名必填且长度为4-32位");
        }
        if (isBlank(realName) || realName.length() > 50) {
            throw new BizException(ErrorCode.PARAM_ERROR, "姓名必填且不超过50个字符");
        }
        if (roleId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色ID必填");
        }
        if (rolesService.getById(roleId) == null) {
            throw new BizException(ErrorCode.FAIL, "角色不存在");
        }
        Integer status = getInt(params, "status");
        if (status == null) status = 1;
        String email = getStr(params, "email");
        String password = getStr(params, "password");
        Integer id = getInt(params, "id");

        if (id == null) {
            // 新增必须提供密码
            if (isBlank(password) || password.length() < 6 || password.length() > 20
                    || !password.matches(ADMIN_PWD_REGEX)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "密码需6-20位，且包含字母、数字、特殊字符");
            }
            if (count(new LambdaQueryWrapper<Admins>().eq(Admins::getUsername, username)) > 0) {
                throw new BizException(ErrorCode.FAIL, "用户名已存在");
            }
            if (!isBlank(email) && count(new LambdaQueryWrapper<Admins>()
                    .eq(Admins::getEmail, email)) > 0) {
                throw new BizException(ErrorCode.FAIL, "邮箱已存在");
            }
            Admins admin = new Admins();
            admin.setUsername(username);
            admin.setReal_name(realName);
            admin.setRole_id(roleId);
            admin.setEmail(isBlank(email) ? null : email);
            admin.setStatus(status);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setCreated_at(LocalDateTime.now());
            admin.setUpdated_at(LocalDateTime.now());
            save(admin);
        } else {
            Admins admin = getById(id);
            if (admin == null) {
                throw new BizException(ErrorCode.FAIL, "用户不存在");
            }
            if (count(new LambdaQueryWrapper<Admins>()
                    .eq(Admins::getUsername, username).ne(Admins::getId, id)) > 0) {
                throw new BizException(ErrorCode.FAIL, "用户名已存在");
            }
            if (!isBlank(email) && count(new LambdaQueryWrapper<Admins>()
                    .eq(Admins::getEmail, email).ne(Admins::getId, id)) > 0) {
                throw new BizException(ErrorCode.FAIL, "邮箱已存在");
            }
            admin.setUsername(username);
            admin.setReal_name(realName);
            admin.setRole_id(roleId);
            admin.setEmail(isBlank(email) ? null : email);
            admin.setStatus(status);
            admin.setUpdated_at(LocalDateTime.now());
            if (!isBlank(password)) {
                if (password.length() < 6 || password.length() > 20 || !password.matches(ADMIN_PWD_REGEX)) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "密码需6-20位，且包含字母、数字、特殊字符");
                }
                admin.setPassword(passwordEncoder.encode(password));
            }
            updateById(admin);
        }
    }

    @Override
    @Transactional
    public void deleteAdminUser(Integer id, Integer currentAdminId) {
        Admins admin = getById(id);
        if (admin == null) {
            throw new BizException(ErrorCode.FAIL, "用户不存在");
        }
        if (id.equals(currentAdminId)) {
            throw new BizException(ErrorCode.FAIL, "不能删除当前登录用户");
        }
        AdminsRoles role = rolesService.getById(admin.getRole_id());
        if (role != null && role.getIs_founder() != null && role.getIs_founder() == 1) {
            throw new BizException(ErrorCode.FAIL, "不能删除系统创建人");
        }
        removeById(id);
    }

    @Override
    @Transactional
    public void updateAdminProfile(Integer adminId, Map<String, Object> params) {
        Admins admin = getById(adminId);
        if (admin == null) {
            throw new BizException(ErrorCode.FAIL, "用户不存在");
        }
        String realName = getStr(params, "real_name");
        String email = getStr(params, "email");
        String bio = getStr(params, "bio");
        if (!isBlank(email)) {
            if (count(new LambdaQueryWrapper<Admins>()
                    .eq(Admins::getEmail, email).ne(Admins::getId, adminId)) > 0) {
                throw new BizException(ErrorCode.FAIL, "邮箱已被其他用户使用");
            }
        }
        if (realName != null) admin.setReal_name(realName);
        if (email != null) admin.setEmail(isBlank(email) ? null : email);
        if (bio != null) admin.setBio(bio);
        admin.setUpdated_at(LocalDateTime.now());
        updateById(admin);
    }

    @Override
    @Transactional
    public void changeAdminPassword(Integer adminId, String currentPassword,
                                    String newPassword, String confirmPassword) {
        if (isBlank(newPassword) || newPassword.length() < 6 || newPassword.length() > 20
                || !newPassword.matches(ADMIN_PWD_REGEX)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "新密码需6-20位，且包含字母、数字、特殊字符");
        }
        if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
            throw new BizException(ErrorCode.FAIL, "两次输入的密码不一致");
        }
        Admins admin = getById(adminId);
        if (admin == null) {
            throw new BizException(ErrorCode.FAIL, "用户不存在");
        }
        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            throw new BizException(ErrorCode.FAIL, "当前密码不正确");
        }
        if (passwordEncoder.matches(newPassword, admin.getPassword())) {
            throw new BizException(ErrorCode.FAIL, "新密码不能与当前密码相同");
        }
        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setUpdated_at(LocalDateTime.now());
        updateById(admin);
    }

    @Override
    @Transactional
    public String uploadAvatar(Integer adminId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择要上传的头像文件");
        }
        String contentType = file.getContentType();
        List<String> allowed = Arrays.asList("image/jpeg", "image/png", "image/gif");
        if (contentType == null || !allowed.contains(contentType)) {
            throw new BizException(ErrorCode.FAIL, "只支持 JPG、PNG、GIF 格式的图片");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BizException(ErrorCode.FAIL, "头像文件大小不能超过2MB");
        }
        String originalName = file.getOriginalFilename();
        String ext = (originalName == null || originalName.lastIndexOf('.') < 0)
                ? "png" : originalName.substring(originalName.lastIndexOf('.') + 1);
        String filename = "avatar_" + adminId + "_" + System.currentTimeMillis() + "." + ext;
        String baseDir = System.getProperty("user.dir") + "/uploads/avatars/";
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File dest = new File(baseDir + filename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BizException(ErrorCode.FAIL, "头像上传失败：" + e.getMessage());
        }

        Admins admin = getById(adminId);
        if (admin != null) {
            if (admin.getAvatar() != null && admin.getAvatar().startsWith("/uploads/")) {
                File old = new File(System.getProperty("user.dir") + admin.getAvatar());
                if (old.exists()) {
                    old.delete();
                }
            }
            admin.setAvatar("/uploads/avatars/" + filename);
            admin.setUpdated_at(LocalDateTime.now());
            updateById(admin);
        }
        return "/uploads/avatars/" + filename;
    }

    // ===================== 私有：菜单树构建 =====================

    private List<Map<String, Object>> buildMenuTree(List<AdminsPermission> list) {
        List<Map<String, Object>> nodes = new ArrayList<>(list.size());
        Map<Integer, Map<String, Object>> index = new HashMap<>(list.size());
        for (AdminsPermission p : list) {
            Map<String, Object> node = new HashMap<>(16);
            node.put("id", p.getId());
            node.put("parent_id", p.getParent_id());
            node.put("name", p.getName());
            node.put("title", p.getTitle());
            node.put("icon", p.getIcon());
            node.put("path", p.getPath());
            node.put("component", p.getComponent());
            node.put("meta", p.getMeta());
            node.put("sort", p.getSort());
            node.put("status", p.getStatus());
            node.put("type", p.getType());
            node.put("children", new ArrayList<Map<String, Object>>());
            index.put(p.getId(), node);
            nodes.add(node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            Object pid = node.get("parent_id");
            int parentId = (pid instanceof Number) ? ((Number) pid).intValue() : 0;
            if (parentId == 0) {
                roots.add(node);
            } else {
                Map<String, Object> parent = index.get(parentId);
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    // ===================== 私有：参数提取 =====================

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

    private String getStrMap(Map<String, Object> m, String k) {
        return getStr(m, k);
    }

    private Integer getIntMap(Map<String, Object> m, String k) {
        return getInt(m, k);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
