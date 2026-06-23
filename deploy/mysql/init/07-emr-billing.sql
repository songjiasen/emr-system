USE emr_billing;

CREATE TABLE IF NOT EXISTS feiyong (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  fee_no VARCHAR(32) NOT NULL COMMENT '费用编号',
  patient_id BIGINT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(64) NOT NULL COMMENT '患者姓名',
  business_type VARCHAR(64) DEFAULT NULL COMMENT '费用来源类型',
  business_id BIGINT DEFAULT NULL COMMENT '费用来源业务ID',
  fee_item VARCHAR(128) NOT NULL COMMENT '费用项目',
  amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '费用金额',
  pay_status VARCHAR(32) NOT NULL DEFAULT 'unpaid' COMMENT '支付状态: unpaid/paid/refunded',
  pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_feiyong_no (fee_no),
  KEY idx_feiyong_patient_status (patient_id, pay_status),
  KEY idx_feiyong_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用记录表';

