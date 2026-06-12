import request from '@/utils/request'

const API_URL = '/api/admin'

export const adminService = {
  // 用户管理
  // 获取所有用户
  async getAllUsers(page = 0, size = 10) {
    return await request.get(`${API_URL}/users`, {
      params: { page, size }
    })
  },

  // 禁用用户
  async disableUser(userId) {
    return await request.post(`${API_URL}/users/${userId}/disable`)
  },

  // 启用用户
  async enableUser(userId) {
    return await request.post(`${API_URL}/users/${userId}/enable`)
  },

  // 设置为管理员
  async setAdmin(userId) {
    return await request.post(`${API_URL}/users/${userId}/set-admin`)
  },
  
  // 取消管理员
  async unsetAdmin(userId) {
    return await request.put(`${API_URL}/users/${userId}/unset-admin`)
  },
  
  // 文章管理
  // 获取所有文章
  async getAllArticles(page = 0, size = 10, status = null) {
    const params = { page, size }
    if (status) params.status = status

    return await request.get(`${API_URL}/articles`, { params })
  },

  // 设置文章为官方
  async setArticleOfficial(articleId) {
    return await request.put(`${API_URL}/articles/${articleId}/set-official`)
  },

  // 取消文章官方状态
  async unsetArticleOfficial(articleId) {
    return await request.put(`${API_URL}/articles/${articleId}/unset-official`)
  },

  // 设置文章为置顶
  async setArticlePinned(articleId) {
    return await request.put(`${API_URL}/articles/${articleId}/set-pinned`)
  },

  // 取消文章置顶状态
  async unsetArticlePinned(articleId) {
    return await request.put(`${API_URL}/articles/${articleId}/unset-pinned`)
  },

  // 设置文章为加精
  async setArticleFeatured(articleId) {
    return await request.put(`${API_URL}/articles/${articleId}/set-featured`)
  },

  // 取消文章加精状态
  async unsetArticleFeatured(articleId) {
    return await request.put(`${API_URL}/articles/${articleId}/unset-featured`)
  },

  // 审核通过文章
  async approveArticle(articleId) {
    return await request.put(`${API_URL}/articles/${articleId}/approve`)
  },

  // 审核不通过文章
  async rejectArticle(articleId) {
    return await request.put(`${API_URL}/articles/${articleId}/reject`)
  },
  
  // 问题管理
  // 获取所有问题
  async getAllQuestions(page = 0, size = 10, status = null) {
    const params = { page, size }
    if (status) params.status = status

    return await request.get(`${API_URL}/questions`, { params })
  },

  // 审核通过问题
  async approveQuestion(questionId) {
    return await request.put(`${API_URL}/questions/${questionId}/approve`)
  },

  // 审核不通过问题
  async rejectQuestion(questionId) {
    return await request.put(`${API_URL}/questions/${questionId}/reject`)
  },
  
  // 标签管理
  // 获取所有标签
  async getAllTags(page = 0, size = 20, status = null) {
    const params = { page, size }
    if (status) params.status = status

    return await request.get(`${API_URL}/tags`, { params })
  },

  // 审核通过标签
  async approveTag(tagId) {
    return await request.put(`${API_URL}/tags/${tagId}/approve`)
  },

  // 审核不通过标签
  async rejectTag(tagId) {
    return await request.put(`${API_URL}/tags/${tagId}/reject`)
  },

  // 添加标签
  async createTag(tag) {
    return await request.post(`${API_URL}/tags`, tag)
  },
  
  // 分类管理
  // 获取所有分类
  async getAllCategories(page = 0, size = 20, status = null) {
    const params = { page, size }
    if (status) params.status = status

    return await request.get(`${API_URL}/categories`, { params })
  },

  // 审核通过分类
  async approveCategory(categoryId) {
    return await request.put(`${API_URL}/categories/${categoryId}/approve`)
  },

  // 审核不通过分类
  async rejectCategory(categoryId) {
    return await request.put(`${API_URL}/categories/${categoryId}/reject`)
  },

  // 添加分类
  async createCategory(category) {
    return await request.post(`${API_URL}/categories`, category)
  },

  // 操作日志
  // 获取操作日志
  async getOperationLogs(page = 0, size = 20, type = null) {
    const params = { page, size }
    if (type) params.type = type

    return await request.get(`${API_URL}/logs`, { params })
  },

  // 仪表盘数据
  async getDashboardData() {
    return await request.get(`${API_URL}/dashboard`)
  }
} 