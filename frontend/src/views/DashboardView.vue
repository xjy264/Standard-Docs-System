<template>
  <div class="dashboard-page">
    <div class="page-title">
      <div>
        <h2>首页</h2>
        <p class="page-subtitle">资料、用户和填报工作的运行总览</p>
      </div>
    </div>

    <div class="dashboard-kpis">
      <div v-for="item in coreStats" :key="item.label" class="dashboard-kpi">
        <div class="kpi-label">{{ item.label }}</div>
        <div class="kpi-value">{{ item.value }}</div>
      </div>
    </div>

    <div class="dashboard-grid">
      <section class="section dashboard-section">
        <div class="section-heading">
          <h3>科室组织树</h3>
        </div>
        <el-tree
          v-if="sectionFileTree.length"
          class="section-file-tree"
          :data="sectionFileTree"
          node-key="id"
          default-expand-all
          :props="{ children: 'children', label: 'deptName' }"
          @node-click="openDept"
        >
          <template #default="{ data }">
            <div class="section-tree-row">
              <span>{{ data.deptName }}</span>
              <el-tag size="small" type="info">{{ data.fileCount || 0 }} 个文件</el-tag>
            </div>
          </template>
        </el-tree>
        <el-empty v-else description="暂无科室数据" />
      </section>

      <section class="section dashboard-section">
        <div class="section-heading">
          <h3>工作状态</h3>
        </div>
        <div class="status-list">
          <div v-for="item in workStats" :key="item.label" class="status-row">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </section>

      <section class="section dashboard-section">
        <div class="section-heading">
          <h3>快捷入口</h3>
        </div>
        <div class="quick-actions">
          <el-button type="primary" plain @click="router.push('/org')">资料目录</el-button>
          <el-button plain @click="router.push('/console/personal')">个人提醒</el-button>
          <el-button v-if="auth.hasPermission('user:view')" plain @click="router.push('/console/users')">用户管理</el-button>
          <el-button v-if="auth.hasPermission('user:approve')" plain @click="router.push('/approvals')">注册审核</el-button>
        </div>
      </section>
    </div>

    <section class="section dashboard-section">
      <div class="section-heading">
        <h3>业务流程</h3>
      </div>
      <el-steps :active="5" finish-status="success">
        <el-step title="注册审批" />
        <el-step title="目录维护" />
        <el-step title="文件详情" />
        <el-step title="附件填报" />
        <el-step title="记录查看" />
      </el-steps>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet } from '../api/http'
import { useAuthStore } from '../stores/auth'

interface DashboardStats {
  userCount: number
  fileCount: number
  submissionCount: number
  attachmentCount: number
  pendingApprovalCount: number
  unreadCount: number
  uploadEnabledFileCount: number
  sectionCount: number
}

interface SectionFileTreeItem {
  id: number
  parentId?: number
  deptName: string
  deptType?: string
  fileCount: number
  children: SectionFileTreeItem[]
}

const router = useRouter()
const auth = useAuthStore()
const stats = ref<Partial<DashboardStats>>({})
const sectionFileTree = ref<SectionFileTreeItem[]>([])

const coreStats = computed(() => [
  { label: '用户总数', value: statValue('userCount') },
  { label: '资料文件数', value: statValue('fileCount') },
  { label: '上传记录数', value: statValue('submissionCount') },
  { label: '附件总数', value: statValue('attachmentCount') }
])

const workStats = computed(() => [
  { label: '待审批注册', value: statValue('pendingApprovalCount') },
  { label: '未读提醒', value: statValue('unreadCount') },
  { label: '开启附件上传资料', value: statValue('uploadEnabledFileCount') },
  { label: '科室数量', value: statValue('sectionCount') }
])

function statValue(key: keyof DashboardStats) {
  return stats.value[key] || 0
}

function openDept(node: SectionFileTreeItem) {
  if (node.deptType === 'SECTION') {
    router.push(`/org/${node.id}`)
  }
}

onMounted(async () => {
  const [statsData, treeData] = await Promise.all([
    apiGet<DashboardStats>('/dashboard/stats'),
    apiGet<SectionFileTreeItem[]>('/dashboard/section-file-tree')
  ])
  stats.value = statsData
  sectionFileTree.value = treeData
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #637083;
  font-size: 13px;
}

.dashboard-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.dashboard-kpi {
  min-height: 104px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid var(--line);
  border-left: 4px solid var(--rail-blue);
  border-radius: 6px;
}

.kpi-label {
  color: #637083;
  font-size: 13px;
}

.kpi-value {
  margin-top: 12px;
  color: var(--rail-blue-dark);
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(320px, 1.2fr) minmax(0, 1fr) minmax(280px, 0.8fr);
  gap: 14px;
}

.dashboard-section {
  margin-bottom: 0;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-heading h3 {
  margin: 0;
  color: #1f2d3d;
  font-size: 16px;
}

.status-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 18px;
}

.status-row {
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #edf1f6;
  color: #637083;
}

.status-row strong {
  color: var(--rail-blue-dark);
  font-size: 18px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.section-file-tree {
  --el-tree-node-hover-bg-color: #f3f7fc;
}

.section-tree-row {
  width: 100%;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-right: 8px;
}

.section-tree-row span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .dashboard-kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid,
  .status-list {
    grid-template-columns: 1fr;
  }
}
</style>
