# Deployment Checklist for chatmoderatorbot.ru

This checklist guides you through deploying the Chatkeep application with the new domain configuration.

## Pre-Deployment

- [ ] DNS records configured for all 5 domains pointing to 89.125.243.104
  - [ ] chatmoderatorbot.ru
  - [ ] www.chatmoderatorbot.ru
  - [ ] api.chatmoderatorbot.ru
  - [ ] grafana.chatmoderatorbot.ru
  - [ ] prometheus.chatmoderatorbot.ru

- [ ] `.env` file created with required variables:
  ```bash
  # Database
  DB_NAME=chatkeep
  DB_USERNAME=chatkeep
  DB_PASSWORD=<secure-password>

  # Telegram
  TELEGRAM_BOT_TOKEN=<bot-token>
  TELEGRAM_BOT_USERNAME=<main-bot-username>
  TELEGRAM_ADMINBOT_TOKEN=<admin-bot-token>
  TELEGRAM_ADMINBOT_USERNAME=<admin-bot-username>

  # JWT
  JWT_SECRET=<secure-jwt-secret>

  # Mini App
  MINI_APP_URL=https://chatmoderatorbot.ru

  # Grafana
  GRAFANA_ADMIN_USER=admin
  GRAFANA_ADMIN_PASSWORD=<secure-password>

  # GitHub (for image pull)
  GITHUB_REPOSITORY=andvl/chatkeep
  IMAGE_TAG=latest
  ```

- [ ] Server has Docker and Docker Compose installed
- [ ] Firewall allows ports 80 and 443
- [ ] Mini App built and in `mini-app/dist/` directory

## Phase 1: HTTP Deployment

### Step 1: Verify DNS Propagation
```bash
# On server or local machine
dig chatmoderatorbot.ru
dig api.chatmoderatorbot.ru
dig grafana.chatmoderatorbot.ru
dig prometheus.chatmoderatorbot.ru
```

- [ ] All domains resolve to 89.125.243.104

### Step 2: Deploy Services (HTTP only)

```bash
cd /root/chatkeep

# Pull latest images
docker compose -f docker-compose.prod.yml pull

# Start services
docker compose -f docker-compose.prod.yml up -d

# Check status
docker compose -f docker-compose.prod.yml ps
```

- [ ] All containers running (db, app, nginx, prometheus, grafana, cloudflared)
- [ ] App health check passing: `docker ps | grep healthy`

### Step 3: Verify HTTP Access

```bash
# Main site
curl http://chatmoderatorbot.ru/health
# Expected: "healthy"

# API
curl http://api.chatmoderatorbot.ru/actuator/health
# Expected: {"status":"UP"}

# Grafana
curl -I http://grafana.chatmoderatorbot.ru
# Expected: 200 OK

# Prometheus (should require auth)
curl -I http://prometheus.chatmoderatorbot.ru
# Expected: 401 Unauthorized
```

- [ ] Main site accessible
- [ ] API responding
- [ ] Grafana accessible
- [ ] Prometheus requires authentication

### Step 4: Set Up Prometheus Basic Auth

```bash
cd /root/chatkeep
sudo ./scripts/setup-basic-auth.sh
# Follow prompts to set username and password
```

- [ ] `.htpasswd` file created
- [ ] Nginx restarted: `docker restart chatkeep-nginx`
- [ ] Can access Prometheus with credentials: `curl -u admin:password http://prometheus.chatmoderatorbot.ru`

## Phase 2: SSL Setup

### Step 5: Obtain SSL Certificates

```bash
cd /root/chatkeep
sudo ./scripts/setup-ssl.sh
```

The script will:
- Install certbot if needed
- Create webroot directory
- Obtain certificates for all 5 domains
- Set up auto-renewal cron job
- Test renewal process

- [ ] Script completed successfully
- [ ] Certificates obtained: `sudo certbot certificates`
- [ ] Auto-renewal cron job exists: `cat /etc/cron.d/certbot-renew`

### Step 6: Enable HTTPS in Nginx Configs

For each file in `docker/nginx/sites/`:

1. **chatmoderatorbot.ru.conf**
   - [ ] Uncomment HTTPS server block
   - [ ] Uncomment HTTP -> HTTPS redirect in HTTP block
   - [ ] Comment out temporary HTTP serving

2. **www.chatmoderatorbot.ru.conf**
   - [ ] Uncomment HTTPS redirect block

3. **api.chatmoderatorbot.ru.conf**
   - [ ] Uncomment HTTPS server block
   - [ ] Uncomment HTTP -> HTTPS redirect

4. **grafana.chatmoderatorbot.ru.conf**
   - [ ] Uncomment HTTPS server block
   - [ ] Uncomment HTTP -> HTTPS redirect

5. **prometheus.chatmoderatorbot.ru.conf**
   - [ ] Uncomment HTTPS server block
   - [ ] Uncomment HTTP -> HTTPS redirect

### Step 7: Enable HTTPS in Docker Compose

Edit `docker-compose.prod.yml`:

- [ ] Uncomment SSL certificate volume mount:
  ```yaml
  - /etc/letsencrypt:/etc/letsencrypt:ro
  ```

- [ ] Uncomment port 443 mapping:
  ```yaml
  - "443:443"
  ```

### Step 8: Restart Nginx

```bash
# Test nginx config first
docker exec chatkeep-nginx nginx -t

# Restart nginx
docker restart chatkeep-nginx

# Check logs for errors
docker logs chatkeep-nginx --tail=50
```

- [ ] Nginx config test passed
- [ ] Nginx restarted successfully
- [ ] No errors in logs

### Step 9: Verify HTTPS Access

```bash
# Main site
curl https://chatmoderatorbot.ru/health
# Expected: "healthy"

# API
curl https://api.chatmoderatorbot.ru/actuator/health
# Expected: {"status":"UP"}

# Grafana
curl -I https://grafana.chatmoderatorbot.ru
# Expected: 200 OK

# Prometheus
curl -u admin:password https://prometheus.chatmoderatorbot.ru
# Expected: Prometheus web interface HTML

# Test HSTS header
curl -I https://chatmoderatorbot.ru
# Expected: Strict-Transport-Security header present
```

- [ ] All HTTPS endpoints accessible
- [ ] HTTP redirects to HTTPS working
- [ ] HSTS headers present
- [ ] No certificate warnings

### Step 10: Test SSL Quality

Use SSL Labs test (optional but recommended):
```
https://www.ssllabs.com/ssltest/analyze.html?d=chatmoderatorbot.ru
```

- [ ] SSL Labs rating: A or higher
- [ ] TLS 1.3 supported
- [ ] No weak ciphers

## Phase 3: Application Testing

### Step 11: Test Telegram Bot

- [ ] Bot responding to commands in Telegram
- [ ] Bot can send messages
- [ ] Bot can handle group messages
- [ ] Admin commands working

### Step 12: Test Mini App

- [ ] Open Mini App in Telegram
- [ ] Mini App loads correctly
- [ ] Can authenticate users
- [ ] Can fetch data from API
- [ ] Static assets loading (check browser console)

### Step 13: Test API

```bash
# Health check
curl https://api.chatmoderatorbot.ru/actuator/health

# Metrics (if exposed)
curl https://api.chatmoderatorbot.ru/actuator/prometheus
```

- [ ] Health endpoint returns UP
- [ ] Metrics endpoint accessible
- [ ] CORS headers correct for Mini App requests

### Step 14: Test Monitoring

#### Grafana
- [ ] Login to https://grafana.chatmoderatorbot.ru
- [ ] Prometheus datasource connected
- [ ] Dashboards loading
- [ ] Metrics visible

#### Prometheus
- [ ] Login to https://prometheus.chatmoderatorbot.ru
- [ ] Targets showing as UP
- [ ] Can query metrics
- [ ] Alerts configured (if any)

## Phase 4: Security & Monitoring

### Step 15: Security Hardening

- [ ] Change default Grafana admin password (if not already done)
- [ ] Verify Prometheus basic auth working
- [ ] Check firewall rules (only 80, 443, 22 open)
- [ ] Verify SSL certificate auto-renewal:
  ```bash
  sudo certbot renew --dry-run
  ```

### Step 16: Set Up Monitoring Alerts (Optional)

- [ ] Configure Grafana alerts for critical metrics
- [ ] Set up notification channels (Telegram, email, etc.)
- [ ] Test alert notifications

### Step 17: Backup Configuration

- [ ] Backup `.env` file securely
- [ ] Document Grafana admin credentials
- [ ] Document Prometheus basic auth credentials
- [ ] Backup SSL certificates (optional, can be re-issued)

## Phase 5: Post-Deployment

### Step 18: Documentation

- [ ] Update project README with new domain
- [ ] Document any custom configuration
- [ ] Update API documentation with new base URL

### Step 19: Monitoring

- [ ] Set up regular health checks
- [ ] Monitor application logs:
  ```bash
  docker logs -f chatkeep-app
  docker logs -f chatkeep-nginx
  ```
- [ ] Monitor disk usage
- [ ] Monitor SSL certificate expiry

### Step 20: Performance Testing

- [ ] Test Mini App load time
- [ ] Test API response times
- [ ] Check Prometheus metrics collection
- [ ] Verify Grafana dashboard refresh rates

## Rollback Plan

If issues occur during deployment:

1. **Nginx Issues**
   ```bash
   # Restore old config
   docker compose -f docker-compose.prod.yml down
   # Edit volumes back to mini-app.conf
   docker compose -f docker-compose.prod.yml up -d
   ```

2. **SSL Issues**
   ```bash
   # Comment out HTTPS blocks in site configs
   # Comment out SSL volume mount in docker-compose.prod.yml
   docker restart chatkeep-nginx
   ```

3. **Complete Rollback**
   ```bash
   docker compose -f docker-compose.prod.yml down
   # Restore previous configuration
   docker compose -f docker-compose.prod.yml up -d
   ```

## Maintenance Commands

```bash
# View all container logs
docker compose -f docker-compose.prod.yml logs -f

# Restart specific service
docker restart chatkeep-app

# Update application
docker compose -f docker-compose.prod.yml pull app
docker compose -f docker-compose.prod.yml up -d app

# Renew SSL certificates manually
sudo certbot renew
docker restart chatkeep-nginx

# Backup database
docker exec chatkeep-db pg_dump -U chatkeep chatkeep > backup.sql

# Restore database
docker exec -i chatkeep-db psql -U chatkeep chatkeep < backup.sql
```

## Success Criteria

Deployment is successful when:

- [ ] All 5 domains accessible via HTTPS
- [ ] HTTP automatically redirects to HTTPS
- [ ] SSL Labs rating A or higher
- [ ] Telegram bot responding
- [ ] Mini App functional in Telegram
- [ ] API endpoints accessible
- [ ] Grafana showing metrics
- [ ] Prometheus collecting data
- [ ] No errors in application logs
- [ ] All health checks passing
- [ ] SSL auto-renewal configured
- [ ] Monitoring alerts configured

## GitHub Actions Configuration

For automated deployments via GitHub Actions, configure the following variables and secrets in your repository settings (`Settings > Secrets and variables > Actions`):

### Production Variables (vars.*)
- `PROD_BOT_USERNAME` - Main Telegram bot username (e.g., `chatmoderatorbot`)
- `PROD_ADMINBOT_USERNAME` - Admin bot username for admin panel login (e.g., `chatkeep_admin_bot`) **REQUIRED** for admin.chatmoderatorbot.ru OAuth
- `PROD_ADMIN_USER_IDS` - Comma-separated Telegram user IDS for admin access
- `PROD_TWITCH_WEBHOOK_URL` - Twitch webhook URL for integration
- `DEPLOY_USER` - SSH user for deployment (e.g., `root`)
- `DEPLOY_PATH` - Deployment path on server (e.g., `~/chatkeep`)

### Production Secrets (secrets.*)
- `PROD_BOT_TOKEN` - Main Telegram bot token from @BotFather
- `PROD_ADMINBOT_TOKEN` - Admin bot token from @BotFather **REQUIRED** for validating admin panel OAuth
- `DB_PASSWORD` - PostgreSQL database password
- `JWT_SECRET` - JWT signing secret (generate with `openssl rand -base64 32`)
- `GRAFANA_ADMIN_PASSWORD` - Grafana admin password
- `DEPLOY_SSH_KEY` - Private SSH key for deployment
- `PROD_TWITCH_CLIENT_ID` - Twitch API client ID
- `PROD_TWITCH_CLIENT_SECRET` - Twitch API client secret
- `PROD_TWITCH_WEBHOOK_SECRET` - Twitch webhook secret

### Test Environment
Configure equivalent `TEST_*` variants for staging environment (chatmodtest.ru)

### Critical Configuration Notes
⚠️ **IMPORTANT**: `PROD_ADMINBOT_USERNAME` must be set for admin panel OAuth to work correctly on admin.chatmoderatorbot.ru. Without it, the login widget will fall back to the main bot, causing authentication failures.

The admin bot must be configured in @BotFather with OAuth enabled:
1. Create a new bot with @BotFather
2. Set domain: `/setdomain` → `admin.chatmoderatorbot.ru`
3. Copy the bot username and token to GitHub secrets

## Support Contacts

- Server IP: 89.125.243.104
- SSH Access: root@89.125.243.104
- Project Repository: https://github.com/andvl/chatkeep
- Domain Registrar: [Your domain registrar]

## Notes

- SSL certificates expire every 90 days (auto-renewed)
- Grafana data stored in Docker volume `grafana_data`
- Prometheus data stored in Docker volume `prometheus_data`
- Database data stored in Docker volume `postgres_data`
- Nginx logs: `docker logs chatkeep-nginx`
- App logs: `docker logs chatkeep-app`
