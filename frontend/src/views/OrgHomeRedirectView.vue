<template>
  <div class="section">
    <el-empty description="正在打开资料页" />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet } from '../api/http'

interface DeptNavigationItem {
  id: number
  children: DeptNavigationItem[]
}

const route = useRoute()
const router = useRouter()
const moduleBase = route.meta.moduleBase === 'rules' ? 'rules' : 'internal'

onMounted(async () => {
  const navigation = await apiGet<DeptNavigationItem[]>('/sections/navigation')
  const entry = navigation[0]
  if (entry) {
    router.replace(`/${moduleBase}/${entry.id}`)
  }
})
</script>
