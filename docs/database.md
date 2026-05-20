# 数据库设计

## 核心表

- `sys_user`：用户表。
- `sys_dept`：部门和组织树表，`dept_type` 区分 `AGENCY`、`SECTION`、`WORKSHOP`。
- `sys_role`：角色模板表。
- `sys_permission`：系统级权限点表。
- `sys_user_role`：用户和角色模板关联表。
- `sys_role_permission`：角色模板和系统级权限关联表。
- `sys_user_permission`：用户单独系统级权限表，支持 `allow` 和 `deny`。
- `sys_notification`：系统内通知表。
- `sys_operation_log`：操作日志表。
- `sys_login_log`：登录日志表。
- `sys_register_approval`：注册审批表。
- `sys_system_config`：系统配置表。

## 资料填报表

- `sys_doc_category`：科室资料二级侧边栏。
- `sys_doc_item`：二级侧边栏下的文件入口，包含 `content_html` 富文本文件内容和附件上传开关。
- `sys_doc_submission`：附件上传记录，`submitter_dept_id` 记录实际上传人所属组织；无所属组织用户上传时允许为空。
- `sys_doc_attachment`：上传记录附件元数据，真实文件保存在 MinIO。

## 初始化数据

初始化脚本位于 `deploy/mysql-init/`：

- `01-schema.sql`：建表脚本。
- `02-init-data.sql`：默认部门、权限点、角色模板、管理员账号和系统配置。
- `03-org-agency-migration.sql`：组织结构兼容迁移，并补齐 `dept_type`。
- `05-doc-submission-system.sql`：新增科室资料填报表。
- `07-drop-legacy-file-library.sql`：删除旧文件库、旧动态字段和值表以及旧权限配置。
