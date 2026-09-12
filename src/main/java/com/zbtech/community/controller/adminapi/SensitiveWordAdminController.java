package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.SensitiveWord;
import com.zbtech.community.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 敏感词 - 管理端（对齐原 foxbook adminapi/controller/SensitiveWord）
 */
@RestController
@RequestMapping({"/adminapi/sensitiveWord", "/adminapi/SensitiveWord"})
@Tag(name = "敏感词管理")
public class SensitiveWordAdminController extends BaseController {

    @Autowired
    private SensitiveWordService service;

    /** GET /adminapi/sensitiveWord/getListByPage 分页列表 */
    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(name = "queryForm[word]", required = false) String word,
                                    @RequestParam(name = "queryForm[status]", required = false) Integer status) {
        getAdminId();
        return pageResult(service.getListByPage(page, pageSize, word, status));
    }

    /** GET /adminapi/sensitiveWord/getList 全部敏感词 */
    @GetMapping("/getList")
    public Result<?> getList() {
        getAdminId();
        return Result.success(service.list());
    }

    /** GET /adminapi/sensitiveWord/getDetail 敏感词详情 */
    @GetMapping("/getDetail")
    public Result<SensitiveWord> getDetail(@RequestParam Integer id) {
        getAdminId();
        return Result.success(service.getWordOrFail(id));
    }

    /** POST /adminapi/sensitiveWord/save 保存敏感词（新增或更新） */
    @PostMapping("/save")
    public Result<SensitiveWord> save(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        String word = strOf(params.get("word"));
        Integer status = intOf(params.get("status"));
        if (status == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "状态不能为空");
        }
        return Result.success("保存成功", service.saveWord(id, word, status));
    }

    /** POST /adminapi/sensitiveWord/deleteWord 删除敏感词 */
    @PostMapping("/deleteWord")
    public Result<?> deleteWord(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        service.getWordOrFail(id);
        service.removeById(id);
        return Result.success("删除成功", null);
    }

    /** POST /adminapi/sensitiveWord/toggleStatus 切换状态 */
    @PostMapping("/toggleStatus")
    public Result<SensitiveWord> toggleStatus(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        Integer status = intOf(params.get("status"));
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "状态值无效（0/1）");
        }
        SensitiveWord record = service.getWordOrFail(id);
        record.setStatus(status);
        record.setUpdated_at(java.time.LocalDateTime.now());
        service.updateById(record);
        return Result.success("状态更新成功", service.getById(id));
    }

    /** POST /adminapi/sensitiveWord/batchDelete 批量删除 */
    @PostMapping("/batchDelete")
    public Result<?> batchDelete(@RequestBody Map<String, Object> params) {
        getAdminId();
        List<Integer> ids = idsOf(params.get("ids"));
        if (ids.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "敏感词ID列表不能为空");
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
