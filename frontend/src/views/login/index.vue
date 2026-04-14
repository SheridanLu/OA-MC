<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">MOCHU-OA 施工管理系统</h2>

      <!-- 第一步: 输入账号 -->
      <div v-if="step === 1">
        <el-form ref="accountFormRef" :model="accountForm" :rules="accountRules">
          <el-form-item prop="username">
            <el-input
              v-model="accountForm.username"
              placeholder="请输入用户名或手机号"
              prefix-icon="User"
              size="large"
              @keyup.enter="handleCheckAccount"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleCheckAccount"
          >
            下一步
          </el-button>
        </el-form>
      </div>

      <!-- 第二步: 密码登录 -->
      <div v-if="step === 2">
        <div class="step-back" @click="step = 1">
          <el-icon><ArrowLeft /></el-icon>
          <span>{{ accountForm.username }}</span>
        </div>
        <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules">
          <el-form-item prop="password">
            <el-input
              v-model="passwordForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handlePasswordLogin"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handlePasswordLogin"
          >
            登录
          </el-button>
        </el-form>
        <div class="login-links">
          <el-link type="primary" @click="step = 3">短信登录</el-link>
          <el-link type="info" @click="handleForgotPassword">忘记密码</el-link>
        </div>
      </div>

      <!-- 第三步: 短信登录 -->
      <div v-if="step === 3">
        <div class="step-back" @click="step = 2">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回密码登录</span>
        </div>
        <el-form ref="smsFormRef" :model="smsForm" :rules="smsRules">
          <el-form-item prop="phone">
            <el-input
              v-model="smsForm.phone"
              placeholder="请输入手机号"
              prefix-icon="Phone"
              size="large"
            />
          </el-form-item>
          <el-form-item prop="smsCode">
            <div class="sms-row">
              <el-input
                v-model="smsForm.smsCode"
                placeholder="验证码"
                size="large"
                @keyup.enter="handleSmsLogin"
              />
              <el-button
                size="large"
                :disabled="smsCountdown > 0"
                @click="handleSendSms"
              >
                {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleSmsLogin"
          >
            登录
          </el-button>
        </el-form>
      </div>
    </div>

    <!-- 忘记密码弹窗 -->
    <el-dialog v-model="forgotPasswordVisible" title="忘记密码" width="420px" :close-on-click-modal="false">
      <el-form ref="forgotFormRef" :model="forgotForm" :rules="forgotRules" label-width="80px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="forgotForm.phone" placeholder="请输入注册手机号" prefix-icon="Phone" size="large" />
        </el-form-item>
        <el-form-item label="验证码" prop="smsCode">
          <div class="sms-row">
            <el-input v-model="forgotForm.smsCode" placeholder="验证码" size="large" />
            <el-button size="large" :disabled="forgotSmsCountdown > 0" @click="handleForgotSendSms">
              {{ forgotSmsCountdown > 0 ? `${forgotSmsCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="forgotForm.newPassword" type="password" placeholder="请输入新密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="请再次输入新密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotPasswordVisible = false">取消</el-button>
        <el-button type="primary" :loading="forgotLoading" @click="handleResetPassword">重置密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { checkAccount, sendSms, resetPassword } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const step = ref(1)
const loading = ref(false)
const smsCountdown = ref(0)

const accountFormRef = ref(null)
const passwordFormRef = ref(null)
const smsFormRef = ref(null)

const accountForm = reactive({ username: '' })
const passwordForm = reactive({ password: '' })
const smsForm = reactive({ phone: '', smsCode: '' })

const accountRules = {
  username: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }]
}
const passwordRules = {
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const smsRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  smsCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const handleCheckAccount = async () => {
  const valid = await accountFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await checkAccount(accountForm.username)
    // 账号存在，检查是否锁定
    if (res.data.locked) {
      ElMessage.error('账号已锁定，请30分钟后再试')
      return
    }
    step.value = 2
  } catch (e) {
    // 404 由拦截器显示"数据不存在或已被删除"，这里不需要额外处理
  } finally {
    loading.value = false
  }
}

const getSafeRedirect = () => {
  const redirect = Array.isArray(route.query.redirect)
    ? route.query.redirect[0]
    : route.query.redirect

  if (typeof redirect !== 'string' || !redirect.startsWith('/')) return '/home'
  if (redirect.startsWith('//') || redirect.includes('://')) return '/home'

  return redirect
}

const handlePasswordLogin = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.loginByPassword({
      username: accountForm.username,
      password: passwordForm.password
    })
    router.push(getSafeRedirect())
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleSendSms = async () => {
  if (!smsForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  try {
    await sendSms(smsForm.phone)
    ElMessage.success('验证码已发送')
    smsCountdown.value = 60
    const timer = setInterval(() => {
      smsCountdown.value--
      if (smsCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    // error handled by interceptor
  }
}

const handleSmsLogin = async () => {
  const valid = await smsFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.loginBySms({
      phone: smsForm.phone,
      smsCode: smsForm.smsCode
    })
    router.push(getSafeRedirect())
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

const forgotPasswordVisible = ref(false)
const forgotLoading = ref(false)
const forgotSmsCountdown = ref(0)
const forgotFormRef = ref(null)

const forgotForm = reactive({
  phone: '',
  smsCode: '',
  newPassword: '',
  confirmPassword: ''
})

const forgotRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  smsCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== forgotForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleForgotPassword = () => {
  forgotForm.phone = ''
  forgotForm.smsCode = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
  forgotPasswordVisible.value = true
}

const handleForgotSendSms = async () => {
  if (!forgotForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(forgotForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    await sendSms(forgotForm.phone)
    ElMessage.success('验证码已发送')
    forgotSmsCountdown.value = 60
    const timer = setInterval(() => {
      forgotSmsCountdown.value--
      if (forgotSmsCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    // error handled by interceptor
  }
}

const handleResetPassword = async () => {
  const valid = await forgotFormRef.value.validate().catch(() => false)
  if (!valid) return

  forgotLoading.value = true
  try {
    await resetPassword({
      phone: forgotForm.phone,
      smsCode: forgotForm.smsCode,
      newPassword: forgotForm.newPassword
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    forgotPasswordVisible.value = false
  } catch (e) {
    // error handled by interceptor
  } finally {
    forgotLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.login-title {
  text-align: center;
  margin-bottom: 32px;
  color: #303133;
  font-size: 22px;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
}

.step-back {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 20px;
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
}

.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
}

.sms-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.sms-row .el-input {
  flex: 1;
}
</style>
