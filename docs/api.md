# 接口说明

统一返回结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

## 认证

- `GET /api/auth/captcha`：获取滑块验证码。
- `POST /api/auth/captcha/check`：校验滑块轨迹，返回登录和注册使用的一次性验证码凭证。
- `POST /api/auth/register`：注册，请求体包含 `phone`、`realName`、`deptId`、`password`、`confirmPassword`，不再填写用户名；密码需满足 8-20 位且包含大小写字母、数字、常见英文特殊符号。
- `POST /api/auth/login`：手机号登录，请求体包含 `phone`、`password`；返回用户信息包含 `admin`，用于标识是否为管理员。
- `POST /api/auth/logout`：登出。
- `GET /api/auth/me`：当前用户信息，返回用户信息包含 `admin`。

## 注册审批

- `GET /api/approvals/pending`：待审批列表，返回审批 ID、用户名、手机号、所属组织、状态和申请时间。
- `GET /api/approvals/history`：审核历史列表，返回当前用户有权查看的已同意和已拒绝记录，包含用户名、手机号、所属组织、审核人和拒绝原因。
- `POST /api/approvals/{id}/approve`：审批通过。
- `POST /api/approvals/{id}/reject`：审批拒绝。

## 组织与用户

- `GET /api/depts/tree`：组织树。
- `GET /api/depts/navigation`：旧组织导航接口，保留兼容控制台和历史页面。
- `GET /api/sections/navigation`：资料主页一级侧边栏科室列表，只返回真实科室节点，不返回搜索、车间或虚拟父级。
- `POST /api/depts`：新建组织节点。
- `PUT /api/depts/{id}`：修改组织节点。
- `DELETE /api/depts/{id}`：删除组织节点。
- `GET /api/users`：用户列表。
- `POST /api/users`：创建用户。
- `PUT /api/users/{id}`：修改用户。
- `POST /api/users/{id}/reset-password`：管理员重置密码，请求体包含 `password` 和 `confirmPassword`，需满足强密码规则且两次一致。
- `POST /api/users/{id}/enable`：启用用户。
- `POST /api/users/{id}/disable`：禁用用户。

## 权限

- `GET /api/roles`：角色模板列表。
- `GET /api/roles/permissions`：权限点列表。
- `POST /api/roles/{id}/permissions`：配置角色权限。
- `GET /api/permission-matrix?deptId=1`：部门用户权限矩阵。
- `POST /api/permission-matrix/users/{userId}`：保存用户权限。

## 首页统计

- `GET /api/dashboard/stats`：首页统计，返回 `userCount`、`fileCount`、`submissionCount`、`attachmentCount`、`pendingApprovalCount`、`unreadCount`、`uploadEnabledFileCount`、`sectionCount`。
- `GET /api/dashboard/section-file-tree`：首页科室组织树和文件数统计，返回 `id`、`parentId`、`deptName`、`deptType`、`fileCount`、`children`。

## 科室资料与车间填报

- `GET /api/doc-tree?sectionDeptId=1`：查询科室下多级资料目录树，节点类型包含文件夹和文件，最高五层；文件夹和文件节点返回 `docYear`，文件节点额外返回 `fileType`、`businessType`、`workshopUploadEnabled`、`uploadDeadline`、`visibilityScope`、`visibleWorkshopIds`；前端默认不传 `businessType`，统一返回上传任务和下达文件；`businessType` 仍可选传 `UPLOAD` 或 `ISSUED` 用于兼容筛选；统一目录和上传任务目录的文件夹节点返回 `showUploadProgress`、`uploadTaskCount`、`completedUploadTaskCount`、`progressPercent`，`showUploadProgress` 表示是否显示上传进度，进度只统计已配置收集项的上传任务文件。
- `POST /api/doc-nodes/folders`：新增文件夹，请求体包含 `sectionDeptId`、`parentId`、`nodeName`、`docYear`、`sortOrder`、`showUploadProgress`；`showUploadProgress` 未传时默认不显示上传进度；`parentId` 为空时仅本科室管理员或超级管理员可新增最高级文件夹。
- `POST /api/doc-nodes/files`：本科室用户或超级管理员新增文件，请求体包含 `sectionDeptId`、`parentId`、`nodeName`、`businessType`、`fileType`、`docYear`、`contentHtml`、`submitterMode`、`requirements`、`uploadDeadline`、`workshopUploadEnabled`、`visibleWorkshopIds`、`sortOrder`；前端不在文件弹窗中展示年份选择，`docYear` 默认来自父级年度目录，未传时后端也会尝试继承父文件夹年份；上传任务的 `requirements` 元素包含 `requirementName`、`description`、`sortOrder`。
- `PUT /api/doc-nodes/{id}`：本科室用户或超级管理员修改文件夹或文件名称、排序；文件夹节点可同步修改年份和是否显示上传进度，未传时默认不显示进度；文件节点可同步修改文件类型、富文本内容、业务类型、提交模式、上传截止时间、车间上传选项、可见车间范围和收集项名称及说明，前端文件编辑弹窗不展示年份选择。文件节点编辑支持 `application/json` 保存元数据，也支持 `multipart/form-data` 附带 `file` 重新上传文件附件，并按新文件名同步文件类型。
- `DELETE /api/doc-nodes/{id}`：本科室用户或超级管理员删除目录节点；文件节点移入本科室共享回收站，文件夹存在子节点时禁止删除且不进入回收站。
- `GET /api/doc-nodes/recycle-bin?sectionDeptId=1`：本科室用户或超级管理员查看本科室回收站文件表格，返回 `id`、`itemId`、`nodeName`、`fileType`、`docYear`、`deletedAt`、`deletedByName`、`originalParentName`、`submissionCount`、`attachmentCount`。
- `POST /api/doc-nodes/{id}/restore`：本科室用户或超级管理员恢复回收站文件，请求体包含 `targetParentId`；目标目录必须是同科室未删除文件夹，若目标目录已有同名文件则拒绝恢复。
- `GET /api/doc-categories?sectionDeptId=1`：查询科室下旧二级侧边栏，保留兼容历史页面和旧数据。
- `POST /api/doc-categories`：本科室用户或超级管理员新增二级侧边栏。
- `PUT /api/doc-categories/{id}`：本科室用户或超级管理员修改二级侧边栏名称和排序。
- `DELETE /api/doc-categories/{id}`：本科室用户或超级管理员删除二级侧边栏。
- `GET /api/doc-items?categoryId=1`：查询二级侧边栏下资料入口表格。
- `GET /api/doc-items/{id}`：查询资料入口详情，包含富文本文件内容、所属科室、二级菜单、业务类型、提交模式、收集项名称及说明和下达附件。
- `POST /api/doc-items`：本科室用户或超级管理员新增资料入口，请求体包含 `categoryId`、`itemName`、`businessType`、`submitterMode`、`requirements`、`contentHtml`、`sortOrder`；上传任务的 `requirements` 元素包含 `requirementName`、`description`、`sortOrder`。
- `PUT /api/doc-items/{id}`：本科室用户或超级管理员修改资料入口名称、业务类型、提交模式、收集项名称及说明、富文本内容和排序。
- `DELETE /api/doc-items/{id}`：本科室用户或超级管理员删除资料入口。
- `POST /api/doc-items/{id}/submissions`：车间用户和科室用户按上传任务提交附件，使用 `multipart/form-data`，包含多个 `files` 和与文件一一对应的 `requirementIds`；所有收集项都必须上传附件。
- `GET /api/doc-items/{id}/submissions`：查询当前上传任务全部上传记录；仅任务所属科室用户和超级管理员可查看，返回 `submitterDeptName`、`uploadUserName`、`submittedAt` 和附件信息。
- `GET /api/doc-items/{id}/my-submission`：查询当前用户在该上传任务下的本人提交记录，未提交时返回空。
- `POST /api/doc-items/{id}/issued-attachments`：本科室用户或超级管理员上传下达文件附件，使用 `multipart/form-data`。
- `POST /api/doc-items/{id}/body-attachments`：本科室用户或超级管理员上传文件正文附件，使用 `multipart/form-data`；同一文件同一时间只允许一个当前有效正文附件，已有附件时必须先删除原有文件后再上传。
- `GET /api/doc-item-attachments/{id}/download`：下载下达文件附件。
- `GET /api/doc-item-attachments/{id}/preview`：查询下达文件附件预览信息；PDF 和图片返回 `inline` 预览地址，Word、Excel、PPT 返回 OnlyOffice 预览配置，未配置 OnlyOffice 时返回普通用户可读提示。前端本地开发时默认将 Office 文件下载地址指向 `http://host.docker.internal:8010`，OnlyOffice 容器需配置 `ONLYOFFICE_ALLOW_PRIVATE_IP_ADDRESS=true` 才能拉取本机后端文件；特殊部署可通过 `VITE_ONLYOFFICE_FILE_BASE` 覆盖。
- `GET /api/doc-item-attachments/{id}/inline`：下达文件附件内联预览文件流，支持 PDF 和图片直接在浏览器中预览。
- `DELETE /api/doc-item-attachments/{id}`：本科室用户或超级管理员软删除文件正文附件，真实文件暂不删除；被删除附件不再展示、下载或预览。
- `GET /api/submissions/{id}`：查看上传记录详情。
- `GET /api/doc-attachments/{id}/download`：下载上传记录附件。

## 通知

- `GET /api/notifications`：通知列表。
- `GET /api/notifications/unread-count`：未读通知数量。
- `POST /api/notifications/{id}/read`：标记通知已读。
