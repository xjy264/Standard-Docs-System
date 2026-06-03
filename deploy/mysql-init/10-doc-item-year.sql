SET NAMES utf8mb4;

SET @doc_item_year_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'doc_year'
);
SET @sql := IF(@doc_item_year_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN doc_year INT NOT NULL DEFAULT 2026 AFTER file_type',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_doc_item
SET doc_year = 2026
WHERE doc_year IS NULL;
