/**
 * 统一头像 URL 解析。
 * 后端头像保存在 uploads/images/avatars/ 下，返回 /uploads/images/avatars/xxx。
 * 开发环境通过 Vite 把 /uploads 代理到后端 8080。
 *
 * - 空值返回 ''（由调用方回退到文字首字母）
 * - 绝对地址 / data URI 原样返回
 * - 以 / 开头的相对路径原样返回（含 /uploads、/images 等，交给代理或静态资源处理）
 * - 其余视为纯文件名，拼到默认头像目录下
 *
 * @param {string} raw 原始头像字段（可能是 avatarUrl 或 avatar）
 * @returns {string} 可直接用于 <img :src> 的地址
 */
export function resolveAvatarUrl(raw) {
  if (!raw) return ''
  const value = String(raw).trim()
  if (!value) return ''
  if (/^(https?:)?\/\//i.test(value) || value.startsWith('data:')) return value
  if (value.startsWith('/')) return value
  return `/uploads/images/avatars/${value}`
}

/**
 * 从可能字段不统一的对象上取头像并解析。
 * 兼容后端 avatarUrl 与前端历史使用的 avatar 字段。
 * @param {object} source 用户/作者对象
 * @returns {string}
 */
export function resolveAvatarFrom(source) {
  if (!source) return ''
  return resolveAvatarUrl(source.avatarUrl || source.avatar || '')
}

/**
 * 取名称首字母作为无头像时的占位。
 * @param {string} name
 * @returns {string}
 */
export function avatarInitial(name) {
  if (!name) return '用'
  return String(name).trim().charAt(0).toUpperCase()
}
