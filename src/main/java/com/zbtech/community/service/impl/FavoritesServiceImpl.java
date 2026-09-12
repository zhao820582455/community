package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.entity.Favorites;
import com.zbtech.community.mapper.FavoritesMapper;
import com.zbtech.community.service.FavoritesService;
import org.springframework.stereotype.Service;

@Service
public class FavoritesServiceImpl extends ServiceImpl<FavoritesMapper, Favorites> implements FavoritesService {
}
