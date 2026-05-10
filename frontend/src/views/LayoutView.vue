<template>
  <div class="layout">
    <header class="topbar">
      <div class="brand">大同房建公寓段标准化资料管理系统</div>
      <div>
        <span>{{ auth.user?.realName || auth.user?.username }}</span>
        <el-button link style="color:#fff;margin-left:14px" @click="logout">退出</el-button>
      </div>
    </header>
    <main class="workspace">
      <aside class="sidebar">
        <el-menu router :default-active="$route.path">
          <el-menu-item index="/dashboard">首页</el-menu-item>
          <el-menu-item index="/files">文件库</el-menu-item>
          <el-menu-item index="/personal">个人空间</el-menu-item>
          <el-menu-item v-if="auth.user?.isSuperAdmin" index="/depts">组织管理</el-menu-item>
          <el-menu-item v-if="auth.hasPermission('user:approve')" index="/approvals">注册审核</el-menu-item>
          <el-menu-item v-if="auth.hasPermission('user:view')" index="/users">用户管理</el-menu-item>
        </el-menu>
      </aside>
      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
