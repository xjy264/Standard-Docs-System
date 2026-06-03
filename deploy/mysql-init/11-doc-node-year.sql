SET NAMES utf8mb4;

SET @doc_node_year_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND COLUMN_NAME = 'doc_year'
);
SET @sql := IF(@doc_node_year_exists = 0,
  'ALTER TABLE sys_doc_node ADD COLUMN doc_year INT NOT NULL DEFAULT 2026 AFTER item_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_doc_node node
JOIN sys_doc_item item
  ON item.id = node.item_id
 AND item.deleted = 0
SET node.doc_year = item.doc_year
WHERE node.node_type = 'FILE'
  AND item.doc_year IS NOT NULL;

UPDATE sys_doc_node
SET doc_year = 2026
WHERE node_type = 'FOLDER'
  AND doc_year IS NULL;
