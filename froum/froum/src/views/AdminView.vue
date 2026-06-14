<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import AdminDashboard from '../components/admin/AdminDashboard.vue'

const router = useRouter()
const store = useStore()
const loading = ref(true)

onMounted(() => {
  // 检查是否已登录
  const token = localStorage.getItem('token')
  if (!token) {
    // 如果未登录，重定向到管理员登录页面
    router.replace('/admin/login')
    return
  }
  
  // 检查是否有管理员权限
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  const isAdmin = userInfo.role === 'admin' || userInfo.role === 'ADMIN' || userInfo.role === 'SUPER_ADMIN'
  
  if (!isAdmin) {
    // 如果不是管理员，重定向到 403 页面
    router.replace('/403')
    return
  }
  
  // 确保Vuex状态与localStorage同步
  if (!store.state.isAuthenticated) {
    store.dispatch('checkAuth')
  }
  
  // 如果是管理员，显示管理后台
  loading.value = false
})
</script>

<template>
  <div class="admin-view">
    <!-- 加载状态 -->
    <div v-if="loading" class="admin-loading">
      <div class="loading-spinner">
        <font-awesome-icon :icon="['fas', 'spinner']" spin />
      </div>
      <p>正在加载管理后台...</p>
    </div>
    
    <!-- 管理后台内容 -->
    <AdminDashboard v-else />
  </div>
</template>

<style scoped>
.admin-view {
  min-height: 100vh;
  background-color: var(--bg-color);
  color: var(--text-color);
  display: flex;
  flex-direction: column;
}

.admin-loading {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
}

.loading-spinner {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.admin-loading p {
  font-size: 1.2rem;
  color: var(--text-color);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .loading-spinner {
    font-size: 2.5rem;
  }
  
  .admin-loading p {
    font-size: 1rem;
  }
}

@media (max-width: 576px) {
  .loading-spinner {
    font-size: 2rem;
  }
}
</style> 