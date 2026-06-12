import request from '../utils/request'
import { parseComments } from '../utils/commentHelper'

const normalizeArticle = (article) => {
  const author = article.author || {}
  const category = article.category || null

  return {
    ...article,
    author: {
      ...author,
      id: author.id,
      name: author.nickname || author.username || author.name || '匿名用户',
      avatar: author.avatarUrl || author.avatar || null
    },
    category: typeof category === 'string' ? category : category?.name || '',
    categoryInfo: category,
    tags: Array.isArray(article.tags) ? article.tags : [],
    likes: article.likeCount ?? article.likes ?? 0,
    views: article.viewCount ?? article.views ?? 0,
    comments: parseComments(article.comments || []),
    createTime: article.publishedAt || article.createdAt || article.createTime,
    isOfficial: article.isOfficial ?? false,
    isTop: article.isPinned ?? article.isTop ?? false,
    isHighlight: article.isFeatured ?? article.isHighlight ?? false
  }
}

const unwrapPage = (response) => {
  const page = response?.data || {}
  const rows = Array.isArray(page.content) ? page.content : Array.isArray(response?.data) ? response.data : []

  return {
    success: response?.success === true || response?.code === 200,
    message: response?.message,
    data: rows.map(normalizeArticle),
    total: page.totalElements ?? rows.length,
    totalElements: page.totalElements ?? rows.length,
    totalPages: page.totalPages ?? 1,
    page: (page.number ?? 0) + 1
  }
}

const toArticlePayload = (article) => ({
  title: article.title?.trim(),
  content: article.content?.trim(),
  summary: article.summary?.trim() || article.title?.trim(),
  categoryId: Number(article.categoryId) || 1,
  tags: Array.isArray(article.tags) ? article.tags.map(tag => tag.trim()).filter(Boolean) : [],
  isDraft: Boolean(article.isDraft),
  coverImage: article.coverImage || null
})

export const articleService = {
  async getArticles(params = {}) {
    const requestParams = {
      page: params.page || 1,
      pageSize: params.pageSize || params.size || 10
    }
    const keyword = params.keyword || params.search
    if (params.categoryId) requestParams.categoryId = params.categoryId
    if (params.tagId) requestParams.tagId = params.tagId
    if (keyword) requestParams.keyword = keyword
    if (params.sort) requestParams.sort = params.sort

    const response = await request.get('/articles', {
      params: requestParams
    })
    return unwrapPage(response)
  },

  async getArticleById(id) {
    const response = await request.get(`/articles/${id}`)
    return {
      ...response,
      data: normalizeArticle(response.data)
    }
  },

  async createArticle(article) {
    if (!article.title?.trim()) {
      throw new Error('文章标题不能为空')
    }
    if (!article.content?.trim()) {
      throw new Error('文章内容不能为空')
    }

    const response = await request.post('/articles', toArticlePayload(article))
    return normalizeArticle(response.data)
  },

  async updateArticle(id, article) {
    const response = await request.put(`/articles/${id}`, toArticlePayload(article))
    return normalizeArticle(response.data)
  },

  async deleteArticle(id) {
    return request.delete(`/articles/${id}`)
  },

  async likeArticle(id) {
    return request.post(`/likes/ARTICLE/${id}`)
  },

  async unlikeArticle(id) {
    return request.delete(`/likes/ARTICLE/${id}`)
  },

  async getArticleComments(id, params = {}) {
    const response = await request.get(`/comments/article/${id}`, {
      params: {
        page: params.page || 1,
        pageSize: params.pageSize || params.size || 20
      }
    })
    const rows = response.data?.content || response.data || []
    return parseComments(rows)
  },

  async addComment(articleId, comment) {
    return request.post('/comments', {
      targetType: 'ARTICLE',
      targetId: Number(articleId),
      content: typeof comment === 'string' ? comment : comment.content,
      parentId: typeof comment === 'object' ? comment.parentId || null : null
    })
  },

  async deleteComment(articleId, commentId) {
    return request.delete(`/comments/${commentId}`)
  },

  async likeComment(articleId, commentId) {
    return request.post(`/likes/COMMENT/${commentId}`)
  },

  async getArticlesByCategory(categoryId, params) {
    return this.getArticles({ ...(params || {}), categoryId })
  },

  async getArticlesByTag(tagId, params) {
    return this.getArticles({ ...(params || {}), tagId })
  },

  async uploadImage(file) {
    if (!file) {
      throw new Error('请选择要上传的文件')
    }
    if (!file.type.startsWith('image/')) {
      throw new Error('只支持图片文件')
    }
    if (file.size > 5 * 1024 * 1024) {
      throw new Error('文件大小不能超过5MB')
    }

    const formData = new FormData()
    formData.append('file', file)
    const response = await request.post('/upload/image', formData)
    return response.data?.url
  },

  async getHotArticles(params = {}) {
    const response = await request.get('/articles/popular', {
      params: {
        limit: params.limit || params.size || params.pageSize || 5
      }
    })
    const articles = Array.isArray(response.data) ? response.data.map(normalizeArticle) : []
    return {
      success: true,
      data: articles,
      total: articles.length
    }
  },

  async getRecommendedArticles(params = {}) {
    const response = await request.get('/articles/featured', {
      params: {
        limit: params.limit || params.size || params.pageSize || 5
      }
    })
    const articles = Array.isArray(response.data) ? response.data.map(normalizeArticle) : []
    return {
      success: true,
      data: articles,
      total: articles.length
    }
  }
}
