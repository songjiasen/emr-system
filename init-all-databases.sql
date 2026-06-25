-- 安心医疗系统一体化数据库初始化脚本
-- 用法：mysql -uroot -p < init-all-databases.sql
-- 说明：
-- 1. 适用于 MySQL 8.x。
-- 2. 不执行 DROP/TRUNCATE，不主动清空已有业务数据。
-- 3. 演示账号、配置和样例数据使用固定主键或唯一键做 upsert，重复执行会刷新这些演示数据。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS emr_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS emr_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS emr_visit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS emr_record DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS emr_clinical DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS emr_workflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS emr_billing DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS emr_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS emr_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE emr_auth;

CREATE TABLE IF NOT EXISTS token (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '登录用户ID',
  username VARCHAR(64) NOT NULL COMMENT '登录账号',
  role_code VARCHAR(32) NOT NULL COMMENT '角色编码: super_admin/admin/doctor/patient/nurse/director',
  table_name VARCHAR(64) NOT NULL COMMENT '用户来源表: users/yisheng/huanzhe',
  token VARCHAR(128) NOT NULL COMMENT '访问令牌',
  expire_at DATETIME NOT NULL COMMENT '过期时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_token_token (token),
  KEY idx_token_user_role (user_id, role_code),
  KEY idx_token_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token认证表';

USE emr_user;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(64) NOT NULL COMMENT '账号',
  password VARCHAR(128) NOT NULL COMMENT '加密密码',
  real_name VARCHAR(64) NOT NULL COMMENT '姓名',
  role_code VARCHAR(32) NOT NULL COMMENT '角色编码: super_admin/admin/nurse/director',
  gender VARCHAR(8) DEFAULT NULL COMMENT '性别',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  department_id BIGINT DEFAULT NULL COMMENT '护士所属科室ID',
  department_name VARCHAR(64) DEFAULT NULL COMMENT '护士所属科室名称冗余',
  avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_department_nurse (department_id),
  KEY idx_users_role_status (role_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS keshileixing (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(64) NOT NULL COMMENT '科室类型名称',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_keshileixing_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室类型表';

CREATE TABLE IF NOT EXISTS yisheng (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(64) NOT NULL COMMENT '医生账号',
  password VARCHAR(128) NOT NULL COMMENT '加密密码',
  name VARCHAR(64) NOT NULL COMMENT '医生姓名',
  gender VARCHAR(8) DEFAULT NULL COMMENT '性别',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  department_id BIGINT DEFAULT NULL COMMENT '科室ID',
  department_name VARCHAR(64) DEFAULT NULL COMMENT '科室名称冗余',
  specialty VARCHAR(255) DEFAULT NULL COMMENT '擅长领域',
  profile TEXT COMMENT '个人简介',
  avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_yisheng_username (username),
  KEY idx_yisheng_department (department_id),
  KEY idx_yisheng_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生信息表';

CREATE TABLE IF NOT EXISTS huanzhe (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(64) NOT NULL COMMENT '患者账号',
  password VARCHAR(128) NOT NULL COMMENT '加密密码',
  name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  gender VARCHAR(8) DEFAULT NULL COMMENT '性别',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  id_card VARCHAR(32) DEFAULT NULL COMMENT '身份证号',
  birth_date DATE DEFAULT NULL COMMENT '出生日期',
  address VARCHAR(255) DEFAULT NULL COMMENT '联系地址',
  emergency_contact VARCHAR(64) DEFAULT NULL COMMENT '紧急联系人',
  emergency_phone VARCHAR(20) DEFAULT NULL COMMENT '紧急联系人电话',
  balance DECIMAL(10,2) NOT NULL DEFAULT 500.00 COMMENT '账户余额',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_huanzhe_username (username),
  KEY idx_huanzhe_phone (phone),
  KEY idx_huanzhe_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者信息表';

INSERT INTO keshileixing (id, name, sort_no, status)
VALUES
  (1, '心内科', 10, 1),
  (2, '儿科', 20, 1),
  (3, '全科医学科', 30, 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  sort_no = VALUES(sort_no),
  status = VALUES(status);

INSERT INTO users (id, username, password, real_name, role_code, gender, phone, department_id, department_name, status)
VALUES
  (1, 'admin', '$2a$10$AFXB5AJBJMPBN2xzDOBOSeDAjtJInkGuMoHQzeT7aXrHQQ.dkBs12', '系统管理员', 'admin', NULL, '13800000000', NULL, NULL, 1),
  (2, 'super_admin', '$2a$10$AFXB5AJBJMPBN2xzDOBOSeDAjtJInkGuMoHQzeT7aXrHQQ.dkBs12', '超级管理员', 'super_admin', NULL, '13800000001', NULL, NULL, 1),
  (3, 'nurse', '$2a$10$1lzYb6DKYQ96jNZigZd2keKt0iExFwRxuUhzexAx5G0rtxxZvpdU6', '演示护士', 'nurse', '女', '13800000003', 1, '心内科', 1),
  (4, 'director', '$2a$10$1lzYb6DKYQ96jNZigZd2keKt0iExFwRxuUhzexAx5G0rtxxZvpdU6', '演示主任', 'director', '男', '13800000004', NULL, NULL, 1)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  real_name = VALUES(real_name),
  role_code = VALUES(role_code),
  gender = VALUES(gender),
  phone = VALUES(phone),
  department_id = VALUES(department_id),
  department_name = VALUES(department_name),
  status = VALUES(status);

INSERT INTO yisheng (id, username, password, name, gender, phone, department_id, department_name, specialty, profile, status)
VALUES
  (1, 'doctor', '$2a$10$1lzYb6DKYQ96jNZigZd2keKt0iExFwRxuUhzexAx5G0rtxxZvpdU6', '演示医生', '男', '13800000002', 3, '全科医学科', '常见病诊疗', '负责门诊常见病、多发病诊疗。', 1),
  (2, 'doctor_heart', '$2a$10$1lzYb6DKYQ96jNZigZd2keKt0iExFwRxuUhzexAx5G0rtxxZvpdU6', '王医生', '男', '13900000001', 1, '心内科', '高血压、冠心病', '从事心内科临床工作多年，擅长常见心血管疾病诊疗。', 1),
  (3, 'doctor_child', '$2a$10$1lzYb6DKYQ96jNZigZd2keKt0iExFwRxuUhzexAx5G0rtxxZvpdU6', '李医生', '女', '13900000002', 2, '儿科', '儿童呼吸道疾病', '擅长儿童常见病、多发病诊疗。', 1)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  name = VALUES(name),
  gender = VALUES(gender),
  phone = VALUES(phone),
  department_id = VALUES(department_id),
  department_name = VALUES(department_name),
  specialty = VALUES(specialty),
  profile = VALUES(profile),
  status = VALUES(status);

INSERT INTO huanzhe (id, username, password, name, gender, phone, id_card, birth_date, address, emergency_contact, emergency_phone, balance, status)
VALUES
  (1, 'patient_demo', '$2a$10$1lzYb6DKYQ96jNZigZd2keKt0iExFwRxuUhzexAx5G0rtxxZvpdU6', '患者演示', '男', '13800000005', '110101199001010011', '1990-01-01', '北京市朝阳区演示地址', '家属演示', '13800000006', 500.00, 1),
  (2, 'patient_family', '$2a$10$1lzYb6DKYQ96jNZigZd2keKt0iExFwRxuUhzexAx5G0rtxxZvpdU6', '李患者', '女', '13800000007', '110101199202020022', '1992-02-02', '北京市海淀区演示地址', '联系人演示', '13800000008', 300.00, 1)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  name = VALUES(name),
  gender = VALUES(gender),
  phone = VALUES(phone),
  id_card = VALUES(id_card),
  birth_date = VALUES(birth_date),
  address = VALUES(address),
  emergency_contact = VALUES(emergency_contact),
  emergency_phone = VALUES(emergency_phone),
  balance = VALUES(balance),
  status = VALUES(status);

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

INSERT INTO yuyueguahao (id, appointment_no, patient_id, patient_name, doctor_id, doctor_name, department_id, department_name, appointment_time, status, cancel_reason, remark)
VALUES
  (1, 'YY202606220001', 1, '患者演示', 2, '王医生', 1, '心内科', '2026-06-22 09:00:00', 'confirmed', NULL, '演示预约'),
  (2, 'YY202606220002', 2, '李患者', 3, '李医生', 2, '儿科', '2026-06-23 10:30:00', 'pending', NULL, '待确认预约')
ON DUPLICATE KEY UPDATE
  patient_id = VALUES(patient_id),
  patient_name = VALUES(patient_name),
  doctor_id = VALUES(doctor_id),
  doctor_name = VALUES(doctor_name),
  department_id = VALUES(department_id),
  department_name = VALUES(department_name),
  appointment_time = VALUES(appointment_time),
  status = VALUES(status),
  cancel_reason = VALUES(cancel_reason),
  remark = VALUES(remark);

INSERT INTO fenzhenjiandang (id, triage_no, appointment_id, patient_id, patient_name, nurse_id, nurse_name, chief_complaint, triage_level, status)
VALUES
  (1, 'FZ202606220001', 1, 1, '患者演示', 3, '演示护士', '头晕一周', 'normal', 'created')
ON DUPLICATE KEY UPDATE
  appointment_id = VALUES(appointment_id),
  patient_id = VALUES(patient_id),
  patient_name = VALUES(patient_name),
  nurse_id = VALUES(nurse_id),
  nurse_name = VALUES(nurse_name),
  chief_complaint = VALUES(chief_complaint),
  triage_level = VALUES(triage_level),
  status = VALUES(status);

INSERT INTO ruyuanxinxi (id, admission_no, patient_id, patient_name, doctor_id, doctor_name, nurse_id, nurse_name, ward_no, bed_no, admission_time, reason, status)
VALUES
  (1, 'RY202606220001', 1, '患者演示', 2, '王医生', 3, '演示护士', 'A1', 'A1-08', '2026-06-22 14:00:00', '观察治疗', 'in_hospital')
ON DUPLICATE KEY UPDATE
  patient_id = VALUES(patient_id),
  patient_name = VALUES(patient_name),
  doctor_id = VALUES(doctor_id),
  doctor_name = VALUES(doctor_name),
  nurse_id = VALUES(nurse_id),
  nurse_name = VALUES(nurse_name),
  ward_no = VALUES(ward_no),
  bed_no = VALUES(bed_no),
  admission_time = VALUES(admission_time),
  reason = VALUES(reason),
  status = VALUES(status);

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

INSERT INTO binglixinxi (id, record_no, appointment_id, appointment_no, patient_id, patient_name, doctor_id, doctor_name, visit_time, chief_complaint, present_illness, past_history, diagnosis, treatment_advice, file_url, archive_status)
VALUES
  (1, 'BL202606220001', 1, 'YY202606220001', 1, '患者演示', 2, '王医生', '2026-06-22 09:30:00', '头晕一周', '近一周反复头晕，活动后明显。', '高血压病史三年。', '高血压', '规律服药，低盐饮食，定期复查。', '/uploads/demo-record.pdf', 'not_submitted')
ON DUPLICATE KEY UPDATE
  appointment_id = VALUES(appointment_id),
  appointment_no = VALUES(appointment_no),
  patient_id = VALUES(patient_id),
  patient_name = VALUES(patient_name),
  doctor_id = VALUES(doctor_id),
  doctor_name = VALUES(doctor_name),
  visit_time = VALUES(visit_time),
  chief_complaint = VALUES(chief_complaint),
  present_illness = VALUES(present_illness),
  past_history = VALUES(past_history),
  diagnosis = VALUES(diagnosis),
  treatment_advice = VALUES(treatment_advice),
  file_url = VALUES(file_url),
  archive_status = VALUES(archive_status);

INSERT INTO binglimoban (id, template_name, template_type, content, creator_id, creator_name, status)
VALUES
  (1, '门诊首诊模板', '门诊', '主诉：\n现病史：\n诊断：\n处理意见：', 2, '王医生', 1),
  (2, '高血压随访模板', '慢病', '血压：\n用药依从性：\n生活方式建议：', 2, '王医生', 1)
ON DUPLICATE KEY UPDATE
  template_name = VALUES(template_name),
  template_type = VALUES(template_type),
  content = VALUES(content),
  creator_id = VALUES(creator_id),
  creator_name = VALUES(creator_name),
  status = VALUES(status);

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

INSERT INTO yizhuxinxi (id, order_no, record_id, patient_id, patient_name, doctor_id, doctor_name, order_type, content, status, audit_opinion)
VALUES
  (1, 'YZ202606220001', 1, 1, '患者演示', 2, '王医生', '长期医嘱', '每日监测血压，低盐饮食。', 'approved', '同意执行')
ON DUPLICATE KEY UPDATE
  record_id = VALUES(record_id),
  patient_id = VALUES(patient_id),
  patient_name = VALUES(patient_name),
  doctor_id = VALUES(doctor_id),
  doctor_name = VALUES(doctor_name),
  order_type = VALUES(order_type),
  content = VALUES(content),
  status = VALUES(status),
  audit_opinion = VALUES(audit_opinion);

INSERT INTO kaifang (id, prescription_no, record_id, patient_id, patient_name, doctor_id, doctor_name, medicine_name, quantity, usage_text, remark, status)
VALUES
  (1, 'CF202606220001', 1, 1, '患者演示', 2, '王医生', '硝苯地平控释片', '1盒', '每日一次，每次一片', '高血压常规用药演示', 'created')
ON DUPLICATE KEY UPDATE
  record_id = VALUES(record_id),
  patient_id = VALUES(patient_id),
  patient_name = VALUES(patient_name),
  doctor_id = VALUES(doctor_id),
  doctor_name = VALUES(doctor_name),
  medicine_name = VALUES(medicine_name),
  quantity = VALUES(quantity),
  usage_text = VALUES(usage_text),
  remark = VALUES(remark),
  status = VALUES(status);

INSERT INTO jianchaxiang (id, test_no, record_id, patient_id, patient_name, doctor_id, doctor_name, test_item, test_reason, status, audit_opinion, result_content)
VALUES
  (1, 'JC202606220001', 1, 1, '患者演示', 2, '王医生', '血常规', '评估基础指标', 'approved', '同意检查', NULL)
ON DUPLICATE KEY UPDATE
  record_id = VALUES(record_id),
  patient_id = VALUES(patient_id),
  patient_name = VALUES(patient_name),
  doctor_id = VALUES(doctor_id),
  doctor_name = VALUES(doctor_name),
  test_item = VALUES(test_item),
  test_reason = VALUES(test_reason),
  status = VALUES(status),
  audit_opinion = VALUES(audit_opinion),
  result_content = VALUES(result_content);

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

INSERT INTO shenherenwu (id, task_no, business_type, business_id, business_no, applicant_id, applicant_name, assignee_role, status)
VALUES
  (1, 'SH202606220001', 'medical_order', 1, 'YZ202606220001', 2, '王医生', 'director', 'approved')
ON DUPLICATE KEY UPDATE
  business_type = VALUES(business_type),
  business_id = VALUES(business_id),
  business_no = VALUES(business_no),
  applicant_id = VALUES(applicant_id),
  applicant_name = VALUES(applicant_name),
  assignee_role = VALUES(assignee_role),
  status = VALUES(status);

USE emr_billing;

CREATE TABLE IF NOT EXISTS feiyong (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  fee_no VARCHAR(32) NOT NULL COMMENT '费用编号',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  business_type VARCHAR(64) DEFAULT NULL COMMENT '费用来源类型',
  business_id BIGINT DEFAULT NULL COMMENT '费用来源业务ID',
  fee_item_code VARCHAR(64) DEFAULT NULL COMMENT '费用项目编码',
  fee_item VARCHAR(128) NOT NULL COMMENT '费用项目',
  amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '费用金额',
  pay_status VARCHAR(32) NOT NULL DEFAULT 'unpaid' COMMENT '支付状态: unpaid/paid/refunded',
  pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_feiyong_no (fee_no),
  KEY idx_feiyong_patient_status (patient_id, pay_status),
  KEY idx_feiyong_item_code (fee_item_code),
  KEY idx_feiyong_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用记录表';

CREATE TABLE IF NOT EXISTS fee_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  item_code VARCHAR(64) NOT NULL COMMENT '费用项目编码',
  item_name VARCHAR(128) NOT NULL COMMENT '费用项目名称',
  amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '标准金额',
  item_category VARCHAR(64) DEFAULT NULL COMMENT '项目分类',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_fee_items_code (item_code),
  KEY idx_fee_items_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用项目配置表';

INSERT INTO fee_items (id, item_code, item_name, amount, item_category, enabled, sort_order, remark)
VALUES
  (1, 'registration', '挂号费', 30.00, '门诊', 1, 10, '门诊基础挂号费用'),
  (2, 'consultation', '诊疗费', 20.00, '门诊', 1, 20, '医生诊疗服务费用'),
  (3, 'lab_test', '检验费', 58.50, '检查检验', 1, 30, '常规检验项目默认费用'),
  (4, 'imaging', '影像检查费', 120.00, '检查检验', 1, 40, '常规影像检查默认费用'),
  (5, 'medicine', '药费', 88.00, '药品', 1, 50, '处方药品默认演示费用'),
  (6, 'bed', '床位费', 120.00, '住院', 1, 60, '住院床位默认日费用')
ON DUPLICATE KEY UPDATE
  item_code = VALUES(item_code),
  item_name = VALUES(item_name),
  amount = VALUES(amount),
  item_category = VALUES(item_category),
  enabled = VALUES(enabled),
  sort_order = VALUES(sort_order),
  remark = VALUES(remark);

INSERT INTO feiyong (id, fee_no, patient_id, patient_name, business_type, business_id, fee_item_code, fee_item, amount, pay_status, pay_time, remark)
VALUES
  (1, 'FEE202606220001', 1, '患者演示', 'appointment', 1, 'registration', '挂号费', 30.00, 'paid', '2026-06-22 09:05:00', '预约挂号演示费用'),
  (2, 'FEE202606220002', 1, '患者演示', 'test_request', 1, 'lab_test', '检验费', 58.50, 'unpaid', NULL, '血常规检查费用')
ON DUPLICATE KEY UPDATE
  patient_id = VALUES(patient_id),
  patient_name = VALUES(patient_name),
  business_type = VALUES(business_type),
  business_id = VALUES(business_id),
  fee_item_code = VALUES(fee_item_code),
  fee_item = VALUES(fee_item),
  amount = VALUES(amount),
  pay_status = VALUES(pay_status),
  pay_time = VALUES(pay_time),
  remark = VALUES(remark);

USE emr_system;

CREATE TABLE IF NOT EXISTS news (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(128) NOT NULL COMMENT '资讯标题',
  category VARCHAR(64) DEFAULT NULL COMMENT '资讯分类',
  cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面图',
  summary VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  content LONGTEXT NOT NULL COMMENT '资讯内容',
  publish_status VARCHAR(32) DEFAULT NULL COMMENT '发布状态',
  publisher_id BIGINT DEFAULT NULL COMMENT '发布人ID',
  publisher_name VARCHAR(64) DEFAULT NULL COMMENT '发布人姓名',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1发布 0下架',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_news_status_time (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历资讯表';

CREATE TABLE IF NOT EXISTS messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '留言用户ID',
  username VARCHAR(64) NOT NULL COMMENT '留言用户账号',
  role_code VARCHAR(32) NOT NULL COMMENT '留言用户角色',
  title VARCHAR(128) DEFAULT NULL COMMENT '留言标题',
  content TEXT NOT NULL COMMENT '留言内容',
  image_url VARCHAR(500) DEFAULT NULL COMMENT '留言图片',
  reply_content TEXT COMMENT '回复内容',
  reply_image_url VARCHAR(500) DEFAULT NULL COMMENT '回复图片',
  reply_user_id BIGINT DEFAULT NULL COMMENT '回复人ID',
  reply_user_name VARCHAR(64) DEFAULT NULL COMMENT '回复人姓名',
  reply_time DATETIME DEFAULT NULL COMMENT '回复时间',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_messages_user (user_id),
  KEY idx_messages_status_time (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='留言板表';

CREATE TABLE IF NOT EXISTS carousels (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(128) NOT NULL COMMENT '轮播图标题',
  image_url VARCHAR(500) NOT NULL COMMENT '轮播图图片',
  link_url VARCHAR(500) DEFAULT NULL COMMENT '跳转链接',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_carousels_status_sort (status, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮播图表';

CREATE TABLE IF NOT EXISTS menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(64) NOT NULL COMMENT '菜单名称',
  role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
  menujson LONGTEXT NOT NULL COMMENT '菜单JSON',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_menu_role_name (role_code, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单配置表';

CREATE TABLE IF NOT EXISTS config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  config_key VARCHAR(128) NOT NULL COMMENT '配置键',
  config_value TEXT COMMENT '配置值',
  description VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS syslog (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
  username VARCHAR(64) DEFAULT NULL COMMENT '操作用户账号',
  role_code VARCHAR(32) DEFAULT NULL COMMENT '操作用户角色',
  operation VARCHAR(128) NOT NULL COMMENT '操作名称',
  request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
  request_uri VARCHAR(255) DEFAULT NULL COMMENT '请求地址',
  request_params TEXT COMMENT '请求参数',
  cost_millis BIGINT DEFAULT NULL COMMENT '执行耗时毫秒',
  ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_syslog_user_time (user_id, created_at),
  KEY idx_syslog_operation_time (operation, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';

INSERT INTO config (id, config_key, config_value, description)
VALUES
  (1, 'hospital_name', '安心医疗', '系统展示的医院名称'),
  (2, 'support_phone', '400-000-0000', '客服电话')
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  description = VALUES(description);

INSERT INTO news (id, title, category, cover_url, summary, content, publish_status, publisher_id, publisher_name, status)
VALUES
  (1, '高血压随访提醒', '慢病管理', '/uploads/banner-emr.png', '规律监测血压，按医嘱服药。', '规律监测血压，按医嘱服药，按时复诊。', 'published', 1, '系统管理员', 1)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  category = VALUES(category),
  cover_url = VALUES(cover_url),
  summary = VALUES(summary),
  content = VALUES(content),
  publish_status = VALUES(publish_status),
  publisher_id = VALUES(publisher_id),
  publisher_name = VALUES(publisher_name),
  status = VALUES(status);

INSERT INTO carousels (id, title, image_url, link_url, sort_no, status)
VALUES
  (1, '安心医疗服务', '/uploads/banner-emr.png', '/news/1', 1, 1)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  image_url = VALUES(image_url),
  link_url = VALUES(link_url),
  sort_no = VALUES(sort_no),
  status = VALUES(status);

INSERT INTO messages (id, user_id, username, role_code, title, content, image_url, reply_content, reply_image_url, reply_user_id, reply_user_name, reply_time, status)
VALUES
  (1, 1, 'patient_demo', 'patient', '复诊咨询', '请问高血压复诊需要空腹吗？', NULL, '建议携带近期血压记录，是否空腹以检查项目为准。', NULL, 2, '王医生', '2026-06-22 16:00:00', 1)
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  username = VALUES(username),
  role_code = VALUES(role_code),
  title = VALUES(title),
  content = VALUES(content),
  image_url = VALUES(image_url),
  reply_content = VALUES(reply_content),
  reply_image_url = VALUES(reply_image_url),
  reply_user_id = VALUES(reply_user_id),
  reply_user_name = VALUES(reply_user_name),
  reply_time = VALUES(reply_time),
  status = VALUES(status);

USE emr_ai;

CREATE TABLE IF NOT EXISTS ai_request_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  request_no VARCHAR(32) NOT NULL COMMENT 'AI请求编号',
  ai_type VARCHAR(64) NOT NULL COMMENT 'AI能力类型: ocr/recommend_medicine/prescription_audit/smart_search',
  user_id BIGINT DEFAULT NULL COMMENT '调用用户ID',
  username VARCHAR(64) DEFAULT NULL COMMENT '调用用户账号',
  input_summary VARCHAR(500) DEFAULT NULL COMMENT '输入摘要',
  result_summary VARCHAR(500) DEFAULT NULL COMMENT '结果摘要',
  provider VARCHAR(64) NOT NULL DEFAULT 'baidu' COMMENT 'AI提供方',
  status VARCHAR(32) NOT NULL DEFAULT 'success' COMMENT '状态: success/failed',
  error_message VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_ai_request_log_no (request_no),
  KEY idx_ai_request_log_type_time (ai_type, created_at),
  KEY idx_ai_request_log_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI调用记录表';

INSERT INTO ai_request_log (id, request_no, ai_type, user_id, username, input_summary, result_summary, provider, status, error_message)
VALUES
  (1, 'AI202606220001', 'recommend_medicine', 2, 'doctor_heart', '诊断：高血压', '建议结合血压水平选择降压药，演示结果仅供参考。', 'baidu', 'success', NULL)
ON DUPLICATE KEY UPDATE
  ai_type = VALUES(ai_type),
  user_id = VALUES(user_id),
  username = VALUES(username),
  input_summary = VALUES(input_summary),
  result_summary = VALUES(result_summary),
  provider = VALUES(provider),
  status = VALUES(status),
  error_message = VALUES(error_message);
