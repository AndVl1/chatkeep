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

### 2. Настроить базу данных

```bash
createdb chatkeep
```

### 3. Установить переменные окружения

```bash
export TELEGRAM_BOT_TOKEN=your_bot_token
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=chatkeep
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
```

### 4. Запустить

```bash
./gradlew bootRun
```

## Команды бота

| Команда | Описание |
|---------|----------|
| `/start` | Справка |
| `/mychats` | Список чатов, где вы админ |
| `/stats <chat_id>` | Статистика чата |
| `/enable <chat_id>` | Включить сбор сообщений |
| `/disable <chat_id>` | Отключить сбор сообщений |

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
