SET NAMES utf8mb4;

DROP TABLE IF EXISTS sys_recycle_bin;
DROP TABLE IF EXISTS sys_file_access_record;
DROP TABLE IF EXISTS sys_file_tag_rel;
DROP TABLE IF EXISTS sys_file_tag;
DROP TABLE IF EXISTS sys_file_favorite;
DROP TABLE IF EXISTS sys_file_copy;
DROP TABLE IF EXISTS sys_file_version;
DROP TABLE IF EXISTS sys_file_permission;
DROP TABLE IF EXISTS sys_folder;
DROP TABLE IF EXISTS sys_file;
DROP TABLE IF EXISTS sys_doc_submission_value;
DROP TABLE IF EXISTS sys_doc_field;
DROP TABLE IF EXISTS sys_storage_stat;

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_category'
    AND COLUMN_NAME = 'status'
);
SET @sql := IF(@column_exists > 0,
  'ALTER TABLE sys_doc_category DROP COLUMN status',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'collect_enabled'
);
SET @sql := IF(@column_exists > 0,
  'ALTER TABLE sys_doc_item DROP COLUMN collect_enabled',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'attachment_required'
);
SET @sql := IF(@column_exists > 0,
  'ALTER TABLE sys_doc_item DROP COLUMN attachment_required',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'status'
);
SET @sql := IF(@column_exists > 0,
  'ALTER TABLE sys_doc_item DROP COLUMN status',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_submission'
    AND INDEX_NAME = 'idx_doc_submission_item'
);
SET @sql := IF(@index_exists = 0,
  'CREATE INDEX idx_doc_submission_item ON sys_doc_submission (item_id, submitted_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DELETE FROM sys_role_permission
WHERE permission_code LIKE 'file:%';

DELETE FROM sys_user_permission
WHERE permission_code LIKE 'file:%';

DELETE FROM sys_permission
WHERE permission_code LIKE 'file:%';

DELETE FROM sys_system_config
WHERE config_key IN (
  'onlyoffice.enabled',
  'onlyoffice.url',
  'cad.preview.enabled',
  'recycle.retention.days'
);
