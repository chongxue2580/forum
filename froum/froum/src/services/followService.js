import request from '../utils/request'

export const followService = {
  async followUser(userId, targetUserId) {
    return request.post(`/follows/USER/${targetUserId ?? userId}`)
  },

  async unfollowUser(userId, targetUserId) {
    return request.delete(`/follows/USER/${targetUserId ?? userId}`)
  },

  async getFollowingUsers(userId, page = 0, size = 10) {
    return request.get('/follows/users', {
      params: { page, size }
    })
  },

  async getFollowers(userId, page = 0, size = 10) {
    return request.get('/follows/followers', {
      params: { page, size }
    })
  },

  async getFollowingQuestions(userId, page = 0, size = 10) {
    return request.get('/follows/questions', {
      params: { page, size }
    })
  },

  async getFollowingUserArticles(userId, page = 0, size = 10) {
    return request.get('/follows/users/articles', {
      params: { page, size }
    })
  },

  async getFollowingUserQuestions(userId, page = 0, size = 10) {
    return request.get('/follows/users/questions', {
      params: { page, size }
    })
  },

  async checkFollowingUser(userId, targetUserId) {
    const response = await request.get(`/follows/USER/${targetUserId}/status`)
    return response.data || false
  },

  async checkFollowingQuestion(userId, questionId) {
    const response = await request.get(`/follows/QUESTION/${questionId}/status`)
    return response.data || false
  },

  async isFollowing(userId, questionId) {
    const followed = await this.checkFollowingQuestion(userId, questionId)
    return { followed }
  }
}
