<template>
  <div class="template-page">
    <div class="page-title compact-title">
      <h2>房建大修项目模板</h2>
    </div>
    <section class="template-layout">
      <div class="template-main">
        <div class="toolbar">
          <el-button type="primary" @click="openTemplateDialog()">新增模板</el-button>
        </div>
        <el-table :data="templates" stripe>
          <el-table-column prop="templateName" label="模板名称" min-width="220" />
          <el-table-column prop="sortOrder" label="排序" width="100" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button link type="primary" @click="selectTemplate(row)">资料项</el-button>
              <el-button link type="primary" @click="openTemplateDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="deleteTemplate(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="template-side">
        <div class="side-header">
          <h3>{{ selectedTemplate?.templateName || '模板资料项' }}</h3>
          <el-button :disabled="!selectedTemplate" type="primary" plain @click="openItemDialog()">新增模板文件</el-button>
        </div>
        <el-table v-if="selectedTemplate" :data="templateItems" border>
          <el-table-column prop="itemName" label="模板文件" min-width="180" />
          <el-table-column prop="originalFileName" label="文件" min-width="180" />
          <el-table-column prop="fileType" label="类型" width="100" />
          <el-table-column prop="sortOrder" label="排序" width="90" />
          <el-table-column label="操作" width="130">
            <template #default="{ row }">
              <el-button link type="primary" @click="openItemDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="deleteItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="请选择模板" />
      </div>
    </section>

    <el-dialog v-model="templateDialogOpen" :title="editingTemplate ? '编辑模板' : '新增模板'" width="520px">
      <el-form label-position="top">
        <el-form-item label="模板名称"><el-input v-model="templateForm.templateName" maxlength="128" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="templateForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitTemplate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDialogOpen" :title="editingItem ? '编辑模板文件' : '新增模板文件'" width="560px" @closed="resetItemUpload">
      <el-form label-position="top">
        <el-form-item label="模板文件名称"><el-input v-model="itemForm.itemName" maxlength="128" /></el-form-item>
        <el-form-item label="文件类型">
          <el-tag>{{ fileTypeText(itemForm.fileType) }}</el-tag>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
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
          <div v-if="editingItem?.originalFileName && !itemFile" class="current-file">当前文件：{{ editingItem.originalFileName }}</div>
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
import { apiDelete, apiGet, apiPost, apiPut, http } from '../api/http'

interface Template {
  id: number
  templateName: string
  sortOrder: number
}

interface TemplateItem {
  id: number
  templateId: number
  itemName: string
  fileType: string
  originalFileName?: string
  sortOrder: number
}

const templates = ref<Template[]>([])
const templateItems = ref<TemplateItem[]>([])
const selectedTemplate = ref<Template>()
const editingTemplate = ref<Template>()
const editingItem = ref<TemplateItem>()
const templateDialogOpen = ref(false)
const itemDialogOpen = ref(false)
const templateForm = reactive({ templateName: '', sortOrder: 0 })
const itemForm = reactive({ itemName: '', fileType: 'OTHER', sortOrder: 0 })
const itemFile = ref<UploadRawFile>()
const itemUploadRef = ref<any>()

async function loadTemplates() {
  templates.value = await apiGet<Template[]>('/repair-project-templates')
  if (selectedTemplate.value) {
    selectedTemplate.value = templates.value.find((item) => item.id === selectedTemplate.value?.id)
  }
}

async function selectTemplate(template: Template) {
  selectedTemplate.value = template
  templateItems.value = await apiGet<TemplateItem[]>(`/repair-project-templates/${template.id}/items`)
}

function openTemplateDialog(template?: Template) {
  editingTemplate.value = template
  templateForm.templateName = template?.templateName || ''
  templateForm.sortOrder = template?.sortOrder || 0
  templateDialogOpen.value = true
}

async function submitTemplate() {
  if (!templateForm.templateName.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  const body = { templateName: templateForm.templateName.trim(), sortOrder: templateForm.sortOrder }
  if (editingTemplate.value) {
    await apiPut(`/repair-project-templates/${editingTemplate.value.id}`, body)
  } else {
    await apiPost('/repair-project-templates', body)
  }
  templateDialogOpen.value = false
  ElMessage.success('保存成功')
  await loadTemplates()
}

async function deleteTemplate(template: Template) {
  await ElMessageBox.confirm(`确定删除“${template.templateName}”吗？`, '删除模板', { type: 'warning' })
  await apiDelete(`/repair-project-templates/${template.id}`)
  if (selectedTemplate.value?.id === template.id) {
    selectedTemplate.value = undefined
    templateItems.value = []
  }
  ElMessage.success('删除成功')
  await loadTemplates()
}

function openItemDialog(item?: TemplateItem) {
  if (!selectedTemplate.value) return
  editingItem.value = item
  itemForm.itemName = item?.itemName || ''
  itemForm.fileType = item?.fileType || 'OTHER'
  itemForm.sortOrder = item?.sortOrder || 0
  itemFile.value = undefined
  itemDialogOpen.value = true
}

async function submitItem() {
  if (!selectedTemplate.value) return
  if (!itemForm.itemName.trim()) {
    ElMessage.warning('请输入资料项名称')
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
      await http.put(`/repair-project-templates/${selectedTemplate.value.id}/items/${editingItem.value.id}`, form)
    } else {
      await http.post(`/repair-project-templates/${selectedTemplate.value.id}/items`, form)
    }
  } else if (editingItem.value) {
    const body = { itemName: itemForm.itemName.trim(), fileType: itemForm.fileType, sortOrder: itemForm.sortOrder }
    await apiPut(`/repair-project-templates/${selectedTemplate.value.id}/items/${editingItem.value.id}`, body)
  }
  itemDialogOpen.value = false
  ElMessage.success('保存成功')
  await selectTemplate(selectedTemplate.value)
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
  return 'OTHER'
}

function fileTypeText(type: string) {
  const labels: Record<string, string> = {
    WORD: 'Word',
    EXCEL: 'Excel',
    PPT: 'PPT',
    PDF: 'PDF',
    ZIP: 'ZIP',
    OTHER: '其他'
  }
  return labels[type] || '其他'
}

async function deleteItem(item: TemplateItem) {
  if (!selectedTemplate.value) return
  await ElMessageBox.confirm(`确定删除“${item.itemName}”吗？`, '删除资料项', { type: 'warning' })
  await apiDelete(`/repair-project-templates/${selectedTemplate.value.id}/items/${item.id}`)
  ElMessage.success('删除成功')
  await selectTemplate(selectedTemplate.value)
}

onMounted(loadTemplates)
</script>

<style scoped>
.compact-title {
  margin-bottom: 12px;
}

.template-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
  gap: 14px;
}

.template-main,
.template-side {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 14px;
}

.toolbar,
.side-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.side-header h3 {
  margin: 0;
  font-size: 16px;
}

.current-file {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 13px;
}

@media (max-width: 1000px) {
  .template-layout {
    grid-template-columns: 1fr;
  }
}
</style>
