export const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

export function ymd(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/**
 * 从某天起的 N 天，给日期条用。
 * start 用于改期场景：默认落在原预约当天，而不是今天。
 */
export function nextDays(count = 7, start = new Date()) {
  const today = new Date()
  const from = new Date(start.getFullYear(), start.getMonth(), start.getDate())
  const zero = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const offset = Math.round((from - zero) / 86400000)

  const out = []
  for (let i = 0; i < count; i++) {
    const d = new Date(from.getFullYear(), from.getMonth(), from.getDate() + i)
    const diff = offset + i
    out.push({
      date: ymd(d),
      weekday: WEEK[d.getDay()],
      label: diff === 0 ? '今天' : diff === 1 ? '明天' : WEEK[d.getDay()],
      day: `${d.getMonth() + 1}/${d.getDate()}`
    })
  }
  return out
}

export function yuan(cent) {
  const value = (cent || 0) / 100
  return Number.isInteger(value) ? `¥${value}` : `¥${value.toFixed(2)}`
}

export function hhmm(dateTime) {
  return (dateTime || '').slice(11, 16)
}

export function cnDate(dateTime) {
  const [y, m, d] = (dateTime || '').slice(0, 10).split('-').map(Number)
  if (!y) return ''
  return `${m}月${d}日 ${WEEK[new Date(y, m - 1, d).getDay()]}`
}

export const STATUS_TEXT = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  NO_SHOW: '已爽约'
}

export const STATUS_TONE = {
  PENDING: 'warn',
  CONFIRMED: 'ok',
  COMPLETED: 'muted',
  CANCELLED: 'muted',
  NO_SHOW: 'bad'
}
