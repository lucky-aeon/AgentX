package org.xhy.interfaces.api.portal.tool;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.tool.dto.ToolDTO;
import org.xhy.application.tool.service.impl.ToolAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.tool.request.CreateToolRequest;
import org.xhy.interfaces.dto.tool.request.UpdateToolRequest;

/**
 * 工具市场
 */
@RestController
@RequestMapping("/tools")
public class PortalToolController {

    private final ToolAppService toolAppService;

    public PortalToolController(ToolAppService toolAppService) {
        this.toolAppService = toolAppService;
    }

    /**
     * 上传工具
     * 
     * @param request 创建工具请求
     * @return 创建的工具信息
     */
    @PostMapping
    public Result<ToolDTO> createTool(@RequestBody @Validated CreateToolRequest request) {
        String userId = UserContext.getCurrentUserId();
        ToolDTO tool = toolAppService.uploadTool(request, userId);
        return Result.success(tool);
    }

    /**
     * 获取用户的工具详情
     *
     * @param toolId 工具 id
     * @return
     */
    @GetMapping("/{toolId}")
    public Result<ToolDTO> getToolDetail(@PathVariable String toolId) {
        String userId = UserContext.getCurrentUserId();
        ToolDTO tool = toolAppService.getToolDetail(toolId, userId);
        return Result.success(tool);
    }

    /**
     * 获取用户的工具列表
     * 
     * @return
     */
    @GetMapping("/user")
    public Result<List<ToolDTO>> getUserTools() {
        String userId = UserContext.getCurrentUserId();
        List<ToolDTO> tools = toolAppService.getUserTools(userId);
        return Result.success(tools);
    }

    /**
     * 编辑工具
     * 
     * @param toolId  工具 id
     * @param request
     * @return
     */
    @PutMapping("/{toolId}")
    public Result<ToolDTO> updateTool(@PathVariable String toolId, @RequestBody @Validated UpdateToolRequest request) {
        String userId = UserContext.getCurrentUserId();
        ToolDTO tool = toolAppService.updateTool(toolId, request, userId);
        return Result.success(tool);
    }

    /**
     * 删除工具
     * 
     * @param toolId 工具 id
     * @return
     */
    @DeleteMapping("/{toolId}")
    public Result<Void> deleteTool(@PathVariable String toolId) {
        String userId = UserContext.getCurrentUserId();
        toolAppService.deleteTool(toolId, userId);
        return Result.success();
    }

    /**
     * 上架工具，根据工具 id 进行上架
     * 
     * @return
     */
    @PostMapping("/market/{toolId}")
    public Result marketTool(@PathVariable String toolId) {
        String userId = UserContext.getCurrentUserId();
        toolAppService.marketTool(toolId, userId);
        return Result.success().message("上架成功");
    }

    // 获取工具市场的工具详情
    // @GetMapping("/market/{toolId}")
    // public Result<ToolDTO> getMarketToolDetail(@PathVariable String toolId) {
    // ToolDTO tool = toolAppService.getMarketToolDetail(toolId);
    // return Result.success(tool);
    // }
}
