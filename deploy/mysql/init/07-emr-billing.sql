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

INSERT INTO fee_items (item_code, item_name, amount, item_category, enabled, sort_order, remark)
VALUES
  ('registration', '挂号费', 30.00, '门诊', 1, 10, '门诊基础挂号费用'),
  ('consultation', '诊疗费', 20.00, '门诊', 1, 20, '医生诊疗服务费用'),
  ('lab_test', '检验费', 58.50, '检查检验', 1, 30, '常规检验项目默认费用'),
  ('test_request_check', '检查费', 100.00, '检查检验', 1, 35, '检查申请患者端去检查自动生成费用'),
  ('imaging', '影像检查费', 120.00, '检查检验', 1, 40, '常规影像检查默认费用'),
  ('medicine', '药费', 88.00, '药品', 1, 50, '处方药品默认演示费用'),
  ('bed', '床位费', 120.00, '住院', 1, 60, '住院床位默认日费用')
ON DUPLICATE KEY UPDATE
  item_name = VALUES(item_name),
  amount = VALUES(amount),
  item_category = VALUES(item_category),
  enabled = VALUES(enabled),
  sort_order = VALUES(sort_order),
  remark = VALUES(remark);
