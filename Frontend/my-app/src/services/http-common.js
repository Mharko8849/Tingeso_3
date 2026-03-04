import axios from 'axios';
import keycloak from './keycloak';
import { showGlobalAlert } from '../components/Alerts/AlertContext';

const backendServer = import.meta.env.VITE_BACKEND_SERVER;
const backendPort = import.meta.env.VITE_BACKEND_PORT;

const baseURL = backendServer && backendPort 
  ? `http://${backendServer}:${backendPort}` 
  : 'https://toolrent.192.168.39.122.nip.io';

const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  async (config) => {
    // Don't attach an Authorization header for the backend login/register endpoints
    // — if we attach an expired token the backend will immediately return 401.
    const url = config.url || '';
    const isAuthEndpoint = url.includes('/api/auth/login') || url.includes('/api/auth/register');
    const isPublicEndpoint = url.includes('/api/kardex/ranking') || url.includes('/images/');

    if (isAuthEndpoint || isPublicEndpoint) {
      return config;
    }

    if (keycloak?.authenticated) {
      try {
        await keycloak.updateToken(30);
        config.headers.Authorization = `Bearer ${keycloak.token}`;
      } catch (e) {
        console.warn('Failed to refresh token', e);
      }
    }
    else {
      // If the app performed a backend login (not Keycloak), the access
      // token is stored in localStorage under 'access_token' or 'app_token'.
      // Attach it so authenticated endpoints (like /inventory) work.
      try {
        const localToken = typeof globalThis !== 'undefined' ? (localStorage.getItem('access_token') || localStorage.getItem('app_token')) : null;
        if (localToken) {
          config.headers.Authorization = `Bearer ${localToken}`;
        }
      } catch (error_) { console.debug(error_); // ignore localStorage errors
      }
    }
    return config;
  },
  (error) => { throw error; },
);

// Response interceptor to handle expired tokens
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

const tryKeycloakRefresh = async () => {
  if (!keycloak?.authenticated) return null;
  await keycloak.updateToken(-1);
  return keycloak.token;
};

const tryLocalRefresh = async () => {
  const refreshToken = localStorage.getItem('refresh_token');
  if (!refreshToken) return null;
  const response = await axios.post(`${baseURL}/api/auth/refresh`, {
    refresh_token: refreshToken,
  });
  const newToken = response.data?.token?.access_token;
  const newRefreshToken = response.data?.token?.refresh_token;
  if (!newToken) return null;
  localStorage.setItem('access_token', newToken);
  if (newRefreshToken) {
    localStorage.setItem('refresh_token', newRefreshToken);
  }
  return newToken;
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If error is not 401 or request already retried, reject
    if (error.response?.status !== 401 || originalRequest._retry) {
      throw error;
    }

    // Don't retry auth endpoints
    const url = originalRequest.url || '';
    if (url.includes('/api/auth/login') || url.includes('/api/auth/register') || url.includes('/api/auth/refresh')) {
      throw error;
    }

    // Check if user was authenticated (has tokens stored)
    const hasAccessToken = !!localStorage.getItem('access_token');
    const hasRefreshToken = !!localStorage.getItem('refresh_token');
    const isKeycloakAuth = keycloak?.authenticated;

    // If no tokens exist, user was never authenticated - just reject without redirect
    if (!hasAccessToken && !hasRefreshToken && !isKeycloakAuth) {
      throw error;
    }

    // If already refreshing, queue this request
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then(token => {
          originalRequest.headers['Authorization'] = `Bearer ${  token}`;
          return api(originalRequest);
        })
        .catch(err => { throw err; });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // Try Keycloak first, then local refresh token
      let newToken = null;
      try {
        newToken = await tryKeycloakRefresh();
      } catch (kcError) {
        console.warn('Keycloak token refresh failed', kcError);
      }

      if (!newToken) {
        newToken = await tryLocalRefresh();
      }

      if (!newToken) {
        throw new Error('Token refresh failed');
      }

      processQueue(null, newToken);
      isRefreshing = false;
      originalRequest.headers['Authorization'] = `Bearer ${  newToken}`;
      return api(originalRequest);
    } catch (refreshError) {
      console.warn('Token refresh failed', refreshError);
      
      // Clear all auth data
      processQueue(refreshError, null);
      isRefreshing = false;
      
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('app_token');
      localStorage.removeItem('user');

      // Show alert notification to user
      showGlobalAlert({
        message: 'Tu sesión ha expirado. Serás redirigido a la página principal.',
        severity: 'warning',
        autoHideMs: 4000,
      });

      // Redirect to home page after a short delay (not login, just home)
      if (typeof globalThis !== 'undefined' && !globalThis.location.pathname.includes('/login')) {
        setTimeout(() => {
          globalThis.location.href = '/';
        }, 1500);
      }

      throw refreshError;
    }
  },
);

export default api;
