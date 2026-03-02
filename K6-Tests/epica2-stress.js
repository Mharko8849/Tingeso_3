/**
 * Stress Testing — Épica 2: Gestión de Préstamos y Devoluciones.
 * Lleva el sistema más allá de su capacidad para encontrar el punto de quiebre.
 *
 * Monitorear en paralelo: kubectl get pods -w
 * Ejecución: k6 run epica2-stress.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getToken, authHeaders } from './helpers/auth.js';

const failedRequests = new Rate('failed_requests');
const loanListDuration = new Trend('loan_list_duration', true);

// Sin thresholds de fallo — el objetivo es observar el comportamiento al colapsar.
export const options = {
    stages: [
        { duration: '30s', target: 100 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 300 },
        { duration: '1m', target: 300 },
        { duration: '30s', target: 500 },
        { duration: '1m', target: 500 },
        { duration: '30s', target: 750 },
        { duration: '1m', target: 750 },
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
const SAMPLE_LOAN_ID = 1;

export function setup() {
    const token = getToken();
    if (!token) throw new Error('No se pudo obtener el token. Revisar helpers/auth.js');
    return { token };
}

export default function (data) {
    const headers = authHeaders(data.token);

    group('Listado de préstamos paginado', () => {
        const res = http.get(`${BASE_URL}/api/loan/paginated?page=0&size=8`, headers);
        loanListDuration.add(res.timings.duration);
        failedRequests.add(res.status !== 200);
        check(res, { 'status 200': (r) => r.status === 200 });
    });
    sleep(0.5);

    group('Herramientas de un préstamo', () => {
        const res = http.get(`${BASE_URL}/api/loantool/loan/${SAMPLE_LOAN_ID}`, headers);
        failedRequests.add(res.status !== 200);
        check(res, { 'status 200': (r) => r.status === 200 });
    });
    sleep(0.5);

    group('Listado de clientes', () => {
        const res = http.get(`${BASE_URL}/api/user/clients`, headers);
        failedRequests.add(res.status !== 200);
        check(res, { 'status 200': (r) => r.status === 200 });
    });
    sleep(0.5);
}
