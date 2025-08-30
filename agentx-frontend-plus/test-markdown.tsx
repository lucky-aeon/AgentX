"use client";

import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

// 测试组件
export function TestMarkdown() {
  // 测试不同的内联代码格式
  const testContent1 = "数据类型为 `character varying(36)`，推荐使用 UUID 格式。";
  const testContent2 = "这是一个测试：`code` 和 `another code`。";
  const testContent3 = `
  这是多种测试：
  - 单个反引号：\`simple\`
  - 带空格：\`character varying(36)\`
  - 多个：\`first\` 和 \`second\`
  `;

  console.log("Test content 1:", testContent1);
  console.log("Test content 2:", testContent2);
  console.log("Test content 3:", testContent3);

  return (
    <div className="p-4 space-y-4">
      <h2>Markdown 测试</h2>
      
      <div className="border p-4">
        <h3>测试 1 - 原始内容：</h3>
        <pre>{testContent1}</pre>
        <h3>ReactMarkdown 渲染结果：</h3>
        <ReactMarkdown 
          remarkPlugins={[remarkGfm]}
          components={{
            code: ({ inline, children, className, ...props }: any) => {
              console.log("Code component called:", { inline, children, className });
              const isInline = inline || !className?.includes('language-');
              
              if (isInline) {
                return (
                  <code 
                    className="!bg-red-100 !text-red-800 !px-1 !py-0.5 !rounded" 
                    {...props}
                  >
                    {children}
                  </code>
                );
              }
              return <code className={className} {...props}>{children}</code>;
            }
          }}
        >
          {testContent1}
        </ReactMarkdown>
      </div>

      <div className="border p-4">
        <h3>测试 2 - 原始内容：</h3>
        <pre>{testContent2}</pre>
        <h3>ReactMarkdown 渲染结果：</h3>
        <ReactMarkdown 
          remarkPlugins={[remarkGfm]}
          components={{
            code: ({ inline, children, className, ...props }: any) => {
              console.log("Code component called:", { inline, children, className });
              const isInline = inline || !className?.includes('language-');
              
              if (isInline) {
                return (
                  <code 
                    className="!bg-red-100 !text-red-800 !px-1 !py-0.5 !rounded" 
                    {...props}
                  >
                    {children}
                  </code>
                );
              }
              return <code className={className} {...props}>{children}</code>;
            }
          }}
        >
          {testContent2}
        </ReactMarkdown>
      </div>

      <div className="border p-4">
        <h3>测试 3 - 原始内容：</h3>
        <pre>{testContent3}</pre>
        <h3>ReactMarkdown 渲染结果：</h3>
        <ReactMarkdown 
          remarkPlugins={[remarkGfm]}
          components={{
            code: ({ inline, children, className, ...props }: any) => {
              console.log("Code component called:", { inline, children, className });
              const isInline = inline || !className?.includes('language-');
              
              if (isInline) {
                return (
                  <code 
                    className="!bg-red-100 !text-red-800 !px-1 !py-0.5 !rounded" 
                    {...props}
                  >
                    {children}
                  </code>
                );
              }
              return <code className={className} {...props}>{children}</code>;
            }
          }}
        >
          {testContent3}
        </ReactMarkdown>
      </div>
    </div>
  );
}