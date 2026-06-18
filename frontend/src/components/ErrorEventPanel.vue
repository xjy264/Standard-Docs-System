<template>
  <div class="error-event-panel">
    <div class="query-bar compact-query">
      <el-form :inline="true" :model="query">
        <el-form-item label="来源">
          <el-select v-model="query.source" clearable placeholder="全部" style="width: 130px">
            <el-option label="后端" value="BACKEND" />
            <el-option label="前端" value="FRONTEND" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="query.severity" clearable placeholder="全部" style="width: 130px">
            <el-option label="错误" value="ERROR" />
            <el-option label="警告" value="WARN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.resolved" clearable placeholder="全部" style="width: 130px">
            <el-option label="未处理" :value="false" />
            <el-option label="已处理" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="错误编号 / 接口 / 消息" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" @click="exportErrors">一键导出</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="error-stat-grid" v-if="stats.length">
      <div v-for="item in stats" :key="item.fingerprint" class="error-stat-item">
        <strong>{{ item.count }}</strong>
        <span>{{ item.source }} / {{ item.severity }}</span>
        <small>{{ item.message }}</small>
      </div>
    </div>

    <el-table :data="rows" stripe>
      <el-table-column prop="errorId" label="错误编号" width="190" />
      <el-table-column prop="source" label="来源" width="90" />
      <el-table-column prop="severity" label="级别" width="90" />
      <el-table-column prop="message" label="摘要" min-width="260" show-overflow-tooltip />
      <el-table-column prop="requestUri" label="接口" min-width="180" show-overflow-tooltip />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.resolved ? 'success' : 'danger'">{{ row.resolved ? '已处理' : '未处理' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="!row.resolved" link type="success" @click="resolve(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        layout="prev, pager, next, total"
        :total="total"
        :page-size="query.size"
        v-model:current-page="query.page"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="detailVisible" title="错误详情" width="760px">
      <div v-if="detail" class="error-detail">
        <p><strong>错误编号：</strong>{{ detail.errorId }}</p>
        <p><strong>追踪编号：</strong>{{ detail.traceId || '-' }}</p>
        <p><strong>请求地址：</strong>{{ detail.requestUri || detail.frontendRoute || '-' }}</p>
        <p><strong>用户：</strong>{{ detail.userId || '-' }} / {{ detail.deptId || '-' }}</p>
        <p><strong>浏览器：</strong>{{ detail.browserInfo || detail.userAgent || '-' }}</p>
        <pre>{{ detail.stackTrace || detail.frontendStack || detail.message }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost, http } from '../api/http'
import { formatDateTime } from '../utils/dateTime'

const rows = ref<any[]>([])
const stats = ref<any[]>([])
const total = ref(0)
const detail = ref<any | null>(null)
const detailVisible = ref(false)
const query = reactive({
  source: '',
  severity: '',
  resolved: undefined as boolean | undefined,
  keyword: '',
  page: 1,
  size: 20
})

async function load() {
  const result = await apiGet('/error-events', {
    source: query.source || undefined,
    severity: query.severity || undefined,
    resolved: query.resolved,
    keyword: query.keyword || undefined,
    page: query.page,
    size: query.size
  })
  rows.value = result.rows || []
  stats.value = result.stats || []
  total.value = result.total || 0
}

function reset() {
  query.source = ''
  query.severity = ''
  query.resolved = undefined
  query.keyword = ''
  query.page = 1
  load()
}

async function openDetail(row: any) {
  detail.value = await apiGet(`/error-events/${row.id}`)
  detailVisible.value = true
}

async function resolve(row: any) {
  const { value } = await ElMessageBox.prompt('请输入处理备注', '标记已处理', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：已修复并发布'
  })
  await apiPost(`/error-events/${row.id}/resolve`, { remark: value })
  ElMessage.success('已标记处理')
  load()
}

async function exportErrors() {
  const response = await http.get('/error-events/export', {
    params: {
      source: query.source || undefined,
      severity: query.severity || undefined,
      resolved: query.resolved,
      days: 7
    },
    responseType: 'blob'
  })
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = 'standard-docs-error-events.zip'
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(load)
</script>

<style scoped>
.compact-query {
  margin-bottom: 12px;
}

.error-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.error-stat-item {
  min-height: 86px;
  padding: 12px;
  border: 1px solid var(--line);
  border-left: 4px solid var(--rail-red);
  border-radius: 6px;
  background: #fff;
}

.error-stat-item strong {
  display: block;
  color: var(--rail-red);
  font-size: 24px;
}

.error-stat-item span,
.error-stat-item small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.error-detail pre {
  max-height: 360px;
  overflow: auto;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #f7f9fc;
  white-space: pre-wrap;
}
</style>
