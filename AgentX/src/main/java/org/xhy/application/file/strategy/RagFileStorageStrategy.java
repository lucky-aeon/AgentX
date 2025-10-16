package org.xhy.application.file.strategy;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.xhy.domain.file.constant.FileTypeEnum;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
import org.xhy.domain.rag.constant.MetadataConstant;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

/** RAG 文件存储策略
 * 
 * 处理RAG文档的完整业务流程，包括文档单元创建、向量存储等
 * 
 * @author shilong.zang
 * @date 2024-12-09 */
@Component
public class RagFileStorageStrategy implements FileStorageStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileDetailRepository fileDetailRepository;
    private final DocumentUnitRepository documentUnitRepository;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public RagFileStorageStrategy(FileDetailRepository fileDetailRepository,
            DocumentUnitRepository documentUnitRepository,
            @Qualifier("initEmbeddingStore") EmbeddingStore<TextSegment> embeddingStore) {
        this.fileDetailRepository = fileDetailRepository;
        this.documentUnitRepository = documentUnitRepository;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public boolean save(FileInfo fileInfo) {
        try {
            FileDetailEntity fileDetailEntity = convertToFileDetailEntity(fileInfo);
            fileDetailRepository.checkInsert(fileDetailEntity);

            // 回写ID到原对象（第三方库需要）
            fileInfo.setId(fileDetailEntity.getId());

            return true;
        } catch (Exception e) {
            throw new RuntimeException("保存RAG文件失败", e);
        }
    }

    @Override
    public void update(FileInfo fileInfo) {
        try {
            FileDetailEntity fileDetailEntity = convertToFileDetailEntity(fileInfo);

            LambdaQueryWrapper<FileDetailEntity> queryWrapper = new LambdaQueryWrapper<FileDetailEntity>()
                    .eq(fileDetailEntity.getUrl() != null, FileDetailEntity::getUrl, fileDetailEntity.getUrl())
                    .eq(fileDetailEntity.getId() != null, FileDetailEntity::getId, fileDetailEntity.getId());

            fileDetailRepository.checkedUpdate(fileDetailEntity, queryWrapper);
        } catch (Exception e) {
            throw new RuntimeException("更新RAG文件失败", e);
        }
    }

    @Override
    public FileInfo getByUrl(String url) {
        try {
            FileDetailEntity fileDetailEntity = fileDetailRepository
                    .selectOne(Wrappers.<FileDetailEntity>lambdaQuery().eq(FileDetailEntity::getUrl, url));

            if (fileDetailEntity == null) {
                return null;
            }

            return convertToFileInfo(fileDetailEntity);
        } catch (Exception e) {
            throw new RuntimeException("查询RAG文件失败", e);
        }
    }

    @Override
    public boolean delete(String url) {
        FileDetailEntity fileDetailEntity = fileDetailRepository
                .selectOne(Wrappers.lambdaQuery(FileDetailEntity.class).eq(FileDetailEntity::getUrl, url));

        if (fileDetailEntity == null) {
            return false;
        }

        // RAG文件删除：需要清理关联的业务数据

        // 1. 删除文件记录
        fileDetailRepository.deleteById(fileDetailEntity.getId());

        // 2. 删除关联的文档单元数据
        documentUnitRepository.delete(Wrappers.lambdaQuery(DocumentUnitEntity.class).eq(DocumentUnitEntity::getFileId,
                fileDetailEntity.getId()));

        // 3. 删除向量存储中的数据
        embeddingStore.removeAll(metadataKey(MetadataConstant.FILE_ID).isIn(fileDetailEntity.getId()));

        return true;
    }

    /** 将FileInfo转换为FileDetailEntity */
    private FileDetailEntity convertToFileDetailEntity(FileInfo fileInfo) throws JsonProcessingException {
        FileDetailEntity fileDetailEntity = BeanUtil.copyProperties(fileInfo, FileDetailEntity.class, "metadata",
                "userMetadata", "thMetadata", "thUserMetadata", "attr", "hashInfo");

        // 设置RAG相关的业务信息
        if (fileInfo.getMetadata() != null) {
            fileDetailEntity.setDataSetId(fileInfo.getMetadata().get("dataset"));
            fileDetailEntity.setUserId(fileInfo.getMetadata().get("userid"));
        }

        // 转换元数据为JSON字符串
        fileDetailEntity.setMetadata(valueToJson(fileInfo.getMetadata()));
        fileDetailEntity.setUserMetadata(valueToJson(fileInfo.getUserMetadata()));
        fileDetailEntity.setThMetadata(valueToJson(fileInfo.getThMetadata()));
        fileDetailEntity.setThUserMetadata(valueToJson(fileInfo.getThUserMetadata()));

        // 转换附加属性和哈希信息
        fileDetailEntity.setAttr(valueToJson(fileInfo.getAttr()));
        fileDetailEntity.setHashInfo(valueToJson(fileInfo.getHashInfo()));

        // RAG文件设置特定的处理状态
        fileDetailEntity.setProcessingStatus(FileProcessingStatusEnum.UPLOADED.getCode());

        return fileDetailEntity;
    }

    /** 将FileDetailEntity转换为FileInfo */
    private FileInfo convertToFileInfo(FileDetailEntity fileDetailEntity) throws JsonProcessingException {
        FileInfo fileInfo = BeanUtil.copyProperties(fileDetailEntity, FileInfo.class, "metadata", "userMetadata",
                "thMetadata", "thUserMetadata", "attr", "hashInfo");

        // 转换JSON字符串为对象
        fileInfo.setMetadata(jsonToMetadata(fileDetailEntity.getMetadata()));
        fileInfo.setUserMetadata(jsonToMetadata(fileDetailEntity.getUserMetadata()));
        fileInfo.setThMetadata(jsonToMetadata(fileDetailEntity.getThMetadata()));
        fileInfo.setThUserMetadata(jsonToMetadata(fileDetailEntity.getThUserMetadata()));
        fileInfo.setAttr(jsonToDict(fileDetailEntity.getAttr()));

        // 哈希信息处理
        if (StrUtil.isNotBlank(fileDetailEntity.getHashInfo())) {
            fileInfo.setHashInfo(objectMapper.readValue(fileDetailEntity.getHashInfo(),
                    org.dromara.x.file.storage.core.hash.HashInfo.class));
        }

        return fileInfo;
    }

    /** 将对象转换为JSON字符串 */
    private String valueToJson(Object value) throws JsonProcessingException {
        if (value == null) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    /** 将JSON字符串转换为元数据Map */
    private java.util.Map<String, String> jsonToMetadata(String json) throws JsonProcessingException {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    /** 将JSON字符串转换为字典对象 */
    private Dict jsonToDict(String json) throws JsonProcessingException {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return objectMapper.readValue(json, Dict.class);
    }
}
