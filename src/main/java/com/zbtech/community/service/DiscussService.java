package com.zbtech.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbtech.community.entity.Discuss;
import com.zbtech.community.vo.DiscussVO;

import java.util.List;

public interface DiscussService extends IService<Discuss> {

    /** 发布话题 */
    Discuss publish(Integer userId, String title, String content, String media);

    /** 话题详情（浏览量 +1） */
    DiscussVO getDetail(Integer id);

    /** 搜索话题（标题/内容 like，limit 10） */
    List<DiscussVO> searchDiscusses(String keyword);

    /** 话题分页（支持 title/user_id 过滤，预加载发起人） */
    IPage<DiscussVO> pageDiscuss(int page, int pageSize, String title, Integer userId);
}
