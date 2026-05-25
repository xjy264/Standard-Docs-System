# 大同房建公寓段标准化资料管理系统

本项目是面向单位内网和公网部署的标准化资料管理系统 MVP，支持用户注册审批、组织树、权限矩阵、科室资料目录、文件详情、车间附件填报、系统内提醒和操作日志。

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

- 科室维护多级资料目录和文件入口，文件详情支持富文本内容。
- 车间用户按文件入口上传附件，MySQL 只保存附件元数据，真实附件保存到 MinIO。
- 科室用户查看本科室资料入口下全部车间上传记录，车间用户只查看本车间记录。
- 超级管理员拥有全部科室目录、文件入口和上传记录管理权限。

## 验证命令

```bash
cd backend && mvn test
cd frontend && npm run build
cd deploy && docker compose up -d
```
