package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Component;

import java.util.List;

/** Markdown AST 解析器
 * 
 * 职责： - 配置和管理Flexmark Parser - 将Markdown文本解析为AST - 提供统一的解析接口 */
@Component
public class MarkdownAstParser {

    private final Parser parser;

    public MarkdownAstParser() {
        this.parser = configureParser();
    }

    /** 解析Markdown文本为AST
     * 
     * @param markdown Markdown文本
     * @return AST根节点 */
    public Node parse(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            // 返回空文档节点
            return parser.parse("");
        }
        return parser.parse(markdown);
    }

    /** 配置Flexmark解析器
     * 
     * @return 配置好的Parser实例 */
    private Parser configureParser() {
        MutableDataSet options = new MutableDataSet();

        // 启用表格扩展
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));

        return Parser.builder(options).build();
    }

    /** 获取Parser实例（用于高级使用场景）
     * 
     * @return Parser实例 */
    public Parser getParser() {
        return parser;
    }
}