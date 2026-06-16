SET NAMES utf8mb4;

SET @business_type_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'business_type'
);
SET @sql := IF(@business_type_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN business_type VARCHAR(32) NOT NULL DEFAULT ''ISSUED'' AFTER item_name',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @submitter_mode_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'submitter_mode'
);
SET @sql := IF(@submitter_mode_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN submitter_mode VARCHAR(32) NOT NULL DEFAULT ''SINGLE'' AFTER business_type',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_doc_item
SET business_type = CASE WHEN attachment_enabled = 1 THEN 'UPLOAD' ELSE 'ISSUED' END,
    submitter_mode = COALESCE(NULLIF(submitter_mode, ''), 'SINGLE')
WHERE deleted = 0
  AND (business_type IS NULL OR business_type = '' OR business_type = 'ISSUED' AND attachment_enabled = 1);

SET @doc_item_section_type_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND INDEX_NAME = 'idx_doc_item_section_type'
);
SET @sql := IF(@doc_item_section_type_index_exists = 0,
  'CREATE INDEX idx_doc_item_section_type ON sys_doc_item (section_dept_id, business_type, deleted, sort_order)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_doc_upload_requirement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id BIGINT NOT NULL,
  requirement_name VARCHAR(128) NOT NULL,
  description VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  INDEX idx_doc_upload_requirement_item (item_id, deleted, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @upload_requirement_description_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_upload_requirement'
    AND COLUMN_NAME = 'description'
);
SET @sql := IF(@upload_requirement_description_exists = 0,
  'ALTER TABLE sys_doc_upload_requirement ADD COLUMN description VARCHAR(500) NULL AFTER requirement_name',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_doc_upload_requirement (
  item_id,
  requirement_name,
  sort_order,
  created_at,
  updated_at,
  deleted
)
SELECT
  item.id,
  '附件',
  0,
  NOW(),
  NOW(),
  0
FROM sys_doc_item item
WHERE item.deleted = 0
  AND item.business_type = 'UPLOAD'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_doc_upload_requirement existing
    WHERE existing.item_id = item.id
      AND existing.deleted = 0
  );

SET @attachment_requirement_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_attachment'
    AND COLUMN_NAME = 'requirement_id'
);
SET @sql := IF(@attachment_requirement_exists = 0,
  'ALTER TABLE sys_doc_attachment ADD COLUMN requirement_id BIGINT NULL AFTER submission_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_doc_attachment attachment
JOIN sys_doc_submission submission
  ON submission.id = attachment.submission_id
JOIN sys_doc_upload_requirement requirement
  ON requirement.item_id = submission.item_id
 AND requirement.deleted = 0
SET attachment.requirement_id = requirement.id
WHERE attachment.requirement_id IS NULL
  AND requirement.requirement_name = '附件';

SET @attachment_requirement_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_attachment'
    AND INDEX_NAME = 'idx_doc_attachment_requirement'
);
SET @sql := IF(@attachment_requirement_index_exists = 0,
  'CREATE INDEX idx_doc_attachment_requirement ON sys_doc_attachment (requirement_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_doc_item_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id BIGINT NOT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  extension VARCHAR(32),
  mime_type VARCHAR(128),
  file_size BIGINT NOT NULL DEFAULT 0,
  storage_bucket VARCHAR(128) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  uploaded_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME NULL,
  deleted_by BIGINT NULL,
  INDEX idx_doc_item_attachment_item (item_id, created_at),
  INDEX idx_doc_item_attachment_deleted (item_id, deleted, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
