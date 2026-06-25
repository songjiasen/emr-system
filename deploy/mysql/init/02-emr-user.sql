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
