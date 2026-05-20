<template>
  <div>
    <div class="page-title"><h2>首页</h2></div>
    <div class="stats-grid">
      <div class="stat-item"><div class="stat-value">{{ stats.userCount || 0 }}</div><div class="stat-label">用户总数</div></div>
      <div class="stat-item"><div class="stat-value">{{ stats.fileCount || 0 }}</div><div class="stat-label">资料总数</div></div>
      <div class="stat-item"><div class="stat-value">{{ stats.unreadCount || 0 }}</div><div class="stat-label">未读提醒</div></div>
    </div>
    <div class="section" style="margin-top:14px">
      <h3>演示主线</h3>
      <el-steps :active="4" finish-status="success">
        <el-step title="注册审批" />
        <el-step title="目录配置" />
        <el-step title="文件详情" />
        <el-step title="附件填报" />
        <el-step title="记录查看" />
      </el-steps>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiGet } from '../api/http'

const stats = ref<Record<string, number>>({})

onMounted(async () => {
  stats.value = await apiGet('/dashboard/stats')
})
</script>
