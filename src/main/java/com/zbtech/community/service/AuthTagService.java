package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.AuthTag;

/**
 * 认证标签业务（对齐原 foxbook adminapi/controller/AuthTag）
 */
public interface AuthTagService extends IService<AuthTag> {

    /**
     * 分页列表，支持搜索：name 模糊、status 精确
     */
    IPage<AuthTag> getListByPage(int page, int pageSize, String name, Integer status);

    /** 获取标签，不存在抛异常 */
    AuthTag getTagOrFail(Integer id);

    /**
     * 保存标签（新增或更新），校验名称唯一
     *
     * @param id     标签ID（null 为新增）
     * @param name   标签名称
     * @param color  标签颜色（默认 #1677ff）
     * @param sort   排序
     * @param status 状态 0/1
     * @return 保存后的标签
     */
    AuthTag saveTag(Integer id, String name, String color, Integer sort, Integer status);
}
