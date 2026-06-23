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

