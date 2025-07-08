"use client"

import React, { useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Bot } from 'lucide-react'
import FileUpload from '@/components/ui/file-upload'

export default function FileUploadDemo() {
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null)
  const [iconUrl, setIconUrl] = useState<string | null>(null)
  const [bannerUrl, setBannerUrl] = useState<string | null>(null)

  return (
    <div className="container py-8">
      <h1 className="text-3xl font-bold mb-8">文件上传组件演示</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* 头像上传 */}
        <Card>
          <CardHeader>
            <CardTitle>头像上传</CardTitle>
          </CardHeader>
          <CardContent>
            <FileUpload
              variant="avatar"
              size="lg"
              value={avatarUrl}
              onChange={setAvatarUrl}
              placeholder={<Bot className="h-6 w-6 text-blue-600" />}
              uploadText="上传头像"
              changeText="更换头像"
              removeText="移除头像"
              maxSize={2 * 1024 * 1024}
            />
            <p className="text-sm text-muted-foreground mt-4">
              当前头像URL: {avatarUrl || '未上传'}
            </p>
          </CardContent>
        </Card>

        {/* 方形图标上传 */}
        <Card>
          <CardHeader>
            <CardTitle>工具图标上传</CardTitle>
          </CardHeader>
          <CardContent>
            <FileUpload
              variant="square"
              size="xl"
              value={iconUrl}
              onChange={setIconUrl}
              uploadText="上传图标"
              changeText="更换图标"
              removeText="清除图标"
              maxSize={2 * 1024 * 1024}
            />
            <p className="text-sm text-muted-foreground mt-4">
              当前图标URL: {iconUrl || '未上传'}
            </p>
          </CardContent>
        </Card>

        {/* 横幅上传 */}
        <Card>
          <CardHeader>
            <CardTitle>横幅上传</CardTitle>
          </CardHeader>
          <CardContent>
            <FileUpload
              variant="rectangle"
              size="lg"
              value={bannerUrl}
              onChange={setBannerUrl}
              uploadText="上传横幅"
              changeText="更换横幅"
              removeText="清除横幅"
              maxSize={5 * 1024 * 1024} // 5MB
            />
            <p className="text-sm text-muted-foreground mt-4">
              当前横幅URL: {bannerUrl || '未上传'}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* 尺寸对比 */}
      <Card className="mt-8">
        <CardHeader>
          <CardTitle>不同尺寸对比</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-6">
            <div>
              <h3 className="text-lg font-medium mb-4">头像变体 - 不同尺寸</h3>
              <div className="flex items-center gap-4">
                {(['sm', 'md', 'lg', 'xl'] as const).map(size => (
                  <div key={size} className="text-center">
                    <FileUpload
                      variant="avatar"
                      size={size}
                      value={avatarUrl}
                      onChange={setAvatarUrl}
                      placeholder={<Bot className="h-4 w-4 text-blue-600" />}
                    />
                    <p className="text-xs text-muted-foreground mt-2">{size}</p>
                  </div>
                ))}
              </div>
            </div>

            <div>
              <h3 className="text-lg font-medium mb-4">方形变体 - 不同尺寸</h3>
              <div className="flex items-center gap-4">
                {(['sm', 'md', 'lg', 'xl'] as const).map(size => (
                  <div key={size} className="text-center">
                    <FileUpload
                      variant="square"
                      size={size}
                      value={iconUrl}
                      onChange={setIconUrl}
                    />
                    <p className="text-xs text-muted-foreground mt-2">{size}</p>
                  </div>
                ))}
              </div>
            </div>

            <div>
              <h3 className="text-lg font-medium mb-4">矩形变体 - 不同尺寸</h3>
              <div className="flex items-center gap-4">
                {(['sm', 'md', 'lg', 'xl'] as const).map(size => (
                  <div key={size} className="text-center">
                    <FileUpload
                      variant="rectangle"
                      size={size}
                      value={bannerUrl}
                      onChange={setBannerUrl}
                    />
                    <p className="text-xs text-muted-foreground mt-2">{size}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
} 