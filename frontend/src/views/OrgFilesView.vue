<template>
  <div class="section-workspace">
    <div class="page-title compact-title">
      <div>
        <h2>{{ currentSection?.deptName || '科室资料' }}</h2>
        <p class="page-subtitle">{{ moduleTitle }}</p>
      </div>
    </div>

    <section class="doc-tree-panel">
      <div v-if="canManageSection" class="tree-toolbar">
        <el-button plain @click="openRecycleBin">回收站</el-button>
        <el-button type="primary" plain @click="openFolderDialog()">新增文件夹</el-button>
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
                <span class="doc-node-name">{{ node.label }}</span>
              </div>
              <div v-if="shouldShowCompletionProgress(data)" class="folder-progress">
                <el-progress :percentage="data.completionPercent || 0" :show-text="false" />
                <span>{{ data.directFileCount || 0 }}/{{ data.progressTarget }}</span>
              </div>
            </div>
            <div v-if="canManageSection || canCreateFileInFolder(data) || canManageNode(data)" class="doc-tree-actions">
              <el-button v-if="canManageSection && data.nodeType === 'FOLDER' && data.level < 5" link type="primary" @click.stop="openFolderDialog(data)">新增文件夹</el-button>
              <el-button v-if="isInternalModule && canManageSection && data.nodeType === 'FOLDER' && isRepairChildFolder(data)" link type="primary" @click.stop="openImportDialog(data)">从模板库导入</el-button>
              <el-button v-if="data.nodeType === 'FOLDER' && (canManageSection || canCreateFileInFolder(data))" link type="primary" @click.stop="openFileDialog(data)">新增文件</el-button>
              <el-button v-if="canManageNode(data)" link type="primary" @click.stop="openEditDialog(data)">编辑</el-button>
              <el-button v-if="canManageNode(data)" link type="danger" @click.stop="deleteNode(data)">删除</el-button>
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
        <el-form-item :label="nodeForm.nodeType === 'FOLDER' ? '文件夹名称' : '文件名称'">
          <el-input v-model="nodeForm.nodeName" maxlength="128" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="nodeForm.sortOrder" :min="0" /></el-form-item>
        <template v-if="nodeForm.nodeType === 'FOLDER' && isInternalModule">
          <el-form-item label="完成进度">
            <el-switch v-model="nodeForm.showUploadProgress" active-text="使用完成进度条" />
          </el-form-item>
          <el-form-item v-if="nodeForm.showUploadProgress" label="完成目标数">
            <el-input-number v-model="nodeForm.progressTarget" :min="1" :step="1" />
          </el-form-item>
        </template>
        <template v-if="nodeForm.nodeType === 'FILE'">
          <el-form-item label="文件">
            <div v-if="showExistingBodyAttachment" class="body-attachment-card">
              <div class="body-attachment-info">
                <span class="body-attachment-name">{{ editingBodyAttachment?.originalFileName || '文件' }}</span>
                <span v-if="editingBodyAttachmentTime" class="body-attachment-meta">上传时间：{{ editingBodyAttachmentTime }}</span>
              </div>
              <div class="body-attachment-actions">
                <el-button link type="primary" @click="downloadBodyAttachment(editingBodyAttachment)">下载</el-button>
                <el-button link type="danger" @click="markBodyAttachmentDeletePending">删除</el-button>
              </div>
            </div>
            <el-alert
              v-if="bodyAttachmentDeletePending"
              title="已选择删除，保存后生效。"
              type="warning"
              show-icon
              :closable="false"
              class="body-attachment-alert"
            />
            <div v-if="selectedBodyAttachmentFile" class="body-attachment-card">
              <div class="body-attachment-info">
                <span class="body-attachment-name">待上传：{{ selectedBodyAttachmentFile.name }}</span>
              </div>
              <div class="body-attachment-actions">
                <el-button link type="danger" @click="removePendingBodyAttachmentFile">移除</el-button>
              </div>
            </div>
            <el-upload
              v-if="showBodyAttachmentDropzone"
              ref="issuedUploadRef"
              class="issued-upload"
              drag
              :auto-upload="false"
              :limit="1"
              :on-change="onIssuedFileChange"
              :on-remove="onIssuedFileRemove"
              :on-exceed="onIssuedFileExceed"
            >
              <div>拖拽文件到此处，或点击选择文件</div>
            </el-upload>
          </el-form-item>
          <el-form-item label="文件类型">
            <el-tag>{{ fileTypeText({ nodeName: nodeForm.nodeName, nodeType: 'FILE', fileType: nodeForm.fileType || 'OTHER' } as DocNode) }}</el-tag>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitNode">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogOpen" title="从模板库导入" width="560px">
      <el-form label-position="top">
        <el-form-item label="文件年份">
          <el-select v-model="importForm.docYear" style="width: 220px">
            <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板库文件">
          <el-checkbox-group v-model="importForm.templateItemIds" class="template-file-list">
            <el-checkbox v-for="item in repairTemplateItems" :key="item.id" :label="item.id">
              {{ item.itemName }}（{{ fileTypeText({ nodeName: item.originalFileName || item.itemName, nodeType: 'FILE', fileType: item.fileType || 'OTHER' } as DocNode) }}）
            </el-checkbox>
          </el-checkbox-group>
          <el-empty v-if="!repairTemplateItems.length" description="暂无模板库文件" />
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
import { formatDateTime } from '../utils/dateTime'

interface SectionItem {
  id: number
  deptName: string
}

interface DeptItem {
  id: number
  deptName: string
  deptType?: string
}

interface RepairTemplateItem {
  id: number
  itemName: string
  fileType?: FileType
  originalFileName?: string
}

interface DocUploadRequirement {
  id?: number
  requirementName: string
  description?: string
  sortOrder: number
}

interface DocItemAttachment {
  id: number
  originalFileName: string
  createdAt?: string
  uploadTime?: string
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
  moduleType?: 'INTERNAL' | 'RULES'
  workshopDeptId?: number
  showUploadProgress?: number
  progressTarget?: number
  directFileCount?: number
  completionPercent?: number
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
  issuedAttachments?: DocItemAttachment[]
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
const moduleType = computed<'INTERNAL' | 'RULES'>(() => route.path.startsWith('/rules') ? 'RULES' : 'INTERNAL')
const moduleBase = computed(() => moduleType.value === 'RULES' ? 'rules' : 'internal')
const isInternalModule = computed(() => moduleType.value === 'INTERNAL')
const moduleTitle = computed(() => moduleType.value === 'RULES' ? '规章制度' : '内业资料')
const sections = ref<SectionItem[]>([])
const depts = ref<DeptItem[]>([])
const treeData = ref<DocNode[]>([])
const treeRef = ref()
const issuedUploadRef = ref<any>()
const nodeDialogOpen = ref(false)
const importDialogOpen = ref(false)
const dialogMode = ref<DialogMode>('create')
const editingNode = ref<DocNode>()
const editingFileType = ref<FileType | ''>('')
const editingBodyAttachment = ref<DocItemAttachment>()
const bodyAttachmentDeletePending = ref(false)
const importingParent = ref<DocNode>()
const repairTemplateItems = ref<RepairTemplateItem[]>([])
const currentYear = new Date().getFullYear()
const selectedYear = ref(currentYear)
const searchKeyword = ref('')
const activeKeyword = ref('')
const issuedFiles = ref<UploadRawFile[]>([])
const yearOptions = Array.from({ length: 21 }, (_, index) => 2016 + index)
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
  showUploadProgress: false,
  progressTarget: undefined as number | undefined,
  requirements: [{ requirementName: '文件', description: '', sortOrder: 0 }] as DocUploadRequirement[]
})
const importForm = reactive({
  docYear: currentYear,
  templateItemIds: [] as number[]
})

const currentSection = computed(() => sections.value.find((item) => item.id === deptId.value))
const canManageSection = computed(() => Boolean(auth.user?.isSuperAdmin) || auth.user?.deptId === deptId.value)
const currentUserDept = computed(() => depts.value.find((dept) => dept.id === auth.user?.deptId))
const isWorkshopUser = computed(() => {
  const dept = currentUserDept.value
  return Boolean(dept?.deptType === 'WORKSHOP' || dept?.deptName?.includes('车间'))
})
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
const selectedBodyAttachmentFile = computed(() => issuedFiles.value[0])
const showExistingBodyAttachment = computed(() =>
  nodeForm.nodeType === 'FILE'
  && dialogMode.value === 'edit'
  && Boolean(editingBodyAttachment.value)
  && !bodyAttachmentDeletePending.value
  && !selectedBodyAttachmentFile.value
)
const showBodyAttachmentDropzone = computed(() =>
  nodeForm.nodeType === 'FILE'
  && !showExistingBodyAttachment.value
  && !selectedBodyAttachmentFile.value
)
const editingBodyAttachmentTime = computed(() => formatBodyAttachmentTime(editingBodyAttachment.value))
const emptyDescription = computed(() => {
  if (searchMode.value) return '暂无匹配文件'
  if (!yearTreeData.value.length) return '暂无文件'
  return '暂无文件'
})
const dialogTitle = computed(() => {
  if (dialogMode.value === 'edit') {
    if (nodeForm.nodeType === 'FOLDER') return '编辑文件夹'
    return '编辑文件'
  }
  if (nodeForm.nodeType === 'FOLDER') return '新增文件夹'
  return '新增文件'
})
const treeStateKey = computed(() => `${moduleBase.value}-tree-state:${deptId.value}`)

async function load(restore = route.query.restore === '1') {
  sections.value = await apiGet<SectionItem[]>('/sections/navigation', { moduleType: moduleType.value })
  depts.value = await apiGet<DeptItem[]>('/depts/tree')
  await loadTree(restore)
}

async function loadTree(restore = false) {
  treeData.value = await apiGet<DocNode[]>('/doc-tree', { sectionDeptId: deptId.value, moduleType: moduleType.value })
  await nextTick()
  if (restore) {
    restoreTreeState()
  }
}

function openRecycleBin() {
  router.push(`/${moduleBase.value}/${deptId.value}/recycle-bin`)
}

function resetForm(type: 'FOLDER' | 'FILE', parent?: DocNode) {
  editingNode.value = undefined
  editingFileType.value = ''
  editingBodyAttachment.value = undefined
  bodyAttachmentDeletePending.value = false
  dialogMode.value = 'create'
  nodeForm.nodeType = type
  nodeForm.parentId = parent?.id
  nodeForm.nodeName = ''
  nodeForm.sortOrder = 0
  nodeForm.docYear = parent?.docYear || selectedYear.value || currentYear
  nodeForm.fileType = type === 'FILE' ? 'OTHER' : ''
  nodeForm.businessType = 'ISSUED'
  nodeForm.submitterMode = 'SINGLE'
  nodeForm.workshopUploadEnabled = false
  nodeForm.uploadDeadline = ''
  nodeForm.visibleWorkshopIds = []
  nodeForm.showUploadProgress = false
  nodeForm.progressTarget = undefined
  nodeForm.requirements = [{ requirementName: '文件', description: '', sortOrder: 0 }]
  issuedFiles.value = []
}

function openFolderDialog(parent?: DocNode) {
  resetForm('FOLDER', parent)
  nodeDialogOpen.value = true
}

function openFileDialog(parent?: DocNode) {
  if (!parent?.id) {
    ElMessage.warning('只能在文件夹下新建文件')
    return
  }
  resetForm('FILE', parent)
  nodeDialogOpen.value = true
}

function canCreateFileInFolder(node: DocNode) {
  return Boolean(isInternalModule.value && node.nodeType === 'FOLDER' && isWorkshopUser.value)
}

function canManageNode(node: DocNode) {
  return canManageSection.value || Boolean(isInternalModule.value && node.nodeType === 'FILE' && node.workshopDeptId === auth.user?.deptId)
}

async function openImportDialog(parent: DocNode) {
  importingParent.value = parent
  importForm.docYear = parent.docYear || selectedYear.value || currentYear
  importForm.templateItemIds = []
  repairTemplateItems.value = await apiGet<RepairTemplateItem[]>('/repair-project-templates/items')
  importForm.templateItemIds = repairTemplateItems.value.map((item) => item.id)
  importDialogOpen.value = true
}

async function submitImportTemplate() {
  if (!importingParent.value) return
  if (!importForm.templateItemIds.length) {
    ElMessage.warning('请选择模板文件')
    return
  }
  await apiPost<DocNode[]>(`/repair-project-templates/import/${importingParent.value.id}`, {
    templateItemIds: importForm.templateItemIds,
    docYear: importForm.docYear
  })
  importDialogOpen.value = false
  ElMessage.success('导入成功')
  await loadTree(false)
}

async function openEditDialog(node: DocNode) {
  editingNode.value = node
  dialogMode.value = 'edit'
  editingBodyAttachment.value = undefined
  bodyAttachmentDeletePending.value = false
  nodeForm.nodeType = node.nodeType
  nodeForm.parentId = node.parentId
  nodeForm.nodeName = node.nodeName
  nodeForm.sortOrder = node.sortOrder || 0
  nodeForm.fileType = node.nodeType === 'FILE' ? node.fileType || guessFileType(node.nodeName) : ''
  editingFileType.value = nodeForm.fileType
  nodeForm.docYear = node.docYear || selectedYear.value || currentYear
  nodeForm.businessType = node.nodeType === 'FILE' ? node.businessType || 'ISSUED' : 'ISSUED'
  nodeForm.submitterMode = node.submitterMode || 'SINGLE'
  nodeForm.workshopUploadEnabled = Boolean(node.workshopUploadEnabled)
  nodeForm.uploadDeadline = node.uploadDeadline || ''
  nodeForm.visibleWorkshopIds = node.visibleWorkshopIds || []
  nodeForm.showUploadProgress = node.showUploadProgress !== 0
  nodeForm.progressTarget = node.progressTarget
  nodeForm.requirements = [{ requirementName: '文件', description: '', sortOrder: 0 }]
  issuedFiles.value = []
  if (node.nodeType === 'FILE' && node.itemId) {
    const item = await apiGet<DocItem>(`/doc-items/${node.itemId}`)
    nodeForm.fileType = item.fileType || node.fileType || guessFileType(node.nodeName)
    editingFileType.value = nodeForm.fileType
    nodeForm.docYear = item.docYear || node.docYear || currentYear
    nodeForm.businessType = item.businessType || node.businessType || 'ISSUED'
    nodeForm.submitterMode = item.submitterMode || 'SINGLE'
    nodeForm.workshopUploadEnabled = Boolean(item.workshopUploadEnabled)
    nodeForm.uploadDeadline = item.uploadDeadline || ''
    nodeForm.visibleWorkshopIds = item.visibleWorkshopIds || []
    editingBodyAttachment.value = item.issuedAttachments?.[0]
    nodeForm.requirements = item.requirements?.length
      ? item.requirements.map((requirement, index) => ({
          id: requirement.id,
          requirementName: requirement.requirementName,
          description: requirement.description || '',
          sortOrder: requirement.sortOrder ?? index
        }))
      : [{ requirementName: '文件', description: '', sortOrder: 0 }]
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
  if (nodeForm.nodeType === 'FILE' && dialogMode.value === 'create' && issuedFiles.value.length !== 1) {
    ElMessage.warning('请上传一个文件')
    return
  }
  if (nodeForm.nodeType === 'FILE' && !nodeForm.docYear) {
    ElMessage.warning('请选择文件年份')
    return
  }
  if (nodeForm.nodeType === 'FOLDER' && isInternalModule.value
    && nodeForm.showUploadProgress && (!nodeForm.progressTarget || nodeForm.progressTarget < 1)) {
    ElMessage.warning('请输入大于 0 的完成目标数')
    return
  }
  const body = {
    sectionDeptId: deptId.value,
    parentId: nodeForm.parentId,
    nodeName: nodeForm.nodeName.trim(),
    sortOrder: nodeForm.sortOrder,
    moduleType: moduleType.value,
    docYear: nodeForm.docYear,
    contentHtml: '',
    fileType: nodeForm.fileType,
    businessType: 'ISSUED',
    submitterMode: 'SINGLE',
    showUploadProgress: nodeForm.nodeType === 'FOLDER' && isInternalModule.value && nodeForm.showUploadProgress,
    progressTarget: nodeForm.nodeType === 'FOLDER' && isInternalModule.value && nodeForm.showUploadProgress
      ? nodeForm.progressTarget
      : null,
    uploadDeadline: null,
    workshopUploadEnabled: false,
    visibleWorkshopIds: [],
    requirements: []
  }
  const treeStateBeforeSave = captureTreeState([nodeForm.parentId])
  let changedNode: DocNode | undefined
  if (dialogMode.value === 'edit' && editingNode.value) {
    const pendingBodyFile = selectedBodyAttachmentFile.value
    const shouldDeleteBodyAttachment = nodeForm.nodeType === 'FILE'
      && bodyAttachmentDeletePending.value
      && Boolean(editingBodyAttachment.value)
    if (nodeForm.nodeType === 'FILE' && pendingBodyFile && shouldDeleteBodyAttachment) {
      await deleteEditingBodyAttachment()
      const response = await http.put(`/doc-nodes/${editingNode.value.id}`, buildFileForm(pendingBodyFile))
      changedNode = response.data.data
    } else if (nodeForm.nodeType === 'FILE' && pendingBodyFile) {
      const response = await http.put(`/doc-nodes/${editingNode.value.id}`, buildFileForm(pendingBodyFile))
      changedNode = response.data.data
    } else {
      changedNode = await apiPut<DocNode>(`/doc-nodes/${editingNode.value.id}`, body)
      if (shouldDeleteBodyAttachment) {
        await deleteEditingBodyAttachment()
      }
    }
  } else if (nodeForm.nodeType === 'FOLDER') {
    changedNode = await apiPost<DocNode>('/doc-nodes/folders', body)
  } else {
    if (!nodeForm.parentId) {
      ElMessage.warning('只能在文件夹下新建文件')
      return
    }
    const response = await http.post('/doc-nodes/files', buildFileForm(issuedFiles.value[0]))
    changedNode = response.data.data
  }
  nodeDialogOpen.value = false
  await afterNodeChanged(changedNode, treeStateBeforeSave)
  ElMessage.success(dialogMode.value === 'edit' ? '修改成功' : '新增成功')
}

function buildFileForm(file: UploadRawFile) {
  const form = new FormData()
  form.append('sectionDeptId', String(deptId.value))
  if (nodeForm.parentId) {
    form.append('parentId', String(nodeForm.parentId))
  }
  form.append('nodeName', nodeForm.nodeName.trim())
  form.append('sortOrder', String(nodeForm.sortOrder || 0))
  form.append('docYear', String(nodeForm.docYear))
  form.append('moduleType', moduleType.value)
  form.append('fileType', nodeForm.fileType || 'OTHER')
  form.append('workshopUploadEnabled', 'false')
  form.append('submitterMode', 'SINGLE')
  form.append('file', file)
  return form
}

async function afterNodeChanged(node?: DocNode, previousState?: SavedTreeState) {
  await loadTree(false)
  if (node?.docYear) {
    selectedYear.value = node.docYear
  }
  await expandChangedNode(node, previousState)
}

async function expandChangedNode(node?: DocNode, previousState?: SavedTreeState) {
  await nextTick()
  if (!node && !previousState) {
    return
  }
  const expandedKeys = [...(previousState?.expandedKeys || [])]
  if (node?.parentId) {
    expandedKeys.push(node.parentId)
  }
  if (node?.nodeType === 'FOLDER') {
    expandedKeys.push(node.id)
  }
  Array.from(new Set(expandedKeys)).forEach((id) => treeRef.value?.getNode?.(id)?.expand?.())
  if (previousState) {
    await nextTick()
    window.scrollTo({ top: previousState.scrollTop || 0 })
  }
}

async function deleteNode(node: DocNode) {
  const message = node.nodeType === 'FILE'
    ? `确定删除“${node.nodeName}”吗？删除后可在回收站恢复，超过 30 天将自动清理。`
    : `确定删除“${node.nodeName}”吗？`
  await ElMessageBox.confirm(message, '删除资料节点', { type: 'warning' })
  await apiDelete(`/doc-nodes/${node.id}`)
  ElMessage.success(node.nodeType === 'FILE' ? '已移入回收站' : '删除成功')
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

function shouldShowCompletionProgress(node: DocNode) {
  return isInternalModule.value
    && node.nodeType === 'FOLDER'
    && node.showUploadProgress !== 0
    && Boolean(node.progressTarget)
}

function submitSearch() {
  activeKeyword.value = searchKeyword.value.trim()
}

function resetSearch() {
  selectedYear.value = currentYear
  searchKeyword.value = ''
  activeKeyword.value = ''
}

function isRepairChildFolder(node: DocNode): boolean {
  if (node.nodeName === '房建大修') {
    return false
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
  router.push(`/${moduleBase.value}/${deptId.value}/items/${node.itemId}`)
}

function onIssuedFileChange(_file: UploadFile, files: UploadFiles) {
  syncIssuedFiles(files)
}

function onIssuedFileRemove(_file: UploadFile, files: UploadFiles) {
  syncIssuedFiles(files)
}

function syncIssuedFiles(files: UploadFiles) {
  issuedFiles.value = files.map((file) => file.raw).filter((file): file is UploadRawFile => Boolean(file))
  const mainFile = issuedFiles.value[0]
  if (!mainFile) {
    nodeForm.fileType = dialogMode.value === 'edit' ? editingFileType.value || 'OTHER' : 'OTHER'
    return
  }
  nodeForm.fileType = guessFileType(mainFile.name)
  if (!nodeForm.nodeName.trim()) {
    nodeForm.nodeName = mainFile.name.replace(/\.[^.]+$/, '')
  }
}

function onIssuedFileExceed() {
  ElMessage.warning('只能上传一个文件')
}

function markBodyAttachmentDeletePending() {
  if (!editingBodyAttachment.value) {
    return
  }
  bodyAttachmentDeletePending.value = true
  issuedFiles.value = []
  issuedUploadRef.value?.clearFiles()
}

function removePendingBodyAttachmentFile() {
  issuedFiles.value = []
  issuedUploadRef.value?.clearFiles()
  nodeForm.fileType = dialogMode.value === 'edit' ? editingFileType.value || 'OTHER' : 'OTHER'
}

async function downloadBodyAttachment(attachment?: DocItemAttachment) {
  if (!attachment) {
    return
  }
  const response = await http.get(`/doc-item-attachments/${attachment.id}/download`, { responseType: 'blob' })
  downloadBlob(response.data, attachment.originalFileName || '文件')
}

async function deleteEditingBodyAttachment() {
  if (!editingBodyAttachment.value) {
    return
  }
  await apiDelete(`/doc-item-attachments/${editingBodyAttachment.value.id}`)
  editingBodyAttachment.value = undefined
  bodyAttachmentDeletePending.value = false
}

function formatBodyAttachmentTime(attachment?: DocItemAttachment) {
  return formatDateTime(attachment?.createdAt || attachment?.uploadTime)
}

function downloadBlob(data: BlobPart, filename: string) {
  const blob = new Blob([data])
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function saveTreeState() {
  const state = captureTreeState()
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

function captureTreeState(extraExpandedKeys: Array<number | undefined> = []): SavedTreeState {
  const expandedKeys = [
    ...collectExpandedNodeIds(),
    ...extraExpandedKeys.filter((id): id is number => Boolean(id))
  ]
  return {
    expandedKeys: Array.from(new Set(expandedKeys)),
    scrollTop: window.scrollY || document.documentElement.scrollTop || 0
  }
}

function restoreTreeState() {
  const saved = readSavedTreeState()
  if (!saved) {
    return
  }
  expandChangedNode(undefined, saved)
}

function guessFileType(name: string): FileType {
  const lower = name.toLowerCase()
  if (/\.(doc|docx)$/.test(lower) || /文件|通知|报告|合同|说明/.test(name)) return 'WORD'
  if (/\.(xls|xlsx)$/.test(lower) || /表|台账|统计|明细|记录|清单/.test(name)) return 'EXCEL'
  if (/\.(ppt|pptx)$/.test(lower)) return 'PPT'
  if (/\.pdf$/.test(lower)) return 'PDF'
  if (/\.zip$/.test(lower)) return 'ZIP'
  if (/\.(png|jpg|jpeg|gif|bmp|webp|svg)$/.test(lower) || /照片|影像|图纸|平面示意图/.test(name)) return 'IMAGE'
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
  if (resolveFileType(node) === 'IMAGE') {
    return imageTypeText(node)
  }
  const labels: Record<FileType, string> = {
    WORD: 'W',
    EXCEL: 'X',
    PPT: 'T',
    PDF: 'P',
    IMAGE: 'IMG',
    ZIP: 'Z',
    OTHER: 'F'
  }
  return labels[resolveFileType(node)]
}

function fileTypeText(node: DocNode) {
  if (resolveFileType(node) === 'IMAGE') {
    return imageTypeText(node)
  }
  const labels: Record<FileType, string> = {
    WORD: 'Word',
    EXCEL: 'Excel',
    PPT: 'PPT',
    PDF: 'PDF',
    IMAGE: 'IMG',
    ZIP: 'ZIP',
    OTHER: '其他'
  }
  return labels[resolveFileType(node)]
}

function imageTypeText(node: DocNode) {
  const extension = fileExtension(node.nodeName)
  return ['JPG', 'JPEG', 'PNG', 'GIF', 'BMP', 'WEBP', 'SVG'].includes(extension) ? extension : 'IMG'
}

function fileExtension(name?: string) {
  const match = (name || '').match(/\.([^.]+)$/)
  return match?.[1]?.toUpperCase() || ''
}

function handleDialogClosed() {
  issuedFiles.value = []
  editingBodyAttachment.value = undefined
  bodyAttachmentDeletePending.value = false
  issuedUploadRef.value?.clearFiles()
}

onMounted(() => load())
watch(() => [route.path, route.params.deptId, route.query.restore], () => load(route.query.restore === '1'))
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

.page-subtitle {
  margin: 6px 0 0;
  color: #637083;
  font-size: 13px;
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

.template-file-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.workshop-upload-options {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
}

.body-attachment-alert {
  margin-bottom: 10px;
}

.body-attachment-card {
  width: 100%;
  margin-bottom: 10px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.body-attachment-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.body-attachment-name {
  overflow: hidden;
  color: #1f2d3d;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.body-attachment-meta {
  color: #637083;
  font-size: 13px;
}

.body-attachment-actions {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
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
  align-items: flex-start;
  gap: 8px;
}

.folder-progress {
  width: 260px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #637083;
  font-size: 13px;
  font-weight: 400;
}

.folder-progress :deep(.el-progress) {
  flex: 1;
}

.doc-tree-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: #1f2d3d;
  line-height: 1.5;
}

.doc-node-name {
  min-width: 0;
  overflow-wrap: anywhere;
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

  .doc-tree-actions {
    flex-wrap: wrap;
  }

  .requirement-row {
    grid-template-columns: 1fr;
  }
}
</style>
