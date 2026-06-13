<template>
  <div class="section-workspace">
    <div class="page-title compact-title">
      <div>
        <h2>{{ currentSection?.deptName || '科室资料' }}</h2>
      </div>
    </div>

    <section class="doc-tree-panel">
      <div v-if="canManageSection" class="tree-toolbar">
        <el-button type="primary" plain @click="openFolderDialog()">新增文件夹</el-button>
        <el-button type="primary" @click="openFileDialog()">发布通知文件</el-button>
      </div>

      <div class="doc-search-bar">
        <el-select v-model="selectedYear" placeholder="年份" class="year-select">
          <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
        </el-select>
        <el-input v-model="searchKeyword" clearable maxlength="128" placeholder="按文件名搜索" class="keyword-input" />
        <el-button type="primary" @click="submitSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table v-if="searchMode && searchResultFiles.length" :data="searchResultFiles" stripe class="doc-file-table">
        <el-table-column label="文件名称" min-width="280">
          <template #default="{ row }">
            <button class="file-link" type="button" @click="openItemDetail(row)">
              <span class="doc-file-icon" :class="fileTypeClass(row)">
                {{ fileTypeLabel(row) }}
              </span>
              <span>{{ row.nodeName }}</span>
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="docYear" label="年份" width="110" />
        <el-table-column label="文件类型" width="110">
          <template #default="{ row }">{{ fileTypeText(row) }}</template>
        </el-table-column>
        <el-table-column prop="submissionCount" label="上传记录" width="110" />
        <el-table-column v-if="canManageSection" label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteNode(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-tree
        v-else-if="!searchMode && yearTreeData.length"
        :data="yearTreeData"
        ref="treeRef"
        node-key="id"
        :props="{ children: 'children', label: 'nodeName' }"
        :indent="26"
        :expand-on-click-node="true"
      >
        <template #default="{ node, data }">
          <div class="doc-tree-row" :class="{ 'is-folder': data.nodeType === 'FOLDER', 'is-file': data.nodeType === 'FILE' }">
            <div class="doc-tree-main">
              <div class="doc-tree-title" @click="data.nodeType === 'FILE' && openItemDetail(data)">
                <el-icon v-if="data.nodeType === 'FOLDER'" class="doc-tree-icon">
                  <Folder />
                </el-icon>
                <span v-if="data.nodeType === 'FILE'" class="doc-file-icon" :class="fileTypeClass(data)">
                  {{ fileTypeLabel(data) }}
                </span>
                <span>{{ node.label }}</span>
              </div>
              <div
                v-if="data.nodeType === 'FOLDER' && data.uploadTaskCount"
                class="folder-progress"
              >
                <el-progress :percentage="data.progressPercent || 0" :show-text="false" />
                <span>已完成 {{ data.completedUploadTaskCount || 0 }}/{{ data.uploadTaskCount || 0 }}</span>
              </div>
            </div>
            <div v-if="canManageSection" class="doc-tree-actions">
              <el-button v-if="data.nodeType === 'FOLDER' && data.level < 5" link type="primary" @click.stop="openFolderDialog(data)">新增文件夹</el-button>
              <el-button v-if="data.nodeType === 'FOLDER' && isRepairFolder(data)" link type="primary" @click.stop="openImportDialog(data)">从项目模板导入</el-button>
              <el-button v-if="data.nodeType === 'FOLDER'" link type="primary" @click.stop="openFileDialog(data)">发布通知文件</el-button>
              <el-button link type="primary" @click.stop="openEditDialog(data)">编辑</el-button>
              <el-button link type="danger" @click.stop="deleteNode(data)">删除</el-button>
            </div>
          </div>
        </template>
      </el-tree>
      <el-empty v-else :description="emptyDescription" />
    </section>

    <el-dialog
      v-model="nodeDialogOpen"
      :title="dialogTitle"
      width="920px"
      destroy-on-close
      @closed="handleDialogClosed"
    >
      <el-form label-position="top">
        <el-form-item :label="nodeForm.nodeType === 'FOLDER' ? '文件夹名称' : '通知文件名称'">
          <el-input v-model="nodeForm.nodeName" maxlength="128" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="nodeForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item v-if="nodeForm.nodeType === 'FILE'" label="文件年份">
          <el-select v-model="nodeForm.docYear" placeholder="请选择文件年份" style="width: 220px">
            <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
          </el-select>
        </el-form-item>
        <template v-if="nodeForm.nodeType === 'FILE'">
          <el-form-item label="文件类型">
            <el-select v-model="nodeForm.fileType" placeholder="请选择文件类型" style="width: 220px">
              <el-option
                v-for="option in fileTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="正文附件">
            <el-upload
              ref="issuedUploadRef"
              class="issued-upload"
              drag
              multiple
              :auto-upload="false"
              :on-change="onIssuedFileChange"
              :on-remove="onIssuedFileRemove"
            >
              <div>拖拽附件到此处，或点击选择文件</div>
            </el-upload>
          </el-form-item>
          <el-form-item label="可见车间">
            <el-select
              v-model="nodeForm.visibleWorkshopIds"
              multiple
              clearable
              placeholder="默认全部车间可见"
              style="width: 100%"
            >
              <el-option
                v-for="dept in workshopOptions"
                :key="dept.id"
                :label="dept.deptName"
                :value="dept.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="车间上传">
            <el-switch v-model="nodeForm.workshopUploadEnabled" active-text="需要车间上传" inactive-text="不需要" />
          </el-form-item>
          <el-form-item v-if="nodeForm.workshopUploadEnabled" label="上传截止时间">
            <el-date-picker
              v-model="nodeForm.uploadDeadline"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="请选择截止时间"
              style="width: 260px"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitNode">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogOpen" title="从项目模板导入" width="560px">
      <el-form label-position="top">
        <el-form-item label="项目文件夹名称"><el-input v-model="importForm.projectFolderName" maxlength="128" /></el-form-item>
        <el-form-item label="文件年份">
          <el-select v-model="importForm.docYear" style="width: 220px">
            <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目模板">
          <el-select v-model="importForm.templateId" style="width: 100%">
            <el-option v-for="template in repairTemplates" :key="template.id" :label="template.templateName" :value="template.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitImportTemplate">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Folder } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type UploadFile, type UploadFiles, type UploadRawFile } from 'element-plus'
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiDelete, apiGet, apiPost, apiPut, http } from '../api/http'
import { useAuthStore } from '../stores/auth'

interface SectionItem {
  id: number
  deptName: string
}

interface DeptItem {
  id: number
  deptName: string
  deptType?: string
}

interface RepairTemplate {
  id: number
  templateName: string
}

interface DocUploadRequirement {
  id?: number
  requirementName: string
  description?: string
  sortOrder: number
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
  fileType?: FileType
  docYear?: number
  businessType?: BusinessType
  submitterMode?: SubmitterMode
  workshopUploadEnabled?: number
  uploadDeadline?: string
  visibilityScope?: string
  visibleWorkshopIds?: number[]
  uploadTaskCount?: number
  completedUploadTaskCount?: number
  progressPercent?: number
  children?: DocNode[]
}

interface DocItem {
  id: number
  itemName: string
  contentHtml?: string
  attachmentEnabled: number
  sortOrder: number
  fileType?: FileType
  docYear?: number
  businessType?: BusinessType
  submitterMode?: SubmitterMode
  workshopUploadEnabled?: number
  uploadDeadline?: string
  visibilityScope?: string
  visibleWorkshopIds?: number[]
  requirements?: DocUploadRequirement[]
}

interface SavedTreeState {
  expandedKeys: number[]
  scrollTop: number
}

type DialogMode = 'create' | 'edit'
type FileType = 'WORD' | 'EXCEL' | 'PPT' | 'PDF' | 'IMAGE' | 'ZIP' | 'OTHER'
type BusinessType = 'UPLOAD' | 'ISSUED'
type SubmitterMode = 'SINGLE' | 'MULTIPLE'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const deptId = computed(() => Number(route.params.deptId))
const sections = ref<SectionItem[]>([])
const depts = ref<DeptItem[]>([])
const treeData = ref<DocNode[]>([])
const treeRef = ref()
const issuedUploadRef = ref<any>()
const nodeDialogOpen = ref(false)
const importDialogOpen = ref(false)
const dialogMode = ref<DialogMode>('create')
const editingNode = ref<DocNode>()
const importingParent = ref<DocNode>()
const repairTemplates = ref<RepairTemplate[]>([])
const currentYear = new Date().getFullYear()
const selectedYear = ref(currentYear)
const searchKeyword = ref('')
const activeKeyword = ref('')
const issuedFiles = ref<UploadRawFile[]>([])
const yearOptions = Array.from({ length: 21 }, (_, index) => 2016 + index)
const fileTypeOptions: Array<{ label: string; value: FileType }> = [
  { label: 'Word', value: 'WORD' },
  { label: 'Excel', value: 'EXCEL' },
  { label: 'PPT', value: 'PPT' },
  { label: 'PDF', value: 'PDF' },
  { label: 'ZIP', value: 'ZIP' },
  { label: '图片', value: 'IMAGE' },
  { label: '其他', value: 'OTHER' }
]
const nodeForm = reactive({
  nodeType: 'FOLDER' as 'FOLDER' | 'FILE',
  parentId: undefined as number | undefined,
  nodeName: '',
  sortOrder: 0,
  fileType: 'PDF' as FileType | '',
  docYear: currentYear,
  businessType: 'ISSUED' as BusinessType,
  submitterMode: 'SINGLE' as SubmitterMode,
  workshopUploadEnabled: false,
  uploadDeadline: '' as string | '',
  visibleWorkshopIds: [] as number[],
  requirements: [{ requirementName: '附件', description: '', sortOrder: 0 }] as DocUploadRequirement[]
})
const importForm = reactive({
  projectFolderName: '',
  docYear: currentYear,
  templateId: undefined as number | undefined
})

const currentSection = computed(() => sections.value.find((item) => item.id === deptId.value))
const canManageSection = computed(() => Boolean(auth.user?.isSuperAdmin) || auth.user?.deptId === deptId.value)
const workshopOptions = computed(() => depts.value.filter((dept) => dept.deptType === 'WORKSHOP' || dept.deptName.includes('车间')))
const allFiles = computed(() => flattenFiles(treeData.value))
const searchMode = computed(() => Boolean(activeKeyword.value))
const yearTreeData = computed(() => filterTreeByYear(treeData.value, selectedYear.value))
const searchResultFiles = computed(() => {
  const keyword = activeKeyword.value
  return allFiles.value.filter((file) => {
    const matchesYear = nodeYear(file) === selectedYear.value
    const matchesKeyword = file.nodeName.includes(keyword)
    return matchesYear && matchesKeyword
  })
})
const emptyDescription = computed(() => {
  if (searchMode.value) return '暂无匹配文件'
  if (!yearTreeData.value.length) return '暂无该年份资料目录'
  return '暂无通知文件'
})
const dialogTitle = computed(() => {
  if (dialogMode.value === 'edit') {
    if (nodeForm.nodeType === 'FOLDER') return '编辑文件夹'
    return '编辑通知文件'
  }
  if (nodeForm.nodeType === 'FOLDER') return '新增文件夹'
  return '发布通知文件'
})
const treeStateKey = computed(() => `org-tree-state:${deptId.value}`)

async function load(restore = route.query.restore === '1') {
  sections.value = await apiGet<SectionItem[]>('/sections/navigation')
  depts.value = await apiGet<DeptItem[]>('/depts/tree')
  await loadTree(restore)
}

async function loadTree(restore = false) {
  treeData.value = await apiGet<DocNode[]>('/doc-tree', { sectionDeptId: deptId.value })
  await nextTick()
  if (restore) {
    restoreTreeState()
  }
}

function resetForm(type: 'FOLDER' | 'FILE', parent?: DocNode) {
  editingNode.value = undefined
  dialogMode.value = 'create'
  nodeForm.nodeType = type
  nodeForm.parentId = parent?.id
  nodeForm.nodeName = ''
  nodeForm.sortOrder = 0
  nodeForm.docYear = parent?.docYear || selectedYear.value || currentYear
  nodeForm.fileType = type === 'FILE' ? 'PDF' : ''
  nodeForm.businessType = 'ISSUED'
  nodeForm.submitterMode = 'SINGLE'
  nodeForm.workshopUploadEnabled = false
  nodeForm.uploadDeadline = ''
  nodeForm.visibleWorkshopIds = []
  nodeForm.requirements = [{ requirementName: '附件', description: '', sortOrder: 0 }]
  issuedFiles.value = []
}

function openFolderDialog(parent?: DocNode) {
  resetForm('FOLDER', parent)
  nodeDialogOpen.value = true
}

function openFileDialog(parent?: DocNode) {
  resetForm('FILE', parent)
  nodeDialogOpen.value = true
}

async function openImportDialog(parent: DocNode) {
  importingParent.value = parent
  importForm.projectFolderName = ''
  importForm.docYear = parent.docYear || selectedYear.value || currentYear
  repairTemplates.value = await apiGet<RepairTemplate[]>('/repair-project-templates')
  importForm.templateId = repairTemplates.value[0]?.id
  importDialogOpen.value = true
}

async function submitImportTemplate() {
  if (!importingParent.value) return
  if (!importForm.projectFolderName.trim()) {
    ElMessage.warning('请输入项目文件夹名称')
    return
  }
  if (!importForm.templateId) {
    ElMessage.warning('请选择项目模板')
    return
  }
  const node = await apiPost<DocNode>(`/repair-project-templates/import/${importingParent.value.id}`, {
    templateId: importForm.templateId,
    projectFolderName: importForm.projectFolderName.trim(),
    docYear: importForm.docYear
  })
  importDialogOpen.value = false
  ElMessage.success('导入成功')
  await afterNodeChanged(node)
}

async function openEditDialog(node: DocNode) {
  editingNode.value = node
  dialogMode.value = 'edit'
  nodeForm.nodeType = node.nodeType
  nodeForm.parentId = node.parentId
  nodeForm.nodeName = node.nodeName
  nodeForm.sortOrder = node.sortOrder || 0
  nodeForm.fileType = node.nodeType === 'FILE' ? node.fileType || guessFileType(node.nodeName) : ''
  nodeForm.docYear = node.docYear || selectedYear.value || currentYear
  nodeForm.businessType = node.nodeType === 'FILE' ? node.businessType || 'ISSUED' : 'ISSUED'
  nodeForm.submitterMode = node.submitterMode || 'SINGLE'
  nodeForm.workshopUploadEnabled = Boolean(node.workshopUploadEnabled)
  nodeForm.uploadDeadline = node.uploadDeadline || ''
  nodeForm.visibleWorkshopIds = node.visibleWorkshopIds || []
  nodeForm.requirements = [{ requirementName: '附件', description: '', sortOrder: 0 }]
  issuedFiles.value = []
  if (node.nodeType === 'FILE' && node.itemId) {
    const item = await apiGet<DocItem>(`/doc-items/${node.itemId}`)
    nodeForm.fileType = item.fileType || node.fileType || guessFileType(node.nodeName)
    nodeForm.docYear = item.docYear || node.docYear || currentYear
    nodeForm.businessType = item.businessType || node.businessType || 'ISSUED'
    nodeForm.submitterMode = item.submitterMode || 'SINGLE'
    nodeForm.workshopUploadEnabled = Boolean(item.workshopUploadEnabled)
    nodeForm.uploadDeadline = item.uploadDeadline || ''
    nodeForm.visibleWorkshopIds = item.visibleWorkshopIds || []
    nodeForm.requirements = item.requirements?.length
      ? item.requirements.map((requirement, index) => ({
          id: requirement.id,
          requirementName: requirement.requirementName,
          description: requirement.description || '',
          sortOrder: requirement.sortOrder ?? index
        }))
      : [{ requirementName: '附件', description: '', sortOrder: 0 }]
  }
  nodeDialogOpen.value = true
}

function addRequirement() {
  nodeForm.requirements.push({ requirementName: '', description: '', sortOrder: nodeForm.requirements.length })
}

function removeRequirement(index: number) {
  if (nodeForm.requirements.length === 1) {
    ElMessage.warning('至少保留一个收集项')
    return
  }
  nodeForm.requirements.splice(index, 1)
}

async function submitNode() {
  if (!nodeForm.nodeName.trim()) {
    ElMessage.warning(nodeForm.nodeType === 'FOLDER' ? '请输入文件夹名称' : '请输入文件名称')
    return
  }
  if (nodeForm.nodeType === 'FILE' && !nodeForm.fileType) {
    ElMessage.warning('请选择文件类型')
    return
  }
  if (nodeForm.nodeType === 'FILE' && !nodeForm.docYear) {
    ElMessage.warning('请选择文件年份')
    return
  }
  if (nodeForm.nodeType === 'FILE' && nodeForm.workshopUploadEnabled && !nodeForm.uploadDeadline) {
    ElMessage.warning('请选择上传截止时间')
    return
  }
  const body = {
    sectionDeptId: deptId.value,
    parentId: nodeForm.parentId,
    nodeName: nodeForm.nodeName.trim(),
    sortOrder: nodeForm.sortOrder,
    docYear: nodeForm.docYear,
    contentHtml: '',
    fileType: nodeForm.fileType,
    businessType: nodeForm.workshopUploadEnabled ? 'UPLOAD' : 'ISSUED',
    submitterMode: 'MULTIPLE',
    uploadDeadline: nodeForm.workshopUploadEnabled ? nodeForm.uploadDeadline || null : null,
    workshopUploadEnabled: nodeForm.workshopUploadEnabled,
    visibleWorkshopIds: nodeForm.visibleWorkshopIds,
    requirements: nodeForm.workshopUploadEnabled
      ? [{ requirementName: '附件', description: '', sortOrder: 0 }]
      : []
  }
  let changedNode: DocNode | undefined
  if (dialogMode.value === 'edit' && editingNode.value) {
    changedNode = await apiPut<DocNode>(`/doc-nodes/${editingNode.value.id}`, body)
  } else if (nodeForm.nodeType === 'FOLDER') {
    changedNode = await apiPost<DocNode>('/doc-nodes/folders', body)
  } else {
    changedNode = await apiPost<DocNode>('/doc-nodes/files', body)
  }
  if (nodeForm.nodeType === 'FILE' && issuedFiles.value.length && changedNode?.itemId) {
    const form = new FormData()
    issuedFiles.value.forEach((file) => form.append('files', file))
    await http.post(`/doc-items/${changedNode.itemId}/body-attachments`, form)
  }
  nodeDialogOpen.value = false
  await afterNodeChanged(changedNode)
  ElMessage.success(dialogMode.value === 'edit' ? '修改成功' : '新增成功')
}

async function afterNodeChanged(node?: DocNode) {
  await loadTree()
  if (node?.docYear) {
    selectedYear.value = node.docYear
  }
  await expandChangedNode(node)
}

async function expandChangedNode(node?: DocNode) {
  await nextTick()
  if (!node || searchMode.value) {
    return
  }
  if (node.parentId) {
    treeRef.value?.getNode?.(node.parentId)?.expand?.()
  }
  if (node.nodeType === 'FOLDER') {
    treeRef.value?.getNode?.(node.id)?.expand?.()
  }
}

async function deleteNode(node: DocNode) {
  await ElMessageBox.confirm(`确定删除“${node.nodeName}”吗？`, '删除资料节点', { type: 'warning' })
  await apiDelete(`/doc-nodes/${node.id}`)
  ElMessage.success('删除成功')
  await loadTree(false)
}

function flattenFiles(nodes: DocNode[]): DocNode[] {
  const files: DocNode[] = []
  for (const node of nodes) {
    if (node.nodeType === 'FILE') {
      files.push(node)
    }
    if (node.children?.length) {
      files.push(...flattenFiles(node.children))
    }
  }
  return files
}

function filterTreeByYear(nodes: DocNode[], year: number): DocNode[] {
  const result: DocNode[] = []
  for (const node of nodes) {
    if (node.nodeType === 'FILE') {
      if (nodeYear(node) === year) {
        result.push({ ...node, children: [] })
      }
      continue
    }
    const children = filterTreeByYear(node.children || [], year)
    if (nodeYear(node) === year || children.length) {
      result.push({ ...node, children })
    }
  }
  return result
}

function nodeYear(node: DocNode) {
  return node.docYear || 2026
}

function submitSearch() {
  activeKeyword.value = searchKeyword.value.trim()
}

function resetSearch() {
  selectedYear.value = currentYear
  searchKeyword.value = ''
  activeKeyword.value = ''
}

function isRepairFolder(node: DocNode): boolean {
  if (node.nodeName === '房建大修') {
    return true
  }
  let parentId = node.parentId
  while (parentId) {
    const parent = findNodeById(treeData.value, parentId)
    if (!parent) return false
    if (parent.nodeName === '房建大修') return true
    parentId = parent.parentId
  }
  return false
}

function findNodeById(nodes: DocNode[], id: number): DocNode | undefined {
  for (const node of nodes) {
    if (node.id === id) {
      return node
    }
    const found = findNodeById(node.children || [], id)
    if (found) {
      return found
    }
  }
  return undefined
}

function openItemDetail(node: DocNode) {
  if (!node.itemId) {
    return
  }
  saveTreeState()
  router.push(`/org/${deptId.value}/items/${node.itemId}`)
}

function onIssuedFileChange(_file: UploadFile, files: UploadFiles) {
  syncIssuedFiles(files)
}

function onIssuedFileRemove(_file: UploadFile, files: UploadFiles) {
  syncIssuedFiles(files)
}

function syncIssuedFiles(files: UploadFiles) {
  issuedFiles.value = files.map((file) => file.raw).filter((file): file is UploadRawFile => Boolean(file))
}

function saveTreeState() {
  const expandedKeys = collectExpandedNodeIds()
  const state: SavedTreeState = {
    expandedKeys,
    scrollTop: window.scrollY || document.documentElement.scrollTop || 0
  }
  sessionStorage.setItem(treeStateKey.value, JSON.stringify(state))
}

function readSavedTreeState(): SavedTreeState | null {
  const raw = sessionStorage.getItem(treeStateKey.value)
  if (!raw) return null
  try {
    return JSON.parse(raw) as SavedTreeState
  } catch {
    return null
  }
}

function collectExpandedNodeIds() {
  const nodesMap = treeRef.value?.store?.nodesMap || {}
  return Object.values(nodesMap)
    .filter((node: any) => node?.expanded && node?.data?.id)
    .map((node: any) => Number(node.data.id))
}

function restoreTreeState() {
  const saved = readSavedTreeState()
  if (!saved) {
    return
  }
  saved.expandedKeys.forEach((id) => treeRef.value?.getNode?.(id)?.expand?.())
  nextTick(() => window.scrollTo({ top: saved.scrollTop || 0 }))
}

function guessFileType(name: string): FileType {
  const lower = name.toLowerCase()
  if (/\.(doc|docx)$/.test(lower) || /文件|通知|报告|合同|说明/.test(name)) return 'WORD'
  if (/\.(xls|xlsx|csv)$/.test(lower) || /表|台账|统计|明细|记录|清单/.test(name)) return 'EXCEL'
  if (/\.(ppt|pptx)$/.test(lower)) return 'PPT'
  if (/\.pdf$/.test(lower)) return 'PDF'
  if (/\.zip$/.test(lower)) return 'ZIP'
  if (/\.(png|jpg|jpeg)$/.test(lower) || /照片|影像|图纸|平面示意图/.test(name)) return 'IMAGE'
  return 'OTHER'
}

function resolveFileType(node: DocNode): FileType {
  return node.fileType || guessFileType(node.nodeName)
}

function fileTypeClass(node: DocNode) {
  const classes: Record<FileType, string> = {
    WORD: 'word',
    EXCEL: 'excel',
    PPT: 'ppt',
    PDF: 'pdf',
    IMAGE: 'image',
    ZIP: 'zip',
    OTHER: 'other'
  }
  return `is-${classes[resolveFileType(node)]}`
}

function fileTypeLabel(node: DocNode) {
  const labels: Record<FileType, string> = {
    WORD: 'W',
    EXCEL: 'X',
    PPT: 'T',
    PDF: 'P',
    IMAGE: 'I',
    ZIP: 'Z',
    OTHER: 'F'
  }
  return labels[resolveFileType(node)]
}

function fileTypeText(node: DocNode) {
  const labels: Record<FileType, string> = {
    WORD: 'Word',
    EXCEL: 'Excel',
    PPT: 'PPT',
    PDF: 'PDF',
    IMAGE: '图片',
    ZIP: 'ZIP',
    OTHER: '其他'
  }
  return labels[resolveFileType(node)]
}

function handleDialogClosed() {
  issuedFiles.value = []
  issuedUploadRef.value?.clearFiles()
}

onMounted(() => load())
watch(() => [route.params.deptId, route.query.restore], () => load(route.query.restore === '1'))
watch(selectedYear, () => {
  activeKeyword.value = ''
})
</script>

<style scoped>
.section-workspace {
  height: 100%;
}

.compact-title {
  margin-bottom: 12px;
}

.business-tabs {
  margin-bottom: 10px;
}

.tree-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}

.doc-tree-panel {
  min-height: calc(100vh - 198px);
  padding: 12px 14px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 6px;
}

.doc-search-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.year-select {
  width: 120px;
}

.keyword-input {
  margin-left: 18px;
  width: 320px;
  max-width: 320px;
}

.doc-file-table {
  width: 100%;
}

.file-link {
  min-width: 0;
  border: 0;
  padding: 0;
  background: transparent;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--rail-blue-dark);
  font: inherit;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.doc-tree-panel :deep(.el-tree-node__content) {
  height: auto;
  min-height: 42px;
  align-items: stretch;
}

.doc-tree-panel :deep(.el-tree-node__expand-icon) {
  position: relative;
  flex: 0 0 22px;
  width: 22px;
  height: 42px;
  margin-right: 6px;
  color: var(--rail-blue-dark);
  transform: none !important;
}

.doc-tree-panel :deep(.el-tree-node__expand-icon svg) {
  display: none;
}

.doc-tree-panel :deep(.el-tree-node__expand-icon::before) {
  content: "";
  position: absolute;
  top: 50%;
  left: 0;
  width: 22px;
  height: 22px;
  transform: translateY(-50%);
  background: center / 16px 16px no-repeat url("data:image/svg+xml,%3Csvg viewBox='0 0 1024 1024' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M456 192h112v264h264v112H568v264H456V568H192V456h264V192Z' fill='%23235a96'/%3E%3C/svg%3E");
}

.doc-tree-panel :deep(.el-tree-node__expand-icon.expanded::before) {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 1024 1024' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M192 456h640v112H192V456Z' fill='%23235a96'/%3E%3C/svg%3E");
}

.doc-tree-panel :deep(.el-tree-node__expand-icon.is-leaf::before) {
  display: none;
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

.doc-tree-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
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

.doc-tree-row.is-folder .doc-tree-title {
  cursor: pointer;
  color: #17233c;
  font-size: 15px;
  font-weight: 700;
}

.doc-tree-icon {
  flex: 0 0 auto;
  width: 24px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #d6952a;
}

.doc-tree-row.is-folder .doc-tree-icon {
  font-size: 18px;
}

.folder-progress {
  width: min(360px, 52vw);
  display: grid;
  grid-template-columns: minmax(120px, 1fr) auto;
  align-items: center;
  gap: 10px;
  color: #637083;
  font-size: 12px;
}

.doc-file-icon {
  flex: 0 0 auto;
  width: 24px;
  height: 28px;
  border-radius: 4px 4px 5px 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-family: Arial, Helvetica, sans-serif;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  box-shadow: inset 0 -3px 0 rgba(0, 0, 0, 0.12);
}

.doc-file-icon.is-upload {
  background: #168a4a;
}

.doc-file-icon.is-word {
  background: #2f74d0;
}

.doc-file-icon.is-excel {
  background: #238b45;
}

.doc-file-icon.is-ppt {
  background: #c65a2e;
}

.doc-file-icon.is-pdf {
  background: #d14343;
}

.doc-file-icon.is-image {
  background: #7c5cc4;
}

.doc-file-icon.is-zip {
  background: #6b7280;
}

.doc-file-icon.is-other {
  background: #64748b;
}

.doc-tree-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 2px;
}

.requirement-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.requirement-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(220px, 1.3fr) 140px auto;
  gap: 10px;
  align-items: center;
}

.requirement-row-heading {
  color: #637083;
  font-size: 13px;
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

.issued-upload,
.issued-upload :deep(.el-upload),
.issued-upload :deep(.el-upload-dragger) {
  width: 100%;
}

@media (max-width: 900px) {
  .doc-tree-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .folder-progress {
    width: 100%;
  }

  .doc-tree-actions {
    flex-wrap: wrap;
  }

  .requirement-row {
    grid-template-columns: 1fr;
  }
}
</style>
