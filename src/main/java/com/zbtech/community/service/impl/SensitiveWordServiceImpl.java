package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.common.BizException;
import com.zbtech.community.common.ErrorCode;
import com.zbtech.community.entity.SensitiveWord;
import com.zbtech.community.mapper.SensitiveWordMapper;
import com.zbtech.community.service.SensitiveWordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord> implements SensitiveWordService {

    @Override
    public IPage<SensitiveWord> getListByPage(int page, int pageSize, String word, Integer status) {
        Page<SensitiveWord> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<SensitiveWord> w = new LambdaQueryWrapper<>();
        if (word != null && !word.isBlank()) {
            w.like(SensitiveWord::getWord, word.trim());
        }
        if (status != null) {
            w.eq(SensitiveWord::getStatus, status);
        }
        w.orderByDesc(SensitiveWord::getCreated_at);
        return page(p, w);
    }

    @Override
    public SensitiveWord getWordOrFail(Integer id) {
        if (id == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "敏感词ID不能为空");
        }
        SensitiveWord record = getById(id);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "敏感词不存在");
        }
        return record;
    }

    @Override
    @Transactional
    public SensitiveWord saveWord(Integer id, String word, Integer status) {
        String trimmedWord = word == null ? "" : word.trim();
        if (trimmedWord.isEmpty() || trimmedWord.length() > 50) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "敏感词长度必须在 1-50 个字符之间");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "状态值无效");
        }

        // 校验词唯一（排除自身）
        LambdaQueryWrapper<SensitiveWord> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(SensitiveWord::getWord, trimmedWord);
        if (id != null) {
            dupCheck.ne(SensitiveWord::getId, id);
        }
        if (count(dupCheck) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "敏感词已存在");
        }

        SensitiveWord record;
        if (id != null) {
            record = getWordOrFail(id);
            record.setWord(trimmedWord);
            record.setStatus(status);
            record.setUpdated_at(LocalDateTime.now());
            updateById(record);
        } else {
            record = new SensitiveWord();
            record.setWord(trimmedWord);
            record.setStatus(status);
            record.setCreated_at(LocalDateTime.now());
            record.setUpdated_at(LocalDateTime.now());
            save(record);
        }
        return getById(record.getId());
    }

    @Override
    public String replaceText(String content) {
        if (content == null || content.isEmpty()) {
            return content == null ? "" : content;
        }
        List<String> words = getActiveWords();
        if (words.isEmpty()) {
            return content;
        }

        // 构建正则模式：(word1|word2|...)，大小写不敏感
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                sb.append("|");
            }
            sb.append(Pattern.quote(words.get(i)));
        }
        Pattern pattern = Pattern.compile("(" + sb + ")", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return pattern.matcher(content).replaceAll(match -> {
            String m = match.group();
            return "*".repeat(Math.max(m.length(), 1));
        });
    }

    @Override
    public List<String> getActiveWords() {
        LambdaQueryWrapper<SensitiveWord> w = new LambdaQueryWrapper<>();
        w.eq(SensitiveWord::getStatus, 1);
        w.orderByDesc(SensitiveWord::getUpdated_at);
        List<SensitiveWord> records = list(w);

        // 去重、去空白、按词长降序排列（优先匹配长词）
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (SensitiveWord r : records) {
            if (r.getWord() != null && !r.getWord().trim().isEmpty()) {
                set.add(r.getWord().trim());
            }
        }
        List<String> result = new ArrayList<>(set);
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }
}
