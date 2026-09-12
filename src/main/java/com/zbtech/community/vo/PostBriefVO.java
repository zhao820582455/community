package com.zbtech.community.vo;

import lombok.Data;

/**
 * 帖子简要信息（评论/消息关联返回，原 foxbook 帖子封面存于 media，不单独建列）
 */
@Data
public class PostBriefVO {
    private Integer id;
    private String title;
    /** 内容摘要（搜索场景为 buildSummary 生成的纯文本摘要，非全文） */
    private String content;
}
