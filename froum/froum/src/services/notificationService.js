import request from '../utils/request'

const unwrapPage = (response) => {
  const page = response?.data || {}
  return {
    content: Array.isArray(page.content) ? page.content : Array.isArray(response?.data) ? response.data : [],
    totalElements: page.totalElements ?? 0,
    totalPages: page.totalPages ?? 1,
    page: (page.number ?? 0) + 1,
    pageSize: page.size ?? 10
  }
}

export const notificationService = {
  async getUserNotifications({ page = 1, pageSize = 10 } = {}) {
    const response = await request.get('/notifications', {
      params: { page, pageSize }
    })
    return {
      ...response,
      data: unwrapPage(response)
    }
  },

  async getNotificationsByType(type, { page = 1, pageSize = 10 } = {}) {
    const response = await request.get(`/notifications/type/${type}`, {
      params: { page, pageSize }
    })
    return {
      ...response,
      data: unwrapPage(response)
    }
  },

  async getUnreadNotifications({ page = 1, pageSize = 10 } = {}) {
    const response = await request.get('/notifications/unread', {
      params: { page, pageSize }
    })
    return {
      ...response,
      data: unwrapPage(response)
    }
  },

  async getReadNotifications({ page = 1, pageSize = 10 } = {}) {
    const response = await request.get('/notifications/read', {
      params: { page, pageSize }
    })
    return {
      ...response,
      data: unwrapPage(response)
    }
  },

  async getUnreadCount() {
    const response = await request.get('/notifications/unread/count')
    return response?.data ?? 0
  },

  async getRecentNotifications() {
    const response = await request.get('/notifications/recent')
    return response?.data || []
  },

  async sendDirectMessage(toUserId, content) {
    return request.post('/notifications/messages', {
      toUserId,
      content
    })
  },

  async markNotificationAsRead(id) {
    return request.post(`/notifications/${id}/read`)
  },

  async markNotificationsAsRead(ids) {
    return request.post('/notifications/read', ids)
  },

  async markAllNotificationsAsRead() {
    return request.post('/notifications/read-all')
  },

  async deleteNotification(id) {
    return request.delete(`/notifications/${id}`)
  },

  async deleteAllNotifications() {
    return request.delete('/notifications/all')
  },

  async deleteAllReadNotifications() {
    return request.delete('/notifications/read')
  }
}

export default notificationService
