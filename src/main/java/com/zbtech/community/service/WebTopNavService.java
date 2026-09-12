package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.WebTopNav;

/**
 * PC Web 顶部导航业务（对齐原 foxbook adminapi/controller/WebTopNav）
 */
public interface WebTopNavService extends IService<WebTopNav> {

    /**
     * 分页列表，支持搜索：title/url/nav_key 模糊、status 精确、created_at 日期区间
     */
    IPage<WebTopNav> getListByPage(int page, int pageSize, String title, String url,
                                    String navKey, Integer status,
                                    String createdStart, String createdEnd);

    /** 获取导航，不存在抛异常 */
    WebTopNav getNavOrFail(Integer id);
}
