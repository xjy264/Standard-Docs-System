SET NAMES utf8mb4;

SET @doc_submission_deleted_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_submission'
    AND COLUMN_NAME = 'deleted'
);
SET @sql := IF(@doc_submission_deleted_exists = 0,
  'ALTER TABLE sys_doc_submission ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER submitted_at',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_submission_deleted_at_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_submission'
    AND COLUMN_NAME = 'deleted_at'
);
SET @sql := IF(@doc_submission_deleted_at_exists = 0,
  'ALTER TABLE sys_doc_submission ADD COLUMN deleted_at DATETIME NULL AFTER deleted',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_submission_deleted_by_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_submission'
    AND COLUMN_NAME = 'deleted_by'
);
SET @sql := IF(@doc_submission_deleted_by_exists = 0,
  'ALTER TABLE sys_doc_submission ADD COLUMN deleted_by BIGINT NULL AFTER deleted_at',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_submission_deleted_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_submission'
    AND INDEX_NAME = 'idx_doc_submission_deleted'
);
SET @sql := IF(@doc_submission_deleted_index_exists = 0,
  'CREATE INDEX idx_doc_submission_deleted ON sys_doc_submission (item_id, deleted, submitted_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_attachment_deleted_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_attachment'
    AND COLUMN_NAME = 'deleted'
);
SET @sql := IF(@doc_attachment_deleted_exists = 0,
  'ALTER TABLE sys_doc_attachment ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER created_at',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_attachment_deleted_at_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_attachment'
    AND COLUMN_NAME = 'deleted_at'
);
SET @sql := IF(@doc_attachment_deleted_at_exists = 0,
  'ALTER TABLE sys_doc_attachment ADD COLUMN deleted_at DATETIME NULL AFTER deleted',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_attachment_deleted_by_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_attachment'
    AND COLUMN_NAME = 'deleted_by'
);
SET @sql := IF(@doc_attachment_deleted_by_exists = 0,
  'ALTER TABLE sys_doc_attachment ADD COLUMN deleted_by BIGINT NULL AFTER deleted_at',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_attachment_deleted_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_attachment'
    AND INDEX_NAME = 'idx_doc_attachment_deleted'
);
SET @sql := IF(@doc_attachment_deleted_index_exists = 0,
  'CREATE INDEX idx_doc_attachment_deleted ON sys_doc_attachment (submission_id, deleted, created_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
