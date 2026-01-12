/**
 * Test utilities for rendering components with all necessary providers
 */

import React, { type PropsWithChildren } from 'react';
import { render, type RenderOptions, type RenderResult } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter, MemoryRouter, type MemoryRouterProps } from 'react-router-dom';
import { AppRoot } from '@telegram-apps/telegram-ui';
import { ConfirmDialogProvider } from '@/components/common/ConfirmDialog';
import { ToastProvider } from '@/components/common/Toast';

// Reset all stores between tests
import { useChatStore } from '@/stores/chatStore';
import { useSettingsStore } from '@/stores/settingsStore';
import { useBlocklistStore } from '@/stores/blocklistStore';
import { useLocksStore } from '@/stores/locksStore';
import { useAuthStore } from '@/stores/authStore';

export function resetAllStores() {
  useChatStore.setState({
    selectedChatId: null,
    chats: [],
  });
  useSettingsStore.setState({
    settingsCache: {},
    pendingChanges: {},
  });
  useBlocklistStore.setState({
    patternsCache: {},
  });
  useLocksStore.setState({
    locksCache: {},
  });
  useAuthStore.setState({
    token: null,
    user: null,
    isAuthenticated: false,
  });
}

interface WrapperOptions {
  route?: string;
  routerType?: 'browser' | 'memory';
  initialEntries?: MemoryRouterProps['initialEntries'];
}

/**
 * Creates a wrapper component with all necessary providers
 */
function createWrapper(options: WrapperOptions = {}) {
  const { routerType = 'memory', initialEntries = ['/'] } = options;

  return function Wrapper({ children }: PropsWithChildren) {
    const Router = routerType === 'browser' ? BrowserRouter : MemoryRouter;
    const routerProps = routerType === 'memory' ? { initialEntries } : {};

    return (
      <AppRoot>
        <ToastProvider>
          <ConfirmDialogProvider>
            <Router {...routerProps}>{children}</Router>
          </ConfirmDialogProvider>
        </ToastProvider>
      </AppRoot>
    );
  };
}

/**
 * Custom render function that wraps component with all providers
 */
export interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  route?: string;
  routerType?: 'browser' | 'memory';
  initialEntries?: MemoryRouterProps['initialEntries'];
}

export function renderWithProviders(
  ui: React.ReactElement,
  options: CustomRenderOptions = {}
): RenderResult & { user: ReturnType<typeof userEvent.setup> } {
  const { route, routerType, initialEntries, ...renderOptions } = options;

  // Reset stores before each render
  resetAllStores();

  const wrapper = createWrapper({
    route,
    routerType,
    initialEntries: initialEntries || (route ? [route] : ['/']),
  });

  const user = userEvent.setup();

  return {
    user,
    ...render(ui, { wrapper, ...renderOptions }),
  };
}

/**
 * Render with providers and return user event setup
 * This is an alias for renderWithProviders for consistency with testing-library patterns
 */
export const customRender = renderWithProviders;

/**
 * Simple wrapper for AppRoot only (useful for component-level tests)
 */
export function renderWithAppRoot(ui: React.ReactElement, options: RenderOptions = {}) {
  return render(ui, {
    wrapper: ({ children }) => <AppRoot>{children}</AppRoot>,
    ...options,
  });
}

/**
 * Wait for loading state to finish
 */
export async function waitForLoadingToFinish(screen: ReturnType<typeof render>) {
  const { queryByRole, queryByText } = screen;

  // Wait for any loading spinners to disappear
  await new Promise(resolve => setTimeout(resolve, 0));

  // Check for common loading indicators
  const spinner = queryByRole('progressbar');
  const loadingText = queryByText(/loading/i);

  if (spinner || loadingText) {
    await new Promise(resolve => setTimeout(resolve, 100));
  }
}

/**
 * Creates mock functions for testing callbacks
 */
export function createMockCallback<T extends (...args: unknown[]) => unknown>() {
  const calls: Parameters<T>[] = [];
  const fn = (...args: Parameters<T>) => {
    calls.push(args);
  };
  return {
    fn: fn as T,
    calls,
    lastCall: () => calls[calls.length - 1],
    callCount: () => calls.length,
    reset: () => {
      calls.length = 0;
    },
  };
}

// Re-export everything from @testing-library/react
export * from '@testing-library/react';
export { userEvent };
