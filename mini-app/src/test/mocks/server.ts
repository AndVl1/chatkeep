/**
 * MSW server setup for Node.js (test environment)
 */

import { setupServer } from 'msw/node';
import { handlers } from './handlers';

// Create the server with the handlers
export const server = setupServer(...handlers);

// Export everything from handlers for convenience
export * from './handlers';
