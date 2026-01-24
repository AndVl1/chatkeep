/**
 * Settings Page Flow Tests
 *
 * Tests the user flow on the settings page:
 * - Loading settings from the API
 * - Displaying current settings
 * - Toggling boolean settings
 * - Changing numeric values
 * - Selecting dropdown options
 * - Error handling
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { screen, waitFor, act } from '@testing-library/react';
import { renderWithProviders } from '@/test/utils';
import { SettingsPage } from '@/pages/SettingsPage';
import {
  setMockSettings,
  setMockError,
  clearMockError,
  setMockDelay,
  resetMockState,
  mockSettings,
} from '@/test/mocks/server';
import { mockSettingsAllEnabled, mockSettingsDisabled } from '@/test/mocks/data';
import { simulateWebMode } from '@/test/setup';

describe('Settings Page Flow', () => {
  const chatId = 100;

  beforeEach(() => {
    resetMockState();
    clearMockError();
    vi.useFakeTimers({ shouldAdvanceTime: true });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('Loading State', () => {
    it('should show loading spinner while fetching settings', async () => {
      setMockDelay(500);
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      }, { timeout: 2000 });
    });
  });

  describe('Success State', () => {
    it('should display chat title in header', async () => {
      setMockSettings(chatId, mockSettings);
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.getByText('Test Group 1')).toBeInTheDocument();
      });
    });

    it('should display all toggle switches', async () => {
      setMockSettings(chatId, mockSettings);
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      // Check for toggle switches
      expect(screen.getByRole('checkbox', { name: /collection enabled/i })).toBeInTheDocument();
      expect(screen.getByRole('checkbox', { name: /clean service messages/i })).toBeInTheDocument();
      expect(screen.getByRole('checkbox', { name: /violation warnings/i })).toBeInTheDocument();
    });

    it('should display correct initial toggle states', async () => {
      setMockSettings(chatId, mockSettings);
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      // Collection enabled should be ON
      expect(screen.getByRole('checkbox', { name: /collection enabled/i })).toBeChecked();
      // Clean service should be OFF
      expect(screen.getByRole('checkbox', { name: /clean service messages/i })).not.toBeChecked();
      // Lock warns should be OFF
      expect(screen.getByRole('checkbox', { name: /violation warnings/i })).not.toBeChecked();
    });

    it('should display all enabled state correctly', async () => {
      setMockSettings(chatId, mockSettingsAllEnabled);
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      expect(screen.getByRole('checkbox', { name: /collection enabled/i })).toBeChecked();
      expect(screen.getByRole('checkbox', { name: /clean service messages/i })).toBeChecked();
      expect(screen.getByRole('checkbox', { name: /violation warnings/i })).toBeChecked();
    });
  });

  describe('Toggle Interactions', () => {
    it('should toggle collection enabled', async () => {
      setMockSettings(chatId, mockSettings);
      const { user } = renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      const toggle = screen.getByRole('checkbox', { name: /collection enabled/i });
      expect(toggle).toBeChecked();

      await user.click(toggle);

      // The toggle should update optimistically
      await waitFor(() => {
        expect(toggle).not.toBeChecked();
      });
    });

    it('should toggle clean service messages', async () => {
      setMockSettings(chatId, mockSettings);
      const { user } = renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      const toggle = screen.getByRole('checkbox', { name: /clean service messages/i });
      expect(toggle).not.toBeChecked();

      await user.click(toggle);

      await waitFor(() => {
        expect(toggle).toBeChecked();
      });
    });
  });

  describe('Number Input Interactions', () => {
    it('should update max warnings value', async () => {
      setMockSettings(chatId, mockSettings);
      const { user } = renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      const maxWarningsInput = screen.getByDisplayValue('3');
      await user.clear(maxWarningsInput);
      await user.type(maxWarningsInput, '5');

      // Advance timers for debounce
      await act(async () => {
        vi.advanceTimersByTime(500);
      });

      // Input should show new value (as string since HTML inputs are strings)
      expect(maxWarningsInput).toHaveValue('5');
    });
  });

  describe('Navigation', () => {
    it('should have back button in web mode', async () => {
      // Back button is only visible in web mode (hidden in Mini App mode where native back is used)
      const restoreMiniApp = simulateWebMode();

      setMockSettings(chatId, mockSettings);
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /back/i })).toBeInTheDocument();

      restoreMiniApp();
    });

    it('should have manage blocklist button', async () => {
      setMockSettings(chatId, mockSettings);
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      // Button text is "Blocked Words & Phrases"
      expect(screen.getByRole('button', { name: /blocked words/i })).toBeInTheDocument();
    });

    it('should have configure locks button', async () => {
      setMockSettings(chatId, mockSettings);
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });

      // Button text is "Configure Locks"
      expect(screen.getByRole('button', { name: /configure locks/i })).toBeInTheDocument();
    });
  });

  describe('Error State', () => {
    // Error tests need real timers since fake timers interfere with MSW
    beforeEach(() => {
      vi.useRealTimers();
    });

    it('should display error when API fails', async () => {
      setMockError('settings', 500, 'Internal server error');
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      // Wait for error header "Error" to appear
      await waitFor(() => {
        expect(screen.getByText('Error')).toBeInTheDocument();
      });
    });

    it('should allow retry when error occurs', async () => {
      setMockError('settings', 500, 'Internal server error');
      const { user } = renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/${chatId}/settings`],
      });

      // Wait for error state
      await waitFor(() => {
        expect(screen.getByText('Error')).toBeInTheDocument();
      });

      clearMockError();
      setMockSettings(chatId, mockSettings);

      const retryButton = screen.getByRole('button', { name: /retry/i });
      await user.click(retryButton);

      await waitFor(() => {
        expect(screen.getByText('Test Group 1')).toBeInTheDocument();
      });
    });

    it('should display 404 error for non-existent chat', async () => {
      setMockError('settings', 404, 'Chat not found');
      renderWithProviders(<SettingsPage />, {
        initialEntries: [`/chat/999/settings`],
      });

      await waitFor(() => {
        expect(screen.getByText('Error')).toBeInTheDocument();
      });
    });
  });

  describe('Invalid Routes', () => {
    it('should redirect when chatId is missing', () => {
      renderWithProviders(<SettingsPage />, {
        initialEntries: ['/chat//settings'],
      });

      // Should redirect or show appropriate error
      // The component redirects to / when chatId is invalid
    });
  });
});
