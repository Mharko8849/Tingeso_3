/**
 * Volume Testing — Épica 2: Gestión de Préstamos y Devoluciones.
 * Evalúa el rendimiento de las queries SQL con diferentes volúmenes de datos en la BD.
 * Correr con la BD en estado inicial, luego con seed pequeño, mediano y grande. Comparar p50/p95.
 *
 * Ejecución: k6 run epica2-volume.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getToken, authHeaders } from './helpers/auth.js';

const paginatedQueryDuration = new Trend('paginated_query_duration', true);
const filterQueryDuration = new Trend('filter_query_duration', true);
const kardexQueryDuration = new Trend('kardex_query_duration', true);
const userLoansQueryDuration = new Trend('user_loans_query_duration', true);
const failedRequests = new Rate('failed_requests');

// Pocos usuarios — el foco es el volumen de datos, no la concurrencia.
export const options = {
    vus: 10,
    duration: '3m',
    thresholds: {
        'http_req_duration': ['p(95)<5000'],
        'paginated_query_duration': ['p(95)<5000'],
        'failed_requests': ['rate<0.05'],
    },
};

const BASE_URL = 'https://toolrent.192.168.39.122.nip.io';
const SAMPLE_USER_ID = 1;
const SAMPLE_LOAN_ID = 1;

export function setup() {
    const token = getToken();
    if (!token) throw new Error('No se pudo obtener el token. Revisar helpers/auth.js');
    console.log('Registrar el volumen actual: SELECT COUNT(*) FROM loans; SELECT COUNT(*) FROM kardex;');
    return { token };
}

export default function (data) {
    const headers = authHeaders(data.token);

    // Paginación — hace COUNT(*) + LIMIT, especialmente sensible al volumen.
    group('Paginación de préstamos', () => {
        const resP0 = http.get(`${BASE_URL}/api/loan/paginated?page=0&size=8`, headers);
        paginatedQueryDuration.add(resP0.timings.duration);
        failedRequests.add(resP0.status !== 200);
        check(resP0, { 'paginated p.0 → 200': (r) => r.status === 200 });
        sleep(0.5);

        // Página intermedia — más lenta con muchos datos por el OFFSET
        const resP10 = http.get(`${BASE_URL}/api/loan/paginated?page=10&size=8`, headers);
        paginatedQueryDuration.add(resP10.timings.duration);
        failedRequests.add(resP10.status !== 200);
        check(resP10, { 'paginated p.10 → 200': (r) => r.status === 200 });
    });

    sleep(1);

    // Filtro por estado — mide si hay índice en la columna state.
    group('Filtro por estado', () => {
        const resActive = http.get(`${BASE_URL}/api/loan/filter/paginated?state=ACTIVO&page=0&size=8`, headers);
        filterQueryDuration.add(resActive.timings.duration);
        failedRequests.add(resActive.status !== 200);
        check(resActive, { 'filter ACTIVO → 200': (r) => r.status === 200 });
        sleep(0.5);

        const resFin = http.get(`${BASE_URL}/api/loan/filter/paginated?state=FINALIZADO&page=0&size=8`, headers);
        filterQueryDuration.add(resFin.timings.duration);
        failedRequests.add(resFin.status !== 200);
        check(resFin, { 'filter FINALIZED → 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('Préstamos por usuario', () => {
        const res = http.get(`${BASE_URL}/api/loan/user/${SAMPLE_USER_ID}/paginated?page=0&size=8`, headers);
        userLoansQueryDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'loan/user paginated → 200': (r) => r.status === 200 });
    });

    sleep(1);

    // El kardex crece con cada operación — se usa /paginated para evitar full-table scan.
    group('Kardex completo', () => {
        const res = http.get(`${BASE_URL}/api/kardex/paginated?page=0&size=20`, headers);
        kardexQueryDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'GET /kardex/paginated → 200': (r) => r.status === 200 });
    });

    sleep(1);
}
