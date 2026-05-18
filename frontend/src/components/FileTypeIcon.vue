<template>
  <span class="file-type-icon" :class="theme.className" :style="iconStyle">
    <span class="file-type-icon-corner" />
    <span class="file-type-icon-label">{{ theme.label }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  extension?: string
  fileName?: string
  size?: number
}>(), {
  size: 46
})

const normalizedExtension = computed(() => {
  const raw = props.extension?.trim().toLowerCase()
  if (raw) {
    return raw
  }
  const name = props.fileName || ''
  const index = name.lastIndexOf('.')
  return index >= 0 ? name.slice(index + 1).trim().toLowerCase() : ''
})

const theme = computed(() => {
  switch (normalizedExtension.value) {
    case 'doc':
    case 'docx':
      return { label: 'DOC', className: 'theme-word' }
    case 'xls':
    case 'xlsx':
      return { label: 'XLS', className: 'theme-excel' }
    case 'ppt':
    case 'pptx':
      return { label: 'PPT', className: 'theme-ppt' }
    case 'pdf':
      return { label: 'PDF', className: 'theme-pdf' }
    case 'jpg':
    case 'jpeg':
    case 'png':
    case 'gif':
    case 'bmp':
    case 'webp':
      return { label: 'IMG', className: 'theme-image' }
    case 'dwg':
    case 'dxf':
      return { label: 'CAD', className: 'theme-cad' }
    case 'zip':
    case 'rar':
    case '7z':
      return { label: 'ZIP', className: 'theme-archive' }
    default:
      return { label: 'FILE', className: 'theme-default' }
  }
})

const iconStyle = computed(() => ({
  '--file-icon-size': `${props.size}px`
}))
</script>

<style scoped>
.file-type-icon {
  position: relative;
  display: inline-flex;
  align-items: flex-end;
  justify-content: center;
  width: var(--file-icon-size);
  height: calc(var(--file-icon-size) * 1.22);
  border-radius: calc(var(--file-icon-size) * 0.16);
  background: #2b9fd0;
  overflow: hidden;
}

.file-type-icon-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: calc(var(--file-icon-size) * 0.42);
  height: calc(var(--file-icon-size) * 0.42);
  background: rgb(255 255 255 / 0.34);
  border-bottom-left-radius: calc(var(--file-icon-size) * 0.14);
  clip-path: polygon(100% 0, 0 0, 100% 100%);
}

.file-type-icon-label {
  position: relative;
  z-index: 1;
  margin-bottom: calc(var(--file-icon-size) * 0.16);
  padding: 0 calc(var(--file-icon-size) * 0.08);
  font-size: calc(var(--file-icon-size) * 0.24);
  line-height: 1;
  font-weight: 500;
  color: #fff;
  letter-spacing: 0;
}

.theme-word {
  background: #2aa3d2;
}

.theme-excel {
  background: #24a564;
}

.theme-ppt {
  background: #f06b3e;
}

.theme-pdf {
  background: #e15044;
}

.theme-image {
  background: #20b7b9;
}

.theme-cad {
  background: #5876d9;
}

.theme-archive {
  background: #d8a237;
}

.theme-default {
  background: #8c97a7;
}
</style>
