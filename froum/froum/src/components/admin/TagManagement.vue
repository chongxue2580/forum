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

const loading = ref(false)
const tags = ref([])
const tagForm = ref({
  name: '',
  description: '',
  color: '#1677ff'
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
  color: tag.color || '#1677ff',
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
    console.error('加载标签列表失败:', error)
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
    color: '#1677ff'
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
  <div class="tag-management">
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
      <el-table v-loading="loading" :data="filteredTags" style="width: 100%" border>
        <el-table-column label="标签信息" min-width="300">
          <template #default="{ row }">
            <div class="tag-info">
              <div class="tag-header">
                <el-tag size="medium" class="tag-name">{{ row.name }}</el-tag>
                <p v-if="row.description" class="tag-description">{{ row.description }}</p>
              </div>
              <div class="tag-meta">
                <div class="meta-left">
                  <div class="creator-info">
                    <font-awesome-icon icon="user" />
                    <span>{{ row.createdBy?.name || '系统' }}</span>
                  </div>
                  <div class="time-info">
                    <font-awesome-icon icon="clock" />
                    <span>{{ row.createTime }}</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="使用统计" width="180">
          <template #default="{ row }">
            <div class="stats">
              <div class="stat-item">
                <font-awesome-icon icon="hashtag" />
                <span>{{ row.count }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.status).type">
              {{ getStatusMeta(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <div class="action-buttons">
              <div class="button-row" v-if="row.status === 'pending'">
                <el-button
                  type="success"
                  size="small"
                  @click="updateTagStatus(row, 'approved')"
                >
                  <font-awesome-icon icon="check" /> 通过
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="updateTagStatus(row, 'rejected')"
                >
                  <font-awesome-icon icon="times" /> 拒绝
                </el-button>
              </div>

              <div class="button-row">
                <el-button
                  type="primary"
                  size="small"
                  @click="editTag(row)"
                >
                  <font-awesome-icon icon="edit" /> 编辑
                </el-button>
                <el-button
                  v-if="row.count === 0"
                  type="danger"
                  size="small"
                  @click="deleteTag(row)"
                >
                  <font-awesome-icon icon="trash-alt" /> 删除
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>

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
.tag-management {
  padding: 24px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  min-height: calc(100vh - 134px);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.tag-management:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.management-header {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.search-filter {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.search-filter .el-input {
  width: 300px;
}

.search-filter .el-select {
  width: 140px;
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
  color: #64748b;
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
  color: #64748b;
  font-size: 13px;
}

.time-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
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
  color: #64748b;
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
  border-top: 1px solid #e2e8f0;
}

.table-container {
  flex: 1;
  overflow: auto;
  margin: 24px 0;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
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
  border: 1px solid #e2e8f0;
}

:deep(.el-table th.el-table__cell) {
  background-color: #f8fafc;
  color: #0f172a;
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
