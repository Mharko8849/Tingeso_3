/**
 * Load Testing — Épica 6: Reportes y Consultas.
 * Simula cargas progresivas sobre los endpoints de kardex, ranking, empleados, clientes y pedidos.
 *
 * Ejecución: k6 run epica6-load.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getToken, authHeaders } from './helpers/auth.js';

const kardexListDuration = new Trend('kardex_list_duration', true);
const kardexFilterDuration = new Trend('kardex_filter_duration', true);
const kardexRankingDuration = new Trend('kardex_ranking_duration', true);
const employeeListDuration = new Trend('employee_list_duration', true);
const clientListDuration = new Trend('client_list_duration', true);
const loanListDuration = new Trend('loan_list_duration', true);
const failedRequests = new Rate('failed_requests');

export const options = {
    stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 10 },
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 500 },
        { duration: '1m', target: 500 },
        { duration: '30s', target: 1000 },
        { duration: '1m', target: 1000 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        'http_req_duration': ['p(95)<3000'],
        'kardex_list_duration': ['p(95)<3000'],
        'kardex_filter_duration': ['p(95)<3000'],
        'kardex_ranking_duration': ['p(95)<3000'],
        'employee_list_duration': ['p(95)<2000'],
        'client_list_duration': ['p(95)<2000'],
        'loan_list_duration': ['p(95)<2000'],
        'failed_requests': ['rate<0.05'],
    },
};

const BASE_URL = 'https://toolrent.192.168.39.122.nip.io';

export function setup() {
    const token = getToken();
    if (!token) throw new Error('No se pudo obtener el token. Revisar helpers/auth.js');
    return { token };
}

export default function (data) {
    const headers = authHeaders(data.token);

    // HU003 — Visualizar y filtrar movimientos del Kardex
    group('Kardex: listado y filtros', () => {
        const res = http.get(`${BASE_URL}/api/kardex/paginated?page=0&size=20`, headers);
        kardexListDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'GET /kardex/paginated → 200': (r) => r.status === 200 });
        sleep(0.5);

        // /kardex/filter ahora devuelve PageResponseDTO — requiere page y size
        const resType = http.get(`${BASE_URL}/api/kardex/filter?type=PRESTAMO&page=0&size=20`, headers);
        kardexFilterDuration.add(resType.timings.duration);
        failedRequests.add(resType.status !== 200);
        check(resType, {
            'kardex/filter?type → 200': (r) => r.status === 200,
            'kardex/filter tiene content': (r) => { try { return JSON.parse(r.body).content !== undefined; } catch (e) { return false; } },
        });
        sleep(0.5);

        const resDates = http.get(
            `${BASE_URL}/api/kardex/filter?initDate=2026-01-01&finalDate=2026-12-31&page=0&size=20`, headers
        );
        kardexFilterDuration.add(resDates.timings.duration);
        failedRequests.add(resDates.status !== 200);
        check(resDates, { 'kardex/filter?fechas → 200': (r) => r.status === 200 });
    });

    sleep(1);

    // HU003 — Ranking de herramientas (global y por rango de fechas)
    group('Kardex: ranking de herramientas', () => {
        const res = http.get(`${BASE_URL}/api/kardex/ranking/paginated?page=0&size=10`, headers);
        kardexRankingDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'GET /kardex/ranking/paginated → 200': (r) => r.status === 200 });
        sleep(0.5);

        const resRange = http.get(
            `${BASE_URL}/api/kardex/ranking/range/paginated?initDate=2026-01-01&finalDate=2026-12-31&page=0&size=10`, headers
        );
        kardexRankingDuration.add(resRange.timings.duration);
        failedRequests.add(resRange.status !== 200);
        check(resRange, { 'kardex/ranking/range/paginated → 200': (r) => r.status === 200 });
    });

    sleep(1);

    // HU004 — Listado de empleados y filtros
    group('Empleados: listado y filtro', () => {
        const res = http.get(`${BASE_URL}/api/user/employees`, headers);
        employeeListDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'GET /user/employees → 200': (r) => r.status === 200 });
        sleep(0.5);

        const resFilter = http.get(`${BASE_URL}/api/user/filter/employee?state=ACTIVO`, headers);
        failedRequests.add(resFilter.status !== 200);
        check(resFilter, { 'GET /user/filter/employee → 200': (r) => r.status === 200 });
    });

    sleep(1);

    // HU004 — Listado de clientes y filtro por estado
    group('Clientes: listado y filtro por estado', () => {
        const res = http.get(`${BASE_URL}/api/user/clients`, headers);
        clientListDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'GET /user/clients → 200': (r) => r.status === 200 });
        sleep(0.5);

        const resRestricted = http.get(`${BASE_URL}/api/user/filter?state=RESTRINGIDO`, headers);
        failedRequests.add(resRestricted.status !== 200);
        check(resRestricted, { 'GET /user/filter?RESTRINGIDO → 200': (r) => r.status === 200 });
    });

    sleep(1);

    // HU004 — Listado general de pedidos y filtros
    group('Pedidos: listado paginado y filtro por estado', () => {
        const res = http.get(`${BASE_URL}/api/loan/paginated?page=0&size=8`, headers);
        loanListDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'GET /loan/paginated → 200': (r) => r.status === 200 });
        sleep(0.5);

        const resActive = http.get(
            `${BASE_URL}/api/loan/filter/paginated?state=ACTIVO&page=0&size=8`, headers
        );
        failedRequests.add(resActive.status !== 200);
        check(resActive, { 'loan/filter/paginated?ACTIVO → 200': (r) => r.status === 200 });
    });

    sleep(1);
}
