<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 从localStorage获取用户信息
const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch (e) {
    return {}
  }
})

// 判断是否为管理员
const isAdmin = computed(() => {
  const role = userInfo.value.role
  return role === 'admin' || role === 'ADMIN' || role === 'SUPER_ADMIN'
})

// 导航到管理页面
const goToAdmin = () => {
  router.push('/admin')
}
</script>

<template>
  <div v-if="isAdmin" class="admin-link">
    <el-button 
      type="primary" 
      size="small" 
      @click="goToAdmin"
      class="admin-button"
    >
      <font-awesome-icon icon="shield-alt" />
      <span>管理后台</span>
    </el-button>
  </div>
</template>

<style scoped>
.admin-link {
  display: inline-block;
}

.admin-button {
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: 500;
}
</style> 