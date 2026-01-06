# Chatkeep

Telegram-бот для модерации групповых чатов и сохранения истории сообщений.

## Возможности

- **Сбор сообщений** — автоматическое сохранение истории групповых чатов в PostgreSQL
- **Модерация** — warn/mute/ban/kick с настраиваемыми порогами
- **Блоклисты** — фильтрация по словам/regex с действиями (delete/warn/mute/ban)
- **Локи (49 типов)** — запрет определённого контента (фото, видео, ссылки, forward и др.)
- **Канальные ответы** — автоматические ответы на посты в каналах с медиа и кнопками
- **Лог-канал** — логирование действий модерации в отдельный канал
- **Админ-сессии** — управление чатом через личные сообщения с ботом

## Технологии

- Kotlin 2.2.21 / Spring Boot 4.0.1
- KTgBotAPI 22.0.0
- PostgreSQL + Flyway + Spring Data JDBC

## Быстрый старт

```bash
# Клонировать и настроить
cp .env.example .env
# Добавить TELEGRAM_BOT_TOKEN в .env

# Запустить (PostgreSQL + приложение)
./scripts/dev.sh docker
```

Подробнее о настройке бота: [CLAUDE.md](CLAUDE.md)

## Mini App (Telegram Web Interface)

Web-интерфейс для управления настройками бота через Telegram Mini Apps.

### Возможности Mini App

- **Выбор чата** из списка ваших админских чатов
- **Настройки модерации**: лимит предупреждений, TTL, действия при превышении
- **Управление локами**: 47 типов блокировок по 6 категориям
  - General (общие), Media (медиа), Messages (сообщения)
  - Links (ссылки), Bots (боты), Advanced (расширенные)
- **Блоклист**: CRUD для паттернов с автодетектом wildcard/regex
- **Интуитивный UI**: на базе Telegram UI Kit

### Запуск Mini App

**Полный стек одной командой:**
```bash
./scripts/dev.sh all
```

**Или по отдельности (разные терминалы):**
```bash
# Терминал 1: База данных
./scripts/dev.sh db

# Терминал 2: Backend (Spring Boot)
./scripts/dev.sh app

# Терминал 3: Frontend (Vite dev server)
./scripts/dev.sh mini-app
```

### Production сборка

```bash
# Собрать Mini App для продакшена
./scripts/dev.sh build-mini-app

# Запустить весь стек через Docker Compose (db + app + nginx с mini-app)
./scripts/dev.sh docker
```

После запуска:
- **Backend API**: http://localhost:8080
- **Mini App** (dev): http://localhost:5173
- **Mini App** (production): http://localhost:3000

### Настройка в Telegram

1. Откройте @BotFather
2. Выберите вашего бота → **Bot Settings** → **Menu Button**
3. Установите URL вашего Mini App
4. Теперь в чате с ботом будет кнопка меню для открытия интерфейса

Подробное руководство: [VERIFICATION.md](VERIFICATION.md)

## Команды бота

### Основные (личные сообщения)

| Команда | Описание |
|---------|----------|
| `/start` | Справка |
| `/mychats` | Список чатов, где вы админ |
| `/connect [chat_id]` | Подключиться к чату для управления |
| `/disconnect` | Отключиться от чата |

### Модерация (в группе или через /connect)

| Команда | Описание |
|---------|----------|
| `/warn @user [причина]` | Выдать предупреждение |
| `/unwarn @user` | Снять предупреждение |
| `/mute @user [время]` | Замутить (1h, 1d, 1w) |
| `/unmute @user` | Размутить |
| `/ban @user [время]` | Забанить |
| `/unban @user` | Разбанить |
| `/kick @user` | Кикнуть |

### Блоклисты

| Команда | Описание |
|---------|----------|
| `/addblock <слово> [действие]` | Добавить в блоклист (delete/warn/mute/ban) |
| `/delblock <слово>` | Удалить из блоклиста |
| `/blocklist` | Показать блоклист |

### Локи

| Команда | Описание |
|---------|----------|
| `/lock <тип>` | Заблокировать тип контента |
| `/unlock <тип>` | Разблокировать |
| `/locks` | Показать активные локи |
| `/locktypes` | Список всех типов локов |
| `/lockwarns on/off` | Включить варны за нарушение локов |

### Канальные ответы

| Команда | Описание |
|---------|----------|
| `/setreply <текст>` | Установить авто-ответ на посты |
| `/delreply` | Удалить авто-ответ |
| `/showreply` | Показать текущий ответ |
| `/replymedia` | Добавить медиа к ответу (ответом на файл) |
| `/clearmedia` | Удалить медиа |
| `/replybutton <текст> <url>` | Добавить кнопку |
| `/clearbuttons` | Удалить кнопки |
| `/replyenable` / `/replydisable` | Вкл/выкл ответы |

### Логирование

| Команда | Описание |
|---------|----------|
| `/setlogchannel @channel` | Установить лог-канал |
| `/unsetlogchannel` | Отключить логирование |
| `/logchannel` | Показать текущий лог-канал |
| `/viewlogs [дни]` | Экспорт логов в JSON |
| `/cleanservice on/off` | Авто-удаление сервисных сообщений |

## Структура проекта

```
src/main/kotlin/ru/andvl/chatkeep/
├── bot/
│   ├── handlers/
│   │   ├── moderation/     # Варны, баны, блоклисты
│   │   ├── locks/          # Система локов
│   │   └── channelreply/   # Канальные ответы
│   └── util/               # Парсеры, утилиты
├── domain/
│   ├── model/              # Сущности
│   └── service/            # Бизнес-логика
└── infrastructure/
    └── repository/         # Репозитории
```

## Деплой

```bash
export TELEGRAM_BOT_TOKEN=your_token
export DB_PASSWORD=secure_password
./scripts/deploy.sh up
```

## Лицензия

MIT
