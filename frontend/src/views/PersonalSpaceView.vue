<template>
  <div>
    <div class="page-title"><h2>个人空间</h2></div>
    <div class="section">
      <el-tabs v-model="active">
        <el-tab-pane label="我的提醒" name="notice"><NotificationList /></el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import NotificationList from '../components/NotificationList.vue'

const active = ref('notice')
</script>
