<template>
  <div>
    <div class="page-title">
      <h2>{{ detail?.dept?.deptName || '组织详情' }}</h2>
      <el-button @click="$router.push('/depts')">返回组织管理</el-button>
    </div>

    <el-alert
      v-if="detail && !detail.assignable"
      :title="detail.assignableMessage"
      type="warning"
      :closable="false"
      show-icon
      class="detail-alert"
    />

    <div class="section">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="组织名称">{{ detail?.dept?.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="组织编码">{{ detail?.dept?.deptCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="层级">{{ isTopLevel(detail?.dept) ? '一级单位' : '二级单位' }}</el-descriptions-item>
        <el-descriptions-item label="配置用户">
          <el-tag :type="detail?.assignable ? 'success' : 'warning'">
            {{ detail?.assignable ? '可配置' : '不直接配置' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="直属用户">{{ detail?.users?.length || 0 }} 人</el-descriptions-item>
        <el-descriptions-item label="组织管理员">{{ detail?.admins?.length || 0 }} 人</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="section">
      <h3>组织管理员</h3>
      <el-table :data="detail?.admins || []" stripe>
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="status" label="状态" width="110" />
      </el-table>
    </div>

    <div class="section">
      <h3>直属用户</h3>
      <el-table :data="detail?.users || []" stripe>
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="identity" label="身份" width="130">
          <template #default="{ row }">
            <el-tag :type="identityTagType(row.identity)">{{ row.identity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="approvalStatus" label="审批状态" width="120" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet } from '../api/http'

const route = useRoute()
const router = useRouter()
const detail = ref<any>()

async function load() {
  try {
    detail.value = await apiGet(`/depts/${route.params.id}/detail`)
  } catch {
    router.push('/depts')
  }
}

function isTopLevel(dept: any) {
  return !Number(dept?.parentId || 0)
}

function identityTagType(identity: string) {
  if (identity === '超级管理员') return 'danger'
  if (identity === '管理员') return 'warning'
  return 'info'
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.detail-alert {
  margin-bottom: 14px;
}

.section h3 {
  margin: 0 0 14px;
  font-size: 16px;
}
</style>
