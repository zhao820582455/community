package com.zbtech.community.controller.api;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.Category;
import com.zbtech.community.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端类目模块（对齐原 foxbook api/controller/Category，仅列表，无自定义方法）
 * 路由前缀 /api/category
 */
@RestController
@RequestMapping("/api/category")
public class CategoryApiController extends BaseController {

    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** 类目列表（前端拉分类树） */
    @GetMapping("/getList")
    public Result<List<Category>> getList() {
        return Result.success(categoryService.list());
    }
}
