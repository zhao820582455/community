package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.WebTopNav;
import com.zbtech.community.mapper.WebTopNavMapper;
import com.zbtech.community.service.WebTopNavService;
import org.springframework.stereotype.Service;

@Service
public class WebTopNavServiceImpl extends ServiceImpl<WebTopNavMapper, WebTopNav> implements WebTopNavService {

    @Override
    public IPage<WebTopNav> getListByPage(int page, int pageSize, String title, String url,
                                           String navKey, Integer status,
                                           String createdStart, String createdEnd) {
        Page<WebTopNav> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<WebTopNav> w = new LambdaQueryWrapper<>();
        if (title != null && !title.isBlank()) {
            w.like(WebTopNav::getTitle, title.trim());
        }
        if (url != null && !url.isBlank()) {
            w.like(WebTopNav::getUrl, url.trim());
        }
        if (navKey != null && !navKey.isBlank()) {
            w.like(WebTopNav::getNav_key, navKey.trim());
        }
        if (status != null) {
            w.eq(WebTopNav::getStatus, status);
        }
        if (createdStart != null && !createdStart.isBlank()) {
            w.ge(WebTopNav::getCreated_at, createdStart.trim() + " 00:00:00");
        }
        if (createdEnd != null && !createdEnd.isBlank()) {
            w.le(WebTopNav::getCreated_at, createdEnd.trim() + " 23:59:59");
        }
        w.orderByAsc(WebTopNav::getSort).orderByDesc(WebTopNav::getCreated_at);
        return page(p, w);
    }

    @Override
    public WebTopNav getNavOrFail(Integer id) {
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "菜单ID不能为空");
        }
        WebTopNav nav = getById(id);
        if (nav == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "菜单不存在");
        }
        return nav;
    }
}
