package com.zbtech.community.controller.api;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.Link;
import com.zbtech.community.service.LinkService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端广告/Banner接口（对齐原 foxbook api/controller/Link）
 * PHP 端继承 BaseApi，仅有通用 getListByPage/getList/getDetail
 */
@RestController
@RequestMapping("/api/link")
public class LinkApiController extends BaseController {

    @Resource
    private LinkService linkService;

    /** GET /api/link/getListByPage 分页列表 */
    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize) {
        return pageResult(linkService.page(toPage(page, pageSize)));
    }

    /** GET /api/link/getList 全部广告 */
    @GetMapping("/getList")
    public Result<List<Link>> getList() {
        return Result.success(linkService.list());
    }

    /** GET /api/link/getDetail 广告详情 */
    @GetMapping("/getDetail")
    public Result<Link> getDetail(@RequestParam Integer id) {
        return Result.success(linkService.getById(id));
    }
}
