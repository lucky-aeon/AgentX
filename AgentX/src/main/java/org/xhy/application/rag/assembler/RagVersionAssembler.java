package org.xhy.application.rag.assembler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.xhy.application.rag.dto.RagMarketDTO;
import org.xhy.application.rag.dto.RagVersionDTO;
import org.xhy.domain.rag.constant.RagPublishStatus;
import org.xhy.domain.rag.model.RagVersionEntity;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** RAG版本Assembler
 * @author xhy
 * @date 2025-07-16 <br/>
 */
public class RagVersionAssembler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Convert Entity to DTO using BeanUtils */
    public static RagVersionDTO toDTO(RagVersionEntity entity) {
        if (entity == null) {
            return null;
        }

        RagVersionDTO dto = new RagVersionDTO();
        BeanUtils.copyProperties(entity, dto);

        // 处理状态描述
        RagPublishStatus status = RagPublishStatus.fromCode(entity.getPublishStatus());
        if (status != null) {
            dto.setPublishStatusDesc(status.getDescription());
        }

        // 处理标签JSON
        if (StringUtils.isNotBlank(entity.getLabels())) {
            try {
                List<String> labels = objectMapper.readValue(entity.getLabels(), new TypeReference<List<String>>() {
                });
                dto.setLabels(labels);
            } catch (Exception e) {
                dto.setLabels(Collections.emptyList());
            }
        } else {
            dto.setLabels(Collections.emptyList());
        }

        return dto;
    }

    /** Convert Entity list to DTO list */
    public static List<RagVersionDTO> toDTOs(List<RagVersionEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(RagVersionAssembler::toDTO).collect(Collectors.toList());
    }

    /** Convert Entity to MarketDTO for market display */
    public static RagMarketDTO toMarketDTO(RagVersionEntity entity) {
        if (entity == null) {
            return null;
        }

        RagMarketDTO dto = new RagMarketDTO();
        BeanUtils.copyProperties(entity, dto);

        // 处理标签JSON
        if (StringUtils.isNotBlank(entity.getLabels())) {
            try {
                List<String> labels = objectMapper.readValue(entity.getLabels(), new TypeReference<List<String>>() {
                });
                dto.setLabels(labels);
            } catch (Exception e) {
                dto.setLabels(Collections.emptyList());
            }
        } else {
            dto.setLabels(Collections.emptyList());
        }

        // 格式化文件大小显示
        dto.setTotalSizeDisplay(formatFileSize(entity.getTotalSize()));

        return dto;
    }

    /** Convert Entity list to MarketDTO list */
    public static List<RagMarketDTO> toMarketDTOs(List<RagVersionEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(RagVersionAssembler::toMarketDTO).collect(Collectors.toList());
    }

    /** 格式化文件大小 */
    private static String formatFileSize(Long size) {
        if (size == null || size == 0) {
            return "0 B";
        }

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        DecimalFormat df = new DecimalFormat("#,##0.#");
        return df.format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}