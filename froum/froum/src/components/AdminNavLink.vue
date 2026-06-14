<script setup>
import { ref, computed } from 'vue'
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

// 导航到前台页面
const goToHome = () => {
  router.push('/')
}

// 判断当前是否在管理页面
const isInAdminPage = computed(() => {
  return router.currentRoute.value.path.startsWith('/admin')
})
</script>

<template>
  <div v-if="isAdmin" class="admin-nav-link">
    <el-tooltip :content="isInAdminPage ? '返回前台' : '进入管理后台'" placement="bottom">
      <el-button 
        :type="isInAdminPage ? 'default' : 'primary'" 
        size="small" 
        @click="isInAdminPage ? goToHome() : goToAdmin()"
        class="admin-button"
      >
        <font-awesome-icon :icon="isInAdminPage ? 'home' : 'shield-alt'" />
        <span>{{ isInAdminPage ? '返回前台' : '管理后台' }}</span>
      </el-button>
    </el-tooltip>
  </div>
</template>

<style scoped>
.admin-nav-link {
  display: inline-block;
}

.admin-button {
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: 500;
}
</style> 