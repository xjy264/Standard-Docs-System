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
- `sys_error_event`：系统错误事件表，记录后端异常、前端运行错误、接口错误、追踪编号、错误编号、用户和版本上下文；完整堆栈仅超级管理员可在通知台查看和导出。
- `sys_register_approval`：注册审批表。
- `sys_system_config`：系统配置表。

## 资料填报表

- `sys_doc_node`：科室资料多级目录节点，节点类型包含文件夹和文件，最高五层，包含 `doc_year` 用于区分年度目录；文件夹节点通过 `show_upload_progress` 控制统一目录中是否显示上传进度，默认不显示。文件节点软删除时写入 `deleted_at`、`deleted_by`，用于科室回收站展示和 30 天自动清理。
- `sys_doc_category`：旧科室资料二级侧边栏，保留兼容历史数据。
- `sys_doc_item`：文件入口，包含 `section_dept_id`、`business_type`、`submitter_mode`、`file_type`、`doc_year`、`content_html` 富文本文件内容和附件上传开关；新目录树中文件节点通过 `item_id` 关联该表。`business_type` 为 `UPLOAD` 时表示上传任务，为 `ISSUED` 时表示下达文件。
- `sys_doc_upload_requirement`：上传任务收集项表，记录每个上传任务需要收集的文件类型或文件项，以及任务发起者填写的收集说明。
- `sys_doc_submission`：附件上传记录，`submitter_dept_id` 记录实际上传人所属组织；无所属组织用户上传时允许为空；支持软删除字段 `deleted`、`deleted_at`、`deleted_by`，用于隐藏已删除提交。
- `sys_doc_attachment`：上传记录附件元数据，`requirement_id` 标记附件对应的上传任务收集项，真实文件保存在 MinIO；支持随提交记录软删除隐藏。
- `sys_doc_item_attachment`：下达文件自身附件元数据，真实文件保存在 MinIO，不与上传记录附件混用；正文附件支持软删除，`deleted`、`deleted_at`、`deleted_by` 用于隐藏当前附件并保留 30 天内恢复文件所需数据；单独软删除超过 30 天的正文附件会被定时清理。
- 回收站清理：后端默认每天执行一次清理任务，永久清理删除超过 30 天的文件节点、资料入口、正文附件、车间上传附件、上传记录和收集项等相关元数据，并删除对应 MinIO 对象；同时清理超过 30 天的独立软删除正文附件。

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
- `13-doc-recycle-bin.sql`：为文件回收站、正文附件软删除和 30 天自动清理追加必要字段和索引。
- `14-doc-submission-soft-delete.sql`：为车间提交记录和提交附件追加软删除字段和索引。
- `15-error-events.sql`：新增系统错误事件表和查询索引，用于通知台 Bug 统计和内网故障导出。

已有 MySQL 数据卷不会自动重新执行 `deploy/mysql-init/` 中新增脚本。升级代码后，在仓库根目录执行 `./run.sh migrate`，按顺序补齐 `13-doc-recycle-bin.sql`、`14-doc-submission-soft-delete.sql` 和 `15-error-events.sql` 中的幂等字段与索引。后端同时接入 Flyway，`V1__baseline_schema_and_seed.sql` 对应当前 01-14 初始化基线，`V15__error_events.sql` 对应错误事件增量；`baseline-on-migrate` 用于兼容已有数据卷，后续新增结构优先追加 `db/migration/V*.sql`，不覆盖历史 SQL。
