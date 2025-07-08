import { MarketTool, UserTool, ToolStatus } from './types';

// 生成模拟市场工具数据
export function generateMockMarketTools(): MarketTool[] {
  const tools: MarketTool[] = [];
  
  const mockToolNames = [
    "Surge部署工具", "GitHub助手", "代码生成器", 
    "文档摘要工具", "SQL查询助手", "数据分析工具",
    "图像处理工具", "音频转文本", "API测试助手"
  ];
  
  const mockSubtitles = [
    "快速部署网站到Surge.sh", "管理GitHub仓库和PR", "自动生成多语言代码",
    "自动生成文档摘要", "SQL生成与优化", "数据可视化与分析",
    "图像编辑与优化", "将音频转换为文本", "测试API端点"
  ];
  
  const mockAuthors = ["用户1", "John Doe", "Jane Smith", "Dev Tools", "AI Helper"];
  
  const mockIcons = [null, "/icons/tool1.png", "/icons/tool2.png"];
  
  const mockLabels = ["数据分析", "代码生成", "文档处理", "图像处理", "音频处理", "网络爬虫", "开发工具", "办公工具"];
  
  for (let i = 0; i < mockToolNames.length; i++) {
    const randomLabels = [];
    const labelPool = [...mockLabels];
    const labelCount = Math.min(Math.floor(Math.random() * 5) + 1, 5); // 最多5个标签
    
    for (let j = 0; j < labelCount; j++) {
      if (labelPool.length === 0) break;
      const randomIndex = Math.floor(Math.random() * labelPool.length);
      randomLabels.push(labelPool[randomIndex]);
      labelPool.splice(randomIndex, 1);
    }
    
    // 随机决定是否为官方工具
    const isOffice = Math.random() > 0.7;
    if (isOffice) randomLabels.push("官方");
    
    const toolList = [];
    const toolCount = Math.floor(Math.random() * 5) + 1; // 1-5个工具
    
    for (let j = 0; j < toolCount; j++) {
      toolList.push({
        name: `功能${j + 1}`,
        description: `这是工具的第${j + 1}个功能，用于执行特定任务。`,
        inputSchema: {
          type: "object",
          properties: {
            param1: {
              type: "string",
              description: "参数1描述",
            }
          },
          required: ["param1"]
        }
      });
    }
    
    tools.push({
      id: `tool-${i + 5}`, // 避免与用户工具ID冲突
      name: mockToolNames[i],
      icon: mockIcons[i % mockIcons.length],
      subtitle: mockSubtitles[i],
      description: `# ${mockToolNames[i]}\n\n这是一个强大的工具，可以帮助您完成各种任务。\n\n## 特性\n\n- 特性1\n- 特性2\n- 特性3\n\n## 使用方法\n\n安装后，您可以在聊天中通过@${mockToolNames[i]}来使用此工具。`,
      user_id: `user-${i % 5 + 1}`,
      author: mockAuthors[i % mockAuthors.length],
      labels: randomLabels,
      tool_type: "mcp",
      upload_type: "github",
      upload_url: `https://github.com/example/tool-${i + 1}`,
      install_command: {
        type: "sse",
        url: `https://api.example.com/tools/${i + 1}`
      },
      tool_list: toolList,
      status: ToolStatus.APPROVED,
      is_office: isOffice,
      installCount: Math.floor(Math.random() * 5000),
      createdAt: new Date(Date.now() - Math.random() * 10000000000).toISOString(),
      updatedAt: new Date(Date.now() - Math.random() * 1000000000).toISOString()
    });
  }
  
  return tools;
}

// 生成模拟用户工具数据
export function generateMockUserTools(): UserTool[] {
  return [
    {
      id: "tool-1",
      name: "数据库查询",
      icon: null,
      subtitle: "数据库查询工具 (预授权).",
      description: "强大的数据库查询工具，支持多种数据库类型。\n\n## 功能亮点\n\n- 支持MySQL、PostgreSQL、SQLite等多种数据库\n- 自动补全SQL语句\n- 查询结果可视化\n- 支持导出为CSV、JSON等格式\n\n## 使用方法\n\n只需在对话中输入您的SQL查询需求，我会自动为您生成最佳查询语句。",
      user_id: "user-1",
      author: "junjiem",
      labels: ["数据", "查询", "utilities"],
      tool_type: "mcp",
      upload_type: "github",
      upload_url: "https://github.com/example/db-query",
      install_command: { type: "sse", url: "https://api.example.com/tools/1" },
      is_office: true,
      status: ToolStatus.APPROVED,
      usageCount: 47,
      current_version: "2.1.0",
      isOwner: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      tool_list: [
        {
          name: "查询数据",
          description: "执行SQL查询并返回结果",
          inputSchema: {
            type: "object",
            properties: {
              sql: {
                type: "string",
                description: "要执行的SQL查询语句",
              },
              database: {
                type: "string",
                description: "要连接的数据库名称",
              },
              limit: {
                type: "number",
                description: "限制返回的结果数量",
              }
            },
            required: ["sql", "database"]
          }
        },
        {
          name: "导出数据",
          description: "将查询结果导出为CSV或JSON格式",
          inputSchema: {
            type: "object",
            properties: {
              format: {
                type: "string",
                description: "导出格式，支持csv或json",
              },
              query_id: {
                type: "string",
                description: "要导出的查询ID",
              }
            },
            required: ["format", "query_id"]
          }
        },
        {
          name: "连接测试",
          description: "测试数据库连接是否可用",
          inputSchema: {
            type: "object",
            properties: {
              host: {
                type: "string",
                description: "数据库主机地址",
              },
              port: {
                type: "number",
                description: "数据库端口",
              },
              username: {
                type: "string",
                description: "用户名",
              },
              password: {
                type: "string",
                description: "密码",
              },
              database: {
                type: "string",
                description: "数据库名称",
              }
            },
            required: ["host", "username", "password", "database"]
          }
        }
      ]
    },
  ];
} 