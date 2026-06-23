package com.emr.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户科室服务启动类。
 * 负责管理员、医生、患者、护士、主任和科室基础资料。
 */
@SpringBootApplication
@MapperScan("com.emr.user.mapper")
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
