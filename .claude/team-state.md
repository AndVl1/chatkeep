# TEAM STATE

## Classification
- Type: OPS
- Complexity: COMPLEX
- Workflow: FULL 7-PHASE

## Task
Настройка тестовой инфраструктуры с разделением prod/test окружений:
1. Тестовый сервер: 204.77.3.163, chatmodtest.ru
2. CI/CD пайплайны с автовыбором окружения по ветке
3. Параметризация конфигов (домен, IP, bot token, DB)
4. SSL через Let's Encrypt
5. Mobile app: настройка base URL в UI
6. SSH ключи и register-ssh для обоих доменов

## Environment Mapping
| Parameter | Production | Test |
|-----------|------------|------|
| Domain | chatmod.ru | chatmodtest.ru |
| IP | prod IP | 204.77.3.163 |
| Subdomains | api., admin., app. | api., admin., app. |
| Bot Token | PROD_BOT_TOKEN | TEST_BOT_TOKEN |
| SSH Secret | SSH_PRIVATE_KEY | TEST_SSH_TOKEN |
| Database | prod DB | separate test DB |

## Progress
- [x] Phase 1: Discovery - COMPLETED
- [x] Phase 2: Exploration - COMPLETED
- [x] Phase 3: Questions - SKIPPED (all clarified in Phase 1)
- [ ] Phase 4: Architecture - IN PROGRESS
- [ ] Phase 5: Implementation - pending
- [ ] Phase 6: Review - pending
- [ ] Phase 6.5: Review Fixes - pending (optional)
- [ ] Phase 7: Summary - pending

## Phase 1 Output
- Confirmed requirements with user
- Bot tokens already in GitHub secrets (PROD_BOT_TOKEN, TEST_BOT_TOKEN)
- Same subdomain structure for both environments
- Let's Encrypt for SSL
- Separate database on test server
- User going to sleep - working autonomously

## Phase 2 Output

### CI/CD Workflows Found (11 total)
1. `ci.yml` - Build/test for backend, frontend, mobile
2. `deploy.yml` - Production deployment (production branch → prod server)
3. `register-ssh.yml` - Manual SSH key registration
4. `deploy-files-to-server.yml` - Manual deployment with SSL
5. `setup-ssl.yml` - SSL certificate update
6. `update-ssl-cert.yml` - Direct SSL update
7. `fix-nginx-and-ssl.yml` - Diagnostic workflow
8. `init-git-repo.yml` - Initial server setup
9. `create-production-pr.yml` - Automated PR creation
10. `cleanup-caches.yml` - Cache management
11. `debug-permissions.yml` - File permission diagnostics

### Hardcoded Values to Parameterize

**Domains (100+ occurrences):**
- `chatmoderatorbot.ru` (main domain - NOT chatmod.ru as user said!)
- All subdomains: api., miniapp., admin., grafana., prometheus., www.

**Files with hardcoded domains:**
- `.github/workflows/*.yml` - certbot commands
- `docker/nginx/sites/*.conf` - server_name, ssl paths
- `docker-compose.prod.yml` - grafana URLs
- `src/main/kotlin/.../CorsConfig.kt` - CORS origins
- `mini-app/.env.production` - bot username

**Server path:** `/root/chatkeep` (hardcoded in multiple places)

### Current Secrets/Variables
**Secrets:** DEPLOY_SSH_KEY, DB_PASSWORD, JWT_SECRET, TELEGRAM_BOT_TOKEN, TELEGRAM_ADMIN_BOT_TOKEN, GRAFANA_ADMIN_PASSWORD
**Variables:** DEPLOY_ENABLED, DEPLOY_HOST, DEPLOY_USER, DEPLOY_PATH

### Mobile App Architecture
- Settings: DataStore (Android/iOS/Desktop), InMemory (WASM)
- Network: Ktor Client with hardcoded base URL in PlatformFactory
- Current base URL: `https://admin.chatmoderatorbot.ru` (hardcoded)
- Pattern: SettingsRepository → DataStoreSettingsRepository (platform-specific)

### Key Finding: Domain Mismatch!
User said "chatmod.ru" and "chatmodtest.ru" but codebase uses "chatmoderatorbot.ru"!
Need to verify correct domains.

## Key Decisions
- Environment selection: by branch (production → prod, other → test)
- Nginx configs: template-based with envsubst
- Base URL in mobile: add to UserSettings, persist in DataStore

## Files Identified

### CI/CD (need parameterization)
- `.github/workflows/deploy.yml`
- `.github/workflows/deploy-files-to-server.yml`
- `.github/workflows/register-ssh.yml`
- `.github/workflows/setup-ssl.yml`
- `.github/workflows/init-git-repo.yml`

### Backend (hardcoded values)
- `src/main/kotlin/ru/andvl/chatkeep/api/config/CorsConfig.kt`
- `src/main/resources/application.yml`

### Nginx (hardcoded domains)
- `docker/nginx/sites/*.conf` (7 files)

### Docker
- `docker-compose.prod.yml`

### Mobile App (base URL settings)
- `feature/settings/api/src/commonMain/kotlin/.../Settings.kt`
- `feature/settings/impl/src/*/kotlin/.../DataStoreSettingsRepository.kt`
- `feature/settings/impl/src/commonMain/kotlin/.../ui/SettingsScreen.kt`
- `composeApp/src/*/kotlin/.../PlatformFactory.*.kt`

## Chosen Approach
(will be determined after architecture design)

## Recovery
Continue from Phase 4: Architecture design. All exploration complete, questions clarified.
Need to design: 1) CI/CD parameterization, 2) Nginx templates, 3) Mobile settings UI
