SET NAMES utf8mb4;

SET @doc_node_module_type_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND COLUMN_NAME = 'module_type'
);
SET @sql := IF(@doc_node_module_type_exists = 0,
  'ALTER TABLE sys_doc_node ADD COLUMN module_type VARCHAR(32) NOT NULL DEFAULT ''INTERNAL'' AFTER section_dept_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_item_module_type_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'module_type'
);
SET @sql := IF(@doc_item_module_type_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN module_type VARCHAR(32) NOT NULL DEFAULT ''INTERNAL'' AFTER section_dept_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_node_workshop_upload_enabled_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND COLUMN_NAME = 'workshop_upload_enabled'
);
SET @sql := IF(@doc_node_workshop_upload_enabled_exists = 0,
  'ALTER TABLE sys_doc_node ADD COLUMN workshop_upload_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER show_upload_progress',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_node_workshop_dept_id_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND COLUMN_NAME = 'workshop_dept_id'
);
SET @sql := IF(@doc_node_workshop_dept_id_exists = 0,
  'ALTER TABLE sys_doc_node ADD COLUMN workshop_dept_id BIGINT NULL AFTER workshop_upload_enabled',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_doc_node
SET module_type = 'INTERNAL'
WHERE module_type IS NULL OR module_type = '';

UPDATE sys_doc_item
SET module_type = 'INTERNAL'
WHERE module_type IS NULL OR module_type = '';

CREATE TABLE IF NOT EXISTS sys_doc_node_workshop_scope (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  node_id BIGINT NOT NULL,
  workshop_dept_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_doc_node_workshop_scope (node_id, workshop_dept_id),
  INDEX idx_doc_node_workshop_scope_workshop (workshop_dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @doc_node_module_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND INDEX_NAME = 'idx_doc_node_module'
);
SET @sql := IF(@doc_node_module_index_exists = 0,
  'CREATE INDEX idx_doc_node_module ON sys_doc_node (section_dept_id, module_type, deleted, sort_order)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_node_workshop_folder_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND INDEX_NAME = 'idx_doc_node_workshop_folder'
);
SET @sql := IF(@doc_node_workshop_folder_index_exists = 0,
  'CREATE INDEX idx_doc_node_workshop_folder ON sys_doc_node (parent_id, workshop_dept_id, deleted)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_item_module_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND INDEX_NAME = 'idx_doc_item_module'
);
SET @sql := IF(@doc_item_module_index_exists = 0,
  'CREATE INDEX idx_doc_item_module ON sys_doc_item (section_dept_id, module_type, business_type, deleted, sort_order)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
