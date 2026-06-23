package com.emr.record;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 病历服务启动类。
 * 负责病历信息、既往史、病历模板、附件信息和病历归档。
 */
@SpringBootApplication
@MapperScan("com.emr.record.mapper")
public class RecordApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecordApplication.class, args);
    }
}
