<template>
  <div class="section">
    <el-empty description="正在打开组织资料页" />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet } from '../api/http'

interface DeptNavigationItem {
  id: number
  children: DeptNavigationItem[]
}

const router = useRouter()

onMounted(async () => {
  const navigation = await apiGet<DeptNavigationItem[]>('/sections/navigation')
  const entry = navigation[0]
  if (entry) {
    router.replace(`/org/${entry.id}`)
  }
})
</script>
