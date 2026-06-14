import request from '@/utils/request'

// 管理员认证相关
export function adminLogin(data) {
  return request({
    url: '/admin/login',
    method: 'post',
    data
  })
}

export function confirmAdminTwoFactorSetup(data) {
  return request({
    url: '/admin/2fa/setup/confirm',
    method: 'post',
    data
  })
}

export function adminLogout() {
  return request({
    url: '/admin/logout',
    method: 'post'
  })
}

// 仪表板相关
export function getDashboardOverview() {
  return request({
    url: '/admin/dashboard/overview',
    method: 'get'
  })
}

export function getTodayStatistics() {
  return request({
    url: '/admin/dashboard/today',
    method: 'get'
  })
}

export function getSystemStatus() {
  return request({
    url: '/admin/dashboard/system-status',
    method: 'get'
  })
}

export function getRecentActivities() {
  return request({
    url: '/admin/dashboard/recent-activities',
    method: 'get'
  })
}

export function getDataTrends() {
  return request({
    url: '/admin/dashboard/trends',
    method: 'get'
  })
}

// 用户管理相关
export function getAllUsers(params) {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

export function getUserById(id) {
  return request({
    url: `/admin/users/${id}`,
    method: 'get'
  })
}

export function disableUser(id, reason) {
  return request({
    url: `/admin/users/${id}/disable`,
    method: 'post',
    params: { reason }
  })
}

export function enableUser(id) {
  return request({
    url: `/admin/users/${id}/enable`,
    method: 'post'
  })
}

export function resetUserPassword(id) {
  return request({
    url: `/admin/users/${id}/reset-password`,
    method: 'post'
  })
}

export function changeUserRole(id, role) {
  return request({
    url: `/admin/users/${id}/role`,
    method: 'post',
    params: { role }
  })
}

export function deleteUser(id) {
  return request({
    url: `/admin/users/${id}`,
    method: 'delete'
  })
}

export function batchDisableUsers(ids, reason) {
  return request({
    url: '/admin/users/batch-disable',
    method: 'post',
    data: { ids, reason }
  })
}

export function batchEnableUsers(ids) {
  return request({
    url: '/admin/users/batch-enable',
    method: 'post',
    data: { ids }
  })
}

export function batchDeleteUsers(ids) {
  return request({
    url: '/admin/users/batch-delete',
    method: 'post',
    data: { ids }
  })
}

export function getUserStatistics() {
  return request({
    url: '/admin/users/statistics',
    method: 'get'
  })
}

export function searchUsers(keyword, params) {
  return request({
    url: '/admin/users/search',
    method: 'get',
    params: { keyword, ...params }
  })
}

export function getRecentUsers(params) {
  return request({
    url: '/admin/users/recent',
    method: 'get',
    params
  })
}

// 文章管理相关
export function getAllArticles(params) {
  return request({
    url: '/admin/articles',
    method: 'get',
    params
  })
}

export function approveArticle(id) {
  return request({
    url: `/admin/articles/${id}/approve`,
    method: 'post'
  })
}

export function rejectArticle(id, reason) {
  return request({
    url: `/admin/articles/${id}/reject`,
    method: 'post',
    params: { reason }
  })
}

export function pinArticle(id) {
  return request({
    url: `/admin/articles/${id}/pin`,
    method: 'post'
  })
}

export function unpinArticle(id) {
  return request({
    url: `/admin/articles/${id}/unpin`,
    method: 'post'
  })
}

export function featureArticle(id) {
  return request({
    url: `/admin/articles/${id}/feature`,
    method: 'post'
  })
}

export function unfeatureArticle(id) {
  return request({
    url: `/admin/articles/${id}/unfeature`,
    method: 'post'
  })
}

export function deleteArticle(id) {
  return request({
    url: `/admin/articles/${id}`,
    method: 'delete'
  })
}

export function getArticleStatistics() {
  return request({
    url: '/admin/articles/statistics',
    method: 'get'
  })
}

// 问答管理相关
export function getAllQuestions(params) {
  return request({
    url: '/admin/questions',
    method: 'get',
    params
  })
}

export function approveQuestion(id) {
  return request({
    url: `/admin/questions/${id}/approve`,
    method: 'post'
  })
}

export function rejectQuestion(id, reason) {
  return request({
    url: `/admin/questions/${id}/reject`,
    method: 'post',
    params: { reason }
  })
}

export function pinQuestion(id) {
  return request({
    url: `/admin/questions/${id}/pin`,
    method: 'post'
  })
}

export function unpinQuestion(id) {
  return request({
    url: `/admin/questions/${id}/unpin`,
    method: 'post'
  })
}

export function featureQuestion(id) {
  return request({
    url: `/admin/questions/${id}/feature`,
    method: 'post'
  })
}

export function unfeatureQuestion(id) {
  return request({
    url: `/admin/questions/${id}/unfeature`,
    method: 'post'
  })
}

export function solveQuestion(id) {
  return request({
    url: `/admin/questions/${id}/solve`,
    method: 'post'
  })
}

export function unsolveQuestion(id) {
  return request({
    url: `/admin/questions/${id}/unsolve`,
    method: 'post'
  })
}

export function deleteQuestion(id) {
  return request({
    url: `/admin/questions/${id}`,
    method: 'delete'
  })
}

export function getQuestionStatistics() {
  return request({
    url: '/admin/questions/statistics',
    method: 'get'
  })
}

// 分类管理相关
export function getAllCategories(params) {
  return request({
    url: '/admin/categories',
    method: 'get',
    params
  })
}

export function createCategory(data) {
  return request({
    url: '/admin/categories',
    method: 'post',
    data
  })
}

export function updateCategory(id, data) {
  return request({
    url: `/admin/categories/${id}`,
    method: 'put',
    data
  })
}

export function approveCategory(id) {
  return request({
    url: `/admin/categories/${id}/approve`,
    method: 'post'
  })
}

export function rejectCategory(id) {
  return request({
    url: `/admin/categories/${id}/reject`,
    method: 'post'
  })
}

export function deleteCategory(id) {
  return request({
    url: `/admin/categories/${id}`,
    method: 'delete'
  })
}

// 标签管理相关
export function getAllTags(params) {
  return request({
    url: '/admin/tags',
    method: 'get',
    params
  })
}

export function createTag(data) {
  return request({
    url: '/admin/tags',
    method: 'post',
    data
  })
}

export function updateTag(id, data) {
  return request({
    url: `/admin/tags/${id}`,
    method: 'put',
    data
  })
}

export function approveTag(id) {
  return request({
    url: `/admin/tags/${id}/approve`,
    method: 'post'
  })
}

export function rejectTag(id) {
  return request({
    url: `/admin/tags/${id}/reject`,
    method: 'post'
  })
}

export function deleteTag(id) {
  return request({
    url: `/admin/tags/${id}`,
    method: 'delete'
  })
}

// 评论管理相关
export function getAllComments(params) {
  return request({
    url: '/admin/comments',
    method: 'get',
    params
  })
}

export function deleteComment(id) {
  return request({
    url: `/admin/comments/${id}`,
    method: 'delete'
  })
}

export function restoreComment(id) {
  return request({
    url: `/admin/comments/${id}/restore`,
    method: 'put'
  })
}

export function batchDeleteComments(commentIds) {
  return request({
    url: '/admin/comments/batch',
    method: 'delete',
    data: commentIds
  })
}

export function getCommentStatistics() {
  return request({
    url: '/admin/comments/statistics',
    method: 'get'
  })
}

export function getUserComments(userId, params) {
  return request({
    url: `/admin/comments/user/${userId}`,
    method: 'get',
    params
  })
}

export function getRecentComments(params) {
  return request({
    url: '/admin/comments/recent',
    method: 'get',
    params
  })
}

// 操作日志相关
export function getOperationLogs(params) {
  return request({
    url: '/admin/operation-logs',
    method: 'get',
    params
  })
}

// 系统设置相关
export function getSystemSettings() {
  return request({
    url: '/admin/settings',
    method: 'get'
  })
}

export function updateSystemSettings(data) {
  return request({
    url: '/admin/settings',
    method: 'put',
    data
  })
}

export function resetSystemSettings() {
  return request({
    url: '/admin/settings/reset',
    method: 'post'
  })
}

export function testSystemEmailSettings(data) {
  return request({
    url: '/admin/settings/email/test',
    method: 'post',
    data
  })
}

export function clearSystemCache() {
  return request({
    url: '/admin/settings/cache/clear',
    method: 'post'
  })
}
