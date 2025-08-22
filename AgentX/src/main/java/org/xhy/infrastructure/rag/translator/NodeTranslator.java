package org.xhy.infrastructure.rag.translator;

import com.vladsch.flexmark.util.ast.Node;
import org.xhy.domain.rag.strategy.context.ProcessingContext;

/** 节点翻译器接口
 * 
 * 用于将特殊节点（代码块、表格、图片等）翻译为自然语言描述
 * 
 * 设计原则： - 单一职责：每个翻译器专注处理特定类型的节点 - AST直接处理：输入AST节点，输出翻译文本 - 无状态：不依赖复杂的对象状态 - 准确性：基于AST结构准确提取内容，避免字符串解析错误
 * 
 * @author claude */
public interface NodeTranslator {

    /** 判断是否可以翻译该类型的节点
     * 
     * @param node AST节点
     * @return 是否可以处理该节点 */
    boolean canTranslate(Node node);

    /** 翻译节点内容
     * 
     * @param node AST节点
     * @param context 处理上下文
     * @return 翻译后的文本内容 */
    String translate(Node node, ProcessingContext context);

    /** 获取翻译器优先级
     * 
     * 数字越小优先级越高，用于确定多个翻译器的处理顺序
     * 
     * @return 优先级数值 */
    default int getPriority() {
        return 100;
    }

    /** 获取翻译器类型标识
     * 
     * 用于日志记录和调试
     * 
     * @return 类型标识 */
    default String getType() {
        return this.getClass().getSimpleName().replace("Translator", "").toLowerCase();
    }
}