import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import LayoutView from '../views/LayoutView.vue'
import DashboardView from '../views/DashboardView.vue'
import FileLibraryView from '../views/FileLibraryView.vue'
import PersonalSpaceView from '../views/PersonalSpaceView.vue'
import RecycleBinView from '../views/RecycleBinView.vue'
import UsersView from '../views/UsersView.vue'
import ApprovalView from '../views/ApprovalView.vue'
import DeptView from '../views/DeptView.vue'
import DeptDetailView from '../views/DeptDetailView.vue'
import RolesView from '../views/RolesView.vue'
import PermissionMatrixView from '../views/PermissionMatrixView.vue'
import LogsView from '../views/LogsView.vue'
import StorageView from '../views/StorageView.vue'
import SettingsView from '../views/SettingsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/register', component: RegisterView },
    {
      path: '/',
      component: LayoutView,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: DashboardView },
        { path: 'files', component: FileLibraryView },
        { path: 'personal', component: PersonalSpaceView },
        { path: 'recycle-bin', component: RecycleBinView },
        { path: 'depts', component: DeptView },
        { path: 'depts/:id', component: DeptDetailView },
        { path: 'approvals', component: ApprovalView },
        { path: 'users', component: UsersView },
        { path: 'roles', component: RolesView },
        { path: 'permission-matrix', component: PermissionMatrixView },
        { path: 'logs', component: LogsView },
        { path: 'storage', component: StorageView },
        { path: 'settings', component: SettingsView }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!auth.token && !['/login', '/register'].includes(to.path)) {
    return '/login'
  }
  if (auth.token && to.path === '/login') {
    return '/dashboard'
  }
  return true
})

export default router
