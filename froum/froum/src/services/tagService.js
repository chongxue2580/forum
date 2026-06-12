import request from '../utils/request'

export const tagService = {
  async getAllTags(page = 0, size = 20) {
    return request.get('/tags', {
      params: { page, size }
    })
  },

  async getTagById(id) {
    return request.get(`/tags/${id}`)
  },

  async createTag(tag) {
    return request.post('/tags', tag)
  },

  async updateTag(id, tag) {
    return request.put(`/tags/${id}`, tag)
  },

  async deleteTag(id) {
    return request.delete(`/tags/${id}`)
  },

  async getPopularTags(limit = 10) {
    return request.get('/tags/popular', {
      params: { limit }
    })
  },

  async getRelatedTags(tagId) {
    return request.get(`/tags/${tagId}/related`)
  },

  async followTag(tagId) {
    return request.post(`/tags/${tagId}/follow`)
  },

  async unfollowTag(tagId) {
    return request.delete(`/tags/${tagId}/follow`)
  },

  async getUserFollowedTags(userId) {
    return request.get(`/users/${userId}/followed-tags`)
  },

  async getArticlesByTag(id, page = 0, size = 10) {
    return request.get(`/tags/${id}/articles`, {
      params: { page, size }
    })
  },

  async getQuestionsByTag(id, page = 0, size = 10) {
    return request.get(`/tags/${id}/questions`, {
      params: { page, size }
    })
  }
}
