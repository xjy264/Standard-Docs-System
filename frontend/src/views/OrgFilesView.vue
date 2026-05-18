<template>
  <div>
    <div class="page-title">
      <div>
        <h2>{{ pageTitle }}</h2>
        <el-breadcrumb class="org-breadcrumb" separator="/">
          <el-breadcrumb-item :to="{ path: `/org/${deptId}` }">{{ currentDept?.deptName || '组织资料' }}</el-breadcrumb-item>
          <el-breadcrumb-item v-for="item in folderPath" :key="item.id" :to="{ path: `/org/${deptId}/folders/${item.id}` }">
            {{ item.folderName }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="page-actions">
        <el-button v-if="canOperate" @click="openCreateFolder">新增文件夹</el-button>
        <el-button v-if="canUpload" type="primary" @click="openUpload">新增文件</el-button>
      </div>
    </div>

    <div class="finder-panel">
      <div class="finder-toolbar">
        <el-button v-if="folderId" @click="goParent">返回上级</el-button>
      </div>
      <div class="finder-grid">
        <div v-for="folder in childFolders" :key="`folder-${folder.id}`" class="finder-item finder-folder-item">
          <el-dropdown
            v-if="canOperate"
            class="finder-more-dropdown"
            trigger="click"
            placement="bottom-end"
            @command="(command: string | number | object) => handleFolderCommand(folder, command)"
          >
            <button class="finder-more-button" type="button" @click.stop>
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">修改文件夹名称</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <button class="finder-main" type="button" @click="openFolder(folder)">
            <el-icon class="finder-icon folder-icon"><FolderOpened /></el-icon>
            <span class="finder-name">{{ folder.folderName }}</span>
          </button>
        </div>
        <div v-for="file in files" :key="`file-${file.id}`" class="finder-item">
          <button class="finder-main" type="button" @click="download(file)">
            <FileTypeIcon :extension="file.extension" :file-name="file.fileName" :size="50" />
            <span class="finder-name">{{ file.fileName }}</span>
          </button>
        </div>
        <el-empty v-if="childFolders.length === 0 && files.length === 0" class="finder-empty" description="暂无文件夹和文件" />
      </div>
    </div>

    <el-dialog v-model="folderDialogOpen" :title="folderDialogTitle" width="420px">
      <el-form label-position="top">
        <el-form-item label="文件夹名称"><el-input v-model="folderForm.folderName" maxlength="128" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="folderDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitFolder">{{ folderDialogConfirmText }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="uploadOpen"
      title="新增文件"
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
        <el-button type="primary" :loading="uploading" :disabled="uploading || selectedFiles.length === 0" @click="submitUpload">新增</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { FolderOpened, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage, type UploadFile, type UploadFiles, type UploadRawFile } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FileTypeIcon from '../components/FileTypeIcon.vue'
import { apiGet, apiPost, apiPut, http } from '../api/http'
import { useAuthStore } from '../stores/auth'

interface DeptNavigationItem {
  id: number
  deptName: string
  children: DeptNavigationItem[]
}

interface FolderItem {
  id: number
  parentId: number
  folderName: string
  deptId: number
}

interface FileItem {
  id: number
  fileName: string
  extension?: string
  originalFileName?: string
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const navigation = ref<DeptNavigationItem[]>([])
const folders = ref<FolderItem[]>([])
const files = ref<FileItem[]>([])
const folderDialogOpen = ref(false)
const uploadOpen = ref(false)
const selectedFiles = ref<UploadRawFile[]>([])
const uploading = ref(false)
const uploadRef = ref<any>()
const folderForm = reactive({ folderName: '' })
const editingFolder = ref<FolderItem>()
const deptId = computed(() => Number(route.params.deptId))
const folderId = computed(() => route.params.folderId ? Number(route.params.folderId) : undefined)
const canOperate = computed(() => Boolean(auth.user?.isSuperAdmin) || auth.user?.deptId === deptId.value)
const canUpload = computed(() => canOperate.value && auth.hasPermission('file:upload'))
const currentDept = computed(() => flattenDepts(navigation.value).find((item) => item.id === deptId.value))
const childFolders = computed(() => {
  const parentId = folderId.value || 0
  return folders.value.filter((item) => Number(item.parentId || 0) === parentId)
})
const folderPath = computed(() => {
  if (!folderId.value) {
    return []
  }
  const result: FolderItem[] = []
  const map = new Map(folders.value.map((item) => [Number(item.id), item]))
  let current = map.get(folderId.value)
  while (current) {
    result.unshift(current)
    current = Number(current.parentId || 0) ? map.get(Number(current.parentId)) : undefined
  }
  return result
})
const pageTitle = computed(() => {
  const currentFolder = folderPath.value[folderPath.value.length - 1]
  return currentFolder ? currentFolder.folderName : `${currentDept.value?.deptName || '组织'}资料`
})
const folderDialogTitle = computed(() => editingFolder.value ? '修改文件夹名称' : '新增文件夹')
const folderDialogConfirmText = computed(() => editingFolder.value ? '修改' : '保存')
const uploadLimit = 20

async function load() {
  if (!deptId.value) {
    return
  }
  const [navResult, folderResult, fileResult] = await Promise.all([
    apiGet<DeptNavigationItem[]>('/depts/navigation'),
    apiGet<FolderItem[]>('/folders', { deptId: deptId.value }),
    folderId.value
      ? apiGet<FileItem[]>('/files', { deptId: deptId.value, folderId: folderId.value })
      : apiGet<FileItem[]>('/files', { deptId: deptId.value, unfiled: true })
  ])
  navigation.value = navResult
  folders.value = folderResult
  files.value = fileResult
}

function flattenDepts(items: DeptNavigationItem[]) {
  const result: DeptNavigationItem[] = []
  const visit = (nodes: DeptNavigationItem[]) => {
    nodes.forEach((node) => {
      result.push(node)
      visit(node.children || [])
    })
  }
  visit(items)
  return result
}

function openCreateFolder() {
  editingFolder.value = undefined
  folderForm.folderName = ''
  folderDialogOpen.value = true
}

function openRenameFolder(folder: FolderItem) {
  editingFolder.value = folder
  folderForm.folderName = folder.folderName
  folderDialogOpen.value = true
}

function handleFolderCommand(folder: FolderItem, command: string | number | object) {
  if (command === 'rename') {
    openRenameFolder(folder)
  }
}

async function submitFolder() {
  if (!folderForm.folderName.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  if (editingFolder.value) {
    await apiPut(`/folders/${editingFolder.value.id}`, {
      folderName: folderForm.folderName.trim(),
      deptId: editingFolder.value.deptId,
      parentId: editingFolder.value.parentId || 0
    })
    ElMessage.success('修改成功')
  } else {
    await apiPost('/folders', {
      folderName: folderForm.folderName.trim(),
      deptId: deptId.value,
      parentId: folderId.value || 0
    })
    ElMessage.success('新增成功')
  }
  folderDialogOpen.value = false
  editingFolder.value = undefined
  await load()
}

function openFolder(folder: FolderItem) {
  router.push(`/org/${deptId.value}/folders/${folder.id}`)
}

function goParent() {
  const current = folderPath.value[folderPath.value.length - 1]
  if (current?.parentId) {
    router.push(`/org/${deptId.value}/folders/${current.parentId}`)
  } else {
    router.push(`/org/${deptId.value}`)
  }
}

function openUpload() {
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
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    for (const file of selectedFiles.value) {
      const form = new FormData()
      form.append('file', file)
      form.append('deptId', String(deptId.value))
      if (folderId.value) {
        form.append('folderId', String(folderId.value))
      }
      await http.post('/files/upload', form)
    }
    ElMessage.success(`新增成功，共 ${selectedFiles.value.length} 个文件`)
    uploadOpen.value = false
    await load()
  } finally {
    uploading.value = false
    resetUploadForm()
  }
}

async function download(file: FileItem) {
  const response = await http.get(`/files/${file.id}/download`, { responseType: 'blob' })
  const blob = new Blob([response.data])
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = file.originalFileName || file.fileName || 'download'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function resetUploadForm() {
  selectedFiles.value = []
  uploadRef.value?.clearFiles()
}

onMounted(load)
watch(() => [route.params.deptId, route.params.folderId], load)
</script>

<style scoped>
.org-breadcrumb {
  margin-top: 8px;
}

.page-actions {
  display: flex;
  gap: 10px;
}

.finder-panel {
  min-height: 520px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 14px;
}

.finder-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  margin-bottom: 14px;
  color: #637083;
}

.finder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(118px, 1fr));
  gap: 22px 18px;
  align-items: start;
}

.finder-empty {
  grid-column: 1 / -1;
  min-height: 420px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.finder-item {
  position: relative;
  min-height: 136px;
  padding: 10px 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #1f2d3d;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.finder-main {
  width: 100%;
  min-height: 108px;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.finder-item:hover {
  background: #eef5ff;
  border-color: #c8ddf5;
}

.finder-more-dropdown {
  position: absolute;
  top: 3px;
  right: 3px;
  z-index: 2;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.18s ease;
}

.finder-folder-item:hover .finder-more-dropdown,
.finder-more-dropdown:focus-within {
  opacity: 1;
  pointer-events: auto;
}

.finder-more-button {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #6a7687;
  cursor: pointer;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.finder-more-button:hover,
.finder-more-button:active {
  background: rgb(220 233 251 / 92%);
  color: #2f5fa8;
}

.finder-more-button :deep(svg) {
  font-size: 13px;
}

.finder-icon {
  font-size: 56px;
}

.folder-icon {
  color: #2f9bd8;
}

.finder-name {
  width: 100%;
  margin-top: 10px;
  line-height: 1.35;
  text-align: center;
  word-break: break-word;
  font-size: 13px;
}

.file-upload-drag {
  width: 100%;
}

.file-upload-drag :deep(.el-upload),
.file-upload-drag :deep(.el-upload-dragger) {
  width: 100%;
}
</style>
