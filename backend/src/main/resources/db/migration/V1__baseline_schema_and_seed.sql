-- Baseline migration generated from deploy/mysql-init/01-14 SQL scripts.

-- Source: deploy/mysql-init/01-schema.sql

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  real_name VARCHAR(64) NOT NULL,
  phone VARCHAR(32),
  dept_id BIGINT,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  approval_status VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
  is_super_admin TINYINT(1) NOT NULL DEFAULT 0,
  last_login_time DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_dept (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NOT NULL DEFAULT 0,
  dept_name VARCHAR(128) NOT NULL,
  dept_code VARCHAR(64) NOT NULL,
  dept_type VARCHAR(32) NOT NULL DEFAULT 'SECTION',
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_name VARCHAR(64) NOT NULL,
  role_code VARCHAR(64) NOT NULL UNIQUE,
  description VARCHAR(255),
  is_system TINYINT(1) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  permission_name VARCHAR(64) NOT NULL,
  permission_code VARCHAR(64) NOT NULL UNIQUE,
  permission_type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
  description VARCHAR(255),
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  permission_code VARCHAR(64) NOT NULL,
  UNIQUE KEY uk_role_permission (role_id, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  permission_code VARCHAR(64) NOT NULL,
  effect VARCHAR(16) NOT NULL,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_permission (user_id, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
  category_id BIGINT NULL,
  section_dept_id BIGINT NULL,
  item_name VARCHAR(128) NOT NULL,
  business_type VARCHAR(32) NOT NULL DEFAULT 'ISSUED',
  submitter_mode VARCHAR(32) NOT NULL DEFAULT 'SINGLE',
  file_type VARCHAR(32) NULL,
  doc_year INT NOT NULL DEFAULT 2026,
  content_html MEDIUMTEXT,
  attachment_enabled TINYINT(1) NOT NULL DEFAULT 0,
  workshop_upload_enabled TINYINT(1) NOT NULL DEFAULT 0,
  upload_deadline DATETIME NULL,
  visibility_scope VARCHAR(32) NOT NULL DEFAULT 'ALL',
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  INDEX idx_doc_item_category (category_id, deleted, sort_order),
  INDEX idx_doc_item_section_type (section_dept_id, business_type, deleted, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_doc_item_workshop_scope (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id BIGINT NOT NULL,
  workshop_dept_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_doc_item_workshop_scope (item_id, workshop_dept_id),
  INDEX idx_doc_item_workshop_scope_workshop (workshop_dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS sys_doc_submission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id BIGINT NOT NULL,
  category_id BIGINT NULL,
  section_dept_id BIGINT NOT NULL,
  workshop_dept_id BIGINT NULL,
  submitter_dept_id BIGINT NULL,
  upload_user_id BIGINT NOT NULL,
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_doc_submission_category (category_id, submitted_at),
  INDEX idx_doc_submission_workshop (workshop_dept_id, submitted_at),
  INDEX idx_doc_submission_submitter (submitter_dept_id, submitted_at),
  INDEX idx_doc_submission_item (item_id, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_doc_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  requirement_id BIGINT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  extension VARCHAR(32),
  mime_type VARCHAR(128),
  file_size BIGINT NOT NULL DEFAULT 0,
  storage_bucket VARCHAR(128) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  uploaded_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_doc_attachment_submission (submission_id),
  INDEX idx_doc_attachment_requirement (requirement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
  INDEX idx_doc_item_attachment_deleted (item_id, deleted, created_at),
  INDEX idx_doc_item_attachment_cleanup (deleted, deleted_at)
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

CREATE TABLE IF NOT EXISTS sys_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(500) NOT NULL,
  biz_type VARCHAR(64),
  biz_id BIGINT,
  read_status VARCHAR(16) NOT NULL DEFAULT 'UNREAD',
  read_time DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notification_user (user_id, read_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT,
  operation_type VARCHAR(64) NOT NULL,
  object_type VARCHAR(64),
  object_id BIGINT,
  ip_address VARCHAR(64),
  user_agent VARCHAR(500),
  result VARCHAR(32) NOT NULL,
  fail_reason VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_login_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64),
  user_id BIGINT,
  ip_address VARCHAR(64),
  user_agent VARCHAR(500),
  result VARCHAR(32) NOT NULL,
  fail_reason VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_register_approval (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  approval_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  approver_id BIGINT,
  reject_reason VARCHAR(500),
  approved_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_system_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(128) NOT NULL UNIQUE,
  config_value VARCHAR(1000),
  description VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Source: deploy/mysql-init/02-init-data.sql

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, deleted)
VALUES
  (1, 0, '机关', 'AGENCY', 'AGENCY', 1, 'ENABLED', 0),
  (2, 1, '办公室', 'OFFICE', 'SECTION', 10, 'ENABLED', 0),
  (3, 1, '技术科', 'TECH', 'SECTION', 20, 'ENABLED', 0),
  (4, 1, '安全科', 'SAFETY', 'SECTION', 30, 'ENABLED', 0),
  (5, 0, '房建车间', 'HOUSE_WORKSHOP', 'WORKSHOP', 40, 'ENABLED', 0),
  (6, 0, '公寓车间', 'APARTMENT_WORKSHOP', 'WORKSHOP', 50, 'ENABLED', 0)
ON DUPLICATE KEY UPDATE dept_name = VALUES(dept_name), dept_type = VALUES(dept_type);

INSERT INTO sys_permission (permission_name, permission_code, permission_type, description, sort_order, status)
VALUES
  ('用户查看', 'user:view', 'SYSTEM', '允许查看用户', 60, 'ENABLED'),
  ('用户管理', 'user:manage', 'SYSTEM', '允许管理用户', 70, 'ENABLED'),
  ('注册审批', 'user:approve', 'SYSTEM', '允许审批注册用户', 80, 'ENABLED'),
  ('组织查看', 'dept:view', 'SYSTEM', '允许查看组织', 90, 'ENABLED'),
  ('组织管理', 'dept:manage', 'SYSTEM', '允许管理组织树', 100, 'ENABLED'),
  ('角色查看', 'role:view', 'SYSTEM', '允许查看角色模板', 110, 'ENABLED'),
  ('角色权限管理', 'role:manage', 'SYSTEM', '允许管理角色和权限矩阵', 120, 'ENABLED'),
  ('操作日志查看', 'log:view', 'SYSTEM', '允许查看操作日志', 130, 'ENABLED'),
  ('存储查看', 'storage:view', 'SYSTEM', '允许查看存储统计', 140, 'ENABLED'),
  ('存储管理', 'storage:manage', 'SYSTEM', '允许管理存储配置', 150, 'ENABLED'),
  ('系统配置管理', 'system:manage', 'SYSTEM', '允许管理系统配置', 160, 'ENABLED')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT INTO sys_role (id, role_name, role_code, description, is_system, status, deleted)
VALUES
  (1, '超级管理员', 'SUPER_ADMIN', '拥有全部系统权限', 1, 'ENABLED', 0),
  (2, '管理员', 'SEGMENT_ADMIN', '负责所属一级组织及下级组织注册审核', 1, 'ENABLED', 0),
  (3, '部门管理员', 'DEPT_ADMIN', '负责本部门资料管理', 1, 'ENABLED', 0),
  (4, '资料员', 'DOC_MANAGER', '负责资料上传和整理', 1, 'ENABLED', 0),
  (5, '普通用户', 'STAFF', '普通资料访问用户', 1, 'ENABLED', 0),
  (6, '只读用户', 'READONLY', '仅用于访问已授权资料', 1, 'ENABLED', 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT 1, permission_code FROM sys_permission
ON DUPLICATE KEY UPDATE permission_code = VALUES(permission_code);

INSERT INTO sys_role_permission (role_id, permission_code)
VALUES
  (2, 'user:view'), (2, 'user:approve'), (2, 'dept:view'),
  (3, 'user:view'), (3, 'dept:view'), (3, 'role:view'), (3, 'storage:view'),
  (4, 'dept:view'),
  (5, 'dept:view')
ON DUPLICATE KEY UPDATE permission_code = VALUES(permission_code);

INSERT INTO sys_user (id, username, password, real_name, phone, dept_id, status, approval_status, is_super_admin, deleted)
VALUES (1, 'admin', '$2y$10$JLYTEoDd2O7bkkA9W176He7tuLuAMKNQ4baclBgz02t4mD8FO3joW', '系统管理员', '00000000000', 1, 'ENABLED', 'APPROVED', 1, 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name);

INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_system_config (config_key, config_value, description)
VALUES
  ('storage.provider', 'minio', '默认文件存储服务')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), description = VALUES(description);

-- Source: deploy/mysql-init/03-org-agency-migration.sql

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
SET dept_name = '机关',
    dept_code = 'AGENCY',
    dept_type = 'AGENCY',
    parent_id = 0,
    sort_order = 1
WHERE dept_name = '大同房建公寓段'
  AND deleted = 0;

SET @agency_id := (
  SELECT id FROM sys_dept
  WHERE dept_name = '机关'
    AND deleted = 0
  ORDER BY id
  LIMIT 1
);

UPDATE sys_dept
SET parent_id = @agency_id,
    dept_type = 'SECTION'
WHERE @agency_id IS NOT NULL
  AND dept_name IN ('办公室', '技术科', '安全科', '计财科')
  AND deleted = 0;

UPDATE sys_dept
SET parent_id = 0,
    dept_type = 'WORKSHOP'
WHERE dept_name LIKE '%车间'
  AND deleted = 0;

-- Source: deploy/mysql-init/04-fix-seed-charset.sql

UPDATE sys_dept
SET dept_name = CASE id
  WHEN 1 THEN '机关'
  WHEN 2 THEN '办公室'
  WHEN 3 THEN '技术科'
  WHEN 4 THEN '安全科'
  WHEN 5 THEN '房建车间'
  WHEN 6 THEN '公寓车间'
  ELSE dept_name
END
WHERE id IN (1, 2, 3, 4, 5, 6);

UPDATE sys_permission
SET permission_name = CASE permission_code
  WHEN 'user:view' THEN '用户查看'
  WHEN 'user:manage' THEN '用户管理'
  WHEN 'user:approve' THEN '注册审批'
  WHEN 'dept:view' THEN '组织查看'
  WHEN 'dept:manage' THEN '组织管理'
  WHEN 'role:view' THEN '角色查看'
  WHEN 'role:manage' THEN '角色权限管理'
  WHEN 'log:view' THEN '操作日志查看'
  WHEN 'storage:view' THEN '存储查看'
  WHEN 'storage:manage' THEN '存储管理'
  WHEN 'system:manage' THEN '系统配置管理'
  ELSE permission_name
END,
description = CASE permission_code
  WHEN 'user:view' THEN '允许查看用户'
  WHEN 'user:manage' THEN '允许管理用户'
  WHEN 'user:approve' THEN '允许审批注册用户'
  WHEN 'dept:view' THEN '允许查看组织'
  WHEN 'dept:manage' THEN '允许管理组织树'
  WHEN 'role:view' THEN '允许查看角色模板'
  WHEN 'role:manage' THEN '允许管理角色和权限矩阵'
  WHEN 'log:view' THEN '允许查看操作日志'
  WHEN 'storage:view' THEN '允许查看存储统计'
  WHEN 'storage:manage' THEN '允许管理存储配置'
  WHEN 'system:manage' THEN '允许管理系统配置'
  ELSE description
END
WHERE permission_code IN (
  'user:view', 'user:manage', 'user:approve',
  'dept:view', 'dept:manage',
  'role:view', 'role:manage',
  'log:view',
  'storage:view', 'storage:manage',
  'system:manage'
);

UPDATE sys_role
SET role_name = CASE role_code
  WHEN 'SUPER_ADMIN' THEN '超级管理员'
  WHEN 'SEGMENT_ADMIN' THEN '管理员'
  WHEN 'DEPT_ADMIN' THEN '部门管理员'
  WHEN 'DOC_MANAGER' THEN '资料员'
  WHEN 'STAFF' THEN '普通用户'
  WHEN 'READONLY' THEN '只读用户'
  ELSE role_name
END,
description = CASE role_code
  WHEN 'SUPER_ADMIN' THEN '拥有全部系统权限'
  WHEN 'SEGMENT_ADMIN' THEN '负责所属一级组织及下级组织注册审核'
  WHEN 'DEPT_ADMIN' THEN '负责本部门资料管理'
  WHEN 'DOC_MANAGER' THEN '负责资料上传和整理'
  WHEN 'STAFF' THEN '普通资料访问用户'
  WHEN 'READONLY' THEN '仅用于访问已授权资料'
  ELSE description
END
WHERE role_code IN (
  'SUPER_ADMIN', 'SEGMENT_ADMIN', 'DEPT_ADMIN',
  'DOC_MANAGER', 'STAFF', 'READONLY'
);

UPDATE sys_user
SET real_name = '系统管理员'
WHERE id = 1;

UPDATE sys_system_config
SET description = CASE config_key
  WHEN 'storage.provider' THEN '默认文件存储服务'
  ELSE description
END
WHERE config_key IN (
  'storage.provider'
);

-- Source: deploy/mysql-init/05-doc-submission-system.sql

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
  file_type VARCHAR(32) NULL,
  doc_year INT NOT NULL DEFAULT 2026,
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

SET @doc_item_file_type_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item'
    AND COLUMN_NAME = 'file_type'
);
SET @sql := IF(@doc_item_file_type_exists = 0,
  'ALTER TABLE sys_doc_item ADD COLUMN file_type VARCHAR(32) NULL AFTER item_name',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
  workshop_dept_id BIGINT NULL,
  submitter_dept_id BIGINT NULL,
  upload_user_id BIGINT NOT NULL,
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_doc_submission_category (category_id, submitted_at),
  INDEX idx_doc_submission_workshop (workshop_dept_id, submitted_at),
  INDEX idx_doc_submission_submitter (submitter_dept_id, submitted_at),
  INDEX idx_doc_submission_item (item_id, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @submitter_column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_submission'
    AND COLUMN_NAME = 'submitter_dept_id'
);
SET @sql := IF(@submitter_column_exists = 0,
  'ALTER TABLE sys_doc_submission ADD COLUMN submitter_dept_id BIGINT NULL AFTER workshop_dept_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_doc_submission
SET submitter_dept_id = COALESCE(submitter_dept_id, workshop_dept_id, section_dept_id)
WHERE submitter_dept_id IS NULL;

SET @workshop_nullable := (
  SELECT IS_NULLABLE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_submission'
    AND COLUMN_NAME = 'workshop_dept_id'
);
SET @sql := IF(@workshop_nullable = 'NO',
  'ALTER TABLE sys_doc_submission MODIFY COLUMN workshop_dept_id BIGINT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @submitter_nullable := (
  SELECT IS_NULLABLE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_submission'
    AND COLUMN_NAME = 'submitter_dept_id'
);
SET @sql := IF(@submitter_nullable = 'NO',
  'ALTER TABLE sys_doc_submission MODIFY COLUMN submitter_dept_id BIGINT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @submitter_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_submission'
    AND INDEX_NAME = 'idx_doc_submission_submitter'
);
SET @sql := IF(@submitter_index_exists = 0,
  'CREATE INDEX idx_doc_submission_submitter ON sys_doc_submission (submitter_dept_id, submitted_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

-- Source: deploy/mysql-init/06-seed-image-doc-items.sql

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_image_doc_categories (
  section_name VARCHAR(128) NOT NULL,
  category_name VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL
) ENGINE=Memory DEFAULT CHARSET=utf8mb4;

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_image_doc_items (
  section_name VARCHAR(128) NOT NULL,
  category_name VARCHAR(128) NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL
) ENGINE=Memory DEFAULT CHARSET=utf8mb4;

TRUNCATE TABLE tmp_image_doc_categories;
TRUNCATE TABLE tmp_image_doc_items;

INSERT INTO tmp_image_doc_categories (section_name, category_name, sort_order) VALUES
('技术科', '技术规范', 10),
('技术科', '技术文件', 20),
('技术科', '房建设备检修记录', 30),
('技术科', '专项检查', 40),
('技术科', '基础台账', 50),
('技术科', '专项台账', 60),
('技术科', '限界管理', 70),
('技术科', '客运车站厕所', 80),
('技术科', '防洪工作', 90),
('技术科', '房建维修', 100),
('技术科', '房建大修', 110),
('技术科', '供热春检秋检', 120),
('技术科', '特种设备', 130),
('技术科', '供热生产管理', 140),
('技术科', '供热综合维修', 150),
('计划财务科', '财务管理', 10),
('劳动人事科（党委组织科）', '劳人管理文件', 10),
('劳动人事科（党委组织科）', '人事命令存档', 20),
('劳动人事科（党委组织科）', '劳资管理资料', 30);

INSERT INTO tmp_image_doc_items (section_name, category_name, item_name, sort_order) VALUES
('技术科', '技术规范', '国铁集团技术规章', 10),
('技术科', '技术规范', '集团公司级规章', 20),
('技术科', '技术规范', '段级技术规章', 30),
('技术科', '技术文件', '集团公司级、段级有效技术文件目录及要求配备的纸质文档', 10),
('技术科', '技术文件', '集团公司级技术文件', 20),
('技术科', '技术文件', '段级技术文件', 30),
('技术科', '房建设备检修记录', '房屋建筑物检修验收记录', 10),
('技术科', '房建设备检修记录', '房建设备病害问题库', 20),
('技术科', '专项检查', '上级各类专项检查、专项排查通知文件及相关检查落实情况等', 10),
('技术科', '基础台账', '房屋设备表（建维修6）', 10),
('技术科', '基础台账', '构筑物设备表（建维修6-1）', 20),
('技术科', '基础台账', '室外公共构筑物设备表（建维修7）', 30),
('技术科', '基础台账', '照明设备表（建维修8）', 40),
('技术科', '基础台账', '室内水道卫生器具设备表（建维修9）', 50),
('技术科', '基础台账', '采暖锅炉设备表（建维修10）', 60),
('技术科', '基础台账', '室内公共设施设备表（建维修10-1）', 70),
('技术科', '基础台账', '站台技术状态调查表（建维修13-1）', 80),
('技术科', '基础台账', '雨棚技术状态调查表（建维修13-2）', 90),
('技术科', '基础台账', '宗地图', 100),
('技术科', '专项台账', '房建车间管辖线别及营业里程统计表', 10),
('技术科', '专项台账', '车站统计表', 20),
('技术科', '专项台账', '站房雨棚特殊设备情况统计表', 30),
('技术科', '专项台账', '站房雨棚电伴热融雪系统统计表', 40),
('技术科', '专项台账', '彩钢屋面设备台账', 50),
('技术科', '专项台账', '客运车站厕所统计表', 60),
('技术科', '专项台账', '房屋、构筑物结构鉴定明细表', 70),
('技术科', '专项台账', '房屋构筑物防雷设施检测记录', 80),
('技术科', '专项台账', '轨旁设备汇总表', 90),
('技术科', '专项台账', '高大门窗台账', 100),
('技术科', '专项台账', '照明配电箱设备台账', 110),
('技术科', '专项台账', '动力配电柜（箱）设备台账', 120),
('技术科', '专项台账', '风电动力设备台账', 130),
('技术科', '限界管理', '站台、雨棚限界信息记录（太房专-007）', 10),
('技术科', '限界管理', '段限设施设备统计表', 20),
('技术科', '限界管理', '超限货物运输路径限界核查资料', 30),
('技术科', '客运车站厕所', '客运车站厕所设施设备台账及平面示意图', 10),
('技术科', '防洪工作', '上级防洪文件（集团公司级、段级）', 10),
('技术科', '防洪工作', '车间防洪工作安排', 20),
('技术科', '防洪工作', '防洪应急预案', 30),
('技术科', '防洪工作', '防洪隐患排查相关记录', 40),
('技术科', '防洪工作', '防洪处置情况统计表', 50),
('技术科', '房建维修', '合同（附合同联签表）', 10),
('技术科', '房建维修', '安全协议', 20),
('技术科', '房建维修', '开工报告', 30),
('技术科', '房建维修', '项目验收签认表', 40),
('技术科', '房建维修', '竣工验收报告', 50),
('技术科', '房建维修', '施工前、中、后的影像资料', 60),
('技术科', '房建维修', '工程洽商、联系单', 70),
('技术科', '房建维修', '技术交底', 80),
('技术科', '房建维修', '隐蔽工程验收记录', 90),
('技术科', '房建维修', '主要工程材料合格证、材料试验报告', 100),
('技术科', '房建维修', '房建设备维修验收记录', 110),
('技术科', '房建维修', '房建设备计划检修（完成）统计表', 120),
('技术科', '房建维修', '房建设备综合维修计划（完成）统计表', 130),
('技术科', '房建大修', '大修项目提报计划资料', 10),
('技术科', '房建大修', '大修项目计划文件', 20),
('技术科', '房建大修', '审查通知书', 30),
('技术科', '房建大修', '施工合同', 40),
('技术科', '房建大修', '安全协议', 50),
('技术科', '房建大修', '施工图及预算', 60),
('技术科', '房建大修', '开工报告', 70),
('技术科', '房建大修', '技术交底', 80),
('技术科', '房建大修', '施工组织方案', 90),
('技术科', '房建大修', '施工材料质量验收单', 100),
('技术科', '房建大修', '隐蔽工程验收记录', 110),
('技术科', '房建大修', '大修分项工程竣工验收记录', 120),
('技术科', '房建大修', '项目验收签认表', 130),
('技术科', '房建大修', '工程竣工验收报告', 140),
('技术科', '房建大修', '大修前、后对比照片和隐蔽工程照片', 150),
('技术科', '房建大修', '计价及结算资料', 160),
('技术科', '房建大修', '设计变更资料', 170),
('技术科', '房建大修', '转固及其他资料', 180),
('技术科', '供热春检秋检', '段发春检、秋检文件', 10),
('技术科', '供热春检秋检', '车间春检、秋检安排', 20),
('技术科', '供热春检秋检', '文电要求的各项检查统计表', 30),
('技术科', '供热春检秋检', '车间各项台账和图纸', 40),
('技术科', '供热春检秋检', '特种设备统计台账、设备取历簿、建立一台一档等资料', 50),
('技术科', '特种设备', '安全附件及仪器仪表台账', 10),
('技术科', '特种设备', '特种设备使用登记证', 20),
('技术科', '特种设备', '特种设备定期检验报告（锅检报告）', 30),
('技术科', '特种设备', '能效测试报告（锅炉）', 40),
('技术科', '特种设备', '机械动力设备台账', 50),
('技术科', '特种设备', '其他相关资料（如设备调拨、报废等原始资料）', 60),
('技术科', '供热生产管理', '整修计划、查勘单以及相关验收记录', 10),
('技术科', '供热生产管理', '各类专项检查资料', 20),
('技术科', '供热综合维修', '合同（附合同联签表）', 10),
('技术科', '供热综合维修', '安全协议', 20),
('技术科', '供热综合维修', '开工报告', 30),
('技术科', '供热综合维修', '竣工验收报告', 40),
('技术科', '供热综合维修', '施工前、中、后的影像资料', 50),
('计划财务科', '财务管理', '固定资产明细表，固定资产台账需与技术台账对应', 10),
('计划财务科', '财务管理', '资产清查记录', 20),
('计划财务科', '财务管理', '收款二维码及音响保管台账，收据申领登记簿、交费记录登记簿', 30),
('计划财务科', '财务管理', '“小金库”承诺书、记名传达文件等其他资料', 40),
('劳动人事科（党委组织科）', '劳人管理文件', '上级各种文件、通知、通报和记名传达记录表等', 10),
('劳动人事科（党委组织科）', '人事命令存档', '需车间留存的人事命令', 10),
('劳动人事科（党委组织科）', '劳资管理资料', '车间绩效考核办法', 10),
('劳动人事科（党委组织科）', '劳资管理资料', '车间月度考核分配表', 20),
('劳动人事科（党委组织科）', '劳资管理资料', '车间月度考勤表、各类请假记录', 30);

INSERT INTO sys_doc_category (
  section_dept_id,
  category_name,
  sort_order,
  created_at,
  updated_at,
  deleted
)
SELECT
  d.id,
  c.category_name,
  c.sort_order,
  NOW(),
  NOW(),
  0
FROM tmp_image_doc_categories c
JOIN sys_dept d
  ON d.dept_name = c.section_name
 AND d.deleted = 0
 AND d.status = 'ENABLED'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_doc_category existing
  WHERE existing.section_dept_id = d.id
    AND existing.category_name = c.category_name
    AND existing.deleted = 0
);

UPDATE sys_doc_category category
JOIN sys_dept d
  ON d.id = category.section_dept_id
 AND d.deleted = 0
 AND d.status = 'ENABLED'
JOIN tmp_image_doc_categories seed
  ON seed.section_name = d.dept_name
 AND seed.category_name = category.category_name
SET category.sort_order = seed.sort_order,
    category.updated_at = NOW()
WHERE category.deleted = 0;

INSERT INTO sys_doc_item (
  category_id,
  item_name,
  content_html,
  attachment_enabled,
  sort_order,
  created_at,
  updated_at,
  deleted
)
SELECT
  category.id,
  seed.item_name,
  '',
  1,
  seed.sort_order,
  NOW(),
  NOW(),
  0
FROM tmp_image_doc_items seed
JOIN sys_dept d
  ON d.dept_name = seed.section_name
 AND d.deleted = 0
 AND d.status = 'ENABLED'
JOIN sys_doc_category category
  ON category.section_dept_id = d.id
 AND category.category_name = seed.category_name
 AND category.deleted = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_doc_item existing
  WHERE existing.category_id = category.id
    AND existing.item_name = seed.item_name
    AND existing.deleted = 0
);

UPDATE sys_doc_item item
JOIN sys_doc_category category
  ON category.id = item.category_id
 AND category.deleted = 0
JOIN sys_dept d
  ON d.id = category.section_dept_id
 AND d.deleted = 0
 AND d.status = 'ENABLED'
JOIN tmp_image_doc_items seed
  ON seed.section_name = d.dept_name
 AND seed.category_name = category.category_name
 AND seed.item_name = item.item_name
SET item.attachment_enabled = 1,
    item.sort_order = seed.sort_order,
    item.updated_at = NOW()
WHERE item.deleted = 0;

SELECT
  seed.section_name,
  COUNT(DISTINCT category.id) AS category_count,
  COUNT(item.id) AS item_count
FROM tmp_image_doc_categories seed
JOIN sys_dept d
  ON d.dept_name = seed.section_name
 AND d.deleted = 0
 AND d.status = 'ENABLED'
LEFT JOIN sys_doc_category category
  ON category.section_dept_id = d.id
 AND category.category_name = seed.category_name
 AND category.deleted = 0
LEFT JOIN sys_doc_item item
  ON item.category_id = category.id
 AND item.deleted = 0
WHERE seed.section_name IN ('技术科', '计划财务科', '劳动人事科（党委组织科）')
GROUP BY seed.section_name
ORDER BY seed.section_name;

-- Source: deploy/mysql-init/07-drop-legacy-file-library.sql

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

-- Source: deploy/mysql-init/08-doc-tree-nodes.sql

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
  deleted_at DATETIME NULL,
  deleted_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  INDEX idx_doc_node_section (section_dept_id, deleted, sort_order),
  INDEX idx_doc_node_parent (parent_id, deleted, sort_order),
  INDEX idx_doc_node_item (item_id, deleted),
  INDEX idx_doc_node_recycle (section_dept_id, node_type, deleted, deleted_at)
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

-- Source: deploy/mysql-init/09-reparent-legacy-doc-files.sql

-- Ensure legacy categories still have a root folder in the new document tree.
INSERT INTO sys_doc_node (
  section_dept_id,
  parent_id,
  node_type,
  node_name,
  item_id,
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

-- Create missing tree nodes for legacy files and place them under their category folder.
INSERT INTO sys_doc_node (
  section_dept_id,
  parent_id,
  node_type,
  node_name,
  item_id,
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
  item.sort_order,
  folder.level + 1,
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

-- Move legacy root-level file nodes under the category folder they originally belonged to.
UPDATE sys_doc_node file_node
JOIN sys_doc_item item
  ON item.id = file_node.item_id
 AND item.deleted = 0
JOIN sys_doc_category category
  ON category.id = item.category_id
 AND category.deleted = 0
JOIN sys_doc_node folder
  ON folder.section_dept_id = category.section_dept_id
 AND folder.parent_id IS NULL
 AND folder.node_type = 'FOLDER'
 AND folder.node_name = category.category_name
 AND folder.deleted = 0
SET file_node.section_dept_id = category.section_dept_id,
    file_node.parent_id = folder.id,
    file_node.level = folder.level + 1,
    file_node.updated_at = NOW()
WHERE file_node.deleted = 0
  AND file_node.node_type = 'FILE'
  AND (file_node.parent_id IS NULL OR file_node.level <= folder.level);

-- Some historical files may remain active while their original category has been soft-deleted.
-- In that case, place them under the active root folder with the same section and sort order.
INSERT INTO sys_doc_node (
  section_dept_id,
  parent_id,
  node_type,
  node_name,
  item_id,
  sort_order,
  level,
  created_by,
  created_at,
  updated_at,
  deleted
)
SELECT
  COALESCE(item.section_dept_id, category.section_dept_id),
  folder.id,
  'FILE',
  item.item_name,
  item.id,
  item.sort_order,
  folder.level + 1,
  item.created_by,
  item.created_at,
  item.updated_at,
  0
FROM sys_doc_item item
JOIN sys_doc_category category
  ON category.id = item.category_id
JOIN sys_doc_node folder
  ON folder.section_dept_id = COALESCE(item.section_dept_id, category.section_dept_id)
 AND folder.parent_id IS NULL
 AND folder.node_type = 'FOLDER'
 AND folder.sort_order = category.sort_order
 AND folder.deleted = 0
LEFT JOIN sys_doc_node existing
  ON existing.item_id = item.id
 AND existing.node_type = 'FILE'
 AND existing.deleted = 0
WHERE item.deleted = 0
  AND category.deleted = 1
  AND existing.id IS NULL;

-- Source: deploy/mysql-init/10-doc-item-business-type.sql

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

-- Source: deploy/mysql-init/10-doc-item-year.sql

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

-- Source: deploy/mysql-init/11-doc-node-upload-progress-visibility.sql

SET @doc_node_show_upload_progress_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND COLUMN_NAME = 'show_upload_progress'
);
SET @sql := IF(@doc_node_show_upload_progress_exists = 0,
  'ALTER TABLE sys_doc_node ADD COLUMN show_upload_progress TINYINT(1) NOT NULL DEFAULT 0 AFTER level',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Source: deploy/mysql-init/11-doc-node-year.sql

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

-- Source: deploy/mysql-init/12-doc-node-upload-progress-default-hidden.sql

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

-- Source: deploy/mysql-init/12-unified-doc-files.sql

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

-- Source: deploy/mysql-init/13-doc-recycle-bin.sql

SET @doc_node_deleted_at_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND COLUMN_NAME = 'deleted_at'
);
SET @sql := IF(@doc_node_deleted_at_exists = 0,
  'ALTER TABLE sys_doc_node ADD COLUMN deleted_at DATETIME NULL AFTER updated_at',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_node_deleted_by_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND COLUMN_NAME = 'deleted_by'
);
SET @sql := IF(@doc_node_deleted_by_exists = 0,
  'ALTER TABLE sys_doc_node ADD COLUMN deleted_by BIGINT NULL AFTER deleted_at',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_node_recycle_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND INDEX_NAME = 'idx_doc_node_recycle'
);
SET @sql := IF(@doc_node_recycle_index_exists = 0,
  'CREATE INDEX idx_doc_node_recycle ON sys_doc_node (section_dept_id, node_type, deleted, deleted_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_item_attachment_deleted_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item_attachment'
    AND COLUMN_NAME = 'deleted'
);
SET @sql := IF(@doc_item_attachment_deleted_exists = 0,
  'ALTER TABLE sys_doc_item_attachment ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER created_at',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_item_attachment_deleted_at_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item_attachment'
    AND COLUMN_NAME = 'deleted_at'
);
SET @sql := IF(@doc_item_attachment_deleted_at_exists = 0,
  'ALTER TABLE sys_doc_item_attachment ADD COLUMN deleted_at DATETIME NULL AFTER deleted',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_item_attachment_deleted_by_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item_attachment'
    AND COLUMN_NAME = 'deleted_by'
);
SET @sql := IF(@doc_item_attachment_deleted_by_exists = 0,
  'ALTER TABLE sys_doc_item_attachment ADD COLUMN deleted_by BIGINT NULL AFTER deleted_at',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_item_attachment_deleted_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item_attachment'
    AND INDEX_NAME = 'idx_doc_item_attachment_deleted'
);
SET @sql := IF(@doc_item_attachment_deleted_index_exists = 0,
  'CREATE INDEX idx_doc_item_attachment_deleted ON sys_doc_item_attachment (item_id, deleted, created_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @doc_item_attachment_cleanup_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_item_attachment'
    AND INDEX_NAME = 'idx_doc_item_attachment_cleanup'
);
SET @sql := IF(@doc_item_attachment_cleanup_index_exists = 0,
  'CREATE INDEX idx_doc_item_attachment_cleanup ON sys_doc_item_attachment (deleted, deleted_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Source: deploy/mysql-init/14-doc-submission-soft-delete.sql

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
