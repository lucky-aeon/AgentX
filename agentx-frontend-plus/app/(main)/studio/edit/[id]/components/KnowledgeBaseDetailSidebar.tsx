import React, { useEffect, useState } from 'react';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from '@/components/ui/sheet';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { X, Database, FileText, Calendar, ChevronDown, ChevronRight, File, RefreshCw } from 'lucide-react';
import { getKnowledgeBaseDetail, getAllKnowledgeBaseFilesWithToast } from '@/lib/agent-knowledge-base-service';
import { getInstalledRagFilesWithToast, getInstalledRagVersionsWithToast, switchRagVersionWithToast } from '@/lib/rag-publish-service';
import type { KnowledgeBase, FileDetail } from '@/lib/agent-knowledge-base-service';
import type { UserRagDTO } from '@/types/rag-publish';

// 缓存已请求过的知识库详情
const knowledgeBaseDetailsCache = new Map<string, any>();

interface KnowledgeBaseDetailSidebarProps {
  knowledgeBase: KnowledgeBase | null;
  isOpen: boolean;
  onClose: () => void;
  onVersionSwitch?: (newKnowledgeBase: KnowledgeBase) => void;
}

const KnowledgeBaseDetailSidebar: React.FC<KnowledgeBaseDetailSidebarProps> = ({ 
  knowledgeBase: initialKnowledgeBase, 
  isOpen, 
  onClose,
  onVersionSwitch,
}) => {
  const [detailedKnowledgeBase, setDetailedKnowledgeBase] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [files, setFiles] = useState<FileDetail[]>([]);
  const [isLoadingFiles, setIsLoadingFiles] = useState(false);
  const [showFileList, setShowFileList] = useState(true);

  // 版本相关状态
  const [versions, setVersions] = useState<UserRagDTO[]>([]);
  const [isLoadingVersions, setIsLoadingVersions] = useState(false);
  const [isSwitchingVersion, setIsSwitchingVersion] = useState(false);

  // 获取版本列表
  const fetchVersionList = async () => {
    const knowledgeBaseToUse = detailedKnowledgeBase || initialKnowledgeBase;
    if (!knowledgeBaseToUse?.userRagId) return;

    setIsLoadingVersions(true);
    try {
 
      const response = await getInstalledRagVersionsWithToast(knowledgeBaseToUse.userRagId);
      if (response.code === 200) {
        setVersions(response.data || []);
      } else {
 
        setVersions([]);
      }
    } catch (error) {
 
      setVersions([]);
    } finally {
      setIsLoadingVersions(false);
    }
  };

  // 版本切换
  const handleVersionSwitch = async (targetVersionId: string) => {
    const knowledgeBaseToUse = detailedKnowledgeBase || initialKnowledgeBase;
    if (!knowledgeBaseToUse?.userRagId || isSwitchingVersion) return;

    setIsSwitchingVersion(true);
    try {
 
      const response = await switchRagVersionWithToast(knowledgeBaseToUse.userRagId, targetVersionId);
      if (response.code === 200) {
        // 更新详细信息
        const updatedKnowledgeBase = {
          ...knowledgeBaseToUse,
          version: response.data.version,
          ragVersionId: response.data.ragVersionId,
          fileCount: response.data.fileCount || knowledgeBaseToUse.fileCount,
          // 其他可能需要更新的字段
        };
        
        setDetailedKnowledgeBase(updatedKnowledgeBase);
        
        // 清除缓存，确保下次获取最新数据
        knowledgeBaseDetailsCache.delete(knowledgeBaseToUse.id);
        
        // 刷新版本列表和文件列表
        await Promise.all([
          fetchVersionList(),
          fetchFileList()
        ]);

        // 通知父组件更新
        if (onVersionSwitch) {
          onVersionSwitch(updatedKnowledgeBase);
        }
      }
    } catch (error) {
 
    } finally {
      setIsSwitchingVersion(false);
    }
  };

  // 获取文件列表
  const fetchFileList = async () => {
    if (!initialKnowledgeBase) return;
    
    setIsLoadingFiles(true);
    try {
      // 优先使用已安装RAG的文件接口，使用详细信息中的userRagId
      const knowledgeBaseToUse = detailedKnowledgeBase || initialKnowledgeBase;
      
      if (knowledgeBaseToUse.userRagId) {
 
        const response = await getInstalledRagFilesWithToast(knowledgeBaseToUse.userRagId);
        if (response.code === 200) {
          setFiles(response.data || []);
        } else {
 
          setFiles([]);
        }
      } else {
 
        // 向后兼容：如果没有userRagId，使用原始方法
        const response = await getAllKnowledgeBaseFilesWithToast(knowledgeBaseToUse.id);
        if (response.code === 200) {
          setFiles(response.data);
        } else {
 
          setFiles([]);
        }
      }
    } catch (error) {
 
      setFiles([]);
    } finally {
      setIsLoadingFiles(false);
    }
  };

  useEffect(() => {
    if (isOpen && initialKnowledgeBase) {
      const cacheKey = initialKnowledgeBase.id;
      
      // 如果缓存中有这个知识库的详情且包含userRagId，直接使用
      const cachedData = knowledgeBaseDetailsCache.get(cacheKey);
      if (cachedData && cachedData.userRagId) {
        setDetailedKnowledgeBase(cachedData);
        // 缓存命中时也要获取文件列表和版本列表
        setTimeout(() => {
          Promise.all([
            fetchVersionList(),
            cachedData.fileCount > 0 ? fetchFileList() : Promise.resolve()
          ]);
        }, 100);
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
              
              // 获取详细信息后立即获取版本列表和文件列表
              setTimeout(() => {
                Promise.all([
                  fetchVersionList(),
                  detailData.fileCount > 0 ? fetchFileList() : Promise.resolve()
                ]);
              }, 100); // 小延迟确保状态更新完成
            } else {
 
            }
          } catch (error) {
 
          } finally {
            setIsLoading(false);
          }
        };

        fetchDetails();
      }
      
      // 重置文件列表状态
      setFiles([]);
      setShowFileList(true);
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
      // 清理版本相关状态
      setVersions([]);
      setIsLoadingVersions(false);
      setIsSwitchingVersion(false);
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

            {/* 版本选择器 - 只有当userRagId存在时才显示 */}
            {displayKnowledgeBase.userRagId && (
              <div className="space-y-2">
                <p className="text-xs text-muted-foreground">当前版本</p>
                <div className="flex items-center space-x-2">
                  {isLoadingVersions ? (
                    <div className="flex items-center space-x-2">
                      <RefreshCw className="w-4 h-4 animate-spin text-gray-500" />
                      <span className="text-sm text-muted-foreground">加载版本...</span>
                    </div>
                  ) : versions.length > 1 ? (
                    <Select
                      value={(() => {
                        // 优先使用当前知识库的ragVersionId
                        if (displayKnowledgeBase.ragVersionId) {
                          return displayKnowledgeBase.ragVersionId;
                        }
                        // 如果没有，查找版本列表中已安装的版本（id不为null的版本）
                        const installedVersion = versions.find(v => v.id !== null);
                        return installedVersion?.ragVersionId || versions[0]?.ragVersionId || '';
                      })()}
                      onValueChange={handleVersionSwitch}
                      disabled={isSwitchingVersion}
                    >
                      <SelectTrigger className="w-full h-8 text-sm">
                        <SelectValue placeholder="选择版本" />
                      </SelectTrigger>
                      <SelectContent>
                        {versions.map((version) => (
                          <SelectItem 
                            key={version.ragVersionId || version.id} 
                            value={version.ragVersionId || ''}
                            className="text-sm"
                          >
                            <div className="flex items-center justify-between w-full">
                              <span>v{version.version}</span>
                              {version.id !== null && (
                                <Badge variant="secondary" className="ml-2 text-xs">当前</Badge>
                              )}
                            </div>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  ) : (
                    <div className="flex items-center space-x-2">
                      <Badge variant="outline" className="text-xs">
                        v{displayKnowledgeBase.version || '1.0'}
                      </Badge>
                      <span className="text-xs text-muted-foreground">
                        {versions.length === 1 ? '仅一个版本' : '无其他版本'}
                      </span>
                    </div>
                  )}
                  {isSwitchingVersion && (
                    <RefreshCw className="w-4 h-4 animate-spin text-blue-500" />
                  )}
                </div>
              </div>
            )}
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