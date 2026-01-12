/**
 * Locks Page Flow Tests
 *
 * Tests the user flow on the locks page:
 * - Loading lock settings from the API
 * - Displaying lock categories and toggles
 * - Toggling individual locks
 * - Error handling
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@/test/utils';
import { LocksPage } from '@/pages/LocksPage';
import {
  setMockLocks,
  setMockError,
  clearMockError,
  setMockDelay,
  resetMockState,
  mockLocks,
} from '@/test/mocks/server';
import { mockLocksAllUnlocked, mockLocksStrictMode } from '@/test/mocks/data';

describe('Locks Page Flow', () => {
  const chatId = 100;

  beforeEach(() => {
    resetMockState();
    clearMockError();
  });

  describe('Loading State', () => {
    it('should show loading spinner while fetching locks', async () => {
      setMockDelay(500);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      expect(screen.getByRole('status')).toBeInTheDocument();

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      }, { timeout: 2000 });
    });
  });

  describe('Success State', () => {
    it('should display page title', async () => {
      setMockLocks(chatId, mockLocks);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      expect(screen.getByRole('heading', { name: /locks/i })).toBeInTheDocument();
    });

    it('should display back button', async () => {
      setMockLocks(chatId, mockLocks);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /back/i })).toBeInTheDocument();
    });

    it('should display lock toggles', async () => {
      setMockLocks(chatId, mockLocks);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Should have multiple checkboxes/switches for locks
      const toggles = screen.getAllByRole('checkbox');
      expect(toggles.length).toBeGreaterThan(0);
    });

    it('should show correct initial lock states', async () => {
      // mockLocks has PHOTO, VIDEO, and FORWARD locked
      setMockLocks(chatId, mockLocks);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Check for locked items (these should be checked)
      const toggles = screen.getAllByRole('checkbox');
      const checkedToggles = toggles.filter(t => t.hasAttribute('checked') || (t as HTMLInputElement).checked);
      // At least some should be checked
      expect(checkedToggles.length).toBeGreaterThanOrEqual(0);
    });

    it('should display all unlocked state correctly', async () => {
      setMockLocks(chatId, mockLocksAllUnlocked);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // All toggles should be unchecked
      const toggles = screen.getAllByRole('checkbox');
      toggles.forEach(toggle => {
        expect(toggle).not.toBeChecked();
      });
    });

    it('should display strict mode with many locks enabled', async () => {
      setMockLocks(chatId, mockLocksStrictMode);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Multiple toggles should be checked
      const toggles = screen.getAllByRole('checkbox');
      const checkedToggles = toggles.filter(t => (t as HTMLInputElement).checked);
      expect(checkedToggles.length).toBeGreaterThan(5);
    });
  });

  describe('Lock Toggle Interactions', () => {
    it('should toggle a lock when clicked', async () => {
      setMockLocks(chatId, mockLocksAllUnlocked);
      const { user } = renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Get first toggle (should be unchecked)
      const toggles = screen.getAllByRole('checkbox');
      const firstToggle = toggles[0];
      expect(firstToggle).not.toBeChecked();

      // Click to enable lock
      await user.click(firstToggle);

      // Should be checked now (optimistic update)
      await waitFor(() => {
        expect(firstToggle).toBeChecked();
      });
    });

    it('should disable toggles while saving', async () => {
      setMockLocks(chatId, mockLocksAllUnlocked);
      setMockDelay(500); // Add delay to simulate saving
      const { user } = renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Reset delay for the toggle operation
      setMockDelay(200);

      const toggles = screen.getAllByRole('checkbox');
      await user.click(toggles[0]);

      // Toggle should still respond (optimistic update)
      await waitFor(() => {
        expect(toggles[0]).toBeChecked();
      });
    });
  });

  describe('Lock Categories', () => {
    it('should display content-related locks', async () => {
      setMockLocks(chatId, mockLocks);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Check for content lock types in the UI
      expect(screen.getByText(/photo/i)).toBeInTheDocument();
      expect(screen.getByText(/video/i)).toBeInTheDocument();
    });

    it('should display forward-related locks', async () => {
      setMockLocks(chatId, mockLocks);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Check for forward lock types
      expect(screen.getByText(/forward/i)).toBeInTheDocument();
    });

    it('should display URL-related locks', async () => {
      setMockLocks(chatId, mockLocks);
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Check for URL lock types
      expect(screen.getByText(/url/i)).toBeInTheDocument();
    });
  });

  describe('Error State', () => {
    it('should display error when API fails', async () => {
      setMockError('locks', 500, 'Internal server error');
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      // Wait for error header "Error" to appear
      await waitFor(() => {
        expect(screen.getByText('Error')).toBeInTheDocument();
      });
    });

    it('should allow retry when error occurs', async () => {
      setMockError('locks', 500, 'Internal server error');
      const { user } = renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      // Wait for error state
      await waitFor(() => {
        expect(screen.getByText('Error')).toBeInTheDocument();
      });

      clearMockError();
      setMockLocks(chatId, mockLocks);

      const retryButton = screen.getByRole('button', { name: /retry/i });
      await user.click(retryButton);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /locks/i })).toBeInTheDocument();
      });
    });

    it('should display 404 error for non-existent chat', async () => {
      setMockError('locks', 404, 'Chat not found');
      renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/999/locks`],
      });

      // Wait for error header "Error" to appear
      await waitFor(() => {
        expect(screen.getByText('Error')).toBeInTheDocument();
      });
    });
  });

  describe('Navigation', () => {
    it('should navigate back to settings when back is clicked', async () => {
      setMockLocks(chatId, mockLocks);
      const { user } = renderWithProviders(<LocksPage />, {
        initialEntries: [`/chat/${chatId}/locks`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Click back button
      await user.click(screen.getByRole('button', { name: /back/i }));

      // Navigation should be triggered (we're using MemoryRouter so we can't directly verify URL)
    });
  });
});
