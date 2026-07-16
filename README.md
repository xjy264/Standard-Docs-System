# 大同房建公寓段标准化资料管理系统

本项目是面向单位内网和公网部署的标准化资料管理系统 MVP，支持用户注册审批、组织树、权限矩阵、内业资料、规章制度、文件详情、车间资料上传、系统内提醒和操作日志。

## 技术栈

- 后端：Java 21、Spring Boot 3、Spring Security、JWT、MyBatis-Plus、MySQL 8、Redis、MinIO、Knife4j。
- 前端：Vue 3、Vite、TypeScript、Element Plus、Pinia、Vue Router、Axios。
- 部署：Docker Compose、Nginx、MySQL、Redis、MinIO。

## 默认账号

- 手机号：`00000000000`
- 初始密码：`Admin12345@@`

数据库中保存的是 BCrypt 加密后的密码。首次部署后建议尽快修改管理员密码。
系统不提供跳过密码的管理员免密登录入口。

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
./run.sh init-prod-env
./run.sh deploy
./run.sh doctor
```

已有数据卷升级代码后，执行幂等数据库迁移：

```bash
./run.sh migrate
```

访问地址：

- 前端：`http://localhost:8000`
- 后端接口：`http://localhost:8010/api`（默认仅本机或 Docker 内网访问）
- MinIO 控制台：`http://localhost:9001`（默认仅本机访问）
- OnlyOffice：`http://localhost:8082`（默认仅本机访问）

公网部署时，修改 `deploy/.env` 中的域名、端口、MinIO 外部地址和 JWT 密钥，只暴露前端或统一 Nginx 入口；Knife4j 默认关闭，本地调试时再通过环境变量开启。

## 内网故障导出

系统会记录后端异常、前端运行错误和接口错误。服务器本地可以导出：

```bash
./run.sh export-errors --days 7
```

导出包包含错误事件、完整堆栈、容器日志摘要和自检结果。密码、Cookie、JWT、CSRF、MinIO 密钥、文件正文和原始敏感请求体不会写入错误事件。

PDF 和图片可直接在浏览器中预览。Word、Excel、PPT 本地预览需要先启动 OnlyOffice：

```bash
cd deploy
docker compose up -d onlyoffice
```

后端启用 Office 预览时设置 `ONLYOFFICE_ENABLED=true`、`ONLYOFFICE_URL=http://localhost:8082`。
本地开发时，前端默认把 Office 文件下载地址指向 `http://host.docker.internal:8010`，便于 OnlyOffice 容器直连后端；同时需要保持 `ONLYOFFICE_ALLOW_PRIVATE_IP_ADDRESS=true`，允许 OnlyOffice 拉取本机后端文件。Office 文件拉取使用短时附件访问票据，不会在 URL 中携带登录 JWT。特殊部署可通过 `VITE_ONLYOFFICE_FILE_BASE` 覆盖。

## 第一阶段重点

- 控制台仅向超级管理员和管理员开放；管理员进入用户管理，超级管理员还可进入组织管理。
- 首页只保留“内业资料”和“技术规章、文件、管理办法”两个蓝色大字入口，不展示说明小字。
- 内业资料使用九个固定科室侧边栏；规章制度模块在九个科室前增加技术规章、技术文件、管理办法三个固定入口。
- 内业资料文件夹可按“直接子文件数/完成目标数”展示完成进度，文件数可超过目标数。
- 内业资料用于车间上传资料：科室可在文件夹上开启车间上传并指定车间范围，车间在对应目录下新建文件并上传正文附件。
- 规章制度用于制度文件展示：科室和超级管理员维护文件夹及文件，车间只查看、预览和下载。
- MySQL 只保存文件和附件元数据，真实附件保存到 MinIO。
- 超级管理员拥有全部科室目录、文件入口和历史上传记录管理权限。

## 验证命令

```bash
cd backend && mvn test
cd frontend && npm run build
./run.sh migrate
./run.sh doctor
```
