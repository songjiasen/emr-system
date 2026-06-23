package com.emr.workflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 工作流服务启动类。
 * 负责病历审核、医嘱审核、检查审核和归档审核等审批流。
 */
@SpringBootApplication
@MapperScan("com.emr.workflow.mapper")
public class WorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
