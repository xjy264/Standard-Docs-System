import { defineStore } from 'pinia'

export interface LoginUser {
  id: number
  username: string
  realName: string
  deptId?: number
  isSuperAdmin?: boolean
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null') as LoginUser | null,
    permissions: JSON.parse(localStorage.getItem('permissions') || '[]') as string[]
  }),
  getters: {
    hasPermission: (state) => (code: string) => Boolean(state.user?.isSuperAdmin) || state.permissions.includes('*') || state.permissions.includes(code)
  },
  actions: {
    setSession(token: string, user: LoginUser, permissions: string[]) {
      this.token = token
      this.user = user
      this.permissions = permissions
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
      localStorage.setItem('permissions', JSON.stringify(permissions))
    },
    logout() {
      this.token = ''
      this.user = null
      this.permissions = []
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('permissions')
    }
  }
})
