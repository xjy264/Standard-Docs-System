SET NAMES utf8mb4;

SET @workshop_upload_enabled_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'workshop_upload_enabled'
);
SET @sql := IF(@workshop_upload_enabled_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN workshop_upload_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER attachment_enabled',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @upload_deadline_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'upload_deadline'
);
SET @sql := IF(@upload_deadline_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN upload_deadline DATETIME NULL AFTER workshop_upload_enabled',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @visibility_scope_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'visibility_scope'
);
SET @sql := IF(@visibility_scope_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN visibility_scope VARCHAR(32) NOT NULL DEFAULT ''ALL'' AFTER upload_deadline',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_doc_item
SET workshop_upload_enabled = CASE WHEN attachment_enabled = 1 OR business_type = 'UPLOAD' THEN 1 ELSE 0 END,
    visibility_scope = COALESCE(NULLIF(visibility_scope, ''), 'ALL')
WHERE deleted = 0;

CREATE TABLE IF NOT EXISTS sys_doc_item_workshop_scope (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id BIGINT NOT NULL,
  workshop_dept_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_doc_item_workshop_scope (item_id, workshop_dept_id),
  INDEX idx_doc_item_workshop_scope_workshop (workshop_dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_repair_project_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_name VARCHAR(128) NOT NULL,
  section_dept_id BIGINT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  INDEX idx_repair_template_deleted_sort (deleted, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_repair_project_template_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_id BIGINT NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  file_type VARCHAR(32) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  INDEX idx_repair_template_item_template (template_id, deleted, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
