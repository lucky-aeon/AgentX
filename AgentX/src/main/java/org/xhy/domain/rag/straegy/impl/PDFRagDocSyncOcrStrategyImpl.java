package org.xhy.domain.rag.straegy.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.model.chat.ChatModel;
import org.dromara.streamquery.stream.core.bean.BeanHelper;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.constant.RAGSystemPrompt;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;
import org.xhy.infrastructure.rag.detector.TikaFileTypeDetector;
import org.xhy.infrastructure.rag.utils.PdfToBase64Converter;

import cn.hutool.core.codec.Base64;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;

/** @author shilong.zang
 * @date 10:20 <br/>
 */
@Service(value = "ragDocSyncOcr-PDF")
public class PDFRagDocSyncOcrStrategyImpl extends RagDocSyncOcrStrategyImpl implements RAGSystemPrompt {

    private static final Logger log = LoggerFactory.getLogger(PDFRagDocSyncOcrStrategyImpl.class);

    private final DocumentUnitRepository documentUnitRepository;

    private final FileDetailRepository fileDetailRepository;

    @Resource
    private FileStorageService fileStorageService;

    // 用于存储当前处理的文件ID，以便更新进度
    private String currentProcessingFileId;

    public PDFRagDocSyncOcrStrategyImpl(DocumentUnitRepository documentUnitRepository,
            FileDetailRepository fileDetailRepository) {
        this.documentUnitRepository = documentUnitRepository;
        this.fileDetailRepository = fileDetailRepository;
    }

    /** 处理消息，增加进度更新功能
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy 当前策略 */
    @Override
    public void handle(RagDocSyncOcrMessage ragDocSyncOcrMessage, String strategy) throws Exception {
        // 设置当前处理的文件ID，用于进度更新
        this.currentProcessingFileId = ragDocSyncOcrMessage.getFileId();

        // 调用父类处理逻辑
        super.handle(ragDocSyncOcrMessage, strategy);
    }

    /** 获取文件页数 */
    @Override
    public void pushPageSize(byte[] bytes, RagDocSyncOcrMessage ragDocSyncOcrMessage) {

        try {
            final int pdfPageCount = PdfToBase64Converter.getPdfPageCount(bytes);
            ragDocSyncOcrMessage.setPageSize(pdfPageCount);

            // 更新数据库中的总页数
            if (currentProcessingFileId != null) {
                LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                        .eq(FileDetailEntity::getId, currentProcessingFileId)
                        .set(FileDetailEntity::getFilePageSize, pdfPageCount);
                fileDetailRepository.update(wrapper);

                log.info("Updated total pages for file {}: {} pages", currentProcessingFileId, pdfPageCount);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /** 获取文件
     *
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy 当前策略 */
    @Override
    public byte[] getFileData(RagDocSyncOcrMessage ragDocSyncOcrMessage, String strategy) {

        final FileDetailEntity fileDetailEntity = fileDetailRepository.selectById(ragDocSyncOcrMessage.getFileId());

        return fileStorageService.download(fileDetailEntity.getUrl()).bytes();
    }

    /** 处理PDF文件 - 按页处理逻辑 */
    @Override
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages) {
        return processFile(fileBytes, totalPages, null);
    }

    /** 处理PDF文件 - 按页处理逻辑（带消息参数） */
    @Override
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages,
            RagDocSyncOcrMessage ragDocSyncOcrMessage) {

        final HashMap<Integer, String> ocrData = new HashMap<>();
        for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
            try {
                // 单独处理每一页以减少内存使用
                String base64 = PdfToBase64Converter.processPdfPageToBase64(fileBytes, pageIndex, "jpg");

                final UserMessage userMessage = UserMessage.userMessage(
                        ImageContent.from(base64, TikaFileTypeDetector.detectFileType(Base64.decode(base64))),
                        TextContent.from(OCR_PROMPT));

                /** 创建OCR处理的模型配置 - 从消息中获取用户配置的OCR模型 */
                ChatModel ocrModel = createOcrModelFromMessage(ragDocSyncOcrMessage);

                final ChatResponse chat = ocrModel.chat(userMessage);

                ocrData.put(pageIndex, processText(chat.aiMessage().text()));

                // 实时更新处理进度
                updateProcessProgress(pageIndex + 1, totalPages);

                log.info("Processing request page {}/{}, current memory usage: {} MB", (pageIndex + 1), totalPages,
                        (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024));

                if ((pageIndex + 1) % 10 == 0) {
                    System.gc();
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                log.info("Page {} processing completed", (pageIndex + 1));
            } catch (Exception e) {
                log.error("Error processing PDF page {}: {}", (pageIndex + 1), e.getMessage());
                // 继续处理下一页，不中断整个流程
            }
        }

        return ocrData;

    }

    /** 保存数据
     *
     * @param ragDocSyncOcrMessage 消息数据
     * @param ocrData ocr数据 */
    @Override
    public void insertData(RagDocSyncOcrMessage ragDocSyncOcrMessage, Map<Integer, String> ocrData) {

        for (int pageIndex = 0; pageIndex < ragDocSyncOcrMessage.getPageSize(); pageIndex++) {

            String content = ocrData.getOrDefault(pageIndex, null);

            final DocumentUnitEntity documentUnitDO = new DocumentUnitEntity();

            documentUnitDO.setContent(content);
            documentUnitDO.setPage(pageIndex);
            documentUnitDO.setFileId(ragDocSyncOcrMessage.getFileId());
            documentUnitDO.setIsVector(false);
            documentUnitDO.setIsOcr(true);

            if (content == null) {
                documentUnitDO.setIsOcr(false);
            }

            documentUnitRepository.checkInsert(documentUnitDO);

        }
    }

    private static final Pattern[] PATTERNS = {Pattern.compile("\\\\（"), Pattern.compile("\\\\）"),
            Pattern.compile("\n{3,}"), Pattern.compile("([^\n])\n([^\n])"), Pattern.compile("\\$\\s+"),
            Pattern.compile("\\s+\\$"), Pattern.compile("\\$\\$")};

    public String processText(String input) {
        String result = input;
        result = PATTERNS[0].matcher(result).replaceAll(Matcher.quoteReplacement("\\("));
        result = PATTERNS[1].matcher(result).replaceAll(Matcher.quoteReplacement("\\)"));
        result = PATTERNS[2].matcher(result).replaceAll("\n\n");
        result = PATTERNS[3].matcher(result).replaceAll("$1\n$2");
        result = PATTERNS[4].matcher(result).replaceAll(Matcher.quoteReplacement("$"));
        result = PATTERNS[5].matcher(result).replaceAll(Matcher.quoteReplacement("$"));
        result = PATTERNS[6].matcher(result).replaceAll(Matcher.quoteReplacement("$$"));
        return result.trim();
    }

    /** 更新处理进度
     * @param currentPage 当前页数
     * @param totalPages 总页数 */
    private void updateProcessProgress(int currentPage, int totalPages) {
        if (currentProcessingFileId == null) {
            return;
        }

        try {
            double progress = (double) currentPage / totalPages * 100.0;

            // 使用新的OCR专用进度字段
            LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                    .eq(FileDetailEntity::getId, currentProcessingFileId)
                    .set(FileDetailEntity::getCurrentOcrPageNumber, currentPage)
                    .set(FileDetailEntity::getOcrProcessProgress, progress);

            fileDetailRepository.update(wrapper);

            log.debug("Updated OCR progress for file {}: {}/{} pages ({}%)", currentProcessingFileId, currentPage,
                    totalPages, String.format("%.1f", progress));
        } catch (Exception e) {
            log.warn("Failed to update OCR progress for file {}: {}", currentProcessingFileId, e.getMessage());
        }
    }

    /** 从消息中创建OCR模型
     * 
     * @param ragDocSyncOcrMessage OCR消息
     * @return ChatModel实例
     * @throws RuntimeException 如果没有配置OCR模型或创建失败 */
    private ChatModel createOcrModelFromMessage(RagDocSyncOcrMessage ragDocSyncOcrMessage) {
        // 检查消息和模型配置是否存在
        if (ragDocSyncOcrMessage == null || ragDocSyncOcrMessage.getOcrModelConfig() == null) {
            String errorMsg = String.format("用户 %s 未配置OCR模型，无法进行文档OCR处理",
                    ragDocSyncOcrMessage != null ? ragDocSyncOcrMessage.getUserId() : "unknown");
            log.error(errorMsg);
            throw new BusinessException(errorMsg);
        }

        try {
            var modelConfig = ragDocSyncOcrMessage.getOcrModelConfig();

            // 验证模型配置的完整性
            if (modelConfig.getModelId() == null || modelConfig.getApiKey() == null
                    || modelConfig.getBaseUrl() == null) {
                String errorMsg = String.format("用户 %s 的OCR模型配置不完整: modelId=%s, apiKey=%s, baseUrl=%s",
                        ragDocSyncOcrMessage.getUserId(), modelConfig.getModelId(),
                        modelConfig.getApiKey() != null ? "已配置" : "未配置", modelConfig.getBaseUrl());
                log.error(errorMsg);
                throw new BusinessException(errorMsg);
            }

            ProviderConfig ocrProviderConfig = new ProviderConfig(modelConfig.getApiKey(), modelConfig.getBaseUrl(),
                    modelConfig.getModelId(), ProviderProtocol.OPENAI);

            ChatModel ocrModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, ocrProviderConfig);

            log.info("Successfully created OCR model for user {}: {}", ragDocSyncOcrMessage.getUserId(),
                    modelConfig.getModelId());
            return ocrModel;

        } catch (RuntimeException e) {
            // 重新抛出已知的业务异常
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("用户 %s 创建OCR模型失败: %s", ragDocSyncOcrMessage.getUserId(), e.getMessage());
            log.error(errorMsg, e);
            throw new BusinessException(errorMsg, e);
        }
    }
}
