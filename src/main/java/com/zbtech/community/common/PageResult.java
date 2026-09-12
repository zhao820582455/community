package com.zbtech.community.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结构（对齐原 foxbook BaseController::dataByPage）
 * { list, total, page, pageSize, lastPage, hasMore }
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> list;
    private Long total;
    private Long page;
    private Long pageSize;
    private Long lastPage;
    private Boolean hasMore;

    public static <T> PageResult<T> of(List<T> list, Long total, Long page, Long pageSize) {
        PageResult<T> pr = new PageResult<>();
        pr.setList(list);
        pr.setTotal(total);
        pr.setPage(page);
        pr.setPageSize(pageSize);
        long lastPage = pageSize == null || pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize;
        pr.setLastPage(lastPage);
        pr.setHasMore(page != null && pageSize != null && pageSize > 0 && page * pageSize < total);
        return pr;
    }
}
