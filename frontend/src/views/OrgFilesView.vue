<template>
  <div class="section-workspace">
    <div class="page-title compact-title">
      <div>
        <h2>{{ currentSection?.deptName || '科室资料' }}</h2>
      </div>
      <div v-if="canManageSection" class="title-actions">
        <el-button type="primary" @click="openFolderDialog()">新增文件夹</el-button>
        <el-button @click="openFileDialog()">新增文件</el-button>
      </div>
    </div>

    <section class="doc-tree-panel">
      <el-tree
        v-if="treeData.length"
        :data="treeData"
        node-key="id"
        :props="{ children: 'children', label: 'nodeName' }"
        :indent="26"
        :expand-on-click-node="false"
        default-expand-all
      >
        <template #default="{ node, data }">
          <div class="doc-tree-row" :class="{ 'is-file': data.nodeType === 'FILE' }">
            <div class="doc-tree-title" @click="data.nodeType === 'FILE' && openItemDetail(data)">
              <el-icon class="doc-tree-icon">
                <Folder v-if="data.nodeType === 'FOLDER'" />
                <Document v-else />
              </el-icon>
              <span>{{ node.label }}</span>
              <el-tag v-if="data.nodeType === 'FILE' && data.attachmentEnabled" size="small" type="success">允许上传</el-tag>
              <span v-if="data.nodeType === 'FILE'" class="submission-count">上传记录：{{ data.submissionCount || 0 }}</span>
            </div>
            <div v-if="canManageSection" class="doc-tree-actions">
              <el-button v-if="data.nodeType === 'FOLDER' && data.level < 5" link type="primary" @click.stop="openFolderDialog(data)">新增文件夹</el-button>
              <el-button v-if="data.nodeType === 'FOLDER'" link type="primary" @click.stop="openFileDialog(data)">新增文件</el-button>
              <el-button link type="primary" @click.stop="openEditDialog(data)">编辑</el-button>
              <el-button link type="danger" @click.stop="deleteNode(data)">删除</el-button>
            </div>
          </div>
        </template>
      </el-tree>
      <el-empty v-else description="暂无资料目录" />
    </section>

    <el-dialog
      v-model="nodeDialogOpen"
      :title="dialogTitle"
      width="920px"
      destroy-on-close
      @closed="destroyEditor"
    >
      <el-form label-position="top">
        <el-form-item :label="nodeForm.nodeType === 'FOLDER' ? '文件夹名称' : '文件名称'">
          <el-input v-model="nodeForm.nodeName" maxlength="128" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="nodeForm.sortOrder" :min="0" /></el-form-item>
        <template v-if="nodeForm.nodeType === 'FILE'">
          <el-form-item label="文件内容">
            <div class="editor-box">
              <Toolbar :editor="editorRef" :default-config="toolbarConfig" mode="default" />
              <Editor
                v-model="nodeForm.contentHtml"
                :default-config="editorConfig"
                mode="default"
                class="content-editor"
                style="height: 300px; overflow-y: hidden"
                @on-created="handleEditorCreated"
              />
            </div>
          </el-form-item>
          <el-form-item label="收集设置">
            <el-checkbox v-model="nodeForm.attachmentEnabled">允许上传附件</el-checkbox>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitNode">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { Document, Folder } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, reactive, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiDelete, apiGet, apiPost, apiPut } from '../api/http'
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
  itemId?: number
  sortOrder: number
  level: number
  attachmentEnabled?: number
  submissionCount?: number
  children?: DocNode[]
}

interface DocItem {
  id: number
  itemName: string
  contentHtml?: string
  attachmentEnabled: number
  sortOrder: number
}

type DialogMode = 'create' | 'edit'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const deptId = computed(() => Number(route.params.deptId))
const sections = ref<SectionItem[]>([])
const treeData = ref<DocNode[]>([])
const nodeDialogOpen = ref(false)
const dialogMode = ref<DialogMode>('create')
const editingNode = ref<DocNode>()
const editorRef = shallowRef<IDomEditor>()
const toolbarConfig: Partial<IToolbarConfig> = {}
const editorConfig: Partial<IEditorConfig> = { placeholder: '请输入文件内容' }
const nodeForm = reactive({
  nodeType: 'FOLDER' as 'FOLDER' | 'FILE',
  parentId: undefined as number | undefined,
  nodeName: '',
  sortOrder: 0,
  attachmentEnabled: false,
  contentHtml: ''
})

const currentSection = computed(() => sections.value.find((item) => item.id === deptId.value))
const canManageSection = computed(() => Boolean(auth.user?.isSuperAdmin) || auth.user?.deptId === deptId.value)
const dialogTitle = computed(() => {
  if (dialogMode.value === 'edit') {
    return nodeForm.nodeType === 'FOLDER' ? '编辑文件夹' : '编辑文件'
  }
  return nodeForm.nodeType === 'FOLDER' ? '新增文件夹' : '新增文件'
})

async function load() {
  sections.value = await apiGet<SectionItem[]>('/sections/navigation')
  await loadTree()
}

async function loadTree() {
  treeData.value = await apiGet<DocNode[]>('/doc-tree', { sectionDeptId: deptId.value })
}

function resetForm(type: 'FOLDER' | 'FILE', parent?: DocNode) {
  editingNode.value = undefined
  dialogMode.value = 'create'
  nodeForm.nodeType = type
  nodeForm.parentId = parent?.id
  nodeForm.nodeName = ''
  nodeForm.sortOrder = 0
  nodeForm.attachmentEnabled = type === 'FILE'
  nodeForm.contentHtml = ''
}

function openFolderDialog(parent?: DocNode) {
  resetForm('FOLDER', parent)
  nodeDialogOpen.value = true
}

function openFileDialog(parent?: DocNode) {
  resetForm('FILE', parent)
  nodeDialogOpen.value = true
}

async function openEditDialog(node: DocNode) {
  editingNode.value = node
  dialogMode.value = 'edit'
  nodeForm.nodeType = node.nodeType
  nodeForm.parentId = node.parentId
  nodeForm.nodeName = node.nodeName
  nodeForm.sortOrder = node.sortOrder || 0
  nodeForm.attachmentEnabled = Boolean(node.attachmentEnabled)
  nodeForm.contentHtml = ''
  if (node.nodeType === 'FILE' && node.itemId) {
    const item = await apiGet<DocItem>(`/doc-items/${node.itemId}`)
    nodeForm.contentHtml = item.contentHtml || ''
    nodeForm.attachmentEnabled = Boolean(item.attachmentEnabled)
  }
  nodeDialogOpen.value = true
}

async function submitNode() {
  if (!nodeForm.nodeName.trim()) {
    ElMessage.warning(nodeForm.nodeType === 'FOLDER' ? '请输入文件夹名称' : '请输入文件名称')
    return
  }
  const body = {
    sectionDeptId: deptId.value,
    parentId: nodeForm.parentId,
    nodeName: nodeForm.nodeName.trim(),
    sortOrder: nodeForm.sortOrder,
    attachmentEnabled: nodeForm.attachmentEnabled,
    contentHtml: nodeForm.contentHtml
  }
  if (dialogMode.value === 'edit' && editingNode.value) {
    await apiPut(`/doc-nodes/${editingNode.value.id}`, body)
    ElMessage.success('修改成功')
  } else if (nodeForm.nodeType === 'FOLDER') {
    await apiPost('/doc-nodes/folders', body)
    ElMessage.success('新增成功')
  } else {
    await apiPost('/doc-nodes/files', body)
    ElMessage.success('新增成功')
  }
  nodeDialogOpen.value = false
  await loadTree()
}

async function deleteNode(node: DocNode) {
  await ElMessageBox.confirm(`确定删除“${node.nodeName}”吗？`, '删除资料节点', { type: 'warning' })
  await apiDelete(`/doc-nodes/${node.id}`)
  ElMessage.success('删除成功')
  await loadTree()
}

function openItemDetail(node: DocNode) {
  if (!node.itemId) {
    return
  }
  router.push(`/org/${deptId.value}/items/${node.itemId}`)
}

function handleEditorCreated(editor: IDomEditor) {
  editorRef.value = editor
}

function destroyEditor() {
  editorRef.value?.destroy()
  editorRef.value = undefined
}

onMounted(load)
onBeforeUnmount(destroyEditor)
watch(() => route.params.deptId, load)
</script>

<style scoped>
.section-workspace {
  height: 100%;
}

.compact-title {
  margin-bottom: 12px;
}

.title-actions {
  display: flex;
  gap: 8px;
}

.doc-tree-panel {
  min-height: calc(100vh - 150px);
  padding: 12px 14px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 6px;
}

.doc-tree-panel :deep(.el-tree-node__content) {
  height: auto;
  min-height: 42px;
  align-items: stretch;
}

.doc-tree-row {
  width: 100%;
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 8px 6px 0;
}

.doc-tree-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1f2d3d;
  line-height: 1.5;
}

.doc-tree-row.is-file .doc-tree-title {
  cursor: pointer;
  color: var(--rail-blue-dark);
  font-weight: 600;
}

.doc-tree-icon {
  flex: 0 0 auto;
}

.submission-count {
  color: #6b7280;
  font-size: 12px;
  font-weight: 400;
}

.doc-tree-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 2px;
}

.editor-box {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 6px;
  overflow: hidden;
}

.content-editor {
  min-height: 300px;
}
</style>
