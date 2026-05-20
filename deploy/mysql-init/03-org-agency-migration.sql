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
