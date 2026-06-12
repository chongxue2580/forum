/**
 * 日期格式化工具函数
 */

/**
 * 格式化日期时间
 * @param {string|Date|number} time - 时间值
 * @param {Object} options - 格式化选项
 * @returns {string} 格式化后的日期字符串
 */
export function formatDateTime(time, options = {}) {
  if (!time) return '未知时间'

  // console.log('formatDateTime called with:', time, typeof time)

  try {
    let date

    // 处理不同的日期格式
    if (Array.isArray(time)) {
      // 处理Spring Boot LocalDateTime数组格式 [year, month, day, hour, minute, second, nanosecond]
      // console.log('Processing array format date:', time)

      if (time.length >= 3) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = time
        // 注意：JavaScript的月份是从0开始的，所以需要减1
        date = new Date(year, month - 1, day, hour, minute, second)
        // console.log('Array date parsed to:', date)
      } else {
        console.warn('Invalid array date format:', time)
        return '数组日期格式无效'
      }
    } else if (typeof time === 'string') {
      // 清理字符串，移除多余的空格
      const cleanTime = time.trim()

      // 处理LocalDateTime格式 (YYYY-MM-DDTHH:mm:ss)
      if (cleanTime.includes('T')) {
        date = new Date(cleanTime)
      } else if (cleanTime.includes('-') && cleanTime.includes(':')) {
        // 处理 YYYY-MM-DD HH:mm:ss 格式
        date = new Date(cleanTime.replace(' ', 'T'))
      } else if (cleanTime.match(/^\d{4}-\d{2}-\d{2}$/)) {
        // 处理仅日期格式 YYYY-MM-DD
        date = new Date(cleanTime + 'T00:00:00')
      } else {
        // 处理其他字符串格式
        date = new Date(cleanTime)
      }
    } else if (time instanceof Date) {
      date = time
    } else if (typeof time === 'number') {
      // 处理时间戳
      date = new Date(time)
    } else {
      console.warn('Unsupported time format:', time, typeof time)
      return '不支持的时间格式'
    }

    // 检查日期是否有效
    if (!date || isNaN(date.getTime())) {
      console.warn('Invalid date after parsing:', time, '→', date)
      return '日期无效'
    }

    // 默认格式化选项
    const defaultOptions = {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }

    const formatOptions = { ...defaultOptions, ...options }

    const result = date.toLocaleDateString('zh-CN', formatOptions)
    // console.log('formatDateTime result:', time, '→', result)
    return result
  } catch (error) {
    console.error('Date formatting error:', error, 'for time:', time)
    return '日期格式错误'
  }
}

/**
 * 格式化日期（不包含时间）
 * @param {string|Date|number} time - 时间值
 * @returns {string} 格式化后的日期字符串
 */
export function formatDate(time) {
  return formatDateTime(time, {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

/**
 * 格式化相对时间（如：2小时前、3天前）
 * @param {string|Date|number} time - 时间值
 * @returns {string} 相对时间字符串
 */
export function formatRelativeTime(time) {
  if (!time) return '未知时间'

  try {
    let date

    // 处理不同的日期格式
    if (Array.isArray(time)) {
      // 处理Spring Boot LocalDateTime数组格式
      if (time.length >= 3) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = time
        date = new Date(year, month - 1, day, hour, minute, second)
      } else {
        return formatDateTime(time)
      }
    } else if (typeof time === 'string') {
      if (time.includes('T')) {
        date = new Date(time)
      } else if (time.includes('-') && time.includes(':')) {
        date = new Date(time.replace(' ', 'T'))
      } else {
        date = new Date(time)
      }
    } else if (time instanceof Date) {
      date = time
    } else {
      date = new Date(time)
    }

    if (isNaN(date.getTime())) {
      return formatDateTime(time)
    }
    
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffSeconds = Math.floor(diffMs / 1000)
    const diffMinutes = Math.floor(diffSeconds / 60)
    const diffHours = Math.floor(diffMinutes / 60)
    const diffDays = Math.floor(diffHours / 24)
    const diffMonths = Math.floor(diffDays / 30)
    const diffYears = Math.floor(diffDays / 365)
    
    if (diffSeconds < 60) {
      return '刚刚'
    } else if (diffMinutes < 60) {
      return `${diffMinutes}分钟前`
    } else if (diffHours < 24) {
      return `${diffHours}小时前`
    } else if (diffDays < 30) {
      return `${diffDays}天前`
    } else if (diffMonths < 12) {
      return `${diffMonths}个月前`
    } else {
      return `${diffYears}年前`
    }
  } catch (error) {
    console.error('Relative time formatting error:', error)
    return formatDateTime(time)
  }
}

/**
 * 获取时间字段值（处理多种可能的字段名）
 * @param {Object} obj - 包含时间字段的对象
 * @param {string} type - 时间类型：'created' 或 'updated'
 * @returns {string|null} 时间值
 */
export function getTimeField(obj, type = 'created') {
  if (!obj) {
    // console.log('getTimeField: obj is null or undefined')
    return null
  }

  // console.log('getTimeField called with:', obj, 'type:', type)
  // console.log('Available fields:', Object.keys(obj))

  let timeValue = null

  if (type === 'created') {
    timeValue = obj.createdAt || obj.createTime || obj.created_at || obj.publishTime || obj.publish_time
  } else if (type === 'updated') {
    timeValue = obj.updatedAt || obj.updateTime || obj.updated_at || obj.modifyTime || obj.modify_time
  }

  // console.log('getTimeField result:', timeValue)
  return timeValue
}
