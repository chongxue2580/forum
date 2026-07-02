<template>
  <div class="admin-questions">
    <div class="page-header">
      <h2>问答管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索问答..."
          prefix-icon="el-icon-search"
          style="width: 300px; margin-right: 10px;"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="选择状态" clearable>
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否解决">
          <el-select v-model="filterForm.solved" placeholder="选择状态" clearable>
            <el-option label="已解决" value="true" />
            <el-option label="未解决" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="特殊标记">
          <el-select v-model="filterForm.special" placeholder="选择标记" clearable>
            <el-option label="置顶" value="pinned" />
            <el-option label="精选" value="featured" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadQuestions">筛选</el-button>
          <el-button @click="resetFilter">重置筛选</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 问答列表 -->
    <el-card>
      <el-table
        v-loading="loading"
        :data="questions"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="scope">
            <div class="question-title">
              <el-link type="primary" @click="viewQuestion(scope.row)">
                {{ scope.row.title }}
              </el-link>
              <div class="question-tags">
                <el-tag v-if="scope.row.isPinned" type="danger" size="mini">置顶</el-tag>
                <el-tag v-if="scope.row.isFeatured" type="warning" size="mini">精选</el-tag>
                <el-tag v-if="scope.row.isSolved" type="success" size="mini">已解决</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="authorName" label="作者" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="统计" width="120">
          <template #default="scope">
            <div class="question-stats">
              <span><i class="el-icon-view"></i> {{ scope.row.viewCount }}</span>
              <span><i class="el-icon-star-off"></i> {{ scope.row.likeCount }}</span>
              <span><i class="el-icon-chat-line-round"></i> {{ scope.row.commentCount }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="mini" @click="viewQuestion(scope.row)">查看</el-button>
            <el-dropdown @command="(command) => handleAction(command, scope.row)">
              <el-button size="mini" type="primary">
                更多<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu #dropdown>
                <el-dropdown-item 
                  v-if="scope.row.status === 'PENDING'"
                  command="approve"
                >
                  审核通过
                </el-dropdown-item>
                <el-dropdown-item 
                  v-if="scope.row.status === 'PENDING'"
                  command="reject"
                >
                  审核拒绝
                </el-dropdown-item>
                <el-dropdown-item 
                  :command="scope.row.isPinned ? 'unpin' : 'pin'"
                >
                  {{ scope.row.isPinned ? '取消置顶' : '设为置顶' }}
                </el-dropdown-item>
                <el-dropdown-item 
                  :command="scope.row.isFeatured ? 'unfeature' : 'feature'"
                >
                  {{ scope.row.isFeatured ? '取消精选' : '设为精选' }}
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 问答详情对话框 -->
    <el-dialog v-model="questionDetailVisible" title="问答详情" width="800px">
      <div v-if="selectedQuestion" class="question-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ selectedQuestion.id }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ selectedQuestion.title }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ selectedQuestion.authorName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedQuestion.status)">
              {{ getStatusText(selectedQuestion.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="是否解决">
            <el-tag :type="selectedQuestion.isSolved ? 'success' : 'info'">
              {{ selectedQuestion.isSolved ? '已解决' : '未解决' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="特殊标记">
            <div>
              <el-tag v-if="selectedQuestion.isPinned" type="danger" size="small">置顶</el-tag>
              <el-tag v-if="selectedQuestion.isFeatured" type="warning" size="small">精选</el-tag>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="浏览量">{{ selectedQuestion.viewCount }}</el-descriptions-item>
          <el-descriptions-item label="点赞数">{{ selectedQuestion.likeCount }}</el-descriptions-item>
          <el-descriptions-item label="评论数">{{ selectedQuestion.commentCount }}</el-descriptions-item>
          <el-descriptions-item label="关注数">{{ selectedQuestion.followCount }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(selectedQuestion.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDate(selectedQuestion.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="question-content">
          <h4>问题内容</h4>
          <div class="content-preview" v-html="selectedQuestion.content"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getAllQuestions, 
  approveQuestion, 
  rejectQuestion, 
  pinQuestion, 
  unpinQuestion,
  featureQuestion,
  unfeatureQuestion,
  deleteQuestion 
} from '@/api/admin'

// 响应式数据
const questions = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filterForm = ref({
  status: '',
  solved: '',
  special: ''
})
const selectedQuestions = ref([])
const questionDetailVisible = ref(false)
const selectedQuestion = ref(null)

// 生命周期
onMounted(() => {
  loadQuestions()
})

// 方法
const loadQuestions = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      status: filterForm.value.status
    }
    
    const response = await getAllQuestions(params)
    if (response.code === 1) {
      questions.value = response.data.content
      total.value = response.data.totalElements
    }
  } catch (error) {
    ElMessage.error('加载问答列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  // 实现搜索逻辑
  loadQuestions()
}

const resetSearch = () => {
  searchKeyword.value = ''
  loadQuestions()
}

const resetFilter = () => {
  filterForm.value = {
    status: '',
    solved: '',
    special: ''
  }
  loadQuestions()
}

const handleSelectionChange = (selection) => {
  selectedQuestions.value = selection
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadQuestions()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadQuestions()
}

const viewQuestion = (question) => {
  selectedQuestion.value = question
  questionDetailVisible.value = true
}

const handleAction = async (command, question) => {
  selectedQuestion.value = question
  
  switch (command) {
    case 'approve':
      await approveQuestionAction(question)
      break
    case 'reject':
      await rejectQuestionAction(question)
      break
    case 'pin':
      await pinQuestionAction(question)
      break
    case 'unpin':
      await unpinQuestionAction(question)
      break
    case 'feature':
      await featureQuestionAction(question)
      break
    case 'unfeature':
      await unfeatureQuestionAction(question)
      break
    case 'delete':
      await deleteQuestionAction(question)
      break
  }
}

const approveQuestionAction = async (question) => {
  try {
    await ElMessageBox.confirm('确定要审核通过该问答吗？', '审核通过', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await approveQuestion(question.id)
    ElMessage.success('审核通过成功')
    loadQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('审核通过失败')
    }
  }
}

const rejectQuestionAction = async (question) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '审核拒绝', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    await rejectQuestion(question.id, reason)
    ElMessage.success('审核拒绝成功')
    loadQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('审核拒绝失败')
    }
  }
}

const pinQuestionAction = async (question) => {
  try {
    await pinQuestion(question.id)
    ElMessage.success('设置置顶成功')
    loadQuestions()
  } catch (error) {
    ElMessage.error('设置置顶失败')
  }
}

const unpinQuestionAction = async (question) => {
  try {
    await unpinQuestion(question.id)
    ElMessage.success('取消置顶成功')
    loadQuestions()
  } catch (error) {
    ElMessage.error('取消置顶失败')
  }
}

const featureQuestionAction = async (question) => {
  try {
    await featureQuestion(question.id)
    ElMessage.success('设置精选成功')
    loadQuestions()
  } catch (error) {
    ElMessage.error('设置精选失败')
  }
}

const unfeatureQuestionAction = async (question) => {
  try {
    await unfeatureQuestion(question.id)
    ElMessage.success('取消精选成功')
    loadQuestions()
  } catch (error) {
    ElMessage.error('取消精选失败')
  }
}

const deleteQuestionAction = async (question) => {
  try {
    await ElMessageBox.confirm('确定要删除该问答吗？此操作不可恢复！', '删除问答', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteQuestion(question.id)
    ElMessage.success('问答已删除')
    loadQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除问答失败')
    }
  }
}

const getStatusType = (status) => {
  const typeMap = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  }
  return textMap[status] || '未知'
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}
</script>

<style scoped>
.admin-questions {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: var(--ad-text);
}

.header-actions {
  display: flex;
  align-items: center;
}

.filter-card {
  margin-bottom: 20px;
}

.question-title {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.question-tags {
  display: flex;
  gap: 4px;
}

.question-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--ad-muted);
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.question-detail {
  padding: 20px 0;
}

.question-content {
  margin-top: 20px;
}

.question-content h4 {
  margin-bottom: 10px;
  color: var(--ad-text);
}

.content-preview {
  max-height: 300px;
  overflow-y: auto;
  padding: 15px;
  background: var(--ad-surface-muted);
  border-radius: 8px;
  border: 1px solid var(--ad-border);
}
</style>
