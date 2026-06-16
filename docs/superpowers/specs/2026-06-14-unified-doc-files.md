# 标准化资料统一通知文件与大修模板库

## 范围

- 科室目录下的文件统一按“通知文件”展示，旧 `business_type` 字段继续保留用于兼容历史数据和上传记录。
- 通知文件正文不再使用富文本，正文附件复用 `sys_doc_item_attachment` 存储元数据，真实文件仍存储在 MinIO。
- 通知文件可以设置可见车间、是否需要车间上传、上传截止时间。
- 车间上传按“每个用户对同一文件只能提交一次”校验，截止时间只限制上传，不影响查看、预览和下载。
- 房建大修模板库由技术科管理员和超管维护，模板库直接存放多个模板文件，只能在“房建大修”子文件夹及更深层文件夹下导入。

## 数据库

- `sys_doc_item` 新增：
  - `workshop_upload_enabled`: 是否需要车间上传。
  - `upload_deadline`: 车间上传截止时间。
  - `visibility_scope`: `ALL` 或 `SELECTED`。
- 新增 `sys_doc_item_workshop_scope`：保存通知文件指定可见车间。
- 新增 `sys_repair_project_template`、`sys_repair_project_template_item`：兼容保存房建大修模板库默认记录和模板文件，旧多模板数据合并展示。
- 迁移脚本为 `deploy/mysql-init/12-unified-doc-files.sql`，可重复执行；旧上传任务默认按全部车间可见迁移。

## 接口

- `GET /api/doc-tree`：新前端不再传 `businessType`；后端仍兼容旧参数。
- `POST /api/doc-nodes/files`、`PUT /api/doc-nodes/{id}`：支持 `uploadDeadline`、`workshopUploadEnabled`、`visibleWorkshopIds`。
- `POST /api/doc-items/{id}/body-attachments`：上传通知文件正文附件。
- `GET /api/doc-item-attachments/{id}/download`：正文附件下载，校验文件可见范围。
- `GET /api/doc-item-attachments/{id}/preview`：返回 PDF/OnlyOffice/不支持/未配置状态。
- `GET /api/doc-item-attachments/{id}/inline`：PDF 内联预览，校验文件可见范围。
- `/api/repair-project-templates/items`：房建大修模板库文件平铺列表、维护；旧模板接口保留兼容。
- `/api/repair-project-templates/import/{parentNodeId}`：按勾选的模板库文件导入到房建大修子文件夹。

## 验证

- 后端：`cd backend && mvn test`
- 前端：`cd frontend && npm run build`
- 手工重点：
  - 资料页不再显示“上传/下达”页签。
  - 指定车间后，非可见车间用户看不到对应文件。
  - 截止时间后车间用户无法上传。
  - 同一用户重复上传同一通知文件被拒绝。
  - PDF 正文附件可内联预览，Office 未配置时显示“预览服务未配置”。
  - 房建大修子文件夹下可从模板库勾选文件并导入为独立资料文件。
