package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.entity.UserAuthTag;
import com.zbtech.community.mapper.UserAuthTagMapper;
import com.zbtech.community.service.UserAuthTagService;
import org.springframework.stereotype.Service;

@Service
public class UserAuthTagServiceImpl extends ServiceImpl<UserAuthTagMapper, UserAuthTag> implements UserAuthTagService {
}
