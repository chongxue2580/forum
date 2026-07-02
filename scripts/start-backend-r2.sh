#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${PROJECT_ROOT}/Blog_Froum"
ENV_FILE="${BACKEND_DIR}/.env.r2"
JAR_FILE="${BACKEND_DIR}/target/Blog_Froum-0.0.1-SNAPSHOT.jar"

if [[ ! -f "${ENV_FILE}" ]]; then
  cat >&2 <<EOF
Missing ${ENV_FILE}

Create it from the template:
  cp ${BACKEND_DIR}/.env.r2.example ${ENV_FILE}

Then fill in your Cloudflare R2 values and rerun:
  ./scripts/start-backend-r2.sh
EOF
  exit 1
fi

if [[ ! -f "${JAR_FILE}" ]]; then
  cat >&2 <<EOF
Missing ${JAR_FILE}

Build the backend first:
  docker run --rm -v "${PROJECT_ROOT}:/workspace" -w /workspace/Blog_Froum maven:3.9.9-eclipse-temurin-17 mvn clean package -DskipTests
EOF
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

required_vars=(
  FILE_STORAGE_TYPE
  R2_ACCOUNT_ID
  R2_ACCESS_KEY_ID
  R2_SECRET_ACCESS_KEY
  R2_BUCKET
  R2_PUBLIC_URL
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Missing required value in ${ENV_FILE}: ${var_name}" >&2
    exit 1
  fi
done

if [[ "${FILE_STORAGE_TYPE}" != "r2" ]]; then
  echo "FILE_STORAGE_TYPE must be r2 in ${ENV_FILE}" >&2
  exit 1
fi

existing_pid="$(pgrep -f 'Blog_Froum-0.0.1-SNAPSHOT.jar' | head -1 || true)"
if [[ -n "${existing_pid}" ]]; then
  echo "Stopping existing backend process ${existing_pid}..."
  kill "${existing_pid}" || true
  for _ in {1..20}; do
    if ! kill -0 "${existing_pid}" 2>/dev/null; then
      break
    fi
    sleep 0.5
  done
fi

cd "${BACKEND_DIR}"
echo "Starting backend with R2 storage..."
exec java -jar "${JAR_FILE}"
