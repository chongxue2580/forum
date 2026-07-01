import request from '../utils/request'

export const reportService = {
  async createReport({ targetType, targetId, reason, description = '' }) {
    return request.post('/reports', {
      targetType,
      targetId: Number(targetId),
      reason: reason?.trim(),
      description: description?.trim() || null
    })
  },

  async getMyReports(params = {}) {
    return request.get('/reports/my', {
      params: {
        page: params.page || 1,
        pageSize: params.pageSize || params.size || 10,
        status: params.status || undefined
      }
    })
  },

  async getMyReport(id) {
    return request.get(`/reports/${id}`)
  }
}
