<template>
  <div>
    <div class="page-title"><h2>我收到的</h2></div>
    <div class="section">
      <el-tabs v-model="active">
        <el-tab-pane label="我收到的抄送" name="received">
          <el-table :data="received" stripe>
            <el-table-column prop="fileId" label="文件 ID" width="120" />
            <el-table-column prop="senderId" label="抄送人 ID" width="120" />
            <el-table-column prop="readStatus" label="状态" width="120" />
            <el-table-column prop="createdAt" label="抄送时间" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }"><el-button link type="primary" @click="markRead(row)">标记已读</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="我发出的抄送" name="sent">
          <el-table :data="sent" stripe>
            <el-table-column prop="fileId" label="文件 ID" width="120" />
            <el-table-column prop="receiverUserId" label="接收人 ID" width="140" />
            <el-table-column prop="readStatus" label="状态" width="120" />
            <el-table-column prop="createdAt" label="抄送时间" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiGet, apiPost } from '../api/http'

const active = ref('received')
const received = ref<any[]>([])
const sent = ref<any[]>([])

async function load() {
  received.value = await apiGet('/copies/received')
  sent.value = await apiGet('/copies/sent')
}

async function markRead(row: any) {
  await apiPost(`/copies/${row.id}/read`)
  load()
}

onMounted(load)
</script>
