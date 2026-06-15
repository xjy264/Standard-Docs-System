import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000
})

const AUTH_INVALID_MESSAGE = '登录状态已失效，请重新登录。'
const SERVICE_UNAVAILABLE_MESSAGE = '系统服务暂时不可用，请稍后重试或联系管理员。'
const ERROR_DEDUPE_MS = 2000
const errorShownAt = new Map<string, number>()
let navigateToLogin: (() => void) | undefined
let authInvalidNotified = false
let authInvalidRedirecting = false

export function setAuthInvalidNavigator(navigator: () => void) {
  navigateToLogin = navigator
}

function hasSilentErrorHeader(config?: any) {
  const headers = config?.headers
  if (!headers) {
    return false
  }
  return Boolean(
    headers['X-Silent-Error']
    || headers['x-silent-error']
    || headers.get?.('X-Silent-Error')
    || headers.get?.('x-silent-error')
  )
}

function showErrorOnce(message: string, key = message) {
  const now = Date.now()
  const lastShownAt = errorShownAt.get(key) || 0
  if (now - lastShownAt < ERROR_DEDUPE_MS) {
    return
  }
  errorShownAt.set(key, now)
  ElMessage.error(message)
}

function extractResponseMessage(data: any) {
  if (!data) {
    return ''
  }
  if (typeof data.message === 'string') {
    return data.message
  }
  if (typeof data.msg === 'string') {
    return data.msg
  }
  if (typeof data.error === 'string' && !data.error.includes('Forbidden')) {
    return data.error
  }
  return ''
}

function normalizeBusinessMessage(message?: string) {
  const text = String(message || '').trim()
  if (!text) {
    return ''
  }
  if (text.startsWith('没有权限：')) {
    return '当前账号没有该功能权限，请联系管理员开通。'
  }
  if (text.startsWith('系统异常')) {
    return SERVICE_UNAVAILABLE_MESSAGE
  }
  if (/验证码接口|返回格式|数据不完整/.test(text)) {
    return '人机验证暂时不可用，请稍后重试或联系管理员。'
  }
  if (/Request failed|AxiosError|status code|XMLHttpRequest|Network Error|timeout of|ECONNABORTED|Failed to fetch|Exception|java\.|SQL|MinIO|9000|Connection refused|Failed to connect/i.test(text)) {
    return ''
  }
  if (/BASIC_CHECK_FAIL|check fail/i.test(text)) {
    return '滑块验证未通过，请重新验证。'
  }
  return text
}

function messageByStatus(status?: number, message?: string) {
  const businessMessage = normalizeBusinessMessage(message)
  if (status === 401 || (status === 403 && !businessMessage)) {
    return AUTH_INVALID_MESSAGE
  }
  if (businessMessage) {
    return businessMessage
  }
  if (status === 403) {
    return '当前账号没有该功能权限，请联系管理员开通。'
  }
  if (status === 404) {
    return '请求的内容不存在或已被删除。'
  }
  if (status === 413) {
    return '上传文件过大，请压缩或拆分后重试。'
  }
  if (status === 408 || status === 504) {
    return '请求超时，请稍后重试。'
  }
  if (status && status >= 500) {
    return SERVICE_UNAVAILABLE_MESSAGE
  }
  return '操作未完成，请稍后重试。'
}

function handleAuthInvalid() {
  const auth = useAuthStore()
  auth.logout()
  if (!authInvalidNotified) {
    authInvalidNotified = true
    showErrorOnce(AUTH_INVALID_MESSAGE, 'auth-invalid')
  }
  if (!authInvalidRedirecting && window.location.pathname !== '/login') {
    authInvalidRedirecting = true
    if (navigateToLogin) {
      navigateToLogin()
    } else {
      window.location.replace('/login')
    }
  }
}

function handleResponseError(status: number | undefined, message: string, silent: boolean) {
  if (message === AUTH_INVALID_MESSAGE) {
    handleAuthInvalid()
    return
  }
  if (!silent) {
    showErrorOnce(message, `status:${status || 'network'}:${message}`)
  }
}

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>
    if (result && typeof result.code === 'number' && result.code !== 200) {
      const message = messageByStatus(result.code, result.message)
      handleResponseError(result.code, message, hasSilentErrorHeader(response.config))
      return Promise.reject(new Error(message))
    }
    return response
  },
  (error) => {
    const status = error.response?.status
    const rawMessage = extractResponseMessage(error.response?.data)
    const message = error.code === 'ECONNABORTED'
      ? '请求超时，请稍后重试。'
      : error.response
        ? messageByStatus(status, rawMessage)
        : '无法连接系统服务，请确认网络正常后重试。'
    handleResponseError(status, message, hasSilentErrorHeader(error.config))
    if (message === AUTH_INVALID_MESSAGE) {
      return Promise.reject(new Error(message))
    }
    return Promise.reject(error)
  }
)

export async function apiGet<T = any>(url: string, params?: Record<string, unknown>) {
  const { data } = await http.get<ApiResult<T>>(url, { params })
  return data.data
}

export async function apiPost<T = any>(url: string, body?: unknown, params?: Record<string, unknown>) {
  const { data } = await http.post<ApiResult<T>>(url, body, { params })
  return data.data
}

export async function apiPut<T = any>(url: string, body?: unknown) {
  const { data } = await http.put<ApiResult<T>>(url, body)
  return data.data
}

export async function apiDelete<T = any>(url: string) {
  const { data } = await http.delete<ApiResult<T>>(url)
  return data.data
}

export async function apiGetSilent<T = any>(url: string, params?: Record<string, unknown>) {
  const { data } = await http.get<ApiResult<T>>(url, { params, headers: { 'X-Silent-Error': '1' } })
  return data.data
}
