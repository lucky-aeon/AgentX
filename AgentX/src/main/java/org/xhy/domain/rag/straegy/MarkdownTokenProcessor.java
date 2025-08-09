package org.xhy.domain.rag.straegy;

import com.vladsch.flexmark.util.ast.Node;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.context.ProcessingContext;

/** Markdown Token处理器接口
 * 
 * @author claude */
public interface MarkdownTokenProcessor {

    /** 判断是否可以处理该节点
     *
     * @param node AST节点
     * @return 是否可以处理 */
    boolean canProcess(Node node);

    /** 处理节点，生成处理后的段落
     *
     * @param node AST节点
     * @param context 处理上下文
     * @return 处理后的段落，如果无法处理返回null */
    ProcessedSegment process(Node node, ProcessingContext context);

    /** 获取处理器优先级，数字越小优先级越高
     *
     * @return 优先级数值 */
    default int getPriority() {
        return 100;
    }

}