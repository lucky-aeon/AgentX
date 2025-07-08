/**
 * 日期时间处理工具函数
 * 解决时区转换问题
 */

/**
 * 将本地时间转换为本地时区的ISO字符串
 * 避免toISOString()自动转换为UTC时间的问题
 */
export function toLocalISOString(date: Date): string {
  const offset = date.getTimezoneOffset()
  const localDate = new Date(date.getTime() - (offset * 60 * 1000))
  return localDate.toISOString().slice(0, -1) // 移除末尾的'Z'
}

/**
 * 保存本地时间为字符串，避免时区转换
 * 用于前端组件内部存储时间
 */
export function saveLocalDateTime(date: Date): string {
  // 使用本地时间格式，避免时区转换
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

/**
 * 将本地时间转换为适合后端的格式
 * 格式: YYYY-MM-DDTHH:mm:ss (ISO 8601格式，兼容Java LocalDateTime)
 */
export function toBackendDateTimeString(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

/**
 * 将本地时间转换为适合后端的时间格式
 * 格式: HH:mm:ss
 */
export function toBackendTimeString(date: Date): string {
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  
  return `${hours}:${minutes}:${seconds}`
}

/**
 * 从后端日期时间字符串创建本地Date对象
 * 支持ISO格式 (YYYY-MM-DDTHH:mm:ss) 和传统格式 (YYYY-MM-DD HH:mm:ss)
 */
export function fromBackendDateTimeString(dateTimeString: string): Date {
  // 如果字符串包含'T'，说明是ISO格式
  if (dateTimeString.includes('T')) {
    return new Date(dateTimeString)
  }
  
  // 否则假设是 'YYYY-MM-DD HH:mm:ss' 格式，转换为ISO格式
  return new Date(dateTimeString.replace(' ', 'T'))
}

/**
 * 格式化显示时间（本地时区）
 */
export function formatDisplayDateTime(date: Date): string {
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

/**
 * 格式化显示时间（仅时间部分）
 */
export function formatDisplayTime(date: Date): string {
  return date.toLocaleString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
} 