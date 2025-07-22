import React, { useEffect, useState } from 'react';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from '@/components/ui/sheet';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { X, Database, FileText, Calendar, ChevronDown, ChevronRight, File } from 'lucide-react';
import { getKnowledgeBaseDetail, getAllKnowledgeBaseFilesWithToast } from '@/lib/agent-knowledge-base-service';
import type { KnowledgeBase, FileDetail } from '@/lib/agent-knowledge-base-service';

// 缓存已请求过的知识库详情
const knowledgeBaseDetailsCache = new Map<string, any>();

interface KnowledgeBaseDetailSidebarProps {
  knowledgeBase: KnowledgeBase | null;
  isOpen: boolean;
  onClose: () => void;
}

const KnowledgeBaseDetailSidebar: React.FC<KnowledgeBaseDetailSidebarProps> = ({ 
  knowledgeBase: initialKnowledgeBase, 
  isOpen, 
  onClose,
}) => {
  const [detailedKnowledgeBase, setDetailedKnowledgeBase] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [files, setFiles] = useState<FileDetail[]>([]);
  const [isLoadingFiles, setIsLoadingFiles] = useState(false);
  const [showFileList, setShowFileList] = useState(true);

  // 获取文件列表
  const fetchFileList = async () => {
    if (!initialKnowledgeBase) return;
    
    setIsLoadingFiles(true);
    try {
      const response = await getAllKnowledgeBaseFilesWithToast(initialKnowledgeBase.id);
      if (response.code === 200) {
        setFiles(response.data);
      } else {
        console.error('获取文件列表失败:', response.message);
        setFiles([]);
      }
    } catch (error) {
      console.error('获取文件列表错误:', error);
      setFiles([]);
    } finally {
      setIsLoadingFiles(false);
    }
  };

  useEffect(() => {
    if (isOpen && initialKnowledgeBase) {
      const cacheKey = initialKnowledgeBase.id;
      
      // 如果缓存中有这个知识库的详情，直接使用
      if (knowledgeBaseDetailsCache.has(cacheKey)) {
        setDetailedKnowledgeBase(knowledgeBaseDetailsCache.get(cacheKey));
      } else {
        const fetchDetails = async () => {
          setIsLoading(true);
          try {
            const response = await getKnowledgeBaseDetail(initialKnowledgeBase.id);
            if (response.code === 200) {
              const detailData = response.data;
              setDetailedKnowledgeBase(detailData);
              // 缓存详情数据
              knowledgeBaseDetailsCache.set(cacheKey, detailData);
            } else {
              console.error('获取知识库详情失败:', response.message);
            }
          } catch (error) {
            console.error('获取知识库详情错误:', error);
          } finally {
            setIsLoading(false);
          }
        };

        fetchDetails();
      }
      
      // 重置文件列表状态并自动加载
      setFiles([]);
      setShowFileList(true);
      
      // 自动获取文件列表
      if (initialKnowledgeBase.fileCount > 0) {
        fetchFileList();
      }
    }
  }, [isOpen, initialKnowledgeBase]);

  const handleClose = () => {
    onClose();
    // 延迟清理状态，避免关闭动画期间看到空白
    setTimeout(() => {
      setDetailedKnowledgeBase(null);
      setIsLoading(false);
      setFiles([]);
      setIsLoadingFiles(false);
      setShowFileList(true);
    }, 200);
  };

  if (!initialKnowledgeBase) return null;

  const displayKnowledgeBase = detailedKnowledgeBase || initialKnowledgeBase;

  return (
    <Sheet open={isOpen} onOpenChange={handleClose}>
      <SheetContent side="right" className="w-[400px] sm:w-[500px] overflow-y-auto">
        <SheetHeader className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Database className="w-5 h-5 text-blue-600" />
              <SheetTitle className="text-lg">{displayKnowledgeBase.name}</SheetTitle>
            </div>
          </div>
          
          <SheetDescription className="text-left">
            {displayKnowledgeBase.description || "暂无描述"}
          </SheetDescription>
        </SheetHeader>

        <Separator className="my-4" />

        <div className="space-y-6">
          {/* 基本信息 */}
          <div className="space-y-3">
            <h3 className="text-sm font-medium text-muted-foreground flex items-center">
              <Database className="w-4 h-4 mr-1" />
              基本信息
            </h3>
            
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <p className="text-xs text-muted-foreground">文件数量</p>
                <div className="flex items-center space-x-2">
                  <FileText className="w-4 h-4 text-gray-500" />
                  <span className="text-sm font-medium">{displayKnowledgeBase.fileCount} 个文件</span>
                </div>
              </div>
              
              <div className="space-y-1">
                <p className="text-xs text-muted-foreground">创建时间</p>
                <div className="flex items-center space-x-2">
                  <Calendar className="w-4 h-4 text-gray-500" />
                  <span className="text-sm">
                    {new Date(displayKnowledgeBase.createdAt).toLocaleDateString('zh-CN')}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <Separator />

          {/* 文件列表 */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-medium text-muted-foreground flex items-center">
                <FileText className="w-4 h-4 mr-1" />
                文件列表
              </h3>
              <Badge variant="outline">{displayKnowledgeBase.fileCount} 个</Badge>
            </div>
            
            {displayKnowledgeBase.fileCount > 0 && (
              <>
                <Button
                  variant="ghost"
                  size="sm"
                  className="w-full justify-start"
                  onClick={() => {
                    setShowFileList(!showFileList);
                  }}
                >
                  {showFileList ? (
                    <ChevronDown className="w-4 h-4 mr-2" />
                  ) : (
                    <ChevronRight className="w-4 h-4 mr-2" />
                  )}
                  {showFileList ? '收起文件列表' : '展开文件列表'}
                </Button>
                
                {showFileList && (
                  <div className="space-y-2">
                    {isLoadingFiles ? (
                      <div className="space-y-2">
                        {[1, 2, 3].map((i) => (
                          <Skeleton key={i} className="h-10 w-full" />
                        ))}
                      </div>
                    ) : files.length > 0 ? (
                      <div className="max-h-64 overflow-y-auto space-y-1">
                        {files.map((file) => (
                          <div
                            key={file.id}
                            className="flex items-center space-x-2 p-2 rounded border bg-white hover:bg-gray-50"
                          >
                            <File className="w-4 h-4 text-gray-500 flex-shrink-0" />
                            <div className="flex-1 min-w-0">
                              <p className="text-sm font-medium truncate">
                                {file.originalFilename || file.filename}
                              </p>
                              <div className="flex items-center space-x-2 text-xs text-muted-foreground">
                                <span>{file.ext?.toUpperCase()}</span>
                                <span>•</span>
                                <span>{(file.size / 1024).toFixed(1)} KB</span>
                                {file.processProgress < 100 && (
                                  <>
                                    <span>•</span>
                                    <span className="text-blue-600">
                                      处理中 {file.processProgress}%
                                    </span>
                                  </>
                                )}
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-4 text-muted-foreground text-sm">
                        暂无文件
                      </div>
                    )}
                  </div>
                )}
              </>
            )}
            
            {displayKnowledgeBase.fileCount === 0 && (
              <div className="text-center py-8 text-muted-foreground">
                <FileText className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p className="text-sm">该知识库暂无文件</p>
              </div>
            )}
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
};

export default KnowledgeBaseDetailSidebar;