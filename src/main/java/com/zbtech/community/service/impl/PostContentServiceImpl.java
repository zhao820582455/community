package com.zbtech.community.service.impl;

import com.zbtech.community.service.PostContentService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 帖子内容渲染实现（对齐原 foxbook app/service/PostContentService.php）
 * 轻量级 Markdown 渲染器：代码块 / 标题 / 列表 / 引用 / 行内样式（代码、链接、粗体、斜体）
 */
@Service
public class PostContentServiceImpl implements PostContentService {

    private static final String NO_SUMMARY = "暂无内容摘要。";

    // ---------- 公共方法 ----------

    @Override
    public String render(String content) {
        content = content == null ? "" : content;
        if (looksLikeMarkdown(content)) {
            return renderMarkdown(content);
        }
        return nl2br(escapeHtml(content));
    }

    @Override
    public String buildSummary(String content, int limit) {
        content = content == null ? "" : content;
        if (content.isEmpty()) {
            return NO_SUMMARY;
        }
        if (looksLikeMarkdown(content)) {
            content = stripMarkdown(content);
        }
        content = stripTags(content).replaceAll("\\s+", " ").trim();
        return content.isEmpty() ? NO_SUMMARY : truncateWidth(content, limit, "...");
    }

    // ---------- Markdown 判定 ----------

    private boolean looksLikeMarkdown(String content) {
        content = content.replace("\r\n", "\n").replace("\r", "\n").trim();
        if (content.isEmpty()) {
            return false;
        }
        return Pattern.compile("```[\\s\\S]*?```").matcher(content).find()
                || Pattern.compile("(?m)^#{1,6}\\s+").matcher(content).find()
                || Pattern.compile("(?m)^>\\s?").matcher(content).find()
                || Pattern.compile("(?m)^([-*+]|\\d+\\.)\\s+").matcher(content).find()
                || Pattern.compile("!\\[[^\\]]*\\]\\(([^)]+)\\)").matcher(content).find()
                || Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)").matcher(content).find()
                || Pattern.compile("`[^`\\n]+`").matcher(content).find()
                || Pattern.compile("(\\*\\*|__)\\S[\\s\\S]*?\\1").matcher(content).find();
    }

    // ---------- 块级渲染 ----------

    private String renderMarkdown(String content) {
        content = content.replace("\r\n", "\n").replace("\r", "\n").trim();
        if (content.isEmpty()) {
            return "";
        }
        String[] blocks = content.split("\n{2,}");
        StringBuilder html = new StringBuilder();
        for (String raw : blocks) {
            String block = raw.trim();
            if (block.isEmpty()) {
                continue;
            }
            if (html.length() > 0) {
                html.append("\n");
            }
            html.append(renderBlock(block));
        }
        return html.toString();
    }

    private String renderBlock(String block) {
        // 代码块
        Matcher codeMatcher = Pattern.compile("^```([^\\n]*)\\n([\\s\\S]*?)\\n```$", Pattern.DOTALL).matcher(block);
        if (codeMatcher.matches()) {
            String language = codeMatcher.group(1).trim();
            String code = escapeHtml(codeMatcher.group(2));
            String cls = language.isEmpty() ? "" : " class=\"language-" + escapeHtml(language) + "\"";
            return "<pre><code" + cls + ">" + code + "</code></pre>";
        }

        // 标题
        Matcher headingMatcher = Pattern.compile("^(#{1,6})\\s+(.*)$").matcher(block);
        if (headingMatcher.matches()) {
            int level = headingMatcher.group(1).length();
            String text = renderInline(headingMatcher.group(2).trim());
            return "<h" + level + ">" + text + "</h" + level + ">";
        }

        String[] lines = block.split("\n");
        if (isListBlock(lines)) {
            return renderList(lines);
        }

        if (isQuoteBlock(lines)) {
            StringBuilder quoted = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].replaceFirst("^>\\s?", "");
                if (i > 0) {
                    quoted.append("<br>");
                }
                quoted.append(renderInline(line));
            }
            return "<blockquote>" + quoted + "</blockquote>";
        }

        // 普通段落：行内换行转 <br>
        StringBuilder para = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                para.append("<br>");
            }
            para.append(renderInline(lines[i]));
        }
        return "<p>" + para + "</p>";
    }

    private String renderList(String[] lines) {
        boolean ordered = lines[0].trim().matches("^\\d+\\.\\s+.*");
        Pattern itemPattern = Pattern.compile(ordered ? "^\\d+\\.\\s+" : "^[-*+]\\s+");
        StringBuilder items = new StringBuilder();
        for (String line : lines) {
            String text = line.trim();
            text = itemPattern.matcher(text).replaceFirst("");
            items.append("<li>").append(renderInline(text)).append("</li>");
        }
        return ordered
                ? "<ol>" + items + "</ol>"
                : "<ul>" + items + "</ul>";
    }

    private boolean isListBlock(String[] lines) {
        if (lines.length == 0) {
            return false;
        }
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty() || !t.matches("^([-*+]|\\d+\\.)\\s+.*")) {
                return false;
            }
        }
        return true;
    }

    private boolean isQuoteBlock(String[] lines) {
        if (lines.length == 0) {
            return false;
        }
        for (String line : lines) {
            if (!line.trim().matches("^>\\s?.*")) {
                return false;
            }
        }
        return true;
    }

    // ---------- 行内渲染 ----------

    private String renderInline(String text) {
        String safe = escapeHtml(text);

        Map<String, String> replacements = new LinkedHashMap<>();
        int[] index = {0};

        // 1. 行内代码 -> 占位符
        Matcher codeMatcher = Pattern.compile("`([^`]+)`").matcher(safe);
        StringBuilder sb = new StringBuilder();
        while (codeMatcher.find()) {
            String key = "@@CODE_" + index[0]++ + "@@";
            replacements.put(key, "<code>" + codeMatcher.group(1) + "</code>");
            codeMatcher.appendReplacement(sb, Matcher.quoteReplacement(key));
        }
        codeMatcher.appendTail(sb);
        safe = sb.toString();

        // 2. 链接 -> 占位符（URL 不合法则保留纯文本 label）
        Matcher linkMatcher = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)").matcher(safe);
        sb = new StringBuilder();
        while (linkMatcher.find()) {
            String label = linkMatcher.group(1);
            String url = sanitizeUrl(linkMatcher.group(2));
            if (url.isEmpty()) {
                linkMatcher.appendReplacement(sb, Matcher.quoteReplacement(label));
                continue;
            }
            String key = "@@LINK_" + index[0]++ + "@@";
            replacements.put(key, "<a href=\"" + url + "\" target=\"_blank\" rel=\"noreferrer\">" + label + "</a>");
            linkMatcher.appendReplacement(sb, Matcher.quoteReplacement(key));
        }
        linkMatcher.appendTail(sb);
        safe = sb.toString();

        // 3. 连续 3+ 个 * / _ -> 占位符保护
        Matcher maskMatcher = Pattern.compile("\\*{3,}|_{3,}").matcher(safe);
        sb = new StringBuilder();
        while (maskMatcher.find()) {
            String key = "@@MASK_" + index[0]++ + "@@";
            replacements.put(key, maskMatcher.group());
            maskMatcher.appendReplacement(sb, Matcher.quoteReplacement(key));
        }
        maskMatcher.appendTail(sb);
        safe = sb.toString();

        // 4. **bold** / __bold__ / *italic* / _italic_
        safe = safe.replaceAll("\\*\\*(?=\\S)([^*\\n]*?\\S)\\*\\*", "<strong>$1</strong>");
        safe = safe.replaceAll("__(?=\\S)([^_\\n]*?\\S)__", "<strong>$1</strong>");
        safe = safe.replaceAll("(?<!\\*)\\*(?=\\S)([^*\\n]*?\\S)\\*(?!\\*)", "<em>$1</em>");
        safe = safe.replaceAll("(?<!_)_(?=\\S)([^_\\n]*?\\S)_(?!_)", "<em>$1</em>");

        // 5. 还原占位符
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            safe = safe.replace(entry.getKey(), entry.getValue());
        }
        return safe;
    }

    private String sanitizeUrl(String url) {
        url = url.trim();
        if (url.isEmpty()) {
            return "";
        }
        if (url.startsWith("/")) {
            return escapeHtml(url);
        }
        if (url.matches("(?i)^https?://.*")) {
            return escapeHtml(url);
        }
        return "";
    }

    // ---------- 摘要辅助 ----------

    private String stripMarkdown(String content) {
        content = content.replaceAll("```[\\s\\S]*?```", " ");
        content = content.replaceAll("(?m)^#{1,6}\\s+", "");
        content = content.replaceAll("(?m)^>\\s?", "");
        content = content.replaceAll("(?m)^([-*+]|\\d+\\.)\\s+", "");
        content = content.replaceAll("!\\[(.*?)\\]\\((.*?)\\)", "$1");
        content = content.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "$1");
        content = content.replace("**", "").replace("__", "").replace("`", "")
                .replace("*", "").replace("_", "");
        return content;
    }

    /** 去掉 HTML 标签（对齐 PHP strip_tags） */
    private String stripTags(String content) {
        return content.replaceAll("<[^>]*>", "");
    }

    /** 按显示宽度截断：全角字符按 2 计，超出加省略号（对齐 PHP mb_strimwidth） */
    private String truncateWidth(String text, int limit, String end) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += displayWidth(text.charAt(i));
            if (width > limit) {
                return text.substring(0, i) + end;
            }
        }
        return text;
    }

    private int displayWidth(char c) {
        // 粗略对齐 mb_strimwidth：非 Latin-1 字符视为全角（宽 2）
        return c > 0xFF ? 2 : 1;
    }

    // ---------- 基础工具 ----------

    /** 对齐 PHP nl2br：\n -> <br> */
    private String nl2br(String text) {
        return text.replace("\r\n", "<br>").replace("\n", "<br>").replace("\r", "<br>");
    }

    /** 对齐 PHP htmlspecialchars(ENT_QUOTES, UTF-8) */
    private String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }
}
