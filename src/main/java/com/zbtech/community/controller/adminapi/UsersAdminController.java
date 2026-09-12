package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.Users;
import com.zbtech.community.service.UsersService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 前台用户管理（对齐原 foxbook adminapi/controller/User.php）
 * 路径：/adminapi/user
 */
@RestController
@RequestMapping("/adminapi/user")
@Tag(name = "前台用户管理")
public class UsersAdminController extends BaseController {

    @Resource
    private UsersService usersService;

    /** 用户列表：支持 nickname/username/email/phone/status/created_at 搜索 + 分页 */
    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(required = false) String nickname,
                                   @RequestParam(required = false) String username,
                                   @RequestParam(required = false) String email,
                                   @RequestParam(required = false) String phone,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) String createdAtStart,
                                   @RequestParam(required = false) String createdAtEnd) {
        Map<String, Object> queryForm = new HashMap<>(8);
        if (nickname != null && !nickname.isBlank()) queryForm.put("nickname", nickname);
        if (username != null && !username.isBlank()) queryForm.put("username", username);
        if (email != null && !email.isBlank()) queryForm.put("email", email);
        if (phone != null && !phone.isBlank()) queryForm.put("phone", phone);
        if (status != null) queryForm.put("status", status);
        if (createdAtStart != null && !createdAtStart.isBlank()) queryForm.put("created_at_start", createdAtStart);
        if (createdAtEnd != null && !createdAtEnd.isBlank()) queryForm.put("created_at_end", createdAtEnd);
        return pageResult(usersService.getAdminUserList(queryForm, page, pageSize));
    }

    /** 用户详情（status 对外以 1正常 / 0禁用 展示） */
    @GetMapping("/getDetail")
    public Result<?> getDetail(@RequestParam Integer id) {
        Users user = usersService.getById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (user.getStatus() != null) {
            user.setStatus(user.getStatus() == 0 ? 1 : 0);
        }
        return Result.success(user);
    }

    /** 保存前台用户：新增/编辑，密码 BCrypt，用户名与邮箱唯一 */
    @PostMapping("/saveUser")
    public Result<?> saveUser(@RequestBody Map<String, Object> params) {
        usersService.saveFrontUser(params);
        return Result.success("用户保存成功");
    }

    /** 删除前台用户 */
    @PostMapping("/deleteUser")
    public Result<?> deleteUser(@RequestParam Integer id) {
        usersService.deleteFrontUser(id);
        return Result.success("用户删除成功");
    }

    /** 封禁/解禁：status 1正常 / 0禁用，并发送系统消息 */
    @PostMapping("/updateStatus")
    public Result<?> updateStatus(@RequestBody Map<String, Object> params) {
        Integer id = intOf(params.get("id"));
        Integer status = intOf(params.get("status"));
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户ID必填");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "状态取值非法");
        }
        usersService.updateFrontUserStatus(id, status);
        return Result.success(status == 1 ? "用户启用成功" : "用户禁用成功");
    }

    /** 设置用户认证标签 */
    @PostMapping("/setAuthTags")
    public Result<?> setAuthTags(@RequestBody Map<String, Object> params) {
        Integer userId = intOf(params.get("id"));
        if (userId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户ID必填");
        }
        List<Integer> tagIds = toIntList(params.get("tag_ids"));
        return Result.success(usersService.setAuthTags(userId, tagIds));
    }

    /** 热门用户 */
    @GetMapping("/getHotUsers")
    public Result<?> getHotUsers() {
        return Result.success(usersService.getHotUsers());
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
