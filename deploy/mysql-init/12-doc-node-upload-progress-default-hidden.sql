SET NAMES utf8mb4;

SET @doc_node_show_upload_progress_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND COLUMN_NAME = 'show_upload_progress'
);
SET @sql := IF(@doc_node_show_upload_progress_exists = 0,
  'ALTER TABLE sys_doc_node ADD COLUMN show_upload_progress TINYINT(1) NOT NULL DEFAULT 0 AFTER level',
  'ALTER TABLE sys_doc_node MODIFY COLUMN show_upload_progress TINYINT(1) NOT NULL DEFAULT 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_doc_node
SET show_upload_progress = 0
WHERE node_type = 'FOLDER';
