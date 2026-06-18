import { defineStore } from 'pinia'

export interface LoginUser {
  id: number
  username?: string
  phone: string
  realName: string
  deptId?: number
  isSuperAdmin?: boolean
  admin?: boolean
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as LoginUser | null,
    permissions: [] as string[],
    initialized: false
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.user),
    hasPermission: (state) => (code: string) => Boolean(state.user?.isSuperAdmin) || state.permissions.includes('*') || state.permissions.includes(code)
  },
  actions: {
    setSession(user: LoginUser, permissions: string[]) {
      this.user = user
      this.permissions = permissions
      this.initialized = true
    },
    updateUser(user: LoginUser) {
      this.user = user
      this.initialized = true
    },
    markInitialized() {
      this.initialized = true
    },
    logout() {
      this.user = null
      this.permissions = []
      this.initialized = true
    }
  }
})
