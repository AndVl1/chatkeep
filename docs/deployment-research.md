# Deployment Research: Chatkeep

> Исследование подготовки к первому продакшен деплою
> Дата: 2026-01-07

---

## Executive Summary

**Готовность к продакшену: 70%**

Проект имеет готовую Docker инфраструктуру и CI/CD pipeline, но требует исправления критических проблем перед релизом.

| Аспект | Статус | Комментарий |
|--------|--------|-------------|
| Docker/Compose | ✅ 95% | Multi-stage build, health checks |
| CI/CD | ✅ 90% | GitHub Actions готов |
| База данных | ✅ 100% | 10 Flyway миграций |
| Безопасность | ⚠️ 70% | Нужен rate limiting |
| Мониторинг | ❌ 0% | Требуется настройка |

---

## 1. Рекомендации по серверу

### Минимальные требования

Для нагрузки ~1700 участников, ~100 сообщений/день:

| Параметр | Минимум | Рекомендуется |
|----------|---------|---------------|
| CPU | 1 vCPU | 2 vCPU |
| RAM | 2 GB | 4 GB |
| Диск | 20 GB SSD | 40 GB NVMe |
| Сеть | 1 TB/мес | 2+ TB/мес |

### Расчёт использования RAM

| Компонент | RAM |
|-----------|-----|
| Spring Boot + JVM | ~512 MB |
| PostgreSQL | ~256 MB |
| Nginx | ~50 MB |
| Мониторинг (Netdata) | ~100 MB |
| **Итого** | **~1 GB** |

С 4 GB RAM остается 3x запас на рост.

---

## 2. Сравнение VPS провайдеров

### Рекомендация: Hetzner Cloud CX22

| Провайдер | План | CPU | RAM | Диск | Цена |
|-----------|------|-----|-----|------|------|
| **Hetzner** | CX22 | 2 vCPU | 4 GB | 40 GB NVMe | **~$5.50/мес** |
| DigitalOcean | Basic | 2 vCPU | 4 GB | 80 GB SSD | $28/мес |
| Vultr | Performance | 2 vCPU | 4 GB | 80 GB SSD | $18/мес |
| Timeweb | Cloud 4 | 2 vCPU | 4 GB | 60 GB SSD | ~$9/мес |
| Selectel | SL1.2-002-004 | 2 vCPU | 4 GB | 40 GB SSD | ~$8/мес |
| Linode | 4GB | 2 vCPU | 4 GB | 80 GB SSD | $24/мес |

**Почему Hetzner:**
- Лучшее соотношение цена/качество
- NVMe SSD включен
- 20 TB трафика
- Надежная репутация

**Альтернатива для России:** Timeweb Cloud (~$9/мес) - если нужны российские платежи.

---

## 3. Стратегия деплоя

### Рекомендация: Docker Compose

| Вариант | Рекомендуется? | Причина |
|---------|----------------|---------|
| **Docker Compose** | ✅ Да | Уже настроен, простой деплой |
| Systemd | ❌ Нет | Больше ручной работы, нет изоляции |
| Kubernetes | ❌ Нет | Overkill для вашей нагрузки |

**Существующие файлы:**
- `docker-compose.yml` - для разработки
- `docker-compose.prod.yml` - для продакшена
- `Dockerfile` - multi-stage build

### Deploy Script

```bash
#!/bin/bash
# /opt/chatkeep/deploy.sh

set -e

cd /opt/chatkeep

# Pull latest images
docker compose -f docker-compose.prod.yml pull

# Stop old containers
docker compose -f docker-compose.prod.yml down

# Start new containers
docker compose -f docker-compose.prod.yml up -d

# Cleanup
docker image prune -f

echo "Deployment complete!"
```

---

## 4. Необходимые компоненты

### 4.1 SSL/TLS (Let's Encrypt)

```bash
# Установка
apt install certbot python3-certbot-nginx

# Получение сертификата
certbot --nginx -d chatkeep.example.com

# Авто-обновление уже настроено через systemd timer
```

**Стоимость:** Бесплатно

### 4.2 Nginx Reverse Proxy

```nginx
# /etc/nginx/sites-available/chatkeep

server {
    listen 80;
    server_name chatkeep.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name chatkeep.example.com;

    ssl_certificate /etc/letsencrypt/live/chatkeep.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/chatkeep.example.com/privkey.pem;

    # Mini App
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
    }

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Actuator (только localhost)
    location /actuator/ {
        allow 127.0.0.1;
        deny all;
        proxy_pass http://localhost:8080/actuator/;
    }
}
```

### 4.3 PostgreSQL Backups

```bash
#!/bin/bash
# /opt/chatkeep/backup.sh

BACKUP_DIR="/opt/chatkeep/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=7

# Создать бэкап
docker exec chatkeep-db pg_dump -U chatkeep chatkeep | \
  gzip > "$BACKUP_DIR/chatkeep_$TIMESTAMP.sql.gz"

# Удалить старые
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete
```

**Cron:** `0 3 * * * /opt/chatkeep/backup.sh`

### 4.4 Мониторинг

**Рекомендация для начала:** Netdata (легковесный)

```bash
bash <(curl -Ss https://get.netdata.cloud/kickstart.sh)
```

- RAM: ~100 MB
- Настройка: автоматическая
- UI: красивый, real-time

**В будущем:** Prometheus + Grafana для детальных метрик.

---

## 5. Переменные окружения

### Backend (.env)

```bash
# ОБЯЗАТЕЛЬНЫЕ
TELEGRAM_BOT_TOKEN=            # От @BotFather
DB_PASSWORD=                    # Минимум 16 символов
JWT_SECRET=                     # Минимум 32 символа

# База данных
DB_HOST=db
DB_PORT=5432
DB_NAME=chatkeep
DB_USERNAME=chatkeep

# Опциональные
LOG_LEVEL=INFO
MINI_APP_URL=https://your-domain.com/
JWT_EXPIRATION_HOURS=24
```

**Генерация секретов:**
```bash
# JWT_SECRET
openssl rand -base64 48

# DB_PASSWORD
openssl rand -base64 32
```

### Frontend (.env.production)

```bash
VITE_API_URL=https://api.your-domain.com/api/v1/miniapp
VITE_BOT_USERNAME=your_bot_username
```

---

## 6. Критические проблемы (исправить до релиза)

### 6.1 Отсутствует Rate Limiting

**Проблема:** API endpoints уязвимы для brute force и DDoS.

**Решение:** Добавить Bucket4j или Spring Rate Limiter.

```kotlin
// build.gradle.kts
implementation("com.bucket4j:bucket4j-core:8.7.0")
```

**Приоритет:** HIGH
**Оценка:** 1 день

### 6.2 Database Connection Pool не настроен

**Проблема:** Дефолтный HikariCP (10 соединений) может быть мал.

**Решение:** Добавить в `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

**Приоритет:** HIGH
**Оценка:** 30 минут

### 6.3 Hardcoded ngrok URL

**Файл:** `application.yml:21`

```yaml
mini-app:
  url: ${MINI_APP_URL:https://unmicrobial-unparticularising-shonta.ngrok-free.dev/}
```

**Проблема:** Dev URL как дефолт сломает продакшен.

**Решение:** Убрать дефолтное значение, требовать явную настройку.

**Приоритет:** MEDIUM
**Оценка:** 5 минут

### 6.4 Console.log в Frontend

**Файлы:** 13 вхождений в `/mini-app/src/`

**Проблема:** Утечка внутренней логики, шум в консоли.

**Решение:** Удалить все `console.log`, добавить proper error reporting.

**Приоритет:** MEDIUM
**Оценка:** 30 минут

---

## 7. Рекомендуемые улучшения

| Улучшение | Приоритет | Время |
|-----------|-----------|-------|
| Структурированные логи (JSON) | Medium | 2-3 часа |
| Prometheus метрики | Medium | 4-6 часов |
| Frontend тесты | Medium | 2-3 дня |
| Graceful shutdown для бота | Low | 2-3 часа |

---

## 8. Итоговая стоимость

| Компонент | Ежемесячно |
|-----------|------------|
| Hetzner CX22 VPS | $5.50 |
| Домен (.com) | ~$1 (годовой/12) |
| SSL сертификат | $0 (Let's Encrypt) |
| Мониторинг | $0 (Netdata) |
| Бэкапы | $0 (локально) |
| **Итого** | **~$6.50/мес** |

---

## 9. Checklist перед деплоем

### Сервер

- [ ] Арендовать VPS (Hetzner CX22)
- [ ] Установить Docker и Docker Compose
- [ ] Настроить SSH ключи
- [ ] Открыть порты 80, 443

### Конфигурация

- [ ] Сгенерировать JWT_SECRET
- [ ] Сгенерировать DB_PASSWORD
- [ ] Получить TELEGRAM_BOT_TOKEN
- [ ] Создать .env файл на сервере

### Код (исправить)

- [ ] Добавить rate limiting
- [ ] Настроить connection pool
- [ ] Убрать ngrok URL из дефолтов
- [ ] Удалить console.log из frontend

### Деплой

- [ ] Настроить Nginx reverse proxy
- [ ] Получить SSL сертификат
- [ ] Запустить docker-compose.prod.yml
- [ ] Настроить cron для бэкапов

### Проверка

- [ ] Проверить /actuator/health
- [ ] Проверить Mini App в Telegram
- [ ] Проверить все команды бота
- [ ] Настроить мониторинг

---

## 10. Quick Start

```bash
# 1. На сервере
apt update && apt install -y docker.io docker-compose-plugin nginx certbot python3-certbot-nginx

# 2. Клонировать
git clone https://github.com/your-repo/chatkeep.git /opt/chatkeep
cd /opt/chatkeep

# 3. Настроить окружение
cp .env.example .env
nano .env  # Заполнить переменные

# 4. Запустить
docker compose -f docker-compose.prod.yml up -d

# 5. Настроить SSL
certbot --nginx -d chatkeep.example.com

# 6. Настроить бэкапы
mkdir -p /opt/chatkeep/backups
crontab -e  # Добавить: 0 3 * * * /opt/chatkeep/backup.sh
```

---

**Исследование выполнено:** tech-researcher, devops, analyst agents
**Дата:** 2026-01-07
