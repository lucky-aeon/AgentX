package org.xhy.domain.neo4j.constant;

/**
 * @author shilong.zang
 * @date 06:54 <br/>
 */
public interface GraphExtractorPrompt {

     String graphExtractorPrompt = """
        从以下提供的文本中提取所有相关的实体和它们之间的关系，构建一个知识图谱。
        请严格按照以下JSON格式返回结果，不要包含任何额外的解释、注释或markdown标记。
        不要有```json这种
        这是目标JSON结构：
        {
          "documentId": "string", // 请为文档生成一个UUID
          "source": "string", // 来源，可留空
          "entities": [
            {
              "id": "string", // 实体的唯一ID
              "labels": ["string"], // 实体的类型或标签，例如：["人物", "组织"]
              "properties": {
                "name": "string", // 实体的核心名称
                "description": "string" // 对实体的简短描述
                // 其他识别出的属性
              }
            }
          ],
          "relationships": [
            {
              "sourceId": "string", // 关系源实体ID
              "targetId": "string", // 关系目标实体ID
              "type": "string", // 关系类型，例如："工作于"、"位于"、"拥有"
              "properties": {
                "description": "string" // 关系描述
                // 其他关系属性
              }
            }
          ]
        }

        提取指南：
        1. 实体类型包括但不限于：人物、组织、地点、技术、项目、概念、事件等
        2. 关系类型应该具有语义意义，如：工作于、位于、拥有、参与、创建、管理等
        3. 实体ID应该是描述性的，便于理解
        4. 实体的name属性是必须的，description属性用于补充说明
        5. 如果文本中没有明确的关系，不要强行创建
        6. 确保所有关系的sourceId和targetId都对应于entities中的实体ID

        待处理的文本如下：
        ---
        {{text}}
        ---
        """;

}
