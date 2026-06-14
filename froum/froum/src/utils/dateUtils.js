const pad = (value) => String(value).padStart(2, '0')

export function toDate(time) {
  if (!time) return null

  if (time instanceof Date) {
    return Number.isNaN(time.getTime()) ? null : time
  }

  if (Array.isArray(time)) {
    if (time.length < 3) return null
    const [year, month, day, hour = 0, minute = 0, second = 0] = time
    const date = new Date(year, month - 1, day, hour, minute, second)
    return Number.isNaN(date.getTime()) ? null : date
  }

  if (typeof time === 'number') {
    const date = new Date(time)
    return Number.isNaN(date.getTime()) ? null : date
  }

  if (typeof time === 'string') {
    const cleanTime = time.trim()
    if (!cleanTime) return null
    const normalized = cleanTime.includes(' ') && !cleanTime.includes('T')
      ? cleanTime.replace(' ', 'T')
      : cleanTime
    const date = new Date(normalized)
    return Number.isNaN(date.getTime()) ? null : date
  }

  return null
}

const formatParts = (date, withSeconds = false) => {
  const value = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
  return withSeconds ? `${value}:${pad(date.getSeconds())}` : value
}

export function formatDateTime(time, options = null) {
  const date = toDate(time)
  if (!date) return '未知时间'

  if (options && Object.keys(options).length > 0) {
    return date.toLocaleString('zh-CN', options)
  }

  return formatParts(date)
}

/**
 * 格式化日期（不包含时间）
 * @param {string|Date|number} time - 时间值
 * @returns {string} 格式化后的日期字符串
 */
export function formatDate(time) {
  const date = toDate(time)
  if (!date) return '未知时间'
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/**
 * 格式化相对时间（如：2小时前、3天前）
 * @param {string|Date|number} time - 时间值
 * @returns {string} 相对时间字符串
 */
export function formatRelativeTime(time) {
  const date = toDate(time)
  if (!date) return '未知时间'
    
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  if (diffMs < 0) return formatDateTime(date)

  const diffSeconds = Math.floor(diffMs / 1000)
  const diffMinutes = Math.floor(diffSeconds / 60)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)
  const diffMonths = Math.floor(diffDays / 30)
  const diffYears = Math.floor(diffDays / 365)

  if (diffSeconds < 60) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 30) return `${diffDays}天前`
  if (diffMonths < 12) return `${diffMonths}个月前`
  return `${diffYears}年前`
}

export function formatFriendlyTime(time) {
  const date = toDate(time)
  if (!date) return '未知时间'

  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const targetDay = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const dayDiff = Math.floor((today - targetDay) / 86400000)
  const timeText = `${pad(date.getHours())}:${pad(date.getMinutes())}`

  if (dayDiff === 0) {
    const diffMs = now.getTime() - date.getTime()
    if (diffMs >= 0 && diffMs < 3600000) return formatRelativeTime(date)
    return `今天 ${timeText}`
  }
  if (dayDiff === 1) return `昨天 ${timeText}`
  if (date.getFullYear() === now.getFullYear()) {
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${timeText}`
  }
  return formatDateTime(date)
}

/**
 * 获取时间字段值（处理多种可能的字段名）
 * @param {Object} obj - 包含时间字段的对象
 * @param {string} type - 时间类型：'created' 或 'updated'
 * @returns {string|null} 时间值
 */
export function getTimeField(obj, type = 'created') {
  if (!obj) {
    return null
  }

  let timeValue = null

  if (type === 'created') {
    timeValue = obj.createdAt || obj.createTime || obj.created_at || obj.publishTime || obj.publish_time
  } else if (type === 'updated') {
    timeValue = obj.updatedAt || obj.updateTime || obj.updated_at || obj.modifyTime || obj.modify_time
  }

  return timeValue
}
