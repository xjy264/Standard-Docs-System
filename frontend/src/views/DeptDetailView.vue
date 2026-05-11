<template>
  <div>
    <div class="page-title">
      <h2>{{ detail?.dept?.deptName || '组织详情' }}</h2>
      <el-button @click="$router.push('/depts')">返回组织管理</el-button>
    </div>

    <el-alert
      v-if="detail && !detail.assignable"
      :title="detail.assignableMessage"
      type="warning"
      :closable="false"
      show-icon
      class="detail-alert"
    />

    <div class="section">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="组织名称">{{ detail?.dept?.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="组织编码">{{ detail?.dept?.deptCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="层级">{{ isTopLevel(detail?.dept) ? '一级单位' : '二级单位' }}</el-descriptions-item>
        <el-descriptions-item label="配置用户">
          <el-tag :type="detail?.assignable ? 'success' : 'warning'">
            {{ detail?.assignable ? '可配置' : '不直接配置' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="直属用户">{{ detail?.users?.length || 0 }} 人</el-descriptions-item>
        <el-descriptions-item label="组织管理员">{{ detail?.admins?.length || 0 }} 人</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="section">
      <h3>组织管理员</h3>
      <el-table :data="detail?.admins || []" stripe>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="status" label="状态" width="110" />
      </el-table>
    </div>

    <div class="section">
      <h3>直属用户</h3>
      <el-table :data="detail?.users || []" stripe>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="identity" label="身份" width="130">
          <template #default="{ row }">
            <el-tag :type="identityTagType(row.identity)">{{ row.identity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="approvalStatus" label="审批状态" width="120" />
      </el-table>
    </div>

    <div class="section">
      <h3>用户权限</h3>
      <el-alert
        v-if="permissionError"
        :title="permissionError"
        type="warning"
        :closable="false"
        show-icon
        class="detail-alert"
      />
      <el-table v-else v-loading="permissionLoading" :data="permissionUsers" stripe>
        <el-table-column prop="realName" label="用户" min-width="130" fixed />
        <el-table-column
          v-for="permission in permissions"
          :key="permission.permissionCode"
          :label="permission.permissionName"
          min-width="150"
        >
          <template #default="{ row }">
            <el-checkbox
              :model-value="has(row.id, permission.permissionCode)"
              @change="toggle(row.id, permission.permissionCode, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="savePermissions(row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet, apiGetSilent, apiPost } from '../api/http'

const route = useRoute()
const router = useRouter()
const detail = ref<any>()
const permissionUsers = ref<any[]>([])
const permissions = ref<any[]>([])
const selected = ref<Record<number, Set<string>>>({})
const permissionLoading = ref(false)
const permissionError = ref('')

async function load() {
  try {
    detail.value = await apiGet(`/depts/${route.params.id}/detail`)
    await loadPermissions()
  } catch {
    router.push('/depts')
  }
}

async function loadPermissions() {
  permissionLoading.value = true
  permissionError.value = ''
  try {
    const result = await apiGetSilent<any>('/permission-matrix', { deptId: route.params.id })
    permissionUsers.value = result.users || []
    permissions.value = result.permissions || []
    selected.value = {}
    Object.entries(result.effective as Record<string, string[]>).forEach(([userId, codes]) => {
      selected.value[Number(userId)] = new Set(codes)
    })
  } catch (error: any) {
    permissionUsers.value = []
    permissions.value = []
    selected.value = {}
    permissionError.value = error?.message || '当前账号没有修改用户权限的权限'
  } finally {
    permissionLoading.value = false
  }
}

function isTopLevel(dept: any) {
  return !Number(dept?.parentId || 0)
}

function identityTagType(identity: string) {
  if (identity === '超级管理员') return 'danger'
  if (identity === '管理员') return 'warning'
  return 'info'
}

function has(userId: number, code: string) {
  return selected.value[userId]?.has(code) || false
}

function toggle(userId: number, code: string, checked: string | number | boolean) {
  selected.value[userId] ||= new Set()
  if (Boolean(checked)) selected.value[userId].add(code)
  else selected.value[userId].delete(code)
  selected.value = { ...selected.value }
}

async function savePermissions(row: any) {
  const payload = Array.from(selected.value[row.id] || []).map((permissionCode) => ({ permissionCode, effect: 'allow' }))
  await apiPost(`/permission-matrix/users/${row.id}`, payload)
  ElMessage.success('权限已保存')
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.detail-alert {
  margin-bottom: 14px;
}

.section h3 {
  margin: 0 0 14px;
  font-size: 16px;
}
</style>
