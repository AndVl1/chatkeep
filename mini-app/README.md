# Chatkeep Mini App

Telegram Mini App for configuring Chatkeep bot settings.

## Features

- Chat selection from user's admin chats
- Settings management (warnings, thresholds, actions)
- Lock configuration (47 lock types across 6 categories)
- Blocklist pattern management
- Real-time updates with optimistic UI

## Tech Stack

- React 18
- TypeScript 5
- Vite 6
- @telegram-apps/sdk-react
- @telegram-apps/ui
- Zustand (state management)
- ky (HTTP client)
- React Router

## Development

```bash
# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Environment Variables

Create `.env.development` and `.env.production`:

```env
VITE_API_URL=/api/v1/miniapp
```

## Project Structure

```
src/
├── api/              # API client and endpoints
├── components/       # React components
│   ├── common/       # Reusable components
│   ├── layout/       # Layout components
│   ├── chats/        # Chat selector
│   ├── settings/     # Settings forms
│   ├── locks/        # Lock configuration
│   └── blocklist/    # Blocklist management
├── hooks/            # Custom hooks
│   ├── api/          # Data fetching hooks
│   ├── telegram/     # Telegram SDK hooks
│   └── ui/           # UI state hooks
├── pages/            # Route pages
├── stores/           # Zustand stores
├── types/            # TypeScript types
├── utils/            # Utilities
└── App.tsx           # App root
```

## Usage

1. Open the bot in Telegram
2. Navigate to Settings
3. Select a chat you're admin in
4. Configure settings, locks, or blocklist
5. Save changes with the main button

## Authentication

The app uses Telegram's initData for authentication:
- Frontend sends `Authorization: tma {initDataRaw}` header
- Backend validates the signature using bot token
- User identity is verified through Telegram
