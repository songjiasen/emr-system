CREATE TABLE IF NOT EXISTS yuyueguahao (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  appointment_no VARCHAR(32) NOT NULL,
  patient_id BIGINT NOT NULL,
  patient_name VARCHAR(64) NOT NULL,
  doctor_id BIGINT NOT NULL,
  doctor_name VARCHAR(64) NOT NULL,
  department_id BIGINT,
  department_name VARCHAR(64),
  appointment_time TIMESTAMP NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  cancel_reason VARCHAR(255),
  remark VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fenzhenjiandang (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  triage_no VARCHAR(32) NOT NULL,
  appointment_id BIGINT,
  patient_id BIGINT NOT NULL,
  patient_name VARCHAR(64) NOT NULL,
  nurse_id BIGINT NOT NULL,
  nurse_name VARCHAR(64) NOT NULL,
  chief_complaint VARCHAR(500),
  triage_level VARCHAR(32),
  status VARCHAR(32) NOT NULL DEFAULT 'created',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ruyuanxinxi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admission_no VARCHAR(32) NOT NULL,
  patient_id BIGINT NOT NULL,
  patient_name VARCHAR(64) NOT NULL,
  doctor_id BIGINT,
  doctor_name VARCHAR(64),
  nurse_id BIGINT,
  nurse_name VARCHAR(64),
  ward_no VARCHAR(32),
  bed_no VARCHAR(32),
  admission_time TIMESTAMP NOT NULL,
  reason VARCHAR(500),
  status VARCHAR(32) NOT NULL DEFAULT 'in_hospital',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chuyuanxinxi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  discharge_no VARCHAR(32) NOT NULL,
  admission_id BIGINT NOT NULL,
  patient_id BIGINT NOT NULL,
  patient_name VARCHAR(64) NOT NULL,
  discharge_time TIMESTAMP NOT NULL,
  discharge_reason VARCHAR(500),
  discharge_summary CLOB,
  operator_id BIGINT,
  operator_name VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
