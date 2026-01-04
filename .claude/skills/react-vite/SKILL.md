---
name: react-vite
description: React 18+ with Vite patterns - use for Mini App frontend development, component structure, hooks, and TypeScript setup
---

# React + Vite Patterns

## Project Setup

### Initialize Project

```bash
npm create vite@latest mini-app -- --template react-ts
cd mini-app
npm install
```

### Install Dependencies

```bash
# Telegram Mini Apps SDK
npm install @telegram-apps/sdk @telegram-apps/sdk-react @telegram-apps/ui

# Routing
npm install react-router-dom

# State management (optional)
npm install zustand

# HTTP client
npm install ky

# Development
npm install -D @types/react @types/react-dom
```

## Project Structure

```
src/
├── components/
│   ├── common/           # Reusable UI components
│   │   ├── Button.tsx
│   │   └── Card.tsx
│   ├── settings/         # Feature-specific components
│   │   ├── LockToggle.tsx
│   │   ├── BlocklistItem.tsx
│   │   └── ChatSelector.tsx
│   └── layout/
│       ├── AppLayout.tsx
│       └── Navigation.tsx
├── hooks/
│   ├── useTelegramAuth.ts
│   ├── useChats.ts
│   └── useSettings.ts
├── pages/
│   ├── HomePage.tsx
│   ├── SettingsPage.tsx
│   └── BlocklistPage.tsx
├── services/
│   └── api.ts            # API client
├── stores/
│   └── settingsStore.ts  # Zustand store
├── types/
│   └── index.ts          # TypeScript types
├── App.tsx
├── main.tsx
└── vite-env.d.ts
```

## Component Patterns

### Functional Component with TypeScript

```tsx
interface ChatCardProps {
  chat: Chat;
  onSelect: (chatId: number) => void;
  isActive?: boolean;
}

export function ChatCard({ chat, onSelect, isActive = false }: ChatCardProps) {
  return (
    <div
      className={`chat-card ${isActive ? 'active' : ''}`}
      onClick={() => onSelect(chat.id)}
    >
      <h3>{chat.title}</h3>
      <span>{chat.memberCount} members</span>
    </div>
  );
}
```

### Page Component Pattern

```tsx
import { useParams, useNavigate } from 'react-router-dom';
import { useSettings } from '../hooks/useSettings';
import { Section, Cell, Switch, Spinner } from '@telegram-apps/ui';

export function SettingsPage() {
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const { settings, isLoading, updateSetting } = useSettings(Number(chatId));

  if (isLoading) {
    return <Spinner size="large" />;
  }

  if (!settings) {
    return <Placeholder>Chat not found</Placeholder>;
  }

  return (
    <div className="settings-page">
      <Section header="General">
        <Cell
          after={
            <Switch
              checked={settings.collectionEnabled}
              onChange={(checked) => updateSetting('collectionEnabled', checked)}
            />
          }
        >
          Message Collection
        </Cell>
        <Cell
          after={
            <Switch
              checked={settings.cleanServiceEnabled}
              onChange={(checked) => updateSetting('cleanServiceEnabled', checked)}
            />
          }
        >
          Clean Service Messages
        </Cell>
      </Section>
    </div>
  );
}
```

## Hooks Patterns

### Custom Hook with API Call

```tsx
import { useState, useEffect, useCallback } from 'react';
import { api } from '../services/api';

export function useChats() {
  const [chats, setChats] = useState<Chat[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetchChats = async () => {
      try {
        setIsLoading(true);
        const data = await api.getChats();
        setChats(data);
      } catch (err) {
        setError(err as Error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchChats();
  }, []);

  const refreshChats = useCallback(async () => {
    const data = await api.getChats();
    setChats(data);
  }, []);

  return { chats, isLoading, error, refreshChats };
}
```

### Hook with Optimistic Updates

```tsx
export function useSettings(chatId: number) {
  const [settings, setSettings] = useState<ChatSettings | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    api.getSettings(chatId).then(setSettings).finally(() => setIsLoading(false));
  }, [chatId]);

  const updateSetting = useCallback(async <K extends keyof ChatSettings>(
    key: K,
    value: ChatSettings[K]
  ) => {
    // Optimistic update
    setSettings(prev => prev ? { ...prev, [key]: value } : null);

    try {
      await api.updateSettings(chatId, { [key]: value });
    } catch (error) {
      // Rollback on error
      api.getSettings(chatId).then(setSettings);
      throw error;
    }
  }, [chatId]);

  return { settings, isLoading, updateSetting };
}
```

### Telegram Auth Hook

```tsx
import { useInitDataRaw, useInitData } from '@telegram-apps/sdk-react';

export function useTelegramAuth() {
  const initDataRaw = useInitDataRaw();
  const initData = useInitData();

  const user = initData?.user;
  const isAuthenticated = !!user && !!initDataRaw;

  const getAuthHeaders = useCallback(() => ({
    'Authorization': `tma ${initDataRaw}`,
    'Content-Type': 'application/json',
  }), [initDataRaw]);

  return {
    user,
    isAuthenticated,
    getAuthHeaders,
    userId: user?.id,
    isPremium: user?.isPremium ?? false,
  };
}
```

## State Management (Zustand)

```tsx
import { create } from 'zustand';

interface SettingsState {
  selectedChatId: number | null;
  settings: Record<number, ChatSettings>;
  setSelectedChat: (chatId: number) => void;
  updateSettings: (chatId: number, settings: Partial<ChatSettings>) => void;
}

export const useSettingsStore = create<SettingsState>((set) => ({
  selectedChatId: null,
  settings: {},

  setSelectedChat: (chatId) => set({ selectedChatId: chatId }),

  updateSettings: (chatId, newSettings) =>
    set((state) => ({
      settings: {
        ...state.settings,
        [chatId]: { ...state.settings[chatId], ...newSettings },
      },
    })),
}));
```

## API Client Pattern

```tsx
import ky from 'ky';

const API_BASE = import.meta.env.VITE_API_URL || '/api/v1';

let authHeaders: Record<string, string> = {};

export function setAuthHeaders(headers: Record<string, string>) {
  authHeaders = headers;
}

const client = ky.extend({
  prefixUrl: API_BASE,
  hooks: {
    beforeRequest: [
      (request) => {
        Object.entries(authHeaders).forEach(([key, value]) => {
          request.headers.set(key, value);
        });
      },
    ],
  },
});

export const api = {
  getChats: () => client.get('miniapp/chats').json<Chat[]>(),

  getSettings: (chatId: number) =>
    client.get(`miniapp/chats/${chatId}/settings`).json<ChatSettings>(),

  updateSettings: (chatId: number, settings: Partial<ChatSettings>) =>
    client.put(`miniapp/chats/${chatId}/settings`, { json: settings }).json<ChatSettings>(),

  getBlocklist: (chatId: number) =>
    client.get(`miniapp/chats/${chatId}/blocklist`).json<BlocklistPattern[]>(),

  addBlocklistPattern: (chatId: number, pattern: CreatePatternRequest) =>
    client.post(`miniapp/chats/${chatId}/blocklist`, { json: pattern }).json<BlocklistPattern>(),

  deleteBlocklistPattern: (chatId: number, patternId: number) =>
    client.delete(`miniapp/chats/${chatId}/blocklist/${patternId}`),
};
```

## Vite Configuration

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import basicSsl from '@vitejs/plugin-basic-ssl';

export default defineConfig({
  plugins: [
    react(),
    basicSsl(), // Enable HTTPS for local development
  ],
  server: {
    host: true, // Expose to network
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
  base: './', // For relative paths
});
```

## Routing Pattern

```tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { HomePage } from './pages/HomePage';
import { SettingsPage } from './pages/SettingsPage';
import { BlocklistPage } from './pages/BlocklistPage';
import { LocksPage } from './pages/LocksPage';

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/chat/:chatId">
            <Route path="settings" element={<SettingsPage />} />
            <Route path="blocklist" element={<BlocklistPage />} />
            <Route path="locks" element={<LocksPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
```

## TypeScript Types

```typescript
// types/index.ts
export interface Chat {
  id: number;
  title: string;
  type: 'group' | 'supergroup' | 'channel';
  memberCount: number;
}

export interface ChatSettings {
  chatId: number;
  collectionEnabled: boolean;
  cleanServiceEnabled: boolean;
  maxWarnings: number;
  warningTtlHours: number;
  thresholdAction: PunishmentType;
  thresholdDurationHours: number;
  defaultBlocklistAction: PunishmentType;
  logChannelId: number | null;
}

export type PunishmentType = 'NOTHING' | 'WARN' | 'MUTE' | 'BAN' | 'KICK';

export interface BlocklistPattern {
  id: number;
  pattern: string;
  matchType: 'EXACT' | 'WILDCARD';
  action: PunishmentType;
  createdAt: string;
}

export interface LockSettings {
  lockedTypes: Record<LockType, LockInfo>;
  lockwarnsEnabled: boolean;
}

export interface LockInfo {
  locked: boolean;
  reason?: string;
}

export type LockType =
  | 'PHOTO' | 'VIDEO' | 'GIF' | 'AUDIO' | 'VOICE'
  | 'DOCUMENT' | 'STICKER' | 'POLL' | 'CONTACT' | 'LOCATION'
  | 'VENUE' | 'GAME' | 'URL' | 'EMAIL' | 'PHONE' | 'HASHTAG'
  | 'MENTION' | 'BOT_COMMAND' | 'SPOILER' | 'DICE' | 'PREMIUM_EMOJI'
  // ... etc
;
```

## Mock Environment Setup

```tsx
// src/mockEnv.ts
import { mockTelegramEnv } from '@telegram-apps/sdk-react';

export function setupMockEnvironment() {
  if (import.meta.env.DEV && !window.Telegram?.WebApp) {
    mockTelegramEnv({
      themeParams: {
        bgColor: '#17212b',
        textColor: '#f5f5f5',
        hintColor: '#708499',
        linkColor: '#6ab3f3',
        buttonColor: '#5288c1',
        buttonTextColor: '#ffffff',
        secondaryBgColor: '#232e3c',
        headerBgColor: '#17212b',
        accentTextColor: '#6ab2f2',
        sectionBgColor: '#17212b',
        sectionHeaderTextColor: '#6ab3f3',
        subtitleTextColor: '#708499',
        destructiveTextColor: '#ec3942',
      },
      initData: {
        user: {
          id: 99281932,
          firstName: 'Test',
          lastName: 'User',
          username: 'testuser',
          languageCode: 'en',
          isPremium: true,
          allowsWriteToPm: true,
        },
        hash: 'mock-hash-value',
        authDate: new Date(),
        startParam: 'debug',
        chatType: 'sender',
        chatInstance: '8428209589180549439',
      },
      version: '7.2',
      platform: 'tdesktop',
    });
    console.log('Mock Telegram environment initialized');
  }
}

// main.tsx
import { setupMockEnvironment } from './mockEnv';
setupMockEnvironment();
```

## Best Practices

1. **Use TypeScript** - Full type safety for API responses and props
2. **Organize by feature** - Group related components, hooks, and types
3. **Use custom hooks** - Extract reusable logic
4. **Optimistic updates** - Better UX for settings changes
5. **Error boundaries** - Wrap pages in error boundaries
6. **Lazy loading** - Use `React.lazy()` for route-based code splitting
7. **CSS Variables** - Use Telegram theme CSS variables for consistent styling
