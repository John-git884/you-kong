import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

// 压测目标：1000 个虚拟用户同时抢同一个档期，验证零超卖
// 运行：k6 run loadtest/booking-contention.js
// 改地址：k6 run -e BASE=http://your-host:8080 loadtest/booking-contention.js

const BASE = __ENV.BASE || 'http://localhost:8080';

const success = new Counter('booking_success');
const conflict = new Counter('booking_conflict');
const other = new Counter('booking_other');

export const options = {
  scenarios: {
    contention: {
      executor: 'per-vu-iterations',
      vus: 1000,
      iterations: 1,
      maxDuration: '60s',
    },
  },
  thresholds: {
    booking_success: ['count==1'],
  },
};

export default function () {
  const body = JSON.stringify({
    staffId: 1,
    serviceId: 1,
    startAt: __ENV.START_AT || '2026-11-18T10:00:00',
    customerName: `压测用户${__VU}`,
    customerPhone: '13800000000',
  });

  const res = http.post(`${BASE}/api/bookings`, body, {
    headers: { 'Content-Type': 'application/json' },
  });

  const payload = res.json();
  if (payload && payload.code === 0) {
    success.add(1);
  } else if (payload && payload.code === 2001) {
    conflict.add(1);
  } else {
    other.add(1);
    console.log(`unexpected: ${res.status} ${res.body}`);
  }

  check(res, {
    'http 200': (r) => r.status === 200,
  });
}
