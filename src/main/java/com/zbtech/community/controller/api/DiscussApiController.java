package com.zbtech.community.controller.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbtech.community.common.BaseController;
import com.zbtech.community.common.PageResult;
import com.zbtech.community.common.Result;
import com.zbtech.community.service.DiscussService;
import com.zbtech.community.vo.DiscussVO;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户端话题模块（对齐原 foxbook api/controller/Discuss）
 * 路由前缀 /api/discuss
 */
@RestController
@RequestMapping("/api/discuss")
public class DiscussApiController extends BaseController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DiscussService discussService;

    public DiscussApiController(DiscussService discussService) {
        this.discussService = discussService;
    }

    /**
     * 发布话题（对齐 PHP：前端 baseAPI.post 统一发 JSON body；兼容 form/query 调用）
     * 注意：PHP 原版 $request->all() 不区分请求格式，Java 翻译时若用 @RequestParam(required)
     * 会导致 JSON body 请求报 MissingServletRequestParameterException → "服务器异常"
     */
    @PostMapping("/publish")
    public Result<DiscussVO> publish(@RequestParam(required = false) String title,
                                     @RequestParam(required = false) String content,
                                     @RequestParam(required = false) String media,
                                     @RequestBody(required = false) Map<String, Object> body) {
        if (body != null) {
            if (title == null && body.get("title") != null) {
                title = String.valueOf(body.get("title"));
            }
            if (content == null && body.get("content") != null) {
                content = String.valueOf(body.get("content"));
            }
            if (media == null && body.get("media") != null) {
                media = toMediaJson(body.get("media"));
            }
        }
        Integer uid = getUserId();
        return Result.success(DiscussVO.from(discussService.publish(uid, title, content, media), null));
    }

    /** media：PHP 端为数组（json cast 自动序列化存 varchar 列），Java 端统一转 JSON 字符串 */
    private String toMediaJson(Object m) {
        try {
            if (m instanceof String) {
                return (String) m;
            }
            return OBJECT_MAPPER.writeValueAsString(m);
        } catch (Exception e) {
            return String.valueOf(m);
        }
    }

    /** 话题详情（浏览量 +1） */
    @GetMapping("/getInfo")
    public Result<DiscussVO> getInfo(@RequestParam Integer id) {
        return Result.success(discussService.getDetail(id));
    }

    /** GET|POST 话题分页（title/user_id 过滤；对齐 PHP：前端统一 POST + JSON body） */
    @RequestMapping(value = "/getListByPage", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<PageResult<DiscussVO>> getListByPage(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int pageSize,
                                                       @RequestParam(required = false) String title,
                                                       @RequestParam(required = false) Integer userId,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        if (body != null) {
            Object bodyPage = body.get("page");
            if (bodyPage != null) {
                Integer v = toInt(bodyPage);
                if (v != null) page = v;
            }
            Object bodyPageSize = body.get("pageSize");
            if (bodyPageSize != null) {
                Integer v = toInt(bodyPageSize);
                if (v != null) pageSize = v;
            }
            Object qf = body.get("queryForm");
            if (qf instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> qm = (Map<String, Object>) qf;
                if (title == null && qm.get("title") != null) {
                    title = String.valueOf(qm.get("title"));
                }
                if (userId == null && qm.get("user_id") != null) {
                    userId = toInt(qm.get("user_id"));
                }
            }
        }
        IPage<DiscussVO> p = discussService.pageDiscuss(page, pageSize, title, userId);
        return Result.dataByPage(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    private Integer toInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
