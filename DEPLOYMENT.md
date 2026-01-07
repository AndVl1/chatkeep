# Deployment Guide

Production deployment checklist and procedures for Chatkeep with Mini App.

## Pre-Deployment Checklist

### 1. Code Quality

- [ ] All tests pass: `./gradlew test`
- [ ] Build succeeds: `./gradlew build`
- [ ] Mini App lint passes: `cd mini-app && npm run lint`
- [ ] Mini App builds: `./scripts/dev.sh build-mini-app`
- [ ] No console errors or warnings in browser

### 2. Configuration

- [ ] Environment variables set (see below)
- [ ] Database migrations tested
- [ ] Bot token is valid and secure
- [ ] CORS settings configured correctly
- [ ] Mini App API base URL set for production

### 3. Docker

- [ ] Docker Compose config is valid: `docker compose config`
- [ ] Nginx config is valid (tested in container)
- [ ] Healthchecks working locally
- [ ] Volumes backed up (if applicable)

## Environment Variables

### Required for Backend

```bash
# Telegram
export TELEGRAM_BOT_TOKEN="your_bot_token_from_botfather"

# Database
export DB_HOST="db"  # or your PostgreSQL host
export DB_PORT="5432"
export DB_NAME="chatkeep"
export DB_USERNAME="chatkeep"
export DB_PASSWORD="secure_random_password"  # CHANGE THIS!

# Optional
export LOG_LEVEL="INFO"  # or DEBUG, WARN, ERROR
```

### Required for Mini App

Create/update `mini-app/.env.production`:

```env
VITE_API_BASE_URL=https://your-domain.com/api
```

## Deployment Procedures

### Option 1: Docker Compose (Recommended for VPS)

```bash
# 1. Clone repository
git clone https://github.com/your-repo/chatkeep.git
cd chatkeep

# 2. Create .env file
cat > .env << EOF
TELEGRAM_BOT_TOKEN=your_token_here
DB_PASSWORD=secure_password_here
LOG_LEVEL=INFO
EOF

# 3. Update Mini App production config
cat > mini-app/.env.production << EOF
VITE_API_BASE_URL=https://your-domain.com/api
EOF

# 4. Build Mini App
./scripts/dev.sh build-mini-app

# 5. Start services
docker compose up -d

# 6. Verify
docker compose ps
docker compose logs -f
```

### Option 2: Separate Services

**Backend (Spring Boot):**

```bash
# Build JAR
./gradlew bootJar

# Run
java -jar build/libs/chatkeep-*.jar \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/chatkeep \
  --spring.datasource.username=chatkeep \
  --spring.datasource.password=secure_password \
  --telegram.bot.token=your_token
```

**Frontend (Static hosting):**

```bash
# Build
cd mini-app
npm run build

# Deploy dist/ to:
# - Nginx
# - Cloudflare Pages
# - Vercel
# - Netlify
# - GitHub Pages
```

### Option 3: Kubernetes (Advanced)

See [helm/README.md](helm/README.md) for Helm chart deployment.

## Post-Deployment Verification

### 1. Service Health

```bash
# Backend health
curl https://your-domain.com/actuator/health

# Expected: {"status":"UP"}

# Mini App
curl https://your-domain.com/mini-app/

# Expected: HTML response

# Database
docker exec chatkeep-db pg_isready -U chatkeep
```

### 2. Functionality Tests

- [ ] Bot responds to `/start` command
- [ ] Bot can be added to a group
- [ ] Messages are saved to database
- [ ] Admin commands work in DM
- [ ] Mini App opens from Telegram menu button
- [ ] Mini App loads chat list
- [ ] Mini App can update settings
- [ ] Mini App can manage locks
- [ ] Mini App can manage blocklist

### 3. Monitoring Setup

```bash
# View logs
docker compose logs -f app
docker compose logs -f mini-app

# Check metrics
curl http://localhost:8080/actuator/prometheus

# Database size
docker exec chatkeep-db psql -U chatkeep -c "SELECT pg_size_pretty(pg_database_size('chatkeep'));"
```

## Telegram Bot Configuration

### 1. Set Menu Button (for Mini App)

**Option A: Using @BotFather**

1. Open @BotFather in Telegram
2. Send `/mybots`
3. Select your bot
4. Bot Settings → Menu Button → Configure Menu Button
5. Enter URL: `https://your-domain.com/mini-app/` or `https://your-domain.com:3000/`
6. Enter button text: "Settings" or "Настройки"

**Option B: Using API**

```bash
BOT_TOKEN="your_token"
MINI_APP_URL="https://your-domain.com/mini-app/"

curl -X POST "https://api.telegram.org/bot${BOT_TOKEN}/setChatMenuButton" \
  -H "Content-Type: application/json" \
  -d "{
    \"menu_button\": {
      \"type\": \"web_app\",
      \"text\": \"Settings\",
      \"web_app\": {
        \"url\": \"${MINI_APP_URL}\"
      }
    }
  }"
```

### 2. Set Bot Commands

```bash
curl -X POST "https://api.telegram.org/bot${BOT_TOKEN}/setMyCommands" \
  -H "Content-Type: application/json" \
  -d '{
    "commands": [
      {"command": "start", "description": "Start bot and show help"},
      {"command": "mychats", "description": "List your admin chats"},
      {"command": "connect", "description": "Connect to chat for management"},
      {"command": "help", "description": "Show help message"}
    ]
  }'
```

## SSL/TLS Setup (Production)

### Using Let's Encrypt with Nginx

Add to `docker-compose.yml`:

```yaml
  nginx:
    image: nginx:alpine
    container_name: chatkeep-nginx
    volumes:
      - ./docker/nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./docker/nginx/ssl:/etc/nginx/ssl:ro
      - certbot-data:/var/www/certbot
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - app
      - mini-app

  certbot:
    image: certbot/certbot
    volumes:
      - certbot-data:/var/www/certbot
      - ./docker/nginx/ssl:/etc/letsencrypt

volumes:
  certbot-data:
```

Obtain certificate:

```bash
docker compose run --rm certbot certonly --webroot \
  -w /var/www/certbot \
  -d your-domain.com \
  --email your-email@example.com \
  --agree-tos
```

## Backup and Restore

### Database Backup

```bash
# Backup
docker exec chatkeep-db pg_dump -U chatkeep chatkeep | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz

# Restore
gunzip < backup_20260104_120000.sql.gz | docker exec -i chatkeep-db psql -U chatkeep chatkeep
```

### Automated Backups (Cron)

```bash
# Add to crontab
crontab -e

# Daily backup at 3 AM
0 3 * * * cd /path/to/chatkeep && docker exec chatkeep-db pg_dump -U chatkeep chatkeep | gzip > backups/backup_$(date +\%Y\%m\%d).sql.gz
```

## Scaling

### Horizontal Scaling (Multiple Backend Instances)

Update `docker-compose.yml`:

```yaml
  app:
    deploy:
      replicas: 3
    # ... rest of config
```

Or use Kubernetes with HPA (see helm charts).

### Database Optimization

```sql
-- Add indexes for frequently queried fields
CREATE INDEX CONCURRENTLY idx_messages_chat_date ON messages(chat_id, message_date DESC);
CREATE INDEX CONCURRENTLY idx_messages_user ON messages(user_id);
CREATE INDEX CONCURRENTLY idx_blocklist_chat ON blocklist_patterns(chat_id);

-- Vacuum and analyze
VACUUM ANALYZE messages;
VACUUM ANALYZE chat_settings;
```

## Monitoring and Alerting

### Prometheus + Grafana

See `docker-compose.monitoring.yml` for full setup.

### Simple Health Monitoring

```bash
#!/bin/bash
# health-monitor.sh

BACKEND="http://localhost:8080/actuator/health"
FRONTEND="http://localhost:3000/health"

check_service() {
    local url=$1
    local name=$2

    if curl -sf "$url" > /dev/null; then
        echo "✓ $name is healthy"
    else
        echo "✗ $name is DOWN"
        # Send alert (email, Telegram, etc.)
    fi
}

check_service "$BACKEND" "Backend"
check_service "$FRONTEND" "Frontend"
```

Run via cron every 5 minutes:

```bash
*/5 * * * * /path/to/health-monitor.sh
```

## Rollback Procedure

### Docker Compose

```bash
# Stop current version
docker compose down

# Restore from backup (if needed)
gunzip < backup_20260104_120000.sql.gz | docker exec -i chatkeep-db psql -U chatkeep chatkeep

# Checkout previous version
git checkout v1.0.0  # or previous commit

# Rebuild and start
./scripts/dev.sh build-mini-app
docker compose up -d --build

# Verify
docker compose ps
docker compose logs -f
```

### Database Migration Rollback

```bash
# Rollback last migration
./gradlew flywayUndo

# Or manually
docker exec -it chatkeep-db psql -U chatkeep chatkeep
# In psql:
-- Check migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

-- Rollback manually if needed
-- DELETE FROM flyway_schema_history WHERE version = 'X.X.X';
-- Then manually undo schema changes
```

## Troubleshooting

### Backend Won't Start

```bash
# Check logs
docker compose logs app

# Common issues:
# - Database not ready: increase healthcheck retries
# - Migration failed: check flyway_schema_history table
# - Bot token invalid: verify TELEGRAM_BOT_TOKEN
```

### Mini App Not Loading

```bash
# Check nginx logs
docker compose logs mini-app

# Verify files exist
docker exec chatkeep-mini-app ls -la /usr/share/nginx/html/

# Test nginx config
docker exec chatkeep-mini-app nginx -t

# Common issues:
# - dist/ folder empty: run ./scripts/dev.sh build-mini-app
# - 404 on routes: check nginx SPA fallback config
```

### Database Connection Issues

```bash
# Check database is running
docker compose ps db

# Test connection
docker exec chatkeep-db psql -U chatkeep -c "SELECT 1;"

# Check logs
docker compose logs db

# Common fixes:
# - Increase max_connections in PostgreSQL
# - Check connection pool settings in Spring Boot
```

## Security Hardening

### 1. Database

- [ ] Change default passwords
- [ ] Restrict network access (bind to 127.0.0.1)
- [ ] Enable SSL for connections
- [ ] Regular backups with encryption

### 2. Application

- [ ] Keep dependencies updated
- [ ] Enable HTTPS only
- [ ] Set secure headers (CSP, HSTS, etc.)
- [ ] Rate limiting on API endpoints
- [ ] Input validation and sanitization

### 3. Docker

- [ ] Don't run containers as root
- [ ] Use read-only volumes where possible
- [ ] Scan images for vulnerabilities
- [ ] Limit container resources (CPU, memory)

### 4. Secrets Management

Use Docker secrets or external secrets manager:

```yaml
services:
  app:
    secrets:
      - bot_token
      - db_password

secrets:
  bot_token:
    file: ./secrets/bot_token.txt
  db_password:
    file: ./secrets/db_password.txt
```

## Performance Tuning

### JVM Options

```bash
# In docker-compose.yml
environment:
  JAVA_OPTS: "-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Nginx Caching

Already configured in `docker/nginx/mini-app.conf`:
- Static assets cached for 1 year
- Gzip compression enabled
- Browser caching headers set

### Database Connection Pool

```yaml
# application.yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

## Useful Commands

```bash
# View all running services
docker compose ps

# Restart specific service
docker compose restart app

# Scale service
docker compose up -d --scale app=3

# View resource usage
docker stats

# Clean up unused resources
docker system prune -a

# Export logs
docker compose logs --since 24h > logs.txt

# Database shell
docker exec -it chatkeep-db psql -U chatkeep chatkeep

# Application shell (if debug enabled)
docker exec -it chatkeep-app /bin/sh
```

## Support and Maintenance

### Regular Maintenance Tasks

- **Daily**: Check logs for errors
- **Weekly**: Review metrics and performance
- **Monthly**: Update dependencies, security patches
- **Quarterly**: Database optimization (VACUUM, REINDEX)

### Logs Retention

Configure log rotation to prevent disk space issues:

```yaml
# docker-compose.yml
services:
  app:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

## Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Telegram Bot API](https://core.telegram.org/bots/api)
- [Telegram Mini Apps](https://core.telegram.org/bots/webapps)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Nginx Documentation](https://nginx.org/en/docs/)
