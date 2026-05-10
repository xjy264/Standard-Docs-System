<template>
  <div>
    <div class="page-title"><h2>回收站</h2></div>
    <div class="section">
      <el-table :data="rows" stripe>
        <el-table-column prop="fileId" label="文件 ID" width="120" />
        <el-table-column prop="originalPath" label="原路径" />
        <el-table-column prop="deletedBy" label="删除人" width="120" />
        <el-table-column prop="deletedAt" label="删除时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }"><el-button link type="primary" @click="restore(row)">恢复</el-button></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { apiGet, apiPost } from '../api/http'

const rows = ref<any[]>([])

async function load() {
  rows.value = await apiGet('/recycle-bin')
}

async function restore(row: any) {
  await apiPost(`/recycle-bin/${row.fileId}/restore`)
  ElMessage.success('恢复成功')
  load()
}

onMounted(load)
</script>
