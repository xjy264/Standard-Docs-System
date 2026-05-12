# 部署说明

## 内网部署

```bash
cd deploy
cp .env.example .env
docker compose up -d
```

内网部署时，将 `APP_DOMAIN`、前端端口、后端端口和 MinIO 地址配置为内网 IP 或内网域名。

## 公网部署

公网部署时建议：

- 使用公网域名指向 Nginx。
- 修改 `JWT_SECRET` 为随机长密钥。
- 修改 MySQL、MinIO 默认密码。
- 只暴露必要端口。
- 如需 HTTPS，在外层 Nginx 或网关配置证书。

## 依赖服务

- MySQL 8：保存业务数据。
- Redis 7：保存验证码。
- MinIO：保存真实文件。
- 后端服务：提供 RESTful API。
- 前端服务：提供 Vue 静态页面。

## 验证

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
```

登录默认账号 `00000000000 / Admin12345@@` 后，完成组织、用户、权限和文件上传下载演示。
