<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import '@/components/admin/AdminCommonStyles.css'
import {
  approveArticle,
  deleteArticle,
  featureArticle,
  getAllArticles,
  pinArticle,
  rejectArticle,
  unfeatureArticle,
  unpinArticle
} from '@/api/admin'

const router = useRouter()

// 文章状态选项
const statusOptions = [
  { value: 'all', label: '全部状态' },
  { value: 'pending', label: '待审核' },
  { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已拒绝' }
]

// 文章类型选项
const typeOptions = [
  { value: 'all', label: '全部类型' },
  { value: 'normal', label: '普通文章' },
  { value: 'official', label: '官方文章' },
  { value: 'featured', label: '精选文章' }
]

const loading = ref(false)
const tableData = ref([])

// 筛选和搜索
const searchQuery = ref('')
const statusFilter = ref('all')
const typeFilter = ref('all')

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const displayedTableData = computed(() => {
  let result = tableData.value
  const keyword = searchQuery.value.trim().toLowerCase()

  if (keyword) {
    result = result.filter(article => {
      return [
        article.title,
        article.author,
        article.category,
        ...(article.tags || [])
      ].some(value => String(value || '').toLowerCase().includes(keyword))
    })
  }

  if (typeFilter.value === 'official') {
    result = result.filter(article => article.isOfficial)
  } else if (typeFilter.value === 'featured') {
    result = result.filter(article => article.isFeatured)
  } else if (typeFilter.value === 'normal') {
    result = result.filter(article => !article.isOfficial && !article.isFeatured)
  }

  return result
})

const normalizeArticle = (article) => ({
  id: article.id,
  title: article.title || '',
  author: article.author?.nickname || article.author?.username || article.author?.name || '未知作者',
  category: typeof article.category === 'string' ? article.category : article.category?.name || '',
  tags: Array.isArray(article.tags) ? article.tags : [],
  status: String(article.status || '').toLowerCase(),
  createTime: formatDateTime(article.createdAt || article.createTime || article.publishedAt),
  updateTime: formatDateTime(article.updatedAt || article.updateTime),
  views: article.viewCount || article.views || 0,
  likes: article.likeCount || article.likes || 0,
  comments: article.commentCount || article.comments || 0,
  isOfficial: Boolean(article.isOfficial),
  isPinned: Boolean(article.isPinned || article.isTop),
  isFeatured: Boolean(article.isFeatured || article.isHighlight)
})

const unwrapPage = (response) => {
  const page = response?.data || response || {}
  return {
    content: Array.isArray(page.content) ? page.content : Array.isArray(page) ? page : [],
    totalElements: page.totalElements || 0
  }
}

const loadArticles = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }

    if (statusFilter.value !== 'all') {
      params.status = statusFilter.value.toUpperCase()
    }

    const response = await getAllArticles(params)
    const page = unwrapPage(response)
    tableData.value = page.content.map(normalizeArticle)
    total.value = page.totalElements
  } catch (error) {
    console.error('加载文章列表失败:', error)
    tableData.value = []
    total.value = 0
    ElMessage.error(error.message || '加载文章列表失败')
  } finally {
    loading.value = false
  }
}

// 处理页码变化
const handlePageChange = (page) => {
  currentPage.value = page
  loadArticles()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadArticles()
}

const handleSearch = () => {
  currentPage.value = 1
  loadArticles()
}

const resetFilters = () => {
  searchQuery.value = ''
  statusFilter.value = 'all'
  typeFilter.value = 'all'
  currentPage.value = 1
  loadArticles()
}

// 处理审核操作
const handleApprove = async (row) => {
  try {
    await approveArticle(row.id)
    ElMessage.success('文章已审核通过')
    loadArticles()
  } catch (error) {
    ElMessage.error(error.message || '审核文章失败')
  }
}

const handleReject = async (row) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝文章', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })
    await rejectArticle(row.id, reason)
    ElMessage.success('文章已拒绝')
    loadArticles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '拒绝文章失败')
    }
  }
}

// 处理文章标记
const toggleOfficial = () => {
  ElMessage.warning('当前后端暂未提供官方文章设置接口')
}

const togglePinned = async (row) => {
  try {
    if (row.isPinned) {
      await unpinArticle(row.id)
      ElMessage.success('已取消置顶')
    } else {
      await pinArticle(row.id)
      ElMessage.success('已设为置顶')
    }
    loadArticles()
  } catch (error) {
    ElMessage.error(error.message || '更新置顶状态失败')
  }
}

const toggleFeatured = async (row) => {
  try {
    if (row.isFeatured) {
      await unfeatureArticle(row.id)
      ElMessage.success('已取消精选')
    } else {
      await featureArticle(row.id)
      ElMessage.success('已设为精选')
    }
    loadArticles()
  } catch (error) {
    ElMessage.error(error.message || '更新精选状态失败')
  }
}

// 处理删除操作
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除文章「${row.title}」吗？`, '删除文章', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteArticle(row.id)
    ElMessage.success('文章已删除')
    loadArticles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除文章失败')
    }
  }
}

// 处理查看详情
const handleViewDetail = (row) => {
  router.push(`/article/${row.id}`)
}

// 处理编辑操作
const handleEdit = (row) => {
  router.push(`/article/${row.id}/edit`)
}

// 格式化文章状态
const formatStatus = (status) => {
  switch (status) {
    case 'pending':
      return { text: '待审核', type: 'warning' }
    case 'approved':
      return { text: '已通过', type: 'success' }
    case 'rejected':
      return { text: '已拒绝', type: 'danger' }
    default:
      return { text: '未知', type: 'info' }
  }
}

const formatDateTime = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN')
}

onMounted(() => {
  loadArticles()
})
</script>

<template>
  <div class="admin-page-container">
    <div class="admin-page-header">
      <h1 class="admin-page-title">
        <font-awesome-icon icon="file-alt" />
        文章管理
      </h1>

      <div class="header-actions">
        <button class="admin-action-btn primary" @click="router.push('/article/new')">
          <font-awesome-icon icon="plus" />
          新建文章
        </button>
        <button class="admin-action-btn success" disabled title="当前后端暂未提供导出接口">
          <font-awesome-icon icon="download" />
          导出数据
        </button>
        <button class="admin-action-btn warning" disabled title="请在仪表盘查看统计数据">
          <font-awesome-icon icon="chart-bar" />
          数据统计
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
          placeholder="搜索文章标题、作者或内容"
          clearable
          size="large"
          style="min-width: 300px; flex: 1;"
        >
          <template #prefix>
            <font-awesome-icon icon="search" />
          </template>
        </el-input>

        <el-select v-model="statusFilter" placeholder="文章状态" size="large" style="min-width: 140px;">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <el-select v-model="typeFilter" placeholder="文章类型" size="large" style="min-width: 140px;">
          <el-option
            v-for="item in typeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <button class="admin-action-btn primary" @click="handleSearch">
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
        文章列表
      </div>
      
      <div class="admin-table-container admin-responsive-table">
        <el-table
          v-loading="loading"
          :data="displayedTableData"
          style="width: 100%"
          stripe
          highlight-current-row
        >
          <el-table-column prop="id" label="ID" width="80" />

          <el-table-column label="文章信息" min-width="320">
            <template #default="{ row }">
              <div class="article-info">
                <div class="article-title">{{ row.title }}</div>
                <div class="article-meta">
                  <div class="meta-left">
                    <div class="author-info">
                      <font-awesome-icon icon="user" />
                      {{ row.author }}
                    </div>
                    <div class="time-info">
                      <font-awesome-icon icon="clock" />
                      {{ row.createTime }}
                    </div>
                  </div>
                  <div class="article-flags">
                    <div v-if="row.isOfficial" class="admin-tag success">
                      <font-awesome-icon icon="shield-alt" />
                      官方
                    </div>
                    <div v-if="row.isPinned" class="admin-tag warning">
                      <font-awesome-icon icon="thumbtack" />
                      置顶
                    </div>
                    <div v-if="row.isFeatured" class="admin-tag primary">
                      <font-awesome-icon icon="star" />
                      精选
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="分类" prop="category" width="120" />

          <el-table-column label="标签" width="200">
            <template #default="{ row }">
              <div class="tag-container">
                <div
                  v-for="(tag, index) in row.tags"
                  :key="index"
                  class="admin-tag primary"
                >
                  {{ tag }}
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <div
                :class="['admin-tag', formatStatus(row.status).type]"
              >
                {{ formatStatus(row.status).text }}
              </div>
            </template>
          </el-table-column>
          
          <el-table-column label="数据统计" width="200">
            <template #default="{ row }">
              <div class="stats">
                <div class="stat-item">
                  <font-awesome-icon icon="eye" />
                  {{ row.views }}
                </div>
                <div class="stat-item">
                  <font-awesome-icon icon="thumbs-up" />
                  {{ row.likes }}
                </div>
                <div class="stat-item">
                  <font-awesome-icon icon="comment" />
                  {{ row.comments }}
                </div>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="240">
            <template #default="{ row }">
              <div class="action-buttons admin-mobile-stack">
                <div class="button-row">
                  <button
                    v-if="row.status === 'pending'"
                    class="admin-action-btn success"
                    @click="handleApprove(row)"
                  >
                    <font-awesome-icon icon="check" />
                    通过
                  </button>

                  <button
                    v-if="row.status === 'pending'"
                    class="admin-action-btn danger"
                    @click="handleReject(row)"
                  >
                    <font-awesome-icon icon="times" />
                    拒绝
                  </button>

                  <button
                    class="admin-action-btn primary"
                    @click="handleViewDetail(row)"
                  >
                    <font-awesome-icon icon="eye" />
                    查看
                  </button>
                </div>

                <div class="button-row">
                  <button
                    class="admin-action-btn primary"
                    @click="handleEdit(row)"
                  >
                    <font-awesome-icon icon="edit" />
                    编辑
                  </button>

                  <el-dropdown trigger="click">
                    <button class="admin-action-btn">
                      <font-awesome-icon icon="ellipsis-h" />
                      更多
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="toggleOfficial(row)">
                          <font-awesome-icon :icon="row.isOfficial ? 'times-circle' : 'check-circle'" />
                          {{ row.isOfficial ? '取消官方' : '设为官方' }}
                        </el-dropdown-item>

                        <el-dropdown-item @click="togglePinned(row)">
                          <font-awesome-icon :icon="row.isPinned ? 'times-circle' : 'thumbtack'" />
                          {{ row.isPinned ? '取消置顶' : '设为置顶' }}
                        </el-dropdown-item>

                        <el-dropdown-item @click="toggleFeatured(row)">
                          <font-awesome-icon :icon="row.isFeatured ? 'times-circle' : 'star'" />
                          {{ row.isFeatured ? '取消精选' : '设为精选' }}
                        </el-dropdown-item>

                        <el-dropdown-item divided @click="handleDelete(row)">
                          <font-awesome-icon icon="trash-alt" />
                          删除文章
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
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
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
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

.article-info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 0.5rem 0;
}

.article-title {
  font-weight: 600;
  color: var(--text-color);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
  background: linear-gradient(135deg, var(--text-color) 0%, var(--primary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.article-flags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.author-info {
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

.tag-container {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
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

  .article-meta {
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
  .stats {
    flex-direction: column;
    gap: 0.5rem;
  }

  .tag-container {
    flex-direction: column;
    gap: 0.25rem;
  }
}
</style>
