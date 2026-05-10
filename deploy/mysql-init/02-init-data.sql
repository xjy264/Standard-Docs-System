INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, sort_order, status, deleted)
VALUES
  (1, 0, '大同房建公寓段', 'DTFJGYD', 1, 'ENABLED', 0),
  (2, 1, '办公室', 'OFFICE', 10, 'ENABLED', 0),
  (3, 1, '技术科', 'TECH', 20, 'ENABLED', 0),
  (4, 1, '安全科', 'SAFETY', 30, 'ENABLED', 0),
  (5, 1, '房建车间', 'HOUSE_WORKSHOP', 40, 'ENABLED', 0),
  (6, 1, '公寓车间', 'APARTMENT_WORKSHOP', 50, 'ENABLED', 0)
ON DUPLICATE KEY UPDATE dept_name = VALUES(dept_name);

INSERT INTO sys_permission (permission_name, permission_code, permission_type, description, sort_order, status)
VALUES
  ('文件上传', 'file:upload', 'SYSTEM', '允许上传文件', 10, 'ENABLED'),
  ('文件删除', 'file:delete', 'SYSTEM', '允许软删除文件', 20, 'ENABLED'),
  ('文件恢复', 'file:restore', 'SYSTEM', '允许恢复回收站文件', 30, 'ENABLED'),
  ('文件彻底删除', 'file:remove', 'SYSTEM', '允许物理删除文件', 40, 'ENABLED'),
  ('文件管理', 'file:manage', 'SYSTEM', '允许管理共享文件', 50, 'ENABLED'),
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
  (2, 'file:upload'), (2, 'user:view'), (2, 'user:approve'), (2, 'dept:view'),
  (3, 'file:upload'), (3, 'file:delete'), (3, 'file:restore'), (3, 'file:manage'),
  (3, 'user:view'), (3, 'dept:view'), (3, 'role:view'), (3, 'storage:view'),
  (4, 'file:upload'), (4, 'file:delete'), (4, 'dept:view'),
  (5, 'file:upload'), (5, 'dept:view')
ON DUPLICATE KEY UPDATE permission_code = VALUES(permission_code);

INSERT INTO sys_user (id, username, password, real_name, phone, dept_id, status, approval_status, is_super_admin, deleted)
VALUES (1, 'admin', '$2y$10$JLYTEoDd2O7bkkA9W176He7tuLuAMKNQ4baclBgz02t4mD8FO3joW', '系统管理员', '00000000000', 1, 'ENABLED', 'APPROVED', 1, 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name);

INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_system_config (config_key, config_value, description)
VALUES
  ('onlyoffice.enabled', 'false', 'OnlyOffice 在线预览和编辑是否启用'),
  ('onlyoffice.url', '', 'OnlyOffice Document Server 地址'),
  ('cad.preview.enabled', 'false', 'CAD 预览转换服务是否启用'),
  ('recycle.retention.days', '30', '回收站保留天数'),
  ('storage.provider', 'minio', '默认文件存储服务')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), description = VALUES(description);
