package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.Link;
import com.zbtech.community.service.LinkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 广告/Banner - 管理端（对齐原 foxbook adminapi/controller/Link）
 */
@RestController
@RequestMapping({"/adminapi/link", "/adminapi/Link"})
@Tag(name = "广告/Banner管理")
public class LinkAdminController extends BaseController {

    @Autowired
    private LinkService service;

    /** GET /adminapi/link/getListByPage 分页列表 */
    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(name = "queryForm[title]", required = false) String title,
                                    @RequestParam(name = "queryForm[url]", required = false) String url,
                                    @RequestParam(name = "queryForm[status]", required = false) Integer status,
                                    @RequestParam(name = "queryForm[type]", required = false) Integer type,
                                    @RequestParam(name = "queryForm[created_at][0]", required = false) String createdStart,
                                    @RequestParam(name = "queryForm[created_at][1]", required = false) String createdEnd) {
        getAdminId();
        return pageResult(service.getListByPage(page, pageSize, title, url, status, type, createdStart, createdEnd));
    }

    /** GET /adminapi/link/getList 全部广告 */
    @GetMapping("/getList")
    public Result<?> getList() {
        getAdminId();
        return Result.success(service.list());
    }

    /** GET /adminapi/link/getBannerDetail 广告详情 */
    @GetMapping("/getBannerDetail")
    public Result<Link> getBannerDetail(@RequestParam Integer id) {
        getAdminId();
        return Result.success("获取广告详情成功", service.getBannerOrFail(id));
    }

    /** GET /adminapi/link/getDetail 广告详情（通用接口名） */
    @GetMapping("/getDetail")
    public Result<Link> getDetail(@RequestParam Integer id) {
        getAdminId();
        return Result.success(service.getBannerOrFail(id));
    }

    /** POST /adminapi/link/save 保存广告（新增或更新） */
    @PostMapping("/save")
    public Result<Link> save(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        String title = strOf(params.get("title"));
        String url = strOf(params.get("url"));
        String coverImg = strOf(params.get("cover_img"));
        Integer type = intOf(params.get("type"));
        String appId = strOf(params.get("app_id"));
        return Result.success("保存成功", service.saveBanner(id, title, url, coverImg, type, appId));
    }

    /** POST /adminapi/link/deleteBanner 删除广告 */
    @PostMapping("/deleteBanner")
    public Result<?> deleteBanner(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        service.getBannerOrFail(id);
        service.removeById(id);
        return Result.success("删除广告成功", null);
    }

    /** POST /adminapi/link/batchDeleteBanners 批量删除 */
    @PostMapping("/batchDeleteBanners")
    public Result<?> batchDeleteBanners(@RequestBody Map<String, Object> params) {
        getAdminId();
        List<Integer> ids = idsOf(params.get("ids"));
        if (ids.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "广告ID列表不能为空");
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
