/**
 * Load Testing — Épica 2: Gestión de Préstamos y Devoluciones.
 * Simula cargas progresivas de usuarios concurrentes para medir hasta dónde aguanta el sistema.
 *
 * Ejecución: k6 run epica2-load.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getToken, authHeaders } from './helpers/auth.js';

const loanListDuration = new Trend('loan_list_duration', true);
const loanDetailDuration = new Trend('loan_detail_duration', true);
const loanFilterDuration = new Trend('loan_filter_duration', true);
const loanToolsDuration = new Trend('loan_tools_duration', true);
const loanTotalDuration = new Trend('loan_total_duration', true);
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
        'http_req_duration': ['p(95)<2000'],
        'failed_requests': ['rate<0.05'],
        'loan_list_duration': ['p(95)<2000'],
        'loan_detail_duration': ['p(95)<1000'],
        'loan_filter_duration': ['p(95)<2000'],
    },
};

const BASE_URL = 'https://toolrent.192.168.39.122.nip.io';
const SAMPLE_USER_ID = 1;
const SAMPLE_LOAN_ID = 1;

export function setup() {
    const token = getToken();
    if (!token) throw new Error('No se pudo obtener el token. Revisar helpers/auth.js');
    return { token };
}

export default function (data) {
    const headers = authHeaders(data.token);

    group('Listado y filtros de préstamos', () => {
        const resPaginated = http.get(`${BASE_URL}/api/loan/paginated?page=0&size=8`, headers);
        loanListDuration.add(resPaginated.timings.duration);
        failedRequests.add(resPaginated.status !== 200);
        check(resPaginated, {
            'GET /loan/paginated → 200': (r) => r.status === 200,
            'Respuesta contiene content': (r) => { try { return JSON.parse(r.body).content !== undefined; } catch (e) { return false; } },
        });
        sleep(0.5);

        const resFilter = http.get(`${BASE_URL}/api/loan/filter/paginated?state=ACTIVO&page=0&size=8`, headers);
        loanFilterDuration.add(resFilter.timings.duration);
        failedRequests.add(resFilter.status !== 200);
        check(resFilter, { 'GET /loan/filter/paginated → 200': (r) => r.status === 200 });
        sleep(0.5);

        const resUserLoans = http.get(`${BASE_URL}/api/loan/user/${SAMPLE_USER_ID}`, headers);
        failedRequests.add(resUserLoans.status !== 200);
        check(resUserLoans, { 'GET /loan/user/{id} → 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('Detalle de préstamo y herramientas asociadas', () => {
        const resLoan = http.get(`${BASE_URL}/api/loan/${SAMPLE_LOAN_ID}`, headers);
        loanDetailDuration.add(resLoan.timings.duration);
        failedRequests.add(resLoan.status !== 200);
        check(resLoan, { 'GET /loan/{id} → 200': (r) => r.status === 200 });
        sleep(0.5);

        const resTools = http.get(`${BASE_URL}/api/loantool/loan/${SAMPLE_LOAN_ID}`, headers);
        loanToolsDuration.add(resTools.timings.duration);
        failedRequests.add(resTools.status !== 200);
        check(resTools, { 'GET /loantool/loan/{id} → 200': (r) => r.status === 200 });
        sleep(0.5);

        const resTotal = http.get(`${BASE_URL}/api/loantool/total/${SAMPLE_LOAN_ID}`, headers);
        loanTotalDuration.add(resTotal.timings.duration);
        failedRequests.add(resTotal.status !== 200);
        check(resTotal, { 'GET /loantool/total/{id} → 200': (r) => r.status === 200 });
    });

    sleep(1);

    // El listado de clientes es parte del flujo de creación de pedidos (HU001)
    group('Listado y filtro de clientes', () => {
        const resClients = http.get(`${BASE_URL}/api/user/clients`, headers);
        failedRequests.add(resClients.status !== 200);
        check(resClients, { 'GET /user/clients → 200': (r) => r.status === 200 });
        sleep(0.5);

        const resFiltered = http.get(`${BASE_URL}/api/user/filter?state=ACTIVO`, headers);
        failedRequests.add(resFiltered.status !== 200);
        check(resFiltered, { 'GET /user/filter?state=ACTIVO → 200': (r) => r.status === 200 });
    });

    sleep(1);
}
