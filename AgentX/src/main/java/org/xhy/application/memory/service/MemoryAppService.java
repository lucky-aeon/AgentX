package org.xhy.application.memory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.xhy.application.memory.assembler.MemoryAssembler;
import org.xhy.application.memory.dto.MemoryItemDTO;
import org.xhy.domain.memory.model.CandidateMemory;
import org.xhy.domain.memory.model.MemoryItemEntity;
import org.xhy.domain.memory.service.MemoryDomainService;
import org.xhy.interfaces.dto.memory.CreateMemoryRequest;
import org.xhy.interfaces.dto.memory.QueryMemoryRequest;

import java.util.ArrayList;
import java.util.List;

@Service
public class MemoryAppService {

    private final MemoryDomainService memoryDomainService;

    public MemoryAppService(MemoryDomainService memoryDomainService) {
        this.memoryDomainService = memoryDomainService;
    }

    /** 分页列出用户记忆 */
    public Page<MemoryItemDTO> listUserMemories(String userId, QueryMemoryRequest request) {
        int pageNo = request.getPage() != null ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        Page<MemoryItemEntity> page = memoryDomainService.pageMemories(userId, request.getType(), pageNo, pageSize);
        Page<MemoryItemDTO> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(MemoryAssembler.toDTOs(page.getRecords()));
        return dtoPage;
    }

    /** 手动创建记忆 */
    public List<String> createMemory(String userId, CreateMemoryRequest request) {
        CandidateMemory cm = org.xhy.application.memory.assembler.MemoryCommandAssembler.toCandidate(request);
        List<CandidateMemory> list = new ArrayList<>();
        list.add(cm);
        return memoryDomainService.saveMemories(userId, null, list);
    }

    /** 归档（软删除）记忆 */
    public boolean deleteMemory(String userId, String itemId) {
        return memoryDomainService.delete(userId, itemId);
    }
}
