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

## Current demonstration environment

- Public URL: `https://squ-fyp-grading.68-221-71-117.sslip.io`
- Azure resource group: `rg-fyp-grading-demo`
- Azure VM: `vm-fyp-grading-demo`
- Region: Spain Central
- VM size: `Standard_B2ats_v2`
- Budget alert: `fyp-demo-budget`, USD 5 monthly threshold monitoring

The `sslip.io` hostname is a free demonstration DNS name. Replace it with the
official SQU domain when the application moves to the university server.

## Required inbound ports

- `22/tcp`: SSH, restricted to the administrator IP when possible.
- `80/tcp`: ACME validation and HTTPS redirect.
- `443/tcp` and `443/udp`: HTTPS and HTTP/3.

Do not expose ports 5432, 8080, 8025, or 1025.

### Inspect captured demonstration emails

Mailpit remains internal on the production VM. To inspect invitation, password-reset,
extension, and report messages without exposing port `8025`, create an SSH tunnel from
PowerShell:

```powershell
$vm = "fyp-squ-ranym-89baa6.spaincentral.cloudapp.azure.com"
$mailpitIp = (ssh azureuser@$vm 'cd /opt/fyp-platform && docker exec $(docker compose --env-file .env.production -f compose.production.yaml ps -q mailpit) hostname -i').Trim()
ssh -N -L "8026:$($mailpitIp):8025" azureuser@$vm
```

Keep that terminal open and browse to `http://localhost:8026`. Port `8026` avoids
conflicts with a local Mailpit instance that may already use `8025`. Mailpit is only a
demonstration SMTP capture service; replace it with SQU SMTP or SendGrid for delivery.

## Initial deployment

1. Create the Azure VM and assign an Azure-managed DNS label.
2. Run `deploy/azure/install-runtime.sh` on the VM.
3. Clone the public GitHub repository into `/opt/fyp-platform`.
4. Create `/opt/fyp-platform/.env.production` from the example using unique secrets.
5. Run `deploy/azure/deploy.sh`.
6. Verify `/actuator/health` internally and the public HTTPS login page.
7. Import only fictional demo data for a public demonstration.

### Provision the principal administrator

The platform uses `ADMIN` as its highest-privilege role. On the first backend
startup, it can create one principal administrator from server-only environment
variables:

```dotenv
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=principal-administrator@squ.edu.om
BOOTSTRAP_ADMIN_NAME=Principal FYP Administrator
BOOTSTRAP_ADMIN_UNIVERSITY_ID=ADMIN-PRINCIPAL
BOOTSTRAP_ADMIN_PASSWORD=<private-initial-password>
```

Set these values in `/opt/fyp-platform/.env.production`, never in a tracked file.
`BOOTSTRAP_ADMIN_PASSWORD` must be unique and delivered through a separate secure
channel. The backend creates the account when absent. If the same e-mail was
pre-provisioned without a password, it activates the account and grants `ADMIN`;
an existing password is never overwritten by later deployments or restarts.

Alternatively, the initial configuration script accepts the principal account
identity as arguments and generates a private password automatically:

```bash
cd /opt/fyp-platform
sudo ./deploy/azure/configure-production.sh \
  fyp.example.squ.edu.om infrastructure@squ.edu.om false \
  principal-administrator@squ.edu.om "Principal FYP Administrator" ADMIN-PRINCIPAL
sudo cat /opt/fyp-platform/.bootstrap-admin-credential
```

The credential file is readable only by privileged server administrators. Delete
it after the account owner confirms access. Once SQU OIDC is enabled, set
`LOCAL_INTERNAL_LOGIN_ENABLED=false`; the database e-mail and `ADMIN` role still
identify and authorize the same principal account without storing the SQU password.

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
