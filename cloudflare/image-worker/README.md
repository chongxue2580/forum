# FroumX Image Worker

This Worker is the image access layer for private Cloudflare R2 storage.

## Request Flow

Upload:

```text
Vue frontend -> Spring Boot /api/upload/* -> Cloudflare R2
```

Read:

```text
Browser -> img.example.com Worker -> private R2 bucket
```

## Features

- Hotlink protection through `Referer` / `Origin`.
- Private object access through signed URLs or a backend auth endpoint.
- Optional image transforms with Cloudflare Images binding:
  - `?w=800`
  - `?h=600`
  - `?q=82`
  - `?format=webp`
  - `?fit=cover`

## Cloudflare Setup

1. Create an R2 bucket.
2. Keep the bucket private. Do not enable public bucket access.
3. Create an R2 API token with object read/write permissions for the bucket.
4. Deploy this Worker and bind the R2 bucket as `FORUM_IMAGES`.
5. Attach a custom domain to the Worker, for example `https://img.example.com`.

## Worker Configuration

Edit `wrangler.toml`:

```toml
[[r2_buckets]]
binding = "FORUM_IMAGES"
bucket_name = "your-r2-bucket-name"

[vars]
ALLOWED_REFERERS = "https://your-forum-domain.com"
ALLOW_EMPTY_REFERER = "false"
PUBLIC_PREFIXES = "images/,documents/"
PRIVATE_PREFIXES = "private/"
```

Set secrets:

```bash
wrangler secret put IMAGE_SIGNING_SECRET
wrangler secret put AUTH_API_URL
```

`IMAGE_SIGNING_SECRET` is optional. It enables signed URLs.

`AUTH_API_URL` is optional. It should point to a backend endpoint that returns `2xx` when the current user may access a private object key, for example:

```text
https://api.example.com/api/upload/r2-auth
```

## Deploy

```bash
cd cloudflare/image-worker
wrangler deploy
```

If your Cloudflare plan does not support Images binding yet:

1. Set `ENABLE_IMAGE_TRANSFORM = "false"`.
2. Remove this block from `wrangler.toml`:

```toml
[images]
binding = "IMAGES"
```

The Worker will still serve images from R2 with hotlink protection and auth checks.

## Signed URL Rule

Signed URLs use:

```text
sig = hex(hmac_sha256(IMAGE_SIGNING_SECRET, objectKey + "." + expires))
```

Example path:

```text
https://img.example.com/private/user/1/avatar.png?expires=1793520000&sig=...
```

## Backend R2 Environment

Set these variables for Spring Boot:

```bash
FILE_STORAGE_TYPE=r2
R2_ACCOUNT_ID=your-cloudflare-account-id
R2_ACCESS_KEY_ID=your-r2-access-key-id
R2_SECRET_ACCESS_KEY=your-r2-secret-access-key
R2_BUCKET=your-r2-bucket-name
R2_PUBLIC_URL=https://img.example.com
R2_PREFIX=
```

With this setup the backend writes objects to R2 and stores Worker URLs in the database.
