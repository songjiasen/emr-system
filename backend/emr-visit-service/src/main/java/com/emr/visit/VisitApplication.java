package com.emr.visit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 就诊流程服务启动类。
 * 负责预约挂号、分诊建档、入院登记、出院办理和病房分配。
 */
@SpringBootApplication
@MapperScan("com.emr.visit.mapper")
public class VisitApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisitApplication.class, args);
    }
}
