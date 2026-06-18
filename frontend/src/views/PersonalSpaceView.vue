<template>
  <div>
    <div class="page-title"><h2>个人空间</h2></div>
    <div class="section">
      <el-tabs v-model="active">
        <el-tab-pane label="我的提醒" name="notice"><NotificationList /></el-tab-pane>
        <el-tab-pane v-if="auth.user?.isSuperAdmin" label="系统错误" name="errors"><ErrorEventPanel /></el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import NotificationList from '../components/NotificationList.vue'
import ErrorEventPanel from '../components/ErrorEventPanel.vue'
import { useAuthStore } from '../stores/auth'

const active = ref('notice')
const auth = useAuthStore()
</script>
