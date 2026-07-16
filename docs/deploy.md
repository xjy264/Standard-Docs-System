# 部署说明

## 内网部署

```bash
./run.sh init-prod-env
./run.sh deploy
./run.sh doctor
```

内网部署时，将 `APP_DOMAIN`、前端端口和 MinIO 地址配置为内网 IP 或内网域名。MySQL、Redis、MinIO 控制台、OnlyOffice 和后端端口默认只绑定本机或 Docker 内网，不应直接暴露到公网。生产环境启用 HTTPS 后设置 `AUTH_COOKIE_SECURE=true`，并通过单位内网证书或上级 HTTPS 网关统一入口。

启用 Office 在线预览时，建议设置 `ONLYOFFICE_ENABLED=true`、`ONLYOFFICE_URL=/onlyoffice`。前端 Nginx 会把 `/onlyoffice/` 反向代理到 Docker 内网中的 OnlyOffice，仍不需要把 `8082` 暴露到公网。

已有数据卷升级代码后，需要在仓库根目录先执行 `./run.sh backup`，再执行 `./run.sh migrate`，对现有 MySQL 库补齐幂等迁移字段、固定资料导航节点和索引；该命令只执行追加迁移，不重建库、不删除历史数据。后端启动时也会通过 Flyway 检查 `db/migration` 下的追加迁移。

## 公网部署

公网部署时建议：

- 使用公网域名指向 Nginx。
- 设置随机长密钥 `JWT_SECRET`，建议使用 32 字节以上随机值。
- 修改 MySQL、Redis、MinIO 默认密码。
- 只暴露前端或统一 Nginx 入口，数据库、Redis、MinIO、OnlyOffice 和后端接口通过内网访问。
- 使用 HTTPS，并设置 `AUTH_COOKIE_SECURE=true`，确保登录 Cookie 只在 HTTPS 下发送。
- 接口文档默认关闭；如需本地调试，临时设置 `KNIFE4J_ENABLED=true`、`SPRINGDOC_API_DOCS_ENABLED=true`、`SPRINGDOC_SWAGGER_UI_ENABLED=true`。
- 使用 `./run.sh backup` 每日备份数据库，默认删除 14 天以前的本机备份；如配置 `BACKUP_PASSWORD`，备份文件会使用 AES-256 加密。

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

## 运维与故障导出

- `./run.sh doctor`：检查容器、MySQL、Redis 和后端健康状态。
- `./run.sh backup`：生成本机数据库备份，默认保留 14 天。
- `./run.sh restore backups/xxx.sql.gz[.enc]`：恢复指定数据库备份，执行前需确认当前数据可覆盖。
- `./run.sh rollback`：使用本机已有镜像重启服务，适合构建失败后回到上一版镜像。
- `./run.sh export-errors --days 7`：导出最近 7 天系统错误故障包，包含错误事件、完整堆栈、容器日志摘要和自检结果。

控制台仅向超级管理员和管理员开放：管理员进入用户管理，超级管理员还可进入组织管理。普通用户直接访问控制台地址会返回首页。

登录默认账号 `00000000000 / Admin12345@@` 后，完成组织、用户、权限和文件上传下载演示。系统不提供跳过密码的免密登录入口。
