package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.Link;

/**
 * 广告/Banner 业务（对齐原 foxbook adminapi/controller/Link）
 */
public interface LinkService extends IService<Link> {

    /**
     * 分页列表，支持搜索：title/url 模糊、status/type 精确、created_at 日期区间
     */
    IPage<Link> getListByPage(int page, int pageSize, String title, String url,
                               Integer status, Integer type,
                               String createdStart, String createdEnd);

    /** 获取 Banner，不存在抛异常 */
    Link getBannerOrFail(Integer id);

    /**
     * 保存 Banner（新增或更新），含字段校验
     *
     * @param id       Banner ID（null 为新增）
     * @param title    标题（最长 10）
     * @param url      跳转链接（最长 255）
     * @param coverImg 封面图（最长 255）
     * @param type     跳转类型 1当前小程序 2外部小程序 3webview
     * @param appId    外部小程序 AppId（type=2 时必填，最长 50）
     * @return 保存后的 Banner
     */
    Link saveBanner(Integer id, String title, String url, String coverImg, Integer type, String appId);
}
