<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveTag,
  createTag,
  deleteTag as deleteTagApi,
  getAllTags,
  rejectTag,
  updateTag as updateTagApi
} from '@/api/admin'

const DEFAULT_TAG_COLOR = '#8a8175'
const loading = ref(false)
const tags = ref([])
// 视图模式：标签默认列表（横排）
const viewMode = ref(localStorage.getItem('adminView_tags') || 'list')
const setView = (v) => { viewMode.value = v; localStorage.setItem('adminView_tags', v) }
const tagForm = ref({
  name: '',
  description: '',
  color: DEFAULT_TAG_COLOR
})
const dialogVisible = ref(false)
const editingTagId = ref(null)
const searchQuery = ref('')
const filterOptions = ref({
  status: ''
})
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusMap = {
  approved: { text: '已通过', class: 'status-approved', type: 'success' },
  rejected: { text: '已拒绝', class: 'status-rejected', type: 'danger' },
  pending: { text: '审核中', class: 'status-pending', type: 'warning' }
}

const normalizePage = (response) => {
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

const normalizeTag = (tag) => ({
  id: tag.id,
  name: tag.name || '',
  description: tag.description || '',
  color: tag.color || DEFAULT_TAG_COLOR,
  count: tag.usageCount || tag.count || 0,
  status: String(tag.status || 'PENDING').toLowerCase(),
  createTime: formatDateTime(tag.createdAt || tag.createTime),
  createdBy: {
    name: tag.createdBy?.name || '系统'
  }
})

const filteredTags = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()
  if (!keyword) return tags.value

  return tags.value.filter(tag =>
    tag.name.toLowerCase().includes(keyword) ||
    tag.description.toLowerCase().includes(keyword)
  )
})

const getStatusMeta = (status) => statusMap[status] || statusMap.pending

const loadTags = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }

    if (filterOptions.value.status) {
      params.status = filterOptions.value.status.toUpperCase()
    }

    const response = await getAllTags(params)
    const page = normalizePage(response)
    tags.value = page.content.map(normalizeTag)
    total.value = page.totalElements
  } catch (error) {
    tags.value = []
    total.value = 0
    ElMessage.error(error.message || '加载标签列表失败')
  } finally {
    loading.value = false
  }
}

const openCreateTag = () => {
  editingTagId.value = null
  tagForm.value = {
    name: '',
    description: '',
    color: DEFAULT_TAG_COLOR
  }
  dialogVisible.value = true
}

const editTag = (tag) => {
  editingTagId.value = tag.id
  tagForm.value = {
    name: tag.name,
    description: tag.description,
    color: tag.color
  }
  dialogVisible.value = true
}

const saveTag = async () => {
  if (!tagForm.value.name.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }

  try {
    if (editingTagId.value) {
      await updateTagApi(editingTagId.value, tagForm.value)
      ElMessage.success('标签已更新')
    } else {
      await createTag(tagForm.value)
      ElMessage.success('标签已创建')
    }

    dialogVisible.value = false
    loadTags()
  } catch (error) {
    ElMessage.error(error.message || '保存标签失败')
  }
}

const updateTagStatus = async (tag, status) => {
  const statusText = {
    approved: '通过',
    rejected: '拒绝'
  }

  try {
    await ElMessageBox.confirm(
      `确定要${statusText[status]}标签「${tag.name}」的审核吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: status === 'approved' ? 'success' : 'warning'
      }
    )

    if (status === 'approved') {
      await approveTag(tag.id)
    } else {
      await rejectTag(tag.id)
    }

    ElMessage.success(`已${statusText[status]}标签审核`)
    loadTags()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '更新标签状态失败')
    }
  }
}

const deleteTag = async (tag) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除标签「${tag.name}」吗？此操作不可恢复！`,
      '确认操作',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteTagApi(tag.id)
    ElMessage.success('标签已删除')
    loadTags()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除标签失败')
    }
  }
}

const handleFilter = () => {
  currentPage.value = 1
  loadTags()
}

const resetFilters = () => {
  searchQuery.value = ''
  filterOptions.value.status = ''
  currentPage.value = 1
  loadTags()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadTags()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadTags()
}

onMounted(loadTags)
</script>

<template>
  <div class="tag-management admin-page-container">
    <div class="management-header">
      <div class="search-filter">
        <el-input
          v-model="searchQuery"
          placeholder="搜索标签名称..."
          clearable
        >
          <template #prefix>
            <font-awesome-icon icon="search" />
          </template>
        </el-input>
        
        <el-select v-model="filterOptions.status" placeholder="审核状态" clearable>
          <el-option label="全部状态" value="" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
          <el-option label="审核中" value="pending" />
        </el-select>

        <el-button type="primary" @click="handleFilter">
          <font-awesome-icon icon="filter" /> 筛选
        </el-button>

        <el-button @click="resetFilters">
          <font-awesome-icon icon="sync" /> 重置
        </el-button>

        <el-button type="primary" @click="openCreateTag">
          <font-awesome-icon icon="plus" /> 新增标签
        </el-button>
      </div>
    </div>

    <div class="table-container">
      <div class="ad-list-head" style="margin-bottom:14px;">
        <div class="admin-section-title" style="margin:0;">
          <font-awesome-icon icon="tags" /> 标签列表
        </div>
        <span class="ad-view-toggle">
          <button :class="{ active: viewMode === 'grid' }" title="网格" @click="setView('grid')"><font-awesome-icon icon="th-large" /></button>
          <button :class="{ active: viewMode === 'list' }" title="列表" @click="setView('list')"><font-awesome-icon icon="list" /></button>
        </span>
      </div>
      <div v-loading="loading">
        <div v-if="filteredTags.length" class="ad-card-grid" :class="{ 'is-list': viewMode === 'list' }">
          <div v-for="row in filteredTags" :key="row.id" class="ad-card">
            <div class="ad-card__head">
              <span class="ad-card__avatar"><font-awesome-icon icon="hashtag" /></span>
              <div style="min-width:0;flex:1;">
                <div class="ad-card__title">{{ row.name }}</div>
                <div class="ad-card__sub">
                  <span><font-awesome-icon icon="user" /> {{ row.createdBy?.name || '系统' }}</span>
                  <span><font-awesome-icon icon="clock" /> {{ row.createTime }}</span>
                </div>
              </div>
            </div>

            <p v-if="row.description" class="tag-desc">{{ row.description }}</p>

            <div class="ad-card__pills">
              <span class="ad-pill" :class="getStatusMeta(row.status).type === 'success' ? 'is-success' : getStatusMeta(row.status).type === 'danger' ? 'is-danger' : 'is-warning'">
                {{ getStatusMeta(row.status).text }}
              </span>
              <span class="ad-pill is-accent"><font-awesome-icon icon="hashtag" /> {{ row.count }} 次使用</span>
            </div>

            <div class="ad-card__actions">
              <button v-if="row.status === 'pending'" class="ad-btn is-success" @click="updateTagStatus(row, 'approved')">
                <font-awesome-icon icon="check" /> 通过
              </button>
              <button v-if="row.status === 'pending'" class="ad-btn is-danger" @click="updateTagStatus(row, 'rejected')">
                <font-awesome-icon icon="times" /> 拒绝
              </button>
              <button
                class="ad-icon-action is-edit"
                title="编辑标签"
                aria-label="编辑标签"
                @click="editTag(row)"
              >
                <font-awesome-icon icon="edit" />
              </button>
              <button
                v-if="row.count === 0"
                class="ad-icon-action is-delete"
                title="删除标签"
                aria-label="删除标签"
                @click="deleteTag(row)"
              >
                <font-awesome-icon icon="trash-alt" />
              </button>
            </div>
          </div>
        </div>

        <div v-else-if="!loading" class="ad-empty">
          <div class="ad-empty__icon"><font-awesome-icon icon="tags" /></div>
          <div class="ad-empty__text">没有符合条件的标签</div>
        </div>
      </div>

      <div class="pagination-container">
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

    <!-- 新增/编辑标签对话框 -->
    <el-dialog
      :title="editingTagId ? '编辑标签' : '新增标签'"
      v-model="dialogVisible"
      width="500px"
    >
      <el-form :model="tagForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="tagForm.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="tagForm.description"
            type="textarea"
            rows="3"
            placeholder="请输入标签描述"
          />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="tagForm.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveTag">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tag-desc {
  margin: 0;
  color: var(--ad-muted);
  font-size: 0.86rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tag-management {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  min-height: calc(100vh - 134px);
}

.management-header {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--ad-border);
}

.search-filter {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.search-filter .el-input {
  flex: 2 1 220px;
  min-width: 180px;
  max-width: 340px;
}

.search-filter .el-select {
  flex: 1 1 130px;
  min-width: 120px;
  max-width: 160px;
}

.tag-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 0;
}

.tag-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tag-name {
  font-size: 14px;
  font-weight: 500;
}

.tag-description {
  margin: 0;
  font-size: 13px;
  color: var(--ad-muted);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.tag-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ad-muted);
  font-size: 13px;
}

.time-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ad-muted);
  font-size: 13px;
}

.stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ad-muted);
  font-size: 13px;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.button-row {
  display: flex;
  gap: 8px;
}

.action-buttons .el-button {
  padding: 6px 12px;
  font-size: 13px;
}

.action-buttons .el-button [class^="fa"] {
  margin-right: 4px;
}

.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid var(--ad-border);
}

.table-container {
  flex: 1;
  overflow: auto;
  margin: 24px 0;
  background: var(--ad-surface);
  border-radius: 12px;
}

:deep(.el-table) {
  flex: 1;
  overflow: hidden;
}

:deep(.el-table__body-wrapper) {
  overflow-y: auto;
}

:deep(.el-table--border) {
  border-radius: 8px;
  border: 1px solid var(--ad-border);
}

:deep(.el-table th.el-table__cell) {
  background-color: var(--ad-surface-muted);
  color: var(--ad-text);
  font-weight: 600;
  padding: 12px 8px;
}

:deep(.el-table td.el-table__cell) {
  padding: 8px;
}

:deep(.el-tag) {
  border-radius: 4px;
  font-weight: 500;
}

@media (max-width: 1200px) {
  .search-filter {
    gap: 12px;
  }
  
  .search-filter .el-input {
    width: 250px;
  }
  
  .search-filter .el-select {
    width: 110px;
  }
}
</style> 
