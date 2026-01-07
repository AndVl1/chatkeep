/**
 * Mock Telegram environment for local development.
 *
 * NOTE: The actual mock setup is done in index.html via inline script
 * to ensure it runs BEFORE any ES modules are parsed.
 *
 * This file is kept for documentation and potential future enhancements.
 */

// Verify mock environment is set up (for debugging)
if (import.meta.env.DEV && typeof window !== 'undefined') {
  if ((window as any).tgWebAppPlatform) {
    console.log('[Mock] Telegram environment verified (set in index.html)');
  } else {
    console.warn('[Mock] Telegram environment NOT set - check index.html');
  }
}
