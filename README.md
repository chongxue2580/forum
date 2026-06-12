# FroumX Forum

FroumX 是一个前后端分离的论坛/问答社区项目。

## 目录结构

```text
Blog_Froum/        Spring Boot 后端
froum/froum/       Vue 3 + Vite 前端
docker/            MySQL 初始化脚本
docker-compose.mysql.yml
```

## 环境要求

- JDK 11+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+

## 启动 MySQL

```bash
docker compose -f docker-compose.mysql.yml up -d
```

默认会创建 `blog_forum` 和 `blog_forum_test` 数据库。

## 启动后端

```bash
cd Blog_Froum
mvn spring-boot:run
```

默认配置使用本地 MySQL：

```text
DB_URL=jdbc:mysql://localhost:3306/blog_forum?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=root
JWT_SECRET=change-me
```

生产部署时请用环境变量覆盖数据库账号和 `JWT_SECRET`。

## 启动前端

```bash
cd froum/froum
npm install
npm run dev
```

默认访问地址：`http://localhost:5181/`

## 构建与测试

```bash
cd Blog_Froum
mvn test

cd ../froum/froum
npm run build
```
