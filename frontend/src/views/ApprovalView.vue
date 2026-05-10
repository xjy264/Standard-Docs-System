<template>
  <div>
    <div class="page-title"><h2>注册审核</h2></div>
    <div class="section">
      <el-table :data="rows" stripe>
        <el-table-column prop="id" label="审批 ID" width="100" />
        <el-table-column prop="userId" label="用户 ID" width="100" />
        <el-table-column prop="approvalStatus" label="状态" width="120" />
        <el-table-column prop="createdAt" label="申请时间" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="approve(row)">通过</el-button>
            <el-button link type="danger" @click="reject(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import { apiGet, apiPost } from '../api/http'

const rows = ref<any[]>([])

async function load() {
  rows.value = await apiGet('/approvals/pending')
}

async function approve(row: any) {
  await apiPost(`/approvals/${row.id}/approve`)
  ElMessage.success('审批通过')
  load()
}

async function reject(row: any) {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '审批拒绝')
  await apiPost(`/approvals/${row.id}/reject`, undefined, { reason: value })
  ElMessage.success('已拒绝')
  load()
}

onMounted(load)
</script>
