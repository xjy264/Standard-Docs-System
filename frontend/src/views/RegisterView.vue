<template>
  <div class="auth-page">
    <section class="auth-brand">
      <h1>账号注册</h1>
      <p>注册后需要管理员审批，通过后才能登录系统。</p>
    </section>
    <section class="auth-panel">
      <h2>提交注册申请</h2>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="真实姓名"><el-input v-model="form.realName" maxlength="10" autocomplete="name" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" maxlength="11" autocomplete="tel" /></el-form-item>
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
        <el-form-item label="确认密码"><el-input v-model="form.confirmPassword" type="password" show-password /></el-form-item>
        <SliderCaptcha
          ref="captchaRef"
          host-id="register-captcha"
          :verified="Boolean(form.captchaKey)"
          unavailable-message="人机验证暂时不可用，请稍后重试或联系管理员。"
          @verified="handleCaptchaVerified"
          @reset="resetCaptchaState"
        />
        <el-button type="primary" style="width:100%" @click="submit">提交注册</el-button>
        <el-button text style="width:100%;margin:10px 0 0" @click="$router.push('/login')">返回登录</el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet, apiPost } from '../api/http'
import SliderCaptcha from '../components/SliderCaptcha.vue'
import { passwordValidationMessage } from '../utils/passwordPolicy'
import { normalizeRegisterPhone, normalizeRegisterRealName } from '../utils/userValidation'

const router = useRouter()
const captchaRef = ref<InstanceType<typeof SliderCaptcha> | null>(null)
const depts = ref<Array<{ id: number; deptName: string }>>([])
const deptTree = computed(() => buildDeptTree(depts.value))
const form = reactive({
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  deptId: undefined as number | undefined,
  captchaKey: '',
  captchaCode: ''
})

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

async function submit() {
  const realName = normalizeRegisterRealName(form.realName)
  if (!realName) {
    ElMessage.warning('真实姓名需为2-10位中文或中间点')
    return
  }
  const phone = normalizeRegisterPhone(form.phone)
  if (!phone) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!form.deptId) {
    ElMessage.warning('请选择所属组织')
    return
  }
  const passwordMessage = passwordValidationMessage(form.password, form.confirmPassword)
  if (passwordMessage) {
    ElMessage.warning(passwordMessage)
    return
  }
  if (!form.captchaKey) {
    captchaRef.value?.setError('请先完成滑块验证。若一直失败，请稍后重试或联系管理员。')
    return
  }
  try {
    await apiPost('/auth/register', { ...form, realName, phone })
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

</script>
