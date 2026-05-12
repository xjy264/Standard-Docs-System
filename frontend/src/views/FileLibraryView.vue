<template>
  <div>
    <div class="page-title">
      <h2>{{ title }}</h2>
      <el-button type="primary" v-if="showUploadButton" @click="openUpload">上传文件</el-button>
    </div>
    <div class="query-bar">
      <el-form inline>
        <el-form-item label="文件名"><el-input v-model="query.keyword" clearable placeholder="输入文件名" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.extension" clearable placeholder="全部" style="width:160px">
            <el-option v-for="item in fileTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属组织"><el-input v-model="query.ownerDeptName" clearable placeholder="输入所属组织" /></el-form-item>
        <el-form-item label="所属人"><el-input v-model="query.ownerName" clearable placeholder="输入所属人" /></el-form-item>
        <el-form-item label="上传日期"><el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" style="width:240px" /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="section">
      <el-table :data="files" stripe>
        <el-table-column prop="fileName" label="文件名称" min-width="260" />
        <el-table-column prop="extension" label="类型" min-width="120" />
        <el-table-column prop="fileSize" label="大小" min-width="140" :formatter="sizeText" />
        <el-table-column prop="ownerDeptName" label="所属组织" min-width="170" />
        <el-table-column prop="ownerName" label="所属人" min-width="150" />
        <el-table-column prop="createdAt" label="上传时间" min-width="180" />
        <el-table-column label="操作" :min-width="operationWidth" align="left">
          <template #default="{ row }">
            <el-button link type="primary" @click="download(row)">下载</el-button>
            <el-button link type="warning" v-if="canReplace(row)" @click="openReplace(row)">替换文件</el-button>
            <el-button link type="danger" v-if="canDelete(row)" @click="deleteFile(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="uploadOpen"
      title="上传文件"
      width="520px"
      :close-on-click-modal="!uploading"
      :close-on-press-escape="!uploading"
      :show-close="!uploading"
      @closed="resetUploadForm"
    >
      <el-form label-position="top">
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            class="file-upload-drag"
            drag
            multiple
            :auto-upload="false"
            :limit="uploadLimit"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :on-exceed="onFileExceed"
          >
            <div>拖拽多个文件到此处，或点击选择文件</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="uploading" @click="uploadOpen = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="uploading || selectedFiles.length === 0" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="replaceOpen" title="替换文件" width="520px">
      <el-form label-position="top">
        <el-form-item label="选择新文件">
          <el-upload ref="replaceUploadRef" class="file-upload-drag" drag :auto-upload="false" :limit="1" :on-change="onReplaceFileChange">
            <div>拖拽文件到此处，或点击选择文件</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeReplace">取消</el-button>
        <el-button type="primary" @click="submitReplace">替换</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox, type UploadFile, type UploadFiles, type UploadRawFile } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { http, apiDelete, apiGet } from '../api/http'
import { useAuthStore } from '../stores/auth'

interface FileTypeOption {
  label: string
  value: string
}

const props = withDefaults(defineProps<{
  title?: string
  mine?: boolean
  manageOwnerFiles?: boolean
  showUpload?: boolean
}>(), {
  title: '文件库',
  mine: false,
  manageOwnerFiles: false,
  showUpload: true
})

const auth = useAuthStore()
const files = ref<any[]>([])
const uploadOpen = ref(false)
const selectedFiles = ref<UploadRawFile[]>([])
const uploading = ref(false)
const uploadRef = ref<any>()
const replaceOpen = ref(false)
const replaceTarget = ref<any>()
const replaceFile = ref<File>()
const replaceUploadRef = ref<any>()
const dateRange = ref<string[]>([])
const query = reactive({ keyword: '', extension: '', ownerDeptName: '', ownerName: '' })
const title = computed(() => props.title)
const showUploadButton = computed(() => props.showUpload && auth.hasPermission('file:upload'))
const operationWidth = computed(() => props.manageOwnerFiles ? 230 : auth.user?.isSuperAdmin ? 130 : 90)
const uploadLimit = 20
const uploadConcurrency = 3
const fileTypeOptions: FileTypeOption[] = [
  { label: 'Word 文档', value: 'doc' },
  { label: 'Excel 表格', value: 'xls' },
  { label: 'PPT 演示', value: 'ppt' },
  { label: 'PDF 文档', value: 'pdf' },
  { label: 'CAD 图纸', value: 'dwg' },
  { label: '图片', value: 'jpg' },
  { label: '压缩包', value: 'zip' }
]

async function load() {
  const params: Record<string, unknown> = { ...query }
  if (props.mine) {
    params.mine = true
  }
  if (dateRange.value?.length === 2) {
    params.uploadStart = dateRange.value[0]
    params.uploadEnd = dateRange.value[1]
  }
  files.value = await apiGet('/files', params)
}

async function openUpload() {
  resetUploadForm()
  uploadOpen.value = true
}

function onFileChange(_file: UploadFile, uploadFiles: UploadFiles) {
  syncSelectedFiles(uploadFiles)
}

function onFileRemove(_file: UploadFile, uploadFiles: UploadFiles) {
  syncSelectedFiles(uploadFiles)
}

function onFileExceed() {
  ElMessage.warning(`单次最多选择 ${uploadLimit} 个文件，请分批上传`)
}

function syncSelectedFiles(uploadFiles: UploadFiles) {
  selectedFiles.value = uploadFiles
    .map((item) => item.raw)
    .filter((file): file is UploadRawFile => Boolean(file))
}

async function submitUpload() {
  if (uploading.value) {
    return
  }
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    const result = await uploadSelectedFiles([...selectedFiles.value])
    if (result.successCount > 0) {
      await load()
      uploadOpen.value = false
    }
    if (result.successCount > 0 && result.failCount === 0) {
      ElMessage.success(`上传成功，共 ${result.successCount} 个文件`)
    } else if (result.successCount > 0) {
      ElMessage.warning(`上传完成，成功 ${result.successCount} 个，失败 ${result.failCount} 个，请重新选择失败文件后再试`)
    } else {
      ElMessage.error('上传失败，请稍后重试')
    }
  } finally {
    uploading.value = false
    resetUploadForm()
  }
}

async function uploadSelectedFiles(uploadFiles: UploadRawFile[]) {
  let nextIndex = 0
  let successCount = 0
  let failCount = 0
  const workerCount = Math.min(uploadConcurrency, uploadFiles.length)

  async function uploadWorker() {
    while (nextIndex < uploadFiles.length) {
      const file = uploadFiles[nextIndex]
      nextIndex += 1
      try {
        await uploadSingleFile(file)
        successCount += 1
      } catch {
        failCount += 1
      }
    }
  }

  await Promise.all(Array.from({ length: workerCount }, () => uploadWorker()))
  return { successCount, failCount }
}

async function uploadSingleFile(file: UploadRawFile) {
  const form = new FormData()
  form.append('file', file)
  await http.post('/files/upload', form, { headers: { 'X-Silent-Error': '1' } })
}

function resetQuery() {
  query.keyword = ''
  query.extension = ''
  query.ownerDeptName = ''
  query.ownerName = ''
  dateRange.value = []
  load()
}

async function download(row: any) {
  const response = await http.get(`/files/${row.id}/download`, { responseType: 'blob' })
  const blob = new Blob([response.data])
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = row.originalFileName || row.fileName || 'download'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

async function deleteFile(row: any) {
  await ElMessageBox.confirm('确认删除该文件？文件将进入回收站。', '删除确认', { type: 'warning' })
  await apiDelete(`/files/${row.id}`)
  ElMessage.success('删除成功')
  load()
}

function openReplace(row: any) {
  replaceTarget.value = row
  replaceOpen.value = true
}

function onReplaceFileChange(file: UploadFile) {
  replaceFile.value = file.raw
}

async function submitReplace() {
  if (!replaceTarget.value) {
    return
  }
  if (!replaceFile.value) {
    ElMessage.warning('请选择新文件')
    return
  }
  const form = new FormData()
  form.append('file', replaceFile.value)
  await http.post(`/files/${replaceTarget.value.id}/replace`, form)
  ElMessage.success('替换成功')
  closeReplace()
  load()
}

function closeReplace() {
  replaceOpen.value = false
  replaceTarget.value = undefined
  replaceFile.value = undefined
  replaceUploadRef.value?.clearFiles()
}

function canReplace(row: any) {
  return props.manageOwnerFiles && isOwner(row)
}

function canDelete(row: any) {
  return (props.manageOwnerFiles && isOwner(row)) || auth.user?.isSuperAdmin
}

function isOwner(row: any) {
  return row.uploadUserId === auth.user?.id
}

function resetUploadForm() {
  selectedFiles.value = []
  uploadRef.value?.clearFiles()
}

function sizeText(_row: any, _column: any, value: number) {
  if (!value) return '0 B'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

onMounted(load)
</script>

<style scoped>
.file-upload-drag {
  width: 100%;
}

.file-upload-drag :deep(.el-upload),
.file-upload-drag :deep(.el-upload-dragger) {
  width: 100%;
}

</style>
