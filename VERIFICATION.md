# Mini App Verification Guide

Подробная инструкция по тестированию Telegram Mini App для Chatkeep.

## Предварительные требования

- **Docker** и **Docker Compose** установлены
- **Node.js** 18+ (для локальной разработки)
- **Telegram бот токен** (получить у @BotFather)
- Telegram аккаунт для тестирования

## Локальная разработка

### 1. Запуск Backend

Вариант A - Полный стек одной командой:
```bash
./scripts/dev.sh all
```

Вариант B - По отдельности (разные терминалы):
```bash
# Терминал 1: База данных
./scripts/dev.sh db

# Терминал 2: Spring Boot приложение
./scripts/dev.sh app

# Терминал 3: Mini App (Vite dev server)
./scripts/dev.sh mini-app
```

### 2. Проверка запуска

После запуска должны быть доступны:
- **Backend API**: http://localhost:8080
- **Mini App Dev Server**: http://localhost:5173
- **PostgreSQL**: localhost:5432

Проверить health:
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:5173
```

### 3. Открыть в браузере

Откройте http://localhost:5173 - должен работать мок Telegram окружения для разработки.

## Чек-лист тестирования

### Chat Selection (Выбор чата)

- [ ] Список чатов загружается
- [ ] Можно выбрать чат из списка
- [ ] Выбранный чат сохраняется при навигации между страницами
- [ ] Если чат не выбран, показывается подсказка
- [ ] Иконки и названия чатов отображаются корректно

### Settings Page (Настройки)

- [ ] Настройки загружаются для выбранного чата
- [ ] **Collection enabled** - переключатель работает
- [ ] **Clean service messages** - переключатель работает
- [ ] **Max warnings** - можно изменить (1-10)
- [ ] **Warning TTL** - можно выбрать период (1 hour - 1 year)
- [ ] **Threshold action** - выбор действия (mute/ban/kick)
- [ ] **Default blocklist action** - выбор действия
- [ ] Кнопка **Save** работает
- [ ] После сохранения показывается уведомление об успехе
- [ ] При ошибке показывается сообщение об ошибке

### Locks Page (Блокировки)

- [ ] Отображаются все 6 категорий табов:
  - General (общие)
  - Media (медиа)
  - Messages (сообщения)
  - Links (ссылки)
  - Bots (боты)
  - Advanced (расширенные)
- [ ] Локи загружаются для выбранного чата
- [ ] Можно переключать индивидуальные локи
- [ ] Переключатель **Lock warns** работает (глобально)
- [ ] Кнопка **Save** сохраняет все изменения (bulk update)
- [ ] Описание локов показывается при наведении/долгом нажатии
- [ ] Переключение между табами работает плавно

### Blocklist Page (Блоклист)

- [ ] Список паттернов загружается для выбранного чата
- [ ] Можно добавить новый паттерн
- [ ] При добавлении паттерна автоматически определяется тип (wildcard/regex)
- [ ] Можно выбрать действие для паттерна (delete/warn/mute/ban)
- [ ] Можно удалить паттерн (свайп или кнопка)
- [ ] Wildcard паттерны (с *) корректно помечаются
- [ ] Regex паттерны корректно работают
- [ ] Пустой блоклист показывает соответствующее сообщение

## Тестирование на реальном устройстве

### 1. Настройка туннеля

Для доступа к локальному dev-серверу из Telegram используйте туннель:

```bash
# Установить cloudflared (macOS)
brew install cloudflared

# Запустить туннель
cloudflared tunnel --url http://localhost:5173
```

Cloudflared выдаст публичный URL (например: `https://random-name.trycloudflare.com`)

Альтернативы:
- **ngrok**: `ngrok http 5173`
- **localtunnel**: `npx localtunnel --port 5173`

### 2. Настройка бота

1. Откройте @BotFather в Telegram
2. Выберите вашего бота (`/mybots` → выбрать бота)
3. Нажмите **Bot Settings** → **Menu Button**
4. **Configure Menu Button**
5. Введите URL: `https://your-tunnel-url.trycloudflare.com`
6. Введите название: "Settings" или "Настройки"

### 3. Открыть Mini App

1. Откройте чат с вашим ботом в Telegram
2. Нажмите на кнопку меню (иконка рядом с полем ввода)
3. Mini App должен открыться во весь экран

### 4. Проверка аутентификации

При открытии Mini App в Telegram:
- В консоли браузера должен отображаться `initData`
- Backend должен успешно валидировать initData
- Логи должны показать успешную аутентификацию

Проверить логи backend:
```bash
docker logs chatkeep-app -f
# или
tail -f /tmp/chatkeep-backend.log  # если запущено через ./scripts/dev.sh all
```

## Production деплой

### 1. Сборка для продакшена

```bash
# Собрать Mini App
./scripts/dev.sh build-mini-app

# Запустить весь стек через Docker Compose
./scripts/dev.sh docker
```

После сборки:
- Mini App будет в `mini-app/dist/`
- Nginx будет раздавать статику из этой папки
- Mini App доступен на http://localhost:3000

### 2. Проверка production сборки

```bash
# Проверить контейнеры
docker compose ps

# Должны быть запущены:
# - chatkeep-db (PostgreSQL)
# - chatkeep-app (Spring Boot)
# - chatkeep-mini-app (Nginx)

# Проверить логи
docker logs chatkeep-mini-app

# Проверить доступность
curl http://localhost:3000
curl http://localhost:3000/api/health  # проксируется на backend
```

### 3. Настройка для продакшена

Обновите `.env.production` в `mini-app/`:
```env
VITE_API_BASE_URL=https://your-domain.com/api
```

Пересоберите:
```bash
cd mini-app
npm run build
```

## Troubleshooting (Решение проблем)

### API ошибки

**Проблема**: Запросы к API возвращают 401 Unauthorized

**Решение**:
```bash
# Проверить логи backend
docker logs chatkeep-app -f

# Проверить, что initData корректно передается
# В консоли браузера должно быть:
console.log(window.Telegram.WebApp.initData)
```

**Проблема**: CORS ошибки

**Решение**:
- В dev-режиме: используйте Vite proxy (настроен в `vite.config.ts`)
- В production: используйте nginx proxy (настроен в `docker/nginx/mini-app.conf`)
- Проверьте CORS конфигурацию в Spring Boot

### Ошибки аутентификации

**Проблема**: initData validation failed

**Решение**:
```bash
# Проверить, что BOT_TOKEN корректный
echo $TELEGRAM_BOT_TOKEN

# Проверить срок действия initData (1 час)
# Если прошло больше часа, перезапустите Mini App

# Проверить логи валидации
grep "initData" /tmp/chatkeep-backend.log
```

**Проблема**: В dev-режиме (браузер) нет initData

**Решение**:
- Это нормально! Используется мок окружения
- Для реального тестирования используйте туннель (см. выше)
- Можно добавить hardcoded мок initData в dev-режиме

### Build ошибки

**Проблема**: `npm run build` падает с ошибкой TypeScript

**Решение**:
```bash
cd mini-app

# Очистить и переустановить зависимости
rm -rf node_modules package-lock.json
npm install

# Проверить TypeScript
npx tsc --noEmit

# Собрать заново
npm run build
```

**Проблема**: Docker контейнер mini-app не стартует

**Решение**:
```bash
# Проверить, что dist существует
ls -la mini-app/dist/

# Если нет, собрать:
./scripts/dev.sh build-mini-app

# Проверить nginx логи
docker logs chatkeep-mini-app

# Перезапустить контейнер
docker compose restart mini-app
```

### Проблемы с данными

**Проблема**: Не загружаются чаты

**Решение**:
```bash
# Проверить, что бот добавлен в чаты как админ
# В Telegram: добавьте бота в группу и дайте права админа

# Проверить базу данных
docker exec -it chatkeep-db psql -U chatkeep -d chatkeep
# В psql:
SELECT * FROM chat_settings;
```

**Проблема**: Изменения не сохраняются

**Решение**:
- Откройте DevTools → Network и проверьте API запросы
- Убедитесь, что запросы возвращают 200 OK
- Проверьте логи backend на ошибки

## Дополнительные команды

```bash
# Просмотр всех логов
docker compose logs -f

# Только логи Mini App
docker compose logs -f mini-app

# Перезапуск сервисов
docker compose restart

# Полная очистка и перезапуск
docker compose down -v
./scripts/dev.sh clean
./scripts/dev.sh docker

# Проверка размера Docker образов
docker images | grep chatkeep

# Exec в контейнер nginx
docker exec -it chatkeep-mini-app sh

# Внутри контейнера проверить файлы
ls -la /usr/share/nginx/html/
cat /etc/nginx/conf.d/default.conf
```

## Мониторинг production

### Healthchecks

```bash
# Mini App health
curl http://localhost:3000/health

# Backend health
curl http://localhost:8080/actuator/health

# Database
docker exec chatkeep-db pg_isready -U chatkeep
```

### Метрики

Backend предоставляет метрики через Actuator:
```bash
# Prometheus метрики
curl http://localhost:8080/actuator/prometheus

# Информация о приложении
curl http://localhost:8080/actuator/info
```

## Полезные ссылки

- [Telegram Mini Apps Documentation](https://core.telegram.org/bots/webapps)
- [@BotFather](https://t.me/BotFather) - создание и настройка ботов
- [Telegram SDK для React](https://github.com/Telegram-Mini-Apps/telegram-apps)
- [Vite Documentation](https://vitejs.dev/)
- [nginx Configuration](https://nginx.org/en/docs/)

## Контакты для поддержки

При возникновении проблем:
1. Проверьте этот VERIFICATION.md
2. Изучите логи (`docker logs` или `tail -f`)
3. Проверьте issues в репозитории
4. Создайте новый issue с подробным описанием проблемы
