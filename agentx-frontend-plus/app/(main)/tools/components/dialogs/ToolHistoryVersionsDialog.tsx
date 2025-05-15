import { useState, useEffect } from "react";
import { UserTool, VersionData } from "../../utils/types";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Calendar, Download, FileText, Command, History, Wrench, Info, List, Check, Eye, EyeOff } from "lucide-react";
import { cn } from "@/lib/utils";
import { formatDate } from "@/lib/utils";
import { getMarketToolVersions, updateToolVersionStatusWithToast } from "@/lib/tool-service";
import ReactMarkdown from "react-markdown";
import { Toggle } from "@/components/ui/toggle";
import { toast } from "@/hooks/use-toast";

interface ToolHistoryVersionsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tool: UserTool | null;
}

export function ToolHistoryVersionsDialog({
  open,
  onOpenChange,
  tool
}: ToolHistoryVersionsDialogProps) {
  const [versions, setVersions] = useState<VersionData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedVersion, setSelectedVersion] = useState<VersionData | null>(null);
  const [activeTab, setActiveTab] = useState("overview");
  const [updatingStatus, setUpdatingStatus] = useState<string | null>(null); // 记录正在更新状态的版本ID
  
  // 获取工具版本列表
  const fetchVersions = async () => {
    if (!tool || !tool.toolId) return;
    
    try {
      setLoading(true);
      setError(null);
      
      const response = await getMarketToolVersions(tool.toolId);
      
      if (response.code === 200) {
        // 按版本号排序，最新版本在前
        const sortedVersions = [...response.data].sort((a, b) => {
          const versionA = a.version.split('.').map(Number);
          const versionB = b.version.split('.').map(Number);
          
          for (let i = 0; i < Math.max(versionA.length, versionB.length); i++) {
            const numA = versionA[i] || 0;
            const numB = versionB[i] || 0;
            
            if (numA > numB) return -1;
            if (numA < numB) return 1;
          }
          
          return 0;
        });
        
        // 转换为 VersionData 类型
        const typedVersions: VersionData[] = sortedVersions.map(item => ({
          id: item.id,
          name: item.name,
          icon: item.icon,
          subtitle: item.subtitle,
          description: item.description,
          userId: item.userId || item.user_id || "",
          version: item.version,
          toolId: item.toolId || item.tool_id || "",
          uploadType: item.uploadType || item.upload_type || null,
          uploadUrl: item.uploadUrl || item.upload_url || null,
          toolList: item.toolList || item.tool_list || [],
          labels: item.labels || [],
          publicStatus: item.publicStatus || item.public_status || false,
          changeLog: item.changeLog || "",
          createdAt: item.createdAt,
          updatedAt: item.updatedAt,
          userName: item.userName || null,
          installCount: item.installCount || null,
          office: item.office || item.is_office || false
        }));
        
        setVersions(typedVersions);
        
        // 默认选择第一个版本（最新版本）
        if (typedVersions.length > 0) {
          setSelectedVersion(typedVersions[0]);
        }
      } else {
        setError("获取版本历史失败");
      }
    } catch (error) {
      setError("获取版本历史出错");
      console.error("获取工具版本历史失败", error);
    } finally {
      setLoading(false);
    }
  };
  
  // 当对话框打开时获取版本信息
  useEffect(() => {
    if (open && tool) {
      fetchVersions();
    }
  }, [open, tool]);
  
  // 选择版本
  const handleSelectVersion = (version: VersionData) => {
    setSelectedVersion(version);
    setActiveTab("overview");
  };
  
  // 切换版本发布状态
  const handleTogglePublicStatus = async (version: VersionData, newStatus: boolean) => {
    if (!tool || !tool.toolId) return;
    
    try {
      setUpdatingStatus(version.id);
      
      const response = await updateToolVersionStatusWithToast(
        tool.toolId,
        version.version,
        newStatus
      );
      
      if (response.code === 200) {
        // 更新本地版本列表状态
        setVersions(versions.map(v => {
          if (v.id === version.id) {
            return { ...v, publicStatus: newStatus };
          }
          return v;
        }));
        
        // 如果当前选中的是这个版本，也更新选中版本的状态
        if (selectedVersion && selectedVersion.id === version.id) {
          setSelectedVersion({ ...selectedVersion, publicStatus: newStatus });
        }
      }
    } catch (error) {
      console.error("切换版本状态失败", error);
    } finally {
      setUpdatingStatus(null);
    }
  };
  
  if (!tool) return null;
  
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[85vh] flex flex-col overflow-hidden">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold flex items-center">
            <History className="h-5 w-5 mr-2" />
            历史版本
          </DialogTitle>
        </DialogHeader>
        
        <div className="flex-1 flex gap-4 overflow-hidden">
          {/* 左侧版本列表 */}
          <div className="w-64 border-r pr-4">
            <h4 className="text-sm font-medium mb-2">已发布版本</h4>
            
            {loading ? (
              <div className="flex justify-center py-8">
                <div className="animate-spin h-6 w-6 border-2 border-primary border-t-transparent rounded-full"></div>
              </div>
            ) : error ? (
              <div className="py-4 text-center text-sm text-red-500">
                {error}
                <Button 
                  variant="outline" 
                  size="sm" 
                  className="mt-2 w-full"
                  onClick={fetchVersions}
                >
                  重试
                </Button>
              </div>
            ) : versions.length === 0 ? (
              <div className="py-8 text-center text-sm text-muted-foreground">
                <FileText className="h-8 w-8 mx-auto text-muted-foreground/60 mb-2" />
                <p>尚未发布任何版本</p>
              </div>
            ) : (
              <ScrollArea className="h-[calc(85vh-150px)]">
                <div className="space-y-2 pr-2">
                  {versions.map((version) => (
                    <Card 
                      key={version.id}
                      className={cn(
                        "cursor-pointer border transition-colors",
                        selectedVersion?.id === version.id
                          ? "border-primary bg-primary/5"
                          : "hover:bg-muted/50"
                      )}
                      onClick={() => handleSelectVersion(version)}
                    >
                      <CardHeader className="py-3 px-3">
                        <div className="flex justify-between items-center">
                          <Badge 
                            variant={version.publicStatus ? "default" : "outline"}
                            className={cn(
                              version.publicStatus ? "bg-green-100 text-green-700 hover:bg-green-100" : "",
                              "text-xs"
                            )}
                          >
                            {version.publicStatus ? "已发布" : "未发布"}
                          </Badge>
                          <span className="text-xs text-muted-foreground">
                            {formatDate(new Date(version.createdAt))}
                          </span>
                        </div>
                      </CardHeader>
                      <CardContent className="py-2 px-3">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="font-medium">v{version.version}</span>
                          {selectedVersion?.id === version.id && (
                            <Check className="h-3.5 w-3.5 text-primary" />
                          )}
                        </div>
                        <p className="text-xs line-clamp-2 text-muted-foreground">
                          {version.changeLog || "无更新说明"}
                        </p>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </ScrollArea>
            )}
          </div>
          
          {/* 右侧版本详情 */}
          <div className="flex-1 overflow-hidden">
            {selectedVersion ? (
              <Tabs 
                value={activeTab} 
                onValueChange={setActiveTab}
                className="h-full flex flex-col"
              >
                <TabsList className="mb-4">
                  <TabsTrigger value="overview">版本概览</TabsTrigger>
                  <TabsTrigger value="changelog">更新日志</TabsTrigger>
                  <TabsTrigger value="tools">工具功能</TabsTrigger>
                </TabsList>
                
                <ScrollArea className="flex-1">
                  <TabsContent value="overview" className="mt-0 h-full">
                    <div className="space-y-4">
                      {/* 版本基本信息 */}
                      <div className="flex items-start gap-4">
                        <div className="h-16 w-16 flex items-center justify-center rounded-md bg-muted shrink-0">
                          {selectedVersion.icon ? (
                            <img 
                              src={selectedVersion.icon} 
                              alt={selectedVersion.name} 
                              className="h-full w-full object-cover rounded-md" 
                            />
                          ) : (
                            <Wrench className="h-8 w-8 text-muted-foreground" />
                          )}
                        </div>
                        <div>
                          <h2 className="text-xl font-bold flex items-center gap-2">
                            {selectedVersion.name}
                            <Badge className="ml-2">v{selectedVersion.version}</Badge>
                            {selectedVersion.office && (
                              <Badge variant="secondary">官方</Badge>
                            )}
                          </h2>
                          <p className="text-muted-foreground">{selectedVersion.subtitle}</p>
                          
                          <div className="flex flex-wrap gap-2 mt-2">
                            {selectedVersion.labels.map((label, index) => (
                              <Badge key={index} variant="outline" className="text-xs">
                                {label}
                              </Badge>
                            ))}
                          </div>
                        </div>
                      </div>
                      
                      {/* 发布状态切换 */}
                      <Card>
                        <CardContent className="p-4">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              {selectedVersion.publicStatus ? (
                                <Eye className="h-5 w-5 text-green-600" />
                              ) : (
                                <EyeOff className="h-5 w-5 text-gray-400" />
                              )}
                              <div>
                                <h4 className="text-sm font-medium">发布状态</h4>
                                <p className="text-xs text-muted-foreground">
                                  {selectedVersion.publicStatus 
                                    ? "此版本当前对外可见，用户可以安装并使用" 
                                    : "此版本当前已隐藏，用户无法安装和使用"}
                                </p>
                              </div>
                            </div>
                            
                            <Button
                              variant={selectedVersion.publicStatus ? "default" : "outline"}
                              size="sm"
                              className={cn(
                                "min-w-[100px]",
                                selectedVersion.publicStatus && "bg-green-600 hover:bg-green-700"
                              )}
                              onClick={() => handleTogglePublicStatus(
                                selectedVersion, 
                                !selectedVersion.publicStatus
                              )}
                              disabled={updatingStatus === selectedVersion.id}
                            >
                              {updatingStatus === selectedVersion.id ? (
                                <>
                                  <span className="animate-spin h-4 w-4 mr-2 border-2 border-current border-t-transparent rounded-full"></span>
                                  更新中...
                                </>
                              ) : selectedVersion.publicStatus ? (
                                "取消发布"
                              ) : (
                                "发布版本"
                              )}
                            </Button>
                          </div>
                        </CardContent>
                      </Card>
                      
                      {/* 版本状态信息 */}
                      <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mt-4">
                        <Card>
                          <CardContent className="p-3 flex items-center gap-2">
                            <Calendar className="h-4 w-4 text-muted-foreground" />
                            <div>
                              <p className="text-xs text-muted-foreground">发布日期</p>
                              <p className="text-sm font-medium">
                                {formatDate(new Date(selectedVersion.createdAt))}
                              </p>
                            </div>
                          </CardContent>
                        </Card>
                        <Card>
                          <CardContent className="p-3 flex items-center gap-2">
                            <Download className="h-4 w-4 text-muted-foreground" />
                            <div>
                              <p className="text-xs text-muted-foreground">安装次数</p>
                              <p className="text-sm font-medium">
                                {selectedVersion.installCount || 0}
                              </p>
                            </div>
                          </CardContent>
                        </Card>
                        <Card>
                          <CardContent className="p-3 flex items-center gap-2">
                            <List className="h-4 w-4 text-muted-foreground" />
                            <div>
                              <p className="text-xs text-muted-foreground">功能数量</p>
                              <p className="text-sm font-medium">
                                {selectedVersion.toolList?.length || 0}
                              </p>
                            </div>
                          </CardContent>
                        </Card>
                      </div>
                      
                      {/* 版本描述 */}
                      <Card>
                        <CardHeader className="py-3 px-4">
                          <CardTitle className="text-base flex items-center gap-2">
                            <Info className="h-4 w-4" />
                            工具描述
                          </CardTitle>
                        </CardHeader>
                        <CardContent className="px-4 py-2 pb-4">
                          <div className="text-sm whitespace-pre-line">
                            {selectedVersion.description || "无描述"}
                          </div>
                        </CardContent>
                      </Card>
                    </div>
                  </TabsContent>
                  
                  <TabsContent value="changelog" className="mt-0">
                    <Card>
                      <CardHeader className="py-3 px-4">
                        <CardTitle className="text-base flex items-center gap-2">
                          <History className="h-4 w-4" />
                          版本更新日志
                        </CardTitle>
                        <CardDescription>
                          版本 {selectedVersion.version} 的更新内容
                        </CardDescription>
                      </CardHeader>
                      <CardContent className="px-4 py-2 pb-4">
                        {selectedVersion.changeLog ? (
                          <div className="prose prose-sm max-w-none">
                            <ReactMarkdown>
                              {selectedVersion.changeLog}
                            </ReactMarkdown>
                          </div>
                        ) : (
                          <div className="text-center text-muted-foreground py-4">
                            未提供更新日志
                          </div>
                        )}
                      </CardContent>
                    </Card>
                  </TabsContent>
                  
                  <TabsContent value="tools" className="mt-0">
                    <Card>
                      <CardHeader className="py-3 px-4">
                        <CardTitle className="text-base flex items-center gap-2">
                          <Command className="h-4 w-4" />
                          工具功能列表
                        </CardTitle>
                        <CardDescription>
                          此版本包含 {selectedVersion.toolList?.length || 0} 个功能
                        </CardDescription>
                      </CardHeader>
                      <CardContent className="px-4 py-2 pb-4">
                        {selectedVersion.toolList && selectedVersion.toolList.length > 0 ? (
                          <div className="space-y-4">
                            {selectedVersion.toolList.map((tool, index) => (
                              <div key={index} className="border rounded-md overflow-hidden">
                                <div className="bg-muted/30 px-3 py-2 flex items-center">
                                  <Command className="h-4 w-4 mr-2 text-primary" />
                                  <span className="font-medium">{tool.name}</span>
                                </div>
                                <div className="p-3">
                                  <p className="text-sm text-muted-foreground mb-3">
                                    {tool.description}
                                  </p>
                                  
                                  {tool.parameters && Object.keys(tool.parameters.properties).length > 0 && (
                                    <div>
                                      <p className="text-xs font-medium mb-2">参数:</p>
                                      <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                                        {Object.entries(tool.parameters.properties)
                                          .filter(([key]) => !['required', 'definitions', 'additionalProperties'].includes(key))
                                          .map(([key, value]: [string, any]) => {
                                            // 移除可能的特殊字符前缀
                                            const cleanKey = key.replace(/^\{/, '');
                                            const isRequired = tool.parameters?.required?.includes(cleanKey);
                                            
                                            if (!value.description) return null;
                                            
                                            return (
                                              <div key={key} className="border rounded p-2 text-xs">
                                                <div className="flex items-center gap-1">
                                                  <code className="bg-muted px-1 py-0.5 rounded text-xs">{cleanKey}</code>
                                                  {isRequired && (
                                                    <Badge variant="outline" className="h-4 px-1 text-[10px]">必填</Badge>
                                                  )}
                                                </div>
                                                <p className="mt-1 text-muted-foreground">
                                                  {value.description}
                                                </p>
                                              </div>
                                            );
                                          }).filter(Boolean)}
                                      </div>
                                    </div>
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <div className="text-center py-8 text-muted-foreground">
                            此版本不包含任何工具功能
                          </div>
                        )}
                      </CardContent>
                    </Card>
                  </TabsContent>
                </ScrollArea>
              </Tabs>
            ) : (
              <div className="h-full flex items-center justify-center text-center">
                <div>
                  <FileText className="h-12 w-12 mx-auto text-muted-foreground/60 mb-4" />
                  <h3 className="text-lg font-medium">无版本信息</h3>
                  <p className="text-muted-foreground">
                    {versions.length > 0 
                      ? "请从左侧选择一个版本查看详情" 
                      : "该工具尚未发布任何版本"}
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
        
        <DialogFooter className="mt-4">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
} 