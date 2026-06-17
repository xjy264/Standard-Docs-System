function apiDateTimeSource(value: string) {
  const trimmed = value.trim()
  if (!trimmed) {
    return ''
  }
  return /(?:Z|[+-]\d{2}:?\d{2})$/i.test(trimmed) ? trimmed : `${trimmed}Z`
}

export function apiDateTimeToTimestamp(value?: string) {
  if (!value) {
    return NaN
  }
  return new Date(apiDateTimeSource(value)).getTime()
}

export function formatDateTime(value?: string, fallback = '') {
  if (!value) {
    return fallback
  }
  const date = new Date(apiDateTimeSource(value))
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const parts = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).formatToParts(date)
  const valueByType = Object.fromEntries(parts.map((part) => [part.type, part.value]))
  return `${valueByType.year}-${valueByType.month}-${valueByType.day} ${valueByType.hour}:${valueByType.minute}:${valueByType.second}`
}
