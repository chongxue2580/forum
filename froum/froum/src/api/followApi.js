import request from '@/utils/request'

/**
 * 关注相关API
 */
export const followApi = {
  /**
   * 关注目标
   * @param {string} targetType - 目标类型 (USER, QUESTION)
   * @param {number} targetId - 目标ID
   */
  async followTarget(targetType, targetId) {
    try {
      const response = await request.post(`/follows/${targetType}/${targetId}`)
      return response
    } catch (error) {
      console.error('关注失败:', error)
      throw error
    }
  },

  /**
   * 取消关注目标
   * @param {string} targetType - 目标类型 (USER, QUESTION)
   * @param {number} targetId - 目标ID
   */
  async unfollowTarget(targetType, targetId) {
    try {
      const response = await request.delete(`/follows/${targetType}/${targetId}`)
      return response
    } catch (error) {
      console.error('取消关注失败:', error)
      throw error
    }
  },

  /**
   * 切换关注状态
   * @param {string} targetType - 目标类型 (USER, QUESTION)
   * @param {number} targetId - 目标ID
   */
  async toggleFollow(targetType, targetId) {
    try {
      // 先检查当前关注状态
      const isFollowed = await this.checkFollowStatus(targetType, targetId)
      
      if (isFollowed) {
        await this.unfollowTarget(targetType, targetId)
        return { success: true, isFollowed: false, message: '取消关注成功' }
      } else {
        await this.followTarget(targetType, targetId)
        return { success: true, isFollowed: true, message: '关注成功' }
      }
    } catch (error) {
      console.error('切换关注状态失败:', error)
      throw error
    }
  },

  /**
   * 检查关注状态
   * @param {string} targetType - 目标类型 (USER, QUESTION)
   * @param {number} targetId - 目标ID
   */
  async checkFollowStatus(targetType, targetId) {
    try {
      const response = await request.get(`/follows/${targetType}/${targetId}/status`)
      return response.data || false
    } catch (error) {
      console.error('检查关注状态失败:', error)
      throw error
    }
  },

  /**
   * 获取关注数
   * @param {string} targetType - 目标类型 (USER, QUESTION)
   * @param {number} targetId - 目标ID
   */
  async getFollowCount(targetType, targetId) {
    try {
      const response = await request.get(`/follows/${targetType}/${targetId}/count`)
      return response.data || 0
    } catch (error) {
      console.error('获取关注数失败:', error)
      throw error
    }
  },

  /**
   * 获取关注信息（包含状态和数量）
   * @param {string} targetType - 目标类型 (USER, QUESTION)
   * @param {number} targetId - 目标ID
   */
  async getFollowInfo(targetType, targetId) {
    try {
      const response = await request.get(`/follows/${targetType}/${targetId}/info`)
      return response.data || { count: 0, isFollowed: false }
    } catch (error) {
      console.error('获取关注信息失败:', error)
      throw error
    }
  },

  /**
   * 获取用户关注的用户列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   */
  async getUserFollowedUsers(page = 0, size = 10, userId = null) {
    try {
      const response = await request.get('/follows/users', {
        params: { page, size, ...(userId ? { userId } : {}) }
      })
      return response
    } catch (error) {
      console.error('获取用户关注列表失败:', error)
      throw error
    }
  },

  /**
   * 获取用户的粉丝列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   */
  async getUserFollowers(page = 0, size = 10, userId = null) {
    try {
      const response = await request.get('/follows/followers', {
        params: { page, size, ...(userId ? { userId } : {}) }
      })
      return response
    } catch (error) {
      console.error('获取粉丝列表失败:', error)
      throw error
    }
  },

  /**
   * 获取用户关注的问答列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   */
  async getUserFollowedQuestions(page = 0, size = 10) {
    try {
      const response = await request.get('/follows/questions', {
        params: { page, size }
      })
      return response
    } catch (error) {
      console.error('获取用户关注问答失败:', error)
      throw error
    }
  },

  /**
   * 获取关注统计
   */
  async getFollowStats() {
    try {
      const response = await request.get('/follows/stats')
      return response.data || { followingUsers: 0, followingQuestions: 0, followers: 0 }
    } catch (error) {
      console.error('获取关注统计失败:', error)
      throw error
    }
  },

  // 兼容旧的API调用方式
  /**
   * 关注用户
   * @param {number} userId - 用户ID
   */
  async followUser(userId) {
    return this.followTarget('USER', userId)
  },

  /**
   * 取消关注用户
   * @param {number} userId - 用户ID
   */
  async unfollowUser(userId) {
    return this.unfollowTarget('USER', userId)
  },

  /**
   * 检查是否关注了用户
   * @param {number} userId - 用户ID
   */
  async checkFollowingUser(userId) {
    return this.checkFollowStatus('USER', userId)
  },

  /**
   * 关注问答
   * @param {number} questionId - 问答ID
   */
  async followQuestion(questionId) {
    return this.followTarget('QUESTION', questionId)
  },

  /**
   * 取消关注问答
   * @param {number} questionId - 问答ID
   */
  async unfollowQuestion(questionId) {
    return this.unfollowTarget('QUESTION', questionId)
  },

  /**
   * 检查是否关注了问答
   * @param {number} questionId - 问答ID
   */
  async checkFollowingQuestion(questionId) {
    return this.checkFollowStatus('QUESTION', questionId)
  },

  /**
   * 获取用户关注的用户的文章列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   */
  async getFollowingUserArticles(page = 0, size = 10) {
    try {
      const response = await request.get('/follows/users/articles', {
        params: { page, size }
      })
      return response
    } catch (error) {
      console.error('获取关注用户文章失败:', error)
      throw error
    }
  },

  /**
   * 获取用户关注的用户的问题列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   */
  async getFollowingUserQuestions(page = 0, size = 10) {
    try {
      const response = await request.get('/follows/users/questions', {
        params: { page, size }
      })
      return response
    } catch (error) {
      console.error('获取关注用户问题失败:', error)
      throw error
    }
  }
}

export default followApi
