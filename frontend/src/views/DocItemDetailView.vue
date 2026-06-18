<template>
  <div class="doc-detail-page">
    <div class="detail-header">
      <div>
        <el-button link type="primary" @click="router.push(`/org/${deptId}?restore=1`)">返回文件菜单</el-button>
        <h2>{{ item?.itemName || '文件详情' }}</h2>
        <p v-if="item" class="detail-meta">
          {{ [item.sectionDeptName, item.categoryName, item.docYear, '文件'].filter(Boolean).join(' / ') }}
        </p>
      </div>
      <div class="header-actions">
        <el-button v-if="canManageSection && !primaryAttachment" type="primary" @click="openIssuedUpload">上传文件</el-button>
        <el-button v-if="canManageSection && workshopUploadEnabled" @click="openRecords">车间提交记录</el-button>
      </div>
    </div>

    <section class="detail-content">
      <div class="section-heading preview-heading">
        <h3>文件预览</h3>
        <div v-if="primaryAttachment" class="preview-heading-actions">
          <el-button type="primary" plain @click="downloadIssuedAttachment(primaryAttachment)">下载文件</el-button>
        </div>
      </div>
      <div v-if="item?.issuedAttachments?.length" class="inline-preview-section">
        <div class="preview-paper">
          <iframe
            v-if="inlinePreviewKind === 'PDF' && inlinePreviewUrl"
            class="pdf-preview-frame"
            :src="inlinePreviewUrl"
            title="PDF 文件预览"
          ></iframe>
          <img
            v-else-if="inlinePreviewKind === 'IMAGE' && inlinePreviewUrl"
            class="image-preview"
            :src="inlinePreviewUrl"
            alt="image preview"
          />
          <div v-else-if="inlinePreviewKind === 'ONLYOFFICE'" id="onlyoffice-inline-preview-host" class="office-preview-host"></div>
          <div v-else class="preview-placeholder">
            <p>{{ inlinePreviewMessage }}</p>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无文件" />
    </section>

    <section v-if="workshopUploadEnabled" class="detail-content">
      <div class="section-heading">
        <h3>车间提交文件</h3>
      </div>
      <div class="upload-task-status">
        <el-tag v-if="submissionStatus" type="success">已提交</el-tag>
        <el-tag v-else-if="deadlineExpired" type="danger">已截止</el-tag>
        <el-tag v-else type="info">待提交</el-tag>
        <span>{{ isSingleMode ? '每车间限提交一次' : '可多次提交' }}</span>
        <span v-if="item?.uploadDeadline">截止时间：{{ formatDateTime(item.uploadDeadline) }}</span>
      </div>
      <el-table :data="item?.requirements || []" border>
        <el-table-column prop="requirementName" label="收集内容" min-width="220" />
        <el-table-column prop="description" label="说明" min-width="220">
          <template #default="{ row }">
            <span>{{ row.description || '无' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交文件" min-width="260">
          <template #default="{ row }">
            <div class="requirement-upload-cell">
              <span class="selected-file-name">
                {{ selectedRequirementFiles[row.id]?.name || (canUploadAttachment ? '未选择' : submissionStatus ? '已提交' : '不可提交') }}
              </span>
              <el-upload
                v-if="canUploadAttachment"
                :auto-upload="false"
                :show-file-list="false"
                :disabled="submitting"
                :on-change="onRequirementFileChange(row)"
              >
                <el-button link type="primary" :disabled="submitting">选择文件</el-button>
              </el-upload>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="canUploadAttachment" class="upload-actions inline-upload-actions">
        <el-button type="primary" :loading="submitting" @click="submitAttachment">提交文件</el-button>
      </div>
    </section>

    <section v-if="workshopUploadEnabled && displayedSubmissions.length" class="detail-content">
      <div class="section-heading">
        <h3>{{ displayedSubmissionTitle }}</h3>
      </div>
      <el-table :data="displayedSubmissions" border>
        <el-table-column label="上传时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column prop="submitterDeptName" label="所属组织" width="150" />
        <el-table-column prop="uploadUserName" label="上传人" width="120" />
        <el-table-column label="文件" min-width="260">
          <template #default="{ row }">
            <div v-if="row.attachments?.length" class="attachment-links">
              <el-button
                v-for="attachment in row.attachments"
                :key="attachment.id"
                link
                type="primary"
                @click="downloadAttachment(attachment)"
              >
                {{ attachment.requirementName || attachment.originalFileName }}
              </el-button>
            </div>
            <span v-else>已上传</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.deleteAllowed" link type="danger" @click="deleteSubmission(row)">删除</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="issuedUploadOpen" title="上传文件" width="720px" @closed="resetIssuedUpload">
      <div v-if="selectedIssuedAttachmentFile" class="selected-upload-card">
        <span>待上传：{{ selectedIssuedAttachmentFile.name }}</span>
        <el-button link type="danger" @click="removePendingIssuedFile">移除</el-button>
      </div>
      <el-upload
        v-else
        ref="issuedUploadRef"
        class="attachment-upload"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="onIssuedFileChange"
        :on-remove="onIssuedFileRemove"
        :on-exceed="onIssuedFileExceed"
      >
        <div>拖拽文件到此处，或点击选择文件</div>
      </el-upload>
      <div class="upload-actions">
        <el-button type="primary" :loading="submittingIssued" @click="submitIssuedAttachments">上传文件</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="recordsOpen" title="车间提交记录" width="980px">
      <el-table :data="records" stripe>
        <el-table-column label="上传时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column prop="submitterDeptName" label="所属车间" width="150" />
        <el-table-column prop="uploadUserName" label="上传人" width="120" />
        <el-table-column label="文件下载" min-width="260">
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
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.deleteAllowed" link type="danger" @click="deleteSubmission(row)">删除</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox, type UploadFile, type UploadFiles, type UploadRawFile } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiDelete, apiGet, http } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { apiDateTimeToTimestamp, formatDateTime } from '../utils/dateTime'

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
  submissionId?: number
  requirementName?: string
  originalFileName: string
}

interface DocSubmission {
  id: number
  itemName: string
  categoryName?: string
  sectionDeptName: string
  submitterDeptId?: number
  uploadUserId?: number
  submitterDeptName: string
  uploadUserName: string
  submittedAt: string
  attachmentCount: number
  ownSubmission?: boolean
  downloadAllowed?: boolean
  deleteAllowed?: boolean
  attachments?: DocAttachment[]
}

interface AttachmentPreview {
  previewType: 'PDF' | 'IMAGE' | 'ONLYOFFICE' | 'UNCONFIGURED' | 'UNSUPPORTED'
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
const records = ref<DocSubmission[]>([])
const mySubmission = ref<DocSubmission | null>(null)
const selectedRequirementFiles = ref<Record<number, { raw: UploadRawFile; name: string }>>({})
const issuedAttachmentFiles = ref<UploadRawFile[]>([])
const issuedUploadRef = ref<any>()
const submitting = ref(false)
const submittingIssued = ref(false)
const inlinePreviewKind = ref<'PDF' | 'IMAGE' | 'ONLYOFFICE' | 'MESSAGE'>('MESSAGE')
const inlinePreviewUrl = ref('')
const inlinePreviewMessage = ref('暂无可预览文件')

const workshopUploadEnabled = computed(() => Boolean(item.value?.workshopUploadEnabled || item.value?.attachmentEnabled))
const canManageSection = computed(() => Boolean(auth.user?.isSuperAdmin) || auth.user?.deptId === item.value?.sectionDeptId)
const deadlineExpired = computed(() => Boolean(item.value?.uploadDeadline && apiDateTimeToTimestamp(item.value.uploadDeadline) < Date.now()))
const primaryAttachment = computed(() => item.value?.issuedAttachments?.[0])
const submitterMode = computed(() => item.value?.submitterMode || 'SINGLE')
const isSingleMode = computed(() => submitterMode.value === 'SINGLE')
const selectedIssuedAttachmentFile = computed(() => issuedAttachmentFiles.value[0])
const mySubmissions = ref<DocSubmission[]>([])
const submissionStatus = computed(() => mySubmission.value)
const displayedSubmissions = computed(() => {
  if (!workshopUploadEnabled.value) {
    return []
  }
  if (isSingleMode.value) {
    return mySubmission.value ? [mySubmission.value] : []
  }
  return mySubmissions.value
})
const displayedSubmissionTitle = computed(() =>
  isSingleMode.value && !displayedSubmissions.value.some((submission) => submission.ownSubmission)
    ? '本车间提交'
    : '我的提交'
)
const canUploadAttachment = computed(() => {
  if (!workshopUploadEnabled.value || deadlineExpired.value) {
    return false
  }
  return !isSingleMode.value || !submissionStatus.value
})

async function load() {
  resetUpload()
  destroyOfficePreview()
  inlinePreviewKind.value = 'MESSAGE'
  inlinePreviewUrl.value = ''
  inlinePreviewMessage.value = '正在加载预览'
  item.value = await apiGet<DocItem>(`/doc-items/${itemId.value}`)
  if (workshopUploadEnabled.value) {
    const [status, submissions] = await Promise.all([
      apiGet<DocSubmission | null>(`/doc-items/${itemId.value}/my-submission`),
      apiGet<DocSubmission[]>(`/doc-items/${itemId.value}/submissions`)
    ])
    mySubmission.value = status
    mySubmissions.value = submissions.filter((submission) => submission.ownSubmission || submission.uploadUserId === auth.user?.id)
  } else {
    mySubmission.value = null
    mySubmissions.value = []
  }
  await loadPrimaryPreview()
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
    ElMessage.warning(`请选择“${missing.requirementName}”文件`)
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
  issuedAttachmentFiles.value = files.slice(0, 1).map((file) => file.raw).filter((file): file is UploadRawFile => Boolean(file))
}

function onIssuedFileExceed() {
  ElMessage.warning('一次只能上传一个文件')
}

function openIssuedUpload() {
  if (primaryAttachment.value) {
    return
  }
  issuedUploadOpen.value = true
}

async function submitIssuedAttachments() {
  if (primaryAttachment.value) {
    return
  }
  if (!issuedAttachmentFiles.value.length) {
    ElMessage.warning('请选择文件')
    return
  }
  if (issuedAttachmentFiles.value.length !== 1) {
    ElMessage.warning('一次只能上传一个文件')
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

function removePendingIssuedFile() {
  issuedAttachmentFiles.value = []
  issuedUploadRef.value?.clearFiles()
}

async function openRecords() {
  records.value = await apiGet<DocSubmission[]>(`/doc-items/${itemId.value}/submissions`)
  recordsOpen.value = true
}

async function deleteSubmission(submission: DocSubmission) {
  await ElMessageBox.confirm('确定删除这条提交记录吗？', '删除提交', { type: 'warning' })
  await apiDelete(`/submissions/${submission.id}`)
  ElMessage.success('删除成功')
  if (recordsOpen.value) {
    records.value = await apiGet<DocSubmission[]>(`/doc-items/${itemId.value}/submissions`)
  }
  await load()
}

async function downloadAttachment(attachment: DocAttachment) {
  const response = await http.get(`/doc-attachments/${attachment.id}/download`, { responseType: 'blob' })
  downloadBlob(response.data, attachment.originalFileName || '文件')
}

async function downloadIssuedAttachment(attachment: DocItemAttachment) {
  const response = await http.get(`/doc-item-attachments/${attachment.id}/download`, { responseType: 'blob' })
  downloadBlob(response.data, attachment.originalFileName || '文件')
}

async function loadPrimaryPreview() {
  const attachment = primaryAttachment.value
  if (!attachment) {
    inlinePreviewKind.value = 'MESSAGE'
    inlinePreviewUrl.value = ''
    inlinePreviewMessage.value = '暂无可预览文件'
    return
  }
  const preview = await apiGet<AttachmentPreview>(`/doc-item-attachments/${attachment.id}/preview`)
  if ((preview.previewType === 'PDF' || preview.previewType === 'IMAGE') && preview.url) {
    inlinePreviewKind.value = preview.previewType
    inlinePreviewUrl.value = new URL(preview.url, window.location.origin).toString()
    inlinePreviewMessage.value = ''
    return
  }
  if (preview.previewType === 'ONLYOFFICE' && preview.documentServerUrl && preview.url) {
    inlinePreviewKind.value = 'ONLYOFFICE'
    inlinePreviewUrl.value = ''
    inlinePreviewMessage.value = ''
    await openOfficePreview(preview, attachment)
    return
  }
  inlinePreviewKind.value = 'MESSAGE'
  inlinePreviewUrl.value = ''
  inlinePreviewMessage.value = preview.previewType === 'UNCONFIGURED'
    ? '本地预览服务未启动，请确认 Docker 和 OnlyOffice 已运行'
    : preview.message || '该格式暂不支持在线预览'
}

async function openOfficePreview(preview: AttachmentPreview, attachment: DocItemAttachment) {
  if (!preview.documentServerUrl || !preview.url) {
    inlinePreviewKind.value = 'MESSAGE'
    inlinePreviewMessage.value = '本地预览服务未启动，请确认 Docker 和 OnlyOffice 已运行'
    return
  }
  const documentServerUrl = preview.documentServerUrl
  const downloadUrl = preview.url
  try {
    await loadOnlyOfficeScript(documentServerUrl)
  } catch {
    inlinePreviewKind.value = 'MESSAGE'
    inlinePreviewMessage.value = '本地预览服务未启动，请确认 Docker 和 OnlyOffice 已运行'
    return
  }
  setTimeout(() => {
    destroyOfficePreview()
    const DocsAPI = (window as any).DocsAPI
    if (!DocsAPI?.DocEditor) {
      inlinePreviewKind.value = 'MESSAGE'
      inlinePreviewMessage.value = '本地预览服务未启动，请确认 Docker 和 OnlyOffice 已运行'
      return
    }
    const fileUrl = onlyOfficeFileUrl(downloadUrl)
    ;(window as any).__docEditor = new DocsAPI.DocEditor('onlyoffice-inline-preview-host', {
      document: {
        fileType: (attachment.extension || preview.fileType || '').toLowerCase(),
        key: `${attachment.id}-${Date.now()}`,
        title: preview.title || attachment.originalFileName,
        url: fileUrl
      },
      documentType: onlyOfficeDocumentType(attachment.extension || preview.fileType),
      editorConfig: { lang: 'zh-CN', mode: 'view' },
      height: '100%',
      width: '100%'
    })
  }, 0)
}

function onlyOfficeFileUrl(downloadUrl: string) {
  const isLocalHost = ['localhost', '127.0.0.1'].includes(window.location.hostname)
  const defaultBase = isLocalHost ? 'http://host.docker.internal:8010' : window.location.origin
  const base = import.meta.env.VITE_ONLYOFFICE_FILE_BASE || defaultBase
  return new URL(downloadUrl, base).toString()
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

.selected-upload-card {
  min-height: 46px;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #f8fafc;
  color: #1f2d3d;
}

.inline-preview-section {
  margin-top: 18px;
}

.preview-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.preview-heading-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-paper {
  width: min(100%, 880px);
  aspect-ratio: 210 / 297;
  margin: 0 auto;
  background: #fff;
  border: 1px solid #d8e0ea;
  box-shadow: 0 18px 42px rgba(31, 45, 61, 0.14);
  overflow: hidden;
}

.pdf-preview-frame,
.image-preview,
.office-preview-host {
  width: 100%;
  height: 100%;
  border: 0;
}

.image-preview {
  display: block;
  object-fit: contain;
  background: #f6f8fb;
}

.preview-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 28px;
  color: #637083;
  text-align: center;
}

.preview-placeholder p {
  margin: 0;
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
