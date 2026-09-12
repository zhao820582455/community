package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.entity.MediaCheck;
import com.zbtech.community.mapper.MediaCheckMapper;
import com.zbtech.community.service.MediaCheckService;
import org.springframework.stereotype.Service;

@Service
public class MediaCheckServiceImpl extends ServiceImpl<MediaCheckMapper, MediaCheck> implements MediaCheckService {
}
