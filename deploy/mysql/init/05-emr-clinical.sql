USE emr_clinical;

CREATE TABLE IF NOT EXISTS yizhuxinxi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  order_no VARCHAR(32) NOT NULL COMMENT '医嘱编号',
  record_id BIGINT DEFAULT NULL COMMENT '病历ID',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  doctor_id BIGINT NOT NULL COMMENT '医生ID',
  doctor_name VARCHAR(64) NOT NULL COMMENT '医生姓名',
  order_type VARCHAR(64) DEFAULT NULL COMMENT '医嘱类型',
  content TEXT NOT NULL COMMENT '医嘱内容',
  status VARCHAR(32) NOT NULL DEFAULT 'pending_audit' COMMENT '状态: pending_audit/approved/rejected/executed',
  audit_opinion VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_yizhuxinxi_no (order_no),
  KEY idx_yizhuxinxi_patient (patient_id),
  KEY idx_yizhuxinxi_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医嘱信息表';

CREATE TABLE IF NOT EXISTS yizhuzhixingjilu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  order_id BIGINT NOT NULL COMMENT '医嘱ID',
  order_no VARCHAR(32) NOT NULL COMMENT '医嘱编号',
  nurse_id BIGINT NOT NULL COMMENT '执行护士ID',
  nurse_name VARCHAR(64) NOT NULL COMMENT '执行护士姓名',
  execution_time DATETIME NOT NULL COMMENT '执行时间',
  execution_result VARCHAR(500) DEFAULT NULL COMMENT '执行结果',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_yizhuzhixing_order (order_id),
  KEY idx_yizhuzhixing_nurse_time (nurse_id, execution_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医嘱执行记录表';

CREATE TABLE IF NOT EXISTS kaifang (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  prescription_no VARCHAR(32) NOT NULL COMMENT '处方编号',
  record_id BIGINT DEFAULT NULL COMMENT '病历ID',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  doctor_id BIGINT NOT NULL COMMENT '医生ID',
  doctor_name VARCHAR(64) NOT NULL COMMENT '医生姓名',
  medicine_name VARCHAR(128) NOT NULL COMMENT '药品名称',
  quantity VARCHAR(64) DEFAULT NULL COMMENT '数量',
  usage_text VARCHAR(255) DEFAULT NULL COMMENT '用法用量',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  status VARCHAR(32) NOT NULL DEFAULT 'created' COMMENT '状态: created/cancelled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_kaifang_no (prescription_no),
  KEY idx_kaifang_patient (patient_id),
  KEY idx_kaifang_doctor (doctor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='开药处方表';

CREATE TABLE IF NOT EXISTS jianchaxiang (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  test_no VARCHAR(32) NOT NULL COMMENT '检查申请编号',
  record_id BIGINT DEFAULT NULL COMMENT '病历ID',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  doctor_id BIGINT NOT NULL COMMENT '医生ID',
  doctor_name VARCHAR(64) NOT NULL COMMENT '医生姓名',
  test_item VARCHAR(128) NOT NULL COMMENT '检查项目',
  test_reason VARCHAR(500) DEFAULT NULL COMMENT '检查原因',
  status VARCHAR(32) NOT NULL DEFAULT 'pending_audit' COMMENT '状态: pending_audit/approved/rejected/finished',
  audit_opinion VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  result_content TEXT COMMENT '检查结果',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_jianchaxiang_no (test_no),
  KEY idx_jianchaxiang_patient (patient_id),
  KEY idx_jianchaxiang_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查项目申请表';
