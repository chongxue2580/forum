import request from '../utils/request'
import { userApi } from '../api/userApi'

const unwrap = (response) => response?.data ?? response

const getCurrentUserId = () => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return userInfo.id || null
  } catch (error) {
    return null
  }
}

const unsupported = (feature) => {
  throw new Error(`当前后端暂未提供${feature}接口`)
}

export const userService = {
  async getUserProfile(userId) {
    return unwrap(await userApi.getUserProfile(userId))
  },

  async getUserStats(userId) {
    return unwrap(await userApi.getUserStats(userId))
  },

  async updateProfile(userId, profileData) {
    return userApi.updateUserProfile(userId, profileData)
  },

  async updatePassword(userId, passwordData) {
    return userApi.updatePassword(userId, passwordData)
  },

  async login(username, password) {
    return userApi.login({ username, password })
  },

  async register(userData) {
    return userApi.register(userData)
  },

  async getFollowers(userId, page = 0, size = 10) {
    return userApi.getUserFollowers(userId, page, size)
  },

  async getFollowing(userId, page = 0, size = 10) {
    return userApi.getUserFollowing(userId, page, size)
  },

  async followUser(userId, targetUserId) {
    return userApi.followUser(targetUserId ?? userId)
  },

  async unfollowUser(userId, targetUserId) {
    return userApi.unfollowUser(targetUserId ?? userId)
  },

  async getUserArticles(userId, page = 0, size = 10) {
    return userApi.getUserArticles(userId, page, size)
  },

  async getUserQuestions(userId, page = 0, size = 10) {
    return userApi.getUserQuestions(userId, page, size)
  },

  async getUserAnswers(userId, page = 0, size = 10) {
    return userApi.getUserAnswers(userId, page, size)
  },

  async getUserCommentedArticles() {
    return unsupported('用户评论过的文章')
  },

  async getUserCommentedQuestions() {
    return unsupported('用户评论过的问题')
  },

  async getUserLikedArticles(userId, page = 0, size = 10) {
    const currentUserId = getCurrentUserId()
    if (currentUserId && Number(userId) !== Number(currentUserId)) {
      return unsupported('查看其他用户点赞文章')
    }
    return request.get('/likes/articles', { params: { page, size } })
  },

  async uploadAvatar(userId, file) {
    const currentUserId = getCurrentUserId()
    if (currentUserId && Number(userId) !== Number(currentUserId)) {
      return unsupported('上传其他用户头像')
    }

    const formData = new FormData()
    formData.append('file', file)
    return request.post('/upload/avatar', formData)
  },

  async checkIsAdmin(userId) {
    const profile = unwrap(await userApi.getUserProfile(userId))
    const role = profile?.role
    return role === 'ADMIN' || role === 'SUPER_ADMIN' || role === 'admin'
  },

  async toggleFollow(targetUserId) {
    return userApi.toggleFollow(targetUserId)
  }
}
