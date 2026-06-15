import request from '../utils/request'

const unwrapPageContent = (response) => response?.data?.content || response?.data || response?.content || []

// 用户相关API
export const userApi = {
  // 获取用户资料
  async getUserProfile(userId) {
    try {
      const response = await request.get(`/user/profile/${userId}`)
      return response
    } catch (error) {
      console.error('获取用户资料失败:', error)
      throw error
    }
  },

  // 更新用户资料
  async updateUserProfile(userId, profileData) {
    try {
      const response = await request.put(`/user/profile/${userId}`, profileData)
      return response
    } catch (error) {
      console.error('更新用户资料失败:', error)
      throw error
    }
  },

  // 用户登录
  async login(loginData) {
    try {
      const response = await request.post('/user/login', loginData)
      return response // 响应拦截器已经返回了response.data，所以这里直接返回response
    } catch (error) {
      console.error('用户登录失败:', error)
      throw error
    }
  },

  // 用户注册
  async register(registerData) {
    try {
      const response = await request.post('/user/register', registerData)
      return response // 响应拦截器已经返回了response.data，所以这里直接返回response
    } catch (error) {
      console.error('用户注册失败:', error)
      throw error
    }
  },

  async sendRegistrationEmailCode(email) {
    try {
      return await request.post('/user/register/email-code', { email })
    } catch (error) {
      console.error('发送注册邮箱验证码失败:', error)
      throw error
    }
  },

  async sendForgotPasswordCode(account) {
    try {
      return await request.post('/user/forgot-password/email-code', { account })
    } catch (error) {
      console.error('发送忘记密码验证码失败:', error)
      throw error
    }
  },

  async resetForgotPassword(resetData) {
    try {
      return await request.post('/user/forgot-password/reset', resetData)
    } catch (error) {
      console.error('重置密码失败:', error)
      throw error
    }
  },

  async sendEmailChangeCode(email) {
    try {
      return await request.post('/user/profile/email-change/email-code', { email })
    } catch (error) {
      console.error('发送更换邮箱验证码失败:', error)
      throw error
    }
  },

  async sendPasswordChangeCode() {
    try {
      return await request.post('/user/profile/password-change/email-code')
    } catch (error) {
      console.error('发送修改密码验证码失败:', error)
      throw error
    }
  },

  // 获取当前用户信息
  async getCurrentUser() {
    try {
      const response = await request.get('/user/profile/me')
      return response
    } catch (error) {
      console.error('获取当前用户信息失败:', error)
      throw error
    }
  },

  async getTwoFactorStatus() {
    try {
      return await request.get('/user/profile/me/2fa/status')
    } catch (error) {
      console.error('获取两步验证状态失败:', error)
      throw error
    }
  },

  async setupTwoFactor() {
    try {
      return await request.post('/user/profile/me/2fa/setup')
    } catch (error) {
      console.error('生成两步验证绑定信息失败:', error)
      throw error
    }
  },

  async enableTwoFactor(code) {
    try {
      return await request.post('/user/profile/me/2fa/enable', { code })
    } catch (error) {
      console.error('启用两步验证失败:', error)
      throw error
    }
  },

  async disableTwoFactor(code) {
    try {
      return await request.post('/user/profile/me/2fa/disable', { code })
    } catch (error) {
      console.error('关闭两步验证失败:', error)
      throw error
    }
  },

  async getRecentUsers(limit = 4) {
    try {
      return await request.get('/user/recent-users', {
        params: { limit }
      })
    } catch (error) {
      console.error('获取最近用户失败:', error)
      throw error
    }
  },

  // 关注用户
  async followUser(targetUserId) {
    try {
      const response = await request.post(`/follows/USER/${targetUserId}`)
      return response
    } catch (error) {
      console.error('关注用户失败:', error)
      throw error
    }
  },

  // 取消关注用户
  async unfollowUser(targetUserId) {
    try {
      const response = await request.delete(`/follows/USER/${targetUserId}`)
      return response
    } catch (error) {
      console.error('取消关注用户失败:', error)
      throw error
    }
  },

  // 检查是否关注用户
  async checkFollowStatus(targetUserId) {
    try {
      const response = await request.get(`/follows/USER/${targetUserId}/status`)
      return Boolean(response.data)
    } catch (error) {
      console.error('检查关注状态失败:', error)
      throw error
    }
  },

  // 获取用户关注列表
  async getUserFollowing(userId, page = 0, size = 10) {
    try {
      const response = await request.get('/follows/users', {
        params: { userId, page, size }
      })
      return unwrapPageContent(response)
    } catch (error) {
      console.error('获取关注列表失败:', error)
      throw error
    }
  },

  // 获取用户粉丝列表
  async getUserFollowers(userId, page = 0, size = 10) {
    try {
      const response = await request.get('/follows/followers', {
        params: { userId, page, size }
      })
      return unwrapPageContent(response)
    } catch (error) {
      console.error('获取粉丝列表失败:', error)
      throw error
    }
  },

  // 切换关注状态
  async toggleFollow(targetUserId) {
    try {
      // 先检查当前关注状态
      const isFollowing = await this.checkFollowStatus(targetUserId)
      
      if (isFollowing) {
        await this.unfollowUser(targetUserId)
        return { success: true, isFollowing: false, message: '取消关注成功' }
      } else {
        await this.followUser(targetUserId)
        return { success: true, isFollowing: true, message: '关注成功' }
      }
    } catch (error) {
      console.error('切换关注状态失败:', error)
      throw error
    }
  },

  // 获取用户统计信息
  async getUserStats(userId) {
    try {
      const response = await request.get(`/user/profile/${userId}/stats`)
      return response
    } catch (error) {
      console.error('获取用户统计失败:', error)
      throw error
    }
  },

  // 更新用户密码
  async updatePassword(userId, passwordData) {
    try {
      const response = await request.put(`/user/profile/${userId}/password`, passwordData)
      return response
    } catch (error) {
      console.error('更新密码失败:', error)
      throw error
    }
  },

  // 获取用户的文章列表
  async getUserArticles(userId, page = 0, size = 10) {
    try {
      const response = await request.get(`/articles/user/${userId}?page=${page}&size=${size}`)
      return response
    } catch (error) {
      console.error('获取用户文章失败:', error)
      throw error
    }
  },

  // 获取用户的问题列表
  async getUserQuestions(userId, page = 0, size = 10) {
    try {
      const response = await request.get(`/questions/user/${userId}?page=${page}&size=${size}`)
      return response
    } catch (error) {
      console.error('获取用户问题失败:', error)
      throw error
    }
  },

  // 获取用户的回答列表
  async getUserAnswers(userId, page = 0, size = 10) {
    try {
      const response = await request.get(`/answers/user/${userId}?page=${page}&size=${size}`)
      return response
    } catch (error) {
      console.error('获取用户回答失败:', error)
      throw error
    }
  }
}

export default userApi
