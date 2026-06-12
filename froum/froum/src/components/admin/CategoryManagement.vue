<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveCategory,
  createCategory,
  deleteCategory as deleteCategoryApi,
  getAllCategories,
  rejectCategory,
  updateCategory as updateCategoryApi
} from '@/api/admin'

const loading = ref(false)
const categories = ref([])
const categoryForm = ref({
  name: '',
  icon: '',
  description: '',
  displayOrder: 999
})
const dialogVisible = ref(false)
const editingCategoryId = ref(null)
const searchQuery = ref('')
const filterOptions = ref({
  status: ''
})
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const availableIcons = ['web', 'code', 'mobile-alt', 'robot', 'link', 'gamepad', 'chart-bar', 'search', 'tools', 'flask', 'lock', 'laptop-code', 'broadcast-tower', 'seedling']

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

const normalizeCategory = (category) => ({
  id: category.id,
  name: category.name || '',
  icon: category.icon || 'folder',
  description: category.description || '',
  articleCount: category.articleCount || 0,
  displayOrder: category.displayOrder ?? 999,
  createTime: formatDateTime(category.createdAt || category.createTime),
  status: String(category.status || 'PENDING').toLowerCase(),
  createdBy: {
    name: category.createdBy?.name || '系统'
  }
})

const filteredCategories = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()
  if (!keyword) return categories.value

  return categories.value.filter(category =>
    category.name.toLowerCase().includes(keyword) ||
    category.description.toLowerCase().includes(keyword)
  )
})

const getStatusMeta = (status) => statusMap[status] || statusMap.pending

const loadCategories = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }

    if (filterOptions.value.status) {
      params.status = filterOptions.value.status.toUpperCase()
    }

    const response = await getAllCategories(params)
    const page = normalizePage(response)
    categories.value = page.content.map(normalizeCategory)
    total.value = page.totalElements
  } catch (error) {
    console.error('加载分类列表失败:', error)
    categories.value = []
    total.value = 0
    ElMessage.error(error.message || '加载分类列表失败')
  } finally {
    loading.value = false
  }
}

const openCreateCategory = () => {
  editingCategoryId.value = null
  categoryForm.value = {
    name: '',
    icon: 'folder',
    description: '',
    displayOrder: 999
  }
  dialogVisible.value = true
}

const editCategory = (category) => {
  editingCategoryId.value = category.id
  categoryForm.value = {
    name: category.name,
    icon: category.icon,
    description: category.description,
    displayOrder: category.displayOrder
  }
  dialogVisible.value = true
}

const saveCategory = async () => {
  if (!categoryForm.value.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }

  try {
    if (editingCategoryId.value) {
      await updateCategoryApi(editingCategoryId.value, categoryForm.value)
      ElMessage.success('分类已更新')
    } else {
      await createCategory(categoryForm.value)
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    loadCategories()
  } catch (error) {
    ElMessage.error(error.message || '保存分类失败')
  }
}

const updateCategoryStatus = async (category, status) => {
  const statusText = {
    approved: '通过',
    rejected: '拒绝'
  }

  try {
    await ElMessageBox.confirm(
      `确定要${statusText[status]}分类「${category.name}」的审核吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: status === 'approved' ? 'success' : 'warning'
      }
    )

    if (status === 'approved') {
      await approveCategory(category.id)
    } else {
      await rejectCategory(category.id)
    }

    ElMessage.success(`已${statusText[status]}分类审核`)
    loadCategories()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '更新分类状态失败')
    }
  }
}

const deleteCategory = async (category) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类「${category.name}」吗？此操作不可恢复！`,
      '确认操作',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteCategoryApi(category.id)
    ElMessage.success('分类已删除')
    loadCategories()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除分类失败')
    }
  }
}

const handleFilter = () => {
  currentPage.value = 1
  loadCategories()
}

const resetFilters = () => {
  searchQuery.value = ''
  filterOptions.value.status = ''
  currentPage.value = 1
  loadCategories()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadCategories()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadCategories()
}

onMounted(loadCategories)
</script>

<template>
  <div class="admin-page-container">
    <div class="admin-page-header">
      <h1 class="admin-page-title">
        <font-awesome-icon icon="folder" />
        分类管理
      </h1>

      <div class="header-actions">
        <button class="admin-action-btn primary" @click="openCreateCategory">
          <font-awesome-icon icon="plus" />
          新增分类
        </button>
        <button class="admin-action-btn success">
          <font-awesome-icon icon="download" />
          导出数据
        </button>
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
          placeholder="搜索分类名称或描述..."
          clearable
          size="large"
          style="min-width: 300px; flex: 1;"
        >
          <template #prefix>
            <font-awesome-icon icon="search" />
          </template>
        </el-input>

        <el-select v-model="filterOptions.status" placeholder="审核状态" clearable size="large" style="min-width: 140px;">
          <el-option label="全部状态" value="" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
          <el-option label="审核中" value="pending" />
        </el-select>

        <button class="admin-action-btn primary" @click="handleFilter">
          <font-awesome-icon icon="filter" />
          筛选
        </button>

        <button class="admin-action-btn" @click="resetFilters">
          <font-awesome-icon icon="sync" />
          重置
        </button>
      </div>
    </div>

    <div class="admin-content-card">
      <div class="admin-section-title">
        <font-awesome-icon icon="table" />
        分类列表
      </div>
      <div class="admin-table-container admin-responsive-table">
        <el-table v-loading="loading" :data="filteredCategories" style="width: 100%" stripe>
        <el-table-column label="分类信息" min-width="300">
          <template #default="{ row }">
            <div class="category-info">
              <div class="category-header">
                <span class="category-icon">{{ row.icon }}</span>
                <h3 class="category-title">{{ row.name }}</h3>
              </div>
              <p class="category-description">{{ row.description }}</p>
              <div class="category-meta">
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

        <el-table-column label="文章数量" width="180">
          <template #default="{ row }">
            <div class="stats">
              <div class="stat-item">
                <font-awesome-icon icon="file-alt" />
                <span>{{ row.articleCount }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <div :class="['admin-tag', getStatusMeta(row.status).type]">
              {{ getStatusMeta(row.status).text }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <div class="action-buttons admin-mobile-stack">
              <div class="button-row" v-if="row.status === 'pending'">
                <button
                  class="admin-action-btn success"
                  @click="updateCategoryStatus(row, 'approved')"
                >
                  <font-awesome-icon icon="check" /> 通过
                </button>
                <button
                  class="admin-action-btn danger"
                  @click="updateCategoryStatus(row, 'rejected')"
                >
                  <font-awesome-icon icon="times" /> 拒绝
                </button>
              </div>

              <div class="button-row">
                <button
                  class="admin-action-btn primary"
                  @click="editCategory(row)"
                >
                  <font-awesome-icon icon="edit" /> 编辑
                </button>
                <button
                  class="admin-action-btn danger"
                  @click="deleteCategory(row)"
                >
                  <font-awesome-icon icon="trash-alt" /> 删除
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

    <!-- 新增/编辑分类对话框 -->
    <el-dialog
      :title="editingCategoryId ? '编辑分类' : '新增分类'"
      v-model="dialogVisible"
      width="500px"
    >
      <el-form :model="categoryForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="图标">
          <el-select v-model="categoryForm.icon" placeholder="请选择图标">
            <el-option
              v-for="icon in availableIcons"
              :key="icon"
              :label="icon"
              :value="icon"
            >
              <span style="font-size: 18px; margin-right: 8px;">{{ icon }}</span>
              {{ icon }}
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="categoryForm.description"
            type="textarea"
            rows="3"
            placeholder="请输入分类描述"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.displayOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveCategory">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
@import './AdminCommonStyles.css';

.header-actions {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.category-info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 0.5rem 0;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.category-icon {
  font-size: 1.5rem;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-light) 0%, rgba(37, 99, 235, 0.1) 100%);
  border-radius: var(--radius-lg);
  border: 2px solid rgba(37, 99, 235, 0.2);
  transition: all 0.3s ease;
}

.category-icon:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow);
}

.category-title {
  margin: 0;
  font-size: 1rem;
  color: var(--text-color);
  font-weight: 600;
  line-height: 1.4;
  background: linear-gradient(135deg, var(--text-color) 0%, var(--primary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.category-description {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-light);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  padding: 0.5rem 0.75rem;
  background: rgba(37, 99, 235, 0.05);
  border-radius: var(--radius);
  border: 1px solid rgba(37, 99, 235, 0.1);
}

.category-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.creator-info {
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

.time-info {
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

.stats {
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

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.button-row {
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

  .category-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .meta-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .action-buttons {
    width: 100%;
  }

  .button-row {
    flex-direction: column;
  }
}

@media (max-width: 576px) {
  .category-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .stats {
    flex-direction: column;
    gap: 0.5rem;
  }
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
