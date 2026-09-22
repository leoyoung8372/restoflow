<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const username = ref('')   // 双向绑定到工号输入框
const password = ref('')   // 双向绑定到密码输入框
const loading = ref(false) // 请求期间禁用按钮，防止连点重复提交

async function handleLogin() {
  // 前端先拦一道空值，省掉一次网络往返；后端也有同样的校验兜底。
  // 提示由 request.ts 的拦截器统一弹出，这里不重复处理。
  if (!username.value.trim() || !password.value.trim()) {
    ElMessage.error('请输入工号和密码')
    return
  }

  loading.value = true
  try {
    await authStore.login({ username: username.value.trim(), password: password.value })
    router.push('/') // 登录成功，跳转首页
  } catch {
    // 失败时拦截器已弹出后端的错误提示，这里无需再处理
  } finally {
    // 无论成功失败都要恢复按钮，否则失败后按钮一直转圈
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="title">RestoFlow</h1>
      <p class="subtitle">餐饮收银系统</p>

      <el-input
        v-model="username"
        placeholder="工号"
        size="large"
        autofocus
        @keyup.enter="handleLogin"
      />
      <el-input
        v-model="password"
        type="password"
        placeholder="密码"
        size="large"
        show-password
        @keyup.enter="handleLogin"
      />

      <el-button
        type="primary"
        size="large"
        class="login-btn"
        :loading="loading"
        @click="handleLogin"
      >
        登录
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.login-card {
  display: flex;
  flex-direction: column;
  gap: 16px; /* 子元素间距，替代给每个元素写 margin */
  width: 360px;
  padding: 40px 32px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 12px rgb(0 0 0 / 10%);
}

.title {
  margin: 0;
  font-size: 24px;
  text-align: center;
}

.subtitle {
  margin: 0 0 8px;
  font-size: 14px;
  color: #909399;
  text-align: center;
}

.login-btn {
  width: 100%;
}
</style>
