package org.xhy.interfaces.api.portal.memory;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.memory.dto.MemoryItemDTO;
import org.xhy.application.memory.service.MemoryAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.memory.CreateMemoryRequest;
import org.xhy.interfaces.dto.memory.QueryMemoryRequest;

/** 用户记忆管理（Portal） */
@RestController
@RequestMapping("/portal/memory")
@Validated
public class PortalMemoryController {

    private final MemoryAppService memoryAppService;

    public PortalMemoryController(MemoryAppService memoryAppService) {
        this.memoryAppService = memoryAppService;
    }

    /** 分页列出当前用户的记忆（可选类型过滤） */
    @GetMapping("/items")
    public Result<Page<MemoryItemDTO>> list(QueryMemoryRequest request) {
        String userId = UserContext.getCurrentUserId();
        Page<MemoryItemDTO> page = memoryAppService.listUserMemories(userId, request);
        return Result.success(page);
    }

    /** 手动新增记忆（立即入库并向量化） */
    @PostMapping("/items")
    public Result<?> create(@RequestBody @Valid CreateMemoryRequest request) {
        String userId = UserContext.getCurrentUserId();
        memoryAppService.createMemory(userId, request);
        return Result.success();
    }

    /** 归档（软删除）记忆 */
    @DeleteMapping("/items/{itemId}")
    public Result<Void> delete(@PathVariable String itemId) {
        String userId = UserContext.getCurrentUserId();
        boolean ok = memoryAppService.deleteMemory(userId, itemId);
        return ok ? Result.success() : Result.notFound("记忆不存在或无权限");
    }
}
