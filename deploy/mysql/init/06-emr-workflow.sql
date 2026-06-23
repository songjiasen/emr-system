USE emr_workflow;

CREATE TABLE IF NOT EXISTS shenherenwu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_no VARCHAR(32) NOT NULL COMMENT '审核任务编号',
  business_type VARCHAR(64) NOT NULL COMMENT '业务类型: medical_order/test_request/record_archive/medical_record',
  business_id BIGINT NOT NULL COMMENT '业务ID',
  business_no VARCHAR(32) DEFAULT NULL COMMENT '业务编号',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID',
  applicant_name VARCHAR(64) NOT NULL COMMENT '申请人姓名',
  assignee_role VARCHAR(32) NOT NULL COMMENT '审核角色: director/admin',
  status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/approved/rejected',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_shenherenwu_no (task_no),
  KEY idx_shenherenwu_business (business_type, business_id),
  KEY idx_shenherenwu_status_role (status, assignee_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核任务表';

CREATE TABLE IF NOT EXISTS shenhejilu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  task_id BIGINT NOT NULL COMMENT '审核任务ID',
  task_no VARCHAR(32) NOT NULL COMMENT '审核任务编号',
  auditor_id BIGINT NOT NULL COMMENT '审核人ID',
  auditor_name VARCHAR(64) NOT NULL COMMENT '审核人姓名',
  audit_result VARCHAR(32) NOT NULL COMMENT '审核结果: approved/rejected',
  audit_opinion VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  audit_time DATETIME NOT NULL COMMENT '审核时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_shenhejilu_task (task_id),
  KEY idx_shenhejilu_auditor_time (auditor_id, audit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核记录表';

