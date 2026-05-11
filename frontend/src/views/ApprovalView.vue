<template>
  <div>
    <div class="page-title"><h2>注册审核</h2></div>
    <div class="section">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待审核" name="pending">
          <el-table :data="rows" stripe>
            <el-table-column prop="id" label="审批 ID" width="100" />
            <el-table-column prop="userId" label="用户 ID" width="100" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">{{ approvalStatusText(row.approvalStatus) }}</template>
            </el-table-column>
            <el-table-column prop="createdAt" label="申请时间" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link type="primary" @click="approve(row)">通过</el-button>
                <el-button link type="danger" @click="reject(row)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="审核历史" name="history">
          <el-table :data="historyRows" stripe>
            <el-table-column prop="id" label="审批 ID" width="100" />
            <el-table-column prop="userId" label="用户 ID" width="100" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">{{ approvalStatusText(row.approvalStatus) }}</template>
            </el-table-column>
            <el-table-column prop="createdAt" label="申请时间" />
            <el-table-column prop="approvedAt" label="审核时间" />
            <el-table-column prop="approverId" label="审核人 ID" width="120" />
            <el-table-column prop="rejectReason" label="拒绝原因" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import { apiGet, apiPost } from '../api/http'

const activeTab = ref('pending')
const rows = ref<any[]>([])
const historyRows = ref<any[]>([])
const historyLoaded = ref(false)

async function load() {
  rows.value = await apiGet('/approvals/pending')
}

async function loadHistory() {
  historyRows.value = await apiGet('/approvals/history')
  historyLoaded.value = true
}

function handleTabChange(tabName: string | number) {
  if (tabName === 'history' && !historyLoaded.value) {
    loadHistory()
  }
}

function approvalStatusText(status: string) {
  const statusMap: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '已同意',
    REJECTED: '已拒绝'
  }
  return statusMap[status] || status
}

async function approve(row: any) {
  await apiPost(`/approvals/${row.id}/approve`)
  ElMessage.success('审批通过')
  historyLoaded.value = false
  load()
}

async function reject(row: any) {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '审批拒绝')
  await apiPost(`/approvals/${row.id}/reject`, undefined, { reason: value })
  ElMessage.success('已拒绝')
  historyLoaded.value = false
  load()
}

onMounted(load)
</script>
