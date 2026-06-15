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

- `sys_doc_node`：科室资料多级目录节点，节点类型包含文件夹和文件，最高五层，包含 `doc_year` 用于区分年度目录；文件夹节点通过 `show_upload_progress` 控制统一目录中是否显示上传进度，默认不显示。
- `sys_doc_category`：旧科室资料二级侧边栏，保留兼容历史数据。
- `sys_doc_item`：文件入口，包含 `section_dept_id`、`business_type`、`submitter_mode`、`file_type`、`doc_year`、`content_html` 富文本文件内容和附件上传开关；新目录树中文件节点通过 `item_id` 关联该表。`business_type` 为 `UPLOAD` 时表示上传任务，为 `ISSUED` 时表示下达文件。
- `sys_doc_upload_requirement`：上传任务收集项表，记录每个上传任务需要收集的文件类型或文件项，以及任务发起者填写的收集说明。
- `sys_doc_submission`：附件上传记录，`submitter_dept_id` 记录实际上传人所属组织；无所属组织用户上传时允许为空。
- `sys_doc_attachment`：上传记录附件元数据，`requirement_id` 标记附件对应的上传任务收集项，真实文件保存在 MinIO。
- `sys_doc_item_attachment`：下达文件自身附件元数据，真实文件保存在 MinIO，不与上传记录附件混用。

## 初始化数据

初始化脚本位于 `deploy/mysql-init/`：

- `01-schema.sql`：建表脚本。
- `02-init-data.sql`：默认部门、权限点、角色模板、管理员账号和系统配置。
- `03-org-agency-migration.sql`：组织结构兼容迁移，并补齐 `dept_type`。
- `05-doc-submission-system.sql`：新增科室资料填报表。
- `07-drop-legacy-file-library.sql`：删除旧文件库、旧动态字段和值表以及旧权限配置。
- `08-doc-tree-nodes.sql`：新增多级资料目录节点表，并迁移旧二级菜单和文件入口。
- `10-doc-item-year.sql`：为历史文件入口补充 `doc_year`，默认回填 `2026`。
- `11-doc-node-year.sql`：为历史目录节点补充 `doc_year`，历史文件夹默认回填 `2026`，文件节点同步文件年份。
- `10-doc-item-business-type.sql`：新增上传任务、下达文件、收集项、收集项说明和下达附件所需字段与表，并为历史上传文件生成默认收集项。
- `11-doc-node-upload-progress-visibility.sql`：为目录文件夹追加上传进度展示字段。
- `12-doc-node-upload-progress-default-hidden.sql`：将目录文件夹上传进度字段默认值调整为不显示，并把已有文件夹统一更新为不显示。
