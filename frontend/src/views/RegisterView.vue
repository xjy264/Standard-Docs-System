<template>
  <div class="auth-page">
    <section class="auth-brand">
      <h1>账号注册</h1>
      <p>注册后需要管理员审批，通过后才能登录系统。</p>
    </section>
    <section class="auth-panel">
      <h2>提交注册申请</h2>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTree"
            node-key="id"
            :props="{ label: 'deptName', children: 'children', disabled: 'disabled' }"
            check-strictly
            default-expand-all
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
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
            <div id="register-captcha" class="slider-captcha-host"></div>
          </div>
        </el-form-item>
        <el-button type="primary" style="width:100%" @click="submit">提交注册</el-button>
        <el-button text style="width:100%;margin:10px 0 0" @click="$router.push('/login')">返回登录</el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet, apiPost } from '../api/http'

declare global {
  interface Window {
    TAC?: new (config: Record<string, unknown>, style?: Record<string, unknown>) => {
      init: () => void
      destroyWindow: () => void
    }
  }
}

const router = useRouter()
const captchaError = ref('')
const depts = ref<Array<{ id: number; deptName: string }>>([])
const deptTree = computed(() => buildDeptTree(depts.value))
const form = reactive({ username: '', password: '', realName: '', phone: '', deptId: undefined as number | undefined, captchaKey: '', captchaCode: '' })
let captchaInstance: InstanceType<NonNullable<typeof window.TAC>> | undefined

function openCaptcha() {
  captchaError.value = ''
  if (!window.TAC) {
    captchaError.value = '人机验证服务未连接，请先启动本项目后端服务后再注册。'
    return
  }
  captchaInstance?.destroyWindow()
  captchaInstance = new window.TAC({
    bindEl: '#register-captcha',
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

async function submit() {
  if (!form.deptId) {
    ElMessage.warning('请选择所属组织')
    return
  }
  if (!form.captchaKey) {
    captchaError.value = '请先完成滑块验证。若一直失败，请检查后端服务是否已启动。'
    return
  }
  try {
    await apiPost('/auth/register', form)
    ElMessage.success('注册申请已提交，请等待管理员审批')
    router.push('/login')
  } catch {
    resetCaptcha()
  }
}

function buildDeptTree(list: any[]) {
  const childrenByParent = new Map<number, any[]>()
  const ids = new Set(list.map((item) => Number(item.id)))
  list.forEach((item) => {
    const parentId = Number(item.parentId || 0)
    const children = childrenByParent.get(parentId) || []
    children.push(item)
    childrenByParent.set(parentId, children)
  })

  const buildNode = (item: any): any => {
    const id = Number(item.id)
    const children = (childrenByParent.get(id) || []).map(buildNode)
    const topLevel = !Number(item.parentId || 0)
    return {
      ...item,
      disabled: topLevel && children.length > 0,
      children
    }
  }

  return list
    .filter((item) => Number(item.parentId || 0) === 0 || !ids.has(Number(item.parentId)))
    .map(buildNode)
}

onMounted(async () => {
  try {
    depts.value = await apiGet('/depts/tree')
  } catch {
    depts.value = []
  }
})

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
