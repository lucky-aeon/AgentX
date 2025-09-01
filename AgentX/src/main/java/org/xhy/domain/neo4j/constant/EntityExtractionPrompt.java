package org.xhy.domain.neo4j.constant;

/** 实体提取专用Prompt模板 用于从查询文本中提取相关实体
 * 
 * @author AgentX */
public interface EntityExtractionPrompt {

    String entityExtractionPrompt = """
            从以下查询文本中提取所有相关的实体信息。
            请严格按照以下JSON格式返回结果，不要包含任何额外的解释、注释或markdown标记。

            这是目标JSON结构：
            {
              "entities": [
                {
                  "text": "string",        // 实体文本
                  "type": "string",        // 实体类型：PERSON(人物)、ORGANIZATION(组织)、TECHNOLOGY(技术)、CONCEPT(概念)、LOCATION(地点)、EVENT(事件)、PRODUCT(产品)、OTHER(其他)
                  "confidence": 0.95       // 置信度(0-1之间的小数)
                }
              ]
            }

            提取指南：
            1. 实体类型说明：
               - PERSON: 人名、职位、角色等
               - ORGANIZATION: 公司、机构、团队、部门等
               - TECHNOLOGY: 技术栈、编程语言、框架、工具等
               - CONCEPT: 概念、理论、方法、策略等
               - LOCATION: 地点、区域、国家、城市等
               - EVENT: 事件、活动、项目等
               - PRODUCT: 产品、服务、系统等
               - OTHER: 其他重要实体

            2. 提取原则：
               - 只提取与查询主题相关的重要实体
               - 避免提取停用词和无意义的词汇
               - 实体文本应该是原文中的准确词汇
               - 置信度反映实体的重要性和准确性
               - 优先提取专有名词和关键概念

            3. 示例：
               查询："如何使用Spring Boot开发微服务架构？"
               提取结果：
               {
                 "entities": [
                   {"text": "Spring Boot", "type": "TECHNOLOGY", "confidence": 0.95},
                   {"text": "微服务架构", "type": "CONCEPT", "confidence": 0.90}
                 ]
               }

            待处理的查询文本：
            ---
            {{text}}
            ---
            """;
}
