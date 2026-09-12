package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.Link;
import com.zbtech.community.mapper.LinkMapper;
import com.zbtech.community.service.LinkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LinkServiceImpl extends ServiceImpl<LinkMapper, Link> implements LinkService {

    @Override
    public IPage<Link> getListByPage(int page, int pageSize, String title, String url,
                                      Integer status, Integer type,
                                      String createdStart, String createdEnd) {
        Page<Link> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Link> w = new LambdaQueryWrapper<>();
        if (title != null && !title.isBlank()) {
            w.like(Link::getTitle, title.trim());
        }
        if (url != null && !url.isBlank()) {
            w.like(Link::getUrl, url.trim());
        }
        if (type != null) {
            w.eq(Link::getType, type);
        }
        if (createdStart != null && !createdStart.isBlank()) {
            w.ge(Link::getCreated_at, createdStart.trim() + " 00:00:00");
        }
        if (createdEnd != null && !createdEnd.isBlank()) {
            w.le(Link::getCreated_at, createdEnd.trim() + " 23:59:59");
        }
        w.orderByDesc(Link::getCreated_at);
        return page(p, w);
    }

    @Override
    public Link getBannerOrFail(Integer id) {
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "广告ID不能为空");
        }
        Link banner = getById(id);
        if (banner == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "广告不存在");
        }
        return banner;
    }

    @Override
    @Transactional
    public Link saveBanner(Integer id, String title, String url, String coverImg, Integer type, String appId) {
        // 字段校验
        String trimmedTitle = title == null ? "" : title.trim();
        String trimmedUrl = url == null ? "" : url.trim();
        String trimmedCover = coverImg == null ? "" : coverImg.trim();

        if (trimmedTitle.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "广告标题不能为空");
        }
        if (trimmedTitle.length() > 10) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "广告标题不能超过10个字符");
        }
        if (trimmedUrl.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "跳转链接不能为空");
        }
        if (trimmedUrl.length() > 255) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "跳转链接不能超过255个字符");
        }
        if (trimmedCover.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "广告图片不能为空");
        }
        if (trimmedCover.length() > 255) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "图片链接不能超过255个字符");
        }

        int resolvedType = type == null ? 1 : type;
        if (resolvedType < 1 || resolvedType > 3) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "跳转类型无效");
        }

        String resolvedAppId = null;
        if (resolvedType == 2) {
            if (appId == null || appId.trim().isEmpty()) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "外部小程序 AppId 不能为空");
            }
            if (appId.trim().length() > 50) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "AppId 不能超过50个字符");
            }
            resolvedAppId = appId.trim();
        }

        Link banner;
        if (id != null) {
            banner = getBannerOrFail(id);
            banner.setTitle(trimmedTitle);
            banner.setUrl(trimmedUrl);
            banner.setCover_img(trimmedCover);
            banner.setType(resolvedType);
            banner.setApp_id(resolvedAppId);
            banner.setUpdated_at(LocalDateTime.now());
            updateById(banner);
        } else {
            banner = new Link();
            banner.setTitle(trimmedTitle);
            banner.setUrl(trimmedUrl);
            banner.setCover_img(trimmedCover);
            banner.setType(resolvedType);
            banner.setApp_id(resolvedAppId);
            banner.setCreated_at(LocalDateTime.now());
            banner.setUpdated_at(LocalDateTime.now());
            save(banner);
        }
        return getById(banner.getId());
    }
}
