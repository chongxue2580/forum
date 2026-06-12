/**
 * 认证相关工具函数
 */

/**
 * 判断用户是否为管理员
 * @param {Object} user - 用户对象
 * @returns {boolean} - 是否为管理员
 */
export function isAdmin(user) {
  if (!user || !user.role) {
    return false
  }
  const role = user.role.toUpperCase()
  return role === 'ADMIN' || role === 'SUPER_ADMIN'
}

/**
 * 判断用户是否为普通用户
 * @param {Object} user - 用户对象
 * @returns {boolean} - 是否为普通用户
 */
export function isUser(user) {
  if (!user || !user.role) {
    return false
  }
  const role = user.role.toUpperCase()
  return role === 'USER'
}

/**
 * 从localStorage获取用户信息
 * @returns {Object|null} - 用户信息对象或null
 */
export function getUserInfo() {
  try {
    const userInfoJson = localStorage.getItem('userInfo')
    if (!userInfoJson) {
      return null
    }
    return JSON.parse(userInfoJson)
  } catch (error) {
    console.error('解析用户信息失败:', error)
    return null
  }
}

/**
 * 检查用户是否已登录
 * @returns {boolean} - 是否已登录
 */
export function isAuthenticated() {
  const token = localStorage.getItem('token')
  const userInfo = getUserInfo()
  return !!(token && userInfo)
}

/**
 * 检查用户是否有管理员权限
 * @returns {boolean} - 是否有管理员权限
 */
export function hasAdminPermission() {
  const userInfo = getUserInfo()
  return isAdmin(userInfo)
}

/**
 * 清除用户认证信息
 */
export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('adminToken')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('adminUser')
}

/**
 * 获取当前用户的角色
 * @returns {string|null} - 用户角色
 */
export function getUserRole() {
  const userInfo = getUserInfo()
  return userInfo?.role || null
}
