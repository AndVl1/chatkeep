import ky, { type KyInstance } from 'ky';

const API_BASE = import.meta.env.VITE_API_URL || '/api/v1/miniapp';

let authHeader: Record<string, string> = {};

export function setAuthHeader(header: Record<string, string>) {
  authHeader = header;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public details?: unknown
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const client: KyInstance = ky.create({
  prefixUrl: API_BASE,
  timeout: 30000,
  retry: {
    limit: 2,
    statusCodes: [408, 429, 500, 502, 503, 504],
  },
  hooks: {
    beforeRequest: [
      (request) => {
        // Skip ngrok browser warning page
        request.headers.set('ngrok-skip-browser-warning', 'true');

        Object.entries(authHeader).forEach(([key, value]) => {
          request.headers.set(key, value);
        });
      },
    ],
    afterResponse: [
      async (_request, _options, response) => {
        if (!response.ok) {
          let errorMessage = `Request failed with status ${response.status}`;
          let errorDetails: unknown;

          try {
            const json: unknown = await response.json();
            if (typeof json === 'object' && json !== null) {
              const errorObj = json as Record<string, unknown>;
              errorMessage = (errorObj.message as string) || (errorObj.error as string) || errorMessage;
            }
            errorDetails = json;
          } catch {
            // Failed to parse error response
          }

          // Handle 401 Unauthorized in web mode
          if (response.status === 401) {
            // Check if we're in web mode (no initData)
            const webApp = (window as any).Telegram?.WebApp;
            const hasInitData = !!webApp?.initData && webApp.initData.length > 0;

            if (!hasInitData) {
              // Web mode: clear auth and reload to login page
              localStorage.removeItem('chatkeep_auth_token');
              localStorage.removeItem('chatkeep_auth_user');
              window.location.href = '/';
            }
          }

          throw new ApiError(response.status, errorMessage, errorDetails);
        }
      },
    ],
  },
});

export default client;
