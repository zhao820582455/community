package com.zbtech.community.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbtech.community.entity.CommentLikes;
import com.zbtech.community.mapper.CommentLikesMapper;
import com.zbtech.community.service.CommentLikesService;
import org.springframework.stereotype.Service;

@Service
public class CommentLikesServiceImpl extends ServiceImpl<CommentLikesMapper, CommentLikes> implements CommentLikesService {
}
