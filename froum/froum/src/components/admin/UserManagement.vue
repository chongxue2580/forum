<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAllUsers,
  disableUser,
  enableUser,
  resetUserPassword,
  changeUserRole,
  deleteUser,
  searchUsers
} from '@/api/admin'

const router = useRouter()
const users = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchQuery = ref('')
const filterOptions = ref({
  role: '',
  status: ''
})

const displayedUsers = computed(() => users.value)

const roleOptions = [
  { label: '普通用户', value: 'USER' },
  { label: '管理员', value: 'ADMIN' },
  { label: '超级管理员', value: 'SUPER_ADMIN' }
]

const statusOptions = [
  { label: '正常', value: 'ACTIVE' },
  { label: '已禁用', value: 'DISABLED' }
]

const unwrapPage = (response) => {
  const page = response?.data || response || {}
  return {
    content: Array.isArray(page.content) ? page.content : Array.isArray(page) ? page : [],
    totalElements: page.totalElements || page.total || 0
  }
}

const formatDateTime = (value) => {
  if (!value) return '从未登录'
  const date = Array.isArray(value)
    ? new Date(value[0], value[1] - 1, value[2], value[3] || 0, value[4] || 0)
    : new Date(value)

  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const normalizeUser = (user) => {
  const role = String(user.role || 'USER').toUpperCase()
  const status = String(user.status || 'ACTIVE').toUpperCase()
  return {
    ...user,
    name: user.nickname || user.username || `用户${user.id}`,
    role,
    status,
    roleLabel: roleOptions.find(option => option.value === role)?.label || role,
    statusLabel: statusOptions.find(option => option.value === status)?.label || status,
    lastLoginText: formatDateTime(user.lastLoginTime),
    createdAtText: formatDateTime(user.createdAt),
    loginCount: user.loginCount || 0
  }
}

const getAvatarColor = (name) => {
  const colors = [
    '#2563eb', '#16a34a', '#d97706', '#7c3aed',
    '#db2777', '#dc2626', '#ea580c', '#0891b2',
    '#4f46e5', '#0f766e', '#65a30d', '#9333ea'
  ]
  const safeName = name || '用户'
  const index = safeName.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % colors.length
  return colors[index]
}

const getAvatarText = (name) => (name || '用').charAt(0)

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }

    if (filterOptions.value.role) {
      params.role = filterOptions.value.role
    }
    if (filterOptions.value.status) {
      params.status = filterOptions.value.status
    }

    const keyword = searchQuery.value.trim()
    const response = keyword
      ? await searchUsers(keyword, params)
      : await getAllUsers(params)

    const page = unwrapPage(response)
    users.value = page.content.map(normalizeUser)
    total.value = page.totalElements
  } catch (error) {
    console.error('加载用户列表失败:', error)
    users.value = []
    total.value = 0
    ElMessage.error(error.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadUsers()
}

const resetFilters = () => {
  searchQuery.value = ''
  filterOptions.value = {
    role: '',
    status: ''
  }
  currentPage.value = 1
  loadUsers()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadUsers()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadUsers()
}

const toggleUserStatus = async (user) => {
  const isActive = user.status === 'ACTIVE'
  try {
    let reason
    if (isActive) {
      const result = await ElMessageBox.prompt(`请输入禁用用户「${user.name}」的原因`, '禁用用户', {
        confirmButtonText: '禁用',
        cancelButtonText: '取消',
        inputType: 'textarea'
      })
      reason = result.value
      await disableUser(user.id, reason)
    } else {
      await ElMessageBox.confirm(`确定要启用用户「${user.name}」吗？`, '启用用户', {
        confirmButtonText: '启用',
        cancelButtonText: '取消',
        type: 'success'
      })
      await enableUser(user.id)
    }

    ElMessage.success(isActive ? '用户已禁用' : '用户已启用')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '更新用户状态失败')
    }
  }
}

const toggleUserRole = async (user) => {
  const targetRole = user.role === 'ADMIN' || user.role === 'SUPER_ADMIN' ? 'USER' : 'ADMIN'
  try {
    await ElMessageBox.confirm(
      `确定要将用户「${user.name}」角色改为「${roleOptions.find(option => option.value === targetRole)?.label}」吗？`,
      '修改角色',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await changeUserRole(user.id, targetRole)
    ElMessage.success('用户角色已更新')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '更新用户角色失败')
    }
  }
}

const handleResetPassword = async (user) => {
  try {
    await ElMessageBox.confirm(`确定要重置用户「${user.name}」的密码吗？`, '重置密码', {
      confirmButtonText: '重置',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await resetUserPassword(user.id)
    const password = response?.data || response
    ElMessageBox.alert(`新密码：${password}`, '密码已重置', {
      confirmButtonText: '知道了'
    })
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '重置密码失败')
    }
  }
}

const handleDeleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${user.name}」吗？此操作不可恢复。`, '删除用户', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteUser(user.id)
    ElMessage.success('用户已删除')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除用户失败')
    }
  }
}

const viewUserProfile = (user) => {
  router.push(`/user/${user.id}`)
}

const sendMessage = (user) => {
  router.push({
    path: '/messages',
    query: { userId: user.id }
  })
}

onMounted(loadUsers)
</script>

<template>
  <div class="user-management admin-page-container">
    <div class="admin-page-header">
      <h1 class="admin-page-title">
        <font-awesome-icon icon="users" />
        用户管理
      </h1>
      <div class="header-actions">
        <el-button type="primary" class="admin-action-btn primary" :loading="loading" @click="loadUsers">
          <font-awesome-icon icon="sync" />
          刷新
        </el-button>
        <el-button class="admin-action-btn" @click="resetFilters">
          <font-awesome-icon icon="times" />
          重置筛选
        </el-button>
      </div>
    </div>

    <div class="admin-content-card">
      <div class="admin-section-title">
        <font-awesome-icon icon="filter" />
        筛选条件
      </div>
      <div class="admin-search-filter">
        <el-input
          v-model="searchQuery"
          placeholder="搜索用户名或邮箱..."
          clearable
          class="search-input"
          size="large"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <font-awesome-icon icon="search" />
          </template>
        </el-input>

        <el-select v-model="filterOptions.role" placeholder="角色筛选" clearable class="filter-select" size="large">
          <el-option label="全部角色" value="" />
          <el-option label="管理员" value="ADMIN" />
          <el-option label="超级管理员" value="SUPER_ADMIN" />
          <el-option label="普通用户" value="USER" />
        </el-select>

        <el-select v-model="filterOptions.status" placeholder="状态筛选" clearable class="filter-select" size="large">
          <el-option label="全部状态" value="" />
          <el-option label="正常" value="ACTIVE" />
          <el-option label="已禁用" value="DISABLED" />
        </el-select>

        <button class="admin-action-btn primary" @click="handleSearch">
          <font-awesome-icon icon="search" />
          查询
        </button>
      </div>
    </div>

    <div class="admin-content-card">
      <div class="admin-section-title">
        <font-awesome-icon icon="table" />
        用户列表
      </div>
      <div class="admin-table-container admin-responsive-table">
        <el-table v-loading="loading" :data="displayedUsers" style="width: 100%" stripe>
          <el-table-column label="用户信息" min-width="320">
            <template #default="{ row }">
              <div class="user-info">
                <el-avatar
                  :size="48"
                  :style="{
                    backgroundColor: getAvatarColor(row.name),
                    color: '#ffffff',
                    fontWeight: '600',
                    fontSize: '18px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    cursor: 'pointer',
                    transition: 'all 0.3s ease',
                    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                    border: '2px solid rgba(255, 255, 255, 0.8)'
                  }"
                  >
                  {{ getAvatarText(row.name) }}
                </el-avatar>
                <div class="user-details">
                  <div class="user-name">{{ row.name }}</div>
                  <div class="user-meta">
                    <div class="meta-item">
                      <font-awesome-icon icon="envelope" />
                      <span>{{ row.email }}</span>
                    </div>
                    <div class="meta-item">
                      <font-awesome-icon icon="clock" />
                      <span>{{ row.lastLoginText }}</span>
                    </div>
                  </div>
                </div>
                <div class="user-tags">
                  <div class="admin-tag" :class="row.role === 'ADMIN' || row.role === 'SUPER_ADMIN' ? 'danger' : 'primary'">
                    <font-awesome-icon :icon="row.role === 'ADMIN' || row.role === 'SUPER_ADMIN' ? 'user-shield' : 'user'" />
                    {{ row.roleLabel }}
                  </div>
                  <div class="admin-tag" :class="row.status === 'ACTIVE' ? 'success' : 'danger'">
                    <font-awesome-icon :icon="row.status === 'ACTIVE' ? 'check-circle' : 'ban'" />
                    {{ row.statusLabel }}
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>

        <el-table-column label="账号数据" width="220">
          <template #default="{ row }">
            <div class="user-stats">
              <div class="stat-item">
                <font-awesome-icon icon="sign-in-alt" />
                <span>{{ row.loginCount }}</span>
                <span class="stat-label">登录</span>
              </div>
              <div class="stat-item">
                <font-awesome-icon icon="clock" />
                <span>{{ row.createdAtText }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons admin-mobile-stack">
                <div class="button-group">
                  <button
                    :class="['admin-action-btn', row.status === 'ACTIVE' ? 'danger' : 'success']"
                    @click="toggleUserStatus(row)"
                  >
                    <font-awesome-icon :icon="row.status === 'ACTIVE' ? 'ban' : 'check-circle'" />
                    {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
                  </button>
                  <button
                    :class="['admin-action-btn', row.role === 'ADMIN' || row.role === 'SUPER_ADMIN' ? 'warning' : 'primary']"
                    @click="toggleUserRole(row)"
                  >
                    <font-awesome-icon :icon="row.role === 'ADMIN' || row.role === 'SUPER_ADMIN' ? 'user-minus' : 'user-plus'" />
                    {{ row.role === 'ADMIN' || row.role === 'SUPER_ADMIN' ? '设为用户' : '设为管理员' }}
                  </button>
                </div>

                <div class="button-group">
                  <button
                    class="admin-action-btn primary"
                    @click="viewUserProfile(row)"
                  >
                    <font-awesome-icon icon="user" /> 查看
                  </button>
                  <button
                    class="admin-action-btn primary"
                    @click="sendMessage(row)"
                  >
                    <font-awesome-icon icon="paper-plane" /> 发消息
                  </button>
                  <button
                    class="admin-action-btn warning"
                    @click="handleResetPassword(row)"
                  >
                    <font-awesome-icon icon="lock" /> 重置密码
                  </button>
                  <button
                    class="admin-action-btn danger"
                    @click="handleDeleteUser(row)"
                  >
                    <font-awesome-icon icon="trash" /> 删除
                  </button>
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="admin-pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        :current-page="currentPage"
        @size-change="handlePageSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
@import './AdminCommonStyles.css';

.user-management {
  /* 使用通用样式 */
}

.header-actions {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.search-input {
  min-width: 300px;
  flex: 1;
}

.filter-select {
  min-width: 160px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 0;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 0.5rem;
  background: linear-gradient(135deg, var(--text-color) 0%, var(--primary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.user-meta {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: var(--text-light);
  font-size: 0.875rem;
  padding: 0.25rem 0.5rem;
  background: rgba(37, 99, 235, 0.05);
  border-radius: var(--radius);
  border: 1px solid rgba(37, 99, 235, 0.1);
}

.user-tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.user-stats {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: var(--text-light);
  font-size: 0.875rem;
  padding: 0.375rem 0.75rem;
  background: linear-gradient(135deg, var(--bg-white) 0%, rgba(37, 99, 235, 0.05) 100%);
  border-radius: var(--radius);
  border: 1px solid rgba(37, 99, 235, 0.1);
  transition: all 0.2s ease;
}

.stat-item:hover {
  transform: scale(1.02);
  box-shadow: var(--shadow-sm);
}

.stat-label {
  color: var(--text-lighter);
  margin-left: 0.25rem;
  font-size: 0.75rem;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.button-group {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .header-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 0.75rem;
  }

  .user-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .user-meta {
    flex-direction: column;
    gap: 0.5rem;
  }

  .user-stats {
    flex-direction: column;
    gap: 0.5rem;
  }

  .action-buttons {
    width: 100%;
  }

  .button-group {
    flex-direction: column;
  }
}

@media (max-width: 576px) {
  .search-input {
    min-width: auto;
  }

  .filter-select {
    min-width: auto;
  }
}
</style>
