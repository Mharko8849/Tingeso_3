/**
 * Helper de autenticación con Keycloak.
 * Obtiene un Bearer Token usando el flujo de contraseña (grant_type=password).
 *
 * Incluye timeout explícito y lógica de reintento con backoff exponencial para
 * absorber el período de recuperación de Keycloak tras tests de stress intensos.
 */

import http from 'k6/http';

const BASE_URL = 'https://toolrent.192.168.39.122.nip.io';
const REALM = 'ToolRent';
const CLIENT_ID = 'toolrent-frontend';
const TEST_USER = 'matigol';
const TEST_PASSWORD = 'matigol';

// Cuántas veces reintentar si Keycloak no responde (timeout o 5xx).
const MAX_RETRIES = 5;
// Espera base entre reintentos en ms — se duplica en cada intento (backoff exponencial).
const RETRY_BASE_DELAY_MS = 3000;

/**
 * Solicita un token de acceso a Keycloak y lo retorna como string.
 * Reintenta hasta MAX_RETRIES veces con backoff exponencial antes de rendirse.
 */
export function getToken() {
    const params = {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        // Timeout explícito: Keycloak puede tardar en arrancar o recuperarse del stress.
        // K6 default es 60s pero lo hacemos explícito para que sea configurable.
        timeout: '30s',
    };

    let lastStatus = 0;
    let lastBody = null;

    for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
        const res = http.post(
            `${BASE_URL}/realms/${REALM}/protocol/openid-connect/token`,
            {
                client_id: CLIENT_ID,
                username: TEST_USER,
                password: TEST_PASSWORD,
                grant_type: 'password',
            },
            params
        );

        lastStatus = res.status;
        lastBody = res.body;

        if (res.status === 200) {
            if (attempt > 1) {
                console.log(`Token obtenido en el intento ${attempt}`);
            }
            return res.json('access_token');
        }

        // Error de red (timeout, conexión rechazada) → status 0
        const isRetriable = res.status === 0 || res.status >= 500;
        if (!isRetriable) {
            // Error de cliente (401, 403, 400) — no tiene sentido reintentar
            console.error(`Error de autenticación no recuperable: ${res.status} — ${res.body}`);
            return null;
        }

        const waitMs = RETRY_BASE_DELAY_MS * Math.pow(2, attempt - 1);
        console.warn(`Intento ${attempt}/${MAX_RETRIES} fallido (status=${res.status}). Reintentando en ${waitMs}ms...`);

        // sleep() recibe segundos
        // eslint-disable-next-line no-undef
        __ENV && void 0; // evitar lint warning por sleep fuera de default function
        // Usamos un busy-wait básico con Date porque sleep() no está disponible en setup()
        const until = Date.now() + waitMs;
        while (Date.now() < until) { /* espera activa */ }
    }

    console.error(`Error al obtener token tras ${MAX_RETRIES} intentos: ${lastStatus} — ${lastBody}`);
    return null;
}

// Retorna los headers de autenticación listos para usar en cada request.
export function authHeaders(token) {
    return {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };
}

export const BASE_URL_EXPORT = BASE_URL;
