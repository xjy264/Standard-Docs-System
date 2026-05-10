<template>
  <div>
    <div class="page-title"><h2>角色模板</h2><el-button type="primary" @click="openCreate">新增角色模板</el-button></div>
    <div class="section">
      <el-table :data="visibleRows" stripe>
        <el-table-column label="角色名称">
          <template #default="{ row }">{{ roleDisplayName(row) }}</template>
        </el-table-column>
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="description" label="说明" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }"><el-button link type="primary" @click="edit(row)">编辑</el-button><el-button link type="warning" @click="config(row)">配置权限</el-button></template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="open" title="角色模板" width="500px">
      <el-form label-position="top">
        <el-form-item label="角色名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="form.roleCode" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="open=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="permOpen" title="配置系统级权限" width="720px">
      <el-checkbox-group v-model="selectedPermissions">
        <el-checkbox v-for="item in permissions" :key="item.permissionCode" :label="item.permissionCode">{{ item.permissionName }}（{{ item.permissionCode }}）</el-checkbox>
      </el-checkbox-group>
      <template #footer><el-button @click="permOpen=false">取消</el-button><el-button type="primary" @click="savePermissions">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { apiGet, apiPost, apiPut } from '../api/http'

const rows = ref<any[]>([])
const visibleRoleCodes = new Set(['SUPER_ADMIN', 'SEGMENT_ADMIN', 'STAFF'])
const visibleRows = computed(() => rows.value.filter((item) => visibleRoleCodes.has(item.roleCode)))
const permissions = ref<any[]>([])
const selectedRole = ref<any>()
const selectedPermissions = ref<string[]>([])
const open = ref(false)
const permOpen = ref(false)
const form = reactive<any>({})

function roleDisplayName(row: any) {
  if (row.roleCode === 'SUPER_ADMIN') return '超级管理员'
  if (row.roleCode === 'SEGMENT_ADMIN') return '管理员'
  if (row.roleCode === 'STAFF') return '普通用户'
  return row.roleName
}

async function load() {
  rows.value = await apiGet('/roles')
  permissions.value = await apiGet('/roles/permissions')
}

function openCreate() {
  Object.assign(form, { id: null, roleName: '', roleCode: '', description: '' })
  open.value = true
}

function edit(row: any) {
  Object.assign(form, row)
  open.value = true
}

function config(row: any) {
  selectedRole.value = row
  selectedPermissions.value = []
  permOpen.value = true
}

async function save() {
  if (form.id) await apiPut(`/roles/${form.id}`, form)
  else await apiPost('/roles', form)
  ElMessage.success('保存成功')
  open.value = false
  load()
}

async function savePermissions() {
  await apiPost(`/roles/${selectedRole.value.id}/permissions`, selectedPermissions.value)
  ElMessage.success('权限已保存')
  permOpen.value = false
}

onMounted(load)
</script>
