# Chatkeep

Telegram-бот для сохранения истории сообщений групповых чатов.

## Возможности

- Автоматический сбор текстовых сообщений из групповых чатов
- Хранение сообщений в PostgreSQL с информацией об авторе
- Управление ботом через личные сообщения (для админов чатов)
- Включение/отключение сбора для отдельных чатов
- Просмотр статистики по чатам

## Технологии

- Kotlin 2.2.21
- Spring Boot 4.0.1
- KTgBotAPI 22.0.0
- PostgreSQL + Flyway
- Spring Data JDBC

## Быстрый старт

### 1. Создать бота

1. Написать [@BotFather](https://t.me/BotFather) в Telegram
2. Создать нового бота: `/newbot`
3. Отключить Privacy Mode: `/setprivacy` → Disable (чтобы бот видел все сообщения)
4. Сохранить токен

### 2. Запуск с Docker (рекомендуется)

```bash
# Скопировать и настроить переменные окружения
cp .env.example .env
# Отредактировать .env, добавить TELEGRAM_BOT_TOKEN

# Запустить всё (PostgreSQL + приложение)
./scripts/dev.sh docker
```

### 3. Запуск без Docker

```bash
# Запустить только PostgreSQL в Docker
./scripts/dev.sh db

# Установить переменные окружения
export TELEGRAM_BOT_TOKEN=your_bot_token

# Запустить приложение
./scripts/dev.sh app
```

### Доступные команды dev.sh

| Команда | Описание |
|---------|----------|
| `./scripts/dev.sh start` | Запустить PostgreSQL + приложение |
| `./scripts/dev.sh db` | Только PostgreSQL |
| `./scripts/dev.sh app` | Только приложение |
| `./scripts/dev.sh test` | Запустить тесты |
| `./scripts/dev.sh build` | Собрать проект |
| `./scripts/dev.sh docker` | Собрать и запустить в Docker |
| `./scripts/dev.sh stop` | Остановить контейнеры |
| `./scripts/dev.sh clean` | Очистить всё |

## Команды бота

| Команда | Описание |
|---------|----------|
| `/start` | Справка |
| `/mychats` | Список чатов, где вы админ |
| `/stats <chat_id>` | Статистика чата |
| `/enable <chat_id>` | Включить сбор сообщений |
| `/disable <chat_id>` | Отключить сбор сообщений |

## Деплой

### Production с Docker Compose

```bash
# На сервере
export TELEGRAM_BOT_TOKEN=your_token
export DB_PASSWORD=secure_password

# Запустить
./scripts/deploy.sh up

# Или pull + restart
./scripts/deploy.sh restart
```

### Доступные команды deploy.sh

| Команда | Описание |
|---------|----------|
| `./scripts/deploy.sh build` | Собрать Docker образ |
| `./scripts/deploy.sh push` | Собрать и отправить в registry |
| `./scripts/deploy.sh pull` | Скачать последний образ |
| `./scripts/deploy.sh up` | Запустить production |
| `./scripts/deploy.sh down` | Остановить |
| `./scripts/deploy.sh restart` | Pull + restart |
| `./scripts/deploy.sh logs` | Логи контейнеров |

## CI/CD

GitHub Actions автоматически:

- **На Pull Request**: сборка и тесты
- **На push в main**: сборка Docker образа и push в ghcr.io

### Настройка деплоя

1. В репозитории: **Settings → Variables → Repository variables**
2. Добавить `DEPLOY_ENABLED=true` для сборки Docker образов
3. (Опционально) Для автодеплоя на сервер:
   - `DEPLOY_HOST` - адрес сервера
   - `DEPLOY_USER` - пользователь SSH
   - `DEPLOY_PATH` - путь к проекту (по умолчанию `~/chatkeep`)
   - Секрет `DEPLOY_SSH_KEY` - приватный SSH ключ

## Структура проекта

```
src/main/kotlin/ru/andvl/chatkeep/
├── bot/                    # Telegram bot
│   ├── ChatkeepBot.kt      # Главный сервис бота
│   └── handlers/           # Обработчики сообщений
├── domain/                 # Бизнес-логика
│   ├── model/              # Сущности
│   └── service/            # Сервисы
└── infrastructure/         # Инфраструктура
    └── repository/         # Репозитории
```

## Лицензия

MIT
