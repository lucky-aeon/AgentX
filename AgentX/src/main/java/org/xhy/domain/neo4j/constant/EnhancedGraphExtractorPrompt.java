package org.xhy.domain.neo4j.constant;

/** 增强的图谱提取提示词 解决分页上下文丢失和主节点缺失问题
 * 
 * @author AgentX */
public interface EnhancedGraphExtractorPrompt {

    /** 分页上下文感知的图谱提取提示词 包含前后页面的上下文信息 */
    String contextAwareExtractionPrompt = """
            你正在处理一个文档的第 {{pageNumber}} 页（共 {{totalPages}} 页）。

            {{#hasPreviousContext}}
            上一页的关键实体和概念：
            {{previousContext}}
            {{/hasPreviousContext}}

            {{#hasNextContext}}
            下一页的预览信息：
            {{nextContext}}
            {{/hasNextContext}}

            从以下当前页内容中提取实体和关系，特别注意：
            1. 与上下页实体的连接关系
            2. 跨页实体的引用和指代
            3. 当前页在整个文档中的语义角色

            请严格按照以下JSON格式返回结果：
            {
              "entities": [
                {
                  "id": "string",           // 实体唯一ID，跨页一致
                  "labels": ["string"],     // 实体类型标签
                  "properties": {
                    "name": "string",       // 实体名称
                    "description": "string", // 详细描述
                    "pageNumber": {{pageNumber}}, // 当前页码
                    "contextRole": "string", // 在文档中的语义角色：main_topic|supporting_detail|cross_reference
                    "importance": 0.95      // 重要性评分(0-1)
                  }
                }
              ],
              "relationships": [
                {
                  "sourceId": "string",
                  "targetId": "string",
                  "type": "string",        // 关系类型
                  "properties": {
                    "description": "string",
                    "pageNumber": {{pageNumber}},
                    "strength": 0.8,       // 关系强度(0-1)
                    "crossPage": false     // 是否为跨页关系
                  }
                }
              ],
              "crossPageReferences": [     // 跨页引用实体
                {
                  "entityId": "string",    // 当前页实体ID
                  "referencedEntityId": "string", // 引用的其他页实体ID
                  "referenceType": "string" // 引用类型：same_entity|related_concept|continuation
                }
              ]
            }

            提取指南：
            1. 实体重要性评分：主题实体(0.8-1.0)，支撑细节(0.5-0.8)，次要信息(0.1-0.5)
            2. 语义角色识别：
               - main_topic: 当前页的核心主题
               - supporting_detail: 支撑主题的细节信息
               - cross_reference: 对其他页面内容的引用
            3. 跨页引用识别：
               - same_entity: 与其他页相同的实体
               - related_concept: 相关概念的引用
               - continuation: 内容的延续

            当前页内容：
            ---
            {{text}}
            ---
            """;

    /** 文档级主节点和结构化提示词 在所有页面处理完成后统一构建文档结构 */
    String documentStructuringPrompt = """
            基于已提取的所有页面实体和关系，为文档创建结构化的知识图谱。

            文档信息：
            - 文档ID: {{documentId}}
            - 总页数: {{totalPages}}
            - 文档类型: {{documentType}}

            已提取实体总数: {{totalEntities}}
            已提取关系总数: {{totalRelationships}}

            请创建以下结构化输出：
            {
              "documentNode": {
                "id": "DOC_{{documentId}}",
                "labels": ["Document", "{{documentType}}"],
                "properties": {
                  "name": "string",         // 文档标题/主题
                  "description": "string",  // 文档整体描述
                  "totalPages": {{totalPages}},
                  "mainTopics": ["string"], // 主要主题列表
                  "documentType": "{{documentType}}"
                }
              },
              "topicNodes": [              // 主题层级节点
                {
                  "id": "TOPIC_{{topicId}}",
                  "labels": ["Topic"],
                  "properties": {
                    "name": "string",
                    "description": "string",
                    "importance": 0.95,
                    "pageRange": "1-3"      // 涉及的页面范围
                  }
                }
              ],
              "hierarchicalRelationships": [ // 层次关系
                {
                  "sourceId": "DOC_{{documentId}}",
                  "targetId": "TOPIC_{{topicId}}",
                  "type": "CONTAINS_TOPIC",
                  "properties": {
                    "description": "文档包含主题",
                    "hierarchyLevel": 1
                  }
                },
                {
                  "sourceId": "TOPIC_{{topicId}}",
                  "targetId": "entity_id",
                  "type": "INCLUDES_ENTITY",
                  "properties": {
                    "description": "主题包含实体",
                    "hierarchyLevel": 2
                  }
                }
              ],
              "consolidatedEntities": [    // 合并重复实体后的结果
                {
                  "id": "string",
                  "consolidatedFrom": ["entity_id1", "entity_id2"], // 合并来源
                  "properties": {
                    // 合并后的属性
                  }
                }
              ]
            }

            构建原则：
            1. 文档节点作为根节点，连接所有内容
            2. 识别3-5个主要主题作为中间层节点
            3. 将相似实体进行智能合并
            4. 建立清晰的层次结构：文档→主题→实体
            5. 保持跨页关系的连续性

            实体数据：
            {{entityData}}

            关系数据：
            {{relationshipData}}
            """;
}
