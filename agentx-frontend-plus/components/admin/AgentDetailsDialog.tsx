"use client";

import React, { useState, useEffect } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Bot, User, MessageCircle, Code, Wrench, Calendar, Eye, ExternalLink } from "lucide-react";
import { Agent } from "@/lib/admin-agent-service";
import { AdminToolService, Tool } from "@/lib/admin-tool-service";
import { useToast } from "@/hooks/use-toast";
import { ToolDetailsDialog } from "./ToolDetailsDialog";

interface AgentDetailsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  agent: Agent | null;
}

export function AgentDetailsDialog({ open, onOpenChange, agent }: AgentDetailsDialogProps) {
  const [tools, setTools] = useState<Tool[]>([]);
  const [loadingTools, setLoadingTools] = useState(false);
  const [selectedTool, setSelectedTool] = useState<Tool | null>(null);
  const [toolDetailsOpen, setToolDetailsOpen] = useState(false);
  const { toast } = useToast();

  // 获取Agent使用的工具详情
  useEffect(() => {
    if (agent?.toolIds && agent.toolIds.length > 0) {
      fetchToolDetails(agent.toolIds);
    } else {
      setTools([]);
    }
  }, [agent]);

  const fetchToolDetails = async (toolIds: string[]) => {
    setLoadingTools(true);
    try {
      // 批量获取工具信息
      const response = await AdminToolService.getTools({ pageSize: 1000 }); // 获取所有工具
      
      if (response.code === 200) {
        // 筛选出Agent使用的工具
        const agentTools = response.data.records.filter(tool => 
          toolIds.includes(tool.id)
        );
        setTools(agentTools);
      } else {
        throw new Error(response.message);
      }
    } catch (error) {
      console.error('获取工具详情失败:', error);
      toast({
        variant: "destructive",
        title: "获取工具信息失败",
        description: "无法加载Agent使用的工具详情"
      });
    } finally {
      setLoadingTools(false);
    }
  };

  // 处理查看工具详情
  const handleViewToolDetails = (tool: Tool) => {
    setSelectedTool(tool);
    setToolDetailsOpen(true);
  };

  if (!agent) return null;

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-4xl max-h-[85vh] overflow-hidden">
          <DialogHeader>
            <DialogTitle className="flex items-center space-x-3">
              <Avatar className="h-12 w-12">
                <AvatarImage src={agent.avatar} />
                <AvatarFallback>
                  <Bot className="h-6 w-6" />
                </AvatarFallback>
              </Avatar>
              <div>
                <h3 className="text-xl font-semibold">{agent.name}</h3>
                <div className="flex items-center space-x-2 mt-1">
                  <Badge variant={agent.enabled ? "default" : "secondary"}>
                    {agent.enabled ? "启用中" : "已禁用"}
                  </Badge>
                  {agent.publishedVersion && (
                    <Badge variant="outline">
                      版本 {agent.publishedVersion}
                    </Badge>
                  )}
                </div>
              </div>
            </DialogTitle>
          </DialogHeader>

          <ScrollArea className="h-[70vh] pr-4">
            <div className="space-y-6">
              {/* 基本信息 */}
              <div className="space-y-4">
                <h4 className="text-sm font-medium text-gray-900 flex items-center">
                  <Bot className="w-4 h-4 mr-2" />
                  基本信息
                </h4>
                <div className="grid grid-cols-1 gap-4">
                  <div>
                    <label className="text-xs font-medium text-gray-500">Agent描述</label>
                    <div className="mt-1 text-sm text-gray-700 bg-gray-50 p-3 rounded-lg">
                      {agent.description || "暂无描述"}
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="text-xs font-medium text-gray-500">状态</label>
                      <div className="mt-1">
                        <Badge variant={agent.enabled ? "default" : "secondary"}>
                          {agent.enabled ? "启用中" : "已禁用"}
                        </Badge>
                      </div>
                    </div>
                    <div>
                      <label className="text-xs font-medium text-gray-500">创建时间</label>
                      <div className="mt-1 flex items-center space-x-2">
                        <Calendar className="w-4 h-4 text-gray-400" />
                        <span className="text-sm">{new Date(agent.createdAt).toLocaleString('zh-CN')}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <Separator />

              {/* 创建者信息 */}
              <div className="space-y-4">
                <h4 className="text-sm font-medium text-gray-900 flex items-center">
                  <User className="w-4 h-4 mr-2" />
                  创建者信息
                </h4>
                <div className="flex items-center space-x-3">
                  <Avatar className="h-10 w-10">
                    <AvatarImage src={agent.userAvatarUrl} />
                    <AvatarFallback>
                      {agent.userNickname?.charAt(0) || agent.userEmail?.charAt(0) || 'U'}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <div className="font-medium">
                      {agent.userNickname || agent.userEmail || '未知用户'}
                    </div>
                    <div className="text-sm text-gray-500">{agent.userEmail}</div>
                  </div>
                </div>
              </div>

              <Separator />

              {/* 系统提示词 */}
              {agent.systemPrompt && (
                <>
                  <div className="space-y-4">
                    <h4 className="text-sm font-medium text-gray-900 flex items-center">
                      <MessageCircle className="w-4 h-4 mr-2" />
                      系统提示词
                    </h4>
                    <div className="text-sm text-gray-700 bg-gray-50 p-4 rounded-lg border-l-4 border-blue-200">
                      <pre className="whitespace-pre-wrap font-mono text-xs leading-relaxed">
                        {agent.systemPrompt}
                      </pre>
                    </div>
                  </div>
                  <Separator />
                </>
              )}

              {/* 欢迎消息 */}
              {agent.welcomeMessage && (
                <>
                  <div className="space-y-4">
                    <h4 className="text-sm font-medium text-gray-900 flex items-center">
                      <MessageCircle className="w-4 h-4 mr-2" />
                      欢迎消息
                    </h4>
                    <div className="text-sm text-gray-700 bg-green-50 p-4 rounded-lg border-l-4 border-green-200">
                      {agent.welcomeMessage}
                    </div>
                  </div>
                  <Separator />
                </>
              )}

              {/* 使用的工具 */}
              <div className="space-y-4">
                <h4 className="text-sm font-medium text-gray-900 flex items-center">
                  <Wrench className="w-4 h-4 mr-2" />
                  使用的工具 ({agent.toolIds?.length || 0})
                </h4>
                {loadingTools ? (
                  <div className="text-sm text-gray-500">加载工具信息中...</div>
                ) : tools.length > 0 ? (
                  <div className="grid grid-cols-1 gap-3">
                    {tools.map((tool) => (
                      <div key={tool.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                        <div className="flex items-center space-x-3">
                          <div className="w-8 h-8 bg-gray-200 rounded-lg flex items-center justify-center">
                            {tool.icon ? (
                              <img src={tool.icon} alt={tool.name} className="w-6 h-6 rounded" />
                            ) : (
                              <Code className="h-4 w-4 text-gray-500" />
                            )}
                          </div>
                          <div>
                            <div className="text-sm font-medium">{tool.name}</div>
                            <div className="text-xs text-gray-500">
                              {tool.description ? tool.description.substring(0, 50) + (tool.description.length > 50 ? '...' : '') : '暂无描述'}
                            </div>
                            <div className="flex items-center space-x-1 mt-1">
                              <Badge variant={tool.isOffice ? "default" : "outline"} className="text-xs">
                                {tool.isOffice ? "官方" : "第三方"}
                              </Badge>
                              {tool.isGlobal && (
                                <Badge variant="secondary" className="text-xs">全局</Badge>
                              )}
                            </div>
                          </div>
                        </div>
                        <Button 
                          variant="ghost" 
                          size="sm"
                          onClick={() => handleViewToolDetails(tool)}
                        >
                          <Eye className="w-4 h-4 mr-1" />
                          查看
                        </Button>
                      </div>
                    ))}
                  </div>
                ) : agent.toolIds && agent.toolIds.length > 0 ? (
                  <div className="text-sm text-gray-500 bg-yellow-50 p-4 rounded-lg border-l-4 border-yellow-200">
                    <div className="font-medium text-yellow-800">工具信息加载失败</div>
                    <div className="text-yellow-600 mt-1">
                      此Agent配置了 {agent.toolIds.length} 个工具，但无法获取详细信息
                    </div>
                  </div>
                ) : (
                  <div className="text-sm text-gray-500 bg-gray-50 p-4 rounded-lg text-center">
                    此Agent暂未配置任何工具
                  </div>
                )}
              </div>

              {/* 知识库 */}
              {agent.knowledgeBaseIds && agent.knowledgeBaseIds.length > 0 && (
                <>
                  <Separator />
                  <div className="space-y-4">
                    <h4 className="text-sm font-medium text-gray-900 flex items-center">
                      <Code className="w-4 h-4 mr-2" />
                      知识库 ({agent.knowledgeBaseIds.length})
                    </h4>
                    <div className="grid grid-cols-2 gap-3">
                      {agent.knowledgeBaseIds.map((kbId, index) => (
                        <div key={kbId} className="flex items-center space-x-2 p-2 bg-gray-50 rounded">
                          <div className="text-sm">知识库 #{index + 1}</div>
                          <div className="text-xs text-gray-500">暂未获取名称</div>
                        </div>
                      ))}
                    </div>
                  </div>
                </>
              )}

              {/* 版本信息 */}
              {agent.versions && agent.versions.length > 0 && (
                <>
                  <Separator />
                  <div className="space-y-4">
                    <h4 className="text-sm font-medium text-gray-900 flex items-center">
                      <Calendar className="w-4 h-4 mr-2" />
                      版本历史 ({agent.versions.length})
                    </h4>
                    <div className="space-y-2 max-h-40 overflow-y-auto">
                      {agent.versions.slice(0, 5).map((version) => (
                        <div key={version.id} className="flex items-center justify-between p-2 bg-gray-50 rounded">
                          <div className="flex items-center space-x-2">
                            <span className="text-sm font-medium">v{version.versionNumber}</span>
                            <Badge 
                              variant={
                                version.publishStatus === 2 ? "default" : 
                                version.publishStatus === 3 ? "destructive" : 
                                "secondary"
                              }
                              className="text-xs"
                            >
                              {version.publishStatus === 1 ? "审核中" : 
                               version.publishStatus === 2 ? "已发布" : 
                               version.publishStatus === 3 ? "已拒绝" : "已下架"}
                            </Badge>
                          </div>
                          <div className="text-xs text-gray-500">
                            {new Date(version.createdAt).toLocaleDateString()}
                          </div>
                        </div>
                      ))}
                      {agent.versions.length > 5 && (
                        <div className="text-xs text-gray-500 text-center py-2">
                          还有 {agent.versions.length - 5} 个版本...
                        </div>
                      )}
                    </div>
                  </div>
                </>
              )}
            </div>
          </ScrollArea>
        </DialogContent>
      </Dialog>
      
      {/* 工具详情弹窗 */}
      <ToolDetailsDialog
        open={toolDetailsOpen}
        onOpenChange={setToolDetailsOpen}
        tool={selectedTool}
      />
    </>
  );
}