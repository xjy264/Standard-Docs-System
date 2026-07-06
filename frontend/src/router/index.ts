import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { apiGetSilent } from '../api/http'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import LayoutView from '../views/LayoutView.vue'
import DashboardView from '../views/DashboardView.vue'
import OrgHomeRedirectView from '../views/OrgHomeRedirectView.vue'
import OrgFilesView from '../views/OrgFilesView.vue'
import DocRecycleBinView from '../views/DocRecycleBinView.vue'
import DocItemDetailView from '../views/DocItemDetailView.vue'
import DocCategorySettingsView from '../views/DocCategorySettingsView.vue'
import DocRootFolderSettingsView from '../views/DocRootFolderSettingsView.vue'
import PersonalSpaceView from '../views/PersonalSpaceView.vue'
import UsersView from '../views/UsersView.vue'
import ApprovalView from '../views/ApprovalView.vue'
import DeptView from '../views/DeptView.vue'
import DeptDetailView from '../views/DeptDetailView.vue'
import RolesView from '../views/RolesView.vue'
import PermissionMatrixView from '../views/PermissionMatrixView.vue'
import LogsView from '../views/LogsView.vue'
import StorageView from '../views/StorageView.vue'
import SettingsView from '../views/SettingsView.vue'
import RepairProjectTemplateView from '../views/RepairProjectTemplateView.vue'

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
        { path: 'files', redirect: '/internal' },
        { path: 'org', redirect: '/internal' },
        { path: 'org/:deptId', redirect: (to) => `/internal/${to.params.deptId}` },
        { path: 'org/:deptId/recycle-bin', redirect: (to) => `/internal/${to.params.deptId}/recycle-bin` },
        { path: 'org/:deptId/items/:itemId', redirect: (to) => `/internal/${to.params.deptId}/items/${to.params.itemId}` },
        { path: 'internal', name: 'internal-home', component: OrgHomeRedirectView, meta: { moduleBase: 'internal' } },
        { path: 'internal/:deptId', name: 'internal-root', component: OrgFilesView, meta: { moduleBase: 'internal', moduleType: 'INTERNAL' } },
        { path: 'internal/:deptId/recycle-bin', name: 'internal-recycle-bin', component: DocRecycleBinView, meta: { moduleBase: 'internal', moduleType: 'INTERNAL' } },
        { path: 'internal/:deptId/items/:itemId', name: 'internal-item-detail', component: DocItemDetailView, meta: { moduleBase: 'internal', moduleType: 'INTERNAL' } },
        { path: 'rules', name: 'rules-home', component: OrgHomeRedirectView, meta: { moduleBase: 'rules' } },
        { path: 'rules/:deptId', name: 'rules-root', component: OrgFilesView, meta: { moduleBase: 'rules', moduleType: 'RULES' } },
        { path: 'rules/:deptId/recycle-bin', name: 'rules-recycle-bin', component: DocRecycleBinView, meta: { moduleBase: 'rules', moduleType: 'RULES' } },
        { path: 'rules/:deptId/items/:itemId', name: 'rules-item-detail', component: DocItemDetailView, meta: { moduleBase: 'rules', moduleType: 'RULES' } },
        { path: 'console', redirect: '/console/personal' },
        { path: 'console/personal', component: PersonalSpaceView },
        { path: 'console/doc-categories', component: DocCategorySettingsView },
        { path: 'console/doc-root-folders', component: DocRootFolderSettingsView },
        { path: 'console/repair-project-templates', component: RepairProjectTemplateView },
        { path: 'console/depts', component: DeptView },
        { path: 'console/depts/:id', component: DeptDetailView },
        { path: 'console/users', component: UsersView },
        { path: 'console/approvals', component: ApprovalView },
        { path: 'personal', redirect: '/console/personal' },
        { path: 'depts', redirect: '/console/depts' },
        { path: 'depts/:id', redirect: (to) => `/console/depts/${to.params.id}` },
        { path: 'approvals', redirect: '/console/approvals' },
        { path: 'users', redirect: '/console/users' },
        { path: 'roles', component: RolesView },
        { path: 'permission-matrix', component: PermissionMatrixView },
        { path: 'logs', component: LogsView },
        { path: 'storage', component: StorageView },
        { path: 'settings', component: SettingsView }
      ]
    }
  ]
})

let restoreSessionPromise: Promise<void> | null = null

async function restoreSession() {
  const auth = useAuthStore()
  if (auth.initialized) {
    return
  }
  if (!restoreSessionPromise) {
    restoreSessionPromise = apiGetSilent<{ user: any; permissions: string[] }>('/auth/me')
      .then((session) => auth.setSession(session.user, session.permissions))
      .catch(() => auth.logout())
      .finally(() => {
        auth.markInitialized()
        restoreSessionPromise = null
      })
  }
  await restoreSessionPromise
}

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await restoreSession()
  if (!auth.isAuthenticated && !['/login', '/register'].includes(to.path)) {
    return '/login'
  }
  if (auth.isAuthenticated && to.path === '/login') {
    return '/dashboard'
  }
  return true
})

export default router
