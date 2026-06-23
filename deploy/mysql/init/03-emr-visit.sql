USE emr_visit;

CREATE TABLE IF NOT EXISTS yuyueguahao (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  appointment_no VARCHAR(32) NOT NULL COMMENT '预约编号',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  doctor_id BIGINT NOT NULL COMMENT '医生ID',
  doctor_name VARCHAR(64) NOT NULL COMMENT '医生姓名',
  department_id BIGINT DEFAULT NULL COMMENT '科室ID',
  department_name VARCHAR(64) DEFAULT NULL COMMENT '科室名称',
  appointment_time DATETIME NOT NULL COMMENT '预约就诊时间',
  status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/confirmed/cancelled/finished',
  cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_yuyueguahao_no (appointment_no),
  KEY idx_yuyueguahao_patient (patient_id, status),
  KEY idx_yuyueguahao_doctor_time (doctor_id, appointment_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约挂号表';

CREATE TABLE IF NOT EXISTS fenzhenjiandang (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  triage_no VARCHAR(32) NOT NULL COMMENT '分诊编号',
  appointment_id BIGINT DEFAULT NULL COMMENT '预约ID',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  nurse_id BIGINT NOT NULL COMMENT '护士ID',
  nurse_name VARCHAR(64) NOT NULL COMMENT '护士姓名',
  chief_complaint VARCHAR(500) DEFAULT NULL COMMENT '主诉',
  triage_level VARCHAR(32) DEFAULT NULL COMMENT '分诊级别',
  status VARCHAR(32) NOT NULL DEFAULT 'created' COMMENT '状态: created/assigned/closed',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_fenzhenjiandang_no (triage_no),
  KEY idx_fenzhenjiandang_patient (patient_id),
  KEY idx_fenzhenjiandang_nurse (nurse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分诊建档表';

CREATE TABLE IF NOT EXISTS ruyuanxinxi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  admission_no VARCHAR(32) NOT NULL COMMENT '入院编号',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  doctor_id BIGINT DEFAULT NULL COMMENT '负责医生ID',
  doctor_name VARCHAR(64) DEFAULT NULL COMMENT '负责医生姓名',
  nurse_id BIGINT DEFAULT NULL COMMENT '办理护士ID',
  nurse_name VARCHAR(64) DEFAULT NULL COMMENT '办理护士姓名',
  ward_no VARCHAR(32) DEFAULT NULL COMMENT '病房号',
  bed_no VARCHAR(32) DEFAULT NULL COMMENT '床位号',
  admission_time DATETIME NOT NULL COMMENT '入院时间',
  reason VARCHAR(500) DEFAULT NULL COMMENT '入院原因',
  status VARCHAR(32) NOT NULL DEFAULT 'in_hospital' COMMENT '状态: in_hospital/discharged',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_ruyuanxinxi_no (admission_no),
  KEY idx_ruyuanxinxi_patient (patient_id, status),
  KEY idx_ruyuanxinxi_ward_bed (ward_no, bed_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入院信息表';

CREATE TABLE IF NOT EXISTS chuyuanxinxi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  discharge_no VARCHAR(32) NOT NULL COMMENT '出院编号',
  admission_id BIGINT NOT NULL COMMENT '入院记录ID',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  discharge_time DATETIME NOT NULL COMMENT '出院时间',
  discharge_reason VARCHAR(500) DEFAULT NULL COMMENT '出院原因',
  discharge_summary TEXT COMMENT '出院小结',
  operator_id BIGINT DEFAULT NULL COMMENT '办理人ID',
  operator_name VARCHAR(64) DEFAULT NULL COMMENT '办理人姓名',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_chuyuanxinxi_no (discharge_no),
  KEY idx_chuyuanxinxi_patient (patient_id),
  KEY idx_chuyuanxinxi_admission (admission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出院信息表';
