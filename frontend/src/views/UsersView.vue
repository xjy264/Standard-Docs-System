<template>
  <div>
    <div class="page-title">
      <h2>用户管理</h2>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>
    <div class="query-bar">
      <el-input v-model="keyword" clearable placeholder="按姓名搜索" style="width:260px" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <div class="section">
      <el-table :data="rows" stripe>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="deptName" label="所属组织" />
        <el-table-column prop="identity" label="身份" width="120">
          <template #default="{ row }">
            <el-tag :type="identityTagType(row.identity)">{{ row.identity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="approvalStatus" label="审批状态" width="120" />
        <el-table-column label="操作" width="320">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row)">编辑</el-button>
            <el-button link type="warning" @click="reset(row)">重置密码</el-button>
            <el-button
              v-if="auth.user?.isSuperAdmin && !row.isSuperAdmin && !row.admin"
              link
              type="success"
              @click="promote(row)"
            >
              设为管理员
            </el-button>
            <el-button
              v-if="auth.user?.isSuperAdmin && !row.isSuperAdmin && row.admin"
              link
              type="info"
              @click="demote(row)"
            >
              取消管理员
            </el-button>
            <el-button link type="danger" @click="toggle(row)">{{ row.status === 'ENABLED' ? '禁用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="open" title="用户信息" width="520px">
      <el-form label-position="top">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码" v-if="!form.id"><el-input v-model="form.password" type="password" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item v-if="auth.user?.isSuperAdmin" label="所属组织">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTree"
            node-key="id"
            :props="{ label: 'deptName', children: 'children', disabled: 'disabled' }"
            check-strictly
            default-expand-all
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="open=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { apiGet, apiPost, apiPut } from '../api/http'
import { useAuthStore } from '../stores/auth'

const rows = ref<any[]>([])
const deptRows = ref<any[]>([])
const deptTree = computed(() => buildDeptTree(deptRows.value))
const open = ref(false)
const keyword = ref('')
const form = reactive<any>({})
const auth = useAuthStore()

async function load() {
  rows.value = await apiGet('/users', { keyword: keyword.value })
}

async function loadDepts() {
  deptRows.value = await apiGet('/depts/tree')
}

function openCreate() {
  Object.assign(form, {
    id: null,
    username: '',
    password: '',
    realName: '',
    phone: '',
    deptId: auth.user?.isSuperAdmin ? undefined : auth.user?.deptId
  })
  open.value = true
}

function edit(row: any) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    phone: row.phone,
    deptId: row.deptId,
    status: row.status,
    approvalStatus: row.approvalStatus,
    isSuperAdmin: row.isSuperAdmin
  })
  open.value = true
}

async function save() {
  if (form.id) await apiPut(`/users/${form.id}`, form)
  else await apiPost('/users', form)
  ElMessage.success('保存成功')
  open.value = false
  load()
}

async function toggle(row: any) {
  await apiPost(`/users/${row.id}/${row.status === 'ENABLED' ? 'disable' : 'enable'}`)
  load()
}

async function reset(row: any) {
  const { value } = await ElMessageBox.prompt('请输入新密码', '重置密码')
  await apiPost(`/users/${row.id}/reset-password`, undefined, { password: value })
  ElMessage.success('重置成功')
}

async function promote(row: any) {
  await ElMessageBox.confirm(`确认将 ${row.realName || row.username} 设为管理员？`, '设为管理员')
  await apiPost(`/users/${row.id}/promote-admin`)
  ElMessage.success('已设为管理员')
  load()
}

async function demote(row: any) {
  await ElMessageBox.confirm(`确认取消 ${row.realName || row.username} 的管理员身份？`, '取消管理员')
  await apiPost(`/users/${row.id}/demote-admin`)
  ElMessage.success('已取消管理员')
  load()
}

function identityTagType(identity: string) {
  if (identity === '超级管理员') return 'danger'
  if (identity === '管理员') return 'warning'
  return 'info'
}

function buildDeptTree(list: any[]) {
  const childrenByParent = new Map<number, any[]>()
  const ids = new Set(list.map((item) => Number(item.id)))
  list.forEach((item) => {
    const parentId = Number(item.parentId || 0)
    const children = childrenByParent.get(parentId) || []
    children.push(item)
    childrenByParent.set(parentId, children)
  })

  const buildNode = (item: any): any => {
    const id = Number(item.id)
    const children = (childrenByParent.get(id) || []).map(buildNode)
    const topLevel = !Number(item.parentId || 0)
    return {
      ...item,
      disabled: topLevel && children.length > 0,
      children
    }
  }

  return list
    .filter((item) => Number(item.parentId || 0) === 0 || !ids.has(Number(item.parentId)))
    .map(buildNode)
}

onMounted(() => {
  load()
  loadDepts()
})
</script>
