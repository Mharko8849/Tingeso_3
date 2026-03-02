/**
 * Stress Testing — Épica 6: Reportes y Consultas.
 * Los endpoints de reportes (ranking, kardex) tienden a colapsar antes que los simples
 * porque ejecutan queries con GROUP BY y JOINs complejos.
 *
 * Monitorear en paralelo: kubectl get pods -w | kubectl top pods
 * Ejecución: k6 run epica6-stress.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getToken, authHeaders } from './helpers/auth.js';

const failedRequests = new Rate('failed_requests');
const kardexDuration = new Trend('kardex_duration', true);
const rankingDuration = new Trend('ranking_duration', true);
const clientsDuration = new Trend('clients_duration', true);
const loansDuration = new Trend('loans_paginated_duration', true);

export const options = {
    stages: [
        { duration: '30s', target: 100 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 300 },
        { duration: '1m', target: 300 },
        { duration: '30s', target: 600 },
        { duration: '1m', target: 600 },
        { duration: '30s', target: 1000 },
        { duration: '1m', target: 1000 },
        { duration: '30s', target: 1500 },
        { duration: '1m', target: 1500 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        'failed_requests': ['rate<0.99'],
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

    // El ranking usa el endpoint paginado para limitar el GROUP BY.
    group('Ranking de herramientas', () => {
        const res = http.get(`${BASE_URL}/api/kardex/ranking/paginated?page=0&size=10`, headers);
        rankingDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'ranking → 200': (r) => r.status === 200 });
    });
    sleep(0.5);

    group('Kardex con filtros', () => {
        // /kardex/filter ahora devuelve PageResponseDTO — requiere page y size
        const res = http.get(
            `${BASE_URL}/api/kardex/filter?type=PRESTAMO&initDate=2026-01-01&finalDate=2026-12-31&page=0&size=20`,
            headers
        );
        kardexDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'kardex/filter → 200': (r) => r.status === 200 });
    });
    sleep(0.5);

    group('Listado de clientes', () => {
        const res = http.get(`${BASE_URL}/api/user/clients`, headers);
        clientsDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'user/clients → 200': (r) => r.status === 200 });
    });
    sleep(0.5);

    group('Pedidos paginados', () => {
        const res = http.get(`${BASE_URL}/api/loan/paginated?page=0&size=8`, headers);
        loansDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'loan/paginated → 200': (r) => r.status === 200 });
    });
    sleep(0.5);
}
