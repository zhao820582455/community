package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.AuthTag;
import com.zbtech.community.mapper.AuthTagMapper;
import com.zbtech.community.service.AuthTagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthTagServiceImpl extends ServiceImpl<AuthTagMapper, AuthTag> implements AuthTagService {

    @Override
    public IPage<AuthTag> getListByPage(int page, int pageSize, String name, Integer status) {
        Page<AuthTag> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<AuthTag> w = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            w.like(AuthTag::getName, name.trim());
        }
        if (status != null) {
            w.eq(AuthTag::getStatus, status);
        }
        w.orderByAsc(AuthTag::getSort).orderByDesc(AuthTag::getCreated_at);
        return page(p, w);
    }

    @Override
    public AuthTag getTagOrFail(Integer id) {
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "标签ID不能为空");
        }
        AuthTag tag = getById(id);
        if (tag == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "标签不存在");
        }
        return tag;
    }

    @Override
    @Transactional
    public AuthTag saveTag(Integer id, String name, String color, Integer sort, Integer status) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty() || trimmedName.length() > 20) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "标签名称长度必须在 1-20 个字符之间");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "状态值无效");
        }

        // 校验名称唯一（排除自身）
        LambdaQueryWrapper<AuthTag> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(AuthTag::getName, trimmedName);
        if (id != null) {
            dupCheck.ne(AuthTag::getId, id);
        }
        if (count(dupCheck) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "标签名称已存在");
        }

        String resolvedColor = (color == null || color.trim().isEmpty()) ? "#1677ff" : color.trim();
        int resolvedSort = sort == null ? 0 : sort;

        AuthTag tag;
        if (id != null) {
            tag = getTagOrFail(id);
            tag.setName(trimmedName);
            tag.setColor(resolvedColor);
            tag.setSort(resolvedSort);
            tag.setStatus(status);
            tag.setUpdated_at(LocalDateTime.now());
            updateById(tag);
        } else {
            tag = new AuthTag();
            tag.setName(trimmedName);
            tag.setColor(resolvedColor);
            tag.setSort(resolvedSort);
            tag.setStatus(status);
            tag.setCreated_at(LocalDateTime.now());
            tag.setUpdated_at(LocalDateTime.now());
            save(tag);
        }
        return getById(tag.getId());
    }
}
