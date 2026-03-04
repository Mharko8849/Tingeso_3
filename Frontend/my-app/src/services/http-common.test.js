import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// We need to test the interceptor logic. Since http-common.js creates
// an axios instance at import time, we must mock axios and keycloak BEFORE import.

const mockInterceptorsRequest = [];
const mockInterceptorsResponse = [];
const mockAxiosInstance = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  interceptors: {
    request: { use: (fulfilled, rejected) => mockInterceptorsRequest.push({ fulfilled, rejected }) },
    response: { use: (fulfilled, rejected) => mockInterceptorsResponse.push({ fulfilled, rejected }) },
  },
};

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => mockAxiosInstance),
    post: vi.fn(),
  },
}));

vi.mock('./keycloak', () => ({
  default: { authenticated: false, token: null, tokenParsed: null, updateToken: vi.fn() },
}));

vi.mock('../components/Alerts/AlertContext', () => ({
  showGlobalAlert: vi.fn(),
}));

// Now import
import keycloak from './keycloak';
import { showGlobalAlert } from '../components/Alerts/AlertContext';
import axios from 'axios';

// Force re-import the module to trigger interceptor registration
let api;
beforeEach(async () => {
  mockInterceptorsRequest.length = 0;
  mockInterceptorsResponse.length = 0;
  vi.resetModules();
  // Re-import to re-register interceptors
  const mod = await import('./http-common.js');
  api = mod.default;
});

describe('http-common request interceptor', () => {
  it('registers a request interceptor', () => {
    expect(mockInterceptorsRequest.length).toBeGreaterThan(0);
  });

  it('skips auth header for login endpoint', async () => {
    const interceptor = mockInterceptorsRequest[0];
    const config = { url: '/api/auth/login', headers: {} };
    const result = await interceptor.fulfilled(config);
    expect(result.headers.Authorization).toBeUndefined();
  });

  it('skips auth header for register endpoint', async () => {
    const interceptor = mockInterceptorsRequest[0];
    const config = { url: '/api/auth/register', headers: {} };
    const result = await interceptor.fulfilled(config);
    expect(result.headers.Authorization).toBeUndefined();
  });

  it('skips auth header for public ranking endpoint', async () => {
    const interceptor = mockInterceptorsRequest[0];
    const config = { url: '/api/kardex/ranking', headers: {} };
    const result = await interceptor.fulfilled(config);
    expect(result.headers.Authorization).toBeUndefined();
  });

  it('skips auth header for images endpoint', async () => {
    const interceptor = mockInterceptorsRequest[0];
    const config = { url: '/images/tool.jpg', headers: {} };
    const result = await interceptor.fulfilled(config);
    expect(result.headers.Authorization).toBeUndefined();
  });

  it('attaches keycloak token when authenticated', async () => {
    keycloak.authenticated = true;
    keycloak.token = 'kc-token-123';
    keycloak.updateToken = vi.fn().mockResolvedValue(true);
    const interceptor = mockInterceptorsRequest[0];
    const config = { url: '/api/inventory', headers: {} };
    const result = await interceptor.fulfilled(config);
    expect(result.headers.Authorization).toBe('Bearer kc-token-123');
    keycloak.authenticated = false;
  });

  it('handles keycloak token refresh failure gracefully', async () => {
    keycloak.authenticated = true;
    keycloak.updateToken = vi.fn().mockRejectedValue(new Error('refresh fail'));
    const interceptor = mockInterceptorsRequest[0];
    const config = { url: '/api/inventory', headers: {} };
    const result = await interceptor.fulfilled(config);
    // Should not throw, just warn
    expect(result).toBeDefined();
    keycloak.authenticated = false;
  });

  it('attaches localStorage token when not keycloak authenticated', async () => {
    keycloak.authenticated = false;
    localStorage.setItem('access_token', 'local-token-456');
    const interceptor = mockInterceptorsRequest[0];
    const config = { url: '/api/inventory', headers: {} };
    const result = await interceptor.fulfilled(config);
    expect(result.headers.Authorization).toBe('Bearer local-token-456');
    localStorage.removeItem('access_token');
  });

  it('attaches app_token when access_token is absent', async () => {
    keycloak.authenticated = false;
    localStorage.removeItem('access_token');
    localStorage.setItem('app_token', 'app-token-789');
    const interceptor = mockInterceptorsRequest[0];
    const config = { url: '/api/inventory', headers: {} };
    const result = await interceptor.fulfilled(config);
    expect(result.headers.Authorization).toBe('Bearer app-token-789');
    localStorage.removeItem('app_token');
  });

  it('sends no auth header when no tokens available', async () => {
    keycloak.authenticated = false;
    localStorage.removeItem('access_token');
    localStorage.removeItem('app_token');
    const interceptor = mockInterceptorsRequest[0];
    const config = { url: '/api/inventory', headers: {} };
    const result = await interceptor.fulfilled(config);
    expect(result.headers.Authorization).toBeUndefined();
  });

  it('throws error from rejected handler', () => {
    const interceptor = mockInterceptorsRequest[0];
    expect(() => interceptor.rejected(new Error('req error'))).toThrow('req error');
  });
});

describe('http-common response interceptor', () => {
  it('registers a response interceptor', () => {
    expect(mockInterceptorsResponse.length).toBeGreaterThan(0);
  });

  it('passes successful responses through', async () => {
    const interceptor = mockInterceptorsResponse[0];
    const response = { data: 'ok', status: 200 };
    const result = await interceptor.fulfilled(response);
    expect(result).toBe(response);
  });

  it('rejects non-401 errors', async () => {
    const interceptor = mockInterceptorsResponse[0];
    const error = { response: { status: 500 }, config: {} };
    await expect(interceptor.rejected(error)).rejects.toBe(error);
  });

  it('rejects 401 for auth endpoints without retry', async () => {
    const interceptor = mockInterceptorsResponse[0];
    const error = { response: { status: 401 }, config: { url: '/api/auth/login' } };
    await expect(interceptor.rejected(error)).rejects.toBe(error);
  });

  it('rejects 401 for refresh endpoints without retry', async () => {
    const interceptor = mockInterceptorsResponse[0];
    const error = { response: { status: 401 }, config: { url: '/api/auth/refresh' } };
    await expect(interceptor.rejected(error)).rejects.toBe(error);
  });

  it('rejects 401 when no tokens exist (unauthenticated)', async () => {
    keycloak.authenticated = false;
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    const interceptor = mockInterceptorsResponse[0];
    const error = { response: { status: 401 }, config: { url: '/api/inventory', headers: {} } };
    await expect(interceptor.rejected(error)).rejects.toBe(error);
  });

  it('rejects 401 when already retried', async () => {
    const interceptor = mockInterceptorsResponse[0];
    const error = { response: { status: 401 }, config: { url: '/api/inventory', _retry: true } };
    await expect(interceptor.rejected(error)).rejects.toBe(error);
  });
});
