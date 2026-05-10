<template>
  <div>
    <div class="page-title"><h2>个人空间</h2></div>
    <div class="section">
      <el-tabs v-model="active">
        <el-tab-pane label="我的上传" name="upload">
          <FileLibraryView title="我的上传" mine manage-owner-files :show-upload="false" />
        </el-tab-pane>
        <el-tab-pane label="回收站" name="recycle"><RecycleBinView /></el-tab-pane>
        <el-tab-pane label="我的提醒" name="notice"><NotificationList /></el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import FileLibraryView from './FileLibraryView.vue'
import RecycleBinView from './RecycleBinView.vue'
import NotificationList from '../components/NotificationList.vue'

const active = ref('upload')
</script>
