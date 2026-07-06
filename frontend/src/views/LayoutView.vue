<template>
  <div class="layout">
    <header class="topbar">
      <div class="top-actions top-left">
        <el-button class="console-button" plain @click="router.push('/dashboard')">首页</el-button>
      </div>
      <div class="brand">标准化资料管理系统</div>
      <div class="top-actions top-right">
        <el-button class="console-button" plain @click="router.push('/console/personal')">控制台</el-button>
        <span>{{ auth.user?.realName || auth.user?.phone }}</span>
        <el-button link style="color:#fff;margin-left:14px" @click="logout">退出</el-button>
      </div>
    </header>
    <main class="workspace" :class="{ 'is-dashboard': isDashboard }">
      <aside v-if="!isDashboard" class="sidebar">
        <el-menu router :default-active="$route.path" :default-openeds="defaultOpeneds">
          <template v-if="isConsole">
            <el-menu-item index="/console/personal">个人空间</el-menu-item>
            <el-menu-item v-if="canManageDocRoots" index="/console/doc-root-folders">资料目录设置</el-menu-item>
            <el-menu-item v-if="canManageRepairTemplates" index="/console/repair-project-templates">大修模板库</el-menu-item>
            <el-menu-item v-if="auth.user?.isSuperAdmin" index="/console/depts">组织管理</el-menu-item>
            <el-menu-item v-if="auth.hasPermission('user:view')" index="/console/users">用户管理</el-menu-item>
          </template>
          <template v-else>
            <el-menu-item v-for="dept in navigation" :key="dept.id" :index="`/${moduleBase}/${dept.id}`">{{ dept.deptName }}</el-menu-item>
          </template>
        </el-menu>
      </aside>
      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet, apiGetSilent } from '../api/http'
import { apiPost } from '../api/http'
import { useAuthStore } from '../stores/auth'

interface DeptNavigationItem {
  id: number
  parentId?: number
  deptName: string
  deptCode?: string
  sortOrder?: number
  status?: string
  children: DeptNavigationItem[]
}

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const navigation = ref<DeptNavigationItem[]>([])
const isConsole = computed(() => route.path.startsWith('/console'))
const isDashboard = computed(() => route.path === '/dashboard')
const moduleBase = computed(() => route.path.startsWith('/rules') ? 'rules' : 'internal')
const canManageDocRoots = computed(() => Boolean(auth.user?.isSuperAdmin || auth.user?.admin))
const canManageRepairTemplates = computed(() => Boolean(auth.user?.isSuperAdmin || auth.user?.admin))
const defaultOpeneds = computed(() => navigation.value.filter((item) => item.children.length).map((item) => `dept-${item.id}`))

async function logout() {
  try {
    await apiPost('/auth/logout')
  } finally {
    auth.logout()
    router.push('/login')
  }
}

async function loadNavigation() {
  try {
    navigation.value = await apiGet('/sections/navigation')
  } catch {
    navigation.value = []
  }
}

async function refreshCurrentUser() {
  if (!auth.isAuthenticated) {
    return true
  }
  try {
    const session = await apiGetSilent<{ user: any; permissions: string[] }>('/auth/me')
    auth.setSession(session.user, session.permissions)
    return true
  } catch {
    auth.logout()
    router.push('/login')
    return false
  }
}

onMounted(async () => {
  const signedIn = await refreshCurrentUser()
  if (!signedIn) {
    return
  }
  await loadNavigation()
})
</script>
