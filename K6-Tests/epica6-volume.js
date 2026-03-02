/**
 * Volume Testing — Épica 6: Reportes y Consultas.
 * Mide cómo se degradan los reportes y consultas cuando la BD tiene más datos.
 * Correr en cada fase del seed (inicial, 1k, 10k, 50k filas). Comparar p50/p95.
 *
 * Ejecución: k6 run epica6-volume.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getToken, authHeaders } from './helpers/auth.js';

const kardexFullDuration = new Trend('kardex_full_duration', true);
const kardexFilterDuration = new Trend('kardex_filter_duration', true);
const rankingDuration = new Trend('ranking_duration', true);
const clientFullDuration = new Trend('client_full_duration', true);
const employeeFullDuration = new Trend('employee_full_duration', true);
const loanPaginatedDuration = new Trend('loan_paginated_duration', true);
const failedRequests = new Rate('failed_requests');

// Pocos usuarios — el foco es el volumen de datos, no la concurrencia.
export const options = {
    vus: 5,
    duration: '3m',
    thresholds: {
        'http_req_duration': ['p(95)<10000'],
        'kardex_full_duration': ['p(95)<10000'],
        'ranking_duration': ['p(95)<10000'],
        'failed_requests': ['rate<0.05'],
    },
};

const BASE_URL = 'https://toolrent.192.168.39.122.nip.io';

export function setup() {
    const token = getToken();
    if (!token) throw new Error('No se pudo obtener el token. Revisar helpers/auth.js');
    console.log('Registrar filas antes de ejecutar: SELECT COUNT(*) FROM loans; SELECT COUNT(*) FROM kardex;');
    return { token };
}

export default function (data) {
    const headers = authHeaders(data.token);

    // El kardex crece con cada operación — es la tabla más sensible al volumen de la Épica 6.
    // Se usa /paginated para evitar full-table scan con grandes volúmenes.
    group('Kardex: listado completo', () => {
        const res = http.get(`${BASE_URL}/api/kardex/paginated?page=0&size=20`, headers);
        kardexFullDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'kardex/paginated → 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('Kardex: filtros combinados', () => {
        // /kardex/filter ahora devuelve PageResponseDTO — requiere page y size
        const res1 = http.get(`${BASE_URL}/api/kardex/filter?type=PRESTAMO&page=0&size=20`, headers);
        kardexFilterDuration.add(res1.timings.duration);
        failedRequests.add(res1.status !== 200);
        check(res1, { 'kardex/filter?type → 200': (r) => r.status === 200 });
        sleep(1);

        const res2 = http.get(
            `${BASE_URL}/api/kardex/filter?initDate=2026-01-01&finalDate=2026-12-31&page=0&size=20`, headers
        );
        kardexFilterDuration.add(res2.timings.duration);
        failedRequests.add(res2.status !== 200);
        check(res2, { 'kardex/filter?fechas → 200': (r) => r.status === 200 });
    });

    sleep(1);

    // Ranking — usa los endpoints paginados para limitar el resultado del GROUP BY.
    group('Ranking de herramientas', () => {
        const res = http.get(`${BASE_URL}/api/kardex/ranking/paginated?page=0&size=10`, headers);
        rankingDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'kardex/ranking/paginated → 200': (r) => r.status === 200 });
        sleep(1);

        const res2 = http.get(
            `${BASE_URL}/api/kardex/ranking/range/paginated?initDate=2026-01-01&finalDate=2026-12-31&page=0&size=10`, headers
        );
        rankingDuration.add(res2.timings.duration);
        failedRequests.add(res2.status !== 200);
        check(res2, { 'kardex/ranking/range/paginated → 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('Listado de clientes', () => {
        const res = http.get(`${BASE_URL}/api/user/clients`, headers);
        clientFullDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'user/clients → 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('Listado de empleados', () => {
        const res = http.get(`${BASE_URL}/api/user/employees`, headers);
        employeeFullDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'user/employees → 200': (r) => r.status === 200 });
    });

    sleep(1);

    // Comparar la primera página vs páginas intermedias para detectar degradación por OFFSET.
    group('Pedidos paginados en distintas páginas', () => {
        const resP0 = http.get(`${BASE_URL}/api/loan/paginated?page=0&size=8`, headers);
        loanPaginatedDuration.add(resP0.timings.duration);
        failedRequests.add(resP0.status !== 200);
        check(resP0, { 'loan/paginated p.0 → 200': (r) => r.status === 200 });
        sleep(1);

        const resP5 = http.get(`${BASE_URL}/api/loan/paginated?page=5&size=8`, headers);
        loanPaginatedDuration.add(resP5.timings.duration);
        failedRequests.add(resP5.status !== 200);
        check(resP5, { 'loan/paginated p.5 → 200': (r) => r.status === 200 });
    });

    sleep(1);
}
