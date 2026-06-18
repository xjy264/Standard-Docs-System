# 部署说明

## 内网部署

```bash
cd deploy
cp .env.example .env
docker compose up -d
```

内网部署时，将 `APP_DOMAIN`、前端端口和 MinIO 地址配置为内网 IP 或内网域名。MySQL、Redis、MinIO 控制台、OnlyOffice 和后端端口默认只绑定本机或 Docker 内网，不应直接暴露到公网。

已有数据卷升级代码后，需要在仓库根目录执行 `./run.sh migrate`，对现有 MySQL 库补齐幂等迁移字段和索引；该命令只执行追加迁移，不重建库、不删除历史数据。

## 公网部署

公网部署时建议：

- 使用公网域名指向 Nginx。
- 设置随机长密钥 `JWT_SECRET`，建议使用 32 字节以上随机值。
- 修改 MySQL、Redis、MinIO 默认密码。
- 只暴露前端或统一 Nginx 入口，数据库、Redis、MinIO、OnlyOffice 和后端接口通过内网访问。
- 使用 HTTPS，并设置 `AUTH_COOKIE_SECURE=true`，确保登录 Cookie 只在 HTTPS 下发送。
- 接口文档默认关闭；如需本地调试，临时设置 `KNIFE4J_ENABLED=true`、`SPRINGDOC_API_DOCS_ENABLED=true`、`SPRINGDOC_SWAGGER_UI_ENABLED=true`。

## 依赖服务

- MySQL 8：保存业务数据。
- Redis 7：保存验证码、登录失败计数和 OnlyOffice 短时附件访问票据。
- MinIO：保存真实文件。
- 后端服务：提供 RESTful API。
- 前端服务：提供 Vue 静态页面。

## 验证

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
```

已有数据库升级验证：

```bash
./run.sh migrate
```

登录默认账号 `00000000000 / Admin12345@@` 后，完成组织、用户、权限和文件上传下载演示。系统不提供跳过密码的免密登录入口。
