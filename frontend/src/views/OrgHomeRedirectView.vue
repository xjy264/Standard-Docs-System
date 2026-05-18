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

function firstEntry(items: DeptNavigationItem[]): DeptNavigationItem | undefined {
  for (const item of items) {
    if (item.children?.length) {
      const child = firstEntry(item.children)
      if (child) {
        return child
      }
    } else {
      return item
    }
  }
  return undefined
}

onMounted(async () => {
  const navigation = await apiGet<DeptNavigationItem[]>('/depts/navigation')
  const entry = firstEntry(navigation)
  if (entry) {
    router.replace(`/org/${entry.id}`)
  }
})
</script>
