<template>
  <el-alert
    v-if="errorMessage"
    :title="errorMessage"
    type="warning"
    :closable="false"
    show-icon
    style="margin-bottom: 14px"
  />
  <el-form-item label="人机验证">
    <div class="slider-captcha-wrapper">
      <div class="slider-captcha-row">
        <el-button :type="verified ? 'success' : 'primary'" plain :disabled="captchaDisabled" @click="openCaptcha">
          {{ buttonText }}
        </el-button>
        <el-button text :disabled="captchaDisabled" @click="reset">重新验证</el-button>
      </div>
      <div :id="hostId" class="slider-captcha-host"></div>
    </div>
  </el-form-item>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

interface CaptchaVerifiedPayload {
  captchaKey: string
  captchaCode: string
}

declare global {
  interface Window {
    TAC?: new (config: Record<string, unknown>, style?: Record<string, unknown>) => {
      init: () => void
      destroyWindow: () => void
      config?: {
        doSendRequest?: (request: Record<string, any>) => Promise<any>
      }
    }
  }
}

const props = defineProps<{
  hostId: string
  verified: boolean
  unavailableMessage: string
}>()

const emit = defineEmits<{
  (e: 'verified', payload: CaptchaVerifiedPayload): void
  (e: 'reset'): void
}>()

const errorMessage = ref('')
const captchaDisabled = computed(() => String(import.meta.env.VITE_CAPTCHA_PROVIDER || 'local').toLowerCase() === 'none')
const buttonText = computed(() => {
  if (captchaDisabled.value) {
    return '本地开发已跳过验证'
  }
  return props.verified ? '验证已通过' : '点击完成滑块验证'
})
let captchaInstance: InstanceType<NonNullable<typeof window.TAC>> | undefined

async function preflightCaptcha() {
  const response = await fetch('/api/auth/captcha', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: '{}'
  })
  const payload = await response.json().catch(() => undefined)
  const data = payload?.data
  if (!response.ok || payload?.code !== 200 || data?.type !== 'SLIDER' || !data?.backgroundImage || !data?.templateImage) {
    throw new Error(payload?.message || '验证码图片加载失败，请检查后端服务。')
  }
}

function wrapTacRequests(instance: InstanceType<NonNullable<typeof window.TAC>>) {
  const config = instance.config
  const originalSend = config?.doSendRequest?.bind(config)
  if (!config || !originalSend) {
    return
  }
  config.doSendRequest = async (request: Record<string, any>) => {
    try {
      const response = await originalSend(request)
      if (request.url === '/api/auth/captcha') {
        const data = response?.data
        if (!response || typeof response.code !== 'number') {
          return { code: 500, msg: '验证码接口返回格式错误' }
        }
        if (response.code === 200 && (!data?.type || !data?.backgroundImage || !data?.templateImage)) {
          return { code: 500, msg: '验证码图片数据不完整' }
        }
      }
      return response
    } catch {
      return { code: 500, msg: props.unavailableMessage }
    }
  }
}

async function openCaptcha() {
  errorMessage.value = ''
  if (captchaDisabled.value) {
    emit('verified', { captchaKey: 'CAPTCHA_DISABLED', captchaCode: 'SLIDER_PASSED' })
    return
  }
  if (!window.TAC) {
    errorMessage.value = props.unavailableMessage
    return
  }
  try {
    await preflightCaptcha()
    captchaInstance?.destroyWindow()
    captchaInstance = new window.TAC({
      bindEl: `#${props.hostId}`,
      requestCaptchaDataUrl: '/api/auth/captcha',
      validCaptchaUrl: '/api/auth/captcha/check',
      validSuccess: (result: any) => {
        emit('verified', {
          captchaKey: result?.data?.captchaKey || '',
          captchaCode: result?.data?.captchaCode || ''
        })
        errorMessage.value = ''
        captchaInstance?.destroyWindow()
      },
      validFail: () => {
        reset()
        errorMessage.value = '滑块验证未通过，请重新验证。'
      }
    }, {
      logoUrl: null,
      i18n: {
        slider_title: '拖动滑块完成验证'
      }
    })
    wrapTacRequests(captchaInstance)
    captchaInstance.init()
  } catch (error: any) {
    reset()
    errorMessage.value = error?.message || props.unavailableMessage
  }
}

function reset() {
  captchaInstance?.destroyWindow()
  captchaInstance = undefined
  emit('reset')
  if (captchaDisabled.value) {
    emit('verified', { captchaKey: 'CAPTCHA_DISABLED', captchaCode: 'SLIDER_PASSED' })
  }
}

function setError(message: string) {
  errorMessage.value = message
}

onMounted(() => {
  if (captchaDisabled.value) {
    emit('verified', { captchaKey: 'CAPTCHA_DISABLED', captchaCode: 'SLIDER_PASSED' })
  }
})

onBeforeUnmount(() => {
  captchaInstance?.destroyWindow()
})

defineExpose({ reset, setError })
</script>

<style scoped>
.slider-captcha-wrapper {
  position: relative;
  width: 100%;
}

.slider-captcha-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.slider-captcha-row .el-button:first-child {
  flex: 1;
}

.slider-captcha-host {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 50%;
  z-index: 3000;
  width: 318px;
  max-width: calc(100vw - 24px);
  transform: translateX(-50%);
}

.slider-captcha-host:empty {
  display: none;
}

.slider-captcha-host :deep(#tianai-captcha-parent) {
  max-width: 100%;
}
</style>
