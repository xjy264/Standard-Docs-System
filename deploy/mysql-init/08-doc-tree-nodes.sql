SET NAMES utf8mb4;

SET @item_file_type_column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'file_type'
);
SET @sql := IF(@item_file_type_column_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN file_type VARCHAR(32) NULL AFTER item_name',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @item_section_column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'section_dept_id'
);
SET @sql := IF(@item_section_column_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN section_dept_id BIGINT NULL AFTER category_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_doc_item item
JOIN sys_doc_category category
  ON category.id = item.category_id
SET item.section_dept_id = category.section_dept_id
WHERE item.section_dept_id IS NULL;

SET @item_category_nullable := (
  SELECT IS_NULLABLE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'category_id'
);
SET @sql := IF(@item_category_nullable = 'NO',
  'ALTER TABLE sys_doc_item MODIFY COLUMN category_id BIGINT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @submission_category_nullable := (
  SELECT IS_NULLABLE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_submission'
    AND COLUMN_NAME = 'category_id'
);
SET @sql := IF(@submission_category_nullable = 'NO',
  'ALTER TABLE sys_doc_submission MODIFY COLUMN category_id BIGINT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_doc_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  section_dept_id BIGINT NOT NULL,
  parent_id BIGINT NULL,
  node_type VARCHAR(16) NOT NULL,
  node_name VARCHAR(128) NOT NULL,
  item_id BIGINT NULL,
  doc_year INT NOT NULL DEFAULT 2026,
  sort_order INT NOT NULL DEFAULT 0,
  level INT NOT NULL DEFAULT 1,
  show_upload_progress TINYINT(1) NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  INDEX idx_doc_node_section (section_dept_id, deleted, sort_order),
  INDEX idx_doc_node_parent (parent_id, deleted, sort_order),
  INDEX idx_doc_node_item (item_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_doc_node (
  section_dept_id,
  parent_id,
  node_type,
  node_name,
  item_id,
  doc_year,
  sort_order,
  level,
  created_by,
  created_at,
  updated_at,
  deleted
)
SELECT
  category.section_dept_id,
  NULL,
  'FOLDER',
  category.category_name,
  NULL,
  2026,
  category.sort_order,
  1,
  category.created_by,
  category.created_at,
  category.updated_at,
  0
FROM sys_doc_category category
WHERE category.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_doc_node existing
    WHERE existing.section_dept_id = category.section_dept_id
      AND existing.parent_id IS NULL
      AND existing.node_type = 'FOLDER'
      AND existing.node_name = category.category_name
      AND existing.deleted = 0
  );

INSERT INTO sys_doc_node (
  section_dept_id,
  parent_id,
  node_type,
  node_name,
  item_id,
  doc_year,
  sort_order,
  level,
  created_by,
  created_at,
  updated_at,
  deleted
)
SELECT
  category.section_dept_id,
  folder.id,
  'FILE',
  item.item_name,
  item.id,
  item.doc_year,
  item.sort_order,
  2,
  item.created_by,
  item.created_at,
  item.updated_at,
  0
FROM sys_doc_item item
JOIN sys_doc_category category
  ON category.id = item.category_id
 AND category.deleted = 0
JOIN sys_doc_node folder
  ON folder.section_dept_id = category.section_dept_id
 AND folder.parent_id IS NULL
 AND folder.node_type = 'FOLDER'
 AND folder.node_name = category.category_name
 AND folder.deleted = 0
WHERE item.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_doc_node existing
    WHERE existing.item_id = item.id
      AND existing.node_type = 'FILE'
      AND existing.deleted = 0
  );
