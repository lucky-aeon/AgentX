# RAG 发布功能需求规划

## 1. 核心设计原则

### 1.1 快照机制设计
- **完整快照**：`rag_versions` 表包含完整的文件数据快照，不依赖原始数据
- **数据独立**：版本快照创建后，原始数据的任何变更都不会影响快照
- **文件复制**：发布时需要复制所有文件内容、向量数据、文档单元等

### 1.2 审核机制设计
- **必须审核**：知识库发布到市场必须经过管理员审核
- **内容审核**：需要审核知识库的内容是否合规
- **审核状态**：审核中、已发布、拒绝、已下架

## 2. 数据库设计

### 2.1 RAG 版本表 (rag_versions) - 完整快照
```sql
CREATE TABLE rag_versions (
    id VARCHAR(36) PRIMARY KEY,
    
    -- 基本信息（快照时的数据）
    name VARCHAR(255) NOT NULL,                 -- 快照时的名称
    icon VARCHAR(255),                          -- 快照时的图标
    description TEXT,                           -- 快照时的描述
    user_id VARCHAR(36) NOT NULL,               -- 创建者
    
    -- 版本信息
    version VARCHAR(50) NOT NULL,               -- 版本号 (如 "1.0.0")
    change_log TEXT,                            -- 更新日志
    labels JSONB,                               -- 标签
    
    -- 原始数据集信息（仅用于标识，不依赖）
    original_rag_id VARCHAR(36) NOT NULL,       -- 原始RAG数据集ID（仅标识用）
    original_rag_name VARCHAR(255),             -- 原始RAG名称（快照时）
    
    -- 快照统计信息
    file_count INTEGER DEFAULT 0,               -- 文件数量
    total_size BIGINT DEFAULT 0,                -- 总大小
    document_count INTEGER DEFAULT 0,           -- 文档单元数量
    
    -- 发布状态（需要审核）
    publish_status INTEGER DEFAULT 1,           -- 1:审核中, 2:已发布, 3:拒绝, 4:已下架
    reject_reason TEXT,                         -- 审核拒绝原因
    review_time TIMESTAMP,                      -- 审核时间
    published_at TIMESTAMP,                     -- 发布时间
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
```

### 2.2 RAG 版本文件表 (rag_version_files) - 文件快照
```sql
CREATE TABLE rag_version_files (
    id VARCHAR(36) PRIMARY KEY,
    rag_version_id VARCHAR(36) NOT NULL,        -- 关联RAG版本
    
    -- 文件信息快照
    original_file_id VARCHAR(36) NOT NULL,      -- 原始文件ID（仅标识）
    file_name VARCHAR(255) NOT NULL,            -- 文件名
    file_size BIGINT DEFAULT 0,                 -- 文件大小
    file_type VARCHAR(50),                      -- 文件类型
    file_path VARCHAR(500),                     -- 文件存储路径
    
    -- 处理状态快照
    process_status INTEGER,                     -- 处理状态
    embedding_status INTEGER,                   -- 向量化状态
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
```

### 2.3 RAG 版本文档单元表 (rag_version_documents) - 文档内容快照
```sql
CREATE TABLE rag_version_documents (
    id VARCHAR(36) PRIMARY KEY,
    rag_version_id VARCHAR(36) NOT NULL,        -- 关联RAG版本
    rag_version_file_id VARCHAR(36) NOT NULL,   -- 关联版本文件
    
    -- 文档内容快照
    original_document_id VARCHAR(36),           -- 原始文档单元ID（仅标识）
    content TEXT NOT NULL,                      -- 文档内容
    page INTEGER,                               -- 页码
    
    -- 向量数据存储在向量数据库中，使用 rag_version_id 作为命名空间
    vector_id VARCHAR(36),                      -- 向量ID
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
```

### 2.4 RAG 安装表 (user_rags) - 用户安装记录
```sql
CREATE TABLE user_rags (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    rag_version_id VARCHAR(36) NOT NULL,        -- 关联的RAG版本快照
    
    -- 安装时的信息
    name VARCHAR(255) NOT NULL,                 -- 安装时的名称
    description TEXT,                           -- 安装时的描述
    icon VARCHAR(255),                          -- 安装时的图标
    version VARCHAR(50) NOT NULL,               -- 版本号
    
    -- 安装状态
    is_active BOOLEAN DEFAULT TRUE,             -- 是否激活
    installed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    UNIQUE KEY unique_user_rag_version (user_id, rag_version_id)
);
```

## 3. 快照创建流程

### 3.1 发布时的快照创建
1. **创建版本记录**：在 `rag_versions` 表中创建版本记录
2. **复制文件信息**：将原始数据集的所有文件信息复制到 `rag_version_files`
3. **复制文档内容**：将所有文档单元复制到 `rag_version_documents`
4. **复制向量数据**：在向量数据库中创建新的命名空间，复制所有向量数据
5. **设置审核状态**：设置为"审核中"状态

### 3.2 数据复制策略
```java
// 伪代码示例
public void createRagVersionSnapshot(String ragId, String version, String changeLog) {
    // 1. 创建版本记录
    RagVersionEntity ragVersion = new RagVersionEntity();
    ragVersion.setOriginalRagId(ragId);
    ragVersion.setVersion(version);
    ragVersion.setChangeLog(changeLog);
    ragVersion.setPublishStatus(PublishStatus.REVIEWING.getCode());
    ragVersionRepository.save(ragVersion);
    
    // 2. 复制文件信息
    List<FileDetailEntity> originalFiles = fileDetailRepository.findByDatasetId(ragId);
    for (FileDetailEntity file : originalFiles) {
        RagVersionFileEntity versionFile = new RagVersionFileEntity();
        BeanUtils.copyProperties(file, versionFile);
        versionFile.setRagVersionId(ragVersion.getId());
        versionFile.setOriginalFileId(file.getId());
        ragVersionFileRepository.save(versionFile);
        
        // 3. 复制文档单元
        List<DocumentUnitEntity> documents = documentUnitRepository.findByFileId(file.getId());
        for (DocumentUnitEntity doc : documents) {
            RagVersionDocumentEntity versionDoc = new RagVersionDocumentEntity();
            BeanUtils.copyProperties(doc, versionDoc);
            versionDoc.setRagVersionId(ragVersion.getId());
            versionDoc.setRagVersionFileId(versionFile.getId());
            versionDoc.setOriginalDocumentId(doc.getId());
            ragVersionDocumentRepository.save(versionDoc);
        }
    }
    
    // 4. 复制向量数据（在向量数据库中创建新的命名空间）
    embeddingService.copyVectorData(ragId, ragVersion.getId());
}
```

## 4. 前端页面设计

### 4.1 页面结构（采用工具页面模式）
```
/knowledge/
├── page.tsx (主页面，垂直布局包含3个Section)
├── [id]/
│   └── page.tsx (数据集详情页面)
├── components/
│   ├── cards/
│   │   ├── CreatedRagCard.tsx        # 我创建的RAG卡片 ✅
│   │   ├── InstalledRagCard.tsx      # 我安装的RAG卡片 ✅
│   │   └── MarketRagCard.tsx         # 市场RAG卡片 ✅
│   ├── dialogs/
│   │   ├── PublishRagDialog.tsx      # 发布RAG对话框 ✅
│   │   ├── InstallRagDialog.tsx      # 安装RAG对话框 ✅
│   │   └── RagVersionHistoryDialog.tsx # 版本历史对话框 ✅
│   └── sections/
│       ├── CreatedRagsSection.tsx    # "我创建的知识库" ✅
│       ├── InstalledRagsSection.tsx  # "我安装的知识库" ✅
│       └── RecommendedRagsSection.tsx # "推荐知识库" ✅
```

### 4.2 页面设计（已实现）
1. **我创建的知识库**
   - ✅ 显示用户创建的原始RAG数据集
   - ✅ 支持发布到市场（创建快照）
   - ✅ 支持版本管理和历史查看
   - ✅ 支持编辑和删除
   - ✅ 点击卡片跳转到详情页面

2. **我安装的知识库**
   - ✅ 显示用户安装的RAG版本快照
   - ✅ 支持卸载
   - ✅ 支持激活/停用状态切换
   - ✅ 显示版本信息和安装时间
   - ✅ 支持查看详情

3. **推荐知识库**
   - ✅ 显示市场上已发布的RAG版本
   - ✅ 支持搜索和筛选
   - ✅ 支持安装
   - ✅ 显示发布者信息、标签、评分等
   - ✅ 显示安装状态

### 4.3 数据集详情页面（已实现）
- ✅ 数据集基本信息显示
- ✅ 文件列表管理（上传、删除、查看）
- ✅ 文件处理状态跟踪（初始化、向量化）
- ✅ RAG智能问答对话
- ✅ 文档搜索功能
- ✅ 语料详情查看
- ✅ 分页浏览和搜索过滤

## 5. 审核机制

### 5.1 管理员审核页面
- **审核列表**：显示所有待审核的RAG版本
- **内容查看**：可以查看RAG的文件列表和部分内容
- **审核操作**：通过/拒绝，填写审核意见
- **下架功能**：对已发布的RAG进行下架

### 5.2 审核流程
1. 用户发布RAG（创建快照）
2. 状态设置为"审核中"
3. 管理员审核内容
4. 审核通过：状态改为"已发布"，在市场中显示
5. 审核拒绝：状态改为"拒绝"，用户可以修改后重新发布

## 6. 使用流程

### 6.1 对话中的RAG选择
1. **创建者**：可以选择自己创建的原始RAG数据集
2. **安装者**：可以选择已安装的RAG版本快照
3. **权限检查**：系统检查用户对RAG的使用权限
4. **数据检索**：根据选择的RAG进行检索和问答

### 6.2 RAG检索适配
- **原始数据集**：直接从 `ai_rag_qa_dataset` 和相关表检索
- **版本快照**：从 `rag_version_documents` 和对应的向量命名空间检索
- **统一接口**：提供统一的RAG检索接口，内部根据类型选择不同的数据源

## 7. 技术实现要点

### 7.1 向量数据隔离
```java
// 向量数据库命名空间设计
// 原始数据集：namespace = "dataset_" + ragId
// 版本快照：namespace = "version_" + ragVersionId

public List<Document> searchDocuments(String ragId, String ragVersionId, String query) {
    String namespace;
    if (ragVersionId != null) {
        namespace = "version_" + ragVersionId;
    } else {
        namespace = "dataset_" + ragId;
    }
    return embeddingService.search(namespace, query);
}
```

### 7.2 权限控制
```java
public boolean canUseRag(String userId, String ragId, String ragVersionId) {
    if (ragVersionId != null) {
        // 检查是否已安装该版本
        return userRagRepository.existsByUserIdAndRagVersionId(userId, ragVersionId);
    } else {
        // 检查是否为创建者
        RagQaDatasetEntity dataset = ragQaDatasetRepository.findById(ragId);
        return dataset.getUserId().equals(userId);
    }
}
```

## 8. 实施计划

### 阶段一：数据库和核心逻辑 ✅ 已完成
- [x] 创建RAG版本相关数据表
- [x] 实现快照创建逻辑
- [x] 实现向量数据复制
- [x] 创建基础的发布API

### 阶段二：审核机制 ✅ 已完成
- [x] 实现管理员审核页面
- [x] 添加审核状态管理
- [x] 实现审核流程API

### 阶段三：前端页面 ✅ 已完成
- [x] 重构知识库页面为垂直布局Section
- [x] 实现RAG发布对话框
- [x] 实现RAG市场页面
- [x] 实现RAG安装功能
- [x] 实现数据集详情页面
- [x] 修复API路径和UI交互问题

### 阶段四：集成和优化 ⏳ 进行中
- [ ] 在对话页面集成RAG选择
- [ ] 实现统一的RAG检索接口
- [ ] 权限控制和数据隔离
- [ ] 性能优化和测试

### 阶段五：UI优化和体验改进 ✅ 已完成
- [x] 知识库市场卡片优化 - 实现鼠标悬停显示按钮
- [x] 移除文件大小和版本号显示
- [x] 实现版本号自动递增逻辑
- [x] 实现知识库创建后自动创建0.0.1版本并安装
- [x] 添加删除保护 - 已安装的知识库不能删除
- [x] 优化已安装知识库卡片设计，参考工具市场设计
- [x] 修复RAG拒绝API调用的交互问题

### 阶段六：安全性和权限完善 ✅ 已完成
- [x] 修复前端用户ID硬编码问题 - 实现JWT token动态解析
- [x] 加强后端删除保护机制 - 防止绕过前端直接调用API
- [x] 完善双重删除保护逻辑 - 版本状态检查 + 安装状态检查
- [x] 添加用户身份验证的备用方案 - API获取用户信息
- [x] 确保删除保护的前后端一致性

## 9. 关键差异点

### 9.1 与工具机制的区别
| 特性 | 工具机制 | RAG机制 |
|------|---------|---------|
| 数据量 | 工具定义较小 | 文件和向量数据较大 |
| 复制策略 | 复制工具定义 | 复制文件+内容+向量 |
| 存储位置 | 数据库 | 数据库+向量数据库+文件存储 |
| 使用方式 | 安装后调用 | 安装后可选择使用 |

### 9.2 快照完整性保证
- **文件完整性**：复制所有文件的完整信息
- **内容完整性**：复制所有文档单元的完整内容
- **向量完整性**：复制所有向量数据到新的命名空间
- **元数据完整性**：复制所有相关的元数据信息

这个设计确保了RAG版本的完整快照特性，原始数据的任何变更都不会影响已发布的版本。

## 10. 已完成的API接口

### 10.1 RAG发布相关接口 (`/api/rag/publish`)

#### 发布RAG版本
```http
POST /api/rag/publish
Content-Type: application/json

{
  "ragId": "string",           // 原始RAG数据集ID
  "version": "string",         // 版本号（x.x.x格式）
  "changeLog": "string",       // 更新日志
  "labels": ["string"]         // 标签列表
}
```

#### 获取用户的RAG版本列表
```http
GET /api/rag/publish/versions?page=1&pageSize=15&keyword=search
```

#### 获取RAG版本历史
```http
GET /api/rag/publish/versions/history/{ragId}
```

#### 获取RAG版本详情
```http
GET /api/rag/publish/versions/{versionId}
```

### 10.2 RAG市场相关接口 (`/api/rag/market`)

#### 获取市场上的RAG版本列表
```http
GET /api/rag/market?page=1&pageSize=15&keyword=search
```

#### 安装RAG版本
```http
POST /api/rag/market/install
Content-Type: application/json

{
  "ragVersionId": "string"     // RAG版本ID
}
```

#### 卸载RAG版本
```http
DELETE /api/rag/market/uninstall/{ragVersionId}
```

#### 获取用户安装的RAG列表
```http
GET /api/rag/market/installed?page=1&pageSize=15&keyword=search
```

#### 获取用户安装的所有RAG（对话中使用）
```http
GET /api/rag/market/installed/all
```

#### 更新安装的RAG状态
```http
PUT /api/rag/market/installed/{ragVersionId}/status?isActive=true
```

#### 获取用户安装的RAG详情
```http
GET /api/rag/market/installed/{ragVersionId}
```

#### 检查RAG使用权限
```http
GET /api/rag/market/permission/check?ragId=xxx&ragVersionId=xxx
```

### 10.3 管理员审核相关接口 (`/api/admin/rag/review`)

#### 获取待审核的RAG版本列表
```http
GET /api/admin/rag/review/pending?page=1&pageSize=15
```

#### 审核RAG版本
```http
POST /api/admin/rag/review/{versionId}
Content-Type: application/json

{
  "status": 2,                 // 2:已发布, 3:拒绝
  "rejectReason": "string"     // 拒绝原因（拒绝时必填）
}
```

#### 获取RAG版本详情（审核用）
```http
GET /api/admin/rag/review/{versionId}
```

#### 下架RAG版本
```http
POST /api/admin/rag/review/{versionId}/remove
```

### 10.4 数据结构定义

#### RagVersionDTO
```typescript
interface RagVersionDTO {
  id: string;
  name: string;
  icon?: string;
  description?: string;
  userId: string;
  userNickname?: string;
  version: string;
  changeLog?: string;
  labels: string[];
  originalRagId: string;
  originalRagName?: string;
  fileCount: number;
  totalSize: number;
  documentCount: number;
  publishStatus: number;       // 1:审核中, 2:已发布, 3:拒绝, 4:已下架
  publishStatusDesc: string;
  rejectReason?: string;
  reviewTime?: string;
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
  installCount: number;
  isInstalled?: boolean;
}
```

#### UserRagDTO
```typescript
interface UserRagDTO {
  id: string;
  userId: string;
  ragVersionId: string;
  name: string;
  description?: string;
  icon?: string;
  version: string;
  isActive: boolean;
  installedAt: string;
  createdAt: string;
  updatedAt: string;
  originalRagId?: string;
  fileCount?: number;
  documentCount?: number;
  creatorNickname?: string;
}
```

#### RagMarketDTO
```typescript
interface RagMarketDTO {
  id: string;
  name: string;
  icon?: string;
  description?: string;
  version: string;
  labels: string[];
  userId: string;
  userNickname?: string;
  userAvatar?: string;
  fileCount: number;
  documentCount: number;
  totalSize: number;
  totalSizeDisplay: string;
  installCount: number;
  publishedAt: string;
  updatedAt: string;
  isInstalled?: boolean;
  rating?: number;
  reviewCount?: number;
}
```

## 11. 实现状态

### 11.1 已完成功能
- ✅ 数据库设计和迁移脚本
- ✅ Domain层实体类和Repository接口
- ✅ 领域服务和业务逻辑
- ✅ Application层服务和DTO
- ✅ Controller层API接口
- ✅ 审核机制和权限控制
- ✅ 快照机制实现
- ✅ 统一的API响应格式
- ✅ 前端页面实现（知识库页面重构）
- ✅ RAG卡片组件和交互逻辑
- ✅ 发布、安装、历史对话框
- ✅ 数据集详情页面和文件管理
- ✅ 前端服务层和API集成
- ✅ UI交互问题修复

### 11.2 待完成功能
- ⏳ 对话集成（RAG选择器）
- ✅ 管理员后台审核页面（已完成）
- ⏳ 向量数据复制（需要配合向量数据库）
- ⏳ 文件存储处理优化

### 11.3 重要改进
- ✅ 页面结构改为垂直布局（参考工具页面）
- ✅ 卡片交互改为下拉菜单模式
- ✅ 修复API路径重复问题
- ✅ 添加点击跳转到详情页面功能
- ✅ 完善数据集详情页面功能
- ✅ 知识库市场卡片悬停效果优化
- ✅ 版本号自动递增机制
- ✅ 知识库创建后自动安装功能
- ✅ 已安装知识库删除保护
- ✅ RAG审核API交互问题修复
- ✅ 用户身份验证安全性完善
- ✅ 前后端删除保护一致性保障

### 11.4 技术特点
- 遵循DDD架构设计
- 使用MyBatis-Plus进行数据访问
- 支持分页查询和搜索
- 完整的权限控制机制
- 事务性数据快照
- 统一的错误处理

## 12. 当前实现总结

### 12.1 功能完成度
**整体进度：约 90% 完成**

- **后端核心功能**：100% 完成
  - 数据库设计和API接口
  - 快照机制和审核流程
  - 权限控制和业务逻辑
  - 管理员后台审核功能
  - 安全性和权限保护机制
  
- **前端页面功能**：100% 完成
  - 知识库页面重构（垂直布局）
  - RAG发布、安装、管理功能
  - 数据集详情页面和文件管理
  - 完整的用户交互体验
  - UI优化和体验改进
  - 用户身份验证完善

### 12.2 主要成就
1. **用户体验优化**：
   - 采用工具页面的垂直布局模式，避免Tab切换
   - 下拉菜单交互模式，符合用户使用习惯
   - 点击卡片直接跳转到详情页面
   - 完善的加载状态和错误提示

2. **功能完整性**：
   - 支持RAG的发布、安装、卸载、管理
   - 版本历史查看和状态管理
   - 文件上传、处理、搜索等完整功能
   - RAG智能问答和文档搜索
   - 管理员后台审核管理
   - 自动版本递增和安装功能

3. **技术实现**：
   - 完整的API服务层和错误处理
   - 统一的数据结构和类型定义
   - 响应式设计和分页处理
   - 实时状态更新和进度跟踪
   - JWT身份验证和权限控制
   - 前后端双重安全保护机制

### 12.3 下一步计划
1. **对话集成**：在对话页面添加RAG选择器
2. **系统优化**：性能优化和错误处理完善
3. **向量数据处理**：完善向量数据复制和管理

### 12.4 重要文件清单
**前端组件**：
- `/knowledge/page.tsx` - 知识库主页面
- `/knowledge/[id]/page.tsx` - 数据集详情页面
- `/components/knowledge/sections/` - 三个Section组件
- `/components/knowledge/cards/` - 三个RAG卡片组件
- `/components/knowledge/dialogs/` - 三个对话框组件
- `/lib/rag-publish-service.ts` - RAG发布服务
- `/types/rag-publish.ts` - TypeScript类型定义

**后端接口**：
- `/api/rag/publish` - RAG发布相关API
- `/api/rag/market` - RAG市场相关API
- `/api/admin/rag/review` - 管理员审核API