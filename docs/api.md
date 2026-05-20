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
- `POST /api/auth/login`：手机号登录，请求体包含 `phone`、`password`。
- `POST /api/auth/logout`：登出。
- `GET /api/auth/me`：当前用户信息。

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

## 科室资料与车间填报

- `GET /api/doc-categories?sectionDeptId=1`：查询科室下二级侧边栏。
- `POST /api/doc-categories`：本科室用户或超级管理员新增二级侧边栏。
- `PUT /api/doc-categories/{id}`：本科室用户或超级管理员修改二级侧边栏名称和排序。
- `DELETE /api/doc-categories/{id}`：本科室用户或超级管理员删除二级侧边栏。
- `GET /api/doc-items?categoryId=1`：查询二级侧边栏下资料入口表格。
- `GET /api/doc-items/{id}`：查询资料入口详情，包含富文本文件内容、所属科室、二级菜单和附件上传开关。
- `POST /api/doc-items`：本科室用户或超级管理员新增资料入口，请求体包含 `categoryId`、`itemName`、`contentHtml`、`attachmentEnabled`、`sortOrder`。
- `PUT /api/doc-items/{id}`：本科室用户或超级管理员修改资料入口名称、富文本内容、附件上传开关和排序。
- `DELETE /api/doc-items/{id}`：本科室用户或超级管理员删除资料入口。
- `POST /api/doc-items/{id}/submissions`：车间用户上传附件，使用 `multipart/form-data`，包含 `files`。
- `GET /api/doc-items/{id}/submissions`：查询当前资料入口上传记录；科室用户看全部车间记录，车间用户只看本车间记录。
- `GET /api/submissions/{id}`：查看上传记录详情。
- `GET /api/doc-attachments/{id}/download`：下载上传记录附件。

## 通知

- `GET /api/notifications`：通知列表。
- `GET /api/notifications/unread-count`：未读通知数量。
- `POST /api/notifications/{id}/read`：标记通知已读。
