import request from '../utils/request'

export const likeService = {
  async getLikedArticles(userId, page = 0, size = 10) {
    return request.get('/likes/articles', {
      params: { page, size }
    })
  },

  async likeArticle(articleId) {
    return request.post(`/likes/ARTICLE/${articleId}`)
  },

  async unlikeArticle(articleId) {
    return request.delete(`/likes/ARTICLE/${articleId}`)
  },

  async hasLikedArticle(articleId) {
    const response = await request.get(`/likes/ARTICLE/${articleId}/status`)
    return response.data || false
  },

  async likeComment(commentId) {
    return request.post(`/likes/COMMENT/${commentId}`)
  },

  async unlikeComment(commentId) {
    return request.delete(`/likes/COMMENT/${commentId}`)
  },

  async hasLikedComment(commentId) {
    const response = await request.get(`/likes/COMMENT/${commentId}/status`)
    return response.data || false
  }
}
