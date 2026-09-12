package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.WebTopNav;
import com.zbtech.community.service.WebTopNavService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * PC Web 顶部导航 - 管理端（对齐原 foxbook adminapi/controller/WebTopNav）
 */
@RestController
@RequestMapping({"/adminapi/webTopNav", "/adminapi/WebTopNav"})
@Tag(name = "顶部导航管理")
public class WebTopNavAdminController extends BaseController {

    @Autowired
    private WebTopNavService service;

    /** GET /adminapi/webTopNav/getListByPage 分页列表 */
    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(name = "queryForm[title]", required = false) String title,
                                    @RequestParam(name = "queryForm[url]", required = false) String url,
                                    @RequestParam(name = "queryForm[nav_key]", required = false) String navKey,
                                    @RequestParam(name = "queryForm[status]", required = false) Integer status,
                                    @RequestParam(name = "queryForm[created_at][0]", required = false) String createdStart,
                                    @RequestParam(name = "queryForm[created_at][1]", required = false) String createdEnd) {
        getAdminId();
        return pageResult(service.getListByPage(page, pageSize, title, url, navKey, status, createdStart, createdEnd));
    }

    /** GET /adminapi/webTopNav/getList 全部导航 */
    @GetMapping("/getList")
    public Result<?> getList() {
        getAdminId();
        return Result.success(service.list());
    }

    /** GET /adminapi/webTopNav/getDetail 导航详情 */
    @GetMapping("/getDetail")
    public Result<WebTopNav> getDetail(@RequestParam Integer id) {
        getAdminId();
        return Result.success(service.getNavOrFail(id));
    }

    /** POST /adminapi/webTopNav/createNav 创建导航 */
    @PostMapping("/createNav")
    public Result<WebTopNav> createNav(@RequestBody Map<String, Object> params) {
        getAdminId();
        WebTopNav nav = buildNavFromParams(params, false);
        nav.setCreated_at(LocalDateTime.now());
        nav.setUpdated_at(LocalDateTime.now());
        service.save(nav);
        return Result.success("创建成功", service.getById(nav.getId()));
    }

    /** POST /adminapi/webTopNav/updateNav 更新导航 */
    @PostMapping("/updateNav")
    public Result<WebTopNav> updateNav(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        WebTopNav nav = service.getNavOrFail(id);
        WebTopNav updated = buildNavFromParams(params, false);
        nav.setTitle(updated.getTitle());
        nav.setUrl(updated.getUrl());
        nav.setNav_key(updated.getNav_key());
        nav.setTarget(updated.getTarget());
        nav.setSort(updated.getSort());
        nav.setStatus(updated.getStatus());
        nav.setUpdated_at(LocalDateTime.now());
        service.updateById(nav);
        return Result.success("更新成功", service.getById(nav.getId()));
    }

    /** POST /adminapi/webTopNav/deleteNav 删除导航 */
    @PostMapping("/deleteNav")
    public Result<?> deleteNav(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        service.getNavOrFail(id);
        service.removeById(id);
        return Result.success("删除成功", null);
    }

    /** POST /adminapi/webTopNav/batchDeleteNavs 批量删除 */
    @PostMapping("/batchDeleteNavs")
    public Result<?> batchDeleteNavs(@RequestBody Map<String, Object> params) {
        getAdminId();
        List<Integer> ids = idsOf(params.get("ids"));
        if (ids.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "菜单ID列表不能为空");
        }
        service.removeByIds(ids);
        return Result.success("批量删除成功", Map.of("deleted_count", ids.size()));
    }

    // ===================== 参数解析 =====================

    private WebTopNav buildNavFromParams(Map<String, Object> params, boolean requireId) {
        String title = strOf(params.get("title"));
        if (title == null || title.trim().isEmpty() || title.trim().length() > 12) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "菜单名称长度必须在 1-12 个字符之间");
        }
        String url = strOf(params.get("url"));
        if (url == null || url.trim().isEmpty() || url.trim().length() > 255) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "跳转地址长度必须在 1-255 个字符之间");
        }
        String navKey = strOf(params.get("nav_key"));
        if (navKey != null && navKey.length() > 50) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "导航标识不能超过50个字符");
        }
        Integer target = intOf(params.get("target"));
        if (target == null || (target != 1 && target != 2)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "打开方式无效（1当前窗口 2新窗口）");
        }
        Integer sort = intOf(params.get("sort"));
        if (sort == null) sort = 0;
        if (sort < 0 || sort > 9999) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "排序值必须在 0-9999 之间");
        }
        Integer status = intOf(params.get("status"));
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "状态值无效（0隐藏 1显示）");
        }

        WebTopNav nav = new WebTopNav();
        nav.setTitle(title.trim());
        nav.setUrl(url.trim());
        nav.setNav_key(navKey != null ? navKey.trim() : "");
        nav.setTarget(target);
        nav.setSort(sort);
        nav.setStatus(status);
        return nav;
    }

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
