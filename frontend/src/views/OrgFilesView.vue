<template>
  <div class="section-workspace">
    <div class="page-title compact-title">
      <div>
        <h2>{{ currentSection?.deptName || '科室资料' }}</h2>
      </div>
    </div>

    <div class="doc-workspace">
      <aside class="doc-category-sidebar">
        <div class="doc-category-header">二级菜单</div>
        <button
          v-for="category in categories"
          :key="category.id"
          class="doc-category-item"
          :class="{ active: category.id === activeCategoryId }"
          type="button"
          @click="selectCategory(category.id)"
        >
          <span>{{ category.categoryName }}</span>
        </button>
        <el-empty v-if="categories.length === 0" description="暂无二级菜单" />
      </aside>

      <section class="doc-main-panel">
        <div v-if="activeCategory" class="doc-main-toolbar">
          <div>
            <h3>{{ activeCategory.categoryName }}</h3>
          </div>
          <el-button v-if="canManageSection" type="primary" @click="openItemDialog()">新增文件</el-button>
        </div>

        <el-table v-if="activeCategory" :data="items" stripe class="file-table">
          <el-table-column type="index" label="序号" width="70" />
          <el-table-column label="文件名称" min-width="220">
            <template #default="{ row }">
              <el-button link type="primary" class="file-name-link" @click="openItemDetail(row)">
                {{ row.itemName }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="允许上传附件" width="140">
            <template #default="{ row }">{{ row.attachmentEnabled ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="submissionCount" label="上传记录数" width="120" />
          <el-table-column prop="sortOrder" label="排序" width="90" />
          <el-table-column prop="updatedAt" label="更新时间" width="180" />
          <el-table-column v-if="canManageSection" label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openItemDialog(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无二级菜单" />
      </section>
    </div>

    <el-dialog v-model="itemDialogOpen" :title="editingItem ? '编辑文件' : '新增文件'" width="920px" @closed="destroyEditor">
      <el-form label-position="top">
        <el-form-item label="文件名称"><el-input v-model="itemForm.itemName" maxlength="128" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="itemForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="文件内容">
          <div class="editor-box">
            <Toolbar :editor="editorRef" :default-config="toolbarConfig" mode="default" />
            <Editor
              v-model="itemForm.contentHtml"
              :default-config="editorConfig"
              mode="default"
              class="content-editor"
              @on-created="handleEditorCreated"
            />
          </div>
        </el-form-item>
        <el-form-item label="收集设置">
          <el-checkbox v-model="itemForm.attachmentEnabled">允许上传附件</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, reactive, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet, apiPost, apiPut } from '../api/http'
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
}

interface DocItem {
  id: number
  categoryId: number
  itemName: string
  contentHtml?: string
  attachmentEnabled: number
  sortOrder: number
  updatedAt?: string
  submissionCount?: number
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const deptId = computed(() => Number(route.params.deptId))
const sections = ref<SectionItem[]>([])
const categories = ref<DocCategory[]>([])
const items = ref<DocItem[]>([])
const activeCategoryId = ref<number>()
const itemDialogOpen = ref(false)
const editingItem = ref<DocItem>()
const editorRef = shallowRef<IDomEditor>()
const toolbarConfig: Partial<IToolbarConfig> = {}
const editorConfig: Partial<IEditorConfig> = { placeholder: '请输入文件内容' }
const itemForm = reactive({ itemName: '', sortOrder: 0, attachmentEnabled: false, contentHtml: '' })

const currentSection = computed(() => sections.value.find((item) => item.id === deptId.value))
const activeCategory = computed(() => categories.value.find((item) => item.id === activeCategoryId.value))
const canManageSection = computed(() => Boolean(auth.user?.isSuperAdmin) || auth.user?.deptId === deptId.value)

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

function openItemDialog(item?: DocItem) {
  editingItem.value = item
  itemForm.itemName = item?.itemName || ''
  itemForm.sortOrder = item?.sortOrder || 0
  itemForm.attachmentEnabled = item ? Boolean(item.attachmentEnabled) : false
  itemForm.contentHtml = item?.contentHtml || ''
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
    contentHtml: itemForm.contentHtml,
    attachmentEnabled: itemForm.attachmentEnabled ? 1 : 0
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

function openItemDetail(item: DocItem) {
  router.push(`/org/${deptId.value}/items/${item.id}`)
}

function handleEditorCreated(editor: IDomEditor) {
  editorRef.value = editor
}

function destroyEditor() {
  editorRef.value?.destroy()
  editorRef.value = undefined
}

onMounted(load)
onBeforeUnmount(destroyEditor)
watch(() => route.params.deptId, () => {
  activeCategoryId.value = undefined
  load()
})
</script>

<style scoped>
.section-workspace {
  height: 100%;
}

.compact-title {
  margin-bottom: 12px;
}

.doc-workspace {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  min-height: calc(100vh - 150px);
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
  text-align: left;
}

.doc-category-item:hover,
.doc-category-item.active {
  background: #eef5ff;
  color: var(--rail-blue-dark);
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
  margin: 0;
  font-size: 18px;
}

.file-name-link {
  padding: 0;
  font-weight: 600;
}

.editor-box {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 6px;
  overflow: hidden;
}

.content-editor {
  height: 320px;
  overflow-y: hidden;
}
</style>
