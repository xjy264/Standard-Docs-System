<template>
  <div class="recycle-page">
    <div class="page-title compact-title">
      <div>
        <el-button link type="primary" @click="router.push(`/org/${deptId}`)">返回科室资料</el-button>
        <h2>回收站</h2>
        <p class="page-subtitle">已删除文件保留 30 天，超过期限后系统将自动清理相关文件。</p>
      </div>
    </div>

    <section class="recycle-panel">
      <el-table v-if="items.length" :data="items" stripe>
        <el-table-column prop="nodeName" label="文件名称" min-width="220" />
        <el-table-column prop="fileType" label="类型" width="110">
          <template #default="{ row }">{{ fileTypeText(row.fileType) }}</template>
        </el-table-column>
        <el-table-column prop="docYear" label="年份" width="100" />
        <el-table-column prop="originalParentName" label="原目录" min-width="160">
          <template #default="{ row }">{{ row.originalParentName || '未记录' }}</template>
        </el-table-column>
        <el-table-column prop="attachmentCount" label="正文附件" width="110" />
        <el-table-column prop="submissionCount" label="车间提交" width="110" />
        <el-table-column prop="deletedByName" label="删除人" width="120">
          <template #default="{ row }">{{ row.deletedByName || '未记录' }}</template>
        </el-table-column>
        <el-table-column prop="deletedAt" label="删除时间" width="180">
          <template #default="{ row }">{{ formatDate(row.deletedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRestore(row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="回收站暂无文件" />
    </section>

    <el-dialog v-model="restoreOpen" title="恢复文件" width="520px" @closed="targetParentId = undefined">
      <p class="restore-name">{{ restoringItem?.nodeName }}</p>
      <el-tree
        v-if="folderTree.length"
        :data="folderTree"
        node-key="id"
        :props="{ children: 'children', label: 'nodeName' }"
        highlight-current
        @node-click="selectFolder"
      />
      <el-empty v-else description="暂无可恢复目录" />
      <template #footer>
        <el-button @click="restoreOpen = false">取消</el-button>
        <el-button type="primary" :loading="restoring" @click="restoreSelected">恢复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet, apiPost } from '../api/http'

interface RecycleItem {
  id: number
  itemId: number
  nodeName: string
  fileType?: string
  docYear?: number
  deletedAt?: string
  deletedByName?: string
  originalParentName?: string
  submissionCount?: number
  attachmentCount?: number
}

interface DocNode {
  id: number
  nodeType: 'FOLDER' | 'FILE'
  nodeName: string
  children?: DocNode[]
}

const route = useRoute()
const router = useRouter()
const deptId = computed(() => Number(route.params.deptId))
const items = ref<RecycleItem[]>([])
const folderTree = ref<DocNode[]>([])
const restoreOpen = ref(false)
const restoring = ref(false)
const restoringItem = ref<RecycleItem>()
const targetParentId = ref<number>()

async function load() {
  const [recycleItems, tree] = await Promise.all([
    apiGet<RecycleItem[]>('/doc-nodes/recycle-bin', { sectionDeptId: deptId.value }),
    apiGet<DocNode[]>('/doc-tree', { sectionDeptId: deptId.value })
  ])
  items.value = recycleItems
  folderTree.value = foldersOnly(tree)
}

function foldersOnly(nodes: DocNode[]): DocNode[] {
  return nodes
    .filter((node) => node.nodeType === 'FOLDER')
    .map((node) => ({
      ...node,
      children: foldersOnly(node.children || [])
    }))
}

function openRestore(item: RecycleItem) {
  restoringItem.value = item
  targetParentId.value = undefined
  restoreOpen.value = true
}

function selectFolder(node: DocNode) {
  targetParentId.value = node.id
}

async function restoreSelected() {
  if (!restoringItem.value || !targetParentId.value) {
    ElMessage.warning('请选择恢复目录')
    return
  }
  restoring.value = true
  try {
    await apiPost(`/doc-nodes/${restoringItem.value.id}/restore`, { targetParentId: targetParentId.value })
    ElMessage.success('恢复成功')
    restoreOpen.value = false
    await load()
  } finally {
    restoring.value = false
  }
}

function fileTypeText(type?: string) {
  const labels: Record<string, string> = {
    WORD: 'Word',
    EXCEL: 'Excel',
    PPT: 'PPT',
    PDF: 'PDF',
    IMAGE: '图片',
    ZIP: 'ZIP',
    OTHER: '其他'
  }
  return labels[type || 'OTHER'] || '其他'
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '未记录'
}

onMounted(load)
watch(() => route.params.deptId, () => load())
</script>

<style scoped>
.recycle-page {
  height: 100%;
}

.compact-title {
  margin-bottom: 12px;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #637083;
}

.recycle-panel {
  min-height: calc(100vh - 198px);
  padding: 12px 14px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 6px;
}

.restore-name {
  margin: 0 0 12px;
  color: #1f2d3d;
  font-weight: 600;
}
</style>

