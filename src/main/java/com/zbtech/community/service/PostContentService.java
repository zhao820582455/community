package com.zbtech.community.service;

/**
 * 帖子内容渲染服务（对齐原 foxbook app/service/PostContentService）
 * 提供轻量级 Markdown 渲染与纯文本摘要能力
 */
public interface PostContentService {

    /**
     * 将帖子内容渲染为 HTML（识别 Markdown 语法则渲染，否则按纯文本转义 + 换行）
     */
    String render(String content);

    /**
     * 生成纯文本摘要，默认截取 120 字符（按显示宽度），为空时返回"暂无内容摘要。"
     */
    String buildSummary(String content, int limit);

    /**
     * 生成纯文本摘要，默认 120 字符
     */
    default String buildSummary(String content) {
        return buildSummary(content, 120);
    }
}
