import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, getToken, setToken } from './api'

function mockFetch({ ok = true, status = 200, body } = {}) {
  global.fetch = vi.fn().mockResolvedValue({
    ok,
    status,
    json: async () => body
  })
}

describe('接口层', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('成功时直接返回 data', async () => {
    mockFetch({ body: { code: 0, message: 'ok', data: { id: 7 } } })
    await expect(api.booking(7)).resolves.toEqual({ id: 7 })
  })

  it('业务错误把 message 抛给调用方', async () => {
    mockFetch({ body: { code: 2001, message: '该时段刚被别人约走了，请换一个' } })
    await expect(api.booking(1)).rejects.toThrow('该时段刚被别人约走了，请换一个')
  })

  it('401 时清掉本地 token，避免带着废 token 反复请求', async () => {
    setToken('stale-token')
    mockFetch({ ok: false, status: 401, body: { code: 4001, message: '登录已失效' } })

    await expect(api.adminStaff(1)).rejects.toThrow('登录已失效')
    expect(getToken()).toBeNull()
  })

  it('已登录时请求带 Authorization 头', async () => {
    setToken('token-123')
    mockFetch({ body: { code: 0, data: [] } })

    await api.adminStaff(1)

    const [, options] = global.fetch.mock.calls[0]
    expect(options.headers.Authorization).toBe('Bearer token-123')
  })

  it('未登录时不带 Authorization 头', async () => {
    mockFetch({ body: { code: 0, data: [] } })
    await api.serviceList(1)

    const [, options] = global.fetch.mock.calls[0]
    expect(options.headers.Authorization).toBeUndefined()
  })

  it('查询参数拼进 URL 而不是请求体', async () => {
    mockFetch({ body: { code: 0, data: { slots: [] } } })

    await api.availability(1, 2, '2026-11-20')

    const [url] = global.fetch.mock.calls[0]
    expect(url).toContain('/api/availability?')
    expect(url).toContain('staffId=1')
    expect(url).toContain('serviceId=2')
    expect(url).toContain('date=2026-11-20')
  })

  it('服务端 500 时提示可读文案', async () => {
    mockFetch({ ok: false, status: 500, body: null })
    await expect(api.serviceList(1)).rejects.toThrow('服务暂时不可用')
  })
})
