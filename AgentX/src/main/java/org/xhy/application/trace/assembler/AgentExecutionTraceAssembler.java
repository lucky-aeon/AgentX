package org.xhy.application.trace.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.trace.dto.*;
import org.xhy.domain.trace.constant.ExecutionStepType;
import org.xhy.domain.trace.model.AgentExecutionDetailEntity;
import org.xhy.domain.trace.model.AgentExecutionSummaryEntity;
import org.xhy.domain.trace.service.AgentExecutionTraceDomainService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Agent执行链路追踪数据转换器 */
public class AgentExecutionTraceAssembler {

    /** 转换执行汇总实体为DTO */
    public static AgentExecutionSummaryDTO toSummaryDTO(AgentExecutionSummaryEntity entity) {
        if (entity == null) {
            return null;
        }

        AgentExecutionSummaryDTO dto = new AgentExecutionSummaryDTO();
        BeanUtils.copyProperties(entity, dto);

        // 设置创建时间
        dto.setCreatedTime(entity.getCreatedAt());

        return dto;
    }

    /** 转换执行汇总实体列表为DTO列表 */
    public static List<AgentExecutionSummaryDTO> toSummaryDTOs(List<AgentExecutionSummaryEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream().map(AgentExecutionTraceAssembler::toSummaryDTO).collect(Collectors.toList());
    }

    /** 转换执行详情实体为DTO */
    public static AgentExecutionDetailDTO toDetailDTO(AgentExecutionDetailEntity entity) {
        if (entity == null) {
            return null;
        }

        AgentExecutionDetailDTO dto = new AgentExecutionDetailDTO();
        BeanUtils.copyProperties(entity, dto);

        return dto;
    }

    /** 转换执行详情实体列表为DTO列表 */
    public static List<AgentExecutionDetailDTO> toDetailDTOs(List<AgentExecutionDetailEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream().map(AgentExecutionTraceAssembler::toDetailDTO).collect(Collectors.toList());
    }

    /** 构建完整的执行链路DTO */
    public static ExecutionTraceDTO toExecutionTraceDTO(AgentExecutionSummaryEntity summaryEntity,
            List<AgentExecutionDetailEntity> detailEntities) {
        if (summaryEntity == null) {
            return null;
        }

        ExecutionTraceDTO traceDTO = new ExecutionTraceDTO();

        // 设置汇总信息
        traceDTO.setSummary(toSummaryDTO(summaryEntity));

        // 设置详细信息
        List<AgentExecutionDetailDTO> allDetails = toDetailDTOs(detailEntities);
        traceDTO.setDetails(allDetails);

        // 按消息类型分类
        if (detailEntities != null && !detailEntities.isEmpty()) {
            traceDTO.setUserMessages(filterByMessageType(allDetails, "USER_MESSAGE"));
            traceDTO.setAiResponses(filterByMessageType(allDetails, "AI_RESPONSE"));
            traceDTO.setToolCalls(filterByMessageType(allDetails, "TOOL_CALL"));

            // 降级调用
            List<AgentExecutionDetailDTO> fallbackCalls = allDetails.stream()
                    .filter(detail -> Boolean.TRUE.equals(detail.getIsFallbackUsed())).collect(Collectors.toList());
            traceDTO.setFallbackCalls(fallbackCalls);

            // 失败的步骤
            List<AgentExecutionDetailDTO> failedSteps = allDetails.stream()
                    .filter(detail -> Boolean.FALSE.equals(detail.getStepSuccess())).collect(Collectors.toList());
            traceDTO.setFailedSteps(failedSteps);
        }

        return traceDTO;
    }

    /** 转换执行统计信息 */
    public static ExecutionStatisticsDTO toStatisticsDTO(
            AgentExecutionTraceDomainService.ExecutionStatistics statistics) {
        if (statistics == null) {
            return null;
        }

        return new ExecutionStatisticsDTO(statistics.getTotalExecutions(), statistics.getSuccessfulExecutions(),
                statistics.getTotalTokens());
    }

    /** 按消息类型过滤详情列表 */
    private static List<AgentExecutionDetailDTO> filterByMessageType(List<AgentExecutionDetailDTO> details,
            String messageType) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyList();
        }

        return details.stream().filter(detail -> messageType.equals(detail.getMessageType()))
                .collect(Collectors.toList());
    }
}