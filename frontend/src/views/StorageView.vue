<template>
  <div>
    <div class="page-title"><h2>存储管理</h2></div>
    <div class="stats-grid">
      <div class="stat-item"><div class="stat-value">{{ stats.fileCount || 0 }}</div><div class="stat-label">文件总数</div></div>
      <div class="stat-item"><div class="stat-value">{{ sizeText(stats.totalSize || 0) }}</div><div class="stat-label">总容量</div></div>
      <div class="stat-item"><div class="stat-value">MinIO</div><div class="stat-label">默认对象存储</div></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiGet } from '../api/http'

const stats = ref<Record<string, number>>({})

function sizeText(value: number) {
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

onMounted(async () => {
  stats.value = await apiGet('/storage/stats')
})
</script>
