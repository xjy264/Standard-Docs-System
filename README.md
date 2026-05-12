# 大同房建公寓段标准化资料管理系统

本项目是面向单位内网和公网部署的标准化资料管理系统 MVP，支持用户注册审批、组织树、权限矩阵、文件上传下载、访问权隔离、回收站、文件抄送、系统内提醒和操作日志。

## 技术栈

- 后端：Java 21、Spring Boot 3、Spring Security、JWT、MyBatis-Plus、MySQL 8、Redis、MinIO、Knife4j。
- 前端：Vue 3、Vite、TypeScript、Element Plus、Pinia、Vue Router、Axios。
- 部署：Docker Compose、Nginx、MySQL、Redis、MinIO。

## 默认账号

- 手机号：`00000000000`
- 初始密码：`Admin12345@@`

数据库中保存的是 BCrypt 加密后的密码。首次部署后建议尽快修改管理员密码。

## 本地启动

后端：

```bash
cd backend
mvn spring-boot:run
```

本地开发模式下前端默认端口为 `8000`，后端默认端口为 `8010`。前端 Vite 代理默认指向 `http://localhost:8010`。

前端：

```bash
cd frontend
npm install
npm run dev
```

## Docker Compose 部署

```bash
cd deploy
cp .env.example .env
docker compose up -d
```

访问地址：

- 前端：`http://localhost:8000`
- 后端接口：`http://localhost:8010/api`
- MinIO 控制台：`http://localhost:9001`
- Knife4j：`http://localhost:8010/doc.html`

公网部署时，修改 `deploy/.env` 中的域名、端口、MinIO 外部地址和 JWT 密钥。

## 第一阶段重点

- 真实文件上传到 MinIO，MySQL 只保存文件元数据。
- 文件访问权按上传人、可见范围、指定授权、抄送授权判断。
- 文件级权限第一阶段不拆分查看、下载、预览、编辑、抄送。
- 删除、恢复、彻底删除仍由系统级权限控制。
- CAD 第一阶段只保证上传下载和未配置预览提示。
- OnlyOffice 第一阶段保留接口和配置项，后续接入在线预览和编辑回写。

## 验证命令

```bash
cd backend && mvn test
cd frontend && npm run build
cd deploy && docker compose up -d
```
