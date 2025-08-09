package org.xhy.domain.rag.straegy;

import com.vladsch.flexmark.util.ast.Node;
import org.xhy.domain.rag.model.ProcessedSegment;

/** 结构化Token处理器接口 职责：仅负责识别和解析Markdown结构，不进行内容智能处理 与MarkdownTokenProcessor的区别： - StructuralTokenProcessor:
 * 纯结构解析，无大模型依赖，便于测试 - MarkdownTokenProcessor: 包含内容智能处理，依赖大模型
 * 
 * @author claude */
public interface StructuralTokenProcessor {

    /** 判断是否可以处理该节点
     *
     * @param node AST节点
     * @return 是否可以处理 */
    boolean canProcess(Node node);

    /** 解析节点结构，提取基础信息但不进行内容处理 只做结构化分析：识别类型、提取元数据、保留原始内容
     *
     * @param node AST节点
     * @return 解析后的段落，包含原始内容和结构化元数据 */
    ProcessedSegment parseStructure(Node node);

    /** 获取处理器优先级，数字越小优先级越高
     *
     * @return 优先级数值 */
    default int getPriority() {
        return 100;
    }

    /** 获取处理器类型标识 用于区分不同类型的结构化处理器
     *
     * @return 类型标识 */
    default String getType() {
        return this.getClass().getSimpleName().replace("Structural", "").replace("Processor", "").toLowerCase();
    }
}