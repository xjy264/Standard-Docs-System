<template>
  <div>
    <div class="page-title">
      <h2>组织管理</h2>
      <el-button type="primary" @click="openCreate">新增组织节点</el-button>
    </div>
    <el-alert
      v-if="auth.user?.isSuperAdmin && missingCoverage.length"
      type="warning"
      :closable="false"
      show-icon
      class="admin-warning"
    >
      <template #title>
        {{ missingCoverage.length }} 个一级组织未设置管理员：{{ missingCoverage.map((item) => item.deptName).join('、') }}
      </template>
    </el-alert>
    <div class="section">
      <el-table :data="treeRows" row-key="id" stripe default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="deptName" label="组织名称">
          <template #default="{ row }">
            <el-button link type="primary" @click="view(row)">{{ row.deptName }}</el-button>
            <el-tag
              v-if="auth.user?.isSuperAdmin && isTopLevel(row) && coverageMap.get(Number(row.id))?.missingAdmin"
              class="admin-tag"
              type="warning"
            >
              未设置管理员
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deptCode" label="组织编码" width="160" />
        <el-table-column label="层级" width="120">
          <template #default="{ row }">{{ isTopLevel(row) ? '一级单位' : '二级单位' }}</template>
        </el-table-column>
        <el-table-column v-if="auth.user?.isSuperAdmin" label="管理员" width="220">
          <template #default="{ row }">
            <span v-if="isTopLevel(row)">{{ adminNames(row) }}</span>
            <span v-else class="muted">随一级组织</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="view(row)">查看</el-button>
            <el-button link type="primary" @click="edit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="open" title="组织节点" width="460px">
      <el-form label-position="top">
        <el-form-item label="组织名称"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="组织编码"><el-input v-model="form.deptCode" /></el-form-item>
        <el-form-item label="上级 ID"><el-input-number v-model="form.parentId" :min="0" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status"><el-option label="启用" value="ENABLED" /><el-option label="禁用" value="DISABLED" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="open=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet, apiPost, apiPut } from '../api/http'
import { useAuthStore } from '../stores/auth'

const rows = ref<any[]>([])
const treeRows = computed(() => buildDeptTree(rows.value))
const coverage = ref<any[]>([])
const coverageMap = computed(() => new Map(coverage.value.map((item) => [Number(item.deptId), item])))
const missingCoverage = computed(() => coverage.value.filter((item) => item.missingAdmin))
const open = ref(false)
const form = reactive<any>({})
const auth = useAuthStore()
const router = useRouter()

async function load() {
  rows.value = await apiGet('/depts/tree')
  if (auth.user?.isSuperAdmin) {
    coverage.value = await apiGet('/depts/admin-coverage')
  }
}

function openCreate() {
  Object.assign(form, { id: null, parentId: 0, deptName: '', deptCode: '', sortOrder: 0, status: 'ENABLED' })
  open.value = true
}

function edit(row: any) {
  Object.assign(form, row)
  open.value = true
}

function view(row: any) {
  router.push(`/depts/${row.id}`)
}

async function save() {
  if (form.id) await apiPut(`/depts/${form.id}`, form)
  else await apiPost('/depts', form)
  ElMessage.success('保存成功')
  open.value = false
  load()
}

function buildDeptTree(list: any[]) {
  const nodeMap = new Map<number, any>()
  const result: any[] = []
  list.forEach((item) => nodeMap.set(Number(item.id), { ...item, children: [] }))
  nodeMap.forEach((item) => {
    const parentId = Number(item.parentId || 0)
    const parent = parentId ? nodeMap.get(parentId) : null
    if (parent) parent.children.push(item)
    else result.push(item)
  })
  const sortNodes = (nodes: any[]) => {
    nodes.sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
    nodes.forEach((node) => sortNodes(node.children))
  }
  sortNodes(result)
  return result
}

function isTopLevel(row: any) {
  return !Number(row.parentId || 0)
}

function adminNames(row: any) {
  const item = coverageMap.value.get(Number(row.id))
  if (!item || item.missingAdmin) return '未设置管理员'
  return item.adminNames.join('、')
}

onMounted(load)
</script>

<style scoped>
.admin-warning {
  margin-bottom: 14px;
}

.admin-tag {
  margin-left: 10px;
}

.muted {
  color: #909399;
}
</style>
