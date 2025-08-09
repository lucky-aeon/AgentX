package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.util.ast.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.StructuralTokenProcessor;

import java.util.HashMap;
import java.util.Map;

/** 结构化表格处理器 职责：仅识别表格结构，提取表头、行数、列数等基础信息，不进行大模型处理
 * 
 * @author claude */
@Component
public class StructuralTableProcessor implements StructuralTokenProcessor {

    private static final Logger log = LoggerFactory.getLogger(StructuralTableProcessor.class);

    @Override
    public boolean canProcess(Node node) {
        return node instanceof TableBlock;
    }

    @Override
    public ProcessedSegment parseStructure(Node node) {
        try {
            TableBlock table = (TableBlock) node;

            // 提取表格结构化信息
            String tableStructure = extractTableStructure(table);
            int columnCount = extractColumnCount(table);
            int rowCount = extractRowCount(table);

            // 构建结构化元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "table");
            metadata.put("columns", columnCount);
            metadata.put("rows", rowCount);
            metadata.put("raw_content", node.getChars().toString());
            metadata.put("structure", tableStructure);

            // ✅ 关键：保留原始表格内容和结构化信息，不进行智能处理
            String displayContent = String.format("表格（%d行 x %d列）：\n%s", rowCount, columnCount, tableStructure);

            ProcessedSegment segment = new ProcessedSegment(displayContent, "table", metadata);

            log.debug("Parsed table structure: columns={}, rows={}", columnCount, rowCount);

            return segment;

        } catch (Exception e) {
            log.error("Failed to parse table structure", e);
            // 回退方案：返回原始表格文本
            String rawText = node.getChars().toString();
            Map<String, Object> fallbackMetadata = new HashMap<>();
            fallbackMetadata.put("type", "table");
            fallbackMetadata.put("raw_content", rawText);
            fallbackMetadata.put("parse_error", e.getMessage());

            return new ProcessedSegment("表格内容：" + rawText, "table", fallbackMetadata);
        }
    }

    @Override
    public int getPriority() {
        return 20; // 中等优先级，与原TableTokenProcessor保持一致
    }

    @Override
    public String getType() {
        return "table";
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

    /** 提取行数 */
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