#!/usr/bin/env bash
set -euo pipefail

UPLOAD_DIR="${UPLOAD_DIR:-Blog_Froum/uploads}"
R2_PREFIX="${R2_PREFIX:-}"

required_vars=(
  R2_ACCOUNT_ID
  R2_ACCESS_KEY_ID
  R2_SECRET_ACCESS_KEY
  R2_BUCKET
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Missing required env var: ${var_name}" >&2
    exit 1
  fi
done

if ! command -v aws >/dev/null 2>&1; then
  echo "aws CLI is required. Install awscli first." >&2
  exit 1
fi

if [[ ! -d "${UPLOAD_DIR}" ]]; then
  echo "Upload directory does not exist: ${UPLOAD_DIR}" >&2
  exit 1
fi

endpoint="https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"
destination="s3://${R2_BUCKET}"
if [[ -n "${R2_PREFIX}" ]]; then
  destination="${destination}/${R2_PREFIX#/}"
fi

export AWS_ACCESS_KEY_ID="${R2_ACCESS_KEY_ID}"
export AWS_SECRET_ACCESS_KEY="${R2_SECRET_ACCESS_KEY}"
export AWS_DEFAULT_REGION="${R2_REGION:-auto}"

aws s3 sync "${UPLOAD_DIR}" "${destination}" \
  --endpoint-url "${endpoint}" \
  --checksum-algorithm CRC32

cat <<EOF

R2 sync finished.

If your old DB values contain:
  http://localhost:8080/uploads

replace them with your Worker URL:
  ${R2_PUBLIC_URL:-https://img.example.com}

Example SQL:
  UPDATE users
  SET avatar_url = REPLACE(avatar_url, 'http://localhost:8080/uploads', '${R2_PUBLIC_URL:-https://img.example.com}')
  WHERE avatar_url LIKE 'http://localhost:8080/uploads%';

  UPDATE articles
  SET cover_image = REPLACE(cover_image, 'http://localhost:8080/uploads', '${R2_PUBLIC_URL:-https://img.example.com}'),
      content = REPLACE(content, 'http://localhost:8080/uploads', '${R2_PUBLIC_URL:-https://img.example.com}')
  WHERE cover_image LIKE 'http://localhost:8080/uploads%'
     OR content LIKE '%http://localhost:8080/uploads%';

  UPDATE questions
  SET content = REPLACE(content, 'http://localhost:8080/uploads', '${R2_PUBLIC_URL:-https://img.example.com}')
  WHERE content LIKE '%http://localhost:8080/uploads%';

EOF
