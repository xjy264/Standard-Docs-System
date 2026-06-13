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
  original_file_name VARCHAR(255) NULL,
  extension VARCHAR(32) NULL,
  mime_type VARCHAR(128) NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  storage_bucket VARCHAR(128) NULL,
  storage_path VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  INDEX idx_repair_template_item_template (template_id, deleted, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @template_original_file_name_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_repair_project_template_item'
    AND COLUMN_NAME = 'original_file_name'
);
SET @sql := IF(@template_original_file_name_exists = 0,
  'ALTER TABLE sys_repair_project_template_item ADD COLUMN original_file_name VARCHAR(255) NULL AFTER file_type',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @template_extension_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_repair_project_template_item'
    AND COLUMN_NAME = 'extension'
);
SET @sql := IF(@template_extension_exists = 0,
  'ALTER TABLE sys_repair_project_template_item ADD COLUMN extension VARCHAR(32) NULL AFTER original_file_name',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @template_mime_type_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_repair_project_template_item'
    AND COLUMN_NAME = 'mime_type'
);
SET @sql := IF(@template_mime_type_exists = 0,
  'ALTER TABLE sys_repair_project_template_item ADD COLUMN mime_type VARCHAR(128) NULL AFTER extension',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @template_file_size_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_repair_project_template_item'
    AND COLUMN_NAME = 'file_size'
);
SET @sql := IF(@template_file_size_exists = 0,
  'ALTER TABLE sys_repair_project_template_item ADD COLUMN file_size BIGINT NOT NULL DEFAULT 0 AFTER mime_type',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @template_storage_bucket_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_repair_project_template_item'
    AND COLUMN_NAME = 'storage_bucket'
);
SET @sql := IF(@template_storage_bucket_exists = 0,
  'ALTER TABLE sys_repair_project_template_item ADD COLUMN storage_bucket VARCHAR(128) NULL AFTER file_size',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @template_storage_path_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_repair_project_template_item'
    AND COLUMN_NAME = 'storage_path'
);
SET @sql := IF(@template_storage_path_exists = 0,
  'ALTER TABLE sys_repair_project_template_item ADD COLUMN storage_path VARCHAR(500) NULL AFTER storage_bucket',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
