<template>
  <div>
    <div class="page-title">
      <div>
        <h2>{{ currentSection?.deptName || '科室资料' }}</h2>
        <p class="page-subtitle">按二级侧边栏维护资料入口，车间用户按清单填报并上传附件。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="canManageSection" @click="openCategoryDialog()">新增二级菜单</el-button>
        <el-button v-if="canManageSection && activeCategory" type="primary" @click="openItemDialog()">新增文件</el-button>
      </div>
    </div>

    <div class="doc-workspace">
      <aside class="doc-category-sidebar">
        <div class="doc-category-header">
          <span>二级侧边栏</span>
        </div>
        <button
          v-for="category in categories"
          :key="category.id"
          class="doc-category-item"
          :class="{ active: category.id === activeCategoryId }"
          type="button"
          @click="selectCategory(category.id)"
        >
          <span>{{ category.categoryName }}</span>
          <small>{{ category.status === 'ENABLED' ? '启用' : '停用' }}</small>
        </button>
        <el-empty v-if="categories.length === 0" description="暂无二级菜单" />
      </aside>

      <section class="doc-main-panel">
        <div v-if="activeCategory" class="doc-main-toolbar">
          <div>
            <h3>{{ activeCategory.categoryName }}</h3>
            <span>资料入口</span>
          </div>
          <div class="toolbar-actions">
            <el-button @click="openRecords">上传记录</el-button>
            <el-button v-if="canManageSection" @click="openCategoryDialog(activeCategory)">编辑二级菜单</el-button>
          </div>
        </div>

        <el-table v-if="activeCategory" :data="items" stripe>
          <el-table-column type="index" label="序号" width="70" />
          <el-table-column prop="itemName" label="文件名称" min-width="180" />
          <el-table-column label="收集信息" width="110">
            <template #default="{ row }">{{ row.collectEnabled ? '需要' : '不需要' }}</template>
          </el-table-column>
          <el-table-column label="附件" width="120">
            <template #default="{ row }">
              {{ row.attachmentRequired ? '必传' : row.attachmentEnabled ? '可传' : '不需要' }}
            </template>
          </el-table-column>
          <el-table-column prop="fieldCount" label="字段数" width="90" />
          <el-table-column prop="submissionCount" label="记录数" width="90" />
          <el-table-column prop="status" label="状态" width="90" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button v-if="canManageSection" link type="primary" @click="openItemDialog(row)">编辑文件</el-button>
              <el-button v-if="canManageSection" link type="primary" @click="openFieldDialog(row)">配置清单</el-button>
              <el-button v-if="isWorkshopUser && row.collectEnabled" link type="primary" @click="openSubmitDialog(row)">填写</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="请选择或新增二级菜单" />
      </section>
    </div>

    <el-dialog v-model="categoryDialogOpen" :title="editingCategory ? '编辑二级菜单' : '新增二级菜单'" width="420px">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model="categoryForm.categoryName" maxlength="128" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="categoryForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="categoryForm.status">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDialogOpen" :title="editingItem ? '编辑文件' : '新增文件'" width="520px">
      <el-form label-position="top">
        <el-form-item label="文件名称"><el-input v-model="itemForm.itemName" maxlength="128" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="itemForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="收集设置">
          <el-checkbox v-model="itemForm.collectEnabled">需要收集信息</el-checkbox>
          <el-checkbox v-model="itemForm.attachmentEnabled">允许上传附件</el-checkbox>
          <el-checkbox v-model="itemForm.attachmentRequired">附件必传</el-checkbox>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="itemForm.status">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitItem">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fieldDialogOpen" title="配置收集信息清单" width="760px">
      <div class="field-actions">
        <el-button @click="addField">新增字段</el-button>
      </div>
      <el-table :data="fieldDrafts" border>
        <el-table-column label="字段名称" min-width="180">
          <template #default="{ row }"><el-input v-model="row.fieldName" maxlength="128" /></template>
        </el-table-column>
        <el-table-column label="类型" width="140">
          <template #default="{ row }">
            <el-select v-model="row.fieldType">
              <el-option label="文本" value="TEXT" />
              <el-option label="日期" value="DATE" />
              <el-option label="数字" value="NUMBER" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="必填" width="90">
          <template #default="{ row }"><el-checkbox v-model="row.required" /></template>
        </el-table-column>
        <el-table-column label="排序" width="120">
          <template #default="{ row }"><el-input-number v-model="row.sortOrder" :min="0" /></template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ $index }"><el-button link type="danger" @click="fieldDrafts.splice($index, 1)">删除</el-button></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="fieldDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitFieldConfig">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="submitDialogOpen" :title="submitItemTarget?.itemName || '填写资料'" width="620px" @closed="resetSubmitForm">
      <el-form label-position="top">
        <el-form-item v-for="field in submitFields" :key="field.id" :label="field.fieldName" :required="Boolean(field.required)">
          <el-date-picker
            v-if="field.fieldType === 'DATE'"
            v-model="submitValues[field.id]"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
          <el-input-number v-else-if="field.fieldType === 'NUMBER'" v-model="submitValues[field.id]" style="width: 100%" />
          <el-input v-else v-model="submitValues[field.id]" />
        </el-form-item>
        <el-form-item v-if="submitItemTarget?.attachmentEnabled" :label="submitItemTarget.attachmentRequired ? '附件（必传）' : '附件'">
          <el-upload
            ref="submitUploadRef"
            class="submission-upload"
            drag
            multiple
            :auto-upload="false"
            :on-change="onSubmitFileChange"
            :on-remove="onSubmitFileRemove"
          >
            <div>拖拽附件到此处，或点击选择文件</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="submitting" @click="submitDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRecord">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recordsOpen" title="上传记录" width="980px">
      <el-table :data="records" stripe>
        <el-table-column prop="submittedAt" label="上传时间" width="180" />
        <el-table-column prop="sectionDeptName" label="所属科室" width="120" />
        <el-table-column prop="categoryName" label="二级菜单" width="140" />
        <el-table-column prop="itemName" label="文件名称" min-width="150" />
        <el-table-column prop="workshopDeptName" label="所属车间" width="130" />
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
        <el-descriptions-item label="所属车间">{{ recordDetail.workshopDeptName }}</el-descriptions-item>
        <el-descriptions-item label="上传人">{{ recordDetail.uploadUserName }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="recordDetail" :data="recordDetail.values || []" style="margin-top: 14px" border>
        <el-table-column prop="fieldName" label="字段" width="180" />
        <el-table-column prop="fieldValue" label="内容" />
      </el-table>
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
import { ElMessage, type UploadFile, type UploadFiles, type UploadRawFile } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { apiDelete, apiGet, apiPost, apiPut, http } from '../api/http'
import { useAuthStore } from '../stores/auth'

interface SectionItem {
  id: number
  deptName: string
}

interface DocCategory {
  id: number
  sectionDeptId: number
  categoryName: string
  sortOrder: number
  status: string
}

interface DocItem {
  id: number
  categoryId: number
  itemName: string
  collectEnabled: number
  attachmentEnabled: number
  attachmentRequired: number
  sortOrder: number
  status: string
  fieldCount?: number
  submissionCount?: number
}

interface DocField {
  id: number
  itemId: number
  fieldName: string
  fieldType: 'TEXT' | 'DATE' | 'NUMBER'
  required: number
  sortOrder: number
}

interface DocSubmission {
  id: number
  itemName: string
  categoryName: string
  sectionDeptName: string
  workshopDeptName: string
  uploadUserName: string
  submittedAt: string
  attachmentCount: number
  values?: Array<{ fieldName: string; fieldValue: string }>
  attachments?: Array<{ id: number; originalFileName: string }>
}

interface FieldDraft {
  id?: number
  fieldName: string
  fieldType: 'TEXT' | 'DATE' | 'NUMBER'
  required: boolean
  sortOrder: number
}

const route = useRoute()
const auth = useAuthStore()
const deptId = computed(() => Number(route.params.deptId))
const sections = ref<SectionItem[]>([])
const categories = ref<DocCategory[]>([])
const items = ref<DocItem[]>([])
const activeCategoryId = ref<number>()
const categoryDialogOpen = ref(false)
const itemDialogOpen = ref(false)
const fieldDialogOpen = ref(false)
const submitDialogOpen = ref(false)
const recordsOpen = ref(false)
const recordDetailOpen = ref(false)
const editingCategory = ref<DocCategory>()
const editingItem = ref<DocItem>()
const fieldItemTarget = ref<DocItem>()
const submitItemTarget = ref<DocItem>()
const records = ref<DocSubmission[]>([])
const recordDetail = ref<DocSubmission>()
const fieldDrafts = ref<FieldDraft[]>([])
const submitFields = ref<DocField[]>([])
const submitValues = reactive<Record<number, string | number | undefined>>({})
const submitFiles = ref<UploadRawFile[]>([])
const submitting = ref(false)
const submitUploadRef = ref<any>()
const categoryForm = reactive({ categoryName: '', sortOrder: 0, status: 'ENABLED' })
const itemForm = reactive({ itemName: '', sortOrder: 0, status: 'ENABLED', collectEnabled: true, attachmentEnabled: false, attachmentRequired: false })

const currentSection = computed(() => sections.value.find((item) => item.id === deptId.value))
const activeCategory = computed(() => categories.value.find((item) => item.id === activeCategoryId.value))
const canManageSection = computed(() => Boolean(auth.user?.isSuperAdmin) || auth.user?.deptId === deptId.value)
const isWorkshopUser = computed(() => Boolean(auth.user?.deptId) && !auth.user?.isSuperAdmin && !sections.value.some((item) => item.id === auth.user?.deptId))

async function load() {
  sections.value = await apiGet<SectionItem[]>('/sections/navigation')
  categories.value = await apiGet<DocCategory[]>('/doc-categories', { sectionDeptId: deptId.value })
  if (!categories.value.some((item) => item.id === activeCategoryId.value)) {
    activeCategoryId.value = categories.value[0]?.id
  }
  await loadItems()
}

async function loadItems() {
  items.value = activeCategoryId.value ? await apiGet<DocItem[]>('/doc-items', { categoryId: activeCategoryId.value }) : []
}

function selectCategory(id: number) {
  activeCategoryId.value = id
  loadItems()
}

function openCategoryDialog(category?: DocCategory) {
  editingCategory.value = category
  categoryForm.categoryName = category?.categoryName || ''
  categoryForm.sortOrder = category?.sortOrder || 0
  categoryForm.status = category?.status || 'ENABLED'
  categoryDialogOpen.value = true
}

async function submitCategory() {
  if (!categoryForm.categoryName.trim()) {
    ElMessage.warning('请输入二级菜单名称')
    return
  }
  const body = { ...categoryForm, sectionDeptId: deptId.value, categoryName: categoryForm.categoryName.trim() }
  if (editingCategory.value) {
    await apiPut(`/doc-categories/${editingCategory.value.id}`, body)
    ElMessage.success('修改成功')
  } else {
    await apiPost('/doc-categories', body)
    ElMessage.success('新增成功')
  }
  categoryDialogOpen.value = false
  await load()
}

function openItemDialog(item?: DocItem) {
  editingItem.value = item
  itemForm.itemName = item?.itemName || ''
  itemForm.sortOrder = item?.sortOrder || 0
  itemForm.status = item?.status || 'ENABLED'
  itemForm.collectEnabled = item ? Boolean(item.collectEnabled) : true
  itemForm.attachmentEnabled = item ? Boolean(item.attachmentEnabled) : false
  itemForm.attachmentRequired = item ? Boolean(item.attachmentRequired) : false
  itemDialogOpen.value = true
}

async function submitItem() {
  if (!activeCategoryId.value || !itemForm.itemName.trim()) {
    ElMessage.warning('请输入文件名称')
    return
  }
  const body = {
    categoryId: activeCategoryId.value,
    itemName: itemForm.itemName.trim(),
    sortOrder: itemForm.sortOrder,
    status: itemForm.status,
    collectEnabled: itemForm.collectEnabled ? 1 : 0,
    attachmentEnabled: itemForm.attachmentEnabled || itemForm.attachmentRequired ? 1 : 0,
    attachmentRequired: itemForm.attachmentRequired ? 1 : 0
  }
  if (editingItem.value) {
    await apiPut(`/doc-items/${editingItem.value.id}`, body)
    ElMessage.success('修改成功')
  } else {
    await apiPost('/doc-items', body)
    ElMessage.success('新增成功')
  }
  itemDialogOpen.value = false
  await loadItems()
}

async function openFieldDialog(item: DocItem) {
  fieldItemTarget.value = item
  const fields = await apiGet<DocField[]>(`/doc-items/${item.id}/fields`)
  fieldDrafts.value = fields.map((field) => ({
    id: field.id,
    fieldName: field.fieldName,
    fieldType: field.fieldType,
    required: Boolean(field.required),
    sortOrder: field.sortOrder || 0
  }))
  fieldDialogOpen.value = true
}

function addField() {
  fieldDrafts.value.push({ fieldName: '', fieldType: 'TEXT', required: false, sortOrder: fieldDrafts.value.length })
}

async function submitFieldConfig() {
  if (!fieldItemTarget.value) {
    return
  }
  if (fieldDrafts.value.some((field) => !field.fieldName.trim())) {
    ElMessage.warning('字段名称不能为空')
    return
  }
  await apiPut(`/doc-items/${fieldItemTarget.value.id}/fields`, fieldDrafts.value.map((field) => ({
    fieldName: field.fieldName.trim(),
    fieldType: field.fieldType,
    required: field.required ? 1 : 0,
    sortOrder: field.sortOrder || 0
  })))
  ElMessage.success('清单已保存')
  fieldDialogOpen.value = false
  await loadItems()
}

async function openSubmitDialog(item: DocItem) {
  submitItemTarget.value = item
  submitFields.value = await apiGet<DocField[]>(`/doc-items/${item.id}/fields`)
  submitDialogOpen.value = true
}

function onSubmitFileChange(_file: UploadFile, uploadFiles: UploadFiles) {
  syncSubmitFiles(uploadFiles)
}

function onSubmitFileRemove(_file: UploadFile, uploadFiles: UploadFiles) {
  syncSubmitFiles(uploadFiles)
}

function syncSubmitFiles(uploadFiles: UploadFiles) {
  submitFiles.value = uploadFiles.map((item) => item.raw).filter((file): file is UploadRawFile => Boolean(file))
}

async function submitRecord() {
  if (!submitItemTarget.value) {
    return
  }
  submitting.value = true
  try {
    const form = new FormData()
    const values: Record<string, string> = {}
    submitFields.value.forEach((field) => {
      const value = submitValues[field.id]
      if (value !== undefined && value !== null && String(value).trim() !== '') {
        values[String(field.id)] = String(value)
      }
    })
    form.append('valuesJson', JSON.stringify(values))
    submitFiles.value.forEach((file) => form.append('files', file))
    await http.post(`/doc-items/${submitItemTarget.value.id}/submissions`, form)
    ElMessage.success('提交成功')
    submitDialogOpen.value = false
    await loadItems()
  } finally {
    submitting.value = false
  }
}

function resetSubmitForm() {
  Object.keys(submitValues).forEach((key) => delete submitValues[Number(key)])
  submitFiles.value = []
  submitUploadRef.value?.clearFiles()
}

async function openRecords() {
  if (!activeCategoryId.value) {
    return
  }
  records.value = await apiGet<DocSubmission[]>(`/doc-categories/${activeCategoryId.value}/submissions`)
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
watch(() => route.params.deptId, () => {
  activeCategoryId.value = undefined
  load()
})
</script>

<style scoped>
.page-subtitle {
  margin: 8px 0 0;
  color: #637083;
}

.page-actions,
.toolbar-actions,
.field-actions {
  display: flex;
  gap: 10px;
}

.doc-workspace {
  display: grid;
  grid-template-columns: 220px 1fr;
  min-height: 560px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 6px;
}

.doc-category-sidebar {
  border-right: 1px solid var(--line);
  padding: 12px;
}

.doc-category-header {
  height: 34px;
  display: flex;
  align-items: center;
  font-weight: 700;
  color: #1f2d3d;
}

.doc-category-item {
  width: 100%;
  min-height: 42px;
  padding: 8px 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #1f2d3d;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  text-align: left;
}

.doc-category-item:hover,
.doc-category-item.active {
  background: #eef5ff;
  color: var(--rail-blue-dark);
}

.doc-category-item small {
  color: #8a96a8;
}

.doc-main-panel {
  padding: 14px;
  min-width: 0;
}

.doc-main-toolbar {
  min-height: 46px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.doc-main-toolbar h3 {
  margin: 0 0 4px;
  font-size: 18px;
}

.doc-main-toolbar span {
  color: #637083;
}

.field-actions {
  justify-content: flex-end;
  margin-bottom: 12px;
}

.submission-upload,
.submission-upload :deep(.el-upload),
.submission-upload :deep(.el-upload-dragger) {
  width: 100%;
}
</style>
