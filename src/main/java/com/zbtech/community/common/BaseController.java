package com.zbtech.community.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 基础控制器：提供分页解析、当前用户/管理员注入等通用能力
 * （对齐原 foxbook BaseController / BaseApi / BaseAdminApi）
 */
public abstract class BaseController {

    protected HttpServletRequest getRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    /** 从请求属性读取用户端 user_id（由鉴权拦截器注入） */
    protected Integer getUserId() {
        HttpServletRequest request = getRequest();
        Object v = request == null ? null : request.getAttribute("user_id");
        if (v == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return (Integer) v;
    }

    /** 从请求属性读取用户端 user_id，未登录返回 null（不抛异常） */
    protected Integer getUserIdOptional() {
        HttpServletRequest request = getRequest();
        Object v = request == null ? null : request.getAttribute("user_id");
        return v == null ? null : (Integer) v;
    }

    /** 从请求属性读取管理端 admin_id */
    protected Integer getAdminId() {
        HttpServletRequest request = getRequest();
        Object v = request == null ? null : request.getAttribute("admin_id");
        if (v == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return (Integer) v;
    }

    /** 解析分页参数：page(默认1) / pageSize(默认10,最大1000) */
    protected <T> Page<T> toPage(int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 1000) pageSize = 1000;
        return new Page<>((long) page, (long) pageSize);
    }

    /** 把 MyBatis-Plus 分页结果包装为原 foxbook 分页结构 */
    protected <T> Result<PageResult<T>> pageResult(IPage<T> page) {
        return Result.dataByPage(PageResult.of(
                page.getRecords(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()));
    }

    protected <T> Result<List<T>> listResult(List<T> list) {
        return Result.success(list);
    }
}
