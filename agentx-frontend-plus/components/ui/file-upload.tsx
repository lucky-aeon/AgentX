"use client"

import React, { useState, useRef, useCallback } from 'react'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Upload, Loader2, X, Trash, Image as ImageIcon } from 'lucide-react'
import { toast } from '@/hooks/use-toast'
import { uploadSingleFile, type UploadResult } from '@/lib/file-upload-service'

export interface FileUploadProps {
  // 基础配置
  value?: string | null  // 当前文件URL
  onChange?: (url: string | null) => void  // URL变化回调
  
  // 外观配置
  variant?: 'avatar' | 'square' | 'rectangle'  // 显示样式
  size?: 'sm' | 'md' | 'lg' | 'xl'  // 尺寸
  placeholder?: React.ReactNode  // 占位符内容
  
  // 上传配置
  accept?: string  // 允许的文件类型
  maxSize?: number  // 最大文件大小（字节）
  disabled?: boolean  // 是否禁用
  
  // 按钮文本
  uploadText?: string
  removeText?: string
  changeText?: string
  
  // 样式
  className?: string
  containerClassName?: string
  
  // 事件回调
  onUploadStart?: () => void
  onUploadComplete?: (result: UploadResult) => void
  onUploadError?: (error: Error) => void
  onRemove?: () => void
}

const VARIANT_STYLES = {
  avatar: {
    container: 'rounded-full overflow-hidden',
    sizes: {
      sm: 'h-8 w-8',
      md: 'h-12 w-12', 
      lg: 'h-16 w-16',
      xl: 'h-20 w-20'
    }
  },
  square: {
    container: 'rounded-md overflow-hidden',
    sizes: {
      sm: 'h-12 w-12',
      md: 'h-16 w-16',
      lg: 'h-20 w-20', 
      xl: 'h-24 w-24'
    }
  },
  rectangle: {
    container: 'rounded-md overflow-hidden',
    sizes: {
      sm: 'h-12 w-20',
      md: 'h-16 w-24',
      lg: 'h-20 w-32',
      xl: 'h-24 w-40'
    }
  }
}

export default function FileUpload({
  value,
  onChange,
  variant = 'square',
  size = 'md',
  placeholder,
  accept = 'image/*',
  maxSize = 2 * 1024 * 1024, // 2MB
  disabled = false,
  uploadText = '上传',
  removeText = '移除',
  changeText = '更换',
  className = '',
  containerClassName = '',
  onUploadStart,
  onUploadComplete,
  onUploadError,
  onRemove
}: FileUploadProps) {
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const fileInputRef = useRef<HTMLInputElement>(null)

  // 获取样式配置
  const variantConfig = VARIANT_STYLES[variant]
  const sizeClass = variantConfig.sizes[size]
  const containerClass = variantConfig.container

  // 默认占位符
  const defaultPlaceholder = variant === 'avatar' ? (
    <Upload className="h-4 w-4 text-muted-foreground" />
  ) : (
    <div className="flex flex-col items-center justify-center text-muted-foreground">
      <ImageIcon className="h-6 w-6 mb-1" />
      <span className="text-xs">上传图片</span>
    </div>
  )

  // 触发文件选择
  const triggerFileSelect = useCallback(() => {
    if (disabled || isUploading) return
    fileInputRef.current?.click()
  }, [disabled, isUploading])

  // 处理文件上传
  const handleFileUpload = useCallback(async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    // 重置input value，以便同一文件能再次上传
    event.target.value = ''

    // 验证文件类型
    if (accept && !file.type.match(accept.replace('*', '.*'))) {
      const errorMsg = '不支持的文件类型'
      toast({
        variant: 'destructive',
        title: '文件类型错误',
        description: errorMsg,
      })
      onUploadError?.(new Error(errorMsg))
      return
    }

    // 验证文件大小
    if (file.size > maxSize) {
      const errorMsg = `文件大小不能超过 ${(maxSize / 1024 / 1024).toFixed(1)}MB`
      toast({
        variant: 'destructive',
        title: '文件过大',
        description: errorMsg,
      })
      onUploadError?.(new Error(errorMsg))
      return
    }

    try {
      setIsUploading(true)
      setUploadProgress(0)
      onUploadStart?.()

      // 上传文件到OSS
      const result = await uploadSingleFile(file, (progress) => {
        setUploadProgress(progress)
      })

      // 上传成功
      onChange?.(result.url)
      onUploadComplete?.(result)
      
      toast({
        title: '上传成功',
        description: '文件已成功上传',
      })
    } catch (error) {
      console.error('文件上传失败:', error)
      const errorMsg = error instanceof Error ? error.message : '上传失败'
      
      toast({
        variant: 'destructive',
        title: '上传失败',
        description: errorMsg,
      })
      
      onUploadError?.(error instanceof Error ? error : new Error(errorMsg))
    } finally {
      setIsUploading(false)
      setUploadProgress(0)
    }
  }, [accept, maxSize, onChange, onUploadStart, onUploadComplete, onUploadError])

  // 处理移除文件
  const handleRemove = useCallback(() => {
    if (disabled || isUploading) return
    
    onChange?.(null)
    onRemove?.()
    
    // 清空文件input
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }, [disabled, isUploading, onChange, onRemove])

  // 检查是否有有效的图片URL
  const hasValidImage = value && typeof value === 'string' && value.trim() !== ''

  return (
    <div className={`flex items-center gap-3 ${containerClassName}`}>
      {/* 预览区域 */}
      <div 
        className={`${containerClass} ${sizeClass} border flex items-center justify-center bg-muted/20 relative ${className}`}
      >
        {hasValidImage ? (
          <img 
            src={value} 
            alt="上传的文件" 
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="flex items-center justify-center h-full w-full">
            {placeholder || defaultPlaceholder}
          </div>
        )}
        
        {/* 上传进度 */}
        {isUploading && (
          <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
            <div className="text-white text-center">
              <Loader2 className="h-4 w-4 animate-spin mx-auto mb-1" />
              <div className="text-xs">{uploadProgress}%</div>
            </div>
          </div>
        )}
      </div>

      {/* 操作按钮 */}
      <div className="flex flex-col gap-2">
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={triggerFileSelect}
          disabled={disabled || isUploading}
          className="text-xs"
        >
          {isUploading ? (
            <>
              <Loader2 className="h-3 w-3 mr-1 animate-spin" />
              上传中...
            </>
          ) : (
            <>
              <Upload className="h-3 w-3 mr-1" />
              {hasValidImage ? changeText : uploadText}
            </>
          )}
        </Button>
        
        {hasValidImage && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleRemove}
            disabled={disabled || isUploading}
            className="text-xs hover:bg-red-50 hover:text-red-600 hover:border-red-200"
          >
            <Trash className="h-3 w-3 mr-1" />
            {removeText}
          </Button>
        )}
      </div>

      {/* 隐藏的文件输入 */}
      <input
        ref={fileInputRef}
        type="file"
        accept={accept}
        onChange={handleFileUpload}
        className="hidden"
      />
    </div>
  )
} 