SET NAMES utf8mb4;

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
