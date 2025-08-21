package org.xhy.infrastructure.rag.translator;

import com.vladsch.flexmark.util.ast.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.domain.rag.translator.NodeTranslator;

import java.util.Comparator;
import java.util.List;

/** 节点翻译服务
 * 
 * 负责协调所有的 NodeTranslator 实现，选择合适的翻译器处理特定类型的节点
 * 
 * @author claude */
@Service
public class NodeTranslatorService {

    private static final Logger log = LoggerFactory.getLogger(NodeTranslatorService.class);

    private final List<NodeTranslator> translators;

    public NodeTranslatorService(List<NodeTranslator> translators) {
        // 按优先级排序翻译器
        this.translators = translators.stream().sorted(Comparator.comparingInt(NodeTranslator::getPriority)).toList();

        log.info("NodeTranslatorService initialized with {} translators: {}", translators.size(),
                this.translators.stream().map(NodeTranslator::getType).toList());
    }

    /** 翻译节点内容
     * 
     * @param node AST节点
     * @param context 处理上下文
     * @return 翻译后的内容，如果没有合适的翻译器则返回原内容 */
    public String translate(Node node, ProcessingContext context) {
        if (node == null) {
            return "";
        }

        String originalContent = node.getChars().toString();

        // 查找第一个可以处理该类型节点的翻译器
        for (NodeTranslator translator : translators) {
            try {
                if (translator.canTranslate(node)) {
                    log.debug("Using {} to translate {} node", translator.getType(), node.getClass().getSimpleName());

                    String translatedContent = translator.translate(node, context);

                    if (translatedContent != null && !translatedContent.equals(originalContent)) {
                        log.debug("Successfully translated {} node: {} chars -> {} chars",
                                node.getClass().getSimpleName(), originalContent.length(), translatedContent.length());
                        return translatedContent;
                    }
                }
            } catch (Exception e) {
                log.warn("Translator {} failed to process {} node: {}", translator.getType(),
                        node.getClass().getSimpleName(), e.getMessage());
                // 继续尝试下一个翻译器
            }
        }

        // 没有找到合适的翻译器，返回原内容
        log.debug("No translator found for {} node, returning original content", node.getClass().getSimpleName());
        return originalContent;
    }

    /** 检查是否有翻译器可以处理指定类型的节点
     * 
     * @param node AST节点
     * @return 是否有翻译器可以处理 */
    public boolean canTranslate(Node node) {
        return translators.stream().anyMatch(translator -> {
            try {
                return translator.canTranslate(node);
            } catch (Exception e) {
                log.warn("Error checking translator capability: {}", e.getMessage());
                return false;
            }
        });
    }

    /** 获取所有已注册的翻译器信息
     * 
     * @return 翻译器信息列表 */
    public List<String> getTranslatorInfo() {
        return translators.stream()
                .map(translator -> String.format("%s (priority: %d)", translator.getType(), translator.getPriority()))
                .toList();
    }
}