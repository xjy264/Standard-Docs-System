<template>
  <div class="doc-detail-page">
    <div class="detail-header">
      <div>
        <el-button link type="primary" @click="router.push(`/org/${deptId}?restore=1`)">返回文件菜单</el-button>
        <h2>{{ item?.itemName || '文件详情' }}</h2>
        <p v-if="item" class="detail-meta">
          {{ [item.sectionDeptName, item.categoryName, item.docYear, '通知文件'].filter(Boolean).join(' / ') }}
        </p>
      </div>
      <div class="header-actions">
        <el-button v-if="canManageSection && workshopUploadEnabled" @click="openRecords">上传记录</el-button>
        <el-button v-if="canManageSection" type="primary" plain @click="issuedUploadOpen = true">上传正文附件</el-button>
      </div>
    </div>

    <section class="detail-content">
      <div class="section-heading">
        <h3>正文附件</h3>
      </div>
      <el-table v-if="item?.issuedAttachments?.length" :data="item.issuedAttachments" border>
        <el-table-column prop="originalFileName" label="附件" />
        <el-table-column prop="createdAt" label="上传时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="previewIssuedAttachment(row)">预览</el-button>
            <el-button link type="primary" @click="downloadIssuedAttachment(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无正文附件" />
    </section>

    <section v-if="workshopUploadEnabled" class="detail-content">
      <div class="upload-task-status">
        <el-tag v-if="mySubmission" type="success">已提交</el-tag>
        <el-tag v-else-if="deadlineExpired" type="danger">已截止</el-tag>
        <el-tag v-else type="info">待上传</el-tag>
        <span>每人限提交一次</span>
        <span v-if="item?.uploadDeadline">截止时间：{{ item.uploadDeadline }}</span>
      </div>
      <el-table :data="item?.requirements || []" border>
        <el-table-column prop="requirementName" label="收集内容" min-width="220" />
        <el-table-column prop="description" label="说明" min-width="220">
          <template #default="{ row }">
            <span>{{ row.description || '无' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上传附件" min-width="260">
          <template #default="{ row }">
            <div class="requirement-upload-cell">
              <span class="selected-file-name">
                {{ selectedRequirementFiles[row.id]?.name || (canUploadAttachment ? '未选择' : mySubmission ? '已提交' : '不可上传') }}
              </span>
              <el-upload
                v-if="canUploadAttachment"
                :auto-upload="false"
                :show-file-list="false"
                :disabled="submitting"
                :on-change="onRequirementFileChange(row)"
              >
                <el-button link type="primary" :disabled="submitting">选择附件</el-button>
              </el-upload>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="canUploadAttachment" class="upload-actions inline-upload-actions">
        <el-button type="primary" :loading="submitting" @click="submitAttachment">上传附件</el-button>
      </div>
    </section>

    <section v-if="workshopUploadEnabled && mySubmission" class="detail-content">
      <div class="section-heading">
        <h3>我的提交</h3>
      </div>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="上传时间">{{ mySubmission.submittedAt }}</el-descriptions-item>
        <el-descriptions-item label="所属组织">{{ mySubmission.submitterDeptName }}</el-descriptions-item>
        <el-descriptions-item label="上传人">{{ mySubmission.uploadUserName }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="mySubmission.attachments || []" style="margin-top: 14px" border>
        <el-table-column prop="requirementName" label="收集内容" width="180" />
        <el-table-column prop="originalFileName" label="附件" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }"><el-button link type="primary" @click="downloadAttachment(row)">下载</el-button></template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="issuedUploadOpen" title="上传正文附件" width="720px" @closed="resetIssuedUpload">
      <el-upload
        ref="issuedUploadRef"
        class="attachment-upload"
        drag
        multiple
        :auto-upload="false"
        :on-change="onIssuedFileChange"
        :on-remove="onIssuedFileRemove"
      >
        <div>拖拽附件到此处，或点击选择文件</div>
      </el-upload>
      <div class="upload-actions">
        <el-button type="primary" :loading="submittingIssued" @click="submitIssuedAttachments">上传附件</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="recordsOpen" title="上传记录" width="980px">
      <el-table :data="records" stripe>
        <el-table-column prop="submittedAt" label="上传时间" width="180" />
        <el-table-column prop="submitterDeptName" label="所属车间" width="150" />
        <el-table-column prop="uploadUserName" label="上传人" width="120" />
        <el-table-column label="附件下载" min-width="260">
          <template #default="{ row }">
            <div class="attachment-links">
              <el-button
                v-for="attachment in row.attachments || []"
                :key="attachment.id"
                link
                type="primary"
                @click="downloadAttachment(attachment)"
              >
                {{ attachment.requirementName || attachment.originalFileName }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="officePreviewOpen" :title="officePreviewTitle" width="92vw" top="4vh" @closed="destroyOfficePreview">
      <div id="onlyoffice-preview-host" class="office-preview-host"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, type UploadFile, type UploadFiles, type UploadRawFile } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet, http } from '../api/http'
import { useAuthStore } from '../stores/auth'

interface DocUploadRequirement {
  id: number
  requirementName: string
  description?: string
  sortOrder: number
}

interface DocItemAttachment {
  id: number
  originalFileName: string
  createdAt: string
  extension?: string
}

interface DocItem {
  id: number
  categoryId?: number
  itemName: string
  contentHtml?: string
  attachmentEnabled: number
  categoryName?: string
  sectionDeptId?: number
  sectionDeptName?: string
  docYear?: number
  businessType?: 'UPLOAD' | 'ISSUED'
  submitterMode?: 'SINGLE' | 'MULTIPLE'
  workshopUploadEnabled?: number
  uploadDeadline?: string
  submissionCount?: number
  requirements?: DocUploadRequirement[]
  issuedAttachments?: DocItemAttachment[]
}

interface DocAttachment {
  id: number
  requirementName?: string
  originalFileName: string
}

interface DocSubmission {
  id: number
  itemName: string
  categoryName?: string
  sectionDeptName: string
  submitterDeptName: string
  uploadUserName: string
  submittedAt: string
  attachmentCount: number
  attachments?: DocAttachment[]
}

interface AttachmentPreview {
  previewType: 'PDF' | 'ONLYOFFICE' | 'UNCONFIGURED' | 'UNSUPPORTED'
  fileType: string
  title: string
  url?: string
  documentServerUrl?: string
  message?: string
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const deptId = computed(() => Number(route.params.deptId))
const itemId = computed(() => Number(route.params.itemId))
const item = ref<DocItem>()
const recordsOpen = ref(false)
const issuedUploadOpen = ref(false)
const officePreviewOpen = ref(false)
const officePreviewTitle = ref('Office 预览')
const records = ref<DocSubmission[]>([])
const mySubmission = ref<DocSubmission | null>(null)
const selectedRequirementFiles = ref<Record<number, { raw: UploadRawFile; name: string }>>({})
const issuedAttachmentFiles = ref<UploadRawFile[]>([])
const issuedUploadRef = ref<any>()
const submitting = ref(false)
const submittingIssued = ref(false)

const workshopUploadEnabled = computed(() => Boolean(item.value?.workshopUploadEnabled || item.value?.attachmentEnabled))
const canManageSection = computed(() => Boolean(auth.user?.isSuperAdmin) || auth.user?.deptId === item.value?.sectionDeptId)
const deadlineExpired = computed(() => Boolean(item.value?.uploadDeadline && new Date(item.value.uploadDeadline).getTime() < Date.now()))
const canUploadAttachment = computed(() => {
  if (!workshopUploadEnabled.value || mySubmission.value || deadlineExpired.value) {
    return false
  }
  return true
})

async function load() {
  resetUpload()
  item.value = await apiGet<DocItem>(`/doc-items/${itemId.value}`)
  if (workshopUploadEnabled.value) {
    mySubmission.value = await apiGet<DocSubmission | null>(`/doc-items/${itemId.value}/my-submission`)
  } else {
    mySubmission.value = null
  }
}

function selectRequirementFile(row: DocUploadRequirement, file: UploadFile) {
  if (!row.id || !file.raw) {
    return
  }
  selectedRequirementFiles.value = {
    ...selectedRequirementFiles.value,
    [row.id]: { raw: file.raw, name: file.name }
  }
}

function onRequirementFileChange(row: DocUploadRequirement) {
  return (file: UploadFile) => selectRequirementFile(row, file)
}

async function submitAttachment() {
  const requirements = item.value?.requirements || []
  if (!requirements.length) {
    ElMessage.warning('该文件未配置上传收集项')
    return
  }
  const missing = requirements.find((requirement) => !selectedRequirementFiles.value[requirement.id])
  if (missing) {
    ElMessage.warning(`请选择“${missing.requirementName}”附件`)
    return
  }
  submitting.value = true
  try {
    const form = new FormData()
    requirements.forEach((requirement) => {
      form.append('requirementIds', String(requirement.id))
      form.append('files', selectedRequirementFiles.value[requirement.id].raw)
    })
    await http.post(`/doc-items/${itemId.value}/submissions`, form)
    ElMessage.success('上传成功')
    resetUpload()
    await load()
  } finally {
    submitting.value = false
  }
}

function resetUpload() {
  selectedRequirementFiles.value = {}
}

function onIssuedFileChange(_file: UploadFile, files: UploadFiles) {
  syncIssuedFiles(files)
}

function onIssuedFileRemove(_file: UploadFile, files: UploadFiles) {
  syncIssuedFiles(files)
}

function syncIssuedFiles(files: UploadFiles) {
  issuedAttachmentFiles.value = files.map((file) => file.raw).filter((file): file is UploadRawFile => Boolean(file))
}

async function submitIssuedAttachments() {
  if (!issuedAttachmentFiles.value.length) {
    ElMessage.warning('请选择附件')
    return
  }
  submittingIssued.value = true
  try {
    const form = new FormData()
    issuedAttachmentFiles.value.forEach((file) => form.append('files', file))
    await http.post(`/doc-items/${itemId.value}/body-attachments`, form)
    ElMessage.success('上传成功')
    issuedUploadOpen.value = false
    await load()
  } finally {
    submittingIssued.value = false
  }
}

function resetIssuedUpload() {
  issuedAttachmentFiles.value = []
  issuedUploadRef.value?.clearFiles()
}

async function openRecords() {
  records.value = await apiGet<DocSubmission[]>(`/doc-items/${itemId.value}/submissions`)
  recordsOpen.value = true
}

async function downloadAttachment(attachment: DocAttachment) {
  const response = await http.get(`/doc-attachments/${attachment.id}/download`, { responseType: 'blob' })
  downloadBlob(response.data, attachment.originalFileName || '附件')
}

async function downloadIssuedAttachment(attachment: DocItemAttachment) {
  const response = await http.get(`/doc-item-attachments/${attachment.id}/download`, { responseType: 'blob' })
  downloadBlob(response.data, attachment.originalFileName || '附件')
}

async function previewIssuedAttachment(attachment: DocItemAttachment) {
  const preview = await apiGet<AttachmentPreview>(`/doc-item-attachments/${attachment.id}/preview`)
  if (preview.previewType === 'PDF' && preview.url) {
    window.open(preview.url, '_blank')
    return
  }
  if (preview.previewType === 'ONLYOFFICE' && preview.documentServerUrl && preview.url) {
    await openOfficePreview(preview, attachment)
    return
  }
  ElMessage.warning(preview.message || '该格式暂不支持在线预览')
}

async function openOfficePreview(preview: AttachmentPreview, attachment: DocItemAttachment) {
  if (!preview.documentServerUrl || !preview.url) {
    ElMessage.warning('预览服务未配置')
    return
  }
  const documentServerUrl = preview.documentServerUrl
  const downloadUrl = preview.url
  officePreviewTitle.value = preview.title || attachment.originalFileName
  officePreviewOpen.value = true
  await loadOnlyOfficeScript(documentServerUrl)
  setTimeout(() => {
    destroyOfficePreview()
    const DocsAPI = (window as any).DocsAPI
    if (!DocsAPI?.DocEditor) {
      ElMessage.warning('预览服务未加载完成')
      return
    }
    const token = auth.token ? `access_token=${encodeURIComponent(auth.token)}` : ''
    const fileUrl = new URL(downloadUrl, window.location.origin)
    if (token) {
      fileUrl.search = fileUrl.search ? `${fileUrl.search}&${token}` : `?${token}`
    }
    ;(window as any).__docEditor = new DocsAPI.DocEditor('onlyoffice-preview-host', {
      document: {
        fileType: (attachment.extension || preview.fileType || '').toLowerCase(),
        key: `${attachment.id}-${Date.now()}`,
        title: preview.title || attachment.originalFileName,
        url: fileUrl.toString()
      },
      documentType: onlyOfficeDocumentType(attachment.extension || preview.fileType),
      editorConfig: { lang: 'zh-CN', mode: 'view' },
      height: '72vh',
      width: '100%'
    })
  }, 0)
}

function loadOnlyOfficeScript(documentServerUrl: string) {
  const scriptUrl = `${documentServerUrl.replace(/\/$/, '')}/web-apps/apps/api/documents/api.js`
  const existing = Array.from(document.scripts).find((script) => script.src === scriptUrl)
  if (existing) {
    return Promise.resolve()
  }
  return new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = scriptUrl
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('OnlyOffice 预览服务加载失败'))
    document.head.appendChild(script)
  })
}

function onlyOfficeDocumentType(extension?: string) {
  const normalized = (extension || '').toLowerCase()
  if (['xls', 'xlsx'].includes(normalized)) return 'cell'
  if (['ppt', 'pptx'].includes(normalized)) return 'slide'
  return 'word'
}

function destroyOfficePreview() {
  const editor = (window as any).__docEditor
  if (editor?.destroyEditor) {
    editor.destroyEditor()
  }
  ;(window as any).__docEditor = undefined
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

onMounted(load)
watch(() => route.params.itemId, load)
</script>

<style scoped>
.doc-detail-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.detail-header h2 {
  margin: 8px 0 0;
}

.detail-meta {
  margin: 8px 0 0;
  color: #637083;
}

.detail-content {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 18px;
}

.section-heading {
  margin-bottom: 14px;
}

.section-heading h3,
.issued-attachments h3 {
  margin: 0;
  font-size: 16px;
}

.upload-task-status {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  color: #637083;
}

.requirement-upload-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.selected-file-name {
  min-width: 0;
  color: #1f2d3d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rich-content {
  min-height: 280px;
  line-height: 1.8;
  color: #1f2d3d;
  word-break: break-word;
}

.rich-content :deep(p) {
  margin: 0 0 10px;
}

.rich-content :deep(img) {
  max-width: 100%;
  height: auto;
}

.rich-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}

.rich-content :deep(th),
.rich-content :deep(td) {
  border: 1px solid var(--line);
  padding: 8px 10px;
}

.rich-content :deep(ul),
.rich-content :deep(ol) {
  padding-left: 22px;
  margin: 8px 0 12px;
}

.rich-content :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 12px;
  border-left: 4px solid #d8e3f2;
  background: #f7f9fc;
  color: #4b5b70;
}

.issued-attachments {
  margin-top: 18px;
}

.issued-attachments h3 {
  margin-bottom: 12px;
}

.attachment-upload,
.attachment-upload :deep(.el-upload),
.attachment-upload :deep(.el-upload-dragger) {
  width: 100%;
}

.upload-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.inline-upload-actions {
  margin-top: 14px;
}

.attachment-links {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.office-preview-host {
  width: 100%;
  height: 72vh;
}

@media (max-width: 900px) {
  .detail-header {
    flex-direction: column;
  }

  .header-actions {
    flex-wrap: wrap;
  }
}
</style>
