<template>
  <div class="template-page">
    <div class="page-title compact-title">
      <h2>房建大修项目模板库</h2>
    </div>
    <section class="template-panel">
      <div class="toolbar">
        <el-button type="primary" @click="openItemDialog()">新增模板文件</el-button>
      </div>
      <el-table :data="templateItems" stripe>
        <el-table-column prop="itemName" label="模板文件" min-width="200" />
        <el-table-column prop="originalFileName" label="文件" min-width="220" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ fileTypeText(row) }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openItemDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="itemDialogOpen" :title="editingItem ? '编辑模板文件' : '新增模板文件'" width="560px" @closed="resetItemUpload">
      <el-form label-position="top">
        <el-form-item label="模板文件名称"><el-input v-model="itemForm.itemName" maxlength="128" /></el-form-item>
        <el-form-item label="文件类型">
          <el-tag>{{ fileTypeText({ fileType: itemForm.fileType, originalFileName: itemFile?.name || editingItem?.originalFileName || itemForm.itemName }) }}</el-tag>
        </el-form-item>
        <el-form-item label="文件">
          <div v-if="itemFile" class="current-file file-card">
            <span>待上传：{{ itemFile.name }}</span>
            <el-button link type="danger" @click="removeItemFile">移除</el-button>
          </div>
          <div v-else-if="editingItem?.originalFileName" class="current-file file-card">
            <span>当前文件：{{ editingItem.originalFileName }}</span>
            <el-upload
              ref="itemUploadRef"
              :auto-upload="false"
              :limit="1"
              :show-file-list="false"
              :on-change="onItemFileChange"
              :on-remove="onItemFileRemove"
              :on-exceed="onItemFileExceed"
            >
              <el-button link type="primary">重新选择</el-button>
            </el-upload>
          </div>
          <el-upload
            v-else
            ref="itemUploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="onItemFileChange"
            :on-remove="onItemFileRemove"
            :on-exceed="onItemFileExceed"
          >
            <div>拖拽文件到此处，或点击选择文件</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="itemForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox, type UploadFile, type UploadFiles, type UploadRawFile } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { apiDelete, apiGet, apiPut, http } from '../api/http'

interface TemplateItem {
  id: number
  templateId: number
  itemName: string
  fileType: string
  originalFileName?: string
  sortOrder: number
}

const templateItems = ref<TemplateItem[]>([])
const editingItem = ref<TemplateItem>()
const itemDialogOpen = ref(false)
const itemForm = reactive({ itemName: '', fileType: 'OTHER', sortOrder: 0 })
const itemFile = ref<UploadRawFile>()
const itemUploadRef = ref<any>()

async function loadTemplateItems() {
  templateItems.value = await apiGet<TemplateItem[]>('/repair-project-templates/items')
}

function openItemDialog(item?: TemplateItem) {
  editingItem.value = item
  itemForm.itemName = item?.itemName || ''
  itemForm.fileType = item?.fileType || 'OTHER'
  itemForm.sortOrder = item?.sortOrder || 0
  itemFile.value = undefined
  itemDialogOpen.value = true
}

async function submitItem() {
  if (!itemForm.itemName.trim()) {
    ElMessage.warning('请输入模板文件名称')
    return
  }
  if (!editingItem.value && !itemFile.value) {
    ElMessage.warning('请上传模板文件')
    return
  }
  if (itemFile.value) {
    const form = new FormData()
    form.append('itemName', itemForm.itemName.trim())
    form.append('sortOrder', String(itemForm.sortOrder || 0))
    form.append('file', itemFile.value)
    if (editingItem.value) {
      await http.put(`/repair-project-templates/items/${editingItem.value.id}`, form)
    } else {
      await http.post('/repair-project-templates/items', form)
    }
  } else if (editingItem.value) {
    const body = { itemName: itemForm.itemName.trim(), fileType: itemForm.fileType, sortOrder: itemForm.sortOrder }
    await apiPut(`/repair-project-templates/items/${editingItem.value.id}`, body)
  }
  itemDialogOpen.value = false
  ElMessage.success('保存成功')
  await loadTemplateItems()
}

function onItemFileChange(_file: UploadFile, files: UploadFiles) {
  syncItemFile(files)
}

function onItemFileRemove(_file: UploadFile, files: UploadFiles) {
  syncItemFile(files)
}

function syncItemFile(files: UploadFiles) {
  itemFile.value = files.map((file) => file.raw).find((file): file is UploadRawFile => Boolean(file))
  if (!itemFile.value) {
    itemForm.fileType = editingItem.value?.fileType || 'OTHER'
    return
  }
  itemForm.fileType = guessFileType(itemFile.value.name)
  if (!itemForm.itemName.trim()) {
    itemForm.itemName = itemFile.value.name.replace(/\.[^.]+$/, '')
  }
}

function onItemFileExceed() {
  ElMessage.warning('只能上传一个文件')
}

function removeItemFile() {
  itemFile.value = undefined
  itemForm.fileType = editingItem.value?.fileType || 'OTHER'
  itemUploadRef.value?.clearFiles()
}

function resetItemUpload() {
  itemFile.value = undefined
  itemUploadRef.value?.clearFiles()
}

function guessFileType(name: string) {
  const lower = name.toLowerCase()
  if (/\.(doc|docx)$/.test(lower)) return 'WORD'
  if (/\.(xls|xlsx)$/.test(lower)) return 'EXCEL'
  if (/\.(ppt|pptx)$/.test(lower)) return 'PPT'
  if (/\.pdf$/.test(lower)) return 'PDF'
  if (/\.zip$/.test(lower)) return 'ZIP'
  if (/\.(png|jpg|jpeg|gif|bmp|webp|svg)$/.test(lower)) return 'IMAGE'
  return 'OTHER'
}

function fileTypeText(input: string | { fileType?: string; originalFileName?: string; itemName?: string }) {
  const type = typeof input === 'string' ? input : input.fileType || 'OTHER'
  if (type === 'IMAGE') {
    const extension = typeof input === 'string' ? '' : fileExtension(input.originalFileName || input.itemName)
    return ['JPG', 'JPEG', 'PNG', 'GIF', 'BMP', 'WEBP', 'SVG'].includes(extension) ? extension : 'IMG'
  }
  const labels: Record<string, string> = {
    WORD: 'Word',
    EXCEL: 'Excel',
    PPT: 'PPT',
    PDF: 'PDF',
    IMAGE: 'IMG',
    ZIP: 'ZIP',
    OTHER: '其他'
  }
  return labels[type] || '其他'
}

function fileExtension(name?: string) {
  const match = (name || '').match(/\.([^.]+)$/)
  return match?.[1]?.toUpperCase() || ''
}

async function deleteItem(item: TemplateItem) {
  await ElMessageBox.confirm(`确定删除“${item.itemName}”吗？`, '删除模板文件', { type: 'warning' })
  await apiDelete(`/repair-project-templates/items/${item.id}`)
  ElMessage.success('删除成功')
  await loadTemplateItems()
}

onMounted(loadTemplateItems)
</script>

<style scoped>
.compact-title {
  margin-bottom: 12px;
}

.template-panel {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 14px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 12px;
}

.current-file {
  color: var(--text-secondary);
  font-size: 13px;
}

.file-card {
  width: 100%;
  min-height: 46px;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #f8fafc;
}
</style>
