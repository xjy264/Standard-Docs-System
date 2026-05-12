<template>
  <div>
    <div class="page-title"><h2>操作日志</h2></div>
    <div class="section">
      <el-tabs v-model="active">
        <el-tab-pane label="操作日志" name="operations">
          <el-table :data="operations" stripe>
            <el-table-column prop="operatorId" label="操作人" width="100" />
            <el-table-column prop="operationType" label="操作类型" width="160" />
            <el-table-column prop="objectType" label="对象类型" width="120" />
            <el-table-column prop="result" label="结果" width="100" />
            <el-table-column prop="createdAt" label="操作时间" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="登录日志" name="logins">
          <el-table :data="logins" stripe>
            <el-table-column prop="username" label="手机号" />
            <el-table-column prop="result" label="结果" width="100" />
            <el-table-column prop="failReason" label="失败原因" />
            <el-table-column prop="createdAt" label="时间" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiGet } from '../api/http'

const active = ref('operations')
const operations = ref<any[]>([])
const logins = ref<any[]>([])

onMounted(async () => {
  operations.value = await apiGet('/logs/operations')
  logins.value = await apiGet('/logs/logins')
})
</script>
