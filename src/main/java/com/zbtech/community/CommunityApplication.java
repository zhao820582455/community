package com.zbtech.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * zbtech community 启动类
 * 原 foxbook 后端 Java(Spring Boot 3.0) 翻译版
 */
@SpringBootApplication
@MapperScan("com.zbtech.community.mapper")
public class CommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunityApplication.class, args);
    }
}
