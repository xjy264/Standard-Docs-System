SET NAMES utf8mb4;

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_dept'
    AND COLUMN_NAME = 'dept_type'
);
SET @sql := IF(@column_exists = 0,
  'ALTER TABLE sys_dept ADD COLUMN dept_type VARCHAR(32) NOT NULL DEFAULT ''SECTION'' AFTER dept_code',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_dept
SET dept_type = 'AGENCY'
WHERE deleted = 0
  AND (dept_name = '机关' OR parent_id = 0 AND dept_name NOT LIKE '%车间');

UPDATE sys_dept
SET dept_type = 'WORKSHOP'
WHERE deleted = 0
  AND dept_name LIKE '%车间';

UPDATE sys_dept child
JOIN sys_dept parent ON child.parent_id = parent.id
SET child.dept_type = 'SECTION'
WHERE child.deleted = 0
  AND parent.deleted = 0
  AND parent.dept_type = 'AGENCY'
  AND child.dept_name NOT LIKE '%车间';

CREATE TABLE IF NOT EXISTS sys_doc_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  section_dept_id BIGINT NOT NULL,
  category_name VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  INDEX idx_doc_category_section (section_dept_id, deleted, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_doc_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  content_html MEDIUMTEXT,
  attachment_enabled TINYINT(1) NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  INDEX idx_doc_item_category (category_id, deleted, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @doc_item_content_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'content_html'
);
SET @sql := IF(@doc_item_content_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN content_html MEDIUMTEXT AFTER item_name',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_item_attachment_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'attachment_enabled'
);
SET @sql := IF(@doc_item_attachment_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN attachment_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER content_html',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_doc_submission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  section_dept_id BIGINT NOT NULL,
  workshop_dept_id BIGINT NOT NULL,
  upload_user_id BIGINT NOT NULL,
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_doc_submission_category (category_id, submitted_at),
  INDEX idx_doc_submission_workshop (workshop_dept_id, submitted_at),
  INDEX idx_doc_submission_item (item_id, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_doc_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  extension VARCHAR(32),
  mime_type VARCHAR(128),
  file_size BIGINT NOT NULL DEFAULT 0,
  storage_bucket VARCHAR(128) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  uploaded_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_doc_attachment_submission (submission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
