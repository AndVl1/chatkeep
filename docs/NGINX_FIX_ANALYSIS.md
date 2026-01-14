# Nginx Routing Fix - Root Cause Analysis

## Problem
Grafana and Prometheus subdomains were serving the Compose Multiplatform Admin app instead of their respective services.

## Root Cause

### The Bug
The GitHub Actions deploy workflow was calling `bash scripts/deploy.sh` without any arguments.

```yaml
# OLD (BROKEN) workflow
- name: Restart services
  script: |
    cd ${{ vars.DEPLOY_PATH || '~/chatkeep' }}
    bash scripts/deploy.sh  # <-- NO ARGUMENTS!
    docker exec chatkeep-nginx nginx -s reload
```

### Why This Failed
1. `scripts/deploy.sh` forwards all arguments to `scripts/chatkeep.sh prod "$@"`
2. With no arguments, `chatkeep.sh prod` defaults to showing help text
3. The help command prints usage info and exits
4. **The actual deploy commands never ran!**
5. Docker containers were never restarted
6. Nginx configs were never reloaded

### What Should Have Happened
The workflow should either:
- Call `bash scripts/deploy.sh restart` to use the prod restart function
- Call `docker-compose up -d` directly to restart containers

## The Fix

### Changed Workflow Step
```yaml
# NEW (FIXED) workflow
- name: Restart services
  script: |
    cd ${{ vars.DEPLOY_PATH || '~/chatkeep' }}

    # Restart docker compose services (picks up new nginx configs)
    echo "Restarting services..."
    docker compose -f docker-compose.prod.yml up -d

    # Reload nginx to ensure new configuration is active
    echo "Reloading nginx..."
    docker exec chatkeep-nginx nginx -t && docker exec chatkeep-nginx nginx -s reload

    # Cleanup old images
    docker image prune -f
```

### Why This Works
1. `git pull` (step 1) pulls updated nginx configs from repo
2. `docker-compose up -d`:
   - Recreates containers if config changed
   - Mounts new nginx configs from host
   - Restarts nginx container with fresh config
3. `nginx -t && nginx -s reload` ensures config is active

## Deployment Flow

### Correct Flow (After Fix)
1. Build WASM artifact (GitHub Actions)
2. Build and push Docker image (GitHub Actions)
3. SSH to server
4. `git pull` - get latest code including nginx configs
5. `docker pull` - get latest app image
6. Copy WASM files to server
7. `docker-compose up -d` - restart services with new configs
8. `nginx -s reload` - reload nginx configuration

### What Each Step Does
- **git pull**: Updates `/root/chatkeep/docker/nginx/sites/` configs
- **docker pull**: Gets latest `ghcr.io/andvl1/chatkeep:latest`
- **Copy WASM**: Updates `/root/chatkeep/chatkeep-admin/...`
- **docker-compose up -d**:
  - Reads `docker-compose.prod.yml`
  - Sees volume mounts changed (git pull)
  - Recreates nginx container with new config mounts
  - Starts all services
- **nginx reload**: Ensures nginx reads new configs

## Verification

### After Deploy Runs
Test that subdomains route correctly:

```bash
# Should return Grafana HTML, NOT "ChatKeep Admin"
curl -skL https://grafana.chatmoderatorbot.ru | head -20

# Should return "401 Unauthorized" (basic auth prompt)
curl -skI https://prometheus.chatmoderatorbot.ru | grep "HTTP/"

# Admin app should still work
curl -skI https://admin.chatmoderatorbot.ru | grep "HTTP/"
```

### Expected Results
- `grafana.chatmoderatorbot.ru` → Grafana UI (proxy to port 3000)
- `prometheus.chatmoderatorbot.ru` → 401 Auth Required (proxy to port 9090)
- `admin.chatmoderatorbot.ru` → ChatKeep Admin (static files)

## Files Modified

- `.github/workflows/deploy.yml` - Fixed restart step to actually run commands

## Next Steps

1. Trigger deploy workflow manually or push to production branch
2. Monitor workflow run
3. Verify subdomain routing works correctly
4. Test Grafana login
5. Test Prometheus basic auth
