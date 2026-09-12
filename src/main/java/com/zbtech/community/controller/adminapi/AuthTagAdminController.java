package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.AuthTag;
import com.zbtech.community.service.AuthTagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证标签 - 管理端（对齐原 foxbook adminapi/controller/AuthTag）
 */
@RestController
@RequestMapping({"/adminapi/authTag", "/adminapi/AuthTag"})
@Tag(name = "认证标签管理")
public class AuthTagAdminController extends BaseController {

    @Autowired
    private AuthTagService service;

    /** GET /adminapi/authTag/getListByPage 分页列表 */
    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(name = "queryForm[name]", required = false) String name,
                                    @RequestParam(name = "queryForm[status]", required = false) Integer status) {
        getAdminId();
        return pageResult(service.getListByPage(page, pageSize, name, status));
    }

    /** GET /adminapi/authTag/getList 全部标签 */
    @GetMapping("/getList")
    public Result<?> getList() {
        getAdminId();
        return Result.success(service.list());
    }

    /** GET /adminapi/authTag/getDetail 标签详情 */
    @GetMapping("/getDetail")
    public Result<AuthTag> getDetail(@RequestParam Integer id) {
        getAdminId();
        return Result.success(service.getTagOrFail(id));
    }

    /** POST /adminapi/authTag/save 保存标签（新增或更新） */
    @PostMapping("/save")
    public Result<AuthTag> save(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        String name = strOf(params.get("name"));
        String color = strOf(params.get("color"));
        Integer sort = intOf(params.get("sort"));
        Integer status = intOf(params.get("status"));
        if (status == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "状态不能为空");
        }
        return Result.success("保存成功", service.saveTag(id, name, color, sort, status));
    }

    /** POST /adminapi/authTag/deleteTag 删除标签 */
    @PostMapping("/deleteTag")
    public Result<?> deleteTag(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        service.getTagOrFail(id);
        service.removeById(id);
        return Result.success("删除成功", null);
    }

    /** POST /adminapi/authTag/toggleStatus 切换状态 */
    @PostMapping("/toggleStatus")
    public Result<AuthTag> toggleStatus(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        Integer status = intOf(params.get("status"));
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "状态值无效（0/1）");
        }
        AuthTag tag = service.getTagOrFail(id);
        tag.setStatus(status);
        tag.setUpdated_at(java.time.LocalDateTime.now());
        service.updateById(tag);
        return Result.success("状态更新成功", service.getById(id));
    }

    /** POST /adminapi/authTag/batchDelete 批量删除 */
    @PostMapping("/batchDelete")
    public Result<?> batchDelete(@RequestBody Map<String, Object> params) {
        getAdminId();
        List<Integer> ids = idsOf(params.get("ids"));
        if (ids.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "标签ID列表不能为空");
        }
        service.removeByIds(ids);
        return Result.success("批量删除成功", Map.of("deleted_count", ids.size()));
    }

    // ===================== 参数解析 =====================

    private Integer intOf(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try { return Integer.valueOf(s); } catch (NumberFormatException e) { return null; }
    }

    private String strOf(Object o) {
        return o == null ? null : o.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Integer> idsOf(Object o) {
        if (!(o instanceof List<?> raw)) return List.of();
        return raw.stream().map(this::intOf).filter(java.util.Objects::nonNull).distinct().toList();
    }
}
