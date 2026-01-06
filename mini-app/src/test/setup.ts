import '@testing-library/jest-dom';
import { expect, afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';

// Cleanup after each test
afterEach(() => {
  cleanup();
});

// Mock Telegram WebApp
global.window = global.window || ({} as any);

// Event listeners storage
const eventListeners: Record<string, Array<(event: any) => void>> = {};

(global.window as any).Telegram = {
  WebApp: {
    initData: 'mock-init-data',
    initDataUnsafe: {
      user: {
        id: 123456,
        first_name: 'Test',
        last_name: 'User',
        username: 'testuser',
        language_code: 'en',
        is_premium: false,
      },
    },
    ready: vi.fn(),
    expand: vi.fn(),
    close: vi.fn(),
    platform: 'tdesktop',
    version: '7.0',
    colorScheme: 'light',
    isExpanded: true,
    viewportHeight: 600,
    viewportStableHeight: 600,
    headerColor: '#ffffff',
    backgroundColor: '#ffffff',
    themeParams: {
      bg_color: '#ffffff',
      text_color: '#000000',
      button_color: '#5288c1',
      button_text_color: '#ffffff',
      hint_color: '#999999',
      link_color: '#5288c1',
      secondary_bg_color: '#f0f0f0',
      section_bg_color: '#ffffff',
      section_header_text_color: '#6d6d72',
      subtitle_text_color: '#999999',
      destructive_text_color: '#ff3b30',
    },
    MainButton: {
      setText: vi.fn(),
      show: vi.fn(),
      hide: vi.fn(),
      onClick: vi.fn(),
      offClick: vi.fn(),
      showProgress: vi.fn(),
      hideProgress: vi.fn(),
    },
    BackButton: {
      show: vi.fn(),
      hide: vi.fn(),
      onClick: vi.fn(),
      offClick: vi.fn(),
    },
    onEvent: vi.fn((eventType: string, callback: (event: any) => void) => {
      if (!eventListeners[eventType]) {
        eventListeners[eventType] = [];
      }
      eventListeners[eventType].push(callback);
    }),
    offEvent: vi.fn((eventType: string, callback: (event: any) => void) => {
      if (eventListeners[eventType]) {
        const index = eventListeners[eventType].indexOf(callback);
        if (index > -1) {
          eventListeners[eventType].splice(index, 1);
        }
      }
    }),
  },
};

// Mock environment variables
vi.stubEnv('VITE_API_URL', 'http://localhost:8080/api/v1/miniapp');
