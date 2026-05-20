# 数据库设计

## 核心表

- `sys_user`：用户表。
- `sys_dept`：部门和组织树表，`dept_type` 区分 `AGENCY`、`SECTION`、`WORKSHOP`。
- `sys_role`：角色模板表。
- `sys_permission`：系统级权限点表。
- `sys_user_role`：用户和角色模板关联表。
- `sys_role_permission`：角色模板和系统级权限关联表。
- `sys_user_permission`：用户单独系统级权限表，支持 `allow` 和 `deny`。
- `sys_file`：旧文件库元数据表，保留兼容历史接口。
- `sys_folder`：旧文件库文件夹表，保留兼容历史接口。
- `sys_file_permission`：文件指定授权表。
- `sys_file_version`：文件版本表。
- `sys_file_copy`：文件抄送表。
- `sys_notification`：系统内通知表。
- `sys_recycle_bin`：回收站记录表。
- `sys_operation_log`：操作日志表。
- `sys_login_log`：登录日志表。
- `sys_register_approval`：注册审批表。
- `sys_system_config`：系统配置表。
- `sys_storage_stat`：存储统计表。

## 补充表

- `sys_file_favorite`：文件收藏。
- `sys_file_tag`：文件标签。
- `sys_file_tag_rel`：文件标签关联。
- `sys_file_access_record`：最近查看、最近编辑、最近下载记录。
- `sys_doc_category`：科室资料二级侧边栏。
- `sys_doc_item`：二级侧边栏下的三级资料入口。
- `sys_doc_field`：资料入口收集字段，支持文本、日期和数字。
- `sys_doc_submission`：车间用户上传记录。
- `sys_doc_submission_value`：上传记录字段值。
- `sys_doc_attachment`：上传记录附件元数据，真实文件保存在 MinIO。

## 初始化数据

初始化脚本位于 `deploy/mysql-init/`：

- `01-schema.sql`：建表脚本。
- `02-init-data.sql`：默认部门、权限点、角色模板、管理员账号和系统配置。
- `03-org-agency-migration.sql`：组织结构兼容迁移，并补齐 `dept_type`。
- `05-doc-submission-system.sql`：新增科室资料填报表，清空旧文件库数据库记录，不删除 MinIO 对象。
