<template>
  <div class="auth-page">
    <section class="auth-brand">
      <h1>大同房建公寓段标准化资料管理系统</h1>
      <p>面向房建、公寓资料归档、共享、搜索和权限管理的统一平台，支持内网和公网部署。</p>
    </section>
    <section class="auth-panel">
      <h2>用户登录</h2>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-alert
          v-if="captchaError"
          :title="captchaError"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 14px"
        />
        <el-form-item label="人机验证">
          <div class="slider-captcha-wrapper">
            <div class="slider-captcha-row">
              <el-button :type="form.captchaKey ? 'success' : 'primary'" plain @click="openCaptcha">
                {{ form.captchaKey ? '验证已通过' : '点击完成滑块验证' }}
              </el-button>
              <el-button text @click="resetCaptcha">重新验证</el-button>
            </div>
            <div id="login-captcha" class="slider-captcha-host"></div>
          </div>
        </el-form-item>
        <el-button type="primary" style="width:100%" :loading="loading" @click="login">登录</el-button>
        <el-button text style="width:100%;margin:10px 0 0" @click="$router.push('/register')">申请注册账号</el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiPost } from '../api/http'
import { useAuthStore } from '../stores/auth'

declare global {
  interface Window {
    TAC?: new (config: Record<string, unknown>, style?: Record<string, unknown>) => {
      init: () => void
      destroyWindow: () => void
    }
  }
}

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const captchaError = ref('')
const form = reactive({ username: 'admin', password: 'Admin12345@@', captchaKey: '', captchaCode: '' })
let captchaInstance: InstanceType<NonNullable<typeof window.TAC>> | undefined

function openCaptcha() {
  captchaError.value = ''
  if (!window.TAC) {
    captchaError.value = '人机验证服务未连接，请先启动本项目后端服务后再登录。'
    return
  }
  captchaInstance?.destroyWindow()
  captchaInstance = new window.TAC({
    bindEl: '#login-captcha',
    requestCaptchaDataUrl: '/api/auth/captcha',
    validCaptchaUrl: '/api/auth/captcha/check',
    validSuccess: (result: any) => {
      form.captchaKey = result?.data?.captchaKey || ''
      form.captchaCode = result?.data?.captchaCode || ''
      captchaError.value = ''
      captchaInstance?.destroyWindow()
    },
    validFail: () => {
      resetCaptcha()
      captchaError.value = '滑块验证未通过，请重新验证。'
    }
  }, {
    logoUrl: null,
    i18n: {
      slider_title: '拖动滑块完成验证'
    }
  })
  captchaInstance.init()
}

function resetCaptcha() {
  form.captchaKey = ''
  form.captchaCode = ''
  captchaInstance?.destroyWindow()
  captchaInstance = undefined
}

async function login() {
  if (!form.captchaKey) {
    captchaError.value = '请先完成滑块验证。若一直失败，请检查后端服务是否已启动。'
    return
  }
  loading.value = true
  try {
    const result = await apiPost<{ token: string; user: any; permissions: string[] }>('/auth/login', form)
    auth.setSession(result.token, result.user, result.permissions)
    router.push('/dashboard')
  } catch {
    resetCaptcha()
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  captchaInstance?.destroyWindow()
})
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
