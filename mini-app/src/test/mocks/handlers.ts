/**
 * MSW (Mock Service Worker) handlers for API mocking
 * These handlers intercept network requests during tests
 */

import { http, HttpResponse, delay } from 'msw';
import type { ChatSettings, BlocklistPattern, AddBlocklistPatternRequest, UpdateLocksRequest, LockSettings } from '@/types';
import {
  mockChats,
  mockSettings,
  mockBlocklistPatterns,
  mockLocks,
  mockNewPattern,
} from './data';

const API_BASE = 'http://localhost:8080/api/v1/miniapp';

// Mutable state for tests to manipulate
let chatsData = [...mockChats];
let settingsData: Record<number, ChatSettings> = { [mockSettings.chatId]: mockSettings };
let blocklistData: Record<number, BlocklistPattern[]> = { 100: [...mockBlocklistPatterns] };
let locksData: Record<number, LockSettings> = { 100: { ...mockLocks } };
let nextPatternId = 100;

// Response delay simulation (can be overridden per test)
let responseDelay = 0;

// Error simulation (can be set per test)
let errorState: { endpoint?: string; status?: number; message?: string } | null = null;

// === State Management ===

export function resetMockState() {
  chatsData = [...mockChats];
  settingsData = { [mockSettings.chatId]: { ...mockSettings } };
  blocklistData = { 100: [...mockBlocklistPatterns] };
  locksData = { 100: { ...mockLocks } };
  nextPatternId = 100;
  responseDelay = 0;
  errorState = null;
}

export function setMockDelay(ms: number) {
  responseDelay = ms;
}

export function setMockError(endpoint: string, status: number, message: string) {
  errorState = { endpoint, status, message };
}

export function clearMockError() {
  errorState = null;
}

export function setMockChats(chats: typeof mockChats) {
  chatsData = chats;
}

export function setMockSettings(chatId: number, settings: ChatSettings) {
  settingsData[chatId] = settings;
}

export function setMockBlocklist(chatId: number, patterns: BlocklistPattern[]) {
  blocklistData[chatId] = patterns;
}

export function setMockLocks(chatId: number, locks: LockSettings) {
  locksData[chatId] = locks;
}

// === Helper Functions ===

function checkError(endpoint: string) {
  if (errorState && errorState.endpoint === endpoint) {
    return HttpResponse.json(
      { error: errorState.message, message: errorState.message },
      { status: errorState.status }
    );
  }
  return null;
}

async function maybeDelay() {
  if (responseDelay > 0) {
    await delay(responseDelay);
  }
}

// === Handlers ===

export const handlers = [
  // GET /chats - List all chats
  http.get(`${API_BASE}/chats`, async () => {
    await maybeDelay();
    const error = checkError('chats');
    if (error) return error;
    return HttpResponse.json(chatsData);
  }),

  // GET /chats/:chatId/settings - Get chat settings
  http.get(`${API_BASE}/chats/:chatId/settings`, async ({ params }) => {
    await maybeDelay();
    const error = checkError('settings');
    if (error) return error;

    const chatId = Number(params.chatId);
    const settings = settingsData[chatId];

    if (!settings) {
      return HttpResponse.json(
        { error: 'Chat not found', message: 'Chat not found' },
        { status: 404 }
      );
    }

    return HttpResponse.json(settings);
  }),

  // PUT /chats/:chatId/settings - Update chat settings
  http.put(`${API_BASE}/chats/:chatId/settings`, async ({ params, request }) => {
    await maybeDelay();
    const error = checkError('settings');
    if (error) return error;

    const chatId = Number(params.chatId);
    const updates = await request.json() as Partial<ChatSettings>;

    if (!settingsData[chatId]) {
      settingsData[chatId] = { ...mockSettings, chatId };
    }

    settingsData[chatId] = { ...settingsData[chatId], ...updates };
    return HttpResponse.json(settingsData[chatId]);
  }),

  // GET /chats/:chatId/blocklist - Get blocklist patterns
  http.get(`${API_BASE}/chats/:chatId/blocklist`, async ({ params }) => {
    await maybeDelay();
    const error = checkError('blocklist');
    if (error) return error;

    const chatId = Number(params.chatId);
    const patterns = blocklistData[chatId] || [];
    return HttpResponse.json(patterns);
  }),

  // POST /chats/:chatId/blocklist - Add blocklist pattern
  http.post(`${API_BASE}/chats/:chatId/blocklist`, async ({ params, request }) => {
    await maybeDelay();
    const error = checkError('blocklist');
    if (error) return error;

    const chatId = Number(params.chatId);
    const body = await request.json() as AddBlocklistPatternRequest;

    const newPattern: BlocklistPattern = {
      id: nextPatternId++,
      pattern: body.pattern,
      matchType: body.matchType,
      action: body.action,
      actionDurationMinutes: body.actionDurationMinutes,
      severity: body.severity,
      createdAt: new Date().toISOString(),
    };

    if (!blocklistData[chatId]) {
      blocklistData[chatId] = [];
    }
    blocklistData[chatId].push(newPattern);

    return HttpResponse.json(newPattern, { status: 201 });
  }),

  // DELETE /chats/:chatId/blocklist/:patternId - Delete blocklist pattern
  http.delete(`${API_BASE}/chats/:chatId/blocklist/:patternId`, async ({ params }) => {
    await maybeDelay();
    const error = checkError('blocklist');
    if (error) return error;

    const chatId = Number(params.chatId);
    const patternId = Number(params.patternId);

    if (!blocklistData[chatId]) {
      return HttpResponse.json(
        { error: 'Pattern not found', message: 'Pattern not found' },
        { status: 404 }
      );
    }

    const index = blocklistData[chatId].findIndex(p => p.id === patternId);
    if (index === -1) {
      return HttpResponse.json(
        { error: 'Pattern not found', message: 'Pattern not found' },
        { status: 404 }
      );
    }

    blocklistData[chatId].splice(index, 1);
    return new HttpResponse(null, { status: 204 });
  }),

  // GET /chats/:chatId/locks - Get lock settings
  http.get(`${API_BASE}/chats/:chatId/locks`, async ({ params }) => {
    await maybeDelay();
    const error = checkError('locks');
    if (error) return error;

    const chatId = Number(params.chatId);
    const locks = locksData[chatId];

    if (!locks) {
      return HttpResponse.json(
        { error: 'Chat not found', message: 'Chat not found' },
        { status: 404 }
      );
    }

    return HttpResponse.json(locks);
  }),

  // PUT /chats/:chatId/locks - Update lock settings
  http.put(`${API_BASE}/chats/:chatId/locks`, async ({ params, request }) => {
    await maybeDelay();
    const error = checkError('locks');
    if (error) return error;

    const chatId = Number(params.chatId);
    const updates = await request.json() as UpdateLocksRequest;

    if (!locksData[chatId]) {
      locksData[chatId] = { ...mockLocks, chatId };
    }

    locksData[chatId] = {
      ...locksData[chatId],
      locks: { ...locksData[chatId].locks, ...updates.locks },
      lockWarnsEnabled: updates.lockWarnsEnabled ?? locksData[chatId].lockWarnsEnabled,
    };

    return HttpResponse.json(locksData[chatId]);
  }),
];

export { mockChats, mockSettings, mockBlocklistPatterns, mockLocks, mockNewPattern };
