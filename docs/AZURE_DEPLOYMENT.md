# Azure Deployment

This deployment targets an Azure for Students subscription and keeps direct
out-of-pocket cost at zero while free quotas or student credit remain available.
Azure resources still have a metered price and must be deleted before the credit
expires to avoid interruption or future charges.

## Production architecture

- Azure Linux VM with a public Azure DNS name.
- GitHub Actions builds the Spring Boot and React images.
- GitHub Container Registry stores the public images.
- Docker Compose runs PostgreSQL, Mailpit, backend, frontend, and Caddy.
- Caddy obtains and renews the public TLS certificate.
- PostgreSQL, backend, and Mailpit have no public host ports.
- Named Docker volumes retain database, reports, and certificates.

## Required inbound ports

- `22/tcp`: SSH, restricted to the administrator IP when possible.
- `80/tcp`: ACME validation and HTTPS redirect.
- `443/tcp` and `443/udp`: HTTPS and HTTP/3.

Do not expose ports 5432, 8080, 8025, or 1025.

## Initial deployment

1. Create the Azure VM and assign an Azure-managed DNS label.
2. Run `deploy/azure/install-runtime.sh` on the VM.
3. Clone the public GitHub repository into `/opt/fyp-platform`.
4. Create `/opt/fyp-platform/.env.production` from the example using unique secrets.
5. Run `deploy/azure/deploy.sh`.
6. Verify `/actuator/health` internally and the public HTTPS login page.
7. Import only fictional demo data for a public demonstration.

## Updating

After a push to `main`, wait for the container publication workflow, then run:

```bash
cd /opt/fyp-platform
git pull --ff-only
./deploy/azure/deploy.sh
```

## Backup

Create a PostgreSQL backup before every important update:

```bash
docker compose --env-file .env.production -f compose.production.yaml \
  exec -T postgres pg_dump -U fyp_platform -d fyp_grading_platform \
  > "fyp-backup-$(date +%Y%m%d-%H%M%S).sql"
```

## University-server migration

Copy the repository, `.env.production`, PostgreSQL dump, and generated-report
volume to the university server. Change `APP_DOMAIN`, SMTP settings, and SQU OIDC
settings, restore PostgreSQL, then start the same production Compose stack. The
application code and grading logic do not need to change.
