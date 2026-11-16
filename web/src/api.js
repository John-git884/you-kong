const BASE = '/api'
const TOKEN_KEY = 'youkong.token'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)

function authHeaders() {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function unwrap(resp) {
  let body = null
  try {
    body = await resp.json()
  } catch {
    // 非 JSON 响应，交给下面的状态码判断
  }
  if (resp.status === 401) {
    clearToken()
    if (location.pathname.startsWith('/admin')) {
      location.assign('/admin/login')
    }
    throw new Error((body && body.message) || '登录已失效，请重新登录')
  }
  if (body && typeof body.code === 'number' && body.code !== 0) {
    throw new Error(body.message || '操作失败')
  }
  if (!resp.ok) {
    throw new Error('服务暂时不可用，请稍后再试')
  }
  return body ? body.data : null
}

function query(params) {
  const usable = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
  if (!usable.length) return ''
  return '?' + usable.map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&')
}

const get = (path, params) =>
  fetch(BASE + path + query(params || {}), { headers: authHeaders() }).then(unwrap)

function send(method, path, payload) {
  const headers = { ...authHeaders() }
  if (payload !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  return fetch(BASE + path, {
    method,
    headers,
    body: payload === undefined ? undefined : JSON.stringify(payload)
  }).then(unwrap)
}

const post = (path, payload) => send('POST', path, payload)
const put = (path, payload) => send('PUT', path, payload)
const del = (path) => send('DELETE', path)

export const api = {
  login: (username, password) => post('/auth/login', { username, password }),

  shop: (shopId) => get(`/shops/${shopId}`),
  staffList: (shopId) => get(`/shops/${shopId}/staff`),
  serviceList: (shopId) => get(`/shops/${shopId}/services`),
  availability: (staffId, serviceId, date) =>
    get('/availability', { staffId, serviceId, date }),
  createBooking: (payload) => post('/bookings', payload),
  booking: (id) => get(`/bookings/${id}`),
  confirm: (id) => post(`/bookings/${id}/confirm`),
  complete: (id) => post(`/bookings/${id}/complete`),
  reschedule: (id, startAt) => post(`/bookings/${id}/reschedule`, { startAt }),
  cancel: (id) => post(`/bookings/${id}/cancel`),
  mine: (phone) => get('/bookings/mine', { phone, shopId: 1 }),
  adminBookings: (shopId, date) => get(`/admin/shops/${shopId}/bookings`, { date }),
  adminCancel: (id) => post(`/admin/bookings/${id}/cancel`),
  noShow: (id) => post(`/admin/bookings/${id}/no-show`),

  adminServices: (shopId) => get(`/admin/shops/${shopId}/services`),
  createService: (shopId, payload) => post(`/admin/shops/${shopId}/services`, payload),
  updateService: (id, payload) => put(`/admin/services/${id}`, payload),
  deleteService: (id) => del(`/admin/services/${id}`),

  adminStaff: (shopId) => get(`/admin/shops/${shopId}/staff`),
  createStaff: (shopId, payload) => post(`/admin/shops/${shopId}/staff`, payload),
  updateStaff: (id, payload) => put(`/admin/staff/${id}`, payload),

  schedules: (staffId) => get(`/admin/staff/${staffId}/schedules`),
  saveSchedules: (staffId, entries) => put(`/admin/staff/${staffId}/schedules`, entries),

  exceptions: (staffId) => get(`/admin/staff/${staffId}/exceptions`),
  saveException: (staffId, payload) => post(`/admin/staff/${staffId}/exceptions`, payload),
  deleteException: (id) => del(`/admin/exceptions/${id}`)
}
