package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.Category;

import java.util.List;
import java.util.Map;

/**
 * 圈子类目业务（对齐原 foxbook adminapi/controller/Category）
 */
public interface CategoryService extends IService<Category> {

    /**
     * 分页列表，支持搜索：name 模糊、created_at 日期区间
     */
    IPage<Category> getListByPage(int page, int pageSize, String name,
                                   String createdStart, String createdEnd);

    /** 获取类目，不存在抛异常 */
    Category getCategoryOrFail(Integer id);

    /** 创建类目（校验名称唯一） */
    Category createCategory(String name);

    /** 更新类目（校验名称唯一） */
    Category updateCategory(Integer id, String name);

    /** 获取类目选项列表（label/value） */
    List<Map<String, Object>> getCategoryOptions();
}
