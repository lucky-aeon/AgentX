# RAG快照机制完善需求文档

## 业务背景

### RAG系统架构概述
AgentX的RAG（知识库）系统采用分层发布机制：
1. **原始RAG数据集**：用户创建和维护的工作数据，可随时修改
2. **版本快照**：发布到市场的固化版本，内容不可变
3. **用户安装快照**：用户从市场安装的个人副本，完全隔离

### 版本管理机制
- 用户创建RAG后自动生成0.0.1版本（REFERENCE类型），仅创建者可见
- 用户可发布RAG版本到市场（1.0.0、1.1.0等），生成SNAPSHOT类型的快照
- 其他用户只能看到和安装已发布的SNAPSHOT版本
- 0.0.1版本永远不会被其他用户安装

### 安装类型说明
**REFERENCE类型（引用类型）**：
- 仅用于用户自己创建的RAG的0.0.1版本
- 动态引用原始RAG数据，随原始数据变化而更新
- 用户修改原始RAG时，对应的0.0.1版本安装记录也会同步更新

**SNAPSHOT类型（快照类型）**：
- 用于所有发布版本和他人安装的版本
- 完全独立的数据副本，与原始RAG隔离
- 安装后内容固定，不会因原始RAG变化而改变

## 当前问题分析

### 1. 快照数据不完整
目前的快照机制只复制了基本信息（name、description、icon），但缺少：
- 文件数据的完整快照
- 文档单元的完整快照
- 相关配置和元数据的快照

### 2. 数据访问逻辑混乱
在查看已安装RAG信息时：
- REFERENCE类型应该显示原始RAG的实时信息
- SNAPSHOT类型应该显示快照数据
- 但当前实现中都从`rag_versions`表获取信息，违背了快照原则

### 3. 快照服务未完善
`RagDataAccessService`中的快照方法大多返回空列表，未实现真正的快照数据获取。

## 需要修改的代码模块

### 1. 数据模型扩展

#### UserRagDTO (`/AgentX/src/main/java/org/xhy/application/rag/dto/UserRagDTO.java`)
**修改内容**：
- 添加`installType`字段
- 添加`isReferenceType()`和`isSnapshotType()`判断方法

#### UserRagEntity 相关表结构
**确认字段**：
- `install_type`字段已存在
- 快照数据字段（name、description、icon等）已存在

### 2. 快照创建机制

#### RagPublishAppService (`/AgentX/src/main/java/org/xhy/application/rag/RagPublishAppService.java`)
**需要完善的功能**：
- 发布版本时创建完整快照
- 复制所有文件到`rag_version_files`表
- 复制所有文档单元到`rag_version_documents`表
- 复制相关配置和元数据

#### RagVersionDomainService (`/AgentX/src/main/java/org/xhy/domain/rag/service/RagVersionDomainService.java`)
**需要添加的方法**：
- `createCompleteSnapshot()` - 创建完整版本快照
- `copyFilesToVersion()` - 复制文件快照
- `copyDocumentsToVersion()` - 复制文档快照

### 3. 快照安装机制

#### UserRagDomainService (`/AgentX/src/main/java/org/xhy/domain/rag/service/UserRagDomainService.java`)
**需要完善的方法**：
- `installRag()` - 安装SNAPSHOT类型时创建用户专属快照
- 添加快照数据复制逻辑

#### 新增服务类建议
**UserRagSnapshotService**：
- 专门处理用户级别的快照创建和管理
- `createUserSnapshot()` - 为用户创建专属快照
- `copySnapshotToUser()` - 从版本快照复制到用户快照

### 4. 数据访问优化

#### RagDataAccessService (`/AgentX/src/main/java/org/xhy/domain/rag/service/RagDataAccessService.java`)
**需要完善的方法**：
```java
// 当前返回空列表，需要实现
private List<FileDetailEntity> getSnapshotFiles(String versionId)
private List<DocumentUnitEntity> getSnapshotDocuments(String versionId)
private List<DocumentUnitEntity> getSnapshotDocumentsByOriginalFile(String versionId, String originalFileId)
```

**实现思路**：
- 从`rag_version_files`和`rag_version_documents`表获取快照数据
- 或从用户专属快照存储获取数据

### 5. 数据转换逻辑修复

#### UserRagAssembler (`/AgentX/src/main/java/org/xhy/application/rag/assembler/UserRagAssembler.java`)
**需要添加的方法**：
- `enrichWithReferenceInfo()` - 处理REFERENCE类型，获取原始RAG信息
- `enrichWithSnapshotInfo()` - 处理SNAPSHOT类型，使用快照数据

#### RagMarketAppService (`/AgentX/src/main/java/org/xhy/application/rag/RagMarketAppService.java`)
**需要修改的方法**：
- `getUserInstalledRags()` - 根据installType选择不同的数据丰富策略
- `getInstalledRagDetail()` - 同样按类型处理
- `getUserAllInstalledRags()` - 同样按类型处理

### 6. 数据库表结构

#### 确认现有表结构
- `rag_versions` - 版本基本信息快照
- `rag_version_files` - 版本文件快照
- `rag_version_documents` - 版本文档快照
- `user_rags` - 用户安装记录和基本信息快照

#### 可能需要的新表
**user_rag_files** - 用户专属文件快照（可选）：
- 如果采用用户级别完全隔离，可能需要此表
- 存储用户安装时的文件快照副本

**user_rag_documents** - 用户专属文档快照（可选）：
- 如果采用用户级别完全隔离，可能需要此表
- 存储用户安装时的文档快照副本

### 7. 前端相关调整

#### API接口修改
**控制器层**：
- 确保返回的`UserRagDTO`包含正确的快照数据
- 区分不同安装类型的数据来源

**前端类型定义** (`/agentx-frontend-plus/types/rag-publish.ts`)：
- 添加`installType`字段定义
- 更新相关接口类型

## 实现优先级

### 第一阶段：修复数据访问逻辑
1. 扩展`UserRagDTO`添加`installType`字段
2. 修改`UserRagAssembler`支持按类型处理
3. 修改`RagMarketAppService`中的查看逻辑
4. 确保REFERENCE类型显示实时数据，SNAPSHOT类型显示快照数据

### 第二阶段：完善快照创建机制
1. 修改发布流程，创建完整版本快照
2. 实现`RagDataAccessService`中的快照获取方法
3. 完善`UserRagDomainService`的安装逻辑

### 第三阶段：优化和测试
1. 性能优化：大量快照数据的存储和查询
2. 数据一致性：确保快照数据的完整性
3. 边界情况处理：删除、更新等操作的影响

## 设计原则

### 1. 数据隔离原则
- SNAPSHOT类型的数据必须完全独立
- 原始RAG的任何变化都不应影响已发布的快照
- 用户删除原始RAG后，已安装的快照仍应可用

### 2. 版本不可变原则
- 发布的版本内容永远不变
- 快照数据只能读取，不能修改
- 保证用户安装的版本内容稳定性

### 3. 性能考虑原则
- 快照数据可能很大，需要考虑存储和查询效率
- 可能需要引入缓存机制
- 考虑分页加载大量快照数据

### 4. 用户体验原则
- REFERENCE类型：用户期望看到最新数据
- SNAPSHOT类型：用户期望看到稳定的版本数据
- 明确标识数据来源和类型，避免用户困惑

## 注意事项

1. **数据量考虑**：快照可能包含大量文件和文档，需要考虑存储空间和查询性能
2. **并发安全**：多个用户同时安装同一版本时的数据一致性
3. **错误处理**：快照创建失败时的回滚机制
4. **历史兼容**：现有数据的迁移和兼容性处理
5. **权限控制**：确保用户只能访问自己安装的快照数据

## 验收标准

1. **功能验收**：
   - REFERENCE类型显示原始RAG的实时信息
   - SNAPSHOT类型显示安装时的快照信息
   - 原始RAG变化不影响已安装的SNAPSHOT版本

2. **性能验收**：
   - 大量快照数据的加载时间在可接受范围内
   - 快照创建过程不影响系统正常运行

3. **数据一致性验收**：
   - 快照数据完整且与发布时一致
   - 用户卸载重装后数据保持一致