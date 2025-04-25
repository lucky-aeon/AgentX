package org.xhy.application.rag.assembler;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.xhy.domain.agent.constant.AgentType;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.interfaces.dto.agent.CreateAgentRequest;
import org.xhy.interfaces.dto.rag.RagUploadRequest;

/**
 * @author shilong.zang
 * @date 11:09 <br/>
 */
public class FileAssembler {

    /**
     * 将CreateAgentRequest转换为AgentEntity
     */
    public static FileDetailEntity toEntity(RagUploadRequest request) {

        FileDetailEntity fileDetailEntity = new FileDetailEntity();
        fileDetailEntity.setMultipartFile(request.getFile());
        fileDetailEntity.setDataSetId(request.getDataSetId());

        return fileDetailEntity;
    }
}
