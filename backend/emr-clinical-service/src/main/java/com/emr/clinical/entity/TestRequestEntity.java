package com.emr.clinical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 检查申请实体。
 * 对应 `jianchaxiang` 表，保存医生申请检查的审核状态和结果。
 */
@TableName("jianchaxiang")
public class TestRequestEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String testNo;
    private Long recordId;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String testItem;
    private String testReason;
    private String status;
    private String auditOpinion;
    private String resultContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTestNo() { return testNo; }
    public void setTestNo(String testNo) { this.testNo = testNo; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getTestItem() { return testItem; }
    public void setTestItem(String testItem) { this.testItem = testItem; }
    public String getTestReason() { return testReason; }
    public void setTestReason(String testReason) { this.testReason = testReason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAuditOpinion() { return auditOpinion; }
    public void setAuditOpinion(String auditOpinion) { this.auditOpinion = auditOpinion; }
    public String getResultContent() { return resultContent; }
    public void setResultContent(String resultContent) { this.resultContent = resultContent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
