<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>SkillsOps</h2>
      <el-tabs v-model="mode" class="login-tabs">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="email">
          <el-input v-model="form.email" placeholder="邮箱（选填）" prefix-icon="Message" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit" :loading="loading" style="width:100%">
            {{ mode === 'login' ? '登录' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const mode = ref('login')
const loading = ref(false)
const formRef = ref(null)
const form = reactive({ username: '', password: '', email: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, message: '至少3位', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '至少6位', trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login(form.username, form.password)
    } else {
      await auth.register({ username: form.username, password: form.password, email: form.email })
      await auth.login(form.username, form.password)
    }
    ElMessage.success('登录成功')
    router.push('/market')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container { display: flex; justify-content: center; align-items: center; height: 100vh; background: #f0f2f5; }
.login-card { width: 400px; }
.login-card h2 { text-align: center; margin-bottom: 20px; color: #409EFF; }
.login-tabs { margin-bottom: 10px; }
</style>
