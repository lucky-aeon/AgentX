package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.util.ast.Node;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.MarkdownTokenProcessor;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.util.HashMap;
import java.util.Map;

/** 表格Token处理器 将表格转换为可搜索的自然语言描述
 * 
 * @author claude */
@Component
public class TableTokenProcessor implements MarkdownTokenProcessor {

    private static final Logger log = LoggerFactory.getLogger(TableTokenProcessor.class);


    public TableTokenProcessor() {
    }

    @Override
    public boolean canProcess(Node node) {
        return node instanceof TableBlock;
    }

    @Override
    public ProcessedSegment process(Node node, ProcessingContext context) {
        try {
            TableBlock table = (TableBlock) node;

            // 提取表格结构化数据
            String tableStructure = extractTableStructure(table);

            // 使用大模型转换为自然语言描述
            String naturalDescription = describeTableWithLLM(tableStructure, context);

            String content = String.format("表格数据：%s", naturalDescription);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "table");
            metadata.put("structure", tableStructure);
            metadata.put("columns", extractColumnCount(table));
            metadata.put("rows", extractRowCount(table));

            return new ProcessedSegment(content, "table", metadata);

        } catch (Exception e) {
            log.error("Failed to process table node", e);
            // 回退方案：提取原始表格文本
            String rawText = node.getChars().toString();
            return new ProcessedSegment("表格内容：" + rawText, "table", null);
        }
    }

    @Override
    public int getPriority() {
        return 20; // 中等优先级
    }


    /** 提取表格结构化数据 */
    private String extractTableStructure(TableBlock table) {
        StringBuilder sb = new StringBuilder();

        // 处理表头
        TableHead head = getTableHead(table);
        if (head != null) {
            sb.append("表头：");
            for (Node row : head.getChildren()) {
                if (row instanceof TableRow) {
                    for (Node cell : row.getChildren()) {
                        if (cell instanceof TableCell) {
                            String cellText = cell.getChars().toString().trim();
                            sb.append(cellText).append(" | ");
                        }
                    }
                    break; // 只处理第一行表头
                }
            }
            sb.append("\n");
        }

        // 处理表体
        TableBody body = getTableBody(table);
        if (body != null) {
            sb.append("数据行：\n");
            for (Node row : body.getChildren()) {
                if (row instanceof TableRow) {
                    for (Node cell : row.getChildren()) {
                        if (cell instanceof TableCell) {
                            String cellText = cell.getChars().toString().trim();
                            sb.append(cellText).append(" | ");
                        }
                    }
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }

    /** 使用大模型将表格结构转换为自然语言描述 */
    private String describeTableWithLLM(String tableStructure, ProcessingContext context) {
        try {
            // 检查是否有可用的LLM配置
            if (context.getLlmConfig() == null) {
                log.warn("No LLM config available for table description, using fallback");
                return tableStructure; // 使用原始结构作为回退
            }

            ChatModel chatModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, context.getLlmConfig());

            String prompt = String.format(
                    "请将以下表格数据转换为简洁的中文自然语言描述，便于搜索和理解。" + "请突出表格的主要内容和数据特点：\n\n%s\n\n" + "请只返回描述内容，不要包含其他说明。",
                    tableStructure);

            UserMessage message = UserMessage.from(prompt);
            ChatResponse response = chatModel.chat(message);

            String description = response.aiMessage().text().trim();
            log.debug("Generated table description: {}", description);

            return description;

        } catch (Exception e) {
            log.warn("Failed to describe table with LLM: {}", e.getMessage());
            return tableStructure; // 回退方案：使用原始结构
        }
    }

    /** 获取表头 */
    private TableHead getTableHead(TableBlock table) {
        for (Node child : table.getChildren()) {
            if (child instanceof TableHead) {
                return (TableHead) child;
            }
        }
        return null;
    }

    /** 获取表体 */
    private TableBody getTableBody(TableBlock table) {
        for (Node child : table.getChildren()) {
            if (child instanceof TableBody) {
                return (TableBody) child;
            }
        }
        return null;
    }

    /** 提取列数 */
    private int extractColumnCount(TableBlock table) {
        TableHead head = getTableHead(table);
        if (head != null) {
            for (Node row : head.getChildren()) {
                if (row instanceof TableRow) {
                    int count = 0;
                    for (Node cell : row.getChildren()) {
                        if (cell instanceof TableCell) {
                            count++;
                        }
                    }
                    return count;
                }
            }
        }
        return 0;
    }

    /** 提取行数（不包括表头） */
    private int extractRowCount(TableBlock table) {
        TableBody body = getTableBody(table);
        if (body != null) {
            int count = 0;
            for (Node row : body.getChildren()) {
                if (row instanceof TableRow) {
                    count++;
                }
            }
            return count;
        }
        return 0;
    }
}