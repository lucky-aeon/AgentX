package org.xhy.infrastructure.rag.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** 智能二次分割器
 * 
 * 基于翻译后的真实内容长度进行智能分割： 1. 优先在段落边界分割 2. 其次在句子边界分割 3. 最后强制截断分割 4. 支持重叠分割以保持上下文 */
@Component
public class MarkdownContentSplitter {

    private static final Logger log = LoggerFactory.getLogger(MarkdownContentSplitter.class);

    @Value("${rag.vector.max-length:1800}")
    private int maxVectorLength;

    @Value("${rag.vector.min-length:200}")
    private int minVectorLength;

    @Value("${rag.vector.overlap-size:100}")
    private int overlapSize;

    // 句子结束标志
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("[。！？.!?]\\s*");

    // 段落分隔符
    private static final String PARAGRAPH_SEPARATOR = "\n\n";

    /** 根据需要分割内容
     * 
     * @param content 翻译后的内容
     * @param titleContext 标题上下文（用于保持语义完整性）
     * @return 分割后的内容列表 */
    public List<String> splitIfNeeded(String content, String titleContext) {
        if (content == null || content.trim().isEmpty()) {
            return List.of();
        }

        // 添加标题上下文
        String fullContent = buildContentWithContext(content, titleContext);

        if (fullContent.length() <= maxVectorLength) {
            log.debug("Content length {} <= max length {}, no split needed", fullContent.length(), maxVectorLength);
            return List.of(fullContent);
        }

        log.debug("Content length {} > max length {}, performing smart split", fullContent.length(), maxVectorLength);

        return performSplit(fullContent, titleContext);
    }

    /** 重载方法，无标题上下文 */
    public List<String> splitIfNeeded(String content) {
        return splitIfNeeded(content, null);
    }

    /** 构建带上下文的完整内容 */
    private String buildContentWithContext(String content, String titleContext) {
        if (titleContext == null || titleContext.trim().isEmpty()) {
            return content;
        }

        // 如果内容已经包含标题上下文，不重复添加
        if (content.trim().startsWith(titleContext.trim())) {
            return content;
        }

        return titleContext + "\n\n" + content;
    }

    /** 执行分割 */
    private List<String> performSplit(String fullContent, String titleContext) {
        List<String> chunks = new ArrayList<>();

        try {
            // 策略1：段落级分割
            List<String> paragraphChunks = splitByParagraphs(fullContent, titleContext);

            if (isValidSplit(paragraphChunks)) {
                log.debug("Successfully split by paragraphs into {} chunks", paragraphChunks.size());
                return paragraphChunks;
            }

            // 策略2：句子级分割
            List<String> sentenceChunks = splitBySentences(fullContent, titleContext);

            if (isValidSplit(sentenceChunks)) {
                log.debug("Successfully split by sentences into {} chunks", sentenceChunks.size());
                return sentenceChunks;
            }

            // 策略3：强制分割
            List<String> forceChunks = forceSplit(fullContent, titleContext);
            log.debug("Applied force split into {} chunks", forceChunks.size());
            return forceChunks;

        } catch (Exception e) {
            log.error("Error during smart split, falling back to force split", e);
            return forceSplit(fullContent, titleContext);
        }
    }

    /** 按段落分割 */
    private List<String> splitByParagraphs(String content, String titleContext) {
        String[] paragraphs = content.split(PARAGRAPH_SEPARATOR);
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        String titlePrefix = buildTitlePrefix(titleContext);
        int titlePrefixLength = titlePrefix.length();
        int availableLength = maxVectorLength - titlePrefixLength - overlapSize;

        for (String paragraph : paragraphs) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isEmpty()) {
                continue;
            }

            // 计算添加当前段落后的长度
            int paragraphLength = trimmedParagraph.length() + 2; // +2 for "\n\n"
            int newLength = currentChunk.length() + paragraphLength;

            if (currentChunk.length() == 0) {
                // 第一个段落，直接添加
                currentChunk.append(trimmedParagraph);
            } else if (newLength <= availableLength) {
                // 可以添加到当前块
                currentChunk.append(PARAGRAPH_SEPARATOR).append(trimmedParagraph);
            } else {
                // 当前块已满，保存并开始新块
                if (currentChunk.length() > 0) {
                    String chunkContent = titlePrefix + currentChunk.toString();
                    chunks.add(chunkContent);

                    // 准备下一个块，考虑重叠
                    currentChunk.setLength(0);
                    String overlap = extractOverlap(trimmedParagraph);
                    if (!overlap.isEmpty()) {
                        currentChunk.append(overlap).append(PARAGRAPH_SEPARATOR);
                    }
                }

                // 处理当前段落
                if (paragraphLength <= availableLength) {
                    currentChunk.append(trimmedParagraph);
                } else {
                    // 单个段落超长，需要进一步分割
                    List<String> subChunks = splitLongParagraph(trimmedParagraph, titlePrefix, availableLength);
                    chunks.addAll(subChunks);
                }
            }
        }

        // 添加最后一个块
        if (currentChunk.length() > 0) {
            String chunkContent = titlePrefix + currentChunk.toString();
            chunks.add(chunkContent);
        }

        return chunks;
    }

    /** 按句子分割 */
    private List<String> splitBySentences(String content, String titleContext) {
        List<String> sentences = extractSentences(content);
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        String titlePrefix = buildTitlePrefix(titleContext);
        int titlePrefixLength = titlePrefix.length();
        int availableLength = maxVectorLength - titlePrefixLength - overlapSize;

        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.isEmpty()) {
                continue;
            }

            int sentenceLength = trimmedSentence.length();
            int newLength = currentChunk.length() + sentenceLength;

            if (currentChunk.length() == 0) {
                currentChunk.append(trimmedSentence);
            } else if (newLength <= availableLength) {
                currentChunk.append(" ").append(trimmedSentence);
            } else {
                // 当前块已满
                if (currentChunk.length() > 0) {
                    String chunkContent = titlePrefix + currentChunk.toString();
                    chunks.add(chunkContent);

                    // 准备下一个块，考虑重叠
                    currentChunk.setLength(0);
                    String overlap = extractOverlap(currentChunk.toString());
                    if (!overlap.isEmpty()) {
                        currentChunk.append(overlap).append(" ");
                    }
                }

                // 处理当前句子
                if (sentenceLength <= availableLength) {
                    currentChunk.append(trimmedSentence);
                } else {
                    // 单个句子超长，强制截断
                    String truncated = trimmedSentence.substring(0,
                            Math.min(availableLength, trimmedSentence.length()));
                    String chunkContent = titlePrefix + truncated;
                    chunks.add(chunkContent);
                }
            }
        }

        // 添加最后一个块
        if (currentChunk.length() > 0) {
            String chunkContent = titlePrefix + currentChunk.toString();
            chunks.add(chunkContent);
        }

        return chunks;
    }

    /** 强制分割 */
    private List<String> forceSplit(String content, String titleContext) {
        List<String> chunks = new ArrayList<>();
        String titlePrefix = buildTitlePrefix(titleContext);
        int titlePrefixLength = titlePrefix.length();
        int availableLength = maxVectorLength - titlePrefixLength;

        if (availableLength <= minVectorLength) {
            // 标题太长，直接截断
            String truncated = content.substring(0, Math.min(content.length(), maxVectorLength));
            chunks.add(truncated);
            return chunks;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + availableLength, content.length());
            String chunk = content.substring(start, end);
            String chunkContent = titlePrefix + chunk;
            chunks.add(chunkContent);

            // 考虑重叠
            start = end - overlapSize;
            if (start <= 0) {
                start = end;
            }
        }

        return chunks;
    }

    /** 分割超长段落 */
    private List<String> splitLongParagraph(String paragraph, String titlePrefix, int availableLength) {
        List<String> chunks = new ArrayList<>();

        // 尝试按句子分割
        List<String> sentences = extractSentences(paragraph);
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.isEmpty()) {
                continue;
            }

            int newLength = currentChunk.length() + trimmedSentence.length();

            if (currentChunk.length() == 0) {
                currentChunk.append(trimmedSentence);
            } else if (newLength <= availableLength) {
                currentChunk.append(" ").append(trimmedSentence);
            } else {
                // 保存当前块
                if (currentChunk.length() > 0) {
                    chunks.add(titlePrefix + currentChunk.toString());
                    currentChunk.setLength(0);
                }

                // 处理当前句子
                if (trimmedSentence.length() <= availableLength) {
                    currentChunk.append(trimmedSentence);
                } else {
                    // 句子仍然超长，强制截断
                    int start = 0;
                    while (start < trimmedSentence.length()) {
                        int end = Math.min(start + availableLength, trimmedSentence.length());
                        String subChunk = trimmedSentence.substring(start, end);
                        chunks.add(titlePrefix + subChunk);
                        start = end;
                    }
                }
            }
        }

        // 添加最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(titlePrefix + currentChunk.toString());
        }

        return chunks;
    }

    /** 提取句子 */
    private List<String> extractSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] parts = SENTENCE_END_PATTERN.split(text);

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }

        return sentences;
    }

    /** 构建标题前缀 */
    private String buildTitlePrefix(String titleContext) {
        if (titleContext == null || titleContext.trim().isEmpty()) {
            return "";
        }

        return titleContext.trim() + "\n\n";
    }

    /** 提取重叠内容 */
    private String extractOverlap(String text) {
        if (text.length() <= overlapSize) {
            return text;
        }

        // 从末尾提取重叠内容，尽量在词边界截断
        String overlap = text.substring(Math.max(0, text.length() - overlapSize));

        // 寻找最近的空格或标点，避免截断单词
        int spaceIndex = overlap.indexOf(' ');
        if (spaceIndex > 0 && spaceIndex < overlap.length() - 10) {
            return overlap.substring(spaceIndex + 1);
        }

        return overlap;
    }

    /** 验证分割结果是否合理 */
    private boolean isValidSplit(List<String> chunks) {
        if (chunks.isEmpty()) {
            return false;
        }

        // 检查每个块的长度
        for (String chunk : chunks) {
            if (chunk.length() > maxVectorLength) {
                log.debug("Chunk too long: {} > {}", chunk.length(), maxVectorLength);
                return false;
            }
            if (chunk.length() < minVectorLength && chunks.size() > 1) {
                log.debug("Chunk too short: {} < {} (and not the only chunk)", chunk.length(), minVectorLength);
                return false;
            }
        }

        return true;
    }

    /** 获取分割统计信息（用于调试） */
    public String getSplitStatistics(String originalContent, List<String> chunks) {
        if (chunks.isEmpty()) {
            return "No chunks generated";
        }

        int totalLength = chunks.stream().mapToInt(String::length).sum();
        int avgLength = totalLength / chunks.size();
        int minLength = chunks.stream().mapToInt(String::length).min().orElse(0);
        int maxLength = chunks.stream().mapToInt(String::length).max().orElse(0);

        return String.format("Split %d chars into %d chunks. Avg: %d, Min: %d, Max: %d", originalContent.length(),
                chunks.size(), avgLength, minLength, maxLength);
    }
}