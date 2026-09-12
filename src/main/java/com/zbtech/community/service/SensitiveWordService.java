package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.SensitiveWord;

import java.util.List;

/**
 * 敏感词业务（对齐原 foxbook adminapi/controller/SensitiveWord + service/SensitiveWordService）
 */
public interface SensitiveWordService extends IService<SensitiveWord> {

    /**
     * 分页列表，支持搜索：word 模糊、status 精确
     */
    IPage<SensitiveWord> getListByPage(int page, int pageSize, String word, Integer status);

    /** 获取敏感词，不存在抛异常 */
    SensitiveWord getWordOrFail(Integer id);

    /**
     * 保存敏感词（新增或更新），校验词唯一
     */
    SensitiveWord saveWord(Integer id, String word, Integer status);

    /**
     * 将文本中的敏感词替换为等长星号（对齐 PHP SensitiveWordService::replaceText）
     */
    String replaceText(String content);

    /**
     * 获取所有启用的敏感词列表（status=1），按词长降序
     */
    List<String> getActiveWords();
}
