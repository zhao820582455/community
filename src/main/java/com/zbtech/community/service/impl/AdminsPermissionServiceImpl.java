package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.entity.AdminsPermission;
import com.zbtech.community.mapper.AdminsPermissionMapper;
import com.zbtech.community.service.AdminsPermissionService;
import org.springframework.stereotype.Service;

@Service
public class AdminsPermissionServiceImpl extends ServiceImpl<AdminsPermissionMapper, AdminsPermission> implements AdminsPermissionService {
}
