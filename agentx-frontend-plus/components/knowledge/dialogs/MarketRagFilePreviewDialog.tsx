"use client"

import { useState, useEffect } from "react"
import ReactMarkdown from 'react-markdown'
import { 
  FileText, 
  Loader2,
  Search,
  FileSearch,
  CheckCircle,
  AlertCircle,
  X
} from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Badge } from "@/components/ui/badge"
import { Card } from "@/components/ui/card"

import type { FileDetail } from "@/types/rag-dataset"
import type { DocumentUnitDTO } from "@/types/rag-dataset"

interface MarketRagFilePreviewDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  file: FileDetail
  ragVersionId: string
}

export function MarketRagFilePreviewDialog({ 
  open, 
  onOpenChange, 
  file, 
  ragVersionId 
}: MarketRagFilePreviewDialogProps) {
  const [documentUnits, setDocumentUnits] = useState<DocumentUnitDTO[]>([])
  const [loading, setLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")

  // 过滤后的文档单元
  const filteredDocuments = documentUnits.filter(unit => 
    searchQuery === "" || 
    unit.content.toLowerCase().includes(searchQuery.toLowerCase())
  )

  // 重置状态
  const resetState = () => {
    setDocumentUnits([])
    setSearchQuery("")
    setLoading(false)
  }

  // 加载文档单元
  useEffect(() => {
    if (open && file && ragVersionId) {
      loadDocumentUnits()
    } else if (!open) {
      resetState()
    }
  }, [open, file, ragVersionId])

  // 市场RAG文档单元已不支持预览
  const loadDocumentUnits = async () => {
    setLoading(false)
    setDocumentUnits([])
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-5xl max-h-[85vh] p-0 flex flex-col">
        <DialogHeader className="px-6 py-4 border-b shrink-0">
          <div className="flex items-center gap-3">
            <FileText className="h-5 w-5" />
            <div>
              <DialogTitle>{file.originalFilename}</DialogTitle>
              <DialogDescription>
                查看文档中的语料内容 • 只读模式
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="px-6 py-3 border-b shrink-0">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Badge variant="outline" className="text-xs">
                共 {filteredDocuments.length} / {documentUnits.length} 个语料
              </Badge>
              <Badge variant="outline" className="text-xs">
                {file.filePageSize || 0} 页
              </Badge>
            </div>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                type="search"
                placeholder="搜索语料内容..."
                className="pl-10 pr-10 w-64"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              {searchQuery && (
                <Button
                  variant="ghost"
                  size="icon"
                  className="absolute right-1 top-1/2 -translate-y-1/2 h-7 w-7"
                  onClick={() => setSearchQuery("")}
                >
                  <X className="h-4 w-4" />
                </Button>
              )}
            </div>
          </div>
        </div>

        <ScrollArea className="flex-1 px-6 py-4 overflow-y-auto" style={{ maxHeight: 'calc(85vh - 160px)' }}>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin" />
              <span className="ml-2">加载中...</span>
            </div>
          ) : filteredDocuments.length === 0 ? (
            <div className="text-center py-8">
              <FileSearch className="h-12 w-12 mx-auto text-gray-400 mb-4" />
              <h3 className="text-lg font-medium mb-2">
                暂不支持市场RAG文档预览
              </h3>
              <p className="text-muted-foreground">
                请安装该RAG后查看详细内容
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {filteredDocuments.map((unit) => (
                <Card key={unit.id} className="p-4">
                  <div className="space-y-3">
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2">
                        <Badge variant="outline" className="text-xs">
                          第 {unit.page + 1} 页
                        </Badge>
                        {unit.isVector && (
                          <Badge variant="default" className="text-xs">
                            <CheckCircle className="h-3 w-3 mr-1" />
                            已向量化
                          </Badge>
                        )}
                        {unit.isOcr && (
                          <Badge variant="secondary" className="text-xs">
                            OCR处理
                          </Badge>
                        )}
                      </div>
                      <Badge variant="outline" className="text-xs">
                        只读
                      </Badge>
                    </div>

                    <div className="text-sm">
                      <div className="leading-tight whitespace-pre-wrap">
                        <ReactMarkdown 
                          components={{
                            h1: ({ children }) => <h1 className="text-xl font-bold mb-1 leading-tight">{children}</h1>,
                            h2: ({ children }) => <h2 className="text-lg font-semibold mb-1 leading-tight">{children}</h2>,
                            h3: ({ children }) => <h3 className="text-md font-medium mb-0.5 leading-tight">{children}</h3>,
                            p: ({ children }) => <p className="mb-0.5 leading-tight">{children}</p>,
                            ul: ({ children }) => <ul className="list-disc list-inside mb-0.5 space-y-0">{children}</ul>,
                            ol: ({ children }) => <ol className="list-decimal list-inside mb-0.5 space-y-0">{children}</ol>,
                            li: ({ children }) => <li className="leading-tight">{children}</li>,
                            strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
                            em: ({ children }) => <em className="italic">{children}</em>,
                            code: ({ children }) => <code className="bg-gray-100 px-1 py-0.5 rounded text-sm font-mono">{children}</code>,
                            pre: ({ children }) => <pre className="bg-gray-100 p-2 rounded text-sm font-mono overflow-x-auto mb-1">{children}</pre>,
                            blockquote: ({ children }) => <blockquote className="border-l-4 border-gray-300 pl-4 italic mb-0.5 leading-tight">{children}</blockquote>,
                            a: ({ children, href }) => <a href={href} className="text-blue-600 hover:underline" target="_blank" rel="noopener noreferrer">{children}</a>,
                            hr: () => <hr className="my-4 border-gray-200" />,
                            table: ({ children }) => <table className="border-collapse border border-gray-300 w-full mb-1">{children}</table>,
                            th: ({ children }) => <th className="border border-gray-300 px-2 py-1 bg-gray-50 font-semibold">{children}</th>,
                            td: ({ children }) => <td className="border border-gray-300 px-2 py-1">{children}</td>,
                          }}
                        >
                          {unit.content}
                        </ReactMarkdown>
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-2 border-t">
                      <span className="text-xs text-muted-foreground">
                        ID: {unit.id}
                      </span>
                      <span className="text-xs text-muted-foreground">
                        更新时间: {new Date(unit.updatedAt).toLocaleString('zh-CN')}
                      </span>
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </ScrollArea>
      </DialogContent>
    </Dialog>
  )
}