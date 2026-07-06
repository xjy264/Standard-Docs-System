<template>
  <div class="dashboard-entry-page">
    <div class="entry-heading">
      <h2>标准化资料管理系统</h2>
      <p>请选择要进入的资料模块</p>
    </div>

    <div class="module-card-grid">
      <button class="module-card" type="button" @click="openModule('internal')">
        <span class="module-card-title">内业资料</span>
        <span class="module-card-desc">车间上传信息、科室查看汇总资料</span>
      </button>
      <button class="module-card" type="button" @click="openModule('rules')">
        <span class="module-card-title">规章制度</span>
        <span class="module-card-desc">制度文件展示、预览和下载</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet } from '../api/http'

interface DeptNavigationItem {
  id: number
  deptName: string
  children: DeptNavigationItem[]
}

const router = useRouter()
const navigation = ref<DeptNavigationItem[]>([])

async function loadNavigation() {
  try {
    navigation.value = await apiGet<DeptNavigationItem[]>('/sections/navigation')
  } catch {
    navigation.value = []
  }
}

function openModule(moduleBase: 'internal' | 'rules') {
  const entry = navigation.value[0]
  if (!entry) {
    ElMessage.warning('暂无可进入科室')
    return
  }
  router.push(`/${moduleBase}/${entry.id}`)
}

onMounted(loadNavigation)
</script>

<style scoped>
.dashboard-entry-page {
  min-height: calc(100vh - 108px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 34px;
  padding: 24px;
}

.entry-heading {
  text-align: center;
}

.entry-heading h2 {
  margin: 0;
  color: var(--rail-blue-dark);
  font-size: 30px;
}

.entry-heading p {
  margin: 12px 0 0;
  color: #637083;
  font-size: 15px;
}

.module-card-grid {
  width: min(860px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 26px;
}

.module-card {
  min-height: 190px;
  border: 1px solid var(--line);
  border-top: 5px solid var(--rail-blue);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 16px 32px rgb(0 63 125 / 10%);
  color: #1f2d3d;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  transition: border-color .15s ease, box-shadow .15s ease, transform .15s ease;
}

.module-card:hover {
  border-color: var(--rail-blue);
  box-shadow: 0 18px 38px rgb(0 63 125 / 16%);
  transform: translateY(-2px);
}

.module-card-title {
  color: var(--rail-blue-dark);
  font-size: 28px;
  font-weight: 700;
}

.module-card-desc {
  color: #637083;
  font-size: 14px;
}

@media (max-width: 720px) {
  .module-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
