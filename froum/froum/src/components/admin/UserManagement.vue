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
  searchUsers,
  batchDisableUsers,
  batchEnableUsers,
  batchDeleteUsers
} from '@/api/admin'

const router = useRouter()
const users = ref([])
const loading = ref(false)
const viewMode = ref(localStorage.getItem('adminView_users') || 'grid')
const setView = (v) => { viewMode.value = v; localStorage.setItem('adminView_users', v) }
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchQuery = ref('')
const userTableRef = ref(null)
const selectedUsers = ref([])
const batchLoading = ref(false)
const filterOptions = ref({
  role: '',
  status: ''
})

const displayedUsers = computed(() => users.value)

// KPI（总数取后端 total，其余按当前页统计）
const kpis = computed(() => {
  const list = users.value
  const admins = list.filter(u => u.role === 'ADMIN' || u.role === 'SUPER_ADMIN').length
  const active = list.filter(u => u.status === 'ACTIVE').length
  const disabled = list.filter(u => u.status === 'DISABLED').length
  return [
    { key: 'total', label: '用户总数', value: total.value, icon: 'users', tone: '' },
    { key: 'active', label: '正常（本页）', value: active, icon: 'circle-check', tone: 'is-success' },
    { key: 'admin', label: '管理员（本页）', value: admins, icon: 'user-shield', tone: '' },
    { key: 'disabled', label: '已禁用（本页）', value: disabled, icon: 'ban', tone: 'is-danger' }
  ]
})

const isSelected = (user) => selectedUsers.value.some(u => u.id === user.id)

const toggleSelect = (user) => {
  if (isSelected(user)) {
    selectedUsers.value = selectedUsers.value.filter(u => u.id !== user.id)
  } else {
    selectedUsers.value = [...selectedUsers.value, user]
  }
}

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
    '#6c8ff5', '#68c3a3', '#f1a766', '#a991f7',
    '#ee86b7', '#ef8c8c', '#7ac7d7', '#96b86d'
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

const handleSelectionChange = (rows) => {
  selectedUsers.value = rows
}

const clearSelection = () => {
  selectedUsers.value = []
}

const reportBatchResult = (result, actionLabel) => {
  const data = result?.data || result || {}
  if (data.failed > 0) {
    ElMessage.warning(`${actionLabel}完成：成功 ${data.succeeded} 个，失败 ${data.failed} 个（${data.errors?.[0] || ''}）`)
  } else {
    ElMessage.success(`已${actionLabel} ${data.succeeded} 个用户`)
  }
}

const handleBatchDisable = async () => {
  const ids = selectedUsers.value.map(user => user.id)
  try {
    const result = await ElMessageBox.prompt(`将批量禁用 ${ids.length} 个用户，请输入禁用原因`, '批量禁用', {
      confirmButtonText: '禁用',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })
    batchLoading.value = true
    const response = await batchDisableUsers(ids, result.value)
    reportBatchResult(response, '批量禁用')
    clearSelection()
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量禁用失败')
    }
  } finally {
    batchLoading.value = false
  }
}

const handleBatchEnable = async () => {
  const ids = selectedUsers.value.map(user => user.id)
  try {
    await ElMessageBox.confirm(`确定要批量启用 ${ids.length} 个用户吗？`, '批量启用', {
      confirmButtonText: '启用',
      cancelButtonText: '取消',
      type: 'success'
    })
    batchLoading.value = true
    const response = await batchEnableUsers(ids)
    reportBatchResult(response, '批量启用')
    clearSelection()
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量启用失败')
    }
  } finally {
    batchLoading.value = false
  }
}

const handleBatchDelete = async () => {
  const ids = selectedUsers.value.map(user => user.id)
  try {
    await ElMessageBox.confirm(
      `确定要批量删除 ${ids.length} 个用户吗？该操作不可恢复，用户的文章、评论等数据将一并删除。`,
      '批量删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    batchLoading.value = true
    const response = await batchDeleteUsers(ids)
    reportBatchResult(response, '批量删除')
    clearSelection()
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量删除失败')
    }
  } finally {
    batchLoading.value = false
  }
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

    <div class="ad-kpi-row">
      <div v-for="kpi in kpis" :key="kpi.key" class="ad-kpi">
        <span class="ad-kpi__icon" :class="kpi.tone">
          <font-awesome-icon :icon="kpi.icon" />
        </span>
        <div>
          <div class="ad-kpi__num">{{ kpi.value }}</div>
          <div class="ad-kpi__label">{{ kpi.label }}</div>
        </div>
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
      <div class="admin-section-title ad-list-head">
        <span><font-awesome-icon icon="table" /> 用户列表</span>
        <span class="ad-view-toggle">
          <button :class="{ active: viewMode === 'grid' }" title="网格" @click="setView('grid')"><font-awesome-icon icon="th-large" /></button>
          <button :class="{ active: viewMode === 'list' }" title="列表" @click="setView('list')"><font-awesome-icon icon="list" /></button>
        </span>
      </div>

      <transition name="batch-bar">
        <div v-if="selectedUsers.length > 0" class="batch-action-bar">
          <span class="batch-count">已选 <strong>{{ selectedUsers.length }}</strong> 个用户</span>
          <div class="batch-buttons">
            <button class="ad-btn is-success" :disabled="batchLoading" @click="handleBatchEnable">
              <font-awesome-icon icon="check-circle" /> 批量启用
            </button>
            <button class="ad-btn is-warning" :disabled="batchLoading" @click="handleBatchDisable">
              <font-awesome-icon icon="ban" /> 批量禁用
            </button>
            <button class="ad-btn is-danger" :disabled="batchLoading" @click="handleBatchDelete">
              <font-awesome-icon icon="trash" /> 批量删除
            </button>
            <button class="ad-btn" :disabled="batchLoading" @click="clearSelection">
              <font-awesome-icon icon="times" /> 取消选择
            </button>
          </div>
        </div>
      </transition>

      <div v-loading="loading || batchLoading">
        <div v-if="displayedUsers.length" class="ad-card-grid" :class="{ 'is-list': viewMode === 'list' }">
          <div
            v-for="row in displayedUsers"
            :key="row.id"
            class="ad-card"
            :class="{ 'is-selected': isSelected(row) }"
          >
            <el-checkbox class="ad-checkbox" :model-value="isSelected(row)" @change="toggleSelect(row)" />

            <div class="ad-card__head">
              <span class="ad-card__avatar" :style="{ backgroundColor: getAvatarColor(row.name) }">
                {{ getAvatarText(row.name) }}
              </span>
              <div style="min-width:0;flex:1;">
                <div class="ad-card__title">{{ row.name }}</div>
                <div class="ad-card__sub">
                  <span><font-awesome-icon icon="envelope" /> {{ row.email || '未设置邮箱' }}</span>
                </div>
              </div>
            </div>

            <div class="ad-card__pills">
              <span class="ad-pill" :class="row.role === 'ADMIN' || row.role === 'SUPER_ADMIN' ? 'is-danger' : 'is-accent'">
                <font-awesome-icon :icon="row.role === 'ADMIN' || row.role === 'SUPER_ADMIN' ? 'user-shield' : 'user'" />
                {{ row.roleLabel }}
              </span>
              <span class="ad-pill" :class="row.status === 'ACTIVE' ? 'is-success' : 'is-danger'">
                <font-awesome-icon :icon="row.status === 'ACTIVE' ? 'circle-check' : 'ban'" />
                {{ row.statusLabel }}
              </span>
            </div>

            <div class="ad-card__metrics">
              <div class="ad-metric">
                <span class="ad-metric__num">{{ row.loginCount }}</span>
                <span class="ad-metric__label">登录次数</span>
              </div>
              <div class="ad-metric">
                <span class="ad-metric__num">{{ row.lastLoginText }}</span>
                <span class="ad-metric__label">最近登录</span>
              </div>
            </div>

            <div class="ad-card__actions">
              <button
                v-if="row.role !== 'SUPER_ADMIN'"
                class="ad-btn"
                :class="row.status === 'ACTIVE' ? 'is-danger' : 'is-success'"
                @click="toggleUserStatus(row)"
              >
                <font-awesome-icon :icon="row.status === 'ACTIVE' ? 'ban' : 'check-circle'" />
                {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
              </button>
              <button
                v-if="row.role !== 'SUPER_ADMIN'"
                class="ad-btn"
                :class="row.role === 'ADMIN' ? 'is-warning' : 'is-primary'"
                @click="toggleUserRole(row)"
              >
                <font-awesome-icon :icon="row.role === 'ADMIN' ? 'user-minus' : 'user-plus'" />
                {{ row.role === 'ADMIN' ? '设为用户' : '设为管理员' }}
              </button>
              <button class="ad-btn" @click="viewUserProfile(row)">
                <font-awesome-icon icon="user" /> 查看
              </button>
              <button v-if="row.role !== 'SUPER_ADMIN'" class="ad-btn" @click="sendMessage(row)">
                <font-awesome-icon icon="paper-plane" /> 私信
              </button>
              <button v-if="row.role !== 'SUPER_ADMIN'" class="ad-btn is-warning" @click="handleResetPassword(row)">
                <font-awesome-icon icon="lock" /> 重置密码
              </button>
              <button v-if="row.role !== 'SUPER_ADMIN'" class="ad-btn is-danger" @click="handleDeleteUser(row)">
                <font-awesome-icon icon="trash" /> 删除
              </button>
              <span v-if="row.role === 'SUPER_ADMIN'" class="ad-superadmin-note">
                <font-awesome-icon icon="user-shield" /> 最高权限账号，受保护
              </span>
            </div>
          </div>
        </div>

        <div v-else-if="!loading" class="ad-empty">
          <div class="ad-empty__icon"><font-awesome-icon icon="users" /></div>
          <div class="ad-empty__text">没有符合条件的用户</div>
        </div>
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
  flex: 1 1 240px;
  min-width: 200px;
  max-width: 360px;
}

.filter-select {
  flex: 0 1 150px;
  min-width: 130px;
  max-width: 170px;
}

.ad-superadmin-note {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ad-accent, #007aff);
  background: var(--ad-accent-weak, rgba(0, 122, 255, 0.1));
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
  font-weight: 750;
  color: #263246;
  margin-bottom: 0.5rem;
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
  color: #8190a3;
  font-size: 0.82rem;
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
  color: #667085;
  font-size: 0.82rem;
  padding: 0.375rem 0.75rem;
  background: #f7fafd;
  border-radius: 999px;
  border: 1px solid rgba(127, 149, 176, 0.16);
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

.batch-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 14px;
  border-radius: 16px;
  background: #f2f6ff;
  border: 1px solid rgba(65, 105, 216, 0.18);
}

.batch-count {
  color: #4169d8;
  font-size: 0.88rem;
  font-weight: 700;
}

.batch-count strong {
  font-size: 1.05rem;
}

.batch-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.batch-bar-enter-active,
.batch-bar-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.batch-bar-enter-from,
.batch-bar-leave-to {
  opacity: 0;
  transform: translateY(-6px);
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
