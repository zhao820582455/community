package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.Agreement;
import com.zbtech.community.common.Result;
import com.zbtech.community.service.AgreementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 协议表 - 管理端（对齐原 foxbook adminapi/controller/Agreement）
 * 兼容前端 camelCase 与项目 snake_case 两种路径前缀
 */
@RestController
@RequestMapping({"/adminapi/agreement", "/adminapi/Agreement"})
@Tag(name = "协议管理")
public class AgreementAdminController extends BaseController {

    @Autowired
    protected AgreementService service;

    /**
     * GET /adminapi/agreement/getListByPage
     * 分页列表，支持 queryForm[title] 模糊、queryForm[created_at][0..1] 日期区间
     */
    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(name = "queryForm[title]", required = false) String title,
                                   @RequestParam(name = "queryForm[created_at][0]", required = false) String createdStart,
                                   @RequestParam(name = "queryForm[created_at][1]", required = false) String createdEnd) {
        getAdminId();
        return pageResult(service.getListByPage(page, pageSize, title, createdStart, createdEnd));
    }

    /** GET /adminapi/agreement/getList 全部协议 */
    @GetMapping("/getList")
    public Result<?> getList() {
        getAdminId();
        return Result.success(service.list());
    }

    /** GET /adminapi/agreement/getAgreement 协议详情（前端使用的接口名） */
    @GetMapping("/getAgreement")
    public Result<Agreement> getAgreement(@RequestParam Integer id) {
        getAdminId();
        return Result.success(service.getAgreementOrFail(id));
    }

    /** GET /adminapi/agreement/getDetail 协议详情（通用接口名） */
    @GetMapping("/getDetail")
    public Result<Agreement> getDetail(@RequestParam Integer id) {
        getAdminId();
        return Result.success(service.getAgreementOrFail(id));
    }

    /**
     * POST /adminapi/agreement/saveAgreement
     * 更新既有协议的标题与内容
     */
    @PostMapping("/saveAgreement")
    public Result<Agreement> saveAgreement(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        String title = strOf(params.get("title"));
        String content = strOf(params.get("content"));
        return Result.success("协议更新成功", service.updateAgreement(id, title, content));
    }

    /**
     * POST /adminapi/agreement/save
     * 通用保存：带 id 走更新，不带 id 走新增
     */
    @PostMapping("/save")
    public Result<Agreement> save(@RequestBody Agreement entity) {
        getAdminId();
        if (entity.getId() != null) {
            return Result.success("保存成功",
                    service.updateAgreement(entity.getId(), entity.getTitle(), entity.getContent()));
        }
        String title = entity.getTitle() == null ? "" : entity.getTitle().trim();
        if (title.isEmpty() || title.length() > 50) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "标题长度必须在 1-50 个字符之间");
        }
        if (entity.getContent() == null || entity.getContent().trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "内容不能为空");
        }
        entity.setTitle(title);
        service.save(entity);
        return Result.success("保存成功", service.getById(entity.getId()));
    }

    /** POST /adminapi/agreement/delete 删除协议 */
    @PostMapping("/delete")
    public Result<?> delete(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        service.getAgreementOrFail(id);
        service.removeById(id);
        return Result.success("删除成功", null);
    }

    /** POST /adminapi/agreement/batchDelete 批量删除 */
    @PostMapping("/batchDelete")
    public Result<?> batchDelete(@RequestBody Map<String, Object> params) {
        getAdminId();
        List<Integer> ids = idsOf(params.get("ids"));
        if (ids.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "协议ID列表不能为空");
        }
        service.removeByIds(ids);
        return Result.success("批量删除成功", Map.of("deleted_count", ids.size()));
    }

    // ===================== 参数工具 =====================

    private Integer intOf(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        String s = o.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String strOf(Object o) {
        return o == null ? null : o.toString();
    }

    private List<Integer> idsOf(Object o) {
        if (!(o instanceof List<?> raw)) {
            return List.of();
        }
        return raw.stream().map(this::intOf).filter(java.util.Objects::nonNull).distinct().toList();
    }
}
