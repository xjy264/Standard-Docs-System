import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import LayoutView from '../views/LayoutView.vue'
import DashboardView from '../views/DashboardView.vue'
import OrgHomeRedirectView from '../views/OrgHomeRedirectView.vue'
import OrgFilesView from '../views/OrgFilesView.vue'
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
        { path: 'files', redirect: '/org' },
        { path: 'org', name: 'org-home', component: OrgHomeRedirectView },
        { path: 'org/:deptId', name: 'org-root', component: OrgFilesView },
        { path: 'org/:deptId/items/:itemId', name: 'doc-item-detail', component: DocItemDetailView },
        { path: 'console', redirect: '/console/personal' },
        { path: 'console/personal', component: PersonalSpaceView },
        { path: 'console/doc-categories', component: DocCategorySettingsView },
        { path: 'console/doc-root-folders', component: DocRootFolderSettingsView },
        { path: 'console/repair-project-templates', component: RepairProjectTemplateView },
        { path: 'console/depts', component: DeptView },
        { path: 'console/depts/:id', component: DeptDetailView },
        { path: 'console/users', component: UsersView },
        { path: 'personal', redirect: '/console/personal' },
        { path: 'depts', redirect: '/console/depts' },
        { path: 'depts/:id', redirect: (to) => `/console/depts/${to.params.id}` },
        { path: 'approvals', component: ApprovalView },
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
