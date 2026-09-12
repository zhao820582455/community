package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.Agreement;
import com.zbtech.community.mapper.AgreementMapper;
import com.zbtech.community.service.AgreementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AgreementServiceImpl extends ServiceImpl<AgreementMapper, Agreement> implements AgreementService {

    @Override
    public IPage<Agreement> getListByPage(int page, int pageSize, String title,
                                          String createdStart, String createdEnd) {
        Page<Agreement> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Agreement> w = new LambdaQueryWrapper<>();
        // 对齐 PHP allowSearch/searchConfig：title 模糊、created_at 日期区间
        if (title != null && !title.isBlank()) {
            w.like(Agreement::getTitle, title.trim());
        }
        if (createdStart != null && !createdStart.isBlank()) {
            w.ge(Agreement::getCreated_at, createdStart.trim() + " 00:00:00");
        }
        if (createdEnd != null && !createdEnd.isBlank()) {
            w.le(Agreement::getCreated_at, createdEnd.trim() + " 23:59:59");
        }
        w.orderByDesc(Agreement::getCreated_at);
        return page(p, w);
    }

    @Override
    public Agreement getAgreementOrFail(Integer id) {
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "协议ID不能为空");
        }
        Agreement agreement = getById(id);
        if (agreement == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "协议不存在");
        }
        return agreement;
    }

    @Override
    @Transactional
    public Agreement updateAgreement(Integer id, String title, String content) {
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "协议ID不能为空");
        }
        String trimmedTitle = title == null ? "" : title.trim();
        if (trimmedTitle.isEmpty() || trimmedTitle.length() > 50) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "标题长度必须在 1-50 个字符之间");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "内容不能为空");
        }

        Agreement agreement = getById(id);
        if (agreement == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "协议不存在");
        }
        agreement.setTitle(trimmedTitle);
        agreement.setContent(content);
        agreement.setUpdated_at(LocalDateTime.now());
        updateById(agreement);
        return getById(id);
    }
}
