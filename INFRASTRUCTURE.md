# Infrastructure Overview

Complete infrastructure documentation for Chatkeep Telegram Bot with Mini App.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Telegram                             │
│  (Users interact via Bot commands & Mini App web interface) │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │
        ┌────────────────┴────────────────┐
        │                                  │
        ▼                                  ▼
┌──────────────┐                  ┌──────────────┐
│   Bot API    │                  │   Mini App   │
│  (Webhook/   │                  │  (Telegram   │
│   Polling)   │                  │   WebView)   │
└──────┬───────┘                  └──────┬───────┘
       │                                  │
       │                                  │
       ▼                                  ▼
┌─────────────────────────────────────────────┐
│         Nginx (Port 3000)                   │
│  - Serves Mini App static files             │
│  - Proxies /api/* to Backend                │
│  - Handles SPA routing                      │
└──────────────────┬──────────────────────────┘
                   │
                   │ HTTP
                   ▼
┌─────────────────────────────────────────────┐
│      Spring Boot App (Port 8080)            │
│  - Telegram Bot handlers (KTgBotAPI)        │
│  - REST API for Mini App                    │
│  - Business logic & moderation              │
│  - WebClient/RestClient                     │
└──────────────────┬──────────────────────────┘
                   │
                   │ JDBC
                   ▼
┌─────────────────────────────────────────────┐
│      PostgreSQL (Port 5432)                 │
│  - Messages storage                         │
│  - Chat settings & locks                    │
│  - Blocklist patterns                       │
│  - User warnings & bans                     │
└─────────────────────────────────────────────┘
```

## Services

### 1. PostgreSQL Database (`db`)

**Image**: `postgres:16-alpine`
**Container**: `chatkeep-db`
**Port**: `5432` (exposed to host)

**Purpose**:
- Persistent storage for all bot data
- Messages, chat settings, user data, moderation logs

**Volumes**:
- `postgres_data` - persistent database storage

**Healthcheck**:
- Command: `pg_isready -U chatkeep -d chatkeep`
- Interval: 5s
- Timeout: 5s
- Retries: 5

**Environment**:
```yaml
POSTGRES_DB: chatkeep
POSTGRES_USER: chatkeep
POSTGRES_PASSWORD: chatkeep  # Change in production!
```

### 2. Spring Boot Application (`app`)

**Image**: Custom (built from Dockerfile)
**Container**: `chatkeep-app`
**Port**: `8080` (exposed to host)

**Purpose**:
- Telegram bot logic (long polling)
- REST API for Mini App
- Moderation features
- Message collection

**Dependencies**:
- Waits for `db` healthcheck to pass

**Environment**:
```yaml
DB_HOST: db
DB_PORT: 5432
DB_NAME: chatkeep
DB_USERNAME: chatkeep
DB_PASSWORD: chatkeep
TELEGRAM_BOT_TOKEN: ${TELEGRAM_BOT_TOKEN}
LOG_LEVEL: ${LOG_LEVEL:-INFO}
```

**Restart Policy**: `unless-stopped`

### 3. Mini App Nginx (`mini-app`)

**Image**: `nginx:alpine`
**Container**: `chatkeep-mini-app`
**Port**: `3000` (exposed to host)

**Purpose**:
- Serve Mini App static files (React SPA)
- Proxy API requests to backend
- Handle SPA routing (fallback to index.html)

**Volumes**:
- `./mini-app/dist` → `/usr/share/nginx/html` (read-only)
- `./docker/nginx/mini-app.conf` → `/etc/nginx/conf.d/default.conf` (read-only)

**Dependencies**:
- Waits for `app` to be ready

**Healthcheck**:
- Command: `wget --spider http://localhost:80/`
- Interval: 10s
- Timeout: 5s
- Retries: 3

**Restart Policy**: `unless-stopped`

## Network

All services run in the default Docker Compose network:
- Services can communicate via service names (e.g., `app` can reach `db:5432`)
- Network isolation from host

## Volumes

### Named Volumes

- `postgres_data`: PostgreSQL data directory
  - Location: `/var/lib/postgresql/data` in container
  - Persists across container restarts
  - Backed up regularly in production

### Bind Mounts

- `./mini-app/dist`: Mini App production build (read-only)
- `./docker/nginx/mini-app.conf`: Nginx configuration (read-only)

## Nginx Configuration

**File**: `/Users/a.vladislavov/personal/chatkeep/docker/nginx/mini-app.conf`

### Features

1. **SPA Routing**: All non-file requests fallback to `index.html`
2. **Static Asset Caching**: JS/CSS/images cached for 1 year
3. **API Proxy**: `/api/*` proxied to `http://app:8080/api/*`
4. **Gzip Compression**: Enabled for text files
5. **Security Headers**: X-Frame-Options, X-Content-Type-Options, X-XSS-Protection
6. **Health Endpoint**: `/health` returns 200 OK

### Routes

```
/                  → index.html (SPA entry)
/settings          → index.html (SPA handles routing)
/locks             → index.html (SPA handles routing)
/blocklist         → index.html (SPA handles routing)
/api/*             → http://app:8080/api/* (proxy)
/health            → 200 "healthy" (nginx health)
/assets/*.js       → cached static file
/assets/*.css      → cached static file
```

## Scripts

### Main Development Script

**File**: `/Users/a.vladislavov/personal/chatkeep/scripts/dev.sh`

**Commands**:

```bash
./scripts/dev.sh start           # Start DB + backend (default)
./scripts/dev.sh db              # Start only PostgreSQL
./scripts/dev.sh app             # Run backend (assumes DB running)
./scripts/dev.sh mini-app        # Start Mini App dev server (Vite)
./scripts/dev.sh build-mini-app  # Build Mini App for production
./scripts/dev.sh all             # Start full stack (db + app + mini-app)
./scripts/dev.sh test            # Run tests
./scripts/dev.sh build           # Build project
./scripts/dev.sh docker          # Build and run with Docker Compose
./scripts/dev.sh stop            # Stop all containers
./scripts/dev.sh logs            # Show Docker logs
./scripts/dev.sh clean           # Clean artifacts and volumes
./scripts/dev.sh help            # Show help
```

### Mini App Helper

**File**: `/Users/a.vladislavov/personal/chatkeep/scripts/mini-app-helper.sh`

**Commands**:

```bash
./scripts/mini-app-helper.sh install  # Install dependencies
./scripts/mini-app-helper.sh dev      # Start dev server
./scripts/mini-app-helper.sh build    # Build for production
./scripts/mini-app-helper.sh preview  # Preview production build
./scripts/mini-app-helper.sh lint     # Run linter
./scripts/mini-app-helper.sh clean    # Clean build artifacts
./scripts/mini-app-helper.sh tunnel   # Start cloudflared tunnel
./scripts/mini-app-helper.sh status   # Show Mini App status
```

### Deployment Script

**File**: `/Users/a.vladislavov/personal/chatkeep/scripts/deploy.sh`

**Commands**:

```bash
./scripts/deploy.sh build    # Build Docker image
./scripts/deploy.sh push     # Build and push to registry
./scripts/deploy.sh pull     # Pull latest image
./scripts/deploy.sh up       # Start production containers
./scripts/deploy.sh down     # Stop production containers
./scripts/deploy.sh restart  # Pull latest and restart
./scripts/deploy.sh status   # Show container status
./scripts/deploy.sh logs     # Show container logs
```

## Ports

| Service   | Container Port | Host Port | Protocol | Purpose                    |
|-----------|----------------|-----------|----------|----------------------------|
| db        | 5432           | 5432      | TCP      | PostgreSQL                 |
| app       | 8080           | 8080      | TCP      | Spring Boot API + Bot      |
| mini-app  | 80             | 3000      | HTTP     | Mini App (nginx)           |
| mini-app* | 5173           | 5173      | HTTP     | Vite dev server (dev only) |

*Note: Vite dev server runs outside Docker in development

## Environment Variables

### Backend (Spring Boot)

| Variable             | Required | Default | Description                          |
|----------------------|----------|---------|--------------------------------------|
| TELEGRAM_BOT_TOKEN   | Yes      | -       | Bot token from @BotFather            |
| DB_HOST              | Yes      | db      | PostgreSQL host                      |
| DB_PORT              | Yes      | 5432    | PostgreSQL port                      |
| DB_NAME              | Yes      | chatkeep| Database name                        |
| DB_USERNAME          | Yes      | chatkeep| Database username                    |
| DB_PASSWORD          | Yes      | chatkeep| Database password                    |
| LOG_LEVEL            | No       | INFO    | Logging level (DEBUG/INFO/WARN/ERROR)|

### Frontend (Mini App)

| Variable              | Required | Default                   | Description           |
|-----------------------|----------|---------------------------|-----------------------|
| VITE_API_BASE_URL     | No       | http://localhost:8080/api | Backend API URL       |

**Files**:
- `.env.development` - used by `npm run dev`
- `.env.production` - used by `npm run build`

## Build Process

### Backend Build

```dockerfile
# Multi-stage build
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build -x test

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend Build

```bash
# Vite build
cd mini-app
npm install
npm run build  # Output: dist/

# Files:
# - dist/index.html
# - dist/assets/*.js (bundled, hashed)
# - dist/assets/*.css (bundled, hashed)
```

## Data Persistence

### Database

- **Volume**: `postgres_data`
- **Backup**: Manual via `pg_dump` or automated scripts
- **Migration**: Flyway (on app startup)

```bash
# Backup
docker exec chatkeep-db pg_dump -U chatkeep chatkeep > backup.sql

# Restore
cat backup.sql | docker exec -i chatkeep-db psql -U chatkeep chatkeep
```

### Application Logs

- **Location**: Container stdout/stderr
- **Access**: `docker compose logs -f app`
- **Rotation**: Configured via Docker logging driver

```yaml
# In docker-compose.yml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

## Security Considerations

### Current Setup (Development)

- Database credentials hardcoded in docker-compose.yml
- No SSL/TLS termination
- Ports exposed to host
- No rate limiting

### Production Recommendations

1. **Secrets Management**:
   - Use Docker secrets or external vault
   - Never commit credentials to git
   - Use strong, randomly generated passwords

2. **Network Security**:
   - Use reverse proxy (nginx, Traefik) with SSL/TLS
   - Don't expose database port to host
   - Internal Docker network only
   - Enable firewall (ufw, iptables)

3. **Application Security**:
   - Enable HTTPS only (redirect HTTP → HTTPS)
   - Set secure headers (HSTS, CSP, etc.)
   - Rate limiting on API endpoints
   - Input validation and sanitization

4. **Container Security**:
   - Run as non-root user
   - Read-only root filesystem where possible
   - Resource limits (CPU, memory)
   - Regular image updates

## Monitoring and Logging

### Application Metrics

Spring Boot Actuator endpoints:

- `/actuator/health` - Health status
- `/actuator/info` - Application info
- `/actuator/metrics` - Micrometer metrics
- `/actuator/prometheus` - Prometheus format

### Log Aggregation

```bash
# View all logs
docker compose logs -f

# Specific service
docker compose logs -f app

# Follow last 100 lines
docker compose logs -f --tail=100 app

# Filter by time
docker compose logs --since 1h app
```

### Metrics Collection (Optional)

Add Prometheus + Grafana:

```yaml
# docker-compose.monitoring.yml
services:
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana
    ports:
      - "3001:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
```

## Scalability

### Horizontal Scaling

**Backend**:
- Multiple app instances (stateless)
- Load balancer (nginx, HAProxy)
- Shared database

**Frontend**:
- CDN for static assets (Cloudflare, AWS CloudFront)
- Multiple nginx instances

**Database**:
- PostgreSQL read replicas
- Connection pooling (HikariCP)
- Consider managed database (AWS RDS, DigitalOcean)

### Vertical Scaling

**Increase resources**:

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

## Disaster Recovery

### Backup Strategy

1. **Database**: Daily automated backups, retained for 30 days
2. **Application code**: Git repository (remote backup)
3. **Configuration**: Version controlled, encrypted secrets

### Recovery Procedure

1. Restore database from backup
2. Deploy previous stable version from git
3. Verify data integrity
4. Monitor for issues

### RTO/RPO Targets

- **RTO** (Recovery Time Objective): < 1 hour
- **RPO** (Recovery Point Objective): < 24 hours (daily backups)

## CI/CD Integration

### GitHub Actions Example

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build Mini App
        run: |
          cd mini-app
          npm ci
          npm run build

      - name: Build and push Docker image
        run: |
          docker build -t ghcr.io/user/chatkeep:latest .
          docker push ghcr.io/user/chatkeep:latest

      - name: Deploy to server
        run: |
          ssh user@server 'cd /app && docker compose pull && docker compose up -d'
```

## Development Workflow

### Local Development

```bash
# Terminal 1: Database
./scripts/dev.sh db

# Terminal 2: Backend
./scripts/dev.sh app

# Terminal 3: Frontend
./scripts/dev.sh mini-app
```

**URLs**:
- Backend: http://localhost:8080
- Mini App: http://localhost:5173
- Database: localhost:5432

### Testing on Device

```bash
# Start tunnel
cloudflared tunnel --url http://localhost:5173

# Or ngrok
ngrok http 5173

# Configure in @BotFather
# Menu Button → https://your-tunnel-url.trycloudflare.com
```

### Production Build

```bash
# Full build
./scripts/dev.sh build-mini-app
./scripts/dev.sh docker

# Verify
curl http://localhost:3000
curl http://localhost:8080/actuator/health
```

## Troubleshooting Reference

See:
- [VERIFICATION.md](VERIFICATION.md) - Detailed testing procedures
- [DEPLOYMENT.md](DEPLOYMENT.md) - Production deployment guide

## Future Enhancements

Potential infrastructure improvements:

- [ ] Kubernetes deployment (Helm charts)
- [ ] Redis for caching and rate limiting
- [ ] Message queue (RabbitMQ, Kafka) for async processing
- [ ] Elasticsearch for message search
- [ ] OpenTelemetry for distributed tracing
- [ ] Automated testing in CI/CD
- [ ] Blue-green deployments
- [ ] Auto-scaling based on load
- [ ] Multi-region deployment

## Support

For infrastructure questions:
1. Check this documentation
2. Review logs: `docker compose logs`
3. Check service health: `docker compose ps`
4. Review [DEPLOYMENT.md](DEPLOYMENT.md) for troubleshooting
