package com.emr.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证权限服务启动类。
 * 负责登录、患者注册、Token、密码和角色权限相关能力。
 */
@SpringBootApplication
@MapperScan("com.emr.auth.mapper")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
