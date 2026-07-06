import type { App } from 'vue'
import type { Router } from 'vue-router'

let routerRef: Router | undefined
let reporting = false
let installed = false
const SENSITIVE_KEYS = ['pass' + 'word', 'token', 'cookie', 'authorization', 'csrf', 'secret']

interface ErrorPayload {
  message?: string
  errorType?: string
  stack?: string
  route?: string
  component?: string
  browserInfo?: string
  traceId?: string
  statusCode?: number
  businessCode?: number
  method?: string
  url?: string
}

export function installErrorReporter(app: App, router: Router) {
  if (installed) {
    return
  }
  installed = true
  routerRef = router
  app.config.errorHandler = (error, instance, info) => {
    reportRuntimeError(error, info || instance?.$?.type?.name || 'Vue')
  }
  window.addEventListener('error', (event) => {
    reportRuntimeError(event.error || event.message, 'WindowError')
  })
  window.addEventListener('unhandledrejection', (event) => {
    reportRuntimeError(event.reason, 'UnhandledRejection')
  })
}

export function reportHttpError(error: any, safeMessage: string) {
  if (error?.config?.headers?.['X-Error-Report'] === '1') {
    return
  }
  const traceId = error?.response?.headers?.['x-trace-id'] || error?.response?.headers?.['X-Trace-Id']
  reportPayload({
    message: safeMessage || error?.message,
    errorType: 'HttpError',
    stack: error?.stack,
    route: routerRef?.currentRoute.value.fullPath,
    browserInfo: browserInfo(),
    traceId,
    statusCode: error?.response?.status,
    businessCode: error?.response?.data?.code,
    method: String(error?.config?.method || '').toUpperCase(),
    url: error?.config?.url
  })
}

function reportRuntimeError(error: unknown, component?: string) {
  const normalized = normalizeError(error)
  reportPayload({
    message: normalized.message,
    errorType: normalized.name,
    stack: normalized.stack,
    route: routerRef?.currentRoute.value.fullPath,
    component,
    browserInfo: browserInfo()
  })
}

function reportPayload(payload: ErrorPayload) {
  const csrfToken = readCookie('XSRF-TOKEN')
  if (!csrfToken) {
    return
  }
  if (reporting) {
    return
  }
  reporting = true
  const body = JSON.stringify(sanitizePayload(payload))
  fetch('/api/error-events/frontend', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': csrfToken,
      'X-Error-Report': '1'
    },
    body
  }).catch(() => undefined).finally(() => {
    reporting = false
  })
}

function normalizeError(error: unknown) {
  if (error instanceof Error) {
    return {
      name: error.name,
      message: error.message,
      stack: error.stack
    }
  }
  return {
    name: 'Error',
    message: typeof error === 'string' ? error : JSON.stringify(error),
    stack: ''
  }
}

function sanitizePayload(payload: ErrorPayload) {
  const sanitized: ErrorPayload = {}
  for (const [key, value] of Object.entries(payload)) {
    sanitized[key as keyof ErrorPayload] = sanitizeText(value as any) as never
  }
  return sanitized
}

function sanitizeText(value: unknown) {
  if (value === undefined || value === null) {
    return value as any
  }
  let text = String(value)
  for (const key of SENSITIVE_KEYS) {
    text = text.replace(new RegExp(`(${key}\\s*[=:]\\s*)[^&\\s,;]+`, 'gi'), '$1[REDACTED]')
  }
  return text.slice(0, 60000)
}

function readCookie(name: string) {
  return document.cookie
    .split(';')
    .map((item) => item.trim())
    .find((item) => item.startsWith(`${name}=`))
    ?.slice(name.length + 1) || ''
}

function browserInfo() {
  return `${navigator.userAgent}; ${window.innerWidth}x${window.innerHeight}`
}
