package com.zbtech.community.controller.api;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.Agreement;
import com.zbtech.community.service.AgreementService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端协议接口（对齐原 foxbook api/controller/Agreement）
 * 路径规则：/api/agreement/{action}
 */
@RestController
@RequestMapping("/api/agreement")
public class AgreementApiController extends BaseController {

    @Resource
    private AgreementService agreementService;

    /**
     * GET /api/agreement/getAgreement
     * 按协议ID获取协议内容（无需登录，公开接口）
     */
    @GetMapping("/getAgreement")
    public Result<Agreement> getAgreement(@RequestParam Integer id) {
        return Result.success(agreementService.getAgreementOrFail(id));
    }
}
