SET NAMES utf8mb4;

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
  WHEN 'file:upload' THEN '文件上传'
  WHEN 'file:delete' THEN '文件删除'
  WHEN 'file:restore' THEN '文件恢复'
  WHEN 'file:remove' THEN '文件彻底删除'
  WHEN 'file:manage' THEN '文件管理'
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
  WHEN 'file:upload' THEN '允许上传文件'
  WHEN 'file:delete' THEN '允许软删除文件'
  WHEN 'file:restore' THEN '允许恢复回收站文件'
  WHEN 'file:remove' THEN '允许物理删除文件'
  WHEN 'file:manage' THEN '允许管理共享文件'
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
  'file:upload', 'file:delete', 'file:restore', 'file:remove', 'file:manage',
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
  WHEN 'onlyoffice.enabled' THEN 'OnlyOffice 在线预览和编辑是否启用'
  WHEN 'onlyoffice.url' THEN 'OnlyOffice Document Server 地址'
  WHEN 'cad.preview.enabled' THEN 'CAD 预览转换服务是否启用'
  WHEN 'recycle.retention.days' THEN '回收站保留天数'
  WHEN 'storage.provider' THEN '默认文件存储服务'
  ELSE description
END
WHERE config_key IN (
  'onlyoffice.enabled',
  'onlyoffice.url',
  'cad.preview.enabled',
  'recycle.retention.days',
  'storage.provider'
);
