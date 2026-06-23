package com.emr.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 智能服务启动类。
 * 负责 OCR 病历识别、智能荐药、处方审核和智能检索。
 */
@SpringBootApplication
@MapperScan("com.emr.ai.mapper")
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
