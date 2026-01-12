/**
 * Test utilities for rendering components with all necessary providers
 */

import React, { type PropsWithChildren } from 'react';
import { render, type RenderOptions, type RenderResult } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route, type MemoryRouterProps } from 'react-router-dom';
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

/**
 * Custom render options with routing support
 */
export interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  /**
   * The current URL path, e.g., "/chat/100/settings"
   */
  initialEntries?: MemoryRouterProps['initialEntries'];
  /**
   * The route pattern with params, e.g., "/chat/:chatId/settings"
   * If provided, the component will be rendered inside a Route with this path
   */
  routePath?: string;
}

/**
 * Creates a wrapper with all providers, optionally with routing
 */
function createTestWrapper(
  options: CustomRenderOptions,
  componentToRender: React.ReactElement
): React.FC<PropsWithChildren> {
  const { initialEntries = ['/'], routePath } = options;

  return function TestWrapper({ children }: PropsWithChildren) {
    // If routePath is provided, wrap the component in Routes
    const content = routePath ? (
      <Routes>
        <Route path={routePath} element={children} />
      </Routes>
    ) : (
      children
    );

    return (
      <AppRoot>
        <ToastProvider>
          <ConfirmDialogProvider>
            <MemoryRouter initialEntries={initialEntries}>
              {content}
            </MemoryRouter>
          </ConfirmDialogProvider>
        </ToastProvider>
      </AppRoot>
    );
  };
}

/**
 * Infer route path from initial entry URL
 * Converts URLs like "/chat/100/settings" to route patterns like "/chat/:chatId/settings"
 */
function inferRoutePath(url: string): string {
  const segments = url.split('/');
  return segments.map((segment, index) => {
    // Common param patterns
    if (/^\d+$/.test(segment)) {
      // Look at previous segment to determine param name
      const prev = segments[index - 1];
      if (prev === 'chat') return ':chatId';
      if (prev === 'blocklist') return ':patternId';
      return ':id';
    }
    return segment;
  }).join('/');
}

/**
 * Custom render function that wraps component with all providers
 * Automatically handles route parameters when initialEntries contains URLs with IDs
 */
export function renderWithProviders(
  ui: React.ReactElement,
  options: CustomRenderOptions = {}
): RenderResult & { user: ReturnType<typeof userEvent.setup> } {
  // Reset stores before each render
  resetAllStores();

  // Auto-infer route path if not provided but initialEntries has params
  let routePath = options.routePath;
  if (!routePath && options.initialEntries && options.initialEntries.length > 0) {
    const firstEntry = options.initialEntries[0];
    const url = typeof firstEntry === 'string' ? firstEntry : firstEntry.pathname || '/';
    if (url !== '/' && url.includes('/')) {
      routePath = inferRoutePath(url);
    }
  }

  const wrapper = createTestWrapper({ ...options, routePath }, ui);
  const user = userEvent.setup();

  return {
    user,
    ...render(ui, { wrapper, ...options }),
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
