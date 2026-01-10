# Nginx Configuration for chatmoderatorbot.ru

This directory contains nginx virtual host configurations for the chatmoderatorbot.ru domain and all its subdomains.

## Domain Structure

| Domain | Target | Description |
|--------|--------|-------------|
| chatmoderatorbot.ru | `/var/www/mini-app` | Main Mini App (static files) |
| www.chatmoderatorbot.ru | Redirect | Redirects to chatmoderatorbot.ru |
| api.chatmoderatorbot.ru | `http://app:8080` | Backend API |
| grafana.chatmoderatorbot.ru | `http://grafana:3000` | Grafana monitoring |
| prometheus.chatmoderatorbot.ru | `http://prometheus:9090` | Prometheus metrics (basic auth) |

## Directory Structure

```
docker/nginx/
├── nginx.conf              # Main nginx configuration
├── ssl.conf                # Shared SSL settings
├── mini-app.conf           # (legacy) Original Mini App config
├── .htpasswd               # Basic auth for Prometheus
└── sites/                  # Virtual host configurations
    ├── chatmoderatorbot.ru.conf
    ├── www.chatmoderatorbot.ru.conf
    ├── api.chatmoderatorbot.ru.conf
    ├── grafana.chatmoderatorbot.ru.conf
    └── prometheus.chatmoderatorbot.ru.conf
```

## Deployment Steps

### Step 1: Initial Setup (HTTP only)

The configurations are set to work on HTTP initially. All HTTPS blocks are commented out.

1. Point all DNS A records to the server IP (89.125.243.104):
   ```
   chatmoderatorbot.ru         A    89.125.243.104
   www.chatmoderatorbot.ru     A    89.125.243.104
   api.chatmoderatorbot.ru     A    89.125.243.104
   grafana.chatmoderatorbot.ru A    89.125.243.104
   prometheus.chatmoderatorbot.ru A 89.125.243.104
   ```

2. Deploy with HTTP configuration:
   ```bash
   docker compose -f docker-compose.prod.yml up -d
   ```

3. Verify HTTP access:
   ```bash
   curl http://chatmoderatorbot.ru/health
   curl http://api.chatmoderatorbot.ru/actuator/health
   curl http://grafana.chatmoderatorbot.ru
   curl -u admin:password http://prometheus.chatmoderatorbot.ru
   ```

### Step 2: Set Up Basic Auth for Prometheus

Run the basic auth setup script:

```bash
sudo ./scripts/setup-basic-auth.sh
# Or with arguments:
sudo ./scripts/setup-basic-auth.sh admin your-secure-password
```

This creates `/root/chatkeep/docker/nginx/.htpasswd` with encrypted credentials.

### Step 3: Obtain SSL Certificates

Run the SSL setup script on the production server:

```bash
sudo ./scripts/setup-ssl.sh
```

The script will:
- Install certbot
- Create webroot directory for ACME challenges
- Obtain certificates for all 5 domains
- Set up auto-renewal cron job
- Test renewal process

Certificates will be stored in:
```
/etc/letsencrypt/live/chatmoderatorbot.ru/
├── fullchain.pem
├── privkey.pem
├── cert.pem
└── chain.pem
```

### Step 4: Enable HTTPS

1. Edit each site config file in `docker/nginx/sites/`:
   - Uncomment the HTTPS server block
   - Uncomment the HTTP -> HTTPS redirect
   - Comment out the temporary HTTP serving block

2. Edit `docker-compose.prod.yml`:
   - Uncomment the SSL certificate volume mount:
     ```yaml
     - /etc/letsencrypt:/etc/letsencrypt:ro
     ```
   - Uncomment port 443:
     ```yaml
     - "443:443"
     ```

3. Restart nginx:
   ```bash
   docker restart chatkeep-nginx
   ```

4. Verify HTTPS access:
   ```bash
   curl https://chatmoderatorbot.ru/health
   curl https://api.chatmoderatorbot.ru/actuator/health
   curl https://grafana.chatmoderatorbot.ru
   curl -u admin:password https://prometheus.chatmoderatorbot.ru
   ```

## SSL Certificate Renewal

Certificates are automatically renewed via cron job:
```
0 0 * * * root certbot renew --quiet --deploy-hook 'docker restart chatkeep-nginx'
```

Manual renewal:
```bash
sudo certbot renew
sudo docker restart chatkeep-nginx
```

Test renewal (dry-run):
```bash
sudo certbot renew --dry-run
```

## Security Features

### All Sites
- TLS 1.2 and 1.3 only
- Modern cipher suites
- HSTS headers (HTTPS only)
- X-Content-Type-Options: nosniff
- X-XSS-Protection: 1; mode=block

### Main Site (chatmoderatorbot.ru)
- X-Frame-Options: ALLOW-FROM https://web.telegram.org (for Mini App)
- CSP headers restricting script sources
- Gzip compression
- Static asset caching (1 year)

### API (api.chatmoderatorbot.ru)
- CORS headers for chatmoderatorbot.ru
- WebSocket support
- Proper timeout configuration

### Grafana (grafana.chatmoderatorbot.ru)
- WebSocket support for live updates
- X-Frame-Options: ALLOW-FROM for Telegram embedding

### Prometheus (prometheus.chatmoderatorbot.ru)
- Basic authentication required
- X-Frame-Options: DENY
- Access restricted to authorized users only

## Troubleshooting

### Check nginx configuration syntax
```bash
docker exec chatkeep-nginx nginx -t
```

### View nginx error logs
```bash
docker logs chatkeep-nginx
```

### Check SSL certificate status
```bash
sudo certbot certificates
```

### Test SSL configuration
```bash
curl -vI https://chatmoderatorbot.ru
# Or use: https://www.ssllabs.com/ssltest/
```

### Reload nginx configuration
```bash
docker restart chatkeep-nginx
# Or without downtime:
docker exec chatkeep-nginx nginx -s reload
```

## Environment Variables

Required in `.env` file:
```bash
# Grafana
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=your-secure-password

# Database
DB_PASSWORD=your-db-password

# Telegram
TELEGRAM_BOT_TOKEN=your-bot-token

# JWT
JWT_SECRET=your-jwt-secret
```

## Migration from Old Config

The old `mini-app.conf` is kept for reference but is no longer used. The new setup uses:
- `nginx.conf` as the main configuration
- `sites/*.conf` for individual virtual hosts
- Proper SSL support
- Multi-domain setup

To migrate:
1. Deploy new configuration (follows steps above)
2. Verify all domains work
3. Remove or archive `mini-app.conf`
