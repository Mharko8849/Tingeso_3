import axios from "axios";
import keycloak from "./keycloak";

const backendServer = import.meta.env.VITE_BACKEND_SERVER;
const backendPort = import.meta.env.VITE_BACKEND_PORT;

const baseURL = backendServer && backendPort 
  ? `http://${backendServer}:${backendPort}` 
  : '/';

const api = axios.create({
  baseURL: baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  async (config) => {
    // If the URL starts with /api/, remove it.
    // This harmonizes frontend calls (e.g., /api/kardex/ranking) with backend endpoints (/kardex)
    // as defined in the controllers and Ingress.
    /*
    if (config.url && config.url.startsWith('/api/')) {
       config.url = config.url.substring(4);
    }
    */

    // Don't attach an Authorization header for the backend login/register endpoints
    // — if we attach an expired token the backend will immediately return 401.
    const url = config.url || '';
    const isAuthEndpoint = url.includes('/auth/login') || url.includes('/auth/register');
    const isPublicEndpoint = url.includes('/kardex/ranking') || url.includes('/images/');

    if (isAuthEndpoint || isPublicEndpoint) {
      return config;
    }

    if (keycloak?.authenticated) {
      try {
        await keycloak.updateToken(30);
        config.headers.Authorization = `Bearer ${keycloak.token}`;
      } catch (e) {
        console.warn("Failed to refresh token", e);
      }
    }
    else {
      // If the app performed a backend login (not Keycloak), the access
      // token is stored in localStorage under 'access_token' or 'app_token'.
      // Attach it so authenticated endpoints (like /inventory) work.
      try {
        const localToken = typeof window !== 'undefined' ? (localStorage.getItem('access_token') || localStorage.getItem('app_token')) : null;
        if (localToken) {
          config.headers.Authorization = `Bearer ${localToken}`;
        }
      } catch (e) {
        // ignore localStorage errors
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default api;
