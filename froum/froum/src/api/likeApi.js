import request from '@/utils/request'

/**
 * 点赞相关API
 */
export const likeApi = {
  /**
   * 点赞目标
   * @param {string} targetType - 目标类型 (ARTICLE, QUESTION, COMMENT)
   * @param {number} targetId - 目标ID
   */
  async likeTarget(targetType, targetId) {
    try {
      const response = await request.post(`/likes/${targetType}/${targetId}`)
      return response
    } catch (error) {
      console.error('点赞失败:', error)
      throw error
    }
  },

  /**
   * 取消点赞目标
   * @param {string} targetType - 目标类型 (ARTICLE, QUESTION, COMMENT)
   * @param {number} targetId - 目标ID
   */
  async unlikeTarget(targetType, targetId) {
    try {
      const response = await request.delete(`/likes/${targetType}/${targetId}`)
      return response
    } catch (error) {
      console.error('取消点赞失败:', error)
      throw error
    }
  },

  /**
   * 切换点赞状态
   * @param {string} targetType - 目标类型 (ARTICLE, QUESTION, COMMENT)
   * @param {number} targetId - 目标ID
   */
  async toggleLike(targetType, targetId) {
    try {
      // 先检查当前点赞状态
      const isLiked = await this.checkLikeStatus(targetType, targetId)
      
      if (isLiked) {
        await this.unlikeTarget(targetType, targetId)
        return { success: true, isLiked: false, message: '取消点赞成功' }
      } else {
        await this.likeTarget(targetType, targetId)
        return { success: true, isLiked: true, message: '点赞成功' }
      }
    } catch (error) {
      console.error('切换点赞状态失败:', error)
      throw error
    }
  },

  /**
   * 检查点赞状态
   * @param {string} targetType - 目标类型 (ARTICLE, QUESTION, COMMENT)
   * @param {number} targetId - 目标ID
   */
  async checkLikeStatus(targetType, targetId) {
    try {
      const response = await request.get(`/likes/${targetType}/${targetId}/status`)
      return response.data || false
    } catch (error) {
      console.error('检查点赞状态失败:', error)
      throw error
    }
  },

  /**
   * 获取点赞数
   * @param {string} targetType - 目标类型 (ARTICLE, QUESTION, COMMENT)
   * @param {number} targetId - 目标ID
   */
  async getLikeCount(targetType, targetId) {
    try {
      const response = await request.get(`/likes/${targetType}/${targetId}/count`)
      return response.data || 0
    } catch (error) {
      console.error('获取点赞数失败:', error)
      throw error
    }
  },

  /**
   * 获取点赞信息（包含状态和数量）
   * @param {string} targetType - 目标类型 (ARTICLE, QUESTION, COMMENT)
   * @param {number} targetId - 目标ID
   */
  async getLikeInfo(targetType, targetId) {
    try {
      const response = await request.get(`/likes/${targetType}/${targetId}/info`)
      const info = response.data || { count: 0, isLiked: false }
      return {
        ...info,
        isLiked: Boolean(info.isLiked ?? info.liked)
      }
    } catch (error) {
      console.error('获取点赞信息失败:', error)
      throw error
    }
  },

  /**
   * 获取用户点赞的文章列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   */
  async getUserLikedArticles(page = 0, size = 10) {
    try {
      const response = await request.get('/likes/articles', {
        params: { page, size }
      })
      return response
    } catch (error) {
      console.error('获取用户点赞文章失败:', error)
      throw error
    }
  },

  /**
   * 获取用户点赞的问答列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   */
  async getUserLikedQuestions(page = 0, size = 10) {
    try {
      const response = await request.get('/likes/questions', {
        params: { page, size }
      })
      return response
    } catch (error) {
      console.error('获取用户点赞问答失败:', error)
      throw error
    }
  },

  /**
   * 获取用户点赞的评论列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   */
  async getUserLikedComments(page = 0, size = 10) {
    try {
      const response = await request.get('/likes/comments', {
        params: { page, size }
      })
      return response
    } catch (error) {
      console.error('获取用户点赞评论失败:', error)
      throw error
    }
  },

  /**
   * 获取用户总点赞数
   */
  async getUserTotalLikeCount() {
    try {
      const response = await request.get('/likes/user/total')
      return response.data || 0
    } catch (error) {
      console.error('获取用户总点赞数失败:', error)
      throw error
    }
  },

  // 兼容旧的API调用方式
  /**
   * 点赞文章
   * @param {number} articleId - 文章ID
   */
  async likeArticle(articleId) {
    return this.likeTarget('ARTICLE', articleId)
  },

  /**
   * 取消点赞文章
   * @param {number} articleId - 文章ID
   */
  async unlikeArticle(articleId) {
    return this.unlikeTarget('ARTICLE', articleId)
  },

  /**
   * 检查用户是否点赞了文章
   * @param {number} articleId - 文章ID
   */
  async hasLikedArticle(articleId) {
    return this.checkLikeStatus('ARTICLE', articleId)
  },

  /**
   * 点赞问答
   * @param {number} questionId - 问答ID
   */
  async likeQuestion(questionId) {
    return this.likeTarget('QUESTION', questionId)
  },

  /**
   * 取消点赞问答
   * @param {number} questionId - 问答ID
   */
  async unlikeQuestion(questionId) {
    return this.unlikeTarget('QUESTION', questionId)
  },

  /**
   * 检查用户是否点赞了问答
   * @param {number} questionId - 问答ID
   */
  async hasLikedQuestion(questionId) {
    return this.checkLikeStatus('QUESTION', questionId)
  },

  /**
   * 点赞评论
   * @param {number} commentId - 评论ID
   */
  async likeComment(commentId) {
    return this.likeTarget('COMMENT', commentId)
  },

  /**
   * 取消点赞评论
   * @param {number} commentId - 评论ID
   */
  async unlikeComment(commentId) {
    return this.unlikeTarget('COMMENT', commentId)
  },

  /**
   * 检查用户是否点赞了评论
   * @param {number} commentId - 评论ID
   */
  async hasLikedComment(commentId) {
    return this.checkLikeStatus('COMMENT', commentId)
  }
}

export default likeApi
