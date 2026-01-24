/**
 * Mini App Mode Flow Tests
 *
 * Tests that verify correct behavior when running inside Telegram Mini App:
 * - Back button is hidden (native Telegram back button is used instead)
 * - Pages load and display correctly
 * - Basic elements are visible
 *
 * Note: By default, test/setup.ts mocks window.Telegram.WebApp with initData,
 * which means we're testing Mini App mode behavior.
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@/test/utils';

// Pages
import { SettingsPage } from '@/pages/SettingsPage';
import { BlocklistPage } from '@/pages/BlocklistPage';
import { LocksPage } from '@/pages/LocksPage';
import { ChannelReplyPage } from '@/pages/ChannelReplyPage';

// Mock setup
import {
  setMockSettings,
  setMockBlocklist,
  setMockLocks,
  setMockChannelReply,
  resetMockState,
  mockSettings,
  mockLocks,
  mockBlocklistPatterns,
} from '@/test/mocks/server';
import type { ChannelReply } from '@/types';

describe('Mini App Mode - Back Button Hidden', () => {
  const chatId = 100;

  const mockChannelReply: ChannelReply = {
    enabled: false,
    replyText: null,
    mediaFileId: null,
    mediaType: null,
    buttons: [],
  };

  beforeEach(() => {
    resetMockState();
    // Note: Telegram WebApp is mocked by default in test/setup.ts
    // This means we're in Mini App mode where back button should be hidden
  });

  describe('Settings Page', () => {
    it('should load without back button in Mini App mode', async () => {
      setMockSettings(chatId, mockSettings);
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      // Back button should NOT be present in Mini App mode
      expect(screen.queryByRole('button', { name: /back/i })).not.toBeInTheDocument();

      // But page content should be visible
      expect(screen.getByText('Test Group 1')).toBeInTheDocument();
      expect(screen.getByRole('checkbox', { name: /collection enabled/i })).toBeInTheDocument();
    });

    it('should display all navigation buttons', async () => {
      setMockSettings(chatId, mockSettings);
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      // Navigation buttons should be present
      expect(screen.getByRole('button', { name: /blocked words/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /configure locks/i })).toBeInTheDocument();
    });
  });

  describe('Blocklist Page', () => {
    it('should load without back button in Mini App mode', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      // Back button should NOT be present in Mini App mode
      expect(screen.queryByRole('button', { name: /back/i })).not.toBeInTheDocument();

      // But page content should be visible
      expect(screen.getAllByText('Blocklist').length).toBeGreaterThan(0);
      expect(screen.getByRole('button', { name: /add pattern/i })).toBeInTheDocument();
    });

    it('should display blocklist patterns', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      // Patterns should be visible
      expect(screen.getByText('spam')).toBeInTheDocument();
    });
  });

  describe('Locks Page', () => {
    it('should load without back button in Mini App mode', async () => {
      setMockLocks(chatId, mockLocks);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      // Back button should NOT be present in Mini App mode
      expect(screen.queryByRole('button', { name: /back/i })).not.toBeInTheDocument();

      // But page content should be visible
      expect(screen.getByText('Configure Locks')).toBeInTheDocument();
      expect(screen.getAllByRole('checkbox').length).toBeGreaterThan(0);
    });

    it('should display lock toggles', async () => {
      setMockLocks(chatId, mockLocks);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      // Lock types should be visible
      expect(screen.getAllByText(/photo/i).length).toBeGreaterThan(0);
      expect(screen.getAllByText(/video/i).length).toBeGreaterThan(0);
    });
  });

  describe('Channel Reply Page', () => {
    it('should load without back button in Mini App mode', async () => {
      setMockChannelReply(chatId, mockChannelReply);
      renderWithProviders(<ChannelReplyPage />, {
        initialEntries: [`/chat/${chatId}/channel-reply`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      }, { timeout: 3000 });

      // Back button should NOT be present in Mini App mode
      expect(screen.queryByRole('button', { name: /back/i })).not.toBeInTheDocument();

      // But page content should be visible
      await waitFor(() => {
        expect(screen.getByRole('checkbox', { name: /enable/i })).toBeInTheDocument();
      });
    });

    it('should display enable toggle and message input', async () => {
      setMockChannelReply(chatId, mockChannelReply);
      renderWithProviders(<ChannelReplyPage />, {
        initialEntries: [`/chat/${chatId}/channel-reply`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      }, { timeout: 3000 });

      // Form elements should be visible
      await waitFor(() => {
        expect(screen.getByRole('checkbox', { name: /enable/i })).toBeInTheDocument();
        expect(screen.getByRole('textbox')).toBeInTheDocument();
      });
    });
  });
});
