package com.zbtech.community.controller.adminapi;

import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.Result;
import com.zbtech.community.entity.Favorites;
import com.zbtech.community.service.FavoritesService;
import com.baomidou.mybatisplus.extension.service.IService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子收藏表 - 管理端通用 CRUD
 */
@RestController
@RequestMapping("/adminapi/favorites")
@Tag(name = "帖子收藏表")
public class FavoritesAdminController extends BaseController {

    @Autowired
    protected FavoritesService service;

    @GetMapping("/getListByPage")
    public Result<?> getListByPage(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize) {
        return pageResult(service.page(toPage(page, pageSize)));
    }

    @GetMapping("/getList")
    public Result<?> getList() {
        return Result.success(service.list());
    }

    @GetMapping("/getDetail")
    public Result<?> getDetail(@RequestParam Integer id) {
        return Result.success(service.getById(id));
    }

    @PostMapping("/save")
    public Result<?> save(@RequestBody Favorites entity) {
        service.saveOrUpdate(entity);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<?> delete(@RequestParam Integer id) {
        service.removeById(id);
        return Result.success();
    }

    @PostMapping("/batchDelete")
    public Result<?> batchDelete(@RequestBody java.util.List<Integer> ids) {
        service.removeByIds(ids);
        return Result.success();
    }
}
