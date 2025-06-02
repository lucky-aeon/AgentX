"use client"

import React from 'react'
import { FileText, Image, Download } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface MessageFileDisplayProps {
  fileUrls: string[] // 文件URL列表
  className?: string // 额外的样式类
}

export default function MessageFileDisplay({
  fileUrls,
  className = ""
}: MessageFileDisplayProps) {
  if (!fileUrls || fileUrls.length === 0) {
    return null
  }

  // 获取文件名从URL
  const getFileNameFromUrl = (url: string): string => {
    try {
      const urlObj = new URL(url)
      const pathname = urlObj.pathname
      const fileName = pathname.substring(pathname.lastIndexOf('/') + 1)
      
      // 如果文件名包含时间戳前缀，提取原始文件名
      const match = fileName.match(/^\d+_[a-z0-9]+\.(.+)$/)
      if (match) {
        return `file.${match[1]}`
      }
      
      return fileName || 'unknown'
    } catch {
      return 'unknown'
    }
  }

  // 获取文件类型
  const getFileType = (url: string): 'image' | 'document' => {
    const fileName = getFileNameFromUrl(url)
    const extension = fileName.split('.').pop()?.toLowerCase()
    
    const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg']
    
    if (imageExtensions.includes(extension || '')) {
      return 'image'
    }
    
    return 'document'
  }

  // 格式化文件大小（模拟，因为无法从URL获取真实大小）
  const formatFileSize = (url: string): string => {
    // 这里可以根据实际情况调整，或者从API获取文件大小
    return 'Unknown size'
  }

  // 下载文件
  const downloadFile = (url: string, fileName: string) => {
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className={`space-y-2 ${className}`}>
      {fileUrls.map((url, index) => {
        const fileName = getFileNameFromUrl(url)
        const fileType = getFileType(url)
        const fileSize = formatFileSize(url)
        
        return (
          <div key={index} className="border rounded-lg overflow-hidden bg-white shadow-sm max-w-xs">
            {fileType === 'image' ? (
              // 图片文件显示
              <div className="relative group">
                <img
                  src={url}
                  alt={fileName}
                  className="w-full max-w-xs h-auto max-h-48 object-cover rounded-lg cursor-pointer hover:opacity-90 transition-opacity"
                  onClick={() => window.open(url, '_blank')}
                  onError={(e) => {
                    // 图片加载失败时的处理
                    const target = e.target as HTMLImageElement
                    target.style.display = 'none'
                    const parent = target.parentElement
                    if (parent) {
                      parent.innerHTML = `
                        <div class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
                          <div class="flex-shrink-0">
                            <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                            </svg>
                          </div>
                          <div class="flex-1 min-w-0">
                            <p class="text-sm font-medium text-gray-900 truncate">${fileName}</p>
                            <p class="text-xs text-gray-500">图片加载失败</p>
                          </div>
                        </div>
                      `
                    }
                  }}
                />
                
                {/* 悬停时显示的下载按钮 */}
                <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation()
                      window.open(url, '_blank')
                    }}
                    className="bg-black/50 text-white hover:bg-black/70"
                  >
                    <Download className="h-4 w-4" />
                  </Button>
                </div>
                
                {/* 文件信息覆盖层 */}
                <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/70 to-transparent p-3">
                  <p className="text-white text-sm font-medium truncate">{fileName}</p>
                  <p className="text-white/80 text-xs">{fileSize}</p>
                </div>
              </div>
            ) : (
              // 文档文件显示
              <div 
                className="flex items-center gap-3 p-3 hover:bg-gray-50 transition-colors cursor-pointer"
                onClick={() => window.open(url, '_blank')}
              >
                <div className="flex-shrink-0">
                  <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                    <FileText className="h-5 w-5 text-blue-600" />
                  </div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">{fileName}</p>
                  <p className="text-xs text-gray-500">{fileSize}</p>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation()
                    window.open(url, '_blank')
                  }}
                >
                  <Download className="h-4 w-4" />
                </Button>
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
} 