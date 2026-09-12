package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.Category;
import com.zbtech.community.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 圈子类目 - 管理端（对齐原 foxbook adminapi/controller/Category）
 */
@RestController
@RequestMapping({"/adminapi/category", "/adminapi/Category"})
@Tag(name = "圈子类目管理")
public class CategoryAdminController extends BaseController {

    @Autowired
    private CategoryService service;

    /** GET /adminapi/category/getListByPage 分页列表 */
    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(name = "queryForm[name]", required = false) String name,
                                    @RequestParam(name = "queryForm[created_at][0]", required = false) String createdStart,
                                    @RequestParam(name = "queryForm[created_at][1]", required = false) String createdEnd) {
        getAdminId();
        return pageResult(service.getListByPage(page, pageSize, name, createdStart, createdEnd));
    }

    /** GET /adminapi/category/getList 全部类目 */
    @GetMapping("/getList")
    public Result<?> getList() {
        getAdminId();
        return Result.success(service.list());
    }

    /** GET /adminapi/category/getCategoryDetail 类目详情 */
    @GetMapping("/getCategoryDetail")
    public Result<Category> getCategoryDetail(@RequestParam Integer id) {
        getAdminId();
        return Result.success("获取类目详情成功", service.getCategoryOrFail(id));
    }

    /** GET /adminapi/category/getDetail 类目详情（通用接口名） */
    @GetMapping("/getDetail")
    public Result<Category> getDetail(@RequestParam Integer id) {
        getAdminId();
        return Result.success(service.getCategoryOrFail(id));
    }

    /** POST /adminapi/category/createCategory 创建类目 */
    @PostMapping("/createCategory")
    public Result<Category> createCategory(@RequestBody Map<String, Object> params) {
        getAdminId();
        String name = params.get("name") != null ? params.get("name").toString() : null;
        return Result.success("创建类目成功", service.createCategory(name));
    }

    /** POST /adminapi/category/updateCategory 更新类目 */
    @PostMapping("/updateCategory")
    public Result<Category> updateCategory(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        String name = params.get("name") != null ? params.get("name").toString() : null;
        return Result.success("更新类目成功", service.updateCategory(id, name));
    }

    /** POST /adminapi/category/save 通用保存（带id更新，不带id新增） */
    @PostMapping("/save")
    public Result<Category> save(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        String name = params.get("name") != null ? params.get("name").toString() : null;
        if (id != null) {
            return Result.success("保存成功", service.updateCategory(id, name));
        }
        return Result.success("保存成功", service.createCategory(name));
    }

    /** POST /adminapi/category/deleteCategory 删除类目 */
    @PostMapping("/deleteCategory")
    public Result<?> deleteCategory(@RequestBody Map<String, Object> params) {
        getAdminId();
        Integer id = intOf(params.get("id"));
        service.getCategoryOrFail(id);
        service.removeById(id);
        return Result.success("删除类目成功", null);
    }

    /** POST /adminapi/category/batchDeleteCategories 批量删除 */
    @PostMapping("/batchDeleteCategories")
    public Result<?> batchDeleteCategories(@RequestBody Map<String, Object> params) {
        getAdminId();
        List<Integer> ids = idsOf(params.get("ids"));
        if (ids.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "请选择要删除的类目");
        }
        service.removeByIds(ids);
        return Result.success("批量删除成功", Map.of("deleted_count", ids.size()));
    }

    /** GET /adminapi/category/getCategoryOptions 获取类目选项 */
    @GetMapping("/getCategoryOptions")
    public Result<?> getCategoryOptions() {
        getAdminId();
        return Result.success("获取类目选项成功", service.getCategoryOptions());
    }

    // ===================== 参数解析 =====================

    private Integer intOf(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try { return Integer.valueOf(s); } catch (NumberFormatException e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private List<Integer> idsOf(Object o) {
        if (!(o instanceof List<?> raw)) return List.of();
        return raw.stream().map(this::intOf).filter(java.util.Objects::nonNull).distinct().toList();
    }
}
