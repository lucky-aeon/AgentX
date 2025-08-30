"use client"

import { useState, useRef, KeyboardEvent, useEffect } from "react"
import { ArrowLeft, Loader2, X, Bold, Italic, Strikethrough, Heading, List, Quote, Table, Code, Image, Eye, Upload, Save } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent } from "@/components/ui/card"
import { 
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage 
} from "@/components/ui/form"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { toast } from "@/hooks/use-toast"
import { z } from "zod"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import Link from "next/link"
import { updateToolWithToast, getToolDetailWithToast } from "@/lib/tool-service"
import { MessageMarkdown } from '@/components/ui/message-markdown'
import { useParams, useRouter } from "next/navigation"
import { Tool } from "@/types/tool"
import FileUpload from "@/components/ui/file-upload"

// 表单验证模式 - 移除icon字段的File类型要求
const formSchema = z.object({
  name: z.string().min(2, "工具名称至少需要2个字符").max(50, "工具名称最多50个字符"),
  subtitle: z.string().min(2, "副标题至少需要2个字符").max(100, "副标题最多100个字符"),
  description: z.string().min(10, "描述至少需要10个字符"),
  uploadUrl: z.string().url("请输入有效的URL"),
  labels: z.array(z.string()).min(1, "请输入至少一个标签").max(5, "最多5个标签"),
  installCommand: z.string().min(1, "请输入安装命令"),
  icon: z.string().optional() // 改为字符串URL类型
});

type FormValues = z.infer<typeof formSchema>;

export default function EditToolPage() {
  const params = useParams();
  const router = useRouter();
  const toolId = params.id as string;
  
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [previewMode, setPreviewMode] = useState(false);
  const [newLabel, setNewLabel] = useState("");
  // 移除iconPreview状态，因为FileUpload组件会自己管理
  // const [iconPreview, setIconPreview] = useState<string | null>(null);
  const labelInputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  // 移除fileInputRef，因为FileUpload组件会自己管理
  // const fileInputRef = useRef<HTMLInputElement>(null);
  const [tool, setTool] = useState<Tool | null>(null);
  
  // 初始化表单
  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "",
      subtitle: "",
      description: "",
      uploadUrl: "",
      labels: [],
      installCommand: ""
    }
  });

  // 获取工具详情数据
  useEffect(() => {
    async function fetchToolDetail() {
      try {
        setIsLoading(true);
        const response = await getToolDetailWithToast(toolId);
        
        if (response.code === 200 && response.data) {
          const tool = response.data;
 
          
          // 填充表单数据
          form.reset({
            name: tool.name || "",
            subtitle: tool.subtitle || "",
            description: tool.description || "",
            uploadUrl: (tool as any).uploadUrl || "", 
            labels: tool.labels || [],
            installCommand: typeof (tool as any).installCommand === 'object' 
              ? JSON.stringify((tool as any).installCommand, null, 2) 
              : ((tool as any).installCommand || ""),
          });
          
          // 如果有图标，设置图标预览
          if (tool.icon) {
            // 直接设置到表单中
            form.setValue("icon", tool.icon);
          }
          setTool(tool);
        } else {
          toast({
            title: "获取工具详情失败",
            description: response.message || "无法加载工具数据",
            variant: "destructive",
          });
          // 获取失败后返回工具列表页
          router.push("/tools");
        }
      } catch (error) {
 
        toast({
          title: "获取工具详情失败",
          description: "无法加载工具数据，请稍后重试",
          variant: "destructive",
        });
        router.push("/tools");
      } finally {
        setIsLoading(false);
      }
    }
    
    fetchToolDetail();
  }, [toolId, form, router]);

  // 添加标签
  const addLabel = () => {
    if (!newLabel.trim()) return;
    
    const currentLabels = form.getValues("labels");
    if (currentLabels.includes(newLabel.trim())) {
      toast({
        variant: "destructive",
        title: "标签已存在",
        description: "请勿添加重复的标签"
      });
      return;
    }
    
    if (currentLabels.length >= 5) {
      toast({
        variant: "destructive",
        title: "标签数量超出限制",
        description: "最多添加5个标签"
      });
      return;
    }
    
    form.setValue("labels", [...currentLabels, newLabel.trim()], { shouldValidate: true });
    setNewLabel("");
  };

  // 处理键盘事件
  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addLabel();
    }
  };

  // 删除标签
  const removeLabel = (label: string) => {
    const currentLabels = form.getValues("labels");
    form.setValue(
      "labels", 
      currentLabels.filter(l => l !== label), 
      { shouldValidate: true }
    );
  };

  // 提交表单
  const onSubmit = async (values: FormValues) => {
    try {
      setIsSubmitting(true);
      
      // 准备更新数据
      let installCommandData;
      try {
        // 尝试解析为JSON对象
        installCommandData = JSON.parse(values.installCommand);
      } catch (e) {
        // 如果无法解析，直接使用原始字符串
        installCommandData = values.installCommand;
      }
      
      const updateData = {
        name: values.name,
        subtitle: values.subtitle,
        description: values.description,
        uploadUrl: values.uploadUrl,
        labels: values.labels,
        installCommand: installCommandData,
        icon: values.icon,
      };
      
 
      
      // 调用API更新工具
      const response = await updateToolWithToast(toolId, updateData);
      
      if (response.code === 200) {
        toast({
          title: "工具更新成功",
          description: "您的工具已更新成功",
        });
        
        router.push("/tools");
      }
    } catch (error) {
 
      toast({
        title: "更新失败",
        description: "无法更新工具，请稍后重试",
        variant: "destructive",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  // 在指定位置插入Markdown格式文本
  const insertMarkdown = (markdownSyntax: string, selectionOffsets?: {before: number, after: number}) => {
    if (!textareaRef.current) return;
    
    const textarea = textareaRef.current;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = textarea.value.substring(start, end);
    
    let textToInsert = '';
    if (selectedText) {
      if (selectionOffsets) {
        // 插入带有选区偏移的文本
        textToInsert = markdownSyntax.replace('$1', selectedText);
      } else {
        // 插入包裹选中文本的标记
        textToInsert = `${markdownSyntax}${selectedText}${markdownSyntax}`;
      }
    } else {
      if (selectionOffsets) {
        // 插入带有占位符的文本
        textToInsert = markdownSyntax.replace('$1', '文本');
      } else {
        // 仅插入标记
        textToInsert = markdownSyntax;
      }
    }
    
    const newText = 
      textarea.value.substring(0, start) + 
      textToInsert + 
      textarea.value.substring(end);
    
    // 更新表单值
    form.setValue('description', newText, { shouldValidate: true });
    
    // 设置新的光标位置
    setTimeout(() => {
      textarea.focus();
      if (selectedText) {
        if (selectionOffsets) {
          const newCursorPos = start + textToInsert.length;
          textarea.setSelectionRange(newCursorPos, newCursorPos);
        } else {
          textarea.setSelectionRange(
            start + markdownSyntax.length,
            end + markdownSyntax.length
          );
        }
      } else {
        if (selectionOffsets) {
          // 选中占位符文本以便用户可以直接替换
          const placeholderStart = start + selectionOffsets.before;
          const placeholderEnd = placeholderStart + 2; // "文本"的长度
          textarea.setSelectionRange(placeholderStart, placeholderEnd);
        } else {
          const newCursorPos = start + markdownSyntax.length;
          textarea.setSelectionRange(newCursorPos, newCursorPos);
        }
      }
    }, 0);
  };

  // Markdown格式功能处理器
  const handleMarkdownFormat = (format: string) => {
    switch (format) {
      case 'bold':
        insertMarkdown('**');
        break;
      case 'italic':
        insertMarkdown('*');
        break;
      case 'strikethrough':
        insertMarkdown('~~');
        break;
      case 'h1':
        insertMarkdown('# $1\n', { before: 2, after: 0 });
        break;
      case 'h2':
        insertMarkdown('## $1\n', { before: 3, after: 0 });
        break;
      case 'h3':
        insertMarkdown('### $1\n', { before: 4, after: 0 });
        break;
      case 'list':
        insertMarkdown('- $1\n', { before: 2, after: 0 });
        break;
      case 'quote':
        insertMarkdown('> $1\n', { before: 2, after: 0 });
        break;
      case 'table':
        insertMarkdown('\n| 标题1 | 标题2 | 标题3 |\n| --- | --- | --- |\n| 内容1 | 内容2 | 内容3 |\n');
        break;
      case 'code':
        insertMarkdown('\n```\n$1\n```\n', { before: 5, after: 4 });
        break;
      case 'image':
        insertMarkdown('![图片描述](图片链接)', { before: 2, after: 1 });
        break;
      default:
        break;
    }
  };

  // 加载状态
  if (isLoading) {
    return (
      <div className="container py-6">
        <div className="flex items-center justify-center h-60">
          <div className="flex flex-col items-center gap-2">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            <p className="text-muted-foreground">正在加载工具数据...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container py-6">
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild className="mb-2">
          <Link href="/tools">
            <ArrowLeft className="mr-2 h-4 w-4" />
            返回工具列表
          </Link>
        </Button>
        <h1 className="text-3xl font-bold tracking-tight">编辑工具</h1>
        <p className="text-muted-foreground">更新您的工具信息</p>
      </div>
      
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* 左侧表单 */}
            <div className="space-y-6">
              <Card>
                <CardContent className="p-6">
                  <div className="space-y-4">
                    <h2 className="text-xl font-semibold">基本信息</h2>
                    
                    <FormField
                      control={form.control}
                      name="name"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>
                            工具名称 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="输入工具名称" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="subtitle"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>
                            副标题 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="一句话描述工具功能" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    
                    <FormField
                      control={form.control}
                      name="labels"
                      render={() => (
                        <FormItem>
                          <FormLabel>
                            标签 <span className="text-red-500">*</span>
                          </FormLabel>
                          <div className="space-y-2">
                            <div className="flex flex-wrap gap-1 mb-2">
                              {form.getValues("labels").map((label) => (
                                <Badge 
                                  key={label} 
                                  className="flex items-center gap-1 px-3 py-1"
                                >
                                  {label}
                                  <X 
                                    className="h-3 w-3 cursor-pointer" 
                                    onClick={() => removeLabel(label)}
                                  />
                                </Badge>
                              ))}
                            </div>
                            <div className="flex gap-2">
                              <Input
                                ref={labelInputRef}
                                value={newLabel}
                                onChange={(e) => setNewLabel(e.target.value)}
                                onKeyDown={handleKeyDown}
                                placeholder="输入标签并按回车"
                                className="flex-1"
                              />
                              <Button type="button" onClick={addLabel} size="sm">
                                添加
                              </Button>
                            </div>
                          </div>
                          <FormDescription>
                            最多添加5个标签，用于分类您的工具
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    
                    <Separator />
                    
                    <h2 className="text-xl font-semibold">GitHub仓库</h2>
                    <FormField
                      control={form.control}
                      name="uploadUrl"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>
                            GitHub仓库URL <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input 
                              placeholder="https://github.com/username/repo" 
                              {...field} 
                            />
                          </FormControl>
                          <FormDescription>
                            您的GitHub仓库需要包含工具定义文件
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    
                    <FormField
                      control={form.control}
                      name="installCommand"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>安装命令：mcp server 的 stdio 或者 sse 格式</FormLabel>
                          <FormControl>
                            <textarea
                              className="flex min-h-[100px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                              placeholder="输入安装命令..."
                              {...field}
                            />
                          </FormControl>
                          <FormDescription>
                            命令将在安装时执行，请确保命令格式正确
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    
                    <FormField
                      control={form.control}
                      name="icon"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>工具图标</FormLabel>
                          <FormControl>
                            <FileUpload
                              variant="square"
                              size="lg"
                              value={field.value}
                              onChange={(url) => field.onChange(url)}
                              placeholder={<Upload className="h-6 w-6 text-muted-foreground" />}
                              uploadText="上传图标"
                              changeText="更换图标"
                              removeText="清除图标"
                              maxSize={2 * 1024 * 1024} // 2MB
                            />
                          </FormControl>
                          <FormDescription>
                            建议上传正方形PNG图标，尺寸为200x200
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                </CardContent>
              </Card>
            </div>
            
            {/* 右侧描述编辑器 */}
            <div className="space-y-6">
              <Card>
                <CardContent className="p-6">
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <h2 className="text-xl font-semibold">详细描述</h2>
                      <ToggleGroup type="single" value={previewMode ? "preview" : "edit"} className="border rounded-md">
                        <ToggleGroupItem 
                          value="edit" 
                          onClick={() => setPreviewMode(false)}
                          className="text-xs"
                        >
                          编辑
                        </ToggleGroupItem>
                        <ToggleGroupItem 
                          value="preview" 
                          onClick={() => setPreviewMode(true)}
                          className="text-xs"
                        >
                          预览
                        </ToggleGroupItem>
                      </ToggleGroup>
                    </div>
                    
                    {/* Markdown 工具栏 */}
                    {!previewMode && (
                      <div className="flex flex-wrap items-center gap-1 border rounded-md p-1 mt-2 mb-3 bg-muted/5">
                        <Button 
                          type="button"
                          variant="ghost" 
                          size="sm"
                          className="h-8 px-2"
                          onClick={() => handleMarkdownFormat('bold')}
                        >
                          <Bold className="h-4 w-4" />
                        </Button>
                        <Button 
                          type="button"
                          variant="ghost" 
                          size="sm"
                          className="h-8 px-2"
                          onClick={() => handleMarkdownFormat('italic')}
                        >
                          <Italic className="h-4 w-4" />
                        </Button>
                        <Button 
                          type="button"
                          variant="ghost" 
                          size="sm"
                          className="h-8 px-2"
                          onClick={() => handleMarkdownFormat('strikethrough')}
                        >
                          <Strikethrough className="h-4 w-4" />
                        </Button>
                        <Separator orientation="vertical" className="mx-1 h-8" />
                        <Button 
                          type="button"
                          variant="ghost" 
                          size="sm"
                          className="h-8 px-2"
                          onClick={() => handleMarkdownFormat('h1')}
                        >
                          <span className="text-xs font-bold">H1</span>
                        </Button>
                        <Button 
                          type="button"
                          variant="ghost" 
                          size="sm"
                          className="h-8 px-2"
                          onClick={() => handleMarkdownFormat('h2')}
                        >
                          <span className="text-xs font-bold">H2</span>
                        </Button>
                        <Button 
                          type="button"
                          variant="ghost" 
                          size="sm"
                          className="h-8 px-2"
                          onClick={() => handleMarkdownFormat('h3')}
                        >
                          <span className="text-xs font-bold">H3</span>
                        </Button>
                        <Separator orientation="vertical" className="mx-1 h-8" />
                        <Button 
                          type="button"
                          variant="ghost" 
                          size="sm"
                          className="h-8 px-2"
                          onClick={() => handleMarkdownFormat('list')}
                        >
                          <List className="h-4 w-4" />
                        </Button>
                        <Button 
                          type="button"
                          variant="ghost" 
                          size="sm"
                          className="h-8 px-2"
                          onClick={() => handleMarkdownFormat('quote')}
                        >
                          <Quote className="h-4 w-4" />
                        </Button>
                        <Button 
                          type="button"
                          variant="ghost" 
                          size="sm"
                          className="h-8 px-2"
                          onClick={() => handleMarkdownFormat('table')}
                        >
                          <Table className="h-4 w-4" />
                        </Button>
                        <Button 
                          type="button"
                          variant="ghost" 
                          size="sm"
                          className="h-8 px-2"
                          onClick={() => handleMarkdownFormat('code')}
                        >
                          <Code className="h-4 w-4" />
                        </Button>
                        <Button 
                          type="button"
                          variant="ghost" 
                          size="sm"
                          className="h-8 px-2"
                          onClick={() => handleMarkdownFormat('image')}
                        >
                          <Image className="h-4 w-4" />
                        </Button>
                      </div>
                    )}
                    
                    <FormField
                      control={form.control}
                      name="description"
                      render={({ field }) => (
                        <FormItem>
                          {!previewMode ? (
                            <div className="mt-4">
                              <FormControl>
                                <textarea
                                  className="flex min-h-[400px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                                  placeholder="使用Markdown语法编写工具的详细描述..."
                                  {...field}
                                  ref={(e) => {
                                    textareaRef.current = e as HTMLTextAreaElement;
                                  }}
                                />
                              </FormControl>
                              <FormDescription>
                                使用 Markdown 格式说明工具的功能、特点和使用方法
                              </FormDescription>
                              <FormMessage />
                            </div>
                          ) : (
                            <div className="mt-4 border rounded-md p-4 min-h-[400px] prose dark:prose-invert max-w-none">
                              <MessageMarkdown showCopyButton={true}
                                content={field.value}
                                
                              />
                            </div>
                          )}
                        </FormItem>
                      )}
                    />
                  </div>
                </CardContent>
              </Card>
              
              <div className="flex justify-end gap-4">
                <Button 
                  type="button" 
                  variant="outline"
                  onClick={() => router.push("/tools")}
                >
                  取消
                </Button>
                <Button
                  type="submit"
                  disabled={isSubmitting}
                >
                  {isSubmitting && (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  )}
                  更新工具
                </Button>
              </div>
            </div>
          </div>
        </form>
      </Form>
    </div>
  );
} 