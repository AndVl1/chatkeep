# TEAM STATE

## Classification
- Type: OPS
- Complexity: MEDIUM
- Workflow: STANDARD (5 phases)

## Task
Редеплой всего на сервер + унификация скриптов:
1. Унифицировать скрипты в scripts/ (один файл для всего)
2. Создать скрипт полного деплоя с параметрами для credentials
3. Сохранить dev-режим в скриптах
4. Восстановить базовые метрики Grafana (uptime, health, request speed)
5. Актуализировать CI/CD при необходимости
6. Редеплойнуть через новые скрипты (бот, Mini App, мониторинг)
7. Проверить работоспособность всех компонентов
8. Написать документацию

## Server Credentials
- IP: 89.125.243.104
- Login: root
- Password: [provided]

## Branch
fix/deployment-critical-issues (текущая)

## Progress
- [x] Phase 1: Discovery - COMPLETED
- [x] Phase 2: Exploration - COMPLETED
- [x] Phase 3: Questions - COMPLETED
- [x] Phase 5: Implementation - COMPLETED
- [x] Phase 6: Review & Verify - COMPLETED
- [x] Phase 7: Summary - COMPLETED

## Phase 6 Verification Results
- Mini App (http://89.125.243.104): HTTP 200 ✓
- Prometheus (http://89.125.243.104:9090): HTTP 302 ✓
- Grafana (http://89.125.243.104:3000): HTTP 200 ✓
- Backend container: healthy (28 hours uptime)
- Bot: active (processing Prometheus scrapes)
- New dashboard jvm-health.json: provisioned ✓

## Phase 3 Answers
1. **Script structure**: Option A - unified `chatkeep.sh` with modes (dev/prod)
2. **Deploy params**: CLI flags `--host`, `--user`, `--password`
3. **Grafana**: Separate dashboard `jvm-health.json` for infrastructure metrics
4. **Documentation**: `scripts/README.md`

## Phase 2 Output

### Scripts Analysis (analyst agent)
- **deploy.sh** (136 lines) - Production deployment, Docker image management
- **dev.sh** (171 lines) - Development orchestration, local stack
- **mini-app-helper.sh** (280 lines) - Frontend-specific, tunneling
- **60%+ code duplication** in common patterns (colors, env loading, health checks)
- **Missing .env loading** in deploy.sh
- **Recommendation**: Single unified script (chatkeep.sh) with modes

### Grafana Metrics (tech-researcher agent)
- Basic metrics ARE collected but NOT visualized (dashboard only has chatkeep_bot_*)
- Need to ADD panels for: process_uptime_seconds, http_server_requests_*, jvm_memory_*
- **Root cause**: Dashboard was built only for custom business metrics
- **Fix**: Add "Infrastructure" section to chatkeep-bot.json
- **Effort**: LOW - just dashboard JSON changes

### CI/CD & Docker (analyst agent)
- **CI workflow**: Tests on PR/main branch
- **Deploy workflow**: Docker build + SSH deploy to production
- **Production path**: /root/chatkeep on 89.125.243.104
- **Issue**: Mini-app build is manual step (not in Docker image)
- **Monitoring**: Prometheus (9090) + Grafana (3000) on localhost only

## Key Decisions
- (будут после Phase 3)

## Files Identified
### Scripts
- scripts/deploy.sh - Production deployment
- scripts/dev.sh - Development orchestration
- scripts/mini-app-helper.sh - Frontend operations

### Docker/Compose
- docker-compose.yml - Development stack
- docker-compose.prod.yml - Production stack
- docker-compose.monitoring.yml - Prometheus + Grafana
- Dockerfile - Multi-stage backend build
- docker/nginx/mini-app.conf - Frontend proxy config

### CI/CD
- .github/workflows/deploy.yml - Production deployment
- .github/workflows/ci.yml - Tests
- .github/workflows/create-production-pr.yml - Release workflow

### Monitoring
- infra/grafana/provisioning/dashboards/chatkeep-bot.json - Dashboard (needs panels)
- infra/prometheus/prometheus.yml - Scrape config
- infra/grafana/provisioning/datasources/prometheus.yml - Datasource

### Config
- .env.example - Environment template
- src/main/resources/application.yml - App config

## Recovery
Continue from Phase 3. Ask clarifying questions.
