#!/usr/bin/env bash
set -euo pipefail

domain="${1:?Public domain is required}"
acme_email="${2:?ACME email is required}"
local_login="${3:-false}"
app_dir="/opt/fyp-platform"

cd "${app_dir}"
umask 077

if [[ -f .env.production ]]; then
  echo ".env.production already exists; refusing to replace production secrets." >&2
  exit 1
fi

postgres_password="$(openssl rand -hex 24)"
token_secret="$(openssl rand -hex 48)"
admin_password="$(openssl rand -base64 24 | tr -d '\n')"

cat > .env.production <<EOF
APP_DOMAIN=${domain}
ACME_EMAIL=${acme_email}
IMAGE_TAG=latest
POSTGRES_DB=fyp_grading_platform
POSTGRES_USER=fyp_platform
POSTGRES_PASSWORD=${postgres_password}
APP_TOKEN_SECRET=${token_secret}
BOOTSTRAP_ADMIN_PASSWORD=${admin_password}
LOCAL_INTERNAL_LOGIN_ENABLED=${local_login}
SQU_SSO_ENABLED=false
INDUSTRY_INVITATION_HOURS=48
PASSWORD_RESET_MINUTES=30
MAIL_HOST=mailpit
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-reply@squ.edu.om
MAIL_SMTP_AUTH=false
MAIL_STARTTLS_ENABLE=false
MAIL_STARTTLS_REQUIRED=false
EOF

printf 'admin@squ.edu.om\n%s\n' "${admin_password}" > .bootstrap-admin-credential
chmod 600 .env.production .bootstrap-admin-credential

echo "Production configuration created. Bootstrap credential is stored privately on the VM."
