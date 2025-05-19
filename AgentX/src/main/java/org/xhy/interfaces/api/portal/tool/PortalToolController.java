package org.xhy.interfaces.api.portal.tool;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.tool.dto.ToolDTO;
import org.xhy.application.tool.dto.ToolVersionDTO;
import org.xhy.application.tool.service.ToolAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.tool.request.CreateToolRequest;
import org.xhy.interfaces.dto.tool.request.MarketToolRequest;
import org.xhy.interfaces.dto.tool.request.QueryToolRequest;
import org.xhy.interfaces.dto.tool.request.UpdateToolRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


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
    * 上架工具
    *
    * 工具审核通过即可上架，并且还需要上传的版本号以及更新日志
    * @param marketToolRequest 工具id
    * @return
    */
    @PostMapping("/market")
    public Result marketTool(@RequestBody @Validated MarketToolRequest marketToolRequest) {
        String userId = UserContext.getCurrentUserId();
        toolAppService.marketTool(marketToolRequest, userId);
        return Result.success().message("上架成功");
    }

    /**
     * 工具市场列表
     * 
     * @param queryToolRequest 查询对象
     * @return
     */
    @GetMapping("/market")
    public Result market(QueryToolRequest queryToolRequest) {
        return Result.success(toolAppService.marketTools(queryToolRequest));
    }
 
    /**
     * 获取工具版本详情
     * 
     * @param toolId 工具id
     * @param version 版本id
     * @return
     */
    @GetMapping("/market/{toolId}/{version}")
    public Result<ToolVersionDTO> getToolVersionDetail(@PathVariable String toolId, @PathVariable String version) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(toolAppService.getToolVersionDetail(toolId, version,userId));
    }

    /**
     * 安装工具
     * @param toolId 工具id
     * @param version 版本id
     * @return
     */
    @PostMapping("/install/{toolId}/{version}")
    public Result installTool(@PathVariable String toolId, @PathVariable String version) {
        String userId = UserContext.getCurrentUserId();
        toolAppService.installTool(toolId, version, userId);
        return Result.success().message("安装成功");
    }

    /**
     * 卸载工具
     * 
     * @param toolId 工具id
     * @return
     */
    @PostMapping("uninstall/{toolId}")
    public Result uninstallTool(@PathVariable String toolId) {
        String userId = UserContext.getCurrentUserId();
        toolAppService.uninstallTool(toolId, userId);
        return Result.success().message("卸载成功");
    }
    

    /**
     * 获取已安装的工具列表
     *
     * @return
     */
    @GetMapping("/installed")
    public Result<Page<ToolVersionDTO>> getInstalledTools(QueryToolRequest queryToolRequest) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(toolAppService.getInstalledTools(userId, queryToolRequest));
    }

    /**
     * 获取工具已发布的所有版本
     * 
     * @param toolId 工具id
     * @return
     */
    @GetMapping("/market/{toolId}/versions")
    public Result<List<ToolVersionDTO>> getToolVersions(@PathVariable String toolId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(toolAppService.getToolVersions(toolId,userId));
    }

    /**
     * 推荐工具
     *
     * @return
     */
    @GetMapping("/recommend")
    public Result<List<ToolVersionDTO>> getRecommendTools() {
        return Result.success(toolAppService.getRecommendTools());
    }

    /**
     * 修改工具版本发布状态
     * @param toolId 工具 id
     * @param version 版本
     * @param publishStatus 发布状态
     * @return
     */
    @PostMapping("/user/{toolId}/{version}/status")
    public Result updateToolVersionStatus(@PathVariable String toolId, @PathVariable String version,Boolean publishStatus) {
        String userId = UserContext.getCurrentUserId();
        toolAppService.updateUserToolVersionStatus(toolId, version, publishStatus, userId);
        return Result.success().message(publishStatus?"发布成功":"下架成功");
    }
}
