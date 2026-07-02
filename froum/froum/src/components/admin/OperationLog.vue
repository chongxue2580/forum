<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import '@/components/admin/AdminCommonStyles.css'
import { getOperationLogs } from '@/api/admin'

const loading = ref(false)
const logs = ref([])
// 视图模式：日志默认列表（横排）
const viewMode = ref(localStorage.getItem('adminView_logs') || 'list')
const setView = (v) => { viewMode.value = v; localStorage.setItem('adminView_logs', v) }
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const searchQuery = ref('')
const filterOptions = reactive({
  type: '',
  action: '',
  dateRange: []
})

const typeMap = {
  USER_LOGIN: { text: '管理员登录', icon: 'sign-in-alt' },
  USER_MANAGEMENT: { text: '用户管理', icon: 'users' },
  ARTICLE_MANAGEMENT: { text: '文章管理', icon: 'file-alt' },
  QUESTION_MANAGEMENT: { text: '问答管理', icon: 'question-circle' },
  CATEGORY_MANAGEMENT: { text: '分类管理', icon: 'folder' },
  TAG_MANAGEMENT: { text: '标签管理', icon: 'tags' },
  COMMENT_MANAGEMENT: { text: '评论管理', icon: 'comment' },
  SYSTEM_SETTINGS: { text: '系统设置', icon: 'cog' },
  ARTICLE_CREATE: { text: '文章创建', icon: 'file-alt' },
  QUESTION_CREATE: { text: '问答创建', icon: 'question-circle' }
}

const actionMap = {
  login: { text: '登录', type: 'primary' },
  create: { text: '创建', type: 'success' },
  update: { text: '更新', type: 'warning' },
  delete: { text: '删除', type: 'danger' },
  approve: { text: '通过', type: 'success' },
  reject: { text: '拒绝', type: 'danger' },
  disable: { text: '禁用', type: 'danger' },
  enable: { text: '启用', type: 'success' },
  reset_password: { text: '重置密码', type: 'warning' },
  change_role: { text: '改角色', type: 'warning' },
  pin: { text: '置顶', type: 'primary' },
  unpin: { text: '取消置顶', type: 'info' },
  feature: { text: '精选', type: 'primary' },
  unfeature: { text: '取消精选', type: 'info' },
  solve: { text: '已解决', type: 'success' },
  unsolve: { text: '未解决', type: 'info' },
  reset: { text: '重置', type: 'warning' },
  email_test: { text: '邮件测试', type: 'primary' },
  clear_cache: { text: '清缓存', type: 'warning' },
  other: { text: '其他', type: 'info' }
}

const unwrapPage = (response) => {
  const page = response?.data || response || {}
  return {
    content: Array.isArray(page.content) ? page.content : Array.isArray(page) ? page : [],
    totalElements: page.totalElements || page.total || 0
  }
}

const formatDateTime = (value) => {
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

const normalizeLog = (log) => ({
  id: log.id,
  operator: {
    id: log.operator?.id,
    name: log.operator?.name || '系统'
  },
  type: log.type || 'SYSTEM',
  action: log.action || 'other',
  target: {
    type: log.target?.type || '-',
    id: log.target?.id,
    name: log.target?.name || '-'
  },
  detail: log.detail || '-',
  ip: log.ip || '-',
  userAgent: log.userAgent || '',
  createTime: formatDateTime(log.createTime || log.createdAt)
})

const getAvatarColor = (name) => {
  const colors = [
    '#596f8f',
    '#4f6f5a',
    '#8b6f3e',
    '#8a8175',
    '#9d4a3f'
  ]
  const safeName = name || '系统'
  const index = safeName.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % colors.length
  return colors[index]
}

const getAvatarText = (name) => (name || '系').charAt(0)
const getTypeInfo = (type) => typeMap[type] || { text: type || '其他操作', icon: 'info-circle' }
const getActionInfo = (action) => actionMap[action] || { text: action || '其他', type: 'info' }

const loadLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchQuery.value.trim()) params.keyword = searchQuery.value.trim()
    if (filterOptions.type) params.type = filterOptions.type
    if (filterOptions.action) params.action = filterOptions.action
    if (filterOptions.dateRange?.length === 2) {
      params.startDate = filterOptions.dateRange[0]
      params.endDate = filterOptions.dateRange[1]
    }

    const page = unwrapPage(await getOperationLogs(params))
    logs.value = page.content.map(normalizeLog)
    total.value = page.totalElements
  } catch (error) {
    logs.value = []
    total.value = 0
    ElMessage.error(error.message || '加载操作日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadLogs()
}

const resetFilters = () => {
  searchQuery.value = ''
  filterOptions.type = ''
  filterOptions.action = ''
  filterOptions.dateRange = []
  currentPage.value = 1
  loadLogs()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadLogs()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadLogs()
}

const exportLogs = () => {
  if (!logs.value.length) {
    ElMessage.warning('当前没有可导出的日志')
    return
  }

  const header = ['ID', '操作人', '类型', '动作', '对象类型', '对象ID', '对象名称', '详情', 'IP', '时间']
  const rows = logs.value.map(log => [
    log.id,
    log.operator.name,
    getTypeInfo(log.type).text,
    getActionInfo(log.action).text,
    log.target.type,
    log.target.id || '',
    log.target.name,
    log.detail,
    log.ip,
    log.createTime
  ])
  const csv = [header, ...rows]
    .map(row => row.map(value => `"${String(value ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\n')

  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `operation-logs-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(loadLogs)
</script>

<template>
  <div class="operation-log admin-page-container">
    <div class="admin-page-header">
      <h1 class="admin-page-title">
        <font-awesome-icon icon="clipboard-list" />
        操作日志
      </h1>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadLogs">
          <font-awesome-icon icon="sync" /> 刷新
        </el-button>
        <el-button type="primary" @click="exportLogs">
          <font-awesome-icon icon="download" /> 导出当前页
        </el-button>
      </div>
    </div>

    <div class="filter-panel admin-content-card">
      <el-input
        v-model="searchQuery"
        placeholder="搜索操作人、对象或详情"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <font-awesome-icon icon="search" />
        </template>
      </el-input>

      <el-select v-model="filterOptions.type" placeholder="操作类型" clearable class="filter-select" @change="handleSearch">
        <el-option v-for="(info, type) in typeMap" :key="type" :label="info.text" :value="type" />
      </el-select>

      <el-select v-model="filterOptions.action" placeholder="操作动作" clearable class="filter-select" @change="handleSearch">
        <el-option v-for="(info, action) in actionMap" :key="action" :label="info.text" :value="action" />
      </el-select>

      <el-date-picker
        v-model="filterOptions.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        class="date-picker"
        @change="handleSearch"
      />

      <el-button type="primary" @click="handleSearch">
        <font-awesome-icon icon="search" /> 查询
      </el-button>
      <el-button @click="resetFilters">
        <font-awesome-icon icon="undo" /> 重置
      </el-button>
    </div>

    <div class="ad-list-head" style="margin-bottom:14px;">
      <div class="admin-section-title" style="margin:0;">
        <font-awesome-icon icon="clipboard-list" /> 操作日志
      </div>
      <span class="ad-view-toggle">
        <button :class="{ active: viewMode === 'grid' }" title="网格" @click="setView('grid')"><font-awesome-icon icon="th-large" /></button>
        <button :class="{ active: viewMode === 'list' }" title="列表" @click="setView('list')"><font-awesome-icon icon="list" /></button>
      </span>
    </div>

    <div v-loading="loading">
      <div v-if="logs.length" class="ad-card-grid" :class="{ 'is-list': viewMode === 'list' }">
        <div v-for="row in logs" :key="row.id" class="ad-card">
          <div class="ad-card__head">
            <span class="ad-card__avatar" :style="{ backgroundColor: getAvatarColor(row.operator.name) }">{{ getAvatarText(row.operator.name) }}</span>
            <div style="min-width:0;flex:1;">
              <div class="ad-card__title">{{ row.operator.name }}</div>
              <div class="ad-card__sub">
                <span><font-awesome-icon icon="clock" /> {{ row.createTime }}</span>
                <span><font-awesome-icon icon="network-wired" /> {{ row.ip }}</span>
              </div>
            </div>
          </div>

          <div class="ad-card__pills">
            <span class="ad-pill" :class="getActionInfo(row.action).type === 'success' ? 'is-success' : getActionInfo(row.action).type === 'danger' ? 'is-danger' : getActionInfo(row.action).type === 'warning' ? 'is-warning' : 'is-accent'">
              {{ getActionInfo(row.action).text }}
            </span>
            <span class="ad-pill is-muted">
              <font-awesome-icon :icon="getTypeInfo(row.type).icon" /> {{ getTypeInfo(row.type).text }}
            </span>
            <span class="ad-pill is-muted">{{ row.target.type }} #{{ row.target.id || '-' }}</span>
          </div>

          <p class="log-detail">{{ row.detail }}</p>
        </div>
      </div>

      <div v-else-if="!loading" class="ad-empty">
        <div class="ad-empty__icon"><font-awesome-icon icon="clipboard-list" /></div>
        <div class="ad-empty__text">暂无操作日志</div>
      </div>
    </div>

    <div class="pagination-container">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.log-detail {
  margin: 0;
  color: var(--ad-muted);
  font-size: 0.86rem;
  line-height: 1.5;
  word-break: break-word;
}

.operation-log {
  background: var(--ad-bg);
}

.header-actions,
.filter-panel {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-panel {
  margin-bottom: 16px;
  padding: 16px;
}

.search-input {
  flex: 2 1 220px;
  min-width: 180px;
  max-width: 320px;
}

.filter-select {
  flex: 1 1 130px;
  min-width: 120px;
  max-width: 160px;
}

.date-picker {
  flex: 1 1 240px;
  min-width: 220px;
  max-width: 280px;
}

.log-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px 0;
}

.log-header,
.operator-info,
.operation-detail,
.cell-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}

.log-header {
  justify-content: space-between;
}

.operator-name {
  color: var(--ad-text);
  font-weight: 600;
}

.operation-type,
.target-info,
.detail-text,
.cell-icon {
  color: var(--ad-muted);
  font-size: 13px;
}

.detail-text {
  margin: 0;
  line-height: 1.5;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 0;
}

@media (max-width: 768px) {
  .search-input,
  .filter-select,
  .date-picker {
    width: 100%;
  }
}
</style>
