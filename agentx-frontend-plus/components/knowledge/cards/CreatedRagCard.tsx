"use client"

import Link from "next/link"
import { Book, Edit, MoreHorizontal, Trash, Share, History, Eye } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Badge } from "@/components/ui/badge"

import type { RagDataset } from "@/types/rag-dataset"

interface CreatedRagCardProps {
  dataset: RagDataset
  onEdit?: (dataset: RagDataset) => void
  onDelete?: (dataset: RagDataset) => void
  onPublish?: (dataset: RagDataset) => void
  onViewHistory?: (dataset: RagDataset) => void
}

export function CreatedRagCard({ 
  dataset, 
  onEdit, 
  onDelete, 
  onPublish, 
  onViewHistory 
}: CreatedRagCardProps) {
  // 格式化时间
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN')
  }

  return (
    <Card className="relative overflow-hidden hover:shadow-md transition-all duration-300 border border-gray-100 min-h-[180px]">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <Link href={`/knowledge/${dataset.id}`} className="flex items-center gap-3 flex-1 min-w-0" style={{ cursor: 'pointer' }}>
            <div className="flex h-12 w-12 items-center justify-center rounded-md bg-primary/10 text-primary overflow-hidden">
              {dataset.icon ? (
                <img
                  src={dataset.icon}
                  alt={dataset.name}
                  className="h-full w-full object-cover"
                />
              ) : (
                <Book className="h-6 w-6" />
              )}
            </div>
            <div className="w-[calc(100%-60px)] min-w-0">
              <h3 className="font-semibold line-clamp-1 truncate text-ellipsis overflow-hidden whitespace-nowrap max-w-full">{dataset.name}</h3>
              <p className="text-sm text-muted-foreground mt-1">
                更新于 {formatDate(dataset.updatedAt)}
              </p>
            </div>
          </Link>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-8 w-8">
                <MoreHorizontal className="h-4 w-4" />
                <span className="sr-only">更多选项</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              {onEdit && (
                <DropdownMenuItem onClick={() => onEdit(dataset)}>
                  <Edit className="mr-2 h-4 w-4" />
                  编辑
                </DropdownMenuItem>
              )}
              {onPublish && (
                <DropdownMenuItem onClick={() => onPublish(dataset)}>
                  <Share className="mr-2 h-4 w-4" />
                  发布到市场
                </DropdownMenuItem>
              )}
              {onViewHistory && (
                <DropdownMenuItem onClick={() => onViewHistory(dataset)}>
                  <History className="mr-2 h-4 w-4" />
                  版本历史
                </DropdownMenuItem>
              )}
              {onDelete && (
                <>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem 
                    className="text-red-600" 
                    onClick={() => onDelete(dataset)}
                  >
                    <Trash className="mr-2 h-4 w-4" />
                    删除
                  </DropdownMenuItem>
                </>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </CardHeader>
      <CardContent className="pt-0">
        <Link href={`/knowledge/${dataset.id}`} className="block" style={{ cursor: 'pointer' }}>
          <div className="min-h-[40px] mb-3 line-clamp-2 text-sm">
            {dataset.description || "无描述"}
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="secondary">
              {dataset.fileCount} 个文件
            </Badge>
            <Badge variant="outline">
              原始数据集
            </Badge>
          </div>
        </Link>
      </CardContent>
    </Card>
  )
}