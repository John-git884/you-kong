#!/usr/bin/env node
/**
 * 并发争抢同一档期的压测脚本，零依赖，Node 18+ 直接跑。
 *
 *   node loadtest/contention.mjs --vus 200 --start-at 2026-11-26T13:00:00
 *
 * 与 booking-contention.js（k6 版）测的是同一件事：
 * 无论多少个请求同时抢同一个时间槽，成功数必须恰好为 1。
 */

import { performance } from 'node:perf_hooks'

function parseArgs(argv) {
  const args = {}
  for (let i = 2; i < argv.length; i += 2) {
    const key = argv[i].replace(/^--/, '')
    args[key] = argv[i + 1]
  }
  return args
}

const args = parseArgs(process.argv)
const BASE = args.base || 'http://127.0.0.1:8080'
const VUS = Number(args.vus || 200)
const START_AT = args['start-at']
const STAFF_ID = Number(args.staff || 1)
const SERVICE_ID = Number(args.service || 1)

if (!START_AT) {
  console.error('缺少 --start-at，例如 --start-at 2026-11-26T13:00:00')
  process.exit(1)
}

async function book(index) {
  const payload = JSON.stringify({
    staffId: STAFF_ID,
    serviceId: SERVICE_ID,
    startAt: START_AT,
    customerName: `压测${index}`,
    customerPhone: `138${String(index).padStart(8, '0')}`
  })

  const started = performance.now()
  try {
    const resp = await fetch(`${BASE}/api/bookings`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: payload
    })
    const body = await resp.json()
    return { code: body.code, status: resp.status, ms: performance.now() - started }
  } catch (error) {
    return { code: 'ERROR', status: 0, ms: performance.now() - started, error: error.message }
  }
}

function percentile(sorted, p) {
  if (!sorted.length) return 0
  const index = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)
  return sorted[index]
}

const startedAt = performance.now()
const results = await Promise.all(Array.from({ length: VUS }, (_, i) => book(i + 1)))
const totalMs = performance.now() - startedAt

const success = results.filter((r) => r.code === 0)
const taken = results.filter((r) => r.code === 2001)
const others = results.filter((r) => r.code !== 0 && r.code !== 2001)
const latencies = results.map((r) => r.ms).sort((a, b) => a - b)

console.log(`并发 ${VUS}  |  总耗时 ${(totalMs / 1000).toFixed(2)}s`)
console.log(`成功 ${success.length}  |  档期冲突 2001 ${taken.length}  |  其它 ${others.length}`)
console.log(
  `延迟 p50 ${percentile(latencies, 50).toFixed(0)}ms  ` +
    `p95 ${percentile(latencies, 95).toFixed(0)}ms  ` +
    `p99 ${percentile(latencies, 99).toFixed(0)}ms  ` +
    `max ${latencies[latencies.length - 1].toFixed(0)}ms`
)

if (others.length) {
  const summary = {}
  for (const item of others) {
    const key = `${item.code}/${item.status}${item.error ? ' ' + item.error : ''}`
    summary[key] = (summary[key] || 0) + 1
  }
  console.log('异常分布:', summary)
}

if (success.length !== 1) {
  console.error(`超卖或全部失败：成功数为 ${success.length}，预期 1`)
  process.exit(1)
}
