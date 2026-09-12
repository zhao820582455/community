package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.entity.AdminsRoles;
import com.zbtech.community.mapper.AdminsRolesMapper;
import com.zbtech.community.service.AdminsRolesService;
import org.springframework.stereotype.Service;

@Service
public class AdminsRolesServiceImpl extends ServiceImpl<AdminsRolesMapper, AdminsRoles> implements AdminsRolesService {
}
