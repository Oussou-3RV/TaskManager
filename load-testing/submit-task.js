import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const VUS = Number(__ENV.VUS || 10);
const ITERATIONS = Number(__ENV.ITERATIONS || 100);

// Repartition par couche, lue depuis l'en-tete Server-Timing renvoye par le
// backend (voir TraceTimingFilter). "network" est ce que k6 mesure en plus du
// temps serveur total : approxime le reseau, PAS le rendu navigateur — k6 est
// un client HTTP pur, sans moteur de rendu. Voir README pour cette limite.
const networkTrend = new Trend('layer_network', true);
const apiTrend = new Trend('layer_api', true);
const businessTrend = new Trend('layer_business', true);
const dbTrend = new Trend('layer_db', true);

export const options = {
    scenarios: {
        submit_task: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: ITERATIONS,
            maxDuration: '5m',
        },
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(50)', 'p(95)', 'p(99)'],
};

export function setup() {
    const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ username: 'operateur', password: 'password123' }),
        { headers: { 'Content-Type': 'application/json' } },
    );
    check(res, { 'login ok': (r) => r.status === 200 });
    return { token: res.json('token') };
}

function parseServerTiming(header) {
    // "api;dur=32.42, business;dur=9.97, db;dur=17.45, total;dur=59.84"
    const result = {};
    if (!header) return result;
    for (const part of header.split(',')) {
        const match = part.match(/(\w+);dur=([\d.]+)/);
        if (match) result[match[1]] = Number(match[2]);
    }
    return result;
}

export default function (data) {
    const idempotencyKey = `${__VU}-${__ITER}-${Date.now()}-${Math.random().toString(36).slice(2)}`;

    const res = http.post(
        `${BASE_URL}/api/v1/tasks`,
        JSON.stringify({ cpuRequired: 0.1, memoryRequiredMb: 1, priority: 'normal' }),
        {
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${data.token}`,
                'Idempotency-Key': idempotencyKey,
            },
        },
    );

    check(res, {
        'status is 202': (r) => r.status === 202,
    });

    const timing = parseServerTiming(res.headers['Server-Timing']);
    if (timing.total !== undefined) {
        const network = Math.max(0, res.timings.duration - timing.total);
        networkTrend.add(network);
        apiTrend.add(timing.api ?? 0);
        businessTrend.add(timing.business ?? 0);
        dbTrend.add(timing.db ?? 0);
    }
}
