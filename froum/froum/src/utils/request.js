import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

const stripDuplicatedApiPrefix = (config) => {
  if (typeof config.url === 'string' && config.url.startsWith('/api/')) {
    config.url = config.url.slice(4)
  }
  if (config.url === '/api') {
    config.url = '/'
  }
}

const createBusinessError = (response, data) => {
  const message = data?.message || data?.msg || '请求失败'
  const error = new Error(message)
  error.response = response
  error.data = data
  error.code = data?.code
  return error
}

const isAdminUrl = (url = '') => typeof url === 'string' && url.startsWith('/admin/')

const isAuthStateError = (message = '') => {
  return message.includes('用户不存在') ||
    message.includes('账号已被') ||
    message.includes('账户被') ||
    message.includes('登录已过期') ||
    message.includes('令牌无效') ||
    message.includes('token')
}

const isProtectedUserPath = (path = window.location.pathname) => {
  return path === '/messages' ||
    path === '/settings' ||
    path === '/following' ||
    path.startsWith('/article/new') ||
    path.endsWith('/edit') ||
    path.startsWith('/user/likes') ||
    path.startsWith('/user/follows')
}

const clearUserAuth = () => {
  const token = localStorage.getItem('token')
  const adminToken = localStorage.getItem('adminToken')

  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('userInfo')

  if (adminToken && adminToken === token) {
    localStorage.removeItem('adminToken')
    localStorage.removeItem('adminUser')
  }
}

const clearAdminAuth = () => {
  const token = localStorage.getItem('token')
  const adminToken = localStorage.getItem('adminToken')

  localStorage.removeItem('adminToken')
  localStorage.removeItem('adminUser')

  if (token && token === adminToken) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('userInfo')
  }
}

const handleUnauthorized = (error) => {
  const requestUrl = error.config?.url || ''
  const adminRequest = isAdminUrl(requestUrl) || window.location.pathname.startsWith('/admin/')

  if (adminRequest) {
    clearAdminAuth()
    if (window.location.pathname.startsWith('/admin/') && window.location.pathname !== '/admin/login') {
      window.location.href = '/admin/login'
    }
    return
  }

  clearUserAuth()
  if (isProtectedUserPath()) {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.href = `/login?redirect=${redirect}`
  }
}

const handleInvalidAuthState = (error) => {
  const requestUrl = error.config?.url || ''
  const adminRequest = isAdminUrl(requestUrl) || window.location.pathname.startsWith('/admin/')

  if (adminRequest) {
    clearAdminAuth()
  } else {
    clearUserAuth()
  }

  if (window.location.pathname.startsWith('/admin/') && window.location.pathname !== '/admin/login') {
    window.location.href = '/admin/login'
    return
  }

  if (isProtectedUserPath()) {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.href = `/login?redirect=${redirect}`
  }
}

request.interceptors.request.use(
  config => {
    stripDuplicatedApiPrefix(config)

    const userToken = localStorage.getItem('token')
    const adminToken = localStorage.getItem('adminToken')
    const isAdminRequest = typeof config.url === 'string' && config.url.startsWith('/admin/')

    if (isAdminRequest && adminToken) {
      config.headers.Authorization = `Bearer ${adminToken}`
    } else if (userToken) {
      config.headers.Authorization = `Bearer ${userToken}`
    }

    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const data = response.data
    const hasBusinessStatus = data && typeof data === 'object' && (
      Object.prototype.hasOwnProperty.call(data, 'success') ||
      Object.prototype.hasOwnProperty.call(data, 'code')
    )

    if (!hasBusinessStatus) {
      return data
    }

    const isSuccess = data.success === true || data.code === 200 || data.code === 1
    if (isSuccess) {
      return data
    }

    return Promise.reject(createBusinessError(response, data))
  },
  error => {
    if (error.response) {
      const { status, data } = error.response
      const responseMessage = data?.message || data?.msg || ''

      if (status === 401) {
        handleUnauthorized(error)
      }

      if (status === 403 && isAuthStateError(responseMessage)) {
        handleInvalidAuthState(error)
      }

      const messageMap = {
        401: '登录已过期，请重新登录',
        403: '没有权限访问该资源',
        404: '请求的资源不存在',
        500: '服务器内部错误'
      }
      ElMessage.error(responseMessage || messageMap[status] || `请求失败 (${status})`)
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error(error.message || '请求失败')
    }

    return Promise.reject(error)
  }
)

export default request
