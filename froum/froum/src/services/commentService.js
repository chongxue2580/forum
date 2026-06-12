import request from '../utils/request'
import { parseComments } from '../utils/commentHelper'

const unwrapPage = (response) => {
  const page = response?.data || {}
  const rows = Array.isArray(page.content) ? page.content : Array.isArray(response?.data) ? response.data : []
  return {
    success: response?.success === true || response?.code === 200,
    message: response?.message,
    data: parseComments(rows),
    total: page.totalElements ?? rows.length,
    totalPages: page.totalPages ?? 1,
    page: (page.number ?? 0) + 1
  }
}

export const commentService = {
  async getCommentById(id) {
    return request.get(`/comments/${id}`)
  },

  async getComments(targetType, targetId, params = {}) {
    const response = await request.get(`/comments/${targetType}/${targetId}`, {
      params: {
        page: params.page || 1,
        pageSize: params.pageSize || params.size || 20
      }
    })
    return unwrapPage(response)
  },

  async getArticleComments(articleId, params = {}) {
    const response = await request.get(`/comments/article/${articleId}`, {
      params: {
        page: params.page || 1,
        pageSize: params.pageSize || params.size || 20
      }
    })
    return unwrapPage(response)
  },

  async getQuestionComments(questionId, params = {}) {
    const response = await request.get(`/comments/question/${questionId}`, {
      params: {
        page: params.page || 1,
        pageSize: params.pageSize || params.size || 20
      }
    })
    return unwrapPage(response)
  },

  async addComment(comment) {
    return request.post('/comments', comment)
  },

  async addArticleComment(articleId, content, parentId = null) {
    return this.addComment({
      targetType: 'ARTICLE',
      targetId: Number(articleId),
      content,
      parentId
    })
  },

  async addQuestionComment(questionId, content, parentId = null) {
    return this.addComment({
      targetType: 'QUESTION',
      targetId: Number(questionId),
      content,
      parentId
    })
  },

  async deleteComment(id) {
    return request.delete(`/comments/${id}`)
  },

  async likeComment(id) {
    return request.post(`/comments/${id}/like`)
  },

  async unlikeComment(id) {
    return request.delete(`/comments/${id}/like`)
  },

  async checkLikedComment(id) {
    const response = await request.get(`/likes/COMMENT/${id}/status`)
    return Boolean(response?.data)
  }
}
