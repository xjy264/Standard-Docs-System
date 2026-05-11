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
        <SliderCaptcha
          ref="captchaRef"
          host-id="login-captcha"
          :verified="Boolean(form.captchaKey)"
          unavailable-message="人机验证服务未连接，请先启动本项目后端服务后再登录。"
          @verified="handleCaptchaVerified"
          @reset="resetCaptchaState"
        />
        <el-button type="primary" style="width:100%" :loading="loading" @click="login">登录</el-button>
        <el-button text style="width:100%;margin:10px 0 0" @click="$router.push('/register')">申请注册账号</el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiPost } from '../api/http'
import SliderCaptcha from '../components/SliderCaptcha.vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const captchaRef = ref<InstanceType<typeof SliderCaptcha> | null>(null)
const form = reactive({ username: 'admin', password: 'Admin12345@@', captchaKey: '', captchaCode: '' })

function resetCaptchaState() {
  form.captchaKey = ''
  form.captchaCode = ''
}

function resetCaptcha() {
  captchaRef.value?.reset()
}

function handleCaptchaVerified(payload: { captchaKey: string; captchaCode: string }) {
  form.captchaKey = payload.captchaKey
  form.captchaCode = payload.captchaCode
}

async function login() {
  if (!form.captchaKey) {
    captchaRef.value?.setError('请先完成滑块验证。若一直失败，请检查后端服务是否已启动。')
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
</script>
