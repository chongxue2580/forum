<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAllUsers,
  getUserDetail,
  banUser,
  unbanUser,
  resetUserPassword,
  changeUserRole,
  deleteUser,
  searchUsers,
  batchBanUsers,
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
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const selectedUserDetail = ref(null)
const banDialogVisible = ref(false)
const banSubmitting = ref(false)
const banTarget = ref(null)
const banMode = ref('single')
const banForm = ref({
  banType: 'LOGIN',
  durationMode: 'permanent',
  durationDays: 7,
  reason: ''
})

const displayedUsers = computed(() => users.value)

const banTypeOptions = [
  { label: '禁止登录', value: 'LOGIN', icon: 'lock' },
  { label: '禁止发布内容', value: 'CONTENT', icon: 'edit' }
]

// KPI（总数取后端 total，其余按当前页统计）
const kpis = computed(() => {
  const list = users.value
  const admins = list.filter(u => u.role === 'ADMIN' || u.role === 'SUPER_ADMIN').length
  const active = list.filter(u => u.status === 'ACTIVE' && !u.banned).length
  const banned = list.filter(u => u.banned || u.status === 'DISABLED').length
  return [
    { key: 'total', label: '用户总数', value: total.value, icon: 'users', tone: '' },
    { key: 'active', label: '正常（本页）', value: active, icon: 'circle-check', tone: 'is-success' },
    { key: 'admin', label: '管理员（本页）', value: admins, icon: 'user-shield', tone: '' },
    { key: 'banned', label: '封禁（本页）', value: banned, icon: 'ban', tone: 'is-danger' }
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
  { label: '已禁用', value: 'DISABLED' },
  { label: '锁定', value: 'LOCKED' }
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

const formatValueDateTime = (value) => {
  if (!value) return '-'
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

const getBanTypeLabel = (type) => {
  const normalized = String(type || 'NONE').toUpperCase()
  if (normalized === 'LOGIN') return '禁止登录'
  if (normalized === 'CONTENT') return '禁止发布'
  return '未封禁'
}

const getBanExpiresText = (value) => (value ? formatValueDateTime(value) : '永久')

const normalizeUser = (user) => {
  const role = String(user.role || 'USER').toUpperCase()
  const status = String(user.status || 'ACTIVE').toUpperCase()
  const banType = String(user.banType || 'NONE').toUpperCase()
  const banned = Boolean(user.banned || status !== 'ACTIVE' || banType !== 'NONE')
  return {
    ...user,
    name: user.nickname || user.username || `用户${user.id}`,
    role,
    status,
    banType,
    banned,
    roleLabel: roleOptions.find(option => option.value === role)?.label || role,
    statusLabel: statusOptions.find(option => option.value === status)?.label || status,
    banTypeLabel: getBanTypeLabel(banType),
    banExpiresText: getBanExpiresText(user.banExpiresAt),
    lastLoginText: formatDateTime(user.lastLoginTime),
    createdAtText: formatDateTime(user.createdAt),
    loginCount: user.loginCount || 0
  }
}

const getAvatarColor = (name) => {
  const colors = [
    '#596f8f',
    '#4f6f5a',
    '#8b6f3e',
    '#8a8175',
    '#9d4a3f'
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

const resetBanForm = () => {
  banForm.value = {
    banType: 'LOGIN',
    durationMode: 'permanent',
    durationDays: 7,
    reason: ''
  }
}

const openBanDialog = (user = null) => {
  resetBanForm()
  banTarget.value = user
  banMode.value = user ? 'single' : 'batch'
  banDialogVisible.value = true
}

const handleUnban = async (user) => {
  try {
    await ElMessageBox.confirm(`确定要解封用户「${user.name}」吗？`, '解封用户', {
      confirmButtonText: '解封',
      cancelButtonText: '取消',
      type: 'success'
    })
    await unbanUser(user.id)
    ElMessage.success('用户已解封')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '解封用户失败')
    }
  }
}

const submitBan = async () => {
  const reason = banForm.value.reason.trim()
  if (!reason) {
    ElMessage.warning('请输入封禁理由')
    return
  }
  if (banForm.value.durationMode === 'days' && (!banForm.value.durationDays || banForm.value.durationDays < 1)) {
    ElMessage.warning('请输入有效的封禁天数')
    return
  }

  const payload = {
    banType: banForm.value.banType,
    reason,
    durationDays: banForm.value.durationMode === 'days' ? Number(banForm.value.durationDays) : null
  }

  try {
    banSubmitting.value = true
    if (banMode.value === 'batch') {
      const ids = selectedUsers.value.map(user => user.id)
      const response = await batchBanUsers(ids, payload)
      reportBatchResult(response, '批量封禁')
      clearSelection()
    } else {
      await banUser(banTarget.value.id, payload)
      ElMessage.success('用户已封禁')
    }
    banDialogVisible.value = false
    loadUsers()
  } catch (error) {
    ElMessage.error(error.message || '封禁用户失败')
  } finally {
    banSubmitting.value = false
  }
}

const toggleUserStatus = (user) => {
  if (user.banned || user.status !== 'ACTIVE') {
    handleUnban(user)
  } else {
    openBanDialog(user)
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

const openUserDetail = async (user) => {
  detailDialogVisible.value = true
  detailLoading.value = true
  selectedUserDetail.value = null
  try {
    const response = await getUserDetail(user.id)
    const detail = response?.data || response
    selectedUserDetail.value = {
      ...detail,
      name: detail.nickname || detail.username || `用户${detail.id}`,
      roleLabel: roleOptions.find(option => option.value === String(detail.role || '').toUpperCase())?.label || detail.role,
      statusLabel: statusOptions.find(option => option.value === String(detail.status || '').toUpperCase())?.label || detail.status,
      banTypeLabel: getBanTypeLabel(detail.banType),
      banExpiresText: getBanExpiresText(detail.banExpiresAt),
      lastLoginText: formatDateTime(detail.lastLoginTime),
      lastAccessText: detail.lastAccessTime ? formatValueDateTime(detail.lastAccessTime) : '暂无记录',
      createdAtText: formatValueDateTime(detail.createdAt),
      updatedAtText: formatValueDateTime(detail.updatedAt)
    }
  } catch (error) {
    ElMessage.error(error.message || '获取用户详情失败')
    detailDialogVisible.value = false
  } finally {
    detailLoading.value = false
  }
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
  openBanDialog()
}

const handleBatchEnable = async () => {
  const ids = selectedUsers.value.map(user => user.id)
  try {
    await ElMessageBox.confirm(`确定要批量解封 ${ids.length} 个用户吗？`, '批量解封', {
      confirmButtonText: '解封',
      cancelButtonText: '取消',
      type: 'success'
    })
    batchLoading.value = true
    const response = await batchEnableUsers(ids)
    reportBatchResult(response, '批量解封')
    clearSelection()
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量解封失败')
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
          <el-option label="锁定" value="LOCKED" />
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
              <font-awesome-icon icon="check-circle" /> 批量解封
            </button>
            <button class="ad-btn is-warning" :disabled="batchLoading" @click="handleBatchDisable">
              <font-awesome-icon icon="ban" /> 批量封禁
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
              <span v-if="row.banned" class="ad-pill is-warning">
                <font-awesome-icon icon="ban" />
                {{ row.banTypeLabel }} · {{ row.banExpiresText }}
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
	                :class="row.banned || row.status !== 'ACTIVE' ? 'is-success' : 'is-danger'"
	                @click="toggleUserStatus(row)"
	              >
	                <font-awesome-icon :icon="row.banned || row.status !== 'ACTIVE' ? 'check-circle' : 'ban'" />
	                {{ row.banned || row.status !== 'ACTIVE' ? '解封' : '封禁' }}
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
	              <button class="ad-btn" @click="openUserDetail(row)">
	                <font-awesome-icon icon="user" /> 查看详情
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

    <el-dialog
      v-model="detailDialogVisible"
      title="用户详情"
      width="760px"
      class="admin-user-detail-dialog"
    >
      <div v-loading="detailLoading" class="detail-dialog-body">
        <template v-if="selectedUserDetail">
          <div class="detail-profile-head">
            <span class="ad-card__avatar" :style="{ backgroundColor: getAvatarColor(selectedUserDetail.name) }">
              {{ getAvatarText(selectedUserDetail.name) }}
            </span>
            <div>
              <h3>{{ selectedUserDetail.name }}</h3>
              <p>ID {{ selectedUserDetail.id }} · {{ selectedUserDetail.username }}</p>
            </div>
          </div>

          <div class="detail-section">
            <h4>基本信息</h4>
            <div class="detail-info-grid">
              <div><span>邮箱</span><strong>{{ selectedUserDetail.email || '-' }}</strong></div>
              <div><span>角色</span><strong>{{ selectedUserDetail.roleLabel }}</strong></div>
              <div><span>状态</span><strong>{{ selectedUserDetail.statusLabel }}</strong></div>
              <div><span>昵称</span><strong>{{ selectedUserDetail.nickname || '-' }}</strong></div>
              <div class="detail-span-2"><span>个人简介</span><strong>{{ selectedUserDetail.bio || '-' }}</strong></div>
            </div>
          </div>

          <div class="detail-section">
            <h4>安全与封禁</h4>
            <div class="detail-info-grid">
              <div><span>二次验证</span><strong>{{ selectedUserDetail.twoFactorEnabled ? '已开启' : '未开启' }}</strong></div>
              <div><span>登录次数</span><strong>{{ selectedUserDetail.loginCount || 0 }}</strong></div>
              <div><span>封禁状态</span><strong>{{ selectedUserDetail.banned ? selectedUserDetail.banTypeLabel : '未封禁' }}</strong></div>
              <div><span>封禁期限</span><strong>{{ selectedUserDetail.banned ? selectedUserDetail.banExpiresText : '-' }}</strong></div>
              <div class="detail-span-2"><span>封禁理由</span><strong>{{ selectedUserDetail.banReason || '-' }}</strong></div>
              <div><span>执行管理员邮箱</span><strong>{{ selectedUserDetail.bannedByEmail || '-' }}</strong></div>
              <div><span>封禁时间</span><strong>{{ formatValueDateTime(selectedUserDetail.bannedAt) }}</strong></div>
            </div>
          </div>

          <div class="detail-section">
            <h4>登录与访问</h4>
            <div class="detail-info-grid">
              <div><span>最近登录</span><strong>{{ selectedUserDetail.lastLoginText }}</strong></div>
              <div><span>在线状态</span><strong>{{ selectedUserDetail.online ? '可能在线' : '离线' }}</strong></div>
              <div><span>最近访问 IP</span><strong>{{ selectedUserDetail.lastKnownIp || '暂无记录' }}</strong></div>
              <div><span>最近访问时间</span><strong>{{ selectedUserDetail.lastAccessText }}</strong></div>
              <div class="detail-span-2"><span>访问设备</span><strong>{{ selectedUserDetail.lastKnownUserAgent || '暂无记录' }}</strong></div>
              <div><span>注册时间</span><strong>{{ selectedUserDetail.createdAtText }}</strong></div>
              <div><span>更新时间</span><strong>{{ selectedUserDetail.updatedAtText }}</strong></div>
            </div>
          </div>

          <div class="detail-section">
            <h4>内容与关系</h4>
            <div class="detail-stat-row">
              <div><strong>{{ selectedUserDetail.articleCount || 0 }}</strong><span>文章</span></div>
              <div><strong>{{ selectedUserDetail.questionCount || 0 }}</strong><span>问题</span></div>
              <div><strong>{{ selectedUserDetail.commentCount || 0 }}</strong><span>评论</span></div>
              <div><strong>{{ selectedUserDetail.likeCount || 0 }}</strong><span>点赞</span></div>
              <div><strong>{{ selectedUserDetail.followingCount || 0 }}</strong><span>关注</span></div>
              <div><strong>{{ selectedUserDetail.followerCount || 0 }}</strong><span>粉丝</span></div>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <button class="ad-btn" @click="detailDialogVisible = false">关闭</button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="banDialogVisible"
      :title="banMode === 'batch' ? '批量封禁用户' : `封禁用户：${banTarget?.name || ''}`"
      width="560px"
      class="admin-user-ban-dialog"
    >
      <div class="ban-form">
        <label>封禁方式</label>
        <div class="ban-type-options">
          <button
            v-for="option in banTypeOptions"
            :key="option.value"
            type="button"
            class="ban-type-option"
            :class="{ active: banForm.banType === option.value }"
            @click="banForm.banType = option.value"
          >
            <font-awesome-icon :icon="option.icon" />
            <span>{{ option.label }}</span>
          </button>
        </div>

        <label>封禁期限</label>
        <el-radio-group v-model="banForm.durationMode" class="duration-radio-group">
          <el-radio-button label="permanent">永久</el-radio-button>
          <el-radio-button label="days">按天数</el-radio-button>
        </el-radio-group>
        <el-input-number
          v-if="banForm.durationMode === 'days'"
          v-model="banForm.durationDays"
          :min="1"
          :max="3650"
          controls-position="right"
          class="duration-input"
        />

        <label>封禁理由</label>
        <el-input
          v-model="banForm.reason"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请输入封禁理由，用户登录或操作受限时会看到该原因"
        />
      </div>
      <template #footer>
        <button class="ad-btn" :disabled="banSubmitting" @click="banDialogVisible = false">取消</button>
        <button class="ad-btn is-danger" :disabled="banSubmitting" @click="submitBan">
          <font-awesome-icon v-if="banSubmitting" icon="sync" spin />
          <font-awesome-icon v-else icon="ban" />
          确认封禁
        </button>
      </template>
    </el-dialog>
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
  color: var(--ad-brand-strong);
  background: var(--ad-brand-soft);
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
  color: var(--ad-text);
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
  color: var(--ad-muted);
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
  color: var(--ad-muted);
  font-size: 0.82rem;
  padding: 0.375rem 0.75rem;
  background: var(--ad-surface-muted);
  border-radius: 999px;
  border: 1px solid var(--ad-border);
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
  background: var(--ad-brand-soft);
  border: 1px solid var(--ad-border);
}

.batch-count {
  color: var(--ad-brand-strong);
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

.detail-dialog-body {
  min-height: 180px;
}

.detail-profile-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 4px 2px 18px;
  border-bottom: 1px solid var(--ad-border);
}

.detail-profile-head .ad-card__avatar {
  width: 54px;
  height: 54px;
  flex: 0 0 54px;
  font-size: 1.25rem;
}

.detail-profile-head h3 {
  margin: 0 0 4px;
  color: var(--ad-text);
  font-size: 1.15rem;
  line-height: 1.25;
}

.detail-profile-head p {
  margin: 0;
  color: var(--ad-muted);
  font-size: 0.86rem;
}

.detail-section {
  padding: 18px 0 2px;
}

.detail-section + .detail-section {
  border-top: 1px solid var(--ad-border);
}

.detail-section h4 {
  margin: 0 0 12px;
  color: var(--ad-text);
  font-size: 0.95rem;
  font-weight: 750;
}

.detail-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

.detail-info-grid > div {
  min-width: 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--ad-surface-muted);
  border: 1px solid var(--ad-border);
}

.detail-info-grid span,
.detail-stat-row span {
  display: block;
  margin-bottom: 5px;
  color: var(--ad-muted);
  font-size: 0.78rem;
  line-height: 1.3;
}

.detail-info-grid strong {
  display: block;
  min-width: 0;
  color: var(--ad-text);
  font-size: 0.9rem;
  font-weight: 650;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.detail-span-2 {
  grid-column: span 2;
}

.detail-stat-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.detail-stat-row > div {
  min-width: 0;
  padding: 12px 8px;
  text-align: center;
  border-radius: 10px;
  background: var(--ad-surface-muted);
  border: 1px solid var(--ad-border);
}

.detail-stat-row strong {
  display: block;
  margin-bottom: 4px;
  color: var(--ad-brand-strong);
  font-size: 1.08rem;
  line-height: 1.2;
}

.ban-form {
  display: grid;
  gap: 12px;
}

.ban-form label {
  color: var(--ad-text);
  font-size: 0.88rem;
  font-weight: 700;
}

.ban-type-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.ban-type-option {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--ad-border);
  color: var(--ad-muted);
  background: var(--ad-surface);
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease;
}

.ban-type-option:hover,
.ban-type-option.active {
  color: var(--ad-danger);
  background: var(--ad-danger-soft);
  border-color: var(--ad-danger);
}

.duration-radio-group {
  display: flex;
}

.duration-input {
  width: 180px;
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

  .detail-info-grid,
  .detail-stat-row,
  .ban-type-options {
    grid-template-columns: 1fr;
  }

  .detail-span-2 {
    grid-column: auto;
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
