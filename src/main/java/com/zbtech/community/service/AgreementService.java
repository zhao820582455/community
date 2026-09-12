package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.Agreement;

/**
 * 协议业务（对齐原 foxbook api/adminapi controller/Agreement）
 */
public interface AgreementService extends IService<Agreement> {

    /**
     * 分页列表（标题模糊 + 创建时间范围）
     *
     * @param title        标题关键字，可空
     * @param createdStart 创建时间起（yyyy-MM-dd），可空
     * @param createdEnd   创建时间止（yyyy-MM-dd），可空
     */
    IPage<Agreement> getListByPage(int page, int pageSize, String title,
                                   String createdStart, String createdEnd);

    /** 协议详情，不存在抛 NOT_FOUND */
    Agreement getAgreementOrFail(Integer id);

    /** 更新协议标题与内容（对齐 adminapi saveAgreement） */
    Agreement updateAgreement(Integer id, String title, String content);
}
