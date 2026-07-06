SET @agency_id := (
  SELECT id FROM sys_dept
  WHERE dept_name = '机关'
    AND deleted = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_dept (parent_id, dept_name, dept_code, dept_type, sort_order, status, deleted)
SELECT @agency_id, '计划财务科', 'FINANCE', 'SECTION', 40, 'ENABLED', 0
WHERE @agency_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dept
    WHERE dept_name = '计划财务科'
      AND deleted = 0
  );

INSERT INTO sys_dept (parent_id, dept_name, dept_code, dept_type, sort_order, status, deleted)
SELECT @agency_id, '劳动人事科（党委组织科）', 'HR_ORG', 'SECTION', 50, 'ENABLED', 0
WHERE @agency_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dept
    WHERE dept_name = '劳动人事科（党委组织科）'
      AND deleted = 0
  );

UPDATE sys_dept
SET parent_id = @agency_id,
    dept_type = 'SECTION',
    status = 'ENABLED',
    sort_order = CASE dept_name
      WHEN '计划财务科' THEN 40
      WHEN '劳动人事科（党委组织科）' THEN 50
      ELSE sort_order
    END
WHERE @agency_id IS NOT NULL
  AND dept_name IN ('计划财务科', '劳动人事科（党委组织科）')
  AND deleted = 0;
