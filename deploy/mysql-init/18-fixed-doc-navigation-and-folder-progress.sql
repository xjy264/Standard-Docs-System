SET NAMES utf8mb4;

SET @doc_node_progress_target_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_doc_node'
    AND COLUMN_NAME = 'progress_target'
);
SET @sql := IF(@doc_node_progress_target_exists = 0,
  'ALTER TABLE sys_doc_node ADD COLUMN progress_target INT NULL AFTER show_upload_progress',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @agency_id := (
  SELECT id FROM sys_dept
  WHERE dept_code = 'AGENCY'
    AND deleted = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_dept (parent_id, dept_name, dept_code, dept_type, sort_order, status, deleted)
SELECT @agency_id, '公寓科', 'APARTMENT_SECTION', 'SECTION', 130, 'ENABLED', 0
WHERE @agency_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_code = 'APARTMENT_SECTION' AND deleted = 0);

INSERT INTO sys_dept (parent_id, dept_name, dept_code, dept_type, sort_order, status, deleted)
SELECT @agency_id, '党群工作科', 'PARTY_AFFAIRS', 'SECTION', 140, 'ENABLED', 0
WHERE @agency_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_code = 'PARTY_AFFAIRS' AND deleted = 0);

INSERT INTO sys_dept (parent_id, dept_name, dept_code, dept_type, sort_order, status, deleted)
SELECT @agency_id, '房建监测和维修中心', 'BUILDING_MAINTENANCE_CENTER', 'SECTION', 170, 'ENABLED', 0
WHERE @agency_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_code = 'BUILDING_MAINTENANCE_CENTER' AND deleted = 0);

INSERT INTO sys_dept (parent_id, dept_name, dept_code, dept_type, sort_order, status, deleted)
SELECT @agency_id, '生产调度和监控中心', 'PRODUCTION_DISPATCH_CENTER', 'SECTION', 180, 'ENABLED', 0
WHERE @agency_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_code = 'PRODUCTION_DISPATCH_CENTER' AND deleted = 0);

INSERT INTO sys_dept (parent_id, dept_name, dept_code, dept_type, sort_order, status, deleted)
SELECT @agency_id, '技术规章', 'DOC_TECH_RULES', 'DOC_SECTION', 10, 'ENABLED', 0
WHERE @agency_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_code = 'DOC_TECH_RULES' AND deleted = 0);

INSERT INTO sys_dept (parent_id, dept_name, dept_code, dept_type, sort_order, status, deleted)
SELECT @agency_id, '技术文件', 'DOC_TECH_FILES', 'DOC_SECTION', 20, 'ENABLED', 0
WHERE @agency_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_code = 'DOC_TECH_FILES' AND deleted = 0);

INSERT INTO sys_dept (parent_id, dept_name, dept_code, dept_type, sort_order, status, deleted)
SELECT @agency_id, '管理办法', 'DOC_MANAGEMENT_METHODS', 'DOC_SECTION', 30, 'ENABLED', 0
WHERE @agency_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_code = 'DOC_MANAGEMENT_METHODS' AND deleted = 0);

UPDATE sys_dept
SET parent_id = @agency_id,
    dept_name = CASE dept_code
      WHEN 'OFFICE' THEN '办公室（党委办公室）'
      WHEN 'SAFETY' THEN '安全培训科'
      WHEN 'FINANCE' THEN '计划财务科'
      WHEN 'APARTMENT_SECTION' THEN '公寓科'
      WHEN 'PARTY_AFFAIRS' THEN '党群工作科'
      WHEN 'HR_ORG' THEN '劳动人事科（党委组织科）'
      WHEN 'TECH' THEN '技术科'
      WHEN 'BUILDING_MAINTENANCE_CENTER' THEN '房建监测和维修中心'
      WHEN 'PRODUCTION_DISPATCH_CENTER' THEN '生产调度和监控中心'
      WHEN 'DOC_TECH_RULES' THEN '技术规章'
      WHEN 'DOC_TECH_FILES' THEN '技术文件'
      WHEN 'DOC_MANAGEMENT_METHODS' THEN '管理办法'
      ELSE dept_name
    END,
    dept_type = CASE
      WHEN dept_code IN ('DOC_TECH_RULES', 'DOC_TECH_FILES', 'DOC_MANAGEMENT_METHODS') THEN 'DOC_SECTION'
      ELSE 'SECTION'
    END,
    sort_order = CASE dept_code
      WHEN 'DOC_TECH_RULES' THEN 10
      WHEN 'DOC_TECH_FILES' THEN 20
      WHEN 'DOC_MANAGEMENT_METHODS' THEN 30
      WHEN 'OFFICE' THEN 100
      WHEN 'SAFETY' THEN 110
      WHEN 'FINANCE' THEN 120
      WHEN 'APARTMENT_SECTION' THEN 130
      WHEN 'PARTY_AFFAIRS' THEN 140
      WHEN 'HR_ORG' THEN 150
      WHEN 'TECH' THEN 160
      WHEN 'BUILDING_MAINTENANCE_CENTER' THEN 170
      WHEN 'PRODUCTION_DISPATCH_CENTER' THEN 180
      ELSE sort_order
    END,
    status = 'ENABLED'
WHERE @agency_id IS NOT NULL
  AND dept_code IN (
    'DOC_TECH_RULES',
    'DOC_TECH_FILES',
    'DOC_MANAGEMENT_METHODS',
    'OFFICE',
    'SAFETY',
    'FINANCE',
    'APARTMENT_SECTION',
    'PARTY_AFFAIRS',
    'HR_ORG',
    'TECH',
    'BUILDING_MAINTENANCE_CENTER',
    'PRODUCTION_DISPATCH_CENTER'
  )
  AND deleted = 0;
