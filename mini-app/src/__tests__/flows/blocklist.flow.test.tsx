/**
 * Blocklist Page Flow Tests
 *
 * Tests the user flow on the blocklist page:
 * - Loading blocklist patterns from the API
 * - Displaying the pattern list
 * - Adding new patterns
 * - Deleting patterns with confirmation
 * - Error handling
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import { renderWithProviders } from '@/test/utils';
import { BlocklistPage } from '@/pages/BlocklistPage';
import {
  setMockBlocklist,
  setMockError,
  clearMockError,
  setMockDelay,
  resetMockState,
  mockBlocklistPatterns,
} from '@/test/mocks/server';
import { mockEmptyBlocklist } from '@/test/mocks/data';

describe('Blocklist Page Flow', () => {
  const chatId = 100;

  beforeEach(() => {
    resetMockState();
    clearMockError();
  });

  describe('Loading State', () => {
    it('should show loading spinner while fetching patterns', async () => {
      setMockDelay(500);
      renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      expect(screen.getByRole('status')).toBeInTheDocument();

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      }, { timeout: 2000 });
    });
  });

  describe('Success State', () => {
    it('should display page title', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      expect(screen.getByRole('heading', { name: /blocklist/i })).toBeInTheDocument();
    });

    it('should display all blocklist patterns', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Should show all patterns
      expect(screen.getByText('spam')).toBeInTheDocument();
      expect(screen.getByText('http://bad-link.com')).toBeInTheDocument();
      expect(screen.getByText('offensive*word')).toBeInTheDocument();
    });

    it('should display add pattern button', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /add pattern/i })).toBeInTheDocument();
    });

    it('should display back button', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /back/i })).toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('should display empty state when no patterns', async () => {
      setMockBlocklist(chatId, mockEmptyBlocklist);
      renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Should still show add button
      expect(screen.getByRole('button', { name: /add pattern/i })).toBeInTheDocument();
      // Should show empty state message
      expect(screen.getByText(/no patterns/i)).toBeInTheDocument();
    });
  });

  describe('Add Pattern Flow', () => {
    it('should show add pattern form when clicking add button', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      const { user } = renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /add pattern/i }));

      // Form should be visible
      expect(screen.getByRole('textbox')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
    });

    it('should hide form when clicking cancel', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      const { user } = renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /add pattern/i }));
      await user.click(screen.getByRole('button', { name: /cancel/i }));

      // Form should be hidden, add button visible again
      expect(screen.getByRole('button', { name: /add pattern/i })).toBeInTheDocument();
    });

    it('should add new pattern when form is submitted', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      const { user } = renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Open add form
      await user.click(screen.getByRole('button', { name: /add pattern/i }));

      // Fill in pattern
      const patternInput = screen.getByRole('textbox');
      await user.type(patternInput, 'new-bad-word');

      // Submit form
      const submitButton = screen.getByRole('button', { name: /save|add|submit/i });
      await user.click(submitButton);

      // Form should close and new pattern should appear
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add pattern/i })).toBeInTheDocument();
      });
    });
  });

  describe('Delete Pattern Flow', () => {
    it('should show delete button for each pattern', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Should have delete buttons
      const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
      expect(deleteButtons.length).toBeGreaterThan(0);
    });

    it('should show confirmation dialog when deleting', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      const { user } = renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      // Click first delete button
      const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
      await user.click(deleteButtons[0]);

      // Confirmation dialog should appear
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
    });

    it('should cancel delete when clicking cancel in dialog', async () => {
      setMockBlocklist(chatId, mockBlocklistPatterns);
      const { user } = renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });

      const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
      await user.click(deleteButtons[0]);

      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });

      // Cancel the deletion
      const cancelButton = within(screen.getByRole('dialog')).getByRole('button', { name: /cancel/i });
      await user.click(cancelButton);

      // Dialog should close
      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });

      // Pattern should still exist
      expect(screen.getByText('spam')).toBeInTheDocument();
    });
  });

  describe('Error State', () => {
    it('should display error when API fails', async () => {
      setMockError('blocklist', 500, 'Internal server error');
      renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.getByText(/error/i)).toBeInTheDocument();
      });
    });

    it('should allow retry when error occurs', async () => {
      setMockError('blocklist', 500, 'Internal server error');
      const { user } = renderWithProviders(<BlocklistPage />, {
        initialEntries: [`/chat/${chatId}/blocklist`],
      });

      await waitFor(() => {
        expect(screen.getByText(/error/i)).toBeInTheDocument();
      });

      clearMockError();
      setMockBlocklist(chatId, mockBlocklistPatterns);

      const retryButton = screen.getByRole('button', { name: /retry/i });
      await user.click(retryButton);

      await waitFor(() => {
        expect(screen.getByText('spam')).toBeInTheDocument();
      });
    });
  });
});
