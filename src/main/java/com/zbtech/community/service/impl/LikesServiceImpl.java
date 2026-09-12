package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.entity.Likes;
import com.zbtech.community.mapper.LikesMapper;
import com.zbtech.community.service.LikesService;
import org.springframework.stereotype.Service;

@Service
public class LikesServiceImpl extends ServiceImpl<LikesMapper, Likes> implements LikesService {
}
