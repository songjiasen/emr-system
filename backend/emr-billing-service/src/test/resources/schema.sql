CREATE TABLE IF NOT EXISTS feiyong (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  fee_no VARCHAR(32) NOT NULL,
  patient_id BIGINT NOT NULL,
  patient_name VARCHAR(64) NOT NULL,
  business_type VARCHAR(64),
  business_id BIGINT,
  fee_item_code VARCHAR(64),
  fee_item VARCHAR(128) NOT NULL,
  amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  pay_status VARCHAR(32) NOT NULL DEFAULT 'unpaid',
  pay_time TIMESTAMP,
  remark VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fee_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  item_category VARCHAR(64),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  remark VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_fee_items_code (item_code)
);

INSERT INTO fee_items (item_code, item_name, amount, item_category, enabled, sort_order, remark)
VALUES ('test_request_check', '检查费', 100.00, '检查检验', TRUE, 35, '检查申请患者端去检查自动生成费用');
