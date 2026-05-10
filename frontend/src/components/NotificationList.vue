<template>
  <el-table :data="rows" stripe>
    <el-table-column prop="title" label="标题" />
    <el-table-column prop="content" label="内容" />
    <el-table-column prop="readStatus" label="状态" width="100" />
    <el-table-column prop="createdAt" label="时间" width="180" />
    <el-table-column label="操作" width="100">
      <template #default="{ row }">
        <el-button link type="primary" @click="read(row)">已读</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiGet, apiPost } from '../api/http'

const rows = ref<any[]>([])

async function load() {
  rows.value = await apiGet('/notifications')
}

async function read(row: any) {
  await apiPost(`/notifications/${row.id}/read`)
  load()
}

onMounted(load)
</script>
