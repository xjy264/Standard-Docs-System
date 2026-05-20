<template>
  <div>
    <div class="page-title">
      <div>
        <h2>二级菜单设置</h2>
      </div>
      <div class="page-actions">
        <el-select v-if="auth.user?.isSuperAdmin" v-model="selectedSectionId" placeholder="选择科室" style="width: 220px" @change="loadCategories">
          <el-option v-for="section in sections" :key="section.id" :label="section.deptName" :value="section.id" />
        </el-select>
        <el-button type="primary" :disabled="!selectedSectionId" @click="openDialog()">新增二级菜单</el-button>
      </div>
    </div>

    <el-table :data="categories" stripe>
      <el-table-column type="index" label="序号" width="70" />
      <el-table-column prop="categoryName" label="二级菜单名称" min-width="220" />
      <el-table-column prop="sortOrder" label="排序" width="120" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="removeCategory(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogOpen" :title="editingCategory ? '编辑二级菜单' : '新增二级菜单'" width="420px">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model="form.categoryName" maxlength="128" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { apiDelete, apiGet, apiPost, apiPut } from '../api/http'
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
  updatedAt?: string
}

const auth = useAuthStore()
const sections = ref<SectionItem[]>([])
const categories = ref<DocCategory[]>([])
const selectedSectionId = ref<number>()
const dialogOpen = ref(false)
const editingCategory = ref<DocCategory>()
const form = reactive({ categoryName: '', sortOrder: 0 })
const isSectionUser = computed(() => Boolean(auth.user?.deptId) && sections.value.some((section) => section.id === auth.user?.deptId))

async function loadSections() {
  sections.value = await apiGet<SectionItem[]>('/sections/navigation')
  if (auth.user?.isSuperAdmin) {
    selectedSectionId.value = selectedSectionId.value || sections.value[0]?.id
  } else if (isSectionUser.value) {
    selectedSectionId.value = auth.user?.deptId
  }
}

async function loadCategories() {
  categories.value = selectedSectionId.value ? await apiGet<DocCategory[]>('/doc-categories', { sectionDeptId: selectedSectionId.value }) : []
}

function openDialog(category?: DocCategory) {
  editingCategory.value = category
  form.categoryName = category?.categoryName || ''
  form.sortOrder = category?.sortOrder || 0
  dialogOpen.value = true
}

async function submitCategory() {
  if (!selectedSectionId.value || !form.categoryName.trim()) {
    ElMessage.warning('请输入二级菜单名称')
    return
  }
  const body = {
    sectionDeptId: selectedSectionId.value,
    categoryName: form.categoryName.trim(),
    sortOrder: form.sortOrder
  }
  if (editingCategory.value) {
    await apiPut(`/doc-categories/${editingCategory.value.id}`, body)
    ElMessage.success('修改成功')
  } else {
    await apiPost('/doc-categories', body)
    ElMessage.success('新增成功')
  }
  dialogOpen.value = false
  await loadCategories()
}

async function removeCategory(category: DocCategory) {
  await ElMessageBox.confirm(`确定删除“${category.categoryName}”吗？`, '删除二级菜单', { type: 'warning' })
  await apiDelete(`/doc-categories/${category.id}`)
  ElMessage.success('删除成功')
  await loadCategories()
}

onMounted(async () => {
  await loadSections()
  await loadCategories()
})
</script>

<style scoped>
.page-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
