<template>
  <div>
    <div class="page-title"><h2>权限矩阵</h2></div>
    <div class="query-bar">
      <span style="margin-right:8px">部门 ID</span>
      <el-input-number v-model="deptId" :min="1" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <div class="section">
      <el-table :data="users" stripe>
        <el-table-column prop="realName" label="用户" width="130" fixed />
        <el-table-column v-for="permission in permissions" :key="permission.permissionCode" :label="permission.permissionName" width="150">
          <template #default="{ row }">
            <el-checkbox :model-value="has(row.id, permission.permissionCode)" @change="toggle(row.id, permission.permissionCode, $event)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="save(row)">保存</el-button></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { apiGet, apiPost } from '../api/http'

const deptId = ref(1)
const users = ref<any[]>([])
const permissions = ref<any[]>([])
const selected = ref<Record<number, Set<string>>>({})

async function load() {
  const result = await apiGet<any>('/permission-matrix', { deptId: deptId.value })
  users.value = result.users
  permissions.value = result.permissions
  selected.value = {}
  Object.entries(result.effective as Record<string, string[]>).forEach(([userId, codes]) => {
    selected.value[Number(userId)] = new Set(codes)
  })
}

function has(userId: number, code: string) {
  return selected.value[userId]?.has(code) || false
}

function toggle(userId: number, code: string, checked: string | number | boolean) {
  selected.value[userId] ||= new Set()
  if (Boolean(checked)) selected.value[userId].add(code)
  else selected.value[userId].delete(code)
}

async function save(row: any) {
  const payload = Array.from(selected.value[row.id] || []).map((permissionCode) => ({ permissionCode, effect: 'allow' }))
  await apiPost(`/permission-matrix/users/${row.id}`, payload)
  ElMessage.success('权限已保存')
}
</script>
