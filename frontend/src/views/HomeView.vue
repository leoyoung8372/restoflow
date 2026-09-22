<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()               // 清除内存与 localStorage 里的登录状态
  ElMessage.success('已退出登录')
  router.push('/login')            // 回到登录页
}
</script>

<template>
  <div class="home-page">
    <div class="welcome">
      <!-- 用 ?. 访问：user 为 null（未登录）时显示空白而不是报错 -->
      <h1>欢迎，{{ authStore.user?.realName }}</h1>
      <p class="hint">工号：{{ authStore.user?.username }}</p>
      <p class="hint">角色：{{ authStore.user?.role }}</p>

      <el-button @click="handleLogout">退出登录</el-button>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 56px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 12px rgb(0 0 0 / 10%);
}

.welcome h1 {
  margin: 0 0 8px;
  font-size: 22px;
}

.hint {
  margin: 0;
  color: #909399;
  font-size: 14px;
}
</style>
