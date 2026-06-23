USE emr_record;

CREATE TABLE IF NOT EXISTS binglixinxi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  record_no VARCHAR(32) NOT NULL COMMENT '病历编号',
  appointment_id BIGINT DEFAULT NULL COMMENT '预约ID',
  appointment_no VARCHAR(32) DEFAULT NULL COMMENT '预约编号',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  doctor_id BIGINT NOT NULL COMMENT '医生ID',
  doctor_name VARCHAR(64) NOT NULL COMMENT '医生姓名',
  visit_time DATETIME NOT NULL COMMENT '就诊时间',
  chief_complaint VARCHAR(500) DEFAULT NULL COMMENT '主诉',
  present_illness TEXT COMMENT '现病史',
  past_history TEXT COMMENT '既往史',
  diagnosis TEXT COMMENT '诊断结果',
  treatment_advice TEXT COMMENT '诊疗医嘱',
  file_url VARCHAR(500) DEFAULT NULL COMMENT '病历附件地址',
  archive_status VARCHAR(32) NOT NULL DEFAULT 'not_submitted' COMMENT '归档状态: not_submitted/pending/archived/rejected',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_binglixinxi_no (record_no),
  KEY idx_binglixinxi_patient (patient_id),
  KEY idx_binglixinxi_doctor_time (doctor_id, visit_time),
  KEY idx_binglixinxi_archive_status (archive_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历信息表';

CREATE TABLE IF NOT EXISTS binglimoban (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
  template_type VARCHAR(64) DEFAULT NULL COMMENT '模板类型',
  content TEXT NOT NULL COMMENT '模板内容',
  creator_id BIGINT DEFAULT NULL COMMENT '创建人ID',
  creator_name VARCHAR(64) DEFAULT NULL COMMENT '创建人姓名',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_binglimoban_type_status (template_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历模板表';

CREATE TABLE IF NOT EXISTS bingliguidangshenqing (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  application_no VARCHAR(32) NOT NULL COMMENT '归档申请编号',
  record_id BIGINT NOT NULL COMMENT '病历ID',
  record_no VARCHAR(32) NOT NULL COMMENT '病历编号',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  doctor_id BIGINT NOT NULL COMMENT '提交医生ID',
  doctor_name VARCHAR(64) NOT NULL COMMENT '提交医生姓名',
  status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/approved/rejected',
  audit_user_id BIGINT DEFAULT NULL COMMENT '审核人ID',
  audit_user_name VARCHAR(64) DEFAULT NULL COMMENT '审核人姓名',
  audit_opinion VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_guidangshenqing_no (application_no),
  KEY idx_guidangshenqing_record (record_id),
  KEY idx_guidangshenqing_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历归档申请表';

CREATE TABLE IF NOT EXISTS bingliguidang (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  archive_no VARCHAR(32) NOT NULL COMMENT '归档编号',
  application_id BIGINT NOT NULL COMMENT '归档申请ID',
  record_id BIGINT NOT NULL COMMENT '病历ID',
  record_no VARCHAR(32) NOT NULL COMMENT '病历编号',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  archive_content LONGTEXT COMMENT '归档内容快照',
  archived_at DATETIME NOT NULL COMMENT '归档时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_bingliguidang_no (archive_no),
  KEY idx_bingliguidang_record (record_id),
  KEY idx_bingliguidang_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历归档表';
