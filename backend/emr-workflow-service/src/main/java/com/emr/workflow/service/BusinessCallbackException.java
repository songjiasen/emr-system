package com.emr.workflow.service;

/**
 * 业务状态回写异常。
 * 工作流审核需要在下游业务确认成功后再提交数据库状态，因此单独区分成 502 响应。
 */
public class BusinessCallbackException extends RuntimeException {

    public BusinessCallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
