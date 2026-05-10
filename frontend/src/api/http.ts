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
      if (!response.config.headers?.['X-Silent-Error']) {
        ElMessage.error(result.message || '操作失败')
      }
      return Promise.reject(new Error(result.message))
    }
    return response
  },
  (error) => {
    if (!error.config?.headers?.['X-Silent-Error']) {
      ElMessage.error(error.response?.data?.message || error.message || '网络异常')
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
