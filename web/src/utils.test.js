import { describe, expect, it } from 'vitest'
import { cnDate, hhmm, nextDays, ymd, yuan } from './utils'

describe(' yuan ', () => {
  it('整数金额不带小数', () => {
    expect(yuan(6800)).toBe('¥68')
    expect(yuan(0)).toBe('¥0')
  })

  it('有零头时保留两位', () => {
    expect(yuan(12850)).toBe('¥128.50')
    expect(yuan(999)).toBe('¥9.99')
  })
})

describe(' ymd ', () => {
  it('补零到两位', () => {
    expect(ymd(new Date(2026, 0, 5))).toBe('2026-01-05')
    expect(ymd(new Date(2026, 10, 16))).toBe('2026-11-16')
  })
})

describe(' nextDays ', () => {
  it('从今天起算时，前两天是今天和明天', () => {
    const days = nextDays(7)
    expect(days).toHaveLength(7)
    expect(days[0].label).toBe('今天')
    expect(days[1].label).toBe('明天')
    expect(days[2].label).toMatch(/^周/)
  })

  it('日期连续且不重复', () => {
    const dates = nextDays(7).map((d) => d.date)
    expect(new Set(dates).size).toBe(7)
  })

  it('改期场景从指定日期起算，不再显示今天', () => {
    const start = new Date()
    start.setDate(start.getDate() + 3)
    const days = nextDays(7, start)
    expect(days[0].date).toBe(ymd(start))
    expect(days[0].label).toBe(days[0].weekday)
    expect(days[0].label).not.toBe('今天')
  })

  it('仍会把明天识别成明天', () => {
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    expect(nextDays(3, tomorrow)[0].label).toBe('明天')
  })
})

describe(' 时间格式化 ', () => {
  it('从 ISO 字符串取时分', () => {
    expect(hhmm('2026-11-17T10:30:00')).toBe('10:30')
    expect(hhmm('2026-11-17T09:05')).toBe('09:05')
  })

  it('转成中文日期带星期', () => {
    expect(cnDate('2026-11-17T10:00:00')).toBe('11月17日 周二')
    expect(cnDate('2026-11-22T18:00:00')).toBe('11月22日 周日')
  })

  it('空值不抛异常', () => {
    expect(hhmm('')).toBe('')
    expect(cnDate('')).toBe('')
  })
})
