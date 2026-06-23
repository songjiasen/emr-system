package com.emr.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 系统服务启动类。
 * 负责资讯、留言、菜单、轮播图、系统配置和操作日志。
 */
@SpringBootApplication
@MapperScan("com.emr.system.mapper")
public class SystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}
