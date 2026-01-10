# Chatkeep Scripts Documentation

This directory contains deployment and development scripts for the Chatkeep project.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Development Commands](#development-commands)
- [Production Commands](#production-commands)
- [Environment Variables](#environment-variables)
- [Deployment Workflows](#deployment-workflows)
- [Troubleshooting](#troubleshooting)

## Overview

The main script `chatkeep.sh` provides a unified interface for both development and production operations. Legacy scripts (`deploy.sh`, `dev.sh`, `mini-app-helper.sh`) are thin wrappers for backward compatibility.

### Main Script

```bash
./scripts/chatkeep.sh <mode> <command> [options]
```

**Modes:**
- `dev` - Development mode (local development)
- `prod` - Production mode (deployment)

## Quick Start

### Local Development

```bash
# Start database and backend
./scripts/chatkeep.sh dev start

# Start full stack (db + backend + frontend)
./scripts/chatkeep.sh dev all

# Start only frontend dev server
./scripts/chatkeep.sh dev mini-app

# Check development environment status
./scripts/chatkeep.sh dev status
```

### Production Deployment

```bash
# Deploy to remote server via SSH (password auth)
./scripts/chatkeep.sh prod deploy \
  --host 89.125.243.104 \
  --user root \
  --password YOUR_PASSWORD

# Deploy using SSH key
./scripts/chatkeep.sh prod deploy \
  --host example.com \
  --user deploy \
  --key ~/.ssh/id_rsa

# Local production testing
./scripts/chatkeep.sh prod build
./scripts/chatkeep.sh prod up
./scripts/chatkeep.sh prod status
```

## Development Commands

### `dev start` (default)

Start PostgreSQL and backend application.

```bash
./scripts/chatkeep.sh dev start
# or simply:
./scripts/chatkeep.sh dev
```

**What it does:**
1. Starts PostgreSQL container via docker-compose
2. Waits for database to be ready
3. Runs backend with `./gradlew bootRun`

### `dev db`

Start only PostgreSQL database.

```bash
./scripts/chatkeep.sh dev db
```

**Use case:** When you want to run the backend manually or in your IDE.

### `dev app` / `dev backend`

Start only the backend application (assumes database is already running).

```bash
./scripts/chatkeep.sh dev app
```

### `dev mini-app` / `dev frontend`

Start only the Mini App development server (Vite).

```bash
./scripts/chatkeep.sh dev mini-app
```

**Features:**
- Automatically kills existing Vite instances on port 5173
- Installs npm dependencies if needed
- Runs on http://localhost:5173

### `dev all` / `dev full`

Start complete development stack: database + backend + frontend.

```bash
./scripts/chatkeep.sh dev all
```

**What it does:**
1. Starts PostgreSQL container
2. Starts backend in background (logs to `/tmp/chatkeep-backend.log`)
3. Starts Mini App dev server in foreground
4. Press Ctrl+C to stop all services

**Output:**
```
Full stack started!
  Backend:  http://localhost:8080
  Mini App: http://localhost:5173
  Swagger:  http://localhost:8080/swagger-ui.html
```

### `dev build`

Build the backend JAR file.

```bash
./scripts/chatkeep.sh dev build
```

Runs: `./gradlew build`

### `dev build-mini-app`

Build Mini App for production.

```bash
./scripts/chatkeep.sh dev build-mini-app
```

**What it does:**
1. Installs npm dependencies if needed
2. Runs `npm run build`
3. Output: `mini-app/dist/`

### `dev docker`

Build and run everything via Docker Compose.

```bash
./scripts/chatkeep.sh dev docker
```

**What it does:**
1. Builds Mini App
2. Runs `docker compose up --build`

### `dev test`

Run backend tests.

```bash
./scripts/chatkeep.sh dev test
```

Runs: `./gradlew test`

### `dev stop`

Stop all development services.

```bash
./scripts/chatkeep.sh dev stop
```

**What it does:**
- Stops all Docker containers
- Kills processes on ports 8080 and 5173

### `dev logs`

View Docker container logs.

```bash
./scripts/chatkeep.sh dev logs
```

Runs: `docker compose logs -f`

### `dev clean`

Clean build artifacts and Docker volumes.

```bash
./scripts/chatkeep.sh dev clean
```

**What it does:**
- Runs `./gradlew clean`
- Removes `mini-app/dist/`
- Runs `docker compose down -v`

### `dev tunnel`

Start cloudflared tunnel to expose Mini App.

```bash
./scripts/chatkeep.sh dev tunnel
```

**Prerequisites:**
- Install cloudflared: `brew install cloudflared`
- Mini App dev server must be running on http://localhost:5173

**Use case:** Testing Telegram Mini App with real bot (requires HTTPS).

### `dev status`

Show development environment status.

```bash
./scripts/chatkeep.sh dev status
```

**Output example:**
```
Service Status:

  PostgreSQL: ✓ running
  Backend:    ✓ running (http://localhost:8080)
  Mini App:   ✓ running (http://localhost:5173)
  Build:      ✓ exists (2.5M)
```

## Production Commands

### `prod deploy`

Deploy to remote server via SSH.

```bash
./scripts/chatkeep.sh prod deploy \
  --host <hostname> \
  --user <username> \
  [--password <password> | --key <ssh-key-path>]
```

**Required flags:**
- `--host`, `-h` - Server hostname or IP address
- `--user`, `-u` - SSH username

**Authentication options:**
- `--password`, `-p` - SSH password (requires `sshpass`)
- `--key`, `-k` - SSH private key path

**What it does on remote server:**
1. SSH to server
2. Navigate to deployment directory (default: `/root/chatkeep`)
3. Pull latest code from git
4. Pull latest Docker images
5. Restart containers with docker-compose
6. Wait for health checks
7. Clean up old images
8. Show container status

**Example:**
```bash
# Password authentication
./scripts/chatkeep.sh prod deploy \
  --host 89.125.243.104 \
  --user root \
  --password mySecurePassword

# SSH key authentication
./scripts/chatkeep.sh prod deploy \
  --host example.com \
  --user deploy \
  --key ~/.ssh/deploy_rsa
```

### `prod build`

Build Docker image locally.

```bash
./scripts/chatkeep.sh prod build
```

**Configuration:**
- Image: `$REGISTRY/$IMAGE_NAME:$IMAGE_TAG`
- Default: `ghcr.io/chatkeep:latest`

### `prod push`

Build and push Docker image to registry.

```bash
./scripts/chatkeep.sh prod push
```

**Prerequisites:**
- Docker registry authentication: `docker login ghcr.io`

### `prod pull`

Pull latest Docker image from registry.

```bash
./scripts/chatkeep.sh prod pull
```

### `prod up`

Start production containers locally.

```bash
./scripts/chatkeep.sh prod up
```

**Prerequisites:**
- Required environment variables must be set
- Uses `docker-compose.prod.yml`

### `prod down`

Stop production containers.

```bash
./scripts/chatkeep.sh prod down
```

### `prod restart`

Pull latest images and restart production containers.

```bash
./scripts/chatkeep.sh prod restart
```

**What it does:**
1. Pull latest images
2. Restart containers
3. Wait for health checks (60 seconds)
4. Clean up old images

### `prod status`

Show production container status.

```bash
./scripts/chatkeep.sh prod status
```

Runs: `docker compose -f docker-compose.prod.yml ps`

### `prod logs`

View production container logs.

```bash
./scripts/chatkeep.sh prod logs
```

Runs: `docker compose -f docker-compose.prod.yml logs -f`

## Environment Variables

### Development

Optional (loaded from `.env` if exists):

```bash
# Telegram Bot
TELEGRAM_BOT_TOKEN=your_bot_token_from_botfather

# Database (defaults work for local development)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=chatkeep
DB_USERNAME=chatkeep
DB_PASSWORD=chatkeep

# Application
LOG_LEVEL=DEBUG
```

### Production

Required for production deployment:

```bash
# Telegram Bot (required)
TELEGRAM_BOT_TOKEN=your_production_bot_token

# Database (required)
DB_PASSWORD=secure_production_password

# Security (required)
JWT_SECRET=your_jwt_secret_key

# Optional
DB_NAME=chatkeep
DB_USERNAME=chatkeep
LOG_LEVEL=INFO
MINI_APP_URL=https://your-domain.com
```

### Docker Configuration

```bash
# Container Registry
REGISTRY=ghcr.io              # default: ghcr.io
IMAGE_NAME=chatkeep           # default: chatkeep
IMAGE_TAG=latest              # default: latest

# Deployment
DEPLOY_PATH=/root/chatkeep    # default: /root/chatkeep
```

### Environment File

Create `.env` in project root:

```bash
# Development .env
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
DB_PASSWORD=dev_password
JWT_SECRET=dev_jwt_secret_change_in_production
LOG_LEVEL=DEBUG
```

## Deployment Workflows

### Local Development Workflow

1. **First time setup:**
   ```bash
   # Install dependencies
   cd mini-app && npm install && cd ..

   # Start full stack
   ./scripts/chatkeep.sh dev all
   ```

2. **Daily development:**
   ```bash
   # Start database and backend
   ./scripts/chatkeep.sh dev start

   # In another terminal: start frontend
   ./scripts/chatkeep.sh dev mini-app
   ```

3. **Before commit:**
   ```bash
   # Run tests
   ./scripts/chatkeep.sh dev test

   # Build to verify
   ./scripts/chatkeep.sh dev build
   ```

### Production Deployment Workflow

#### Option 1: Remote SSH Deployment

```bash
# 1. Ensure .env or environment variables are set
export TELEGRAM_BOT_TOKEN=your_token
export DB_PASSWORD=your_password
export JWT_SECRET=your_secret

# 2. Deploy to remote server
./scripts/chatkeep.sh prod deploy \
  --host 89.125.243.104 \
  --user root \
  --password YOUR_SSH_PASSWORD

# 3. Monitor logs (on remote server)
ssh root@89.125.243.104 "cd /root/chatkeep && docker compose -f docker-compose.prod.yml logs -f"
```

#### Option 2: Manual Docker Workflow

```bash
# 1. Build image
./scripts/chatkeep.sh prod build

# 2. Push to registry
docker login ghcr.io
./scripts/chatkeep.sh prod push

# 3. On production server
ssh user@server
cd /root/chatkeep
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

#### Option 3: Local Production Testing

```bash
# 1. Set production environment variables
cp .env.example .env.prod
# Edit .env.prod with production values

# 2. Load environment
source .env.prod

# 3. Start production stack locally
./scripts/chatkeep.sh prod up

# 4. Test
curl http://localhost:80/health

# 5. Stop when done
./scripts/chatkeep.sh prod down
```

### CI/CD Integration

Example GitHub Actions workflow:

```yaml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build and push
        run: |
          docker login ghcr.io -u ${{ github.actor }} -p ${{ secrets.GITHUB_TOKEN }}
          ./scripts/chatkeep.sh prod push

      - name: Deploy to server
        run: |
          ./scripts/chatkeep.sh prod deploy \
            --host ${{ secrets.SERVER_HOST }} \
            --user ${{ secrets.SERVER_USER }} \
            --key ${{ secrets.SSH_PRIVATE_KEY }}
```

## Troubleshooting

### Database Connection Issues

**Problem:** Backend can't connect to PostgreSQL

**Solution:**
```bash
# Check if PostgreSQL is running
docker ps | grep chatkeep-db

# Check PostgreSQL logs
docker logs chatkeep-db

# Restart database
docker restart chatkeep-db

# Verify connection
docker exec chatkeep-db pg_isready -U chatkeep -d chatkeep
```

### Port Already in Use

**Problem:** Port 8080 or 5173 already in use

**Solution:**
```bash
# Find and kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Find and kill process on port 5173
lsof -ti:5173 | xargs kill -9

# Or use the stop command
./scripts/chatkeep.sh dev stop
```

### Backend Fails to Start

**Problem:** Backend exits immediately

**Solution:**
```bash
# Check logs when running in background
tail -f /tmp/chatkeep-backend.log

# Run in foreground to see errors
./scripts/chatkeep.sh dev app

# Check environment variables
env | grep -E 'DB_|TELEGRAM_'

# Verify database is running
./scripts/chatkeep.sh dev status
```

### Docker Daemon Not Running

**Problem:** "Error: Docker daemon is not running"

**Solution:**
```bash
# macOS
open -a Docker

# Linux
sudo systemctl start docker

# Verify
docker info
```

### Mini App Build Fails

**Problem:** npm build fails

**Solution:**
```bash
# Clean and reinstall dependencies
cd mini-app
rm -rf node_modules package-lock.json
npm install
npm run build

# Or use clean command
./scripts/chatkeep.sh dev clean
cd mini-app && npm install && cd ..
./scripts/chatkeep.sh dev build-mini-app
```

### Remote Deployment Fails

**Problem:** SSH connection or deployment fails

**Solutions:**

1. **Test SSH connection:**
   ```bash
   ssh -v user@host
   ```

2. **Check sshpass installation (for password auth):**
   ```bash
   brew install sshpass  # macOS
   apt-get install sshpass  # Ubuntu
   ```

3. **Verify remote directory exists:**
   ```bash
   ssh user@host "ls -la /root/chatkeep"
   ```

4. **Check remote Docker:**
   ```bash
   ssh user@host "docker info"
   ```

### Health Check Timeout

**Problem:** Container health checks never pass

**Solution:**
```bash
# Check container logs
./scripts/chatkeep.sh prod logs

# Check individual service health
docker exec chatkeep-app wget -q -O- http://localhost:8080/actuator/health

# Increase timeout in script (edit chatkeep.sh)
# wait_for_health "application" 120  # Increase from 60 to 120
```

### Permission Denied

**Problem:** "Permission denied" when running script

**Solution:**
```bash
# Make script executable
chmod +x scripts/chatkeep.sh
chmod +x scripts/deploy.sh
chmod +x scripts/dev.sh
chmod +x scripts/mini-app-helper.sh
```

## Backward Compatibility

Old scripts still work but forward to `chatkeep.sh`:

```bash
# These still work:
./scripts/deploy.sh restart
./scripts/dev.sh start
./scripts/mini-app-helper.sh all

# But these are preferred:
./scripts/chatkeep.sh prod restart
./scripts/chatkeep.sh dev start
./scripts/chatkeep.sh dev all
```

## Advanced Usage

### Custom Docker Registry

```bash
# Use different registry
export REGISTRY=docker.io/myuser
./scripts/chatkeep.sh prod build
./scripts/chatkeep.sh prod push
```

### Custom Deployment Path

```bash
# Deploy to different directory
export DEPLOY_PATH=/opt/chatkeep
./scripts/chatkeep.sh prod deploy --host server --user deploy
```

### Build Specific Tag

```bash
# Build specific version
export IMAGE_TAG=v1.2.3
./scripts/chatkeep.sh prod build
./scripts/chatkeep.sh prod push
```

### Parallel Development

```bash
# Terminal 1: Database
./scripts/chatkeep.sh dev db

# Terminal 2: Backend
./scripts/chatkeep.sh dev app

# Terminal 3: Frontend
./scripts/chatkeep.sh dev mini-app

# Terminal 4: Logs
./scripts/chatkeep.sh dev logs
```

## Getting Help

```bash
# Main help
./scripts/chatkeep.sh help

# Development mode help
./scripts/chatkeep.sh dev help

# Production mode help
./scripts/chatkeep.sh prod help
```

## Contributing

When adding new commands:

1. Add function to appropriate section (dev_* or prod_*)
2. Add case statement entry in main router
3. Update help text
4. Document in this README
5. Test both direct call and via legacy wrappers
