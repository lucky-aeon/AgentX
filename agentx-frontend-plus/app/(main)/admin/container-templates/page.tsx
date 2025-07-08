"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious } from "@/components/ui/pagination";
import { toast } from "@/hooks/use-toast";
import { Plus, Edit, Trash2, Settings, Star, Play, Pause, Search, Container } from "lucide-react";
import { ContainerTemplateService, type ContainerTemplate, type PageResponse, type TemplateStatistics, type CreateContainerTemplateRequest, type UpdateContainerTemplateRequest } from "@/lib/container-template-service";
import { createContainerFromTemplateWithToast } from "@/lib/admin-container-service";

export default function ContainerTemplatesPage() {
  const [templates, setTemplates] = useState<ContainerTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [pageData, setPageData] = useState<PageResponse<ContainerTemplate>>({
    records: [],
    total: 0,
    size: 15,
    current: 1,
    pages: 0
  });
  const [statistics, setStatistics] = useState<TemplateStatistics>({
    totalTemplates: 0,
    enabledTemplates: 0
  });

  // 搜索和筛选状态
  const [searchQuery, setSearchQuery] = useState("");
  const [typeFilter, setTypeFilter] = useState<string>("all");
  const [enabledFilter, setEnabledFilter] = useState<string>("all");

  // 对话框状态
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<ContainerTemplate | null>(null);

  // 表单状态
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    type: "user",
    image: "ghcr.io/lucky-aeon/mcp-gateway",
    imageTag: "latest",
    internalPort: 8080,
    cpuLimit: 1.0,
    memoryLimit: 512,
    environment: {} as Record<string, string>,
    volumeMountPath: "/app/data",
    command: [] as string[],
    networkMode: "bridge",
    restartPolicy: "unless-stopped",
    enabled: true,
    isDefault: false,
    sortOrder: 0
  });

  // 获取模板列表
  const fetchTemplates = async (page: number = 1, keyword?: string, type?: string, enabled?: boolean) => {
    setLoading(true);
    try {
      const response = await ContainerTemplateService.getTemplates({
        page,
        pageSize: 15,
        keyword: keyword?.trim() || undefined,
        type: type === "all" ? undefined : type,
        enabled: enabled
      });

      if (response.code === 200) {
        setTemplates(response.data.records || []);
        setPageData(response.data);
      } else {
        toast({
          title: "获取模板列表失败",
          description: response.message,
          variant: "destructive",
        });
      }

      // 获取统计信息
      const statsResponse = await ContainerTemplateService.getStatistics();
      if (statsResponse.code === 200) {
        setStatistics(statsResponse.data);
      }

    } catch (error) {
      toast({
        title: "获取模板列表失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    fetchTemplates(1, searchQuery, typeFilter === "all" ? undefined : typeFilter, 
      enabledFilter === "all" ? undefined : enabledFilter === "enabled");
  };

  const handleCreate = async () => {
    try {
      const request: CreateContainerTemplateRequest = {
        name: formData.name,
        description: formData.description,
        type: formData.type,
        image: formData.image,
        imageTag: formData.imageTag,
        internalPort: formData.internalPort,
        cpuLimit: formData.cpuLimit,
        memoryLimit: formData.memoryLimit,
        environment: formData.environment,
        volumeMountPath: formData.volumeMountPath,
        command: formData.command,
        networkMode: formData.networkMode,
        restartPolicy: formData.restartPolicy,
        enabled: formData.enabled,
        isDefault: formData.isDefault,
        sortOrder: formData.sortOrder
      };

      const response = await ContainerTemplateService.createTemplate(request);
      
      if (response.code === 200) {
        toast({
          title: "创建成功",
          description: "容器模板已创建",
        });
        
        setIsCreateDialogOpen(false);
        resetForm();
        fetchTemplates();
      } else {
        toast({
          title: "创建失败",
          description: response.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "创建失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  const handleEdit = async () => {
    if (!selectedTemplate) return;

    try {
      const request: UpdateContainerTemplateRequest = {
        name: formData.name,
        description: formData.description,
        type: formData.type,
        image: formData.image,
        imageTag: formData.imageTag,
        internalPort: formData.internalPort,
        cpuLimit: formData.cpuLimit,
        memoryLimit: formData.memoryLimit,
        environment: formData.environment,
        volumeMountPath: formData.volumeMountPath,
        command: formData.command,
        networkMode: formData.networkMode,
        restartPolicy: formData.restartPolicy,
        enabled: formData.enabled,
        isDefault: formData.isDefault,
        sortOrder: formData.sortOrder
      };

      const response = await ContainerTemplateService.updateTemplate(selectedTemplate.id, request);
      
      if (response.code === 200) {
        toast({
          title: "更新成功",
          description: "容器模板已更新",
        });
        
        setIsEditDialogOpen(false);
        setSelectedTemplate(null);
        resetForm();
        fetchTemplates();
      } else {
        toast({
          title: "更新失败",
          description: response.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "更新失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  const handleDelete = async (template: ContainerTemplate) => {
    if (!confirm(`确定要删除模板 "${template.name}" 吗？`)) {
      return;
    }

    try {
      const response = await ContainerTemplateService.deleteTemplate(template.id);
      
      if (response.code === 200) {
        toast({
          title: "删除成功",
          description: "容器模板已删除",
        });
        
        fetchTemplates();
      } else {
        toast({
          title: "删除失败",
          description: response.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "删除失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  const handleToggleStatus = async (template: ContainerTemplate) => {
    try {
      const response = await ContainerTemplateService.toggleTemplateStatus(template.id, !template.enabled);
      
      if (response.code === 200) {
        toast({
          title: template.enabled ? "已禁用" : "已启用",
          description: `模板 "${template.name}" 已${template.enabled ? "禁用" : "启用"}`,
        });
        
        fetchTemplates();
      } else {
        toast({
          title: "操作失败",
          description: response.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "操作失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  const handleSetDefault = async (template: ContainerTemplate) => {
    try {
      const response = await ContainerTemplateService.setDefaultTemplate(template.id);
      
      if (response.code === 200) {
        toast({
          title: "设置成功",
          description: `模板 "${template.name}" 已设为默认模板`,
        });
        
        fetchTemplates();
      } else {
        toast({
          title: "设置失败",
          description: response.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "设置失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  const handleCreateContainer = async (template: ContainerTemplate) => {
    try {
      const response = await createContainerFromTemplateWithToast(template.id);
      
      if (response.code === 200) {
        // 容器创建成功，可以选择跳转到容器管理页面或显示容器信息
        toast({
          title: "容器创建成功",
          description: `已从模板 "${template.name}" 创建容器`,
        });
      }
    } catch (error) {
      // 错误处理已经由 withToast 处理了
    }
  };

  const resetForm = () => {
    setFormData({
      name: "",
      description: "",
      type: "user",
      image: "ghcr.io/lucky-aeon/mcp-gateway",
      imageTag: "latest",
      internalPort: 8080,
      cpuLimit: 1.0,
      memoryLimit: 512,
      environment: {},
      volumeMountPath: "/app/data",
      command: [],
      networkMode: "bridge",
      restartPolicy: "unless-stopped",
      enabled: true,
      isDefault: false,
      sortOrder: 0
    });
  };

  const openEditDialog = (template: ContainerTemplate) => {
    setSelectedTemplate(template);
    setFormData({
      name: template.name,
      description: template.description,
      type: template.type,
      image: template.image,
      imageTag: template.imageTag,
      internalPort: template.internalPort,
      cpuLimit: template.cpuLimit,
      memoryLimit: template.memoryLimit,
      environment: template.environment,
      volumeMountPath: template.volumeMountPath,
      command: template.command,
      networkMode: template.networkMode,
      restartPolicy: template.restartPolicy,
      enabled: template.enabled,
      isDefault: template.isDefault,
      sortOrder: template.sortOrder
    });
    setIsEditDialogOpen(true);
  };

  useEffect(() => {
    fetchTemplates();
  }, []);

  const getTypeLabel = (type: string) => {
    switch (type) {
      case "user":
        return "用户容器";
      case "review":
        return "审核容器";
      default:
        return type;
    }
  };

  const getTypeBadgeVariant = (type: string) => {
    switch (type) {
      case "review":
        return "destructive";
      case "user":
        return "default";
      default:
        return "secondary";
    }
  };

  return (
    <div className="container mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold">容器模板管理</h1>
          <p className="text-gray-600 mt-2">管理Docker容器模板，配置容器创建规格</p>
        </div>
        <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="w-4 h-4 mr-2" />
              创建模板
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>创建容器模板</DialogTitle>
              <DialogDescription>
                配置新的MCP网关容器模板
              </DialogDescription>
            </DialogHeader>
            
            <div className="space-y-4">
              {/* 基本信息 */}
              <div className="space-y-4">
                <div>
                  <Label htmlFor="name">模板名称 *</Label>
                  <Input
                    id="name"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="请输入模板名称"
                  />
                </div>
                
                <div>
                  <Label htmlFor="description">模板描述</Label>
                  <Textarea
                    id="description"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="请输入模板描述"
                    rows={2}
                  />
                </div>

                <div>
                  <Label htmlFor="type">容器类型 *</Label>
                  <Select value={formData.type} onValueChange={(value) => setFormData({ ...formData, type: value })}>
                    <SelectTrigger>
                      <SelectValue placeholder="选择容器类型" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="user">用户容器</SelectItem>
                      <SelectItem value="review">审核容器</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {/* 镜像配置 */}
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="image">镜像名称 *</Label>
                    <Input
                      id="image"
                      value={formData.image}
                      onChange={(e) => setFormData({ ...formData, image: e.target.value })}
                      placeholder="ghcr.io/lucky-aeon/mcp-gateway"
                    />
                  </div>
                  <div>
                    <Label htmlFor="imageTag">镜像标签</Label>
                    <Input
                      id="imageTag"
                      value={formData.imageTag}
                      onChange={(e) => setFormData({ ...formData, imageTag: e.target.value })}
                      placeholder="latest"
                    />
                  </div>
                </div>
              </div>

              {/* 资源配置 */}
              <div className="space-y-4">
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <Label htmlFor="internalPort">内部端口 *</Label>
                    <Input
                      id="internalPort"
                      type="number"
                      value={formData.internalPort || ""}
                      onChange={(e) => setFormData({ ...formData, internalPort: parseInt(e.target.value) || 0 })}
                      placeholder="8080"
                    />
                  </div>
                  <div>
                    <Label htmlFor="cpuLimit">CPU(核)</Label>
                    <Input
                      id="cpuLimit"
                      type="number"
                      step="0.1"
                      value={formData.cpuLimit || ""}
                      onChange={(e) => setFormData({ ...formData, cpuLimit: parseFloat(e.target.value) || 0 })}
                      placeholder="1.0"
                    />
                  </div>
                  <div>
                    <Label htmlFor="memoryLimit">内存(MB)</Label>
                    <Input
                      id="memoryLimit"
                      type="number"
                      value={formData.memoryLimit || ""}
                      onChange={(e) => setFormData({ ...formData, memoryLimit: parseInt(e.target.value) || 0 })}
                      placeholder="512"
                    />
                  </div>
                </div>
              </div>

              {/* 网络和存储 */}
              <div className="space-y-4">
                <div>
                  <Label htmlFor="volumeMountPath">数据卷路径</Label>
                  <Input
                    id="volumeMountPath"
                    value={formData.volumeMountPath}
                    onChange={(e) => setFormData({ ...formData, volumeMountPath: e.target.value })}
                    placeholder="/app/data"
                  />
                </div>
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="networkMode">网络模式</Label>
                    <Select value={formData.networkMode} onValueChange={(value) => setFormData({ ...formData, networkMode: value })}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="bridge">bridge</SelectItem>
                        <SelectItem value="host">host</SelectItem>
                        <SelectItem value="none">none</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <Label htmlFor="restartPolicy">重启策略</Label>
                    <Select value={formData.restartPolicy} onValueChange={(value) => setFormData({ ...formData, restartPolicy: value })}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="no">no</SelectItem>
                        <SelectItem value="always">always</SelectItem>
                        <SelectItem value="unless-stopped">unless-stopped</SelectItem>
                        <SelectItem value="on-failure">on-failure</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>
              </div>

              {/* 高级设置 */}
              <div className="space-y-4 pt-4 border-t">
                <div className="flex items-center space-x-2">
                  <Switch
                    id="enabled"
                    checked={formData.enabled}
                    onCheckedChange={(checked) => setFormData({ ...formData, enabled: checked })}
                  />
                  <Label htmlFor="enabled">启用模板</Label>
                </div>
                
                <div className="flex items-center space-x-2">
                  <Switch
                    id="isDefault"
                    checked={formData.isDefault}
                    onCheckedChange={(checked) => setFormData({ ...formData, isDefault: checked })}
                  />
                  <Label htmlFor="isDefault">设为默认模板</Label>
                </div>
                
                <div>
                  <Label htmlFor="sortOrder">排序权重</Label>
                  <Input
                    id="sortOrder"
                    type="number"
                    value={formData.sortOrder || ""}
                    onChange={(e) => setFormData({ ...formData, sortOrder: parseInt(e.target.value) || 0 })}
                    placeholder="0"
                  />
                </div>
              </div>
            </div>
            
            <DialogFooter>
              <Button variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
                取消
              </Button>
              <Button onClick={handleCreate}>
                创建模板
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">总模板数</CardTitle>
            <Settings className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{statistics.totalTemplates}</div>
            <p className="text-xs text-muted-foreground">
              所有容器模板
            </p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">启用模板</CardTitle>
            <Play className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{statistics.enabledTemplates}</div>
            <p className="text-xs text-muted-foreground">
              可用于创建容器
            </p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">禁用模板</CardTitle>
            <Pause className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{statistics.totalTemplates - statistics.enabledTemplates}</div>
            <p className="text-xs text-muted-foreground">
              暂停使用的模板
            </p>
          </CardContent>
        </Card>
      </div>

      {/* 搜索和筛选 */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                <Input
                  placeholder="搜索模板名称、描述或镜像..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10"
                  onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                />
              </div>
            </div>
            
            <Select value={typeFilter} onValueChange={setTypeFilter}>
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="模板类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">所有类型</SelectItem>
                <SelectItem value="user">用户容器</SelectItem>
                <SelectItem value="review">审核容器</SelectItem>
              </SelectContent>
            </Select>
            
            <Select value={enabledFilter} onValueChange={setEnabledFilter}>
              <SelectTrigger className="w-[120px]">
                <SelectValue placeholder="状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部状态</SelectItem>
                <SelectItem value="enabled">已启用</SelectItem>
                <SelectItem value="disabled">已禁用</SelectItem>
              </SelectContent>
            </Select>
            
            <Button onClick={handleSearch}>
              <Search className="w-4 h-4 mr-2" />
              搜索
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 模板列表 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
        {loading ? (
          Array.from({ length: 6 }).map((_, index) => (
            <Card key={index} className="animate-pulse">
              <CardHeader>
                <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                <div className="h-3 bg-gray-200 rounded w-1/2"></div>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  <div className="h-3 bg-gray-200 rounded"></div>
                  <div className="h-3 bg-gray-200 rounded w-2/3"></div>
                </div>
              </CardContent>
            </Card>
          ))
        ) : templates.length === 0 ? (
          <div className="col-span-full text-center py-12">
            <Settings className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-4 text-lg font-medium text-gray-900">暂无模板</h3>
            <p className="mt-2 text-gray-500">创建第一个容器模板来开始使用</p>
          </div>
        ) : (
          templates.map((template) => (
            <Card key={template.id} className="relative">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <CardTitle className="text-lg flex items-center gap-2">
                      {template.name}
                      {template.isDefault && (
                        <Badge variant="secondary" className="text-xs">
                          <Star className="w-3 h-3 mr-1" />
                          默认
                        </Badge>
                      )}
                    </CardTitle>
                    <div className="flex items-center gap-2 mt-2">
                      <Badge variant={getTypeBadgeVariant(template.type)}>
                        {getTypeLabel(template.type)}
                      </Badge>
                      <Badge variant={template.enabled ? "default" : "secondary"}>
                        {template.enabled ? "已启用" : "已禁用"}
                      </Badge>
                    </div>
                  </div>
                </div>
                <CardDescription className="mt-2">
                  {template.description || "暂无描述"}
                </CardDescription>
              </CardHeader>
              
              <CardContent>
                <div className="space-y-3">
                  <div className="flex justify-between items-center text-sm">
                    <span className="text-gray-600">镜像:</span>
                    <span className="font-mono text-xs bg-gray-100 px-2 py-1 rounded">
                      {template.fullImageName}
                    </span>
                  </div>
                  
                  <div className="grid grid-cols-3 gap-4 text-sm">
                    <div>
                      <span className="text-gray-600">端口:</span>
                      <div className="font-medium">{template.internalPort}</div>
                    </div>
                    <div>
                      <span className="text-gray-600">CPU:</span>
                      <div className="font-medium">{template.cpuLimit}核</div>
                    </div>
                    <div>
                      <span className="text-gray-600">内存:</span>
                      <div className="font-medium">{template.memoryLimit}MB</div>
                    </div>
                  </div>
                  
                  <div className="flex justify-between items-center text-xs text-gray-500">
                    <span>创建者: {template.createdBy}</span>
                    <span>{new Date(template.createdAt).toLocaleString('zh-CN')}</span>
                  </div>
                </div>
                
                <div className="flex gap-2 mt-4 flex-wrap">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => openEditDialog(template)}
                  >
                    <Edit className="w-4 h-4 mr-1" />
                    编辑
                  </Button>
                  
                  {template.type === "review" && template.enabled && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleCreateContainer(template)}
                      className="text-blue-600 hover:text-blue-700"
                    >
                      <Container className="w-4 h-4 mr-1" />
                      创建容器
                    </Button>
                  )}
                  
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleToggleStatus(template)}
                  >
                    {template.enabled ? (
                      <>
                        <Pause className="w-4 h-4 mr-1" />
                        禁用
                      </>
                    ) : (
                      <>
                        <Play className="w-4 h-4 mr-1" />
                        启用
                      </>
                    )}
                  </Button>
                  
                  {!template.isDefault && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleSetDefault(template)}
                    >
                      <Star className="w-4 h-4 mr-1" />
                      默认
                    </Button>
                  )}
                  
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleDelete(template)}
                    className="text-red-600 hover:text-red-700"
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>

      {/* 编辑对话框 */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>编辑容器模板</DialogTitle>
            <DialogDescription>
              修改MCP网关容器模板配置
            </DialogDescription>
          </DialogHeader>
          
          <div className="space-y-4">
            {/* 基本信息 */}
            <div className="space-y-4">
              <div>
                <Label htmlFor="edit-name">模板名称 *</Label>
                <Input
                  id="edit-name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="请输入模板名称"
                />
              </div>
              
              <div>
                <Label htmlFor="edit-description">模板描述</Label>
                <Textarea
                  id="edit-description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="请输入模板描述"
                  rows={2}
                />
              </div>
            </div>

            {/* 镜像配置 */}
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="edit-image">镜像名称 *</Label>
                  <Input
                    id="edit-image"
                    value={formData.image}
                    onChange={(e) => setFormData({ ...formData, image: e.target.value })}
                    placeholder="ghcr.io/lucky-aeon/mcp-gateway"
                  />
                </div>
                <div>
                  <Label htmlFor="edit-imageTag">镜像标签</Label>
                  <Input
                    id="edit-imageTag"
                    value={formData.imageTag}
                    onChange={(e) => setFormData({ ...formData, imageTag: e.target.value })}
                    placeholder="latest"
                  />
                </div>
              </div>
            </div>

            {/* 资源配置 */}
            <div className="space-y-4">
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <Label htmlFor="edit-internalPort">内部端口 *</Label>
                  <Input
                    id="edit-internalPort"
                    type="number"
                    value={formData.internalPort || ""}
                    onChange={(e) => setFormData({ ...formData, internalPort: parseInt(e.target.value) || 0 })}
                    placeholder="8080"
                  />
                </div>
                <div>
                  <Label htmlFor="edit-cpuLimit">CPU(核)</Label>
                  <Input
                    id="edit-cpuLimit"
                    type="number"
                    step="0.1"
                    value={formData.cpuLimit || ""}
                    onChange={(e) => setFormData({ ...formData, cpuLimit: parseFloat(e.target.value) || 0 })}
                    placeholder="1.0"
                  />
                </div>
                <div>
                  <Label htmlFor="edit-memoryLimit">内存(MB)</Label>
                  <Input
                    id="edit-memoryLimit"
                    type="number"
                    value={formData.memoryLimit || ""}
                    onChange={(e) => setFormData({ ...formData, memoryLimit: parseInt(e.target.value) || 0 })}
                    placeholder="512"
                  />
                </div>
              </div>
            </div>

            {/* 网络和存储 */}
            <div className="space-y-4">
              <div>
                <Label htmlFor="edit-volumeMountPath">数据卷路径</Label>
                <Input
                  id="edit-volumeMountPath"
                  value={formData.volumeMountPath}
                  onChange={(e) => setFormData({ ...formData, volumeMountPath: e.target.value })}
                  placeholder="/app/data"
                />
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="edit-networkMode">网络模式</Label>
                  <Select value={formData.networkMode} onValueChange={(value) => setFormData({ ...formData, networkMode: value })}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="bridge">bridge</SelectItem>
                      <SelectItem value="host">host</SelectItem>
                      <SelectItem value="none">none</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label htmlFor="edit-restartPolicy">重启策略</Label>
                  <Select value={formData.restartPolicy} onValueChange={(value) => setFormData({ ...formData, restartPolicy: value })}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="no">no</SelectItem>
                      <SelectItem value="always">always</SelectItem>
                      <SelectItem value="unless-stopped">unless-stopped</SelectItem>
                      <SelectItem value="on-failure">on-failure</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </div>

            {/* 高级设置 */}
            <div className="space-y-4 pt-4 border-t">
              <div className="flex items-center space-x-2">
                <Switch
                  id="edit-enabled"
                  checked={formData.enabled}
                  onCheckedChange={(checked) => setFormData({ ...formData, enabled: checked })}
                />
                <Label htmlFor="edit-enabled">启用模板</Label>
              </div>
              
              <div className="flex items-center space-x-2">
                <Switch
                  id="edit-isDefault"
                  checked={formData.isDefault}
                  onCheckedChange={(checked) => setFormData({ ...formData, isDefault: checked })}
                />
                <Label htmlFor="edit-isDefault">设为默认模板</Label>
              </div>
              
              <div>
                <Label htmlFor="edit-sortOrder">排序权重</Label>
                <Input
                  id="edit-sortOrder"
                  type="number"
                  value={formData.sortOrder || ""}
                  onChange={(e) => setFormData({ ...formData, sortOrder: parseInt(e.target.value) || 0 })}
                  placeholder="0"
                />
              </div>
            </div>
          </div>
          
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>
              取消
            </Button>
            <Button onClick={handleEdit}>
              保存更改
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}