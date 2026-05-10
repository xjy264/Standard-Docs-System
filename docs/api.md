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
- `POST /api/auth/register`：注册。
- `POST /api/auth/login`：登录。
- `POST /api/auth/logout`：登出。
- `GET /api/auth/me`：当前用户信息。

## 注册审批

- `GET /api/approvals/pending`：待审批列表。
- `POST /api/approvals/{id}/approve`：审批通过。
- `POST /api/approvals/{id}/reject`：审批拒绝。

## 组织与用户

- `GET /api/depts/tree`：组织树。
- `POST /api/depts`：新建组织节点。
- `PUT /api/depts/{id}`：修改组织节点。
- `DELETE /api/depts/{id}`：删除组织节点。
- `GET /api/users`：用户列表。
- `POST /api/users`：创建用户。
- `PUT /api/users/{id}`：修改用户。
- `POST /api/users/{id}/enable`：启用用户。
- `POST /api/users/{id}/disable`：禁用用户。

## 权限

- `GET /api/roles`：角色模板列表。
- `GET /api/roles/permissions`：权限点列表。
- `POST /api/roles/{id}/permissions`：配置角色权限。
- `GET /api/permission-matrix?deptId=1`：部门用户权限矩阵。
- `POST /api/permission-matrix/users/{userId}`：保存用户权限。

## 文件

- `POST /api/files/upload`：上传文件。
- `GET /api/files`：文件列表和搜索。
- `GET /api/files/{id}`：文件详情。
- `GET /api/files/{id}/download`：文件下载。
- `GET /api/files/{id}/preview`：文件预览配置。
- `GET /api/files/{id}/office-config`：Office 在线编辑配置。
- `DELETE /api/files/{id}`：软删除。
- `POST /api/files/{id}/restore`：恢复。
- `DELETE /api/files/{id}/remove`：彻底删除。

## 抄送与通知

- `POST /api/copies`：创建文件抄送。
- `GET /api/copies/received`：我收到的抄送。
- `GET /api/copies/sent`：我发出的抄送。
- `POST /api/copies/{id}/read`：标记抄送已读。
- `GET /api/notifications`：通知列表。
- `GET /api/notifications/unread-count`：未读通知数量。
- `POST /api/notifications/{id}/read`：标记通知已读。
