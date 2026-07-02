# R2 + Worker 图片存储方案

## 架构

上传链路：

```text
论坛前端 -> Spring Boot /api/upload/* -> Cloudflare R2
```

访问链路：

```text
浏览器 -> 图片 Worker 域名 -> 私有 R2 bucket
```

Worker 负责：

- 防盗链：检查 `Referer` / `Origin`。
- 压缩：支持 `?w=800&q=82&format=webp`。
- 鉴权访问：私有路径支持签名 URL 或转发后端 `/api/upload/r2-auth` 验证。

## 后端环境变量

```bash
FILE_STORAGE_TYPE=r2
R2_ACCOUNT_ID=Cloudflare Account ID
R2_ACCESS_KEY_ID=R2 API Token Access Key ID
R2_SECRET_ACCESS_KEY=R2 API Token Secret Access Key
R2_BUCKET=Bucket 名称
R2_PUBLIC_URL=https://img.example.com
R2_PREFIX=
```

`R2_PUBLIC_URL` 应该填 Worker 自定义域名，不建议填 R2 原始访问域名。

## Worker 部署

```bash
cd cloudflare/image-worker
npm install
npm run deploy
```

需要先修改：

- `wrangler.toml` 的 `bucket_name`
- `ALLOWED_REFERERS`
- 是否启用 `[images]` binding

如果 Cloudflare 账号没开 Images binding：

1. 删除 `wrangler.toml` 里的 `[images]` 区块。
2. 设置 `ENABLE_IMAGE_TRANSFORM = "false"`。

## 历史图片迁移

```bash
export R2_ACCOUNT_ID=...
export R2_ACCESS_KEY_ID=...
export R2_SECRET_ACCESS_KEY=...
export R2_BUCKET=...
export R2_PUBLIC_URL=https://img.example.com

bash scripts/migrate-local-uploads-to-r2.sh
```

脚本会把 `Blog_Froum/uploads` 同步到 R2，并输出数据库 URL 替换 SQL。

## 私有图片

公开图片默认路径：

```text
images/
documents/
```

私有图片建议路径：

```text
private/users/{userId}/...
```

Worker 访问私有路径时支持两种方式：

1. 签名 URL：`expires` + `sig`
2. 后端鉴权：Worker 转发 `Authorization` 到 `AUTH_API_URL`

后端已提供：

```text
GET /api/upload/r2-auth?key=private/users/{userId}/xxx.png
```
