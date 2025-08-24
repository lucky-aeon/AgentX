"use client"

import { useState, useRef, KeyboardEvent } from "react"
import { ArrowLeft, Loader2, X, Bold, Italic, Strikethrough, Heading, List, Quote, Table, Code, Image, Eye, Upload } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
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
import { uploadToolWithToast } from "@/lib/tool-service"
import { MessageMarkdown } from '@/components/ui/message-markdown'
import FileUpload from "@/components/ui/file-upload"

const mcpServerCommandTemp = '例如：{"mcpServers": {"file-system": {"args": ["-y", "@modelcontextprotocol/server-filesystem", "/etc/proxy"], "command": "npx"}}}';

// 表单验证模式 - 更新icon字段为字符串URL类型
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

export default function UploadToolPage() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [previewMode, setPreviewMode] = useState(false);
  const [newLabel, setNewLabel] = useState("");
  // 移除iconPreview状态，因为FileUpload组件会自己管理
  // const [iconPreview, setIconPreview] = useState<string | null>(null);
  const labelInputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  // 移除fileInputRef，因为FileUpload组件会自己管理
  // const fileInputRef = useRef<HTMLInputElement>(null);
  
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

  // 移除图标上传相关函数，现在由FileUpload组件处理
  // const handleIconUpload = ...
  // const clearIcon = ...

  // 提交表单
  const onSubmit = async (values: FormValues) => {
    try {
      setIsSubmitting(true);
      // 真实环境下调用API
      let installCommandObj = values.installCommand;
      try {
        installCommandObj = JSON.parse(values.installCommand);
      } catch (e) {
        // 如果不是合法JSON，提示错误并返回
        toast({
          title: "安装命令格式错误",
          description: "请填写合法的 JSON 格式安装命令",
          variant: "destructive"
        });
        setIsSubmitting(false);
        return;
      }
      const payload = {
        name: values.name,
        subtitle: values.subtitle,
        description: values.description,
        uploadUrl: values.uploadUrl,
        labels: values.labels,
        installCommand: installCommandObj,
        icon: values.icon, // 现在直接使用表单中的icon URL
      };
      const response = await uploadToolWithToast(payload);
      if (response.code === 200) {
        form.reset();
        // 移除setIconPreview(null);
        window.location.href = "/tools";
      }
    } catch (error) {
      console.error("提交工具失败", error);
      toast({
        title: "提交失败",
        description: "提交工具时出现错误，请稍后重试",
        variant: "destructive"
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

  return (
    <div className="container py-6">
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild className="mb-2">
          <Link href="/tools">
            <ArrowLeft className="mr-2 h-4 w-4" />
            返回工具市场
          </Link>
        </Button>
        <h1 className="text-3xl font-bold tracking-tight">上传工具</h1>
        <p className="text-muted-foreground">创建并分享您的工具到工具市场</p>
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
                              placeholder={mcpServerCommandTemp}
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

                    {/* 工具图标上传 */}
                    <FormField
                      control={form.control}
                      name="icon"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>工具图标</FormLabel>
                          <FormControl>
                            <FileUpload
                              variant="square"
                              size="xl"
                              value={field.value}
                              onChange={(url) => field.onChange(url)}
                              placeholder={<Upload className="h-6 w-6 text-muted-foreground/50" />}
                              uploadText="上传图标"
                              changeText="更换图标"
                              removeText="清除图标"
                              maxSize={2 * 1024 * 1024} // 2MB
                            />
                          </FormControl>
                          <FormDescription>
                            推荐尺寸 512x512px，大小不超过2MB
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                </CardContent>
              </Card>
              
              <div className="flex justify-end">
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  提交工具
                </Button>
              </div>
            </div>
            
            {/* 右侧 - Markdown编辑器 */}
            <div className="space-y-4">
              <Card>
                <CardContent className="p-6">
                  <div className="space-y-4">
                    <div className="flex justify-between items-center">
                      <h2 className="text-xl font-semibold">
                        详细描述 <span className="text-red-500">*</span>
                      </h2>
                      <div className="flex gap-2">
                        <Button 
                          type="button" 
                          variant={previewMode ? "outline" : "default"} 
                          size="sm"
                          onClick={() => setPreviewMode(false)}
                        >
                          编辑
                        </Button>
                        <Button 
                          type="button" 
                          variant={previewMode ? "default" : "outline"} 
                          size="sm"
                          onClick={() => setPreviewMode(true)}
                        >
                          预览
                        </Button>
                      </div>
                    </div>
                    
                    {!previewMode && (
                      <div className="border rounded-md bg-muted/30 p-1">
                        <div className="flex items-center flex-wrap gap-1 p-1">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleMarkdownFormat('bold')}
                            type="button"
                            className="h-9 w-9 p-0"
                          >
                            <Bold className="h-4 w-4" />
                            <span className="sr-only">粗体</span>
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleMarkdownFormat('italic')}
                            type="button"
                            className="h-9 w-9 p-0"
                          >
                            <Italic className="h-4 w-4" />
                            <span className="sr-only">斜体</span>
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleMarkdownFormat('strikethrough')}
                            type="button"
                            className="h-9 w-9 p-0"
                          >
                            <Strikethrough className="h-4 w-4" />
                            <span className="sr-only">删除线</span>
                          </Button>
                          <Separator orientation="vertical" className="mx-1 h-6" />
                          <ToggleGroup type="single" className="flex-wrap">
                            <ToggleGroupItem 
                              value="h1" 
                              size="sm"
                              onClick={() => handleMarkdownFormat('h1')}
                              className="text-sm px-2"
                            >
                              H1
                            </ToggleGroupItem>
                            <ToggleGroupItem 
                              value="h2" 
                              size="sm"
                              onClick={() => handleMarkdownFormat('h2')}
                              className="text-sm px-2"
                            >
                              H2
                            </ToggleGroupItem>
                            <ToggleGroupItem 
                              value="h3" 
                              size="sm"
                              onClick={() => handleMarkdownFormat('h3')}
                              className="text-sm px-2"
                            >
                              H3
                            </ToggleGroupItem>
                          </ToggleGroup>
                          <Separator orientation="vertical" className="mx-1 h-6" />
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleMarkdownFormat('list')}
                            type="button"
                            className="h-9 w-9 p-0"
                          >
                            <List className="h-4 w-4" />
                            <span className="sr-only">列表</span>
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleMarkdownFormat('quote')}
                            type="button"
                            className="h-9 w-9 p-0"
                          >
                            <Quote className="h-4 w-4" />
                            <span className="sr-only">引用</span>
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleMarkdownFormat('table')}
                            type="button"
                            className="h-9 w-9 p-0"
                          >
                            <Table className="h-4 w-4" />
                            <span className="sr-only">表格</span>
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleMarkdownFormat('code')}
                            type="button"
                            className="h-9 w-9 p-0"
                          >
                            <Code className="h-4 w-4" />
                            <span className="sr-only">代码</span>
                          </Button>
                          <Separator orientation="vertical" className="mx-1 h-6" />
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleMarkdownFormat('image')}
                            type="button"
                            className="h-9 w-9 p-0"
                          >
                            <Image className="h-4 w-4" />
                            <span className="sr-only">图片</span>
                          </Button>
                        </div>
                      </div>
                    )}
                    
                    <FormField
                      control={form.control}
                      name="description"
                      render={({ field }) => (
                        <FormItem>
                          {previewMode ? (
                            <div className="border rounded-md p-4 min-h-[400px] prose dark:prose-invert max-w-none">
                              <MessageMarkdown 
                                content={field.value || '### 预览\n\n开始编辑以查看预览'}
                                showCopyButton={true}
                              />
                            </div>
                          ) : (
                            <FormControl>
                              <textarea
                                className="flex min-h-[400px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 font-mono"
                                placeholder="# 工具名称

## 功能简介
描述工具的主要功能和用途

## 特性
- 特性1
- 特性2
- 特性3

## 使用方法
安装后，您可以在聊天中通过以下方式使用此工具"
                                {...field}
                                ref={(element) => {
                                  textareaRef.current = element;
                                }}
                              />
                            </FormControl>
                          )}
                          <FormDescription>
                            支持Markdown格式，可以添加标题、列表、代码块等
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </form>
      </Form>
    </div>
  )
} 