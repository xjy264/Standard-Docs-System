<template>
  <div>
    <div class="page-title">
      <div>
        <h2>资料目录设置</h2>
      </div>
      <div class="page-actions">
        <el-select
          v-if="auth.user?.isSuperAdmin"
          v-model="selectedSectionId"
          placeholder="选择科室"
          style="width: 220px"
          @change="loadTree"
        >
          <el-option v-for="section in sections" :key="section.id" :label="section.deptName" :value="section.id" />
        </el-select>
        <el-button type="primary" :disabled="!selectedSectionId" @click="openDialog">新增最高级文件夹</el-button>
      </div>
    </div>

    <el-table :data="rootFolders" stripe>
      <el-table-column type="index" label="序号" width="70" />
      <el-table-column prop="nodeName" label="文件夹名称" min-width="220" />
      <el-table-column prop="sortOrder" label="排序" width="120" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
    </el-table>

    <el-dialog v-model="dialogOpen" title="新增最高级文件夹" width="420px">
      <el-form label-position="top">
        <el-form-item label="文件夹名称"><el-input v-model="form.nodeName" maxlength="128" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitFolder">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { apiGet, apiPost } from '../api/http'
import { useAuthStore } from '../stores/auth'

interface SectionItem {
  id: number
  deptName: string
}

interface DocNode {
  id: number
  sectionDeptId: number
  parentId?: number
  nodeType: 'FOLDER' | 'FILE'
  nodeName: string
  sortOrder: number
  level: number
  updatedAt?: string
}

const auth = useAuthStore()
const sections = ref<SectionItem[]>([])
const selectedSectionId = ref<number>()
const treeData = ref<DocNode[]>([])
const dialogOpen = ref(false)
const form = reactive({ nodeName: '', sortOrder: 0 })
const rootFolders = computed(() => treeData.value.filter((node) => node.nodeType === 'FOLDER' && !node.parentId))

async function loadSections() {
  sections.value = await apiGet<SectionItem[]>('/sections/navigation')
  if (auth.user?.isSuperAdmin) {
    selectedSectionId.value = selectedSectionId.value || sections.value[0]?.id
  } else if (auth.user?.admin && auth.user?.deptId) {
    selectedSectionId.value = auth.user.deptId
  }
}

async function loadTree() {
  treeData.value = selectedSectionId.value ? await apiGet<DocNode[]>('/doc-tree', { sectionDeptId: selectedSectionId.value }) : []
}

function openDialog() {
  form.nodeName = ''
  form.sortOrder = 0
  dialogOpen.value = true
}

async function submitFolder() {
  if (!selectedSectionId.value || !form.nodeName.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  await apiPost('/doc-nodes/folders', {
    sectionDeptId: selectedSectionId.value,
    parentId: undefined,
    nodeName: form.nodeName.trim(),
    sortOrder: form.sortOrder
  })
  ElMessage.success('新增成功')
  dialogOpen.value = false
  await loadTree()
}

onMounted(async () => {
  await loadSections()
  await loadTree()
})
</script>

<style scoped>
.page-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
