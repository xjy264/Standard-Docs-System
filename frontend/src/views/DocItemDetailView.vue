<template>
  <div class="doc-detail-page">
    <div class="detail-header">
      <div>
        <el-button link type="primary" @click="router.push(`/org/${deptId}`)">返回文件菜单</el-button>
        <h2>{{ item?.itemName || '文件详情' }}</h2>
        <p v-if="item" class="detail-meta">{{ [item.sectionDeptName, item.categoryName, item.docYear].filter(Boolean).join(' / ') }}</p>
      </div>
      <div class="header-actions">
        <el-button v-if="canUploadAttachment" type="primary" @click="uploadOpen = true">上传附件</el-button>
        <el-button @click="openRecords">上传记录</el-button>
      </div>
    </div>

    <section class="detail-content">
      <div v-if="item?.contentHtml" class="rich-content" v-html="item.contentHtml"></div>
      <el-empty v-else description="暂无文件内容" />
    </section>

    <el-dialog v-model="uploadOpen" title="上传附件" width="720px" @closed="resetUpload">
      <el-upload
        ref="uploadRef"
        class="attachment-upload"
        drag
        multiple
        :auto-upload="false"
        :on-change="onFileChange"
        :on-remove="onFileRemove"
      >
        <div>拖拽附件到此处，或点击选择文件</div>
      </el-upload>
      <div class="upload-actions">
        <el-button type="primary" :loading="submitting" @click="submitAttachment">上传附件</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="recordsOpen" title="上传记录" width="980px">
      <el-table :data="records" stripe>
        <el-table-column prop="submittedAt" label="上传时间" width="180" />
        <el-table-column prop="sectionDeptName" label="所属科室" width="120" />
        <el-table-column prop="categoryName" label="所属目录" width="140" />
        <el-table-column prop="itemName" label="文件名称" min-width="150" />
        <el-table-column prop="submitterDeptName" label="所属组织" width="130" />
        <el-table-column prop="uploadUserName" label="上传人" width="120" />
        <el-table-column prop="attachmentCount" label="附件数" width="90" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }"><el-button link type="primary" @click="openRecordDetail(row)">查看</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="recordDetailOpen" title="上传记录详情" width="720px">
      <el-descriptions v-if="recordDetail" :column="2" border>
        <el-descriptions-item label="文件名称">{{ recordDetail.itemName }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ recordDetail.submittedAt }}</el-descriptions-item>
        <el-descriptions-item label="所属组织">{{ recordDetail.submitterDeptName }}</el-descriptions-item>
        <el-descriptions-item label="上传人">{{ recordDetail.uploadUserName }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="recordDetail" :data="recordDetail.attachments || []" style="margin-top: 14px" border>
        <el-table-column prop="originalFileName" label="附件" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }"><el-button link type="primary" @click="downloadAttachment(row)">下载</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage, type UploadFile, type UploadFiles, type UploadRawFile } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet, http } from '../api/http'
import { useAuthStore } from '../stores/auth'

interface SectionItem {
  id: number
  deptName: string
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
  attachments?: Array<{ id: number; originalFileName: string }>
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const deptId = computed(() => Number(route.params.deptId))
const itemId = computed(() => Number(route.params.itemId))
const sections = ref<SectionItem[]>([])
const item = ref<DocItem>()
const recordsOpen = ref(false)
const uploadOpen = ref(false)
const recordDetailOpen = ref(false)
const records = ref<DocSubmission[]>([])
const recordDetail = ref<DocSubmission>()
const uploadFiles = ref<UploadRawFile[]>([])
const uploadRef = ref<any>()
const submitting = ref(false)

const canUploadAttachment = computed(() => Boolean(item.value?.attachmentEnabled))

async function load() {
  sections.value = await apiGet<SectionItem[]>('/sections/navigation')
  item.value = await apiGet<DocItem>(`/doc-items/${itemId.value}`)
}

function onFileChange(_file: UploadFile, files: UploadFiles) {
  syncFiles(files)
}

function onFileRemove(_file: UploadFile, files: UploadFiles) {
  syncFiles(files)
}

function syncFiles(files: UploadFiles) {
  uploadFiles.value = files.map((file) => file.raw).filter((file): file is UploadRawFile => Boolean(file))
}

async function submitAttachment() {
  if (!uploadFiles.value.length) {
    ElMessage.warning('请选择附件')
    return
  }
  submitting.value = true
  try {
    const form = new FormData()
    uploadFiles.value.forEach((file) => form.append('files', file))
    await http.post(`/doc-items/${itemId.value}/submissions`, form)
    ElMessage.success('上传成功')
    uploadOpen.value = false
    if (recordsOpen.value) {
      await openRecords()
    }
  } finally {
    submitting.value = false
  }
}

function resetUpload() {
  uploadFiles.value = []
  uploadRef.value?.clearFiles()
}

async function openRecords() {
  records.value = await apiGet<DocSubmission[]>(`/doc-items/${itemId.value}/submissions`)
  recordsOpen.value = true
}

async function openRecordDetail(record: DocSubmission) {
  recordDetail.value = await apiGet<DocSubmission>(`/submissions/${record.id}`)
  recordDetailOpen.value = true
}

async function downloadAttachment(attachment: { id: number; originalFileName: string }) {
  const response = await http.get(`/doc-attachments/${attachment.id}/download`, { responseType: 'blob' })
  const blob = new Blob([response.data])
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = attachment.originalFileName || '附件'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

onMounted(load)
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
</style>
