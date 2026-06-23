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

