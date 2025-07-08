import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * 设置cookie
 * @param name cookie名称
 * @param value cookie值
 * @param days 有效天数
 */
export function setCookie(name: string, value: string, days: number = 30) {
  let expires = ""
  if (days) {
    const date = new Date()
    date.setTime(date.getTime() + days * 24 * 60 * 60 * 1000)
    expires = "; expires=" + date.toUTCString()
  }
  document.cookie = name + "=" + encodeURIComponent(value) + expires + "; path=/; SameSite=Lax"
}

/**
 * 获取cookie
 * @param name cookie名称
 * @returns cookie值
 */
export function getCookie(name: string): string | null {
  const nameEQ = name + "="
  const ca = document.cookie.split(";")
  for (let i = 0; i < ca.length; i++) {
    let c = ca[i]
    while (c.charAt(0) === " ") c = c.substring(1, c.length)
    if (c.indexOf(nameEQ) === 0) return decodeURIComponent(c.substring(nameEQ.length, c.length))
  }
  return null
}

/**
 * 删除cookie
 * @param name cookie名称
 */
export function deleteCookie(name: string) {
  document.cookie = name + "=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;"
}

/**
 * 格式化日期为易读格式 YYYY-MM-DD
 */
export function formatDate(date: Date): string {
  return date.toISOString().split('T')[0];
}
