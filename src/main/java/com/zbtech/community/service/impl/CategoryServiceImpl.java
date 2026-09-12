package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.Category;
import com.zbtech.community.mapper.CategoryMapper;
import com.zbtech.community.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public IPage<Category> getListByPage(int page, int pageSize, String name,
                                          String createdStart, String createdEnd) {
        Page<Category> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Category> w = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            w.like(Category::getName, name.trim());
        }
        if (createdStart != null && !createdStart.isBlank()) {
            w.ge(Category::getCreated_at, createdStart.trim() + " 00:00:00");
        }
        if (createdEnd != null && !createdEnd.isBlank()) {
            w.le(Category::getCreated_at, createdEnd.trim() + " 23:59:59");
        }
        w.orderByDesc(Category::getCreated_at);
        return page(p, w);
    }

    @Override
    public Category getCategoryOrFail(Integer id) {
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "类目ID不能为空");
        }
        Category category = getById(id);
        if (category == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "类目不存在");
        }
        return category;
    }

    @Override
    @Transactional
    public Category createCategory(String name) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "类目名称不能为空");
        }
        if (trimmedName.length() > 50) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "类目名称不能超过50个字符");
        }
        // 校验名称唯一
        LambdaQueryWrapper<Category> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(Category::getName, trimmedName);
        if (count(dupCheck) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "类目名称已存在");
        }

        Category category = new Category();
        category.setName(trimmedName);
        category.setCreated_at(LocalDateTime.now());
        category.setUpdated_at(LocalDateTime.now());
        save(category);
        return getById(category.getId());
    }

    @Override
    @Transactional
    public Category updateCategory(Integer id, String name) {
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "类目ID不能为空");
        }
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "类目名称不能为空");
        }
        if (trimmedName.length() > 50) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "类目名称不能超过50个字符");
        }

        Category category = getCategoryOrFail(id);

        // 校验名称唯一（排除自身）
        LambdaQueryWrapper<Category> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(Category::getName, trimmedName).ne(Category::getId, id);
        if (count(dupCheck) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "类目名称已存在");
        }

        category.setName(trimmedName);
        category.setUpdated_at(LocalDateTime.now());
        updateById(category);
        return getById(id);
    }

    @Override
    public List<Map<String, Object>> getCategoryOptions() {
        LambdaQueryWrapper<Category> w = new LambdaQueryWrapper<>();
        w.select(Category::getId, Category::getName).orderByDesc(Category::getCreated_at);
        List<Category> categories = list(w);

        List<Map<String, Object>> options = new ArrayList<>();
        for (Category c : categories) {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("label", c.getName());
            option.put("value", c.getId());
            options.add(option);
        }
        return options;
    }
}
