CREATE TABLE IF NOT EXISTS binglixinxi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_no VARCHAR(32) NOT NULL,
  appointment_id BIGINT,
  appointment_no VARCHAR(32),
  patient_id BIGINT NOT NULL,
  patient_name VARCHAR(64) NOT NULL,
  doctor_id BIGINT NOT NULL,
  doctor_name VARCHAR(64) NOT NULL,
  visit_time TIMESTAMP NOT NULL,
  chief_complaint VARCHAR(500),
  present_illness CLOB,
  past_history CLOB,
  diagnosis CLOB,
  treatment_advice CLOB,
  file_url VARCHAR(500),
  archive_status VARCHAR(32) NOT NULL DEFAULT 'not_submitted',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS binglimoban (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_name VARCHAR(128) NOT NULL,
  template_type VARCHAR(64),
  content CLOB NOT NULL,
  creator_id BIGINT,
  creator_name VARCHAR(64),
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bingliguidangshenqing (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_no VARCHAR(32) NOT NULL,
  record_id BIGINT NOT NULL,
  record_no VARCHAR(32) NOT NULL,
  patient_id BIGINT NOT NULL,
  patient_name VARCHAR(64) NOT NULL,
  doctor_id BIGINT NOT NULL,
  doctor_name VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  audit_user_id BIGINT,
  audit_user_name VARCHAR(64),
  audit_opinion VARCHAR(500),
  audit_time TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bingliguidang (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  archive_no VARCHAR(32) NOT NULL,
  application_id BIGINT NOT NULL,
  record_id BIGINT NOT NULL,
  record_no VARCHAR(32) NOT NULL,
  patient_id BIGINT NOT NULL,
  patient_name VARCHAR(64) NOT NULL,
  archive_content CLOB,
  archived_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
