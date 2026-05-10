<template>
  <div>
    <div class="page-title"><h2>系统设置</h2></div>
    <div class="section">
      <el-alert title="文件查看统一通过下载完成，文件所有者可在个人空间替换或删除文件。第一阶段重点保证真实文件上传、下载、搜索和权限隔离。" type="info" show-icon />
      <el-table :data="configs" stripe style="margin-top:14px">
        <el-table-column prop="configKey" label="配置项" width="240" />
        <el-table-column prop="configValue" label="配置值" />
        <el-table-column prop="description" label="说明" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiGet } from '../api/http'

const configs = ref<any[]>([])

onMounted(async () => {
  configs.value = await apiGet('/system-config')
})
</script>
